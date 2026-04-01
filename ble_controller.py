"""
ble_controller.py - BLE GATT Server for RPi Fuel Automation
============================================================

The RPi acts as a BLE GATT peripheral (server / advertiser), replacing
the external FCC hardware.  Mobile apps, POS systems, or the UFill APOS
can scan for and connect to the RPi using the standard FCC GATT protocol.

MULTI-CLIENT SUPPORT
--------------------
Multiple POS devices can connect simultaneously - one per bay / nozzle pair.
Each connected client gets its own independent PacketAssembler so multi-chunk
BLE packets from different clients never corrupt each other.

    e.g. Bay-1 POS + Bay-2 POS both connected concurrently → 2 assemblers,
         each in its own reassembly state.

Notifications (0xABF2) are broadcast to ALL subscribed clients via bless
update_value(), which is a BLE GATT server convention.  The POS app is
expected to filter responses by nozzle/DU ID embedded in the payload.

Client identification uses the BlueZ D-Bus device path passed in bless
write callbacks via kwargs['device'] → MAC address extracted as client ID.

Stale client sessions are cleaned up automatically after `session_timeout`
seconds of inactivity (no writes received from that client).

ADVERTISING NAME
----------------
Built entirely from config at startup - nothing hardcoded:

    {adv_prefix}{sap_code} {du_nos}

    e.g.  "REL222459 0304"   (adv_prefix="REL", sap_code="222459", du_nos="03"+"04")
    e.g.  "IOT181846 0506"   (adv_prefix="IOT", sap_code="181846", du_nos="05"+"06")

CONFIG KEYS  (under [ble] section of config.yaml)
--------------------------------------------------
    enabled         : true / false
    adv_prefix      : string prefix for the broadcast name  (e.g. "REL", "IOT")
    sap_code        : site SAP/RO code  (e.g. "222459")
    dispensers[].du_no : 2-digit ASCII DU number string ("03", "04", …)
    mtu             : BLE MTU bytes (default 20)
    max_connections : soft cap on simultaneous clients (default 7, BlueZ hw limit)
    session_timeout : seconds before idle client assembler is discarded (default 30)
    reconnect_delay : seconds between server restart attempts on error

GATT PROFILE
------------
    Service UUID  : 0xABF0  (0000ABF0-0000-1000-8000-00805F9B34FB)
    Write Char    : 0xABF1  - client writes command packets here
    Notify Char   : 0xABF2  - server sends response packets here (broadcast)

THREADING MODEL
---------------
    ble-server thread : owns asyncio event loop, runs bless GATT server.
    Main / pump thread: calls start() once; stop() on shutdown.
    No blocking calls from main thread into BLE server.

DEPENDENCIES
------------
    bless >= 0.3.0  : pip install bless

DEBUG LOGGING
-------------
    Set logging level to DEBUG in config.yaml to see every BLE step:
        logging:
          level: "DEBUG"
    Every phase - config parse, server init, service/char registration,
    advertising start, client connect/disconnect, per-client read/write/notify -
    emits a DEBUG line so the full lifecycle can be traced.
"""

import asyncio
import logging
import threading
import time
from typing import Callable, Dict, Optional, Any

from ble_protocol import (
    SERVICE_UUID,
    WRITE_CHAR_UUID,
    NOTIFY_CHAR_UUID,
    PacketAssembler,
    parse_response,
    build_response,
    chunk_packet,
    ACK,
    NACK_SERVICE_OFFLINE,
)

logger = logging.getLogger(__name__)

# Defaults (overridden by config)
DEFAULT_MTU             = 512    # BlueZ on RPi hardware maximum ATT MTU
RECONNECT_DELAY         = 5.0
DEFAULT_MAX_CONNECTIONS = 7      # BlueZ hardware cap on RPi 4
DEFAULT_SESSION_TIMEOUT = 30.0   # seconds of write-inactivity before removing client session


