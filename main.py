#!/usr/bin/env python3
"""
main.py - RPi4 Fuel Automation Orchestrator
============================================

FLOW
----
1. Continuously poll status of all DUs/nozzles.
2. Detect nozzle hook-status change (previous vs current).
3. OFF_HOOK (nozzle lifted):
     - Read Preset ('H') to check if attendant set an amount/volume.
     - If preset exists  -> Authorize ('A').
     - Read start totalizers for transaction record.
4. ON_HOOK (nozzle replaced):
     - Read Transaction ('R') for volume/amount.
     - Read end totalizers.
     - Clear Sale ('F') to release the dispenser.
     - Publish TransactionRecord to cloud.
     - Reset nozzle state for next sale.
5. Duplicate-operation prevention via per-nozzle action lock.
6. Retry/timeout handled by NozzleController command methods.
7. Communication layer (NozzleController / RS485Transport) is completely
   separate from this state-machine / business-logic layer.
8. Multiple DUs and nozzles handled concurrently via per-nozzle threads.

THREADING MODEL
---------------
    MainThread         - polling loop + edge detection
    action-{nozzle_id} - per-nozzle OFF_HOOK / ON_HOOK handler (daemon)
    paho-network       - MQTT background thread (daemon)

SIGNAL HANDLING
---------------
    SIGINT  (Ctrl+C)   -> graceful shutdown
    SIGTERM (systemd)  -> graceful shutdown
"""

import argparse
import collections
import fcntl
import logging
import logging.handlers
import os
import signal
import struct
import sys
import threading
import time
from dataclasses import dataclass, field
from datetime import datetime
from typing import Dict, List, Optional, Set

import yaml

from pump_controller import NozzleConfig, NozzleController, RS485Transport, TransactionRecord
from cloud_publisher import MQTTPublisher
from tqcl_protocol import PresetType, PumpState

# Populated here (after PumpState is imported) and used by _ble_command_handler
_TQCL_TO_BLE_STATE: Dict[int, int] = {
    PumpState.IDLE:         0,   # Pump idle
    PumpState.CALL:         1,   # Pump calling (nozzle lifted)
    PumpState.PRESET_READY: 2,   # Pump auth
    PumpState.AUTHORIZED:   13,  # Pump auth
    PumpState.FUELING:      3,   # Pump fuelling
    PumpState.PAYABLE:      4,   # Dispensing done (idle, txn ready)
    PumpState.SUSPENDED:    11,  # Pump paused
    PumpState.STOPPED:      7,   # Pump stop
    PumpState.UNKNOWN:      5,   # Pump offline
}

# BLE controller is an optional dependency (requires bless + BlueZ).
# Import is deferred to runtime so missing bless does not prevent RS485-only operation.
# ble_protocol has no hardware deps and is always importable.
try:
    from ble_controller import BLEController
    from ble_protocol import build_response, ACK, NACK_SERVICE_OFFLINE
    _BLE_IMPORT_OK = True
except ImportError:
    _BLE_IMPORT_OK = False

log = logging.getLogger(__name__)


# ── Logging setup ─────────────────────────────────────────────────────────────

def setup_logging(cfg: dict):
    """Configure rotating file + console logging from config.yaml [logging] section."""
    level = getattr(logging, cfg.get('level', 'INFO').upper(), logging.INFO)
    handlers = []

    log_file = cfg.get('file', '/var/log/fuel-automation/rpi_automation.log')
    try:
        os.makedirs(os.path.dirname(log_file), exist_ok=True)
        fh = logging.handlers.RotatingFileHandler(
            log_file,
            maxBytes=cfg.get('max_bytes', 10_485_760),
            backupCount=cfg.get('backup_count', 5),
        )
        fh.setFormatter(logging.Formatter('%(asctime)s [%(levelname)s] %(name)s: %(message)s'))
        handlers.append(fh)
    except PermissionError:
        pass

    if cfg.get('console', True):
        import io
        utf8_stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace', line_buffering=True)
        ch = logging.StreamHandler(utf8_stdout)
        ch.setFormatter(logging.Formatter('%(asctime)s [%(levelname)s] %(name)s: %(message)s'))
        handlers.append(ch)

    logging.basicConfig(level=level, handlers=handlers)


# ── Config loader ─────────────────────────────────────────────────────────────

def load_config(path: str) -> dict:
    """Load and parse config.yaml using PyYAML safe_load."""
    with open(path) as f:
        return yaml.safe_load(f)


def build_nozzle_configs(full_cfg: dict) -> List[NozzleConfig]:
    """Build flat list of NozzleConfig from config.yaml dispenser/nozzle tree."""
    configs = []
    for du in full_cfg.get('dispensers', []):
        du_id = du['du_id']
        dua   = du['dua']
        for nozzle in du.get('nozzles', []):
            if not nozzle.get('enabled', True):
                continue
            configs.append(NozzleConfig(
                nza=nozzle['nza'],
                dua=dua,
                nozzle_id=nozzle['nozzle_id'],
                du_id=du_id,
                product=nozzle['product'],
                product_code=nozzle.get('product_code', 1),
                unit_price=nozzle.get('unit_price', 0.0),
                preset_value=float(nozzle.get('preset_value', 5000.0)),
            ))
    return configs


# ── Dry-run transport ─────────────────────────────────────────────────────────

class DryRunTransport(RS485Transport):
    """Simulated RS485 transport - returns canned responses without hardware."""

    def __init__(self):
        super().__init__('/dev/null', 9600, 0.5, 0.1)

    def connect(self):
        log.info("[DRY-RUN] RS485 transport active - no serial port opened")

    def disconnect(self):
        log.info("[DRY-RUN] RS485 transport disconnected")

    def send_recv(self, frame: bytes, expected_len: int) -> bytes:
        import tqcl_protocol as tqcl
        log.info("[DRY-RUN] TX [%d]: %s", len(frame), frame.hex(' ').upper())
        nza = frame[0]
        dua = frame[1]
        cmd = chr(frame[2]) if len(frame) > 2 else '?'

        if cmd == 'S':
            payload = bytes([nza, dua, 0x01, 0x30, tqcl.EOC])
        elif cmd == 'H':
            preset_data = b'0000100.00'
            payload = bytes([nza, dua, 0x01, 0x30, 0x4D, 0x30]) + preset_data + bytes([tqcl.EOC])
        elif cmd == 'T':
            payload = bytes([nza, dua, 0x01, 0x30]) + b'000463547.300 ' + bytes([tqcl.EOC])
        elif cmd == 'M':
            payload = bytes([nza, dua, 0x01, 0x30]) + b'00005137.880  ' + bytes([tqcl.EOC])
        elif cmd == 'R':
            payload = bytes([nza, dua, 0x08, 0x34]) + b'0093.50' + b'000032.10' + b'000747.90' + bytes([tqcl.EOC])
        elif cmd in ('A', 'O', 'Z', 'P', 'E', 'F', 'D', 'U', 'B', 'C'):
            payload = bytes([nza, dua, ord('Y'), tqcl.EOC])
        else:
            payload = bytes([nza, dua, ord('Y'), tqcl.EOC])

        bcc = 0
        for b in payload:
            bcc ^= b
        resp = payload + bytes([bcc])
        log.info("[DRY-RUN] RX [%d]: %s", len(resp), resp.hex(' ').upper())
        return resp


# ── Per-nozzle transaction state ──────────────────────────────────────────────

@dataclass
class NozzleTransaction:
    """
    Holds state for an active (in-progress) fueling transaction on one nozzle.

    Created when the nozzle goes OFF_HOOK (lifted).
    Used to build the final TransactionRecord when the nozzle goes ON_HOOK (replaced).
    """
    nozzle_id:    int
    txn_id:       str
    start_time:   str
    preset_value: float
    preset_type:  str           # 'amount' or 'volume'
    stot_volume:  float = 0.0   # start volume totalizer
    stot_amount:  float = 0.0   # start amount totalizer
    authorized:   bool  = False


# ── Automation engine ─────────────────────────────────────────────────────────

class FuelAutomation:
    """
    Top-level orchestrator using an edge-detection state machine.

    Polling loop detects nozzle hook-status transitions and fires
    per-nozzle action threads for OFF_HOOK and ON_HOOK events.
    """

    def __init__(self, cfg: dict, dry_run: bool = False):
        self._cfg     = cfg
        self._dry_run = dry_run
        self._running = False

        setup_logging(cfg.get('logging', {}))

        serial_cfg   = cfg['serial']
        protocol_cfg = cfg['protocol']

        self._transport = (
            DryRunTransport() if dry_run else
            RS485Transport(
                port=serial_cfg['port'],
                baud=serial_cfg['baud_rate'],
                timeout=serial_cfg['timeout'],
                gap=serial_cfg['inter_command_gap'],
            )
        )

        self._publisher = MQTTPublisher(cfg.get('mqtt', {}), cfg)

        # ── BLE du_map: BLE pump_no (int) → list of nozzle_ids ──────────────
        # Built from ble.dispensers config so _ble_command_handler can look up
        # which nozzles belong to a given FP number.
        # Also used to build the FP → nozzle mapping for GET_STATUS (0x01).
        self._ble_du_map: Dict[int, List[int]] = {}    # pump_no → [nozzle_ids]
        self._ble_du_order: List[int] = []             # ordered list of pump_nos for 0x01
        for ble_du in cfg.get('ble', {}).get('dispensers', []):
            try:
                pump_no = int(str(ble_du.get('du_no', '0')).strip())
                nozzle_ids = list(ble_du.get('nozzles', []))
                self._ble_du_map[pump_no] = nozzle_ids
                self._ble_du_order.append(pump_no)
            except (ValueError, TypeError):
                pass

        # ── BLE transaction history: last 3 completed transactions per nozzle ─
        # Updated in _handle_on_hook after every successful sale.
        # Read in _ble_command_handler for GET_LAST_3_TRANSACTIONS (0x0A).
        self._ble_txn_history: Dict[int, collections.deque] = {
            nid: collections.deque(maxlen=3) for nid in (
                nid for nids in self._ble_du_map.values() for nid in nids
            )
        }

        # ── BLE controller (optional — only if ble.enabled=true in config) ────
        self._ble: Optional['BLEController'] = None
        if _BLE_IMPORT_OK and BLEController.is_enabled(cfg):
            self._ble = BLEController.from_config(
                cfg,
                on_connected=lambda: log.info("BLE: FCC connected"),
                on_disconnected=lambda: log.info("BLE: FCC disconnected"),
                on_command=self._ble_command_handler,
            )
            log.info("BLE controller initialised (FCC sap_code=%s)",
                     getattr(self._ble, 'sap_code', '?'))
        elif cfg.get('ble', {}).get('enabled', False) and not _BLE_IMPORT_OK:
            log.warning("BLE enabled in config but 'bless' is not installed. "
                        "Run: pip install bless")

        self._nozzle_cfgs = build_nozzle_configs(cfg)

        self._controllers: Dict[int, NozzleController] = {}
        for ncfg in self._nozzle_cfgs:
            ctrl = NozzleController(
                cfg=ncfg,
                transport=self._transport,
                protocol_cfg=protocol_cfg,
                on_transaction=None,   # we handle the callback ourselves below
            )
            self._controllers[ncfg.nozzle_id] = ctrl
            log.info("Registered nozzle %d: NZA=0x%02X DUA=0x%02X product=%s",
                     ncfg.nozzle_id, ncfg.nza, ncfg.dua, ncfg.product)

        # ── Per-nozzle state ──────────────────────────────────────────────────
        # Previous nozzle_on_hook value (None = not yet polled)
        self._prev_on_hook: Dict[int, Optional[bool]] = {
            nid: None for nid in self._controllers
        }
        # Active transaction per nozzle (None = idle)
        self._active_txns: Dict[int, Optional[NozzleTransaction]] = {
            nid: None for nid in self._controllers
        }
        # Nozzles currently executing an action thread (OFF_HOOK or ON_HOOK handler)
        self._action_in_progress: Set[int] = set()
        self._action_lock = threading.Lock()
        # Consecutive clear_sale failure count per nozzle (for backoff)
        self._stuck_fail_count: Dict[int, int] = {
            nid: 0 for nid in self._controllers
        }
        # Timestamp of last _clear_stuck trigger per nozzle (for non-blocking backoff)
        self._stuck_next_retry: Dict[int, float] = {
            nid: 0.0 for nid in self._controllers
        }
        # Track whether we already logged the STOPPED waiting message (suppress repeat logs)
        self._prev_stopped_logged: Dict[int, bool] = {
            nid: False for nid in self._controllers
        }
        # Consecutive poll-failure count per nozzle.
        # When this crosses POWER_RESTART_THRESHOLD and the next poll succeeds,
        # the dispenser is treated as having just power-cycled → Clear Sale + Pump Start.
        self._consecutive_poll_failures: Dict[int, int] = {
            nid: 0 for nid in self._controllers
        }
        # Number of back-to-back poll failures before we declare a power restart.
        # At 0.5 s poll interval: 6 failures = ~3 seconds of silence.
        self._POWER_RESTART_THRESHOLD: int = 6

        signal.signal(signal.SIGINT,  self._shutdown)
        signal.signal(signal.SIGTERM, self._shutdown)

    # ── Startup ───────────────────────────────────────────────────────────────

    def start(self):
        log.info("=" * 60)
        log.info("RPi4 Fuel Automation starting - site=%s", self._cfg['site']['id'])
        log.info("Protocol: TQCL v2.06 Rev.7  |  Nozzles: %d active",
                 len(self._controllers))
        log.info("=" * 60)

        self._transport.connect()
        self._publisher.connect()   # returns immediately; MQTT connects in background
        self._publisher.replay_pending()

        # Start BLE daemon thread if configured (returns immediately;
        # BLE scans and connects in its own thread, never blocks RS485 flow).
        if self._ble is not None:
            self._ble.start()
            log.info("BLE daemon thread started (scanning for FCC in background)")

        self._running = True

        # Startup scan: clear any nozzles stuck in PAYABLE from previous session
        for ctrl in self._controllers.values():
            self._startup_check(ctrl)

        self._main_loop()

    def _startup_check(self, ctrl: NozzleController):
        """
        Poll nozzle at startup.

        Steps:
          1. If stuck in PAYABLE  -> Read Transaction then Clear Sale.
          2. If stuck in STOPPED  -> skip Clear Sale (dispenser NAKs it);
             Pump Start will handle recovery.
          3. Always send Pump Start ('O') to put dispenser into Remote
             Control Mode.  Without this the dispenser operates in
             standalone/attended mode and ignores our preset/authorize
             commands on the next lift.
        """
        nza = ctrl.cfg.nza
        resp = ctrl.poll_status()
        if not resp:
            log.error("NZA 0x%02X startup: no status response", nza)
            return

        log.info("NZA 0x%02X startup: state=%-14s nozzle=%s",
                 nza, resp.state.name,
                 "ON_HOOK" if resp.nozzle_on_hook else "OFF_HOOK")

        if resp.state == PumpState.PAYABLE:
            log.warning("NZA 0x%02X startup: stuck in PAYABLE - reading txn then clearing",
                        nza)
            ctrl.read_transaction()   # dispenser may require R before F
            ok = ctrl.clear_sale()
            if ok:
                log.info("NZA 0x%02X startup: clear_sale OK", nza)
            else:
                log.error("NZA 0x%02X startup: clear_sale FAILED - "
                          "dispenser may need manual reset", nza)
        elif resp.state == PumpState.STOPPED:
            log.warning("NZA 0x%02X startup: in STOPPED state - "
                        "skipping Clear Sale (NAK'd in STOPPED); sending Pump Start", nza)

        # Always send Pump Start to activate Remote Control Mode.
        # This is required on every service start so the dispenser accepts
        # our authorize/preset commands.  Applies regardless of current state.
        ps_ok = ctrl.pump_start()
        if ps_ok:
            log.info("NZA 0x%02X startup: pump_start OK - remote control mode active", nza)
        else:
            # FAILED on IDLE dispensers is normal - they are already in remote
            # control mode and NAK a redundant Pump Start.  Not an error.
            log.info("NZA 0x%02X startup: pump_start NAK'd "
                     "(dispenser already in remote/IDLE mode - OK)", nza)

        # Seed the previous hook state so the first poll delta is correct
        self._prev_on_hook[ctrl.cfg.nozzle_id] = resp.nozzle_on_hook

    # ── Main polling loop ─────────────────────────────────────────────────────

    def _main_loop(self):
        """
        Continuously poll all nozzles and fire edge-triggered action threads.

        Edge detection (previous vs current nozzle_on_hook):
            True  -> False  :  OFF_HOOK  (nozzle lifted)   -> _handle_off_hook()
            False -> True   :  ON_HOOK   (nozzle replaced)  -> _handle_on_hook()
        """
        interval = self._cfg['protocol']['status_poll_interval']

        while self._running:
            for nozzle_id, ctrl in self._controllers.items():

                # Skip nozzles already executing an action
                with self._action_lock:
                    if nozzle_id in self._action_in_progress:
                        continue

                resp = ctrl.poll_status()
                if not resp:
                    self._consecutive_poll_failures[nozzle_id] = (
                        self._consecutive_poll_failures.get(nozzle_id, 0) + 1
                    )
                    fails = self._consecutive_poll_failures[nozzle_id]
                    if fails == self._POWER_RESTART_THRESHOLD:
                        log.warning("NZA 0x%02X poll: %d consecutive failures - "
                                    "dispenser may have lost power",
                                    ctrl.cfg.nza, fails)
                    elif fails > self._POWER_RESTART_THRESHOLD and fails % 20 == 0:
                        log.warning("NZA 0x%02X poll: still no response (%d failures)",
                                    ctrl.cfg.nza, fails)
                    continue

                # ── Dispenser just came back after silence → power restart ────
                prev_failures = self._consecutive_poll_failures.get(nozzle_id, 0)
                self._consecutive_poll_failures[nozzle_id] = 0   # reset on success

                if prev_failures >= self._POWER_RESTART_THRESHOLD:
                    log.warning("NZA 0x%02X poll: dispenser responded after %d failures - "
                                "POWER RESTART detected → running Clear Sale + Pump Start",
                                ctrl.cfg.nza, prev_failures)
                    # Reset hook/state so the next lift is detected as a fresh edge
                    self._prev_on_hook[nozzle_id] = None
                    self._active_txns[nozzle_id] = None
                    self._prev_stopped_logged[nozzle_id] = False
                    with self._action_lock:
                        self._action_in_progress.add(nozzle_id)
                    threading.Thread(
                        target=self._power_restart_recovery,
                        args=(ctrl, nozzle_id),
                        daemon=True,
                        name=f"pwrrst-{nozzle_id}",
                    ).start()
                    continue   # skip normal edge logic until recovery thread finishes

                curr_on_hook = resp.nozzle_on_hook
                prev_on_hook = self._prev_on_hook.get(nozzle_id)

                # Clear STOPPED log suppression flag when state leaves STOPPED
                if resp.state != PumpState.STOPPED:
                    self._prev_stopped_logged[nozzle_id] = False

                # Log every state/hook change
                if prev_on_hook is not None and curr_on_hook != prev_on_hook:
                    log.info("NZA 0x%02X  %-14s  hook: %s -> %s",
                             ctrl.cfg.nza,
                             resp.state.name,
                             "ON_HOOK"  if prev_on_hook else "OFF_HOOK",
                             "ON_HOOK"  if curr_on_hook  else "OFF_HOOK")

                # Update previous state BEFORE spawning thread (prevents double-fire)
                self._prev_on_hook[nozzle_id] = curr_on_hook

                # ── OFF_HOOK edge: nozzle lifted ──────────────────────────────
                if prev_on_hook is True and curr_on_hook is False:
                    with self._action_lock:
                        self._action_in_progress.add(nozzle_id)
                    threading.Thread(
                        target=self._handle_off_hook,
                        args=(ctrl, nozzle_id),
                        daemon=True,
                        name=f"action-{nozzle_id}",
                    ).start()

                # ── ON_HOOK edge: nozzle replaced ─────────────────────────────
                elif prev_on_hook is False and curr_on_hook is True:
                    with self._action_lock:
                        self._action_in_progress.add(nozzle_id)
                    threading.Thread(
                        target=self._handle_on_hook,
                        args=(ctrl, nozzle_id),
                        daemon=True,
                        name=f"action-{nozzle_id}",
                    ).start()

                # ── PAYABLE with no active transaction (stuck) ────────────────
                # Only PAYABLE triggers Clear Sale — STOPPED self-recovers to IDLE.
                # Non-blocking backoff: check timestamp before spawning thread.
                elif (resp.state == PumpState.PAYABLE
                      and self._active_txns.get(nozzle_id) is None
                      and prev_on_hook is not None
                      and time.monotonic() >= self._stuck_next_retry.get(nozzle_id, 0.0)):
                    log.warning("NZA 0x%02X stuck in PAYABLE (no active txn) - clearing",
                                ctrl.cfg.nza)
                    with self._action_lock:
                        self._action_in_progress.add(nozzle_id)
                    threading.Thread(
                        target=self._clear_stuck,
                        args=(ctrl, nozzle_id),
                        daemon=True,
                        name=f"clear-{nozzle_id}",
                    ).start()

                # ── STOPPED with no active transaction ────────────────────────
                # Clear Sale is NOT sent — dispenser NAKs it in STOPPED state.
                # Send Pump Start ('O') once per STOPPED entry to help dispenser
                # re-enter Remote Control Mode and return to IDLE.
                elif (resp.state == PumpState.STOPPED
                      and self._active_txns.get(nozzle_id) is None
                      and prev_on_hook is not None):
                    if self._prev_stopped_logged.get(nozzle_id) is not True:
                        log.info("NZA 0x%02X in STOPPED (no txn) - "
                                 "sending Pump Start to recover", ctrl.cfg.nza)
                        self._prev_stopped_logged[nozzle_id] = True
                        with self._action_lock:
                            self._action_in_progress.add(nozzle_id)
                        threading.Thread(
                            target=self._pump_start_recovery,
                            args=(ctrl, nozzle_id),
                            daemon=True,
                            name=f"recover-{nozzle_id}",
                        ).start()

            time.sleep(interval)

    # ── OFF_HOOK handler ──────────────────────────────────────────────────────

    def _handle_off_hook(self, ctrl: NozzleController, nozzle_id: int):
        """
        Nozzle lifted (OFF_HOOK edge).

        Steps:
          1. Read Preset ('H') - check if attendant set a preset on keypad.
          2. If preset exists (value > 0): Authorize ('A').
          3. Read start totalizers.
          4. Save NozzleTransaction state for the ON_HOOK handler.
        """
        nza = ctrl.cfg.nza
        log.info("NZA 0x%02X -- OFF_HOOK -----------------------------------------", nza)

        try:
            # ── 1. Check preset ───────────────────────────────────────────────
            preset_resp = ctrl.read_preset()

            if preset_resp and preset_resp.preset_value > 0.0:
                preset_value = preset_resp.preset_value
                preset_type  = 'volume' if preset_resp.preset_type == '1' else 'amount'
                log.info("NZA 0x%02X preset found: mode=%s type=%s value=%.2f",
                         nza, preset_resp.mode, preset_type, preset_value)
            else:
                # No preset set (mode='N') — covers two cases:
                #   1. Test delivery: dispenser requires a test before normal sales.
                #      Attendant lifts nozzle with no keypad preset; the dispenser
                #      controls the test quantity internally. We must still AUTHORIZE
                #      so the test delivery can actually proceed.
                #   2. Attended mode: attendant intends to authorize via keypad.
                # Either way: authorize and track the transaction; actual volume/amount
                # is read from the dispenser at ON_HOOK via Read Transaction ('R').
                preset_value = 0.0
                preset_type  = 'volume'
                log.info("NZA 0x%02X no preset (mode=%s) - authorizing for "
                         "test delivery / attended mode",
                         nza, preset_resp.mode if preset_resp else '?')

            # ── 2. Authorize ──────────────────────────────────────────────────
            log.info("NZA 0x%02X authorizing ...", nza)
            auth_result = ctrl.authorize()

            if auth_result is True:
                log.info("NZA 0x%02X AUTHORIZATION SUCCESS", nza)
                authorized = True
            elif auth_result is False:
                # Attended mode: attendant presses keypad to authorize physically.
                # Transaction still proceeds - fueling will start after keypad press.
                log.warning("NZA 0x%02X authorize NAK (attended mode) "
                            "- attendant must press keypad", nza)
                authorized = False
            else:
                log.error("NZA 0x%02X authorize: no response - aborting", nza)
                return

            # ── 3. Read start totalizers ──────────────────────────────────────
            stot_vol = ctrl.read_volume_totalizer()
            stot_amt = ctrl.read_amount_totalizer()
            stot_volume = stot_vol.totalizer if stot_vol else 0.0
            stot_amount = stot_amt.totalizer if stot_amt else 0.0
            log.info("NZA 0x%02X STOT_VOL=%.3f  STOT_AMT=%.2f",
                     nza, stot_volume, stot_amount)

            # ── 4. Save transaction state ─────────────────────────────────────
            pump_char = chr(ord('A') + ctrl.cfg.du_id - 1)
            ts     = datetime.now().strftime('%Y%m%d%H%M%S')
            txn_id = f"{ts}{pump_char}{nozzle_id}"

            self._active_txns[nozzle_id] = NozzleTransaction(
                nozzle_id=nozzle_id,
                txn_id=txn_id,
                start_time=datetime.now().isoformat(timespec='seconds'),
                preset_value=preset_value,
                preset_type=preset_type,
                stot_volume=stot_volume,
                stot_amount=stot_amount,
                authorized=authorized,
            )
            log.info("NZA 0x%02X transaction started: txn=%s preset=%s %.2f",
                     nza, txn_id, preset_type, preset_value)

        except Exception as exc:
            log.error("NZA 0x%02X OFF_HOOK handler error: %s", nza, exc, exc_info=True)
        finally:
            with self._action_lock:
                self._action_in_progress.discard(nozzle_id)
            log.info("NZA 0x%02X -- OFF_HOOK done ----------------------------------------", nza)

    # ── ON_HOOK handler ───────────────────────────────────────────────────────

    def _handle_on_hook(self, ctrl: NozzleController, nozzle_id: int):
        """
        Nozzle replaced (ON_HOOK edge).

        Steps:
          1. Read Transaction ('R') for volume/amount/price.
          2. Read end totalizers.
          3. Clear Sale ('F') to release dispenser back to IDLE.
          4. Publish TransactionRecord.
          5. Reset nozzle state for next sale.
        """
        nza = ctrl.cfg.nza
        log.info("NZA 0x%02X -- ON_HOOK ------------------------------------------", nza)

        try:
            txn = self._active_txns.get(nozzle_id)
            end_time = datetime.now().isoformat(timespec='seconds')

            if txn is None:
                # Nozzle was replaced without a tracked transaction
                # (e.g. lifted before system started, or during STOPPED state).
                # Only send Clear Sale if dispenser is actually in PAYABLE (0x34);
                # STOPPED (0x36) does not accept Clear Sale (NAK).
                # Always follow up with Pump Start to re-enter Remote Control Mode.
                status = ctrl.poll_status()
                if status and status.state == PumpState.PAYABLE:
                    log.info("NZA 0x%02X no active txn but PAYABLE - sending Clear Sale", nza)
                    ctrl.clear_sale()
                else:
                    st = status.state.name if status else "UNKNOWN"
                    log.info("NZA 0x%02X no active txn, state=%s - skipping Clear Sale", nza, st)
                # Pump Start only needed if not already IDLE.
                # In IDLE the dispenser is already ready for the next customer;
                # sending Pump Start when IDLE always NAKs (dispenser rejects it).
                # STOPPED needs Pump Start to re-enter Remote Control Mode.
                if status and status.state == PumpState.IDLE:
                    log.info("NZA 0x%02X no-txn ON_HOOK: state=IDLE, pump_start not needed", nza)
                else:
                    ps_ok = ctrl.pump_start()
                    log.info("NZA 0x%02X pump_start (no-txn ON_HOOK): %s",
                             nza, "OK" if ps_ok else "FAILED")
                return

            log.info("NZA 0x%02X closing txn=%s", nza, txn.txn_id)

            # ── 1. Read transaction details ───────────────────────────────────
            txn_resp = ctrl.read_transaction()
            if txn_resp:
                volume     = txn_resp.volume
                amount     = txn_resp.amount
                unit_price = txn_resp.unit_price or ctrl.cfg.unit_price
                log.info("NZA 0x%02X Read Txn: vol=%.3fL  amt=%.2f  price=%.2f",
                         nza, volume, amount, unit_price)
            else:
                log.error("NZA 0x%02X read_transaction failed - using zeros", nza)
                volume     = 0.0
                amount     = 0.0
                unit_price = ctrl.cfg.unit_price

            # ── 2. Read end totalizers ────────────────────────────────────────
            etot_vol = ctrl.read_volume_totalizer()
            etot_amt = ctrl.read_amount_totalizer()
            etot_volume = etot_vol.totalizer if etot_vol else 0.0
            etot_amount = etot_amt.totalizer if etot_amt else 0.0
            log.info("NZA 0x%02X ETOT_VOL=%.3f  ETOT_AMT=%.2f",
                     nza, etot_volume, etot_amount)

            # Totalizer cross-check
            vol_diff = round(etot_volume - txn.stot_volume, 3)
            amt_diff = round(etot_amount - txn.stot_amount, 2)
            if volume > 0 and abs(vol_diff - volume) > 0.1:
                log.warning("NZA 0x%02X totalizer mismatch: "
                            "txn_vol=%.3f  tot_diff=%.3f",
                            nza, volume, vol_diff)

            # ── 3. Clear Sale ─────────────────────────────────────────────────
            # If nozzle was returned during SUSPENDED state, the dispenser may
            # not yet be in PAYABLE (it may transition SUSPENDED→STOPPED→PAYABLE).
            # Try pump_stop first to help it settle, then attempt clear_sale.
            # If clear_sale still fails, _clear_stuck will retry on next PAYABLE poll.
            pre_status = ctrl.poll_status()
            if pre_status and pre_status.state == PumpState.SUSPENDED:
                log.warning("NZA 0x%02X state=SUSPENDED at ON_HOOK - sending Pump Stop to settle",
                            nza)
                ctrl.pump_stop()
            cs_ok = ctrl.clear_sale(txn_id=txn.txn_id)
            if cs_ok:
                log.info("NZA 0x%02X Clear Sale OK - dispenser released to IDLE", nza)
            else:
                log.warning("NZA 0x%02X Clear Sale FAILED - dispenser may stay in PAYABLE; "
                            "_clear_stuck will retry on next poll", nza)

            # ── 3b. Pump Start - re-enter Remote Control Mode ─────────────────
            # Required after every Clear Sale so the next nozzle lift is handled
            # by the controller (preset + authorize), not in standalone mode.
            ps_ok = ctrl.pump_start()
            log.info("NZA 0x%02X Pump Start after Clear Sale: %s",
                     nza, "OK - remote control mode active" if ps_ok else "FAILED")

            # ── 4. Publish record ─────────────────────────────────────────────
            du_id = ctrl.cfg.du_id
            record = TransactionRecord(
                nozzle_id=nozzle_id,
                product=ctrl.cfg.product,
                unit_price=unit_price,
                preset_type=txn.preset_type,
                preset_value=txn.preset_value,
                stot_volume=txn.stot_volume,
                stot_amount=txn.stot_amount,
                etot_volume=etot_volume,
                etot_amount=etot_amount,
                volume=volume,
                amount=amount,
                start_time=txn.start_time,
                end_time=end_time,
                transaction_id=txn.txn_id,
                density=self._cfg.get('transaction', {}).get('density_default', 0.830),
            )

            log.info("NZA 0x%02X TRANSACTION COMPLETE: vol=%.3fL  amt=%.2f  txn=%s",
                     nza, volume, amount, txn.txn_id)
            self._publisher.publish_transaction(record, du_id=du_id)

            # ── 4b. Store in BLE transaction history (last 3 per nozzle) ─────
            if nozzle_id in self._ble_txn_history:
                self._ble_txn_history[nozzle_id].append(record)
                log.debug("NZA 0x%02X BLE txn history updated (%d stored)",
                          nza, len(self._ble_txn_history[nozzle_id]))

            # ── 5. Reset state ────────────────────────────────────────────────
            self._active_txns[nozzle_id] = None
            self._stuck_fail_count[nozzle_id] = 0      # clear any stuck backoff
            self._stuck_next_retry[nozzle_id] = 0.0   # allow immediate retry if needed
            log.info("NZA 0x%02X nozzle ready for next sale", nza)

        except Exception as exc:
            log.error("NZA 0x%02X ON_HOOK handler error: %s", nza, exc, exc_info=True)
        finally:
            with self._action_lock:
                self._action_in_progress.discard(nozzle_id)
            log.info("NZA 0x%02X -- ON_HOOK done -----------------------------------------", nza)

    # ── Stuck PAYABLE cleaner ─────────────────────────────────────────────────

    def _clear_stuck(self, ctrl: NozzleController, nozzle_id: int):
        """
        Attempt to unstick a nozzle stuck in PAYABLE with no active transaction.

        Only called for PAYABLE state (0x34).  STOPPED state (0x36) self-recovers.

        Strategy:
          1. Read Transaction ('R') — some firmware requires the master to read
             transaction data before it accepts Clear Sale ('F').
          2. Send Clear Sale ('F').
          3. On failure, schedule the next retry via _stuck_next_retry timestamp
             (exponential backoff, max 30 s).  The main poll loop checks the
             timestamp so it never blocks nozzle-lift detection.
          4. After 10 consecutive failures, log a MANUAL RESET warning.
        """
        nza = ctrl.cfg.nza
        fail_count = self._stuck_fail_count.get(nozzle_id, 0)

        try:
            # Step 1: Read Transaction first
            txn = ctrl.read_transaction()
            if txn:
                log.info("NZA 0x%02X clear_stuck: read_transaction ok "
                         "vol=%.3fL amt=%.2f unit_price=%.2f",
                         nza, txn.volume, txn.amount, txn.unit_price)

            # Step 2: Send Clear Sale
            ok = ctrl.clear_sale()
            if ok:
                log.info("NZA 0x%02X stuck PAYABLE cleared successfully", nza)
                self._stuck_fail_count[nozzle_id] = 0
                self._stuck_next_retry[nozzle_id] = 0.0   # allow immediate retry if needed
                # Step 3: Pump Start - re-enter Remote Control Mode so next
                # lift is handled by controller (not standalone attended mode)
                ps_ok = ctrl.pump_start()
                if ps_ok:
                    log.info("NZA 0x%02X pump_start OK after clear_stuck", nza)
                else:
                    log.warning("NZA 0x%02X pump_start FAILED after clear_stuck "
                                "- will retry on next cycle", nza)
            else:
                fail_count += 1
                self._stuck_fail_count[nozzle_id] = fail_count
                # Exponential backoff: 2, 4, 8, 16, 30, 30, ... seconds (non-blocking)
                backoff = min(2 ** fail_count, 30)
                self._stuck_next_retry[nozzle_id] = time.monotonic() + backoff

                if fail_count == 1:
                    log.error("NZA 0x%02X clear_stuck: clear_sale FAILED "
                              "(dispenser NAK'd 'F') - retry in %ds",
                              nza, backoff)
                elif fail_count >= 10:
                    log.error("NZA 0x%02X clear_stuck: %d consecutive failures - "
                              "MANUAL RESET required at pump keypad. Retry in %ds.",
                              nza, fail_count, backoff)
                else:
                    log.warning("NZA 0x%02X clear_stuck: attempt %d failed - "
                                "retry in %ds",
                                nza, fail_count, backoff)

        except Exception as exc:
            log.error("NZA 0x%02X clear_stuck error: %s", nza, exc)
            fail_count += 1
            self._stuck_fail_count[nozzle_id] = fail_count
            self._stuck_next_retry[nozzle_id] = time.monotonic() + min(2 ** fail_count, 30)
        finally:
            with self._action_lock:
                self._action_in_progress.discard(nozzle_id)

    # ── Power-restart recovery ────────────────────────────────────────────────

    def _power_restart_recovery(self, ctrl: NozzleController, nozzle_id: int):
        """
        Full recovery sequence after a dispenser power restart is detected.

        Triggered when poll_status() starts succeeding again after
        _POWER_RESTART_THRESHOLD consecutive failures (i.e., the dispenser
        was off and has just come back online).

        Steps (identical to _startup_check):
          1. Poll current state.
          2. If PAYABLE  -> Read Transaction + Clear Sale (clear stale txn).
          3. If STOPPED  -> skip Clear Sale (dispenser NAKs it); Pump Start handles it.
          4. Always send Pump Start ('O') to re-enter Remote Control Mode.
          5. Re-seed _prev_on_hook so the next nozzle lift is detected correctly.
        """
        nza = ctrl.cfg.nza
        try:
            resp = ctrl.poll_status()
            if not resp:
                log.error("NZA 0x%02X power-restart recovery: no status response", nza)
                return

            log.info("NZA 0x%02X power-restart recovery: state=%-14s nozzle=%s",
                     nza, resp.state.name,
                     "ON_HOOK" if resp.nozzle_on_hook else "OFF_HOOK")

            if resp.state == PumpState.PAYABLE:
                log.warning("NZA 0x%02X power-restart: stuck in PAYABLE - "
                            "reading txn then clearing", nza)
                ctrl.read_transaction()
                ok = ctrl.clear_sale()
                if ok:
                    log.info("NZA 0x%02X power-restart: clear_sale OK", nza)
                else:
                    log.error("NZA 0x%02X power-restart: clear_sale FAILED - "
                              "dispenser may need manual reset", nza)
            elif resp.state == PumpState.STOPPED:
                log.warning("NZA 0x%02X power-restart: STOPPED state - "
                            "skipping Clear Sale; sending Pump Start", nza)

            ps_ok = ctrl.pump_start()
            if ps_ok:
                log.info("NZA 0x%02X power-restart: pump_start OK - "
                         "remote control mode active", nza)
            else:
                log.info("NZA 0x%02X power-restart: pump_start NAK'd "
                         "(dispenser already in remote/IDLE mode - OK)", nza)

            # Re-seed hook state for correct next-lift edge detection
            self._prev_on_hook[nozzle_id] = resp.nozzle_on_hook

        except Exception as exc:
            log.error("NZA 0x%02X power-restart recovery error: %s", nza, exc, exc_info=True)
        finally:
            with self._action_lock:
                self._action_in_progress.discard(nozzle_id)

    # ── STOPPED recovery via Pump Start ──────────────────────────────────────

    def _pump_start_recovery(self, ctrl: NozzleController, nozzle_id: int):
        """
        Send Pump Start ('O') to a nozzle stuck in STOPPED state.

        Pump Start is the standard way to return the dispenser to Remote
        Control Mode from STOPPED.  Clear Sale ('F') is NOT sent here —
        the dispenser NAKs it in STOPPED state.

        If Pump Start succeeds the dispenser transitions to IDLE and the
        main loop will detect the next nozzle lift normally.
        If it fails we reset _prev_stopped_logged so the main loop will
        retry on the next STOPPED poll cycle.
        """
        nza = ctrl.cfg.nza
        try:
            ok = ctrl.pump_start()
            if ok:
                log.info("NZA 0x%02X STOPPED recovery: Pump Start OK - "
                         "dispenser entering remote control mode", nza)
            else:
                log.warning("NZA 0x%02X STOPPED recovery: Pump Start FAILED - "
                            "will retry next cycle", nza)
                # Reset flag so the next poll cycle retries
                self._prev_stopped_logged[nozzle_id] = False
        except Exception as exc:
            log.error("NZA 0x%02X STOPPED recovery error: %s", nza, exc)
            self._prev_stopped_logged[nozzle_id] = False
        finally:
            with self._action_lock:
                self._action_in_progress.discard(nozzle_id)

    # ── BLE command handler ───────────────────────────────────────────────────

    def _ble_command_handler(self, client_id: str, command: int, payload: bytes) -> Optional[bytes]:
        """Handle an incoming BLE FCC protocol command and return response bytes.

        Called from the BLE asyncio thread (bless write callback).
        MUST NOT block — no RS485 I/O, only cached state reads.

        Returns:
            bytes : ACK response to notify back to the BLE client.
            None  : No response (unknown/unhandled command sends NACK).
        """
        if not _BLE_IMPORT_OK:
            return None

        try:
            # ── 0x04  Get Site Details ────────────────────────────────────────
            if command == 0x04:
                site_cfg = self._cfg.get('site', {})
                site_id  = str(site_cfg.get('id', '')).encode('ascii', errors='replace')
                site_name = str(site_cfg.get('name', '')).encode('ascii', errors='replace')
                data = site_id[:12].ljust(12, b'\x00') + site_name[:20].ljust(20, b'\x00')
                log.debug("BLE cmd 0x04 site_details: client=%s  id=%s", client_id, site_cfg.get('id'))
                return build_response(command, ACK, data)

            # ── 0x06  Get Product Details ─────────────────────────────────────
            elif command == 0x06:
                # Deduplicate products by product_code across all nozzles
                seen: Dict[int, NozzleConfig] = {}
                for ncfg in self._nozzle_cfgs:
                    if ncfg.product_code not in seen:
                        seen[ncfg.product_code] = ncfg
                products = list(seen.values())
                data = bytes([len(products)])
                for ncfg in products:
                    price_raw = int(ncfg.unit_price * 100)
                    name_bytes = ncfg.product.encode('ascii', errors='replace')[:10].ljust(10, b'\x00')
                    data += bytes([ncfg.product_code]) + struct.pack(">I", price_raw) + name_bytes
                log.debug("BLE cmd 0x06 products: client=%s  count=%d", client_id, len(products))
                return build_response(command, ACK, data)

            # ── 0x18  Shift Status ────────────────────────────────────────────
            elif command == 0x18:
                now = datetime.now()
                shift_number = 1   # no shift tracking; always report shift 1
                data = (
                    bytes([0x01])                   # shift_status = Running
                    + struct.pack(">I", shift_number)
                    + bytes([
                        now.year - 2000, now.month,  now.day,
                        now.hour,        now.minute,  now.second,
                    ])
                )
                log.debug("BLE cmd 0x18 shift_status: client=%s  shift=%d  %s",
                          client_id, shift_number, now.isoformat(timespec='seconds'))
                return build_response(command, ACK, data)

            # ── 0x01  Get Status ──────────────────────────────────────────────
            elif command == 0x01:
                data = bytes([len(self._ble_du_order)])
                for pump_no in self._ble_du_order:
                    nozzle_ids = self._ble_du_map.get(pump_no, [])
                    pump_state     = 0    # idle
                    active_nozzle  = 0
                    vol_raw        = 0
                    amt_raw        = 0
                    price_raw      = 0
                    nozzle_entries = []

                    for nid in nozzle_ids:
                        ctrl = self._controllers.get(nid)
                        if ctrl is None:
                            continue
                        state     = ctrl.current_state
                        ble_state = _TQCL_TO_BLE_STATE.get(state, 5)
                        nozzle_entries.append((ctrl.cfg.nza, ble_state, ctrl.cfg.product_code))

                        # Elevate FP state to the most active nozzle state
                        if state in (PumpState.FUELING, PumpState.AUTHORIZED,
                                     PumpState.CALL, PumpState.PRESET_READY):
                            pump_state    = ble_state
                            active_nozzle = ctrl.cfg.nza
                            price_raw     = int(ctrl.cfg.unit_price * 100)
                            txn = self._active_txns.get(nid)
                            if txn:
                                vol_raw = int(txn.stot_volume * 100)   # start vol (running vol not available)
                                amt_raw = int(txn.stot_amount * 100)
                        elif state == PumpState.PAYABLE and pump_state == 0:
                            pump_state    = ble_state
                            active_nozzle = ctrl.cfg.nza
                            price_raw     = int(ctrl.cfg.unit_price * 100)

                    data += bytes([pump_no, pump_state, active_nozzle])
                    data += struct.pack(">I", vol_raw)
                    data += struct.pack(">I", amt_raw)
                    data += struct.pack(">I", price_raw)
                    data += bytes([len(nozzle_entries)])
                    for nza, nz_st, prod_id in nozzle_entries:
                        data += bytes([nza, nz_st, prod_id])

                log.debug("BLE cmd 0x01 get_status: client=%s  fp_count=%d",
                          client_id, len(self._ble_du_order))
                return build_response(command, ACK, data)

            # ── 0x0A  Get Last 3 Transactions ────────────────────────────────
            elif command == 0x0A:
                if not payload:
                    return build_response(command, NACK_SERVICE_OFFLINE)
                pump_no    = payload[0]
                nozzle_ids = self._ble_du_map.get(pump_no, [])

                # Collect all stored transactions for this FP, sort newest first
                all_txns: List[tuple] = []   # (end_time_str, nozzle_id, record)
                for nid in nozzle_ids:
                    for rec in self._ble_txn_history.get(nid, []):
                        all_txns.append((rec.end_time, nid, rec))
                all_txns.sort(key=lambda t: t[0], reverse=True)
                txns_to_send = all_txns[:3]

                data = bytes([pump_no, len(txns_to_send)])
                for _, nid, rec in txns_to_send:
                    ctrl = self._controllers.get(nid)
                    nza      = ctrl.cfg.nza      if ctrl else 1
                    prod_id  = ctrl.cfg.product_code if ctrl else 1
                    try:
                        end_dt = datetime.fromisoformat(rec.end_time)
                    except (ValueError, TypeError):
                        end_dt = datetime.now()
                    # Unique ID: last 4 digits of transaction_id (numeric suffix)
                    try:
                        uid = int(rec.transaction_id[-4:]) & 0xFFFF
                    except (ValueError, IndexError):
                        uid = 0
                    vol_raw = int(rec.volume * 100)
                    amt_raw = int(rec.amount * 100)
                    # 19-byte record: pump_no nza yr mo dy uid(2B) hr mn sc vol(4B) amt(4B) prod
                    data += bytes([
                        pump_no, nza,
                        end_dt.year - 2000, end_dt.month, end_dt.day,
                    ])
                    data += struct.pack(">H", uid)
                    data += bytes([end_dt.hour, end_dt.minute, end_dt.second])
                    data += struct.pack(">I", vol_raw)
                    data += struct.pack(">I", amt_raw)
                    data += bytes([prod_id])

                log.debug("BLE cmd 0x0A last3txns: client=%s  pump=%d  count=%d",
                          client_id, pump_no, len(txns_to_send))
                return build_response(command, ACK, data)

            # ── 0x26  Get Last 5 Transactions Extended ────────────────────────
            elif command == 0x26:
                if not payload:
                    return build_response(command, NACK_SERVICE_OFFLINE)
                pump_no    = payload[0]
                nozzle_ids = self._ble_du_map.get(pump_no, [])

                # Collect all stored transactions for this FP, sort newest first
                all_txns: List[tuple] = []
                for nid in nozzle_ids:
                    for rec in self._ble_txn_history.get(nid, []):
                        all_txns.append((rec.end_time, nid, rec))
                all_txns.sort(key=lambda t: t[0], reverse=True)
                txns_to_send = all_txns[:5]   # extended: up to 5

                data = bytes([pump_no, len(txns_to_send)])
                for _, nid, rec in txns_to_send:
                    ctrl = self._controllers.get(nid)
                    nza      = ctrl.cfg.nza          if ctrl else 1
                    prod_id  = ctrl.cfg.product_code if ctrl else 1
                    try:
                        end_dt = datetime.fromisoformat(rec.end_time)
                    except (ValueError, TypeError):
                        end_dt = datetime.now()
                    try:
                        uid = int(rec.transaction_id[-4:]) & 0xFFFF
                    except (ValueError, IndexError):
                        uid = 0
                    vol_raw   = int(rec.volume     * 100)
                    amt_raw   = int(rec.amount     * 100)
                    price_raw = int(rec.unit_price * 100)
                    # Extended record: pump nza auth(2B) yr mo dy uid(2B) hr mn sc
                    #                  vol(4B) amt(4B) price(4B) mop prod trx_id(20B)
                    data += bytes([pump_no, nza])
                    data += struct.pack(">H", uid)       # auth_number = uid (proxy)
                    data += bytes([end_dt.year - 2000, end_dt.month, end_dt.day])
                    data += struct.pack(">H", uid)
                    data += bytes([end_dt.hour, end_dt.minute, end_dt.second])
                    data += struct.pack(">I", vol_raw)
                    data += struct.pack(">I", amt_raw)
                    data += struct.pack(">I", price_raw)
                    data += bytes([0x01])               # mop = 1 (cash / default)
                    data += bytes([prod_id])
                    trx_bytes = rec.transaction_id.encode('ascii', errors='replace')[:20]
                    data += trx_bytes.ljust(20, b'\x00')

                log.debug("BLE cmd 0x26 last5txns_ext: client=%s  pump=%d  count=%d",
                          client_id, pump_no, len(txns_to_send))
                return build_response(command, ACK, data)

            else:
                log.debug("BLE cmd 0x%02X unhandled — sending NACK (client=%s)", command, client_id)
                return build_response(command, NACK_SERVICE_OFFLINE)

        except Exception as exc:
            log.error("BLE _ble_command_handler error cmd=0x%02X client=%s: %s",
                      command, client_id, exc, exc_info=True)
            return build_response(command, NACK_SERVICE_OFFLINE)

    # ── Transaction publish callback ──────────────────────────────────────────

    def _handle_transaction(self, record: TransactionRecord):
        """Legacy callback - not used in new flow (publishing done in _handle_on_hook)."""
        pass

    # ── Graceful shutdown ─────────────────────────────────────────────────────

    def _shutdown(self, signum, frame):
        log.info("Shutdown signal %d received - stopping ...", signum)
        self._running = False

        # Signal BLE to stop cleanly (non-blocking — wakes async keep-alive loop).
        # RS485 and MQTT teardown proceed immediately without waiting for BLE.
        ble_stop_thread = None
        if self._ble is not None:
            ble_stop_thread = threading.Thread(
                target=self._ble.stop, name="ble-stop", daemon=False,
            )
            ble_stop_thread.start()

        # RS485 and MQTT disconnect immediately — unaffected by BLE
        self._transport.disconnect()
        self._publisher.disconnect()

        # Wait briefly for BLE server.stop() to complete so BlueZ unregisters
        # the GATT service cleanly (prevents stale registration on next start).
        # Max 8s — well within TimeoutStopSec=20 in the service file.
        if ble_stop_thread is not None:
            ble_stop_thread.join(timeout=8)

        sys.exit(0)