class BLEController:
    """BLE GATT Server - RPi advertises as a BLE peripheral.

    Supports simultaneous connections from multiple POS devices (one per bay).
    Each client gets an independent PacketAssembler keyed by BLE MAC address.

    Public interface is identical to the old BLE-client BLEController so
    main.py requires no changes: from_config(), start(), stop(), is_enabled().
    """

    # ── Static helpers ────────────────────────────────────────────────────

    @staticmethod
    def _du_no_to_bcd(du_no: str) -> int:
        """Convert a 2-digit DU number string to a packed BCD byte.

        Each decimal digit maps to one 4-bit nibble:
            "03"  →  (0 << 4) | 3  =  0x03
            "04"  →  (0 << 4) | 4  =  0x04
            "12"  →  (1 << 4) | 2  =  0x12
            "34"  →  (3 << 4) | 4  =  0x34

        The string is zero-padded to 2 digits and only the last two digits
        are used, so "3" → "03" → 0x03 and "34" → 0x34.
        """
        s = du_no.zfill(2)[-2:]          # normalise to exactly 2 digits
        high = int(s[0]) & 0x0F          # upper nibble from first digit
        low  = int(s[1]) & 0x0F          # lower nibble from second digit
        return (high << 4) | low

    @staticmethod
    def is_enabled(cfg: dict) -> bool:
        """Return True if BLE is enabled in config."""
        enabled = bool(cfg.get('ble', {}).get('enabled', False))
        logger.debug("BLE [is_enabled]: %s", enabled)
        return enabled

    # ── Factory ───────────────────────────────────────────────────────────

    @classmethod
    def from_config(
        cls,
        cfg: dict,
        on_connected:    Optional[Callable] = None,
        on_disconnected: Optional[Callable] = None,
        on_command:      Optional[Callable] = None,
    ) -> "BLEController":
        """Create a BLEController from the full parsed config.yaml dict."""
        logger.debug("BLE [from_config]: parsing config ...")
        ble_cfg  = cfg.get('ble', {})
        site_cfg = cfg.get('site', {})

        # ── adv_prefix ───────────────────────────────────────────────────
        adv_prefix = str(ble_cfg.get('adv_prefix', 'REL')).strip()
        logger.debug("BLE [from_config]: adv_prefix='%s'", adv_prefix)

        # ── sap_code ─────────────────────────────────────────────────────
        sap_code = str(ble_cfg.get('sap_code', '')).strip()
        if not sap_code:
            site_id = str(site_cfg.get('id', '')).strip()
            if site_id:
                sap_code = site_id.split('_')[-1]
                logger.info(
                    "BLE [from_config]: sap_code auto-derived from site.id '%s' -> '%s'",
                    site_id, sap_code,
                )
        logger.debug("BLE [from_config]: sap_code='%s'", sap_code)

        # ── du_nos: DU numbers as plain ASCII, concatenated ──────────────
        # du_no is a 2-digit decimal string in config (e.g. "03", "04").
        # Concatenated directly as ASCII - matching the real FCC / ESP32 format:
        #   "03" + "04"  →  "0304"   (4 pure ASCII bytes 0x30 0x33 0x30 0x34)
        # This ensures the advertising name is fully printable and compatible
        # with PAX APOS and other Android BLE clients.
        du_list = [str(d.get('du_no', '')).strip() for d in ble_cfg.get('dispensers', [])]
        du_nos  = "".join(du_list)   # plain ASCII: "0304", "0506", etc.
        logger.debug(
            "BLE [from_config]: dispensers raw_list=%s  du_nos='%s'",
            du_list, du_nos,
        )

        # ── Build advertising/broadcast name ──────────────────────────────
        adv_name = f"{adv_prefix}{sap_code} {du_nos}"
        adv_name_hex = adv_name.encode('ascii').hex().upper()
        logger.debug(
            "BLE [from_config]: adv_name='%s'  (ascii hex=%s)",
            adv_name, adv_name_hex,
        )

        # ── Tunables ─────────────────────────────────────────────────────
        mtu             = int(ble_cfg.get('mtu', DEFAULT_MTU))
        reconnect_delay = float(ble_cfg.get('reconnect_delay', RECONNECT_DELAY))
        max_connections = int(ble_cfg.get('max_connections', DEFAULT_MAX_CONNECTIONS))
        session_timeout = float(ble_cfg.get('session_timeout', DEFAULT_SESSION_TIMEOUT))
        logger.debug(
            "BLE [from_config]: mtu=%d  reconnect_delay=%.1fs  "
            "max_connections=%d  session_timeout=%.0fs",
            mtu, reconnect_delay, max_connections, session_timeout,
        )

        logger.info(
            "BLEController.from_config: adv_name='%s'  service=%s  mtu=%d  max_connections=%d",
            adv_name, SERVICE_UUID, mtu, max_connections,
        )

        return cls(
            adv_name        = adv_name,
            sap_code        = sap_code,
            mtu             = mtu,
            reconnect_delay = reconnect_delay,
            max_connections = max_connections,
            session_timeout = session_timeout,
            on_connected    = on_connected,
            on_disconnected = on_disconnected,
            on_command      = on_command,
        )

    # ── Constructor ───────────────────────────────────────────────────────

    def __init__(
        self,
        adv_name:        str,
        sap_code:        str               = "",
        mtu:             int               = DEFAULT_MTU,
        reconnect_delay: float             = RECONNECT_DELAY,
        max_connections: int               = DEFAULT_MAX_CONNECTIONS,
        session_timeout: float             = DEFAULT_SESSION_TIMEOUT,
        on_connected:    Optional[Callable] = None,
        on_disconnected: Optional[Callable] = None,
        on_command:      Optional[Callable] = None,
    ) -> None:
        logger.debug(
            "BLE [__init__]: adv_name='%s'  sap_code='%s'  mtu=%d  "
            "max_connections=%d  session_timeout=%.0fs",
            adv_name, sap_code, mtu, max_connections, session_timeout,
        )
        self._adv_name        = adv_name
        self.sap_code         = sap_code
        self._mtu             = mtu
        self._reconnect_delay = reconnect_delay
        self._max_connections = max_connections
        self._session_timeout = session_timeout
        self._on_connected_cb    = on_connected
        self._on_disconnected_cb = on_disconnected
        self._on_command_cb      = on_command    # (client_id, cmd, payload) -> Optional[bytes]

        self._thread:     Optional[threading.Thread]          = None
        self._loop:       Optional[asyncio.AbstractEventLoop] = None
        self._stop_event: threading.Event                     = threading.Event()
        self._async_stop: Optional[asyncio.Event]             = None   # set from stop() via call_soon_threadsafe
        self._server:     Any                                 = None
        self._attempt     = 0    # server start attempt counter

        # ── Per-client state (keyed by BLE MAC address string) ────────────
        # Each connected POS device gets its own PacketAssembler so that
        # multi-chunk BLE writes from different clients never interfere.
        self._assemblers:       Dict[str, PacketAssembler] = {}
        self._client_last_seen: Dict[str, float]           = {}  # monotonic timestamp

        # ── Aggregate connection state ────────────────────────────────────
        self._connected_cache: bool = False  # updated by async keep-alive loop

    # ── Public API ────────────────────────────────────────────────────────

    @property
    def is_connected(self) -> bool:
        """True if at least one BLE client is currently connected.

        Uses a cached value updated by the async keep-alive loop every second,
        because bless 0.3.0 on Linux/BlueZ exposes is_connected as a coroutine
        which cannot be awaited from a synchronous property.
        """
        logger.debug("BLE [is_connected]: cached=%s", self._connected_cache)
        return self._connected_cache

    @property
    def active_client_count(self) -> int:
        """Number of clients that have written within session_timeout seconds."""
        now = time.monotonic()
        return sum(
            1 for ts in self._client_last_seen.values()
            if now - ts <= self._session_timeout
        )

    def start(self) -> None:
        """Start the BLE GATT server on a background daemon thread."""
        logger.debug("BLE [start]: spawning ble-server daemon thread ...")
        self._stop_event.clear()
        self._thread = threading.Thread(
            target=self._thread_main,
            name="ble-server",
            daemon=True,
        )
        self._thread.start()
        logger.info("BLEController: GATT server thread started (ble-server)")

    def stop(self) -> None:
        """Gracefully stop the BLE GATT server (stops advertising).

        Uses an asyncio.Event (_async_stop) to wake up the keep-alive loop
        cleanly so that _run_server's finally block runs while the event loop
        is still live - ensuring bless/BlueZ properly unregisters the GATT
        service.  This avoids the stale registration that caused first-start
        failures after a service/device restart.
        """
        logger.debug("BLE [stop]: signalling stop ...")
        self._stop_event.set()
        if self._loop is not None and not self._loop.is_closed():
            try:
                if self._async_stop is not None:
                    # Wake the keep-alive loop - it will exit naturally,
                    # run finally { await server.stop() }, then return.
                    self._loop.call_soon_threadsafe(self._async_stop.set)
                    logger.debug("BLE [stop]: async_stop event set (clean shutdown)")
                else:
                    # _run_server not yet in keep-alive (still starting up);
                    # fall back to loop.stop() as last resort.
                    self._loop.call_soon_threadsafe(self._loop.stop)
                    logger.debug("BLE [stop]: loop.stop() fallback (server not yet running)")
            except Exception as exc:
                logger.debug("BLE [stop]: stop signal error (ignored): %s", exc)
        if self._thread is not None:
            logger.debug("BLE [stop]: joining ble-server thread (timeout=5s) ...")
            self._thread.join(timeout=5)
            if self._thread.is_alive():
                logger.warning("BLE [stop]: thread still alive after 5s - forcing loop.stop()")
                try:
                    if self._loop and not self._loop.is_closed():
                        self._loop.call_soon_threadsafe(self._loop.stop)
                except Exception:
                    pass
                self._thread.join(timeout=3)
            logger.debug("BLE [stop]: thread joined (alive=%s)", self._thread.is_alive())
        logger.info("BLEController: stopped")

    def notify(self, data: bytes) -> bool:
        """Broadcast a raw packet to ALL connected BLE clients via Notify (0xABF2).

        bless update_value() broadcasts to every subscribed client.
        POS apps are expected to filter by nozzle/DU ID in the payload.

        Thread-safe: can be called from any thread.
        Returns True if the notify was queued, False if no server/loop ready.
        """
        logger.debug("BLE [notify]: %d bytes -> %s", len(data), data.hex().upper())
        if self._server is None or self._loop is None:
            logger.debug("BLE [notify]: server/loop not ready - skipped")
            return False
        asyncio.run_coroutine_threadsafe(
            self._async_notify(data), self._loop
        )
        return True

    # ── BLE server thread ─────────────────────────────────────────────────

    def _thread_main(self) -> None:
        """Entry point for the BLE GATT server daemon thread."""
        logger.debug("BLE [thread_main]: creating new asyncio event loop")
        self._loop = asyncio.new_event_loop()
        asyncio.set_event_loop(self._loop)
        try:
            self._loop.run_until_complete(self._run_server())
        except Exception as exc:
            logger.error("BLE server thread crashed: %s", exc)
        finally:
            logger.debug("BLE [thread_main]: closing event loop")
            self._loop.close()

    async def _run_server(self) -> None:
        """Set up bless GATT server, start advertising, keep alive."""
        logger.debug("BLE [run_server]: importing bless ...")
        try:
            from bless import (
                BlessServer,
                GATTCharacteristicProperties,
                GATTAttributePermissions,
            )
            logger.debug("BLE [run_server]: bless imported OK")
        except ImportError as exc:
            logger.error(
                "BLE: 'bless' not installed (%s) - run: pip install bless. "
                "BLE server will not start.", exc
            )
            return

        while not self._stop_event.is_set():
            self._attempt += 1
            logger.debug("BLE [run_server]: attempt #%d starting ...", self._attempt)

            logger.debug("BLE [run_server]: creating BlessServer name='%s'", self._adv_name)
            server = BlessServer(name=self._adv_name, loop=self._loop)
            server.read_request_func  = self._handle_read
            server.write_request_func = self._handle_write
            self._server = server
            logger.debug("BLE [run_server]: read/write handlers registered")

            try:
                # ── Register GATT service ─────────────────────────────────
                logger.debug("BLE [run_server]: adding service %s ...", SERVICE_UUID)
                await server.add_new_service(SERVICE_UUID)
                logger.debug("BLE [run_server]: service %s added", SERVICE_UUID)

                # Write characteristic 0xABF1 ─────────────────────────────
                # write_without_response is added alongside write so that
                # Android BLE clients (PAX A920, APOS) can use WriteCommand
                # ATT PDUs in addition to WriteRequest PDUs.  Having both
                # flags present allows the client's ATT layer to fully
                # negotiate the bearer (MTU exchange, security, flow) before
                # the first WriteRequest arrives.  Without write_without_response,
                # some Android BLE stacks stall the ATT MTU exchange waiting for
                # a GATT feature flag they expect to see in the service discovery
                # response, which results in a ~17-second timeout then disconnect.
                logger.debug("BLE [run_server]: adding Write char %s ...", WRITE_CHAR_UUID)
                await server.add_new_characteristic(
                    SERVICE_UUID,
                    WRITE_CHAR_UUID,
                    GATTCharacteristicProperties.write
                    | GATTCharacteristicProperties.write_without_response,
                    None,
                    GATTAttributePermissions.writeable,
                )
                logger.debug(
                    "BLE [run_server]: Write char %s added "
                    "(props=write|write_without_response)", WRITE_CHAR_UUID
                )

                # Notify + Read characteristic 0xABF2 ─────────────────────
                logger.debug("BLE [run_server]: adding Notify char %s ...", NOTIFY_CHAR_UUID)
                await server.add_new_characteristic(
                    SERVICE_UUID,
                    NOTIFY_CHAR_UUID,
                    GATTCharacteristicProperties.notify
                    | GATTCharacteristicProperties.read,
                    bytearray(1),
                    GATTAttributePermissions.readable,
                )
                logger.debug("BLE [run_server]: Notify char %s added (props=notify|read)",
                             NOTIFY_CHAR_UUID)

                # ── Start advertising ─────────────────────────────────────
                logger.debug("BLE [run_server]: calling server.start() to begin advertising ...")
                await server.start()
                logger.info(
                    "BLE: GATT server advertising as '%s'  service=%s  max_connections=%d",
                    self._adv_name, SERVICE_UUID, self._max_connections,
                )
                adv_state = await self._get_is_advertising(server)
                logger.debug("BLE [run_server]: advertising started - is_advertising=%s", adv_state)

                # ── Verify BlueZ experimental mode (needed for ATT MTU exchange) ──
                # BlueZ GATT server requires --experimental mode to properly
                # respond to ATT_EXCHANGE_MTU_REQ from Android BLE clients.
                # Without it, PAX connects and discovers services but BlueZ
                # never sends ATT_EXCHANGE_MTU_RSP -> PAX times out after ~17s
                # and disconnects.  Log a clear warning if experimental mode is
                # not detected so the operator knows exactly what to fix.
                await self._check_bluez_experimental()

                # ── Force adapter to discoverable so BlueZ sends ADV_IND ─────
                # BlueZ registers the bless LEAdvertisement object but only
                # activates connectable LE advertising when the adapter goes
                # into Discoverable mode.  Without this call, the advertisement
                # exists in BlueZ's table but no HCI LE advertising PDUs are
                # sent to the controller - PAX gets status=133 (connection
                # timeout) because the air is completely silent.
                await self._set_adapter_discoverable(self._adv_name)

                # ── Fire on_connected callback (server ready) ─────────────
                if self._on_connected_cb is not None:
                    logger.debug("BLE [run_server]: firing on_connected callback (server ready)")
                    try:
                        self._on_connected_cb()
                    except Exception as exc:
                        logger.debug("BLE [run_server]: on_connected callback error: %s", exc)

                # ── Keep alive - poll every second ────────────────────────
                # Use asyncio.Event (_async_stop) so stop() can wake us
                # immediately without calling loop.stop().  This ensures the
                # finally block below runs while the event loop is still live,
                # so `await server.stop()` properly unregisters from BlueZ.
                self._async_stop = asyncio.Event()
                logger.debug("BLE [run_server]: entering keep-alive loop")
                tick = 0
                while not self._async_stop.is_set():
                    try:
                        await asyncio.wait_for(self._async_stop.wait(), timeout=1.0)
                        break   # _async_stop was set - exit cleanly
                    except asyncio.TimeoutError:
                        pass    # 1s tick - do housekeeping below
                    tick += 1

                    # Update connected cache every tick (bless is_connected is a coroutine)
                    self._connected_cache = await self._get_is_connected(server)

                    # Expire stale client sessions once per second
                    self._expire_stale_sessions()

                    if tick % 30 == 0:   # heartbeat every 30s at DEBUG level
                        adv_state = await self._get_is_advertising(server)
                        logger.debug(
                            "BLE [run_server]: heartbeat tick=%d  is_advertising=%s  "
                            "is_connected=%s  active_clients=%d/%d",
                            tick,
                            adv_state,
                            self._connected_cache,
                            self.active_client_count,
                            self._max_connections,
                        )
                self._async_stop = None
                logger.debug("BLE [run_server]: stop signalled - exiting keep-alive loop")

            except Exception as exc:
                logger.error("BLE: GATT server error: %s - retrying in %.0fs",
                             exc, self._reconnect_delay)
                logger.debug("BLE [run_server]: exception detail:", exc_info=True)
                if self._on_disconnected_cb is not None:
                    logger.debug("BLE [run_server]: firing on_disconnected callback")
                    try:
                        self._on_disconnected_cb()
                    except Exception as cb_exc:
                        logger.debug("BLE [run_server]: on_disconnected callback error: %s", cb_exc)
            finally:
                logger.debug("BLE [run_server]: stopping bless server ...")
                try:
                    await server.stop()
                    logger.debug("BLE [run_server]: bless server stopped")
                except Exception as exc:
                    logger.debug("BLE [run_server]: server.stop() error (ignored): %s", exc)
                self._server = None

            if not self._stop_event.is_set():
                logger.debug("BLE [run_server]: waiting %.1fs before retry ...", self._reconnect_delay)
                await asyncio.sleep(self._reconnect_delay)

        logger.info("BLE: GATT server shut down")
        logger.debug("BLE [run_server]: _run_server() returning (total attempts=%d)", self._attempt)

    # ── Per-client session management ─────────────────────────────────────

    @staticmethod
    def _extract_client_id(kwargs: dict) -> str:
        """Extract a stable client identifier from bless write callback kwargs.

        On Linux/BlueZ, bless passes the D-Bus device object path as
        kwargs['device'], e.g. '/org/bluez/hci0/dev_AA_BB_CC_DD_EE_FF'.
        We extract the MAC address as a human-readable client identifier.

        Falls back to 'unknown' if the key is absent (non-BlueZ platforms).
        """
        device_path = str(kwargs.get('device', '')).strip()
        if device_path:
            # D-Bus path: /org/bluez/hci0/dev_AA_BB_CC_DD_EE_FF
            last_part = device_path.split('/')[-1]   # 'dev_AA_BB_CC_DD_EE_FF'
            if last_part.startswith('dev_'):
                mac = last_part[4:].replace('_', ':').upper()
                return mac   # 'AA:BB:CC:DD:EE:FF'
        return 'unknown'

    def _get_assembler(self, client_id: str) -> PacketAssembler:
        """Return the PacketAssembler for client_id, creating one if needed.

        Also updates the last-seen timestamp used for stale session cleanup.
        """
        if client_id not in self._assemblers:
            # Soft cap check - warn but do not refuse (BlueZ enforces hw limit)
            current = len(self._assemblers)
            if current >= self._max_connections:
                logger.warning(
                    "BLE [session]: new client %s - already at max_connections=%d "
                    "(active sessions: %s). BlueZ hardware limit applies.",
                    client_id, self._max_connections,
                    list(self._assemblers.keys()),
                )
            else:
                logger.info(
                    "BLE [session]: new client connected - %s  "
                    "(total active sessions: %d/%d)",
                    client_id, current + 1, self._max_connections,
                )
            self._assemblers[client_id] = PacketAssembler()

        self._client_last_seen[client_id] = time.monotonic()
        return self._assemblers[client_id]

    def _expire_stale_sessions(self) -> None:
        """Remove assemblers for clients that have been idle > session_timeout.

        Called once per second from the keep-alive loop.
        A client that disconnects without sending further writes will be
        cleaned up after session_timeout seconds (default 30s).
        """
        now = time.monotonic()
        stale = [
            cid for cid, ts in self._client_last_seen.items()
            if now - ts > self._session_timeout
        ]
        for cid in stale:
            self._assemblers.pop(cid, None)
            self._client_last_seen.pop(cid, None)
            logger.info(
                "BLE [session]: client %s session expired after %.0fs of inactivity - "
                "assembler removed  (remaining active sessions: %d)",
                cid, self._session_timeout, len(self._assemblers),
            )

    # ── Internal async helpers ────────────────────────────────────────────

    @staticmethod
    async def _check_bluez_experimental() -> None:
        """Detect whether bluetoothd is running in --experimental mode.

        BlueZ GATT server mode requires the --experimental flag for the ATT
        bearer to be properly initialized on LE connections.  Without it:
          - PAX A920 / Android BLE clients send ATT_EXCHANGE_MTU_REQ after
            service discovery
          - BlueZ never replies with ATT_EXCHANGE_MTU_RSP
          - Client times out (~17 s) and disconnects

        We probe for experimental mode by checking whether the adapter exposes
        the 'ExperimentalFeatures' D-Bus property (only present with --experimental)
        OR by reading the bluetoothd command line from /proc.

        If not found, we log a loud WARNING with exact fix instructions so the
        operator can resolve it without debugging from scratch.

        FIX - run on RPi once:
            sudo mkdir -p /etc/systemd/system/bluetooth.service.d
            sudo tee /etc/systemd/system/bluetooth.service.d/experimental.conf <<'EOF'
            [Service]
            ExecStart=
            ExecStart=/usr/lib/bluetooth/bluetoothd --experimental
            EOF
            sudo systemctl daemon-reload
            sudo systemctl restart bluetooth
            sudo systemctl restart fuel-automation

        ALSO add to /etc/bluetooth/main.conf (under [GATT] section):
            [GATT]
            Cache = no
        """
        experimental = False

        # ── Method 1: check /proc for --experimental on bluetoothd cmdline ──
        # pidof needs shell=True for the $() substitution to work.
        try:
            import subprocess
            result = subprocess.run(
                "cat /proc/$(pidof bluetoothd 2>/dev/null)/cmdline 2>/dev/null || true",
                shell=True, capture_output=True, timeout=2, text=False,
            )
            if result.stdout and b"experimental" in result.stdout:
                experimental = True
                logger.debug("BLE [bluez_check]: bluetoothd --experimental confirmed via /proc/cmdline")
        except Exception as e:
            logger.debug("BLE [bluez_check]: /proc cmdline check failed: %s", e)

        # ── Method 2: probe D-Bus for ExperimentalFeatures property ─────────
        if not experimental:
            try:
                from dbus_next.aio import MessageBus  # type: ignore
                from dbus_next.constants import BusType  # type: ignore

                bus = await MessageBus(bus_type=BusType.SYSTEM).connect()
                introspection = await bus.introspect('org.bluez', '/org/bluez/hci0')
                adapter = bus.get_proxy_object('org.bluez', '/org/bluez/hci0', introspection)
                props = adapter.get_interface('org.freedesktop.DBus.Properties')
                try:
                    val = await props.call_get('org.bluez.Adapter1', 'ExperimentalFeatures')
                    if val is not None:
                        experimental = True
                        logger.debug(
                            "BLE [bluez_check]: ExperimentalFeatures D-Bus property present"
                            " - experimental mode active"
                        )
                except Exception:
                    pass  # property not exposed -> experimental mode off
                bus.disconnect()
            except Exception as e:
                logger.debug("BLE [bluez_check]: D-Bus experimental check failed: %s", e)

        if experimental:
            logger.info(
                "BLE [bluez_check]: bluetoothd --experimental mode confirmed OK - "
                "ATT MTU exchange will work correctly with PAX A920 / Android BLE clients"
            )
        else:
            logger.warning(
                "BLE [bluez_check]: bluetoothd --experimental mode NOT detected!\n"
                "  This causes PAX A920 / Android BLE clients to time out after ~17 s\n"
                "  because BlueZ does not respond to ATT_EXCHANGE_MTU_REQ without\n"
                "  the --experimental flag.\n"
                "  FIX (run once on RPi, then restart services):\n"
                "    sudo mkdir -p /etc/systemd/system/bluetooth.service.d\n"
                "    sudo tee /etc/systemd/system/bluetooth.service.d/experimental.conf <<'EOF'\n"
                "    [Service]\n"
                "    ExecStart=\n"
                "    ExecStart=/usr/lib/bluetooth/bluetoothd --experimental\n"
                "    EOF\n"
                "    sudo systemctl daemon-reload && sudo systemctl restart bluetooth\n"
                "  Also add to /etc/bluetooth/main.conf under [GATT]:\n"
                "    [GATT]\n"
                "    Cache = no\n"
                "  Then: sudo systemctl restart fuel-automation"
            )

    @staticmethod
    async def _set_adapter_discoverable(adv_name: str = "") -> None:
        """Set BlueZ adapter Discoverable=True and push name into main ADV_IND.

        Two problems are solved here:

        1. bless registers an LEAdvertisement object, but BlueZ 5.x only sends
           actual HCI ADV_IND packets once the adapter enters Discoverable mode.
           Setting Discoverable=True triggers the HCI LE Set Advertise Enable.

        2. bless puts the device name in the SCAN_RSP (scan response), not in
           the main ADV_IND.  Passive BLE scanners never send a SCAN_REQ and
           therefore never see the name.  We use btmgmt add-adv to inject a
           second advertisement instance (Instance 2) that puts both the service
           UUID and local name into the main ADV_IND packet so ALL scanners
           (active and passive) can see the device name.
        """
        try:
            from dbus_next.aio import MessageBus  # type: ignore
            from dbus_next.constants import BusType  # type: ignore
            from dbus_next.signature import Variant  # type: ignore

            bus = await MessageBus(bus_type=BusType.SYSTEM).connect()
            introspection = await bus.introspect('org.bluez', '/org/bluez/hci0')
            adapter = bus.get_proxy_object('org.bluez', '/org/bluez/hci0', introspection)
            props = adapter.get_interface('org.freedesktop.DBus.Properties')
            await props.call_set(
                'org.bluez.Adapter1', 'DiscoverableTimeout', Variant('u', 0)
            )
            await props.call_set(
                'org.bluez.Adapter1', 'Discoverable', Variant('b', True)
            )
            if adv_name:
                await props.call_set(
                    'org.bluez.Adapter1', 'Alias', Variant('s', adv_name)
                )
            bus.disconnect()
            logger.info(
                "BLE [run_server]: adapter Discoverable=True Alias='%s' - "
                "LE ADV_IND (connectable) advertising now active", adv_name
            )
        except Exception as exc:
            logger.warning("BLE [run_server]: could not set Discoverable: %s", exc)

        # Note: device name in main ADV_IND is handled via the
        # BlueZLEAdvertisement patch (Includes=["local-name"]) applied
        # before server.start() - no btmgmt subprocess needed here.

    @staticmethod
    async def _get_is_advertising(server: Any) -> Any:
        """Safely retrieve is_advertising state from bless server.

        bless 0.3.0 on Linux/BlueZ exposes is_advertising as a coroutine;
        on macOS/Windows it may be a plain property or callable.
        This helper handles both so we never get a RuntimeWarning about
        'coroutine was never awaited'.
        """
        attr = getattr(server, 'is_advertising', None)
        if attr is None:
            return '?'
        if asyncio.iscoroutinefunction(attr):
            try:
                return await attr()
            except Exception:
                return '?'
        if callable(attr):
            try:
                return attr()
            except Exception:
                return '?'
        return attr   # plain property value

    @staticmethod
    async def _get_is_connected(server: Any) -> bool:
        """Safely retrieve is_connected state from bless server.

        bless 0.3.0 on Linux/BlueZ exposes is_connected as a coroutine;
        on macOS/Windows it may be a plain callable or property.
        Returns bool; defaults to False on any error.
        """
        attr = getattr(server, 'is_connected', None)
        if attr is None:
            return False
        if asyncio.iscoroutinefunction(attr):
            try:
                return bool(await attr())
            except Exception:
                return False
        if callable(attr):
            try:
                return bool(attr())
            except Exception:
                return False
        return bool(attr)

    # ── GATT callbacks ────────────────────────────────────────────────────

    def _handle_read(self, characteristic: Any, **kwargs: Any) -> bytearray:
        """Called by bless when a BLE client reads a characteristic."""
        client_id = self._extract_client_id(kwargs)
        val = characteristic.value or bytearray()
        logger.debug(
            "BLE [handle_read]: client=%s  char=%s  returning %d bytes: %s",
            client_id, characteristic.uuid,
            len(val), val.hex().upper() if val else '(empty)',
        )
        return val

    def _handle_write(self, characteristic: Any, value: Any, **kwargs: Any) -> None:
        """Called by bless when a BLE client writes to 0xABF1.

        Each client has its own PacketAssembler so multi-chunk packets from
        different POS devices are reassembled independently without corruption.
        """
        if not isinstance(value, (bytes, bytearray)):
            logger.debug("BLE [handle_write]: ignoring non-bytes value type=%s", type(value).__name__)
            return

        # ── Identify the writing client ───────────────────────────────────
        client_id = self._extract_client_id(kwargs)
        data = bytes(value)
        logger.debug(
            "BLE [handle_write]: client=%s  char=%s  rx %d bytes: %s",
            client_id, characteristic.uuid, len(data), data.hex().upper(),
        )

        # ── Feed chunk into this client's assembler ───────────────────────
        assembler = self._get_assembler(client_id)
        logger.debug(
            "BLE [handle_write]: client=%s  feeding chunk to assembler "
            "(active sessions: %d/%d) ...",
            client_id, len(self._assemblers), self._max_connections,
        )
        complete = assembler.feed(data)
        if complete is None:
            logger.debug(
                "BLE [handle_write]: client=%s  packet incomplete - waiting for more chunks",
                client_id,
            )
            return

        logger.debug(
            "BLE [handle_write]: client=%s  packet complete (%d bytes): %s",
            client_id, len(complete), complete.hex().upper(),
        )

        command, ack_nack, payload = parse_response(complete)
        if command is None:
            logger.warning(
                "BLE [handle_write]: client=%s  could not parse packet (%d bytes): %s",
                client_id, len(complete), complete.hex().upper(),
            )
            return

        logger.info(
            "BLE: rx client=%s  cmd=0x%02X  ack=%s  payload_len=%d",
            client_id, command, ack_nack, len(payload) if payload else 0,
        )
        logger.debug(
            "BLE [handle_write]: client=%s  payload hex: %s",
            client_id, payload.hex().upper() if payload else '(none)',
        )

        # ── Dispatch to command handler ───────────────────────────────────
        if self._on_command_cb is not None:
            try:
                response = self._on_command_cb(client_id, command, payload or b"")
            except Exception as exc:
                logger.error("BLE [handle_write]: on_command error cmd=0x%02X: %s", command, exc)
                response = build_response(command, NACK_SERVICE_OFFLINE)

            if response is not None and self._loop is not None and not self._loop.is_closed():
                logger.debug(
                    "BLE [handle_write]: sending response %d bytes for cmd=0x%02X: %s",
                    len(response), command, response.hex().upper(),
                )
                # Called from within the asyncio event loop thread (bless/dbus-next),
                # so create_task is the correct way to schedule the notify coroutine.
                self._loop.create_task(self._async_notify(response))
        else:
            logger.debug("BLE [handle_write]: no on_command handler registered - cmd=0x%02X ignored", command)

    # ── Notify helper ─────────────────────────────────────────────────────

    async def _async_notify(self, data: bytes) -> None:
        """Update the Notify characteristic value and broadcast to all clients.

        bless update_value() notifies all subscribed BLE clients.
        POS apps filter by nozzle/DU ID in the payload to process only
        responses relevant to their bay.

        ATT notifications are capped at ATT_MTU - 3 bytes per packet.
        For responses that exceed this limit, application-level 0x23-chunking
        is used so the PAX client can reassemble the full packet.
        ATT MTU is negotiated per-connection by BlueZ (PAX requests 500,
        BlueZ max is 512, so all responses up to 497 bytes fit in one notify).
        """
        active = self.active_client_count
        # ATT payload per notification = MTU - 3 bytes (ATT opcode + handle)
        att_payload_max = self._mtu - 3  # e.g. 500-3 = 497 bytes

        logger.debug(
            "BLE [async_notify]: broadcasting %d bytes to %d client(s) "
            "(mtu=%d  att_max=%d): %s",
            len(data), active, self._mtu, att_payload_max,
            data.hex().upper() if len(data) <= 64 else data[:32].hex().upper() + "...",
        )
        if self._server is None:
            logger.debug("BLE [async_notify]: server is None - skipped")
            return
        try:
            char = self._server.get_characteristic(NOTIFY_CHAR_UUID)
            if char is None:
                logger.debug("BLE [async_notify]: Notify char %s not found", NOTIFY_CHAR_UUID)
                return

            if len(data) <= att_payload_max:
                # Fits in one ATT notification - send directly, no app-level chunking
                char.value = bytearray(data)
                self._server.update_value(SERVICE_UUID, NOTIFY_CHAR_UUID)
                logger.debug(
                    "BLE [async_notify]: %d bytes sent as single notification", len(data)
                )
            else:
                # Response too large for one ATT notification - use 0x23 app chunking
                chunks = chunk_packet(data, self._mtu)
                logger.debug(
                    "BLE [async_notify]: %d bytes split into %d application chunks (mtu=%d)",
                    len(data), len(chunks), self._mtu,
                )
                for i, chunk in enumerate(chunks):
                    char.value = bytearray(chunk)
                    self._server.update_value(SERVICE_UUID, NOTIFY_CHAR_UUID)
                    await asyncio.sleep(0.01)  # 10ms gap - let client process each chunk
                logger.debug(
                    "BLE [async_notify]: all %d chunks sent", len(chunks)
                )
        except Exception as exc:
            logger.debug("BLE [async_notify]: error: %s", exc)