# ── Entry point ───────────────────────────────────────────────────────────────

_LOCK_FILE = '/tmp/fuel-automation.lock'
_lock_fh   = None   # module-level so GC doesn't close it


def _acquire_instance_lock():
    """
    Acquire an exclusive flock on _LOCK_FILE.

    If another instance of main.py is already running (regardless of whether
    it was started by systemd, the UAT service, or a manual 'python3 main.py'),
    this will print an error and sys.exit(1) immediately.

    The lock is automatically released by the OS when this process exits,
    so no cleanup is needed on crash or SIGKILL.

    Uses 'a+' (append+read) open mode so the file is never truncated before
    we hold the lock — prevents a race condition where two simultaneous starts
    both truncate the file before either acquires the flock, causing both to
    see an empty PID and incorrectly both succeed.
    """
    global _lock_fh
    _lock_fh = open(_LOCK_FILE, 'a+')   # create if missing; NEVER truncate
    try:
        fcntl.flock(_lock_fh, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except BlockingIOError:
        # Read PID written by the winner — seek to start first (file opened in append mode)
        try:
            _lock_fh.seek(0)
            existing_pid = _lock_fh.read().strip()
        except Exception:
            existing_pid = '?'
        print(
            f"[fuel-automation] ERROR: another instance is already running "
            f"(PID {existing_pid}). "
            f"Run 'sudo kill {existing_pid}' or 'sudo systemctl restart fuel-automation' "
            f"to replace it.",
            file=sys.stderr,
        )
        sys.exit(1)
    # We hold the lock — overwrite file with our PID
    _lock_fh.seek(0)
    _lock_fh.truncate()
    _lock_fh.write(str(os.getpid()))
    _lock_fh.flush()


def main():
    _acquire_instance_lock()

    parser = argparse.ArgumentParser(
        description='RPi4 Fuel Automation - TQCL RS485 driver'
    )
    parser.add_argument('--config',   default='config.yaml',
                        help='Path to config.yaml')
    parser.add_argument('--dry-run',  action='store_true',
                        help='Simulate RS485 without hardware')
    args = parser.parse_args()

    cfg    = load_config(args.config)
    engine = FuelAutomation(cfg, dry_run=args.dry_run)
    engine.start()


if __name__ == '__main__':
    main()
