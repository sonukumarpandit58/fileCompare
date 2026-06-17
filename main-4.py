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
import json
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
from mqtt_topics import apply_topic_gates
from mqtt_log import log_mpd_update

from pump_controller import NozzleConfig, NozzleController, RS485Transport, TransactionRecord
from cloud_publisher import MQTTPublisher
from tqcl_protocol import PresetType, PumpState
from products import get_product_alias, get_product_name, is_valid_product_code

# Tatsuno imports are deferred — only loaded when mpd_type == "tatsuno"
_TATSUNO_IMPORT_OK = False
try:
    from tatsuno_controller import TatsunoTransport, TatsunoController, TatsunoNozzleConfig
    _TATSUNO_IMPORT_OK = True
except ImportError:
    pass

try:
    from version_info import __version__, __build__, __build_date__, __git_hash__
except ImportError:
    __version__ = "dev"
    __build__ = 0
    __build_date__ = "unknown"
    __git_hash__ = ""

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

# BLE controller uses ESP32 UART bridge (requires pyserial).
# Import is deferred to runtime so missing pyserial does not prevent RS485-only operation.
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
    import log_tagger; log_tagger.install()
    try:
        from mqtt_log import scrub_all_logs as _sc; _sc()
    except Exception:
        pass


# ── Config loader ─────────────────────────────────────────────────────────────

def load_config(path: str) -> dict:
    """Load and parse config.yaml using PyYAML safe_load."""
    with open(path) as f:
        return yaml.safe_load(f)


def build_nozzle_configs(full_cfg: dict) -> List[NozzleConfig]:
    """Build flat list of NozzleConfig from config.yaml dispenser/nozzle tree.

    product resolution (priority order):
      1. Explicit 'product' key in config.yaml  (manual override)
      2. Auto-resolved from 'product_code' via the BPCL product catalogue
      3. Fallback to "MS" if code is unknown

    This means a nozzle entry only needs 'product_code' set correctly;
    the alias used in PSV/MQTT is always the canonical TMS productAlias.
    A warning is logged when an unrecognised product_code is encountered.
    """
    configs = []
    for du in full_cfg.get('dispensers', []):
        du_id = du['du_id']
        dua   = du['dua']
        for nozzle in du.get('nozzles', []):
            if not nozzle.get('enabled', True):
                continue
            product_code = nozzle.get('product_code', 1)
            # Resolve alias: explicit config field wins, otherwise derive from code
            explicit_product = nozzle.get('product', '').strip()
            if explicit_product:
                product = explicit_product
            else:
                if not is_valid_product_code(product_code):
                    import logging as _log
                    _log.getLogger(__name__).warning(
                        "Nozzle %d: unknown product_code=%d — defaulting to 'MS'. "
                        "Check products.py for valid codes.",
                        nozzle['nozzle_id'], product_code
                    )
                product = get_product_alias(product_code)
            configs.append(NozzleConfig(
                nza=nozzle['nza'],
                dua=dua,
                nozzle_id=nozzle['nozzle_id'],
                du_id=du_id,
                product=product,
                product_code=product_code,
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

    # Path where TMS-pushed prices are persisted across restarts.
    # Written on every successful price-update; read at startup before
    # RS485 polling begins so nozzles start with the latest known prices.
    _PRICES_FILE = '/var/lib/fuel-automation/prices.json'

    def __init__(self, cfg: dict, dry_run: bool = False, config_path: str = 'config.yaml'):
        self._cfg         = cfg
        self._config_path = config_path
        self._dry_run     = dry_run
        self._running     = False

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
        self._publisher.set_price_update_callback(self._handle_price_update)

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

        # ── BLE transaction history: last 5 completed transactions per nozzle ─
        # Updated in _handle_on_hook after every successful sale.
        # Read in _ble_command_handler for GET_LAST_3/5_TRANSACTIONS.
        self._ble_txn_history: Dict[int, collections.deque] = {
            nid: collections.deque(maxlen=5) for nid in (
                nid for nids in self._ble_du_map.values() for nid in nids
            )
        }
        self._load_txn_history_from_db()

        # Track transaction IDs that have had MOP changed via 0x27.
        # These are excluded from 0x26/0x0A responses (PAX already processed them).
        self._mop_changed_txns: set = set()

        # Store BLE preset payment details (from 0x28) keyed by nozzle_id.
        # When the transaction completes (ON_HOOK), these details are attached
        # to the transaction record and sent to TMS as MOP change data.
        self._ble_preset_payments: Dict[int, dict] = {}

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
            log.warning("BLE enabled in config but 'pyserial' is not installed. "
                        "Run: pip install pyserial")

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

        # DU sibling grouping: nozzle_id → [all nozzle_ids on the same physical DU]
        #
        # Tokheim multi-nozzle DUs share a single pump state machine across all
        # nozzles (NZAs).  When nozzle 3 (HSD) is fueling, nozzles 1 and 2 (MS)
        # on the same DU also transition through CALL→FUELING→PAYABLE even though
        # they were never lifted.  Without this map the code would fire clear_stuck
        # on nozzles 1 and 2 racing against nozzle 3's real ON_HOOK clear_sale,
        # causing NAK errors and corrupted transaction state.
        #
        # Usage: before spawning clear_stuck for nozzle N, check whether any
        # sibling (same DU, different nozzle_id) has an active transaction or
        # action in progress.  If yes, skip — the sibling's ON_HOOK handler will
        # clear the shared pump state for everyone.
        self._du_nozzle_groups: Dict[int, List[int]] = {}
        for du in cfg.get('dispensers', []):
            group = [
                n['nozzle_id'] for n in du.get('nozzles', [])
                if n.get('enabled', True) and n['nozzle_id'] in self._controllers
            ]
            for nid in group:
                self._du_nozzle_groups[nid] = group
        for nid, group in self._du_nozzle_groups.items():
            if len(group) > 1:
                log.info("DU sibling group: nozzle %d shares DU with nozzles %s",
                         nid, [s for s in group if s != nid])

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

        # Track previous pump state per nozzle for state-change logging
        self._prev_pump_state: Dict[int, Optional[PumpState]] = {
            nid: None for nid in self._controllers
        }
        # Track last fueling counter log time per nozzle (avoid flooding)
        self._last_fueling_log: Dict[int, float] = {
            nid: 0.0 for nid in self._controllers
        }

        signal.signal(signal.SIGINT,  self._shutdown)
        signal.signal(signal.SIGTERM, self._shutdown)

    # ── TMS price update handling ─────────────────────────────────────────────

    def _load_persisted_prices(self):
        """Apply prices previously pushed by TMS (from prices.json) to all controllers.

        Called once at startup before the RS485 poll loop begins.
        Prices in this file always override config.yaml values so the device
        uses the latest TMS price even after a reboot.
        """
        try:
            with open(self._PRICES_FILE) as f:
                stored: dict = json.load(f)   # {str(product_code): unit_price}
        except FileNotFoundError:
            log.info("Price store not found (%s) — using config.yaml prices", self._PRICES_FILE)
            return
        except Exception as exc:
            log.warning("Could not load price store: %s — using config.yaml prices", exc)
            return

        applied = 0
        for ctrl in self._controllers.values():
            key = str(ctrl.cfg.product_code)
            if key in stored:
                old = ctrl.cfg.unit_price
                ctrl.cfg.unit_price = float(stored[key])
                if ctrl.cfg.unit_price != old:
                    log.info("Price restore: nozzle %d product_code=%d  %.2f → %.2f",
                             ctrl.cfg.nozzle_id, ctrl.cfg.product_code, old, ctrl.cfg.unit_price)
                    applied += 1
        if applied:
            log.info("Price store: applied %d price(s) from %s", applied, self._PRICES_FILE)

    def _handle_price_update(self, updates: list):
        """Handle a TMS price-update received via MQTT.

        Called from the paho on_message callback (runs in paho's network thread).
        Thread-safe: RS485Transport lock serialises bus access so set_rate()
        safely queues behind any active polling/transaction.

        Args:
            updates: list of dicts, each {"product_code": int, "unit_price": float}

        Persists the full current price map to _PRICES_FILE so prices survive
        restarts even if TMS does not republish them.
        """
        if not updates:
            return

        log.info("\n" + "=" * 66)
        log.info("  *** TMS PRICE UPDATE RECEIVED via MQTT ***")
        log.info("  Source  : TMS Admin -> fuel-automation/rate_update/%s",
                 self._cfg.get("site", {}).get("serial_number", "?"))
        log.info("  Products: %d item(s) to apply", len(updates))
        for e in updates:
            log.info("    [%-10s]  alias=%-10s  new price=Rs %.2f/L",
                     "code=%s" % e.get("product_code", "?"),
                     e.get("alias", "-"),
                     e.get("unit_price", 0.0))
        log.info("=" * 66)

        updated_nozzles  = []
        applied_products = []
        skipped_products = []

        for entry in updates:
            product_code = entry.get('product_code', 0)
            new_price    = entry.get('unit_price', 0.0)
            alias        = str(entry.get('alias', '')).strip().lower()

            matched = False
            for ctrl in self._controllers.values():
                # Match by alias (product name) first — works across TMS and RPi.
                # Fall back to product_code if alias not supplied.
                name_match = alias and ctrl.cfg.product.strip().lower() == alias
                code_match = (not alias) and product_code and ctrl.cfg.product_code == product_code
                if not (name_match or code_match):
                    continue

                matched = True
                old_price = ctrl.cfg.unit_price
                ctrl.cfg.unit_price = new_price
                log.info(
                    "  [MEMORY] nozzle %d [%s] %.2f → %.2f",
                    ctrl.cfg.nozzle_id, ctrl.cfg.product, old_price, new_price,
                )
                ok = ctrl.set_rate(new_price)
                if ok:
                    log.info(
                        "  [MPD OK] nozzle %d [%s] Rs %.2f sent to dispenser via RS-485",
                        ctrl.cfg.nozzle_id, ctrl.cfg.product, new_price,
                    )
                else:
                    log.warning(
                        "  [MPD FAIL] nozzle %d [%s] set_rate failed — "
                        "price saved in memory, hardware may show old price",
                        ctrl.cfg.nozzle_id, ctrl.cfg.product,
                    )
                updated_nozzles.append(ctrl.cfg.nozzle_id)

            label = alias or str(product_code)
            if matched:
                if label not in applied_products:
                    applied_products.append(label)
            else:
                log.warning("  [SKIP] no nozzle matched alias='%s' product_code=%d",
                            alias, product_code)
                skipped_products.append(label)

        if not updated_nozzles:
            log.warning("Price update: no nozzles matched any of the %d product(s)", len(updates))
            return {'applied': [], 'skipped': skipped_products}

        log.info("=" * 66)
        log.info("  *** TMS PRICE UPDATE COMPLETE ***")
        log.info("  Nozzles updated : %s", updated_nozzles)
        log.info("  Applied products: %s", applied_products)
        log.info("  Skipped products: %s", skipped_products)
        log.info("  ACK will be published to TMS via rate_ack topic.")
        log.info("=" * 66)

        # ── Persist current price map so prices survive restarts ──────────────
        try:
            price_map = {
                str(ctrl.cfg.product_code): ctrl.cfg.unit_price
                for ctrl in self._controllers.values()
            }
            import os as _os
            _os.makedirs(_os.path.dirname(self._PRICES_FILE), exist_ok=True)
            with open(self._PRICES_FILE, 'w') as f:
                json.dump(price_map, f, indent=2)
            log.info("Price store saved: %s", self._PRICES_FILE)
        except Exception as exc:
            log.error("Could not save price store: %s", exc)

        return {'applied': applied_products, 'skipped': skipped_products}

    # ── Load transaction history from local DB on startup ────────────────────

    def _load_txn_history_from_db(self):
        """Pre-populate BLE txn history and MOP-changed set from local SQLite DB.

        Reads the last 5 transactions per nozzle (oldest→newest) so the
        in-memory deque is in the correct order: new transactions appended at
        the right push the oldest off the left, not the newest.

        Also restores _mop_changed_txns from the mop_changes table so that
        transactions processed via 0x27 before a restart remain excluded from
        future 0x26/0x0A responses.

        All data is served from local DB — no internet required for 0x26.
        """
        import sqlite3
        db_path = self._cfg.get('cloud', {}).get('db_path',
                    '/var/lib/fuel-automation/transactions.db')
        try:
            conn = sqlite3.connect(db_path)

            # ── Load last 5 transactions per nozzle (oldest-first into deque) ──
            # Fetch the newest 50 rows overall, then reverse so oldest come first.
            # deque(maxlen=5).append() will then keep newest 5 with oldest at left,
            # meaning future appends push the oldest off correctly.
            rows = conn.execute(
                "SELECT txn_id, psv, created_at FROM transactions "
                "ORDER BY created_at DESC LIMIT 50"
            ).fetchall()
            rows = list(reversed(rows))   # oldest first → correct deque order

            # ── Restore MOP-changed txn IDs so 0x26 still excludes them ────────
            try:
                mop_rows = conn.execute(
                    "SELECT DISTINCT txn_id FROM mop_changes"
                ).fetchall()
                for (mid,) in mop_rows:
                    self._mop_changed_txns.add(mid)
                if mop_rows:
                    log.info("BLE txn history: restored %d MOP-changed txns from DB",
                             len(mop_rows))
            except Exception:
                pass  # mop_changes table may not exist on older installs

            conn.close()
        except Exception as exc:
            log.warning("BLE txn history: could not load from DB: %s", exc)
            return

        from pump_controller import TransactionRecord
        loaded = 0
        for txn_id, psv, created_at in rows:
            try:
                f = psv.split('|')
                if len(f) < 21:
                    continue
                nozzle_id   = int(f[3])
                unit_price  = float(f[4])
                amount      = float(f[7])
                start_time  = f[14]
                end_time    = f[15]
                txn_id_str  = f[16]
                product     = f[19]

                stot_vol = float(f[10])
                etot_vol = float(f[12])
                volume   = round(etot_vol - stot_vol, 3)
                if volume <= 0 and unit_price > 0:
                    volume = round(amount / unit_price, 3)

                deque = self._ble_txn_history.get(nozzle_id)
                if deque is None:
                    continue
                # No manual "already full" check — deque(maxlen=5) handles it.
                # Oldest rows (left of deque) are naturally evicted as newer
                # ones are appended.

                rec = TransactionRecord(
                    nozzle_id      = nozzle_id,
                    product        = product,
                    unit_price     = unit_price,
                    preset_type    = 'amount',
                    preset_value   = amount,
                    stot_volume    = stot_vol,
                    stot_amount    = float(f[11]),
                    etot_volume    = etot_vol,
                    etot_amount    = float(f[13]),
                    volume         = volume,
                    amount         = amount,
                    start_time     = start_time,
                    end_time       = end_time,
                    transaction_id = txn_id_str,
                    density        = float(f[9]) if f[9] else 0.83,
                )
                deque.append(rec)
                loaded += 1
            except Exception as exc:
                log.debug("BLE txn history: skip row %s: %s", txn_id, exc)
                continue

        if loaded > 0:
            log.info("BLE txn history: loaded %d transactions from DB "
                     "(deques ready for 0x26/0x0A, no internet needed)", loaded)
        else:
            log.info("BLE txn history: no transactions found in DB")

    # ── Startup ───────────────────────────────────────────────────────────────

    def start(self):
        log.info("=" * 60)
        log.info("RPi4 Fuel Automation starting - site=%s", self._cfg['site']['id'])
        log.info("Version : v%s  build=%d  date=%s  git=%s",
                 __version__, __build__, __build_date__, __git_hash__ or "n/a")
        log.info("Protocol: TQCL v2.06 Rev.7  |  Nozzles: %d active",
                 len(self._controllers))
        log.info("=" * 60)

        self._transport.connect()
        self._publisher.connect()   # returns immediately; MQTT connects in background
        self._publisher.replay_pending()

        # Apply any prices previously pushed by TMS so nozzles start with
        # the latest known prices instead of the static config.yaml values.
        self._load_persisted_prices()

        # Start BLE daemon thread if configured (returns immediately;
        # BLE scans and connects in its own thread, never blocks RS485 flow).
        if self._ble is not None:
            self._ble.start()
            log.info("BLE daemon thread started (scanning for FCC in background)")

        self._running = True

        # Startup scan: clear any nozzles stuck in PAYABLE from previous session
        for ctrl in self._controllers.values():
            self._startup_check(ctrl)

        # Start hourly MPD time-sync daemon thread
        self._time_sync_thread = threading.Thread(
            target=self._time_sync_loop,
            daemon=True,
            name="mpd-time-sync",
        )
        self._time_sync_thread.start()
        log.info("MPD time-sync thread started (checks every hour)")

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

    # ── MPD time synchronisation ─────────────────────────────────────────────

    # Maximum allowed time drift in seconds between RPi clock and MPD clock.
    # If the MPD clock drifts more than this, it gets corrected.
    _TIME_SYNC_TOLERANCE_SECS = 60       # 1 minute

    def _time_sync_loop(self):
        """Background daemon thread: check and correct MPD time periodically.

        Interval is configurable via protocol.time_sync_interval in config.yaml
        (default 3600 seconds = 1 hour).

        For each nozzle controller, reads the dispenser's internal RTC via
        the Read Date Time command (0x84).  If the MPD clock drifts more
        than _TIME_SYNC_TOLERANCE_SECS from the RPi system clock, sends a
        Set Date Time command ('W') to correct it.

        The Set Date Time command only succeeds when the pump is IDLE, so
        if a nozzle is busy (fueling, payable, etc.) it is skipped and
        will be retried on the next cycle.

        Runs once immediately at startup, then every time_sync_interval seconds.
        """
        interval = int(self._cfg['protocol'].get('time_sync_interval', 3600))
        if interval <= 0:
            log.info("MPD time-sync: DISABLED (time_sync_interval=%d)", interval)
            return
        log.info("MPD time-sync: interval=%ds (from config protocol.time_sync_interval)", interval)
        # Short initial delay to let the main loop stabilise after startup
        time.sleep(5)
        while self._running:
            self._sync_all_mpd_times()
            # Sleep in small increments so we can exit promptly on shutdown
            elapsed = 0
            while self._running and elapsed < interval:
                time.sleep(min(10, interval - elapsed))
                elapsed += 10

    def _sync_all_mpd_times(self):
        """Read time from each MPD and correct if drifted beyond tolerance.

        Designed to have near-zero impact on the core polling loop:
          - Aborts entirely if ANY nozzle has an active action (fueling, etc.)
          - Uses only 1 retry per command (max ~0.25s bus hold)
          - Yields 0.5s between DU checks so status polls can interleave
        """
        # Abort if ANY nozzle is in an active action — time sync is low
        # priority and must never delay a fueling/payment cycle.
        with self._action_lock:
            if self._action_in_progress:
                log.debug("MPD time-sync: skipped - active actions on nozzles %s",
                          self._action_in_progress)
                return

        # De-duplicate by DUA — multiple nozzles on the same DU share one RTC.
        # Only need to read/set time once per physical dispenser unit.
        seen_duas: set = set()
        for nozzle_id, ctrl in self._controllers.items():
            dua = ctrl.cfg.dua
            if dua in seen_duas:
                continue
            seen_duas.add(dua)

            # Re-check before each DU — an action may have started since we began
            with self._action_lock:
                if self._action_in_progress:
                    log.debug("MPD time-sync: aborting mid-sync - action started on nozzles %s",
                              self._action_in_progress)
                    return

            try:
                self._sync_mpd_time(ctrl)
            except Exception as exc:
                log.warning("MPD time-sync: NZA 0x%02X error: %s", ctrl.cfg.nza, exc)

            # Yield to let the main polling loop get several status polls through
            # before we touch the bus again for the next DU.
            time.sleep(0.5)

    def _sync_mpd_time(self, ctrl: NozzleController):
        """Read MPD time, compare with RPi, correct if drifted.

        If reading fails (firmware may not support 0x84), falls back to a
        blind set using the RPi system clock so the MPD time stays accurate.
        """
        log.info("MPD time-sync: NZA 0x%02X attempting to read date/time from MPD ...",
                 ctrl.cfg.nza)
        mpd_dt = ctrl.read_date_time()

        if mpd_dt is None:
            # Read not supported by firmware or bus error — fall back to
            # blind set so the MPD clock is at least kept in sync with RPi.
            now = datetime.now()
            log.warning("MPD time-sync: NZA 0x%02X could not read MPD time "
                        "(firmware may not support 0x84) - "
                        "attempting blind set to RPi time %s",
                        ctrl.cfg.nza, now.strftime("%d/%m/%Y %H:%M:%S"))
            self._set_mpd_time(ctrl, now)
            return

        # Build a datetime from MPD response
        try:
            mpd_time = datetime(
                year=mpd_dt.full_year,
                month=mpd_dt.month,
                day=mpd_dt.day,
                hour=mpd_dt.hours,
                minute=mpd_dt.minutes,
                second=mpd_dt.seconds,
            )
        except (ValueError, OverflowError) as exc:
            log.warning("MPD time-sync: NZA 0x%02X invalid MPD time %s: %s",
                        ctrl.cfg.nza, mpd_dt, exc)
            return

        rpi_time = datetime.now()
        drift = abs((rpi_time - mpd_time).total_seconds())

        log.info("MPD time-sync: NZA 0x%02X  MPD time=%s  RPi time=%s  drift=%.0fs",
                 ctrl.cfg.nza,
                 mpd_time.strftime("%d/%m/%Y %H:%M:%S"),
                 rpi_time.strftime("%d/%m/%Y %H:%M:%S"),
                 drift)

        if drift <= self._TIME_SYNC_TOLERANCE_SECS:
            log.info("MPD time-sync: NZA 0x%02X time OK (drift %.0fs <= %ds tolerance) - no update needed",
                     ctrl.cfg.nza, drift, self._TIME_SYNC_TOLERANCE_SECS)
            return

        # Drift exceeds tolerance — correct MPD clock to RPi system time
        now = datetime.now()
        log.info("MPD time-sync: NZA 0x%02X drift %.0fs exceeds %ds tolerance - "
                 "attempting to update MPD time to %s",
                 ctrl.cfg.nza, drift, self._TIME_SYNC_TOLERANCE_SECS,
                 now.strftime("%d/%m/%Y %H:%M:%S"))
        self._set_mpd_time(ctrl, now)

    def _set_mpd_time(self, ctrl: NozzleController, now: datetime):
        """Send Set Date Time ('W') to the MPD and log the result."""
        log.info("MPD time-sync: NZA 0x%02X sending Set Date Time -> "
                 "date=%02d/%02d/%04d time=%02d:%02d:%02d",
                 ctrl.cfg.nza,
                 now.day, now.month, now.year,
                 now.hour, now.minute, now.second)

        ok = ctrl.set_date_time(
            day=now.day,
            month=now.month,
            year=now.year - 2000,
            hours=now.hour,
            minutes=now.minute,
            seconds=now.second,
        )
        if ok:
            log.info("MPD time-sync: NZA 0x%02X TIME UPDATED SUCCESSFULLY - "
                     "new MPD date=%02d/%02d/%04d time=%02d:%02d:%02d",
                     ctrl.cfg.nza,
                     now.day, now.month, now.year,
                     now.hour, now.minute, now.second)
        else:
            log.warning("MPD time-sync: NZA 0x%02X set_date_time FAILED "
                        "(pump may not be in IDLE or firmware does not support 'W' cmd - "
                        "will retry next cycle)",
                        ctrl.cfg.nza)

    # ── Logging helpers ────────────────────────────────────────────────────────

    def _pump_label(self, nozzle_id: int) -> str:
        """Return a human-readable pump label like 'Pump-1/HSD (NZA 0x01)'."""
        ctrl = self._controllers.get(nozzle_id)
        if ctrl:
            return f"Pump-{ctrl.cfg.du_id}/{ctrl.cfg.product}"
        return f"Nozzle-{nozzle_id}"

    _STATE_LABELS = {
        PumpState.IDLE:         "IDLE (ready for customer)",
        PumpState.CALL:         "CALL (nozzle lifted, waiting for preset)",
        PumpState.PRESET_READY: "PRESET READY (preset set, waiting for authorize)",
        PumpState.AUTHORIZED:   "AUTHORIZED (ready to fuel)",
        PumpState.FUELING:      "FUELING (dispensing fuel...)",
        PumpState.PAYABLE:      "PAYABLE (fueling done, waiting for payment)",
        PumpState.SUSPENDED:    "SUSPENDED (fueling paused)",
        PumpState.STOPPED:      "STOPPED (pump stopped)",
        PumpState.UNKNOWN:      "UNKNOWN (offline?)",
    }

    def _state_label(self, state: PumpState) -> str:
        """Return a human-readable state label."""
        return self._STATE_LABELS.get(state, state.name)

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

                # ── Log pump state changes (human-readable) ──────────────────
                prev_state = self._prev_pump_state.get(nozzle_id)
                if prev_state is not None and resp.state != prev_state:
                    label = self._pump_label(nozzle_id)
                    log.info("===== %s | STATE CHANGE: %s --> %s =====",
                             label,
                             self._state_label(prev_state),
                             self._state_label(resp.state))
                    # Publish OFFLINE when nozzle stops responding
                    if resp.state == PumpState.UNKNOWN:
                        self._publisher.publish_nozzle_status(
                            nozzle_id=nozzle_id,
                            status='OFFLINE',
                            product=ctrl.cfg.product,
                        )
                self._prev_pump_state[nozzle_id] = resp.state

                # ── Log live fueling counters every 3 seconds ────────────────
                if resp.state == PumpState.FUELING:
                    now_mono = time.monotonic()
                    if now_mono - self._last_fueling_log.get(nozzle_id, 0.0) >= 3.0:
                        self._last_fueling_log[nozzle_id] = now_mono
                        txn = self._active_txns.get(nozzle_id)
                        label = self._pump_label(nozzle_id)
                        # Read live totalizer to compute running volume/amount
                        live_vol = ctrl.read_volume_totalizer()
                        live_amt = ctrl.read_amount_totalizer()
                        if txn and live_vol and live_amt:
                            running_vol = round(live_vol.totalizer - txn.stot_volume, 3)
                            running_amt = round(live_amt.totalizer - txn.stot_amount, 2)
                            elapsed = (datetime.now() - datetime.fromisoformat(txn.start_time)).total_seconds()
                            log.info("  >> %s | FUELING: %.3f L | Rs %.2f | "
                                     "preset=%s %.2f | elapsed=%.0fs | motor=%s",
                                     label, running_vol, running_amt,
                                     txn.preset_type, txn.preset_value,
                                     elapsed,
                                     "ON" if resp.motor_on else "OFF")
                            self._publisher.publish_nozzle_status(
                                nozzle_id=nozzle_id,
                                status='FUELING',
                                product=ctrl.cfg.product,
                                volume=running_vol,
                                amount=running_amt,
                            )
                        elif txn:
                            elapsed = (datetime.now() - datetime.fromisoformat(txn.start_time)).total_seconds()
                            log.info("  >> %s | FUELING in progress | "
                                     "preset=%s %.2f | elapsed=%.0fs | motor=%s",
                                     label, txn.preset_type, txn.preset_value,
                                     elapsed,
                                     "ON" if resp.motor_on else "OFF")

                # Log every hook change
                if prev_on_hook is not None and curr_on_hook != prev_on_hook:
                    label = self._pump_label(nozzle_id)
                    log.info("===== %s | NOZZLE %s =====",
                             label,
                             "REPLACED (ON HOOK)" if curr_on_hook else "LIFTED (OFF HOOK)")

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

                # ── State-based OFF_HOOK for broken-hook-bit nozzles ─────────
                # Some Tokheim NZAs have a nozzle_on_hook bit that is permanently
                # stuck at True (never reports False even when the nozzle is
                # physically lifted).  The hook-bit edge detector above therefore
                # never fires for these nozzles.
                #
                # Fallback: if the pump state just transitioned from PRESET_READY
                # or IDLE to CALL, the nozzle MUST have been lifted.  Treat this
                # as an OFF_HOOK event.
                #
                # Safety guard — only for nozzles that are the SOLE enabled nozzle
                # on their DU.  On multi-nozzle DUs (e.g. 3-nozzle DU-5) ALL NZAs
                # show CALL simultaneously when any nozzle is lifted, so we MUST
                # NOT fire a spurious OFF_HOOK on the sibling NZAs; the one with a
                # working hook bit fires correctly.  For sole-nozzle DUs there is
                # no ambiguity.
                elif (resp.state == PumpState.CALL
                      and prev_state in (PumpState.PRESET_READY, PumpState.IDLE)
                      and curr_on_hook is True
                      and self._active_txns.get(nozzle_id) is None
                      and len([s for s in self._du_nozzle_groups.get(nozzle_id, [nozzle_id])
                                if s != nozzle_id]) == 0):
                    log.info("NZA 0x%02X: CALL via state-transition "
                             "(hook bit unreliable, sole nozzle on DU) → OFF_HOOK",
                             ctrl.cfg.nza)
                    with self._action_lock:
                        self._action_in_progress.add(nozzle_id)
                    threading.Thread(
                        target=self._handle_off_hook,
                        args=(ctrl, nozzle_id),
                        daemon=True,
                        name=f"action-{nozzle_id}",
                    ).start()

                # ── State-based ON_HOOK for broken-hook-bit nozzles ───────────
                # Companion to the state-based OFF_HOOK above.  When hook bit is
                # stuck True the ON_HOOK edge (False→True) is never detected either.
                # Fallback: if the pump state transitions to PAYABLE (or IDLE) while
                # an active transaction exists, the nozzle was replaced — treat as
                # ON_HOOK.
                elif (resp.state in (PumpState.PAYABLE, PumpState.IDLE)
                      and prev_state not in (PumpState.PAYABLE, PumpState.IDLE, None)
                      and self._active_txns.get(nozzle_id) is not None
                      and len([s for s in self._du_nozzle_groups.get(nozzle_id, [nozzle_id])
                                if s != nozzle_id]) == 0):
                    log.info("NZA 0x%02X: state→%s with active txn "
                             "(hook bit unreliable, sole nozzle on DU) → ON_HOOK",
                             ctrl.cfg.nza, resp.state.name)
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
                #
                # IMPORTANT: On multi-nozzle DUs (e.g. 3-nozzle Tokheim) the pump
                # state is SHARED — when nozzle 3 fuels, nozzles 1 and 2 also show
                # PAYABLE even though they were never lifted.  Skip clear_stuck if
                # any sibling nozzle on the same DU has an active transaction or
                # action in progress; the sibling's ON_HOOK handler will clear the
                # pump state for all nozzles on that DU.
                elif (resp.state == PumpState.PAYABLE
                      and self._active_txns.get(nozzle_id) is None
                      and prev_on_hook is not None
                      and not any(
                          self._active_txns.get(s) is not None
                          or s in self._action_in_progress
                          for s in self._du_nozzle_groups.get(nozzle_id, [])
                          if s != nozzle_id
                      )
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
        label = self._pump_label(nozzle_id)
        log.info("=" * 70)
        log.info("  %s | NOZZLE LIFTED (OFF HOOK)", label)
        log.info("=" * 70)

        # Publish CALL status immediately so TMS knows the nozzle is lifted
        self._publisher.publish_nozzle_status(
            nozzle_id=nozzle_id,
            status='CALL',
            product=ctrl.cfg.product,
        )

        try:
            # ── Check if BLE 0x28 already set a preset on this pump ──────────
            ble_txn = self._active_txns.get(nozzle_id)
            if ble_txn and not ble_txn.authorized:
                log.info("  %s | [STEP 1] BLE PRESET (from PAX 0x28): %s = Rs %.2f | TxnID: %s",
                         label, ble_txn.preset_type.upper(), ble_txn.preset_value, ble_txn.txn_id)

                # Tokheim dispensers may drop the preset back to CALL when the
                # nozzle is physically lifted from PRESET_READY (idle-preset mode).
                # Re-set the preset from the stored 0x28 values so we can authorize.
                curr_status = ctrl.poll_status()
                curr_state  = curr_status.state if curr_status else PumpState.UNKNOWN
                log.info("  %s | [STEP 1] Pump state on nozzle lift: %s",
                         label, curr_state.name if curr_status else 'UNKNOWN')

                if curr_state != PumpState.PRESET_READY:
                    # Preset lost (CALL) or pump reset — re-set using 0x28 values
                    pt = (PresetType.VOLUME if ble_txn.preset_type == 'volume'
                          else PresetType.AMOUNT)
                    log.info("  %s | [STEP 1] Re-setting preset: %s = %.2f",
                             label, ble_txn.preset_type.upper(), ble_txn.preset_value)
                    preset_ok = ctrl.set_preset(pt, ble_txn.preset_value)
                    if not preset_ok:
                        log.error("  %s | [STEP 1] Re-set preset FAILED — aborting", label)
                        self._active_txns[nozzle_id] = None
                        return
                    log.info("  %s | [STEP 1] Re-set preset OK", label)
                else:
                    log.info("  %s | [STEP 1] Pump already in PRESET_READY — no re-set needed",
                             label)

                log.info("  %s | [STEP 2] AUTHORIZING pump (BLE preset) ...", label)
                auth_result = ctrl.authorize()
                if auth_result is True:
                    log.info("  %s | [STEP 2] AUTHORIZE SUCCESS - pump ready to fuel", label)
                    ble_txn.authorized = True
                elif auth_result is False:
                    # NAK — pump may have auto-authorized or is in attended mode.
                    # Mark authorized so we don't re-enter this block.
                    log.warning("  %s | [STEP 2] AUTHORIZE NAK — pump likely auto-authorized "
                                "or in attended mode; marking authorized, fueling should proceed",
                                label)
                    ble_txn.authorized = True
                else:
                    log.error("  %s | [STEP 2] AUTHORIZE FAILED - no response from MPD, aborting", label)
                    self._active_txns[nozzle_id] = None
                    return
                log.info("  %s | READY TO FUEL - customer can start dispensing", label)
                self._publisher.publish_nozzle_status(
                    nozzle_id=nozzle_id,
                    status='AUTHORIZED',
                    product=ctrl.cfg.product,
                )
                return

            # ── Step 1: Check preset ─────────────────────────────────────────
            log.info("  %s | [STEP 1] Reading preset from MPD ...", label)
            preset_value = 0.0
            preset_type  = 'volume'
            preset_resp  = ctrl.read_preset()

            if preset_resp and preset_resp.preset_value > 0.0:
                preset_value = preset_resp.preset_value
                preset_type  = 'volume' if preset_resp.preset_type == '1' else 'amount'
                log.info("  %s | [STEP 1] PRESET FOUND: %s = %.2f (mode=%s)",
                         label, preset_type.upper(), preset_value, preset_resp.mode)
            else:
                log.info("  %s | [STEP 1] No preset yet - waiting for attendant to enter on keypad ...",
                         label)

                preset_wait_timeout = float(self._cfg['protocol'].get('preset_timeout', 90))
                poll_start = time.monotonic()
                while self._running:
                    elapsed = time.monotonic() - poll_start
                    if elapsed >= preset_wait_timeout:
                        log.warning("  %s | [STEP 1] TIMEOUT after %.0fs - no preset entered, aborting",
                                    label, elapsed)
                        return

                    time.sleep(1.0)

                    status = ctrl.poll_status()
                    if status:
                        # Use pump STATE to detect nozzle replacement.
                        # nozzle_on_hook bit is permanently stuck True on some
                        # Tokheim NZAs (hardware quirk); pump returning to IDLE
                        # is the definitive "nozzle replaced" signal for those.
                        if status.state == PumpState.IDLE:
                            log.info("  %s | [STEP 1] Pump returned to IDLE "
                                     "(nozzle replaced) - aborting", label)
                            return
                        # Traditional hook-bit check for reliable NZAs.
                        if status.nozzle_on_hook and status.state != PumpState.CALL:
                            log.info("  %s | [STEP 1] Nozzle replaced while waiting - aborting",
                                     label)
                            return

                    preset_resp = ctrl.read_preset()
                    if preset_resp and preset_resp.preset_value > 0.0:
                        preset_value = preset_resp.preset_value
                        preset_type  = 'volume' if preset_resp.preset_type == '1' else 'amount'
                        log.info("  %s | [STEP 1] PRESET ENTERED by attendant: %s = %.2f (waited %.1fs)",
                                 label, preset_type.upper(), preset_value, elapsed)
                        break
                else:
                    return

            # ── Step 2: Authorize ────────────────────────────────────────────
            log.info("  %s | [STEP 2] AUTHORIZING pump ...", label)
            auth_result = ctrl.authorize()

            if auth_result is True:
                log.info("  %s | [STEP 2] AUTHORIZE SUCCESS - pump ready to fuel", label)
                authorized = True
            elif auth_result is False:
                log.warning("  %s | [STEP 2] AUTHORIZE NAK (attended mode - attendant must press keypad)",
                            label)
                authorized = False
            else:
                log.error("  %s | [STEP 2] AUTHORIZE FAILED - no response from MPD, aborting", label)
                return

            # ── Step 3: Read start totalizers ────────────────────────────────
            log.info("  %s | [STEP 3] Reading start totalizers ...", label)
            stot_vol = ctrl.read_volume_totalizer()
            stot_amt = ctrl.read_amount_totalizer()
            stot_volume = stot_vol.totalizer if stot_vol else 0.0
            stot_amount = stot_amt.totalizer if stot_amt else 0.0
            log.info("  %s | [STEP 3] Start totalizers: Volume=%.3f L | Amount=Rs %.2f",
                     label, stot_volume, stot_amount)

            # ── Step 4: Save transaction state ───────────────────────────────
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
            log.info("  %s | [STEP 4] TRANSACTION STARTED", label)
            log.info("  %s |   TxnID: %s", label, txn_id)
            log.info("  %s |   Preset: %s = Rs %.2f", label, preset_type.upper(), preset_value)
            log.info("  %s |   Product: %s | Price: Rs %.2f/L", label, ctrl.cfg.product, ctrl.cfg.unit_price)
            log.info("  %s |   READY TO FUEL - customer can start dispensing", label)
            self._publisher.publish_nozzle_status(
                nozzle_id=nozzle_id,
                status='AUTHORIZED',
                product=ctrl.cfg.product,
            )

        except Exception as exc:
            log.error("  %s | OFF_HOOK ERROR: %s", label, exc, exc_info=True)
        finally:
            with self._action_lock:
                self._action_in_progress.discard(nozzle_id)

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
        label = self._pump_label(nozzle_id)
        log.info("=" * 70)
        log.info("  %s | NOZZLE REPLACED (ON HOOK) - FUELING STOPPED", label)
        log.info("=" * 70)

        try:
            txn = self._active_txns.get(nozzle_id)
            end_time = datetime.now().isoformat(timespec='seconds')

            if txn is None:
                status = ctrl.poll_status()
                if status and status.state == PumpState.PAYABLE:
                    log.info("  %s | No active txn but PAYABLE - sending Clear Sale", label)
                    ctrl.clear_sale()
                else:
                    st = status.state.name if status else "UNKNOWN"
                    log.info("  %s | No active txn, state=%s - skipping Clear Sale", label, st)
                if status and status.state == PumpState.IDLE:
                    log.info("  %s | Already IDLE - pump ready for next customer", label)
                else:
                    ps_ok = ctrl.pump_start()
                    log.info("  %s | Pump Start: %s", label, "OK" if ps_ok else "FAILED")
                return

            # Calculate fueling duration
            try:
                start_dt = datetime.fromisoformat(txn.start_time)
                duration_secs = (datetime.now() - start_dt).total_seconds()
                duration_str = f"{int(duration_secs // 60)}m {int(duration_secs % 60)}s"
            except Exception:
                duration_str = "unknown"

            log.info("  %s | [STEP 1] READING TRANSACTION from MPD ...", label)

            # ── Step 1: Read transaction details ─────────────────────────────
            txn_resp = ctrl.read_transaction()
            if txn_resp:
                volume     = txn_resp.volume
                amount     = txn_resp.amount
                unit_price = txn_resp.unit_price or ctrl.cfg.unit_price
            else:
                log.error("  %s | [STEP 1] Read transaction FAILED - using zeros", label)
                volume     = 0.0
                amount     = 0.0
                unit_price = ctrl.cfg.unit_price

            # ── Step 2: Read end totalizers ──────────────────────────────────
            log.info("  %s | [STEP 2] READING END TOTALIZERS ...", label)
            etot_vol = ctrl.read_volume_totalizer()
            etot_amt = ctrl.read_amount_totalizer()
            etot_volume = etot_vol.totalizer if etot_vol else 0.0
            etot_amount = etot_amt.totalizer if etot_amt else 0.0

            # Totalizer cross-check
            vol_diff = round(etot_volume - txn.stot_volume, 3)
            amt_diff = round(etot_amount - txn.stot_amount, 2)
            if volume > 0 and abs(vol_diff - volume) > 0.1:
                log.warning("  %s | Totalizer mismatch: txn_vol=%.3f vs tot_diff=%.3f",
                            label, volume, vol_diff)

            # ── Step 3: Clear Sale ───────────────────────────────────────────
            log.info("  %s | [STEP 3] CLEARING SALE on MPD ...", label)
            pre_status = ctrl.poll_status()
            if pre_status and pre_status.state == PumpState.SUSPENDED:
                log.warning("  %s | Pump SUSPENDED - sending Pump Stop first to settle", label)
                ctrl.pump_stop()
            cs_ok = ctrl.clear_sale(txn_id=txn.txn_id)
            if cs_ok:
                log.info("  %s | [STEP 3] Clear Sale OK - dispenser released", label)
            else:
                log.warning("  %s | [STEP 3] Clear Sale FAILED - will retry on next poll", label)

            # ── Step 3b: Pump Start ──────────────────────────────────────────
            ps_ok = ctrl.pump_start()
            log.info("  %s | [STEP 3] Pump Start: %s",
                     label, "OK - remote control active" if ps_ok else "FAILED")

            # ── Step 4: Publish record ───────────────────────────────────────
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

            log.info("  %s | [STEP 4] PUBLISHING TRANSACTION to cloud ...", label)
            self._publisher.publish_transaction(record, du_id=du_id)

            # ── Step 4a: BLE preset payment auto-apply ───────────────────────
            ble_pay = self._ble_preset_payments.pop(nozzle_id, None)
            if ble_pay:
                mop_data = {
                    'mop': ble_pay['mop'],
                    'payment_mode': ble_pay['payment_type'],
                    'discount': 0,
                    'net_amount': amount,
                    'is_mop_change': 1,
                    'is_trx_printed': 0,
                    'is_discount': 0,
                    'terminal_id': ble_pay.get('terminal_id', ''),
                    'mobile_number': ble_pay.get('mobile_number', ''),
                    'vehicle_number': ble_pay.get('vehicle_number', ''),
                    'vehicle_type': ble_pay.get('vehicle_type', 0),
                    'voucher_id': ble_pay.get('payment_trx_id', ''),
                    'cash_memo': ble_pay.get('cash_memo', ''),
                    'txn_reference': ble_pay.get('txn_reference', ''),
                }
                self._publisher.handle_mop_change(txn.txn_id, ctrl.cfg.du_id, mop_data)
                self._mop_changed_txns.add(txn.txn_id)
                log.info("  %s | [STEP 4] BLE payment auto-applied: MOP=%d", label, ble_pay['mop'])

            # ── Store in BLE transaction history ─────────────────────────────
            if nozzle_id in self._ble_txn_history:
                self._ble_txn_history[nozzle_id].append(record)

            # ══════════════════════════════════════════════════════════════════
            # TRANSACTION COMPLETE SUMMARY
            # ══════════════════════════════════════════════════════════════════
            log.info("-" * 70)
            log.info("  %s | TRANSACTION COMPLETE", label)
            log.info("-" * 70)
            log.info("  %s |   TxnID      : %s", label, txn.txn_id)
            log.info("  %s |   Product    : %s", label, ctrl.cfg.product)
            log.info("  %s |   Unit Price : Rs %.2f/L", label, unit_price)
            log.info("  %s |   Volume     : %.3f L", label, volume)
            log.info("  %s |   Amount     : Rs %.2f", label, amount)
            log.info("  %s |   Preset     : %s = Rs %.2f", label, txn.preset_type.upper(), txn.preset_value)
            log.info("  %s |   Duration   : %s", label, duration_str)
            log.info("  %s |   Start Time : %s", label, txn.start_time)
            log.info("  %s |   End Time   : %s", label, end_time)
            log.info("  %s |   Start Tot  : Vol=%.3f L | Amt=Rs %.2f", label, txn.stot_volume, txn.stot_amount)
            log.info("  %s |   End Tot    : Vol=%.3f L | Amt=Rs %.2f", label, etot_volume, etot_amount)
            if ble_pay:
                log.info("  %s |   Payment   : MOP=%d (BLE preset)", label, ble_pay['mop'])
            log.info("-" * 70)
            log.info("  %s | PUMP READY for next customer", label)
            log.info("-" * 70)

            # Publish IDLE so TMS shows the final dispensed totals as last txn
            self._publisher.publish_nozzle_status(
                nozzle_id=nozzle_id,
                status='IDLE',
                product=ctrl.cfg.product,
                volume=volume,
                amount=amount,
            )

            # ── 5. Reset state ────────────────────────────────────────────────
            self._active_txns[nozzle_id] = None
            self._stuck_fail_count[nozzle_id] = 0
            self._stuck_next_retry[nozzle_id] = 0.0

        except Exception as exc:
            log.error("  %s | ON_HOOK ERROR: %s", label, exc, exc_info=True)
        finally:
            with self._action_lock:
                self._action_in_progress.discard(nozzle_id)

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
                    price_raw  = int(ncfg.unit_price * 100)
                    # Use full product name (e.g. "HI-SPD HSD") for BLE display;
                    # falls back to alias if code is not in catalogue.
                    display_name = get_product_name(ncfg.product_code, fallback=ncfg.product)
                    name_bytes   = display_name.encode('ascii', errors='replace')[:10].ljust(10, b'\x00')
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
                # Exclude transactions that have had MOP changed via 0x27
                all_txns: List[tuple] = []   # (end_time_str, nozzle_id, record)
                for nid in nozzle_ids:
                    for rec in self._ble_txn_history.get(nid, []):
                        if rec.transaction_id in self._mop_changed_txns:
                            continue  # skip — MOP already assigned
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
                    vol_raw   = int(rec.volume     * 100)
                    amt_raw   = int(rec.amount     * 100)
                    price_raw = int(rec.unit_price * 100)
                    # 0x0A per-txn record (23 bytes):
                    # pump(1) nozzle(1) yr(1) mo(1) dy(1) uid(2) hr(1) mn(1) sc(1)
                    # vol*100(4) amt*100(4) price*100(4) prod_id(1)
                    data += bytes([
                        pump_no, nza,
                        end_dt.year - 2000, end_dt.month, end_dt.day,
                    ])
                    data += struct.pack(">H", uid)
                    data += bytes([end_dt.hour, end_dt.minute, end_dt.second])
                    data += struct.pack(">I", vol_raw)
                    data += struct.pack(">I", amt_raw)
                    data += struct.pack(">I", price_raw)
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
                # Exclude transactions that have had MOP changed via 0x27
                all_txns: List[tuple] = []
                for nid in nozzle_ids:
                    for rec in self._ble_txn_history.get(nid, []):
                        if rec.transaction_id in self._mop_changed_txns:
                            continue  # skip — MOP already assigned
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
                        start_dt = datetime.fromisoformat(rec.start_time)
                    except (ValueError, TypeError):
                        start_dt = end_dt
                    try:
                        uid = int(rec.transaction_id[-4:]) & 0xFFFF
                    except (ValueError, IndexError):
                        uid = 0
                    vol_raw   = int(rec.volume     * 100)
                    amt_raw   = int(rec.amount     * 100)
                    price_raw = int(rec.unit_price * 100)

                    # ── Per-transaction record per BLE Protocol v2.5 §2.27 ──
                    # Pump Number (1B)
                    data += bytes([pump_no])
                    # Nozzle Number (1B)
                    data += bytes([nza])
                    # Year (1B), Month (1B), Day (1B) — end/completion time
                    data += bytes([end_dt.year - 2000, end_dt.month, end_dt.day])
                    # Unique ID (2B)
                    data += struct.pack(">H", uid)
                    # Hour (1B), Minute (1B), Second (1B) — end/completion time
                    data += bytes([end_dt.hour, end_dt.minute, end_dt.second])
                    # Volume*100 (4B)
                    data += struct.pack(">I", vol_raw)
                    # Amount*100 (4B)
                    data += struct.pack(">I", amt_raw)
                    # Product price*100 (4B)
                    data += struct.pack(">I", price_raw)
                    # Product ID (1B)
                    data += bytes([prod_id])
                    # Auth-ID (2B) — use uid as proxy
                    data += struct.pack(">H", uid)
                    # Trx Start: Year (1B), Month (1B), Day (1B)
                    data += bytes([start_dt.year - 2000, start_dt.month, start_dt.day])
                    # Trx Start: Hour (1B), Minute (1B), Second (1B)
                    data += bytes([start_dt.hour, start_dt.minute, start_dt.second])
                    # Trx Preset Type (1B): per reference FCC device
                    #   0x03 = volume preset (default), 0x04 = amount preset
                    preset_type_val = 0x03
                    preset_val_raw  = 0
                    if hasattr(rec, 'preset_type') and rec.preset_type:
                        if rec.preset_type == 'volume':
                            preset_type_val = 0x03
                            preset_val_raw = int(getattr(rec, 'preset_value', 0) * 100)
                        elif rec.preset_type == 'amount':
                            preset_type_val = 0x04
                            preset_val_raw = int(getattr(rec, 'preset_value', 0) * 100)
                    data += bytes([preset_type_val])
                    # Trx Preset Value (4B)
                    data += struct.pack(">I", preset_val_raw)
                    # Attendant ID (1B)
                    data += bytes([0x00])
                    # Option Byte (1B): 0x02 = bit1 set → Payment Mode field present
                    data += bytes([0x02])
                    # Net Amount*100 (4B)
                    data += struct.pack(">I", amt_raw)
                    # MOP (1B): 0x01 = cash (default; EDC will override via 0x27)
                    data += bytes([0x01])
                    # Payment Mode (1B)
                    data += bytes([0x00])

                log.debug("BLE cmd 0x26 last5txns_ext: client=%s  pump=%d  count=%d",
                          client_id, pump_no, len(txns_to_send))
                return build_response(command, ACK, data)

            # ── 0x27  MOP Change Extended With Extra Fields ─────────────────────
            elif command == 0x27:
                if not payload or len(payload) < 14:
                    return build_response(command, NACK_SERVICE_OFFLINE)

                pos = 0
                pump_no       = payload[pos]; pos += 1
                txn_id_5b     = payload[pos:pos+5]; pos += 5
                is_mop_change = payload[pos]; pos += 1
                is_trx_printed = payload[pos]; pos += 1
                is_discount   = payload[pos]; pos += 1
                discount_raw  = struct.unpack(">I", payload[pos:pos+4])[0]; pos += 4
                net_amt_raw   = struct.unpack(">I", payload[pos:pos+4])[0]; pos += 4
                terminal_id   = payload[pos:pos+10].decode('ascii', errors='replace').strip(); pos += 10
                mop           = payload[pos]; pos += 1
                payment_mode  = payload[pos]; pos += 1
                mobile_number = payload[pos:pos+13].decode('ascii', errors='replace').strip(); pos += 13
                vehicle_number = payload[pos:pos+10].decode('ascii', errors='replace').strip(); pos += 10
                vehicle_type  = payload[pos]; pos += 1
                voucher_id    = payload[pos:pos+20].decode('ascii', errors='replace').strip('\x00').strip(); pos += 20
                cash_memo     = payload[pos:pos+10].decode('ascii', errors='replace').strip(); pos += 10
                txn_reference = ''
                if pos + 40 <= len(payload):
                    txn_reference = payload[pos:pos+40].decode('ascii', errors='replace').strip(); pos += 40

                log.info("BLE cmd 0x27 MOP change: pump=%d mop=%d payment_mode=%d "
                         "voucher=%s mobile=%s vehicle=%s vehicle_type=%d "
                         "discount=%.2f net_amt=%.2f terminal=%s",
                         pump_no, mop, payment_mode, voucher_id, mobile_number,
                         vehicle_number, vehicle_type,
                         discount_raw / 100.0, net_amt_raw / 100.0, terminal_id)

                # ── Find matching transaction ────────────────────────────────
                nozzle_ids = self._ble_du_map.get(pump_no, [])
                matched_txn_id = None
                matched_rec = None

                # Try to match by UID from the 5-byte TxnID
                # The 5B could be [Year, Month, Day, UID_hi, UID_lo] or similar
                # Search all recent txns for this pump
                all_txns = []
                for nid in nozzle_ids:
                    for rec in self._ble_txn_history.get(nid, []):
                        all_txns.append((rec.end_time, nid, rec))
                all_txns.sort(key=lambda t: t[0], reverse=True)

                if all_txns:
                    req_net_amt = net_amt_raw / 100.0  # from 0x27 request

                    # Strategy 1: Match by amount (closest match within 1.0 tolerance)
                    best_match = None
                    best_diff = float('inf')
                    for _, nid, rec in all_txns:
                        diff = abs(rec.amount - req_net_amt)
                        if diff < best_diff:
                            best_diff = diff
                            best_match = (nid, rec)

                    if best_match and best_diff <= 1.0:
                        _, rec = best_match
                        matched_txn_id = rec.transaction_id
                        matched_rec = rec
                        log.info("BLE 0x27: matched txn=%s by amount (req=%.2f rec=%.2f diff=%.2f)",
                                 matched_txn_id, req_net_amt, rec.amount, best_diff)

                    # Strategy 2: Try UID match (bytes 3-4 of txn_id_5b)
                    if not matched_txn_id:
                        req_uid = struct.unpack(">H", txn_id_5b[3:5])[0] if len(txn_id_5b) >= 5 else 0
                        if req_uid != 0:
                            for _, nid, rec in all_txns:
                                try:
                                    rec_uid = int(rec.transaction_id[-4:]) & 0xFFFF
                                except (ValueError, IndexError):
                                    rec_uid = 0
                                if rec_uid == req_uid:
                                    matched_txn_id = rec.transaction_id
                                    matched_rec = rec
                                    log.info("BLE 0x27: matched txn=%s by UID=0x%04X", matched_txn_id, req_uid)
                                    break

                    # Strategy 3: Fallback to most recent
                    if not matched_txn_id:
                        _, _, rec = all_txns[0]
                        matched_txn_id = rec.transaction_id
                        matched_rec = rec
                        log.warning("BLE 0x27: no amount/UID match, fallback to most recent txn=%s (amt=%.2f, req=%.2f)",
                                    matched_txn_id, rec.amount, req_net_amt)

                if not matched_txn_id:
                    log.warning("BLE 0x27: no matching txn for pump=%d", pump_no)
                    data = bytes([pump_no, 0x00])  # error code 0x00 = fail
                    data += b'\x00' * 10
                    return build_response(command, ACK, data)

                # ── Save MOP change to DB and re-publish to cloud ────────────
                mop_data = {
                    'mop': mop,
                    'payment_mode': payment_mode,
                    'discount': discount_raw / 100.0,
                    'net_amount': net_amt_raw / 100.0,
                    'is_mop_change': is_mop_change,
                    'is_trx_printed': is_trx_printed,
                    'is_discount': is_discount,
                    'terminal_id': terminal_id,
                    'mobile_number': mobile_number,
                    'vehicle_number': vehicle_number,
                    'vehicle_type': vehicle_type,
                    'voucher_id': voucher_id,
                    'cash_memo': cash_memo,
                    'txn_reference': txn_reference,
                }
                self._publisher.handle_mop_change(matched_txn_id, pump_no, mop_data)

                # ── Build success response ───────────────────────────────────
                # Response: Pump(1B) + Error(1B=0x01 success) + TxnID(10B ASCII)
                txn_id_ascii = matched_txn_id.encode('ascii', errors='replace')[:10]
                data = bytes([pump_no, 0x01])
                data += txn_id_ascii.ljust(10, b'\x00')

                # Mark as MOP-changed so 0x26/0x0A exclude it
                self._mop_changed_txns.add(matched_txn_id)
                log.info("BLE cmd 0x27 MOP change SUCCESS: pump=%d txn=%s mop=%d (excluded from future 0x26)",
                         pump_no, matched_txn_id, mop)
                return build_response(command, ACK, data)

            elif command == 0x28:
                # ── 0x28: Preset Extended With Extra Field ───────────────────
                # PAX sends preset/authorize command via BLE to remotely control MPD pump.
                # Request: pump(1), nozzle(1), preset_type(1), preset_value*100(4),
                #          MOP(1), payment_type(1), mobile(13), vehicle(10),
                #          vehicle_type(1), payment_trx_id(20), cash_memo(10),
                #          terminal_id(10), txn_reference(40)
                # Response: pump(1), nozzle(1), error_code(1), product_id(1),
                #           product_price*100(4), auth_number(2)
                if not payload or len(payload) < 12:
                    log.warning("BLE cmd 0x28: payload too short (%d bytes)", len(payload) if payload else 0)
                    return build_response(command, NACK_SERVICE_OFFLINE)

                pos = 0
                pump_no      = payload[pos]; pos += 1
                nozzle_no    = payload[pos]; pos += 1
                preset_type  = payload[pos]; pos += 1  # 1=Volume, 2=Amount
                preset_val_raw = struct.unpack(">I", payload[pos:pos+4])[0]; pos += 4
                preset_value = preset_val_raw / 100.0
                mop_28       = payload[pos] if pos < len(payload) else 0; pos += 1
                payment_type = payload[pos] if pos < len(payload) else 0; pos += 1
                mobile_28    = payload[pos:pos+13].decode('ascii', errors='replace').strip('\x00').strip() if pos+13 <= len(payload) else ''; pos += 13
                vehicle_28   = payload[pos:pos+10].decode('ascii', errors='replace').strip('\x00').strip() if pos+10 <= len(payload) else ''; pos += 10
                veh_type_28  = payload[pos] if pos < len(payload) else 0; pos += 1
                pay_trx_id   = payload[pos:pos+20].decode('ascii', errors='replace').strip('\x00').strip() if pos+20 <= len(payload) else ''; pos += 20
                cash_memo_28 = payload[pos:pos+10].decode('ascii', errors='replace').strip('\x00').strip() if pos+10 <= len(payload) else ''; pos += 10
                terminal_28  = payload[pos:pos+10].decode('ascii', errors='replace').strip('\x00').strip() if pos+10 <= len(payload) else ''; pos += 10
                txn_ref_28   = payload[pos:pos+40].decode('ascii', errors='replace').strip('\x00').strip() if pos+40 <= len(payload) else ''; pos += 40

                log.info("BLE cmd 0x28 Preset Extended: pump=%d nozzle=%d preset_type=%d "
                         "preset_value=%.2f MOP=%d payment_type=%d mobile=%s vehicle=%s "
                         "pay_trx_id=%s terminal=%s txn_ref=%s",
                         pump_no, nozzle_no, preset_type, preset_value, mop_28,
                         payment_type, mobile_28, vehicle_28, pay_trx_id,
                         terminal_28, txn_ref_28)

                # ── Find the pump controller for this pump/nozzle ────────────
                nozzle_ids = self._ble_du_map.get(pump_no, [])
                if not nozzle_ids:
                    log.warning("BLE 0x28: no nozzles mapped for pump=%d", pump_no)
                    data = bytes([pump_no, nozzle_no, 0x00, 0x00])  # error_code=0x00 (fail)
                    data += struct.pack(">I", 0)  # price=0
                    data += struct.pack(">H", 0)  # auth_number=0
                    return build_response(command, ACK, data)

                # nozzle_no=0 means default (first) nozzle for this pump
                if nozzle_no == 0:
                    target_nid = nozzle_ids[0]
                elif nozzle_no <= len(nozzle_ids):
                    target_nid = nozzle_ids[nozzle_no - 1]
                else:
                    target_nid = nozzle_ids[0]

                ctrl = self._controllers.get(target_nid)
                if not ctrl:
                    log.warning("BLE 0x28: no controller for nozzle_id=%d", target_nid)
                    data = bytes([pump_no, nozzle_no or 1, 0x00, 0x00])
                    data += struct.pack(">I", 0)
                    data += struct.pack(">H", 0)
                    return build_response(command, ACK, data)

                # ── Determine preset type ────────────────────────────────────
                if preset_type == 1:
                    pt = PresetType.VOLUME
                else:
                    pt = PresetType.AMOUNT  # default to amount

                nozzle_resp = nozzle_no if nozzle_no != 0 else 1
                product_id  = ctrl.cfg.product_code or 1
                price_raw   = int(ctrl.cfg.unit_price * 100)

                # ── Step 1: Clear any pending transaction on this pump ───────
                # The pump may be stuck in PAYABLE state from a previous
                # transaction. Must clear before we can set a new preset.
                status = ctrl.poll_status()
                pump_state = status.state if status else PumpState.UNKNOWN
                log.info("BLE 0x28: NZA 0x%02X current state=%s before preset",
                         ctrl.cfg.nza, pump_state.name if status else 'UNKNOWN')

                if pump_state == PumpState.PAYABLE:
                    log.info("BLE 0x28: NZA 0x%02X in PAYABLE - clearing previous TX first",
                             ctrl.cfg.nza)
                    cs_ok = ctrl.clear_sale()
                    log.info("BLE 0x28: NZA 0x%02X clear_sale: %s",
                             ctrl.cfg.nza, "OK" if cs_ok else "FAILED")
                    if not cs_ok:
                        log.error("BLE 0x28: clear_sale FAILED - cannot preset pump=%d", pump_no)
                        data = bytes([pump_no, nozzle_resp, 0x00, product_id])
                        data += struct.pack(">I", price_raw)
                        data += struct.pack(">H", 0)
                        return build_response(command, ACK, data)
                    # After clear, send pump_start to re-enter remote control mode
                    ps_ok = ctrl.pump_start()
                    log.info("BLE 0x28: NZA 0x%02X pump_start after clear: %s",
                             ctrl.cfg.nza, "OK" if ps_ok else "FAILED")
                elif pump_state == PumpState.STOPPED:
                    log.info("BLE 0x28: NZA 0x%02X in STOPPED - sending pump_start", ctrl.cfg.nza)
                    ctrl.pump_stop()
                    ctrl.pump_start()

                # ── Step 2: Set preset on MPD ────────────────────────────────
                log.info("BLE 0x28: setting preset on NZA 0x%02X: type=%s value=%.2f",
                         ctrl.cfg.nza, pt.name, preset_value)
                preset_ok = ctrl.set_preset(pt, preset_value)
                if not preset_ok:
                    log.error("BLE 0x28: set_preset FAILED for pump=%d nozzle=%d",
                              pump_no, target_nid)
                    data = bytes([pump_no, nozzle_resp, 0x00, product_id])
                    data += struct.pack(">I", price_raw)
                    data += struct.pack(">H", 0)
                    return build_response(command, ACK, data)

                log.info("BLE 0x28: set_preset SUCCESS for pump=%d - MPD is preset", pump_no)

                # ── Step 3: Read start totalizers for transaction tracking ───
                stot_vol = ctrl.read_volume_totalizer()
                stot_amt = ctrl.read_amount_totalizer()
                stot_volume = stot_vol.totalizer if stot_vol else 0.0
                stot_amount = stot_amt.totalizer if stot_amt else 0.0
                log.info("BLE 0x28: NZA 0x%02X STOT_VOL=%.3f STOT_AMT=%.2f",
                         ctrl.cfg.nza, stot_volume, stot_amount)

                # ── Create active transaction so ON_HOOK handler can pick it up
                pump_char = chr(ord('A') + ctrl.cfg.du_id - 1)
                ts = datetime.now().strftime('%Y%m%d%H%M%S')
                txn_id = f"{ts}{pump_char}{target_nid}"
                preset_type_str = 'volume' if pt == PresetType.VOLUME else 'amount'

                self._active_txns[target_nid] = NozzleTransaction(
                    nozzle_id=target_nid,
                    txn_id=txn_id,
                    start_time=datetime.now().isoformat(timespec='seconds'),
                    preset_value=preset_value,
                    preset_type=preset_type_str,
                    stot_volume=stot_volume,
                    stot_amount=stot_amount,
                    authorized=False,  # not authorized yet — nozzle lift will trigger authorize
                )

                # ── Store BLE preset payment details for later use when txn completes
                self._ble_preset_payments[target_nid] = {
                    'mop': mop_28,
                    'payment_type': payment_type,
                    'mobile_number': mobile_28,
                    'vehicle_number': vehicle_28,
                    'vehicle_type': veh_type_28,
                    'payment_trx_id': pay_trx_id,
                    'cash_memo': cash_memo_28,
                    'terminal_id': terminal_28,
                    'txn_reference': txn_ref_28,
                }

                log.info("BLE 0x28: MPD preset done txn=%s preset=%s %.2f "
                         "product=%s price=%.2f — waiting for nozzle lift to authorize",
                         txn_id, preset_type_str, preset_value,
                         ctrl.cfg.product, ctrl.cfg.unit_price)

                # ── Step 4: Build response — only sent AFTER preset succeeds ─
                # This tells PAX that MPD is preset and ready for fueling.
                # error_code=0x01 means preset success.
                auth_number = int(ts[-4:]) & 0xFFFF

                data = bytes([pump_no, nozzle_resp, 0x01, product_id])
                data += struct.pack(">I", price_raw)
                data += struct.pack(">H", auth_number)

                log.info("BLE 0x28 response: pump=%d nozzle=%d error=0x01(preset OK) "
                         "product_id=%d price=%.2f auth=%d",
                         pump_no, nozzle_resp, product_id,
                         ctrl.cfg.unit_price, auth_number)
                return build_response(command, ACK, data)

            elif command == 0x29:
                # ── 0x29: Authorize Extended ─────────────────────────────────
                # EDC sends this after 0x28 (Preset Extended) once the nozzle is
                # lifted, asking FCC to authorize (start) the pump.
                # Request : pump(1), nozzle(1), reserved(5)
                # Response: pump(1), nozzle(1), error_code(1)
                #             0x01 = authorized / success
                #             0x00 = failed
                if not payload or len(payload) < 1:
                    return build_response(command, NACK_SERVICE_OFFLINE)

                pump_no_29   = payload[0]
                nozzle_no_29 = payload[1] if len(payload) > 1 else 0

                log.info("BLE cmd 0x29 Authorize Extended: pump=%d nozzle=%d",
                         pump_no_29, nozzle_no_29)

                nozzle_ids_29 = self._ble_du_map.get(pump_no_29, [])
                if not nozzle_ids_29:
                    log.warning("BLE 0x29: no nozzles mapped for pump=%d", pump_no_29)
                    return build_response(command, ACK,
                                         bytes([pump_no_29, nozzle_no_29, 0x00]))

                # Resolve nozzle: 0 means first nozzle of this pump
                if nozzle_no_29 == 0:
                    target_nid_29 = nozzle_ids_29[0]
                elif nozzle_no_29 <= len(nozzle_ids_29):
                    target_nid_29 = nozzle_ids_29[nozzle_no_29 - 1]
                else:
                    target_nid_29 = nozzle_ids_29[0]

                nozzle_resp_29 = nozzle_ids_29.index(target_nid_29) + 1

                ctrl_29 = self._controllers.get(target_nid_29)
                if not ctrl_29:
                    log.warning("BLE 0x29: no controller for nozzle_id=%d", target_nid_29)
                    return build_response(command, ACK,
                                         bytes([pump_no_29, nozzle_resp_29, 0x00]))

                ble_txn_29 = self._active_txns.get(target_nid_29)

                # If ON_HOOK handler already authorized this nozzle, just ACK
                if ble_txn_29 and ble_txn_29.authorized:
                    log.info("BLE 0x29: pump=%d nozzle_id=%d already authorized (txn=%s)",
                             pump_no_29, target_nid_29, ble_txn_29.txn_id)
                    return build_response(command, ACK,
                                         bytes([pump_no_29, nozzle_resp_29, 0x01]))

                # Check pump state before authorizing
                status_29  = ctrl_29.poll_status()
                pstate_29  = status_29.state if status_29 else PumpState.UNKNOWN
                log.info("BLE 0x29: NZA 0x%02X current state=%s — sending authorize",
                         ctrl_29.cfg.nza,
                         pstate_29.name if status_29 else 'UNKNOWN')

                auth_result_29 = ctrl_29.authorize()

                if auth_result_29 is True:
                    log.info("BLE 0x29: AUTHORIZE SUCCESS pump=%d nozzle_id=%d",
                             pump_no_29, target_nid_29)
                    if ble_txn_29:
                        ble_txn_29.authorized = True
                    return build_response(command, ACK,
                                         bytes([pump_no_29, nozzle_resp_29, 0x01]))
                elif auth_result_29 is False:
                    # Dispenser NAKed authorize — nozzle is likely still on the hook.
                    # DO NOT mark authorized=True here; _handle_off_hook will call
                    # authorize() again once it detects the nozzle is physically lifted.
                    # ACK EDC so it does not keep retrying 0x29.
                    log.warning("BLE 0x29: AUTHORIZE NAK pump=%d — "
                                "nozzle not yet lifted; _handle_off_hook will authorize on lift",
                                pump_no_29)
                    return build_response(command, ACK,
                                         bytes([pump_no_29, nozzle_resp_29, 0x01]))
                else:
                    log.error("BLE 0x29: AUTHORIZE FAILED (no response) pump=%d", pump_no_29)
                    return build_response(command, ACK,
                                         bytes([pump_no_29, nozzle_resp_29, 0x00]))

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


# ── Tatsuno entry point ───────────────────────────────────────────────────────

_PRICE_STATUS_PUB_HOLDER = [None]
import threading as _threading_mod
_MPD_WRITE_LOCK = _threading_mod.Lock()  # serialize MPD-price-write batches
_LATEST_BATCH_ID = [0]  # incremented on every _tatsuno_price_update call

def run_tatsuno(cfg: dict, dry_run: bool = False, config_path: str = 'config.yaml'):
    """
    Tatsuno MPD entry point.

    Replaces the Tokheim FuelAutomation loop when mpd_type == "tatsuno".

    Flow:
      1. Build TatsunoNozzleConfig lists from config.yaml dispensers.
      2. Create shared TatsunoTransport (one serial port, one RS485 bus).
      3. Create one TatsunoController per dispenser SA.
         Each controller owns all nozzles on that SA.
      4. Connect MQTT publisher; register on_transaction callback.
      5. Run power-on handshake on each controller (blocking, retried until OK).
      6. Start background polling on each controller.
      7. Block main thread until SIGINT/SIGTERM.

    The TatsunoController handles everything internally:
      CALL  → Authorization (TDC 78)
      PAYABLE → build TransactionRecord → Clear Sale (TDC 27) → on_transaction()
    """
    if not _TATSUNO_IMPORT_OK:
        log.critical("mpd_type=tatsuno but tatsuno_controller.py could not be imported. "
                     "Check that tatsuno_controller.py and tatsuno_protocol.py are present.")
        sys.exit(1)

    setup_logging(cfg.get('logging', {}))

    log.info("=" * 60)
    log.info("RPi4 Fuel Automation starting - site=%s  [TATSUNO mode]",
             cfg['site']['id'])
    log.info("Version : v%s  build=%d  date=%s  git=%s",
             __version__, __build__, __build_date__, __git_hash__ or "n/a")
    log.info("=" * 60)

    serial_cfg   = cfg['serial']
    protocol_cfg = cfg.get('protocol', {})

    # MPD identifier as known at the site (e.g. "3" for MPD-3).  Used in
    # the CPCL TX topic IOT/CPCL/<ro>/DU_<mpd_no>/TX AND the payload "du_no"
    # field IOCL expects.  Falls back to None → publisher uses the internal
    # du_id (RS-485 comm address) for back-compat.
    try:
        _cfg_mpd_no = int(cfg.get('mpd_no')) if cfg.get('mpd_no') is not None else None
    except (ValueError, TypeError):
        _cfg_mpd_no = None
    if _cfg_mpd_no is not None:
        log.info("Config mpd_no = %d  (used for CPCL TX topic + du_no)", _cfg_mpd_no)

    # ── MQTT publisher ─────────────────────────────────────────────────────────
    publisher = MQTTPublisher(cfg.get('mqtt', {}), cfg)
    publisher.connect()
    publisher.replay_pending()

    # ── Transaction callback factory: one closure per controller (captures du_id) ─
    def make_on_transaction(ctrl_du_id: int):
        def on_transaction(record: TransactionRecord):
            try:
                publisher.publish_transaction(record, du_id=ctrl_du_id)
            except Exception as exc:
                log.error("Tatsuno on_transaction publish error: %s", exc)

            # Also publish to IOCL CPCL per-txn feed (fire-and-forget).
            # Done AFTER the local AWS publish so a CPCL outage never blocks
            # transaction-of-record persistence.
            if _cpcl_tx_pub is not None:
                try:
                    # `mpd_no` from config.yaml is the MPD identifier IOCL
                    # expects in the topic AND payload du_no field (e.g. "3"
                    # for MPD-3).  The internal du_id (7, 8 etc.) is the
                    # RS-485 comm address and still goes into pump_id.
                    # `tx_id` is the MPD's FCC TrxID (chargeslip number).
                    _fcc_id_int = None
                    try:
                        if getattr(record, 'fcc_txn_id', None):
                            _fcc_id_int = int(record.fcc_txn_id)
                    except (ValueError, TypeError):
                        pass

                    _cpcl_tx_pub.publish_txn(
                        du_no         = ctrl_du_id,
                        nozzle_number = record.nozzle_id,
                        unit_price    = record.unit_price,
                        fuel_volume   = record.volume,
                        fuel_amount   = record.amount,
                        vol_tot_start = record.stot_volume,
                        amt_tot_start = record.stot_amount,
                        vol_tot_end   = record.etot_volume,
                        amt_tot_end   = record.etot_amount,
                        fuel_preset   = record.preset_value,
                        pump_id       = ctrl_du_id,
                        mpd_no        = _cfg_mpd_no,
                        tx_id_override = _fcc_id_int,
                    )
                except Exception as exc:
                    log.error("CPCL TX publish error: %s", exc)
        return on_transaction

    # Nozzle-status forwarder: TatsunoController calls this on every state
    # transition + during FUELING. We route it to the existing MQTT publisher
    # so TMS's Pump Status page gets the same feed it had for Tokheim pumps.
    def on_nozzle_status(nozzle_id: int, status: str, product: str = "",
                          volume: float = 0.0, amount: float = 0.0,
                          rate: float = 0.0):
        try:
            publisher.publish_nozzle_status(
                nozzle_id=nozzle_id,
                status=status,
                product=product,
                volume=volume,
                amount=amount,
                rate=rate,
            )
        except Exception as exc:
            log.error("Tatsuno on_nozzle_status publish error: %s", exc)

    # ── Config.yaml unit_price updater ────────────────────────────────────────
    def _persist_prices_to_config(product_price_map: dict):
        """
        Update unit_price in config.yaml for each product_code → price pair.

        Reads and rewrites the file line by line so all comments and formatting
        are preserved.  Only lines inside a nozzle block whose product_code
        matches are touched.

        Args:
            product_price_map: {product_code (int): new_price (float)}
        """
        import re as _re
        try:
            with open(config_path, 'r') as _f:
                lines = _f.readlines()

            in_nozzle_block  = False
            block_prod_code  = None
            updated_count    = 0

            for i, line in enumerate(lines):
                stripped = line.strip()

                # Detect start of a nozzle list entry (- nza: or - nozzle_number:)
                if _re.match(r'^\s*-\s+(nza|nozzle_number)\s*:', line):
                    in_nozzle_block = True
                    block_prod_code = None

                # Capture product_code inside the current nozzle block
                if in_nozzle_block:
                    m = _re.match(r'^(\s*)product_code\s*:\s*(\d+)', line)
                    if m:
                        block_prod_code = int(m.group(2))

                # Update unit_price if block product_code matches
                if in_nozzle_block and block_prod_code in product_price_map:
                    m = _re.match(r'^(\s*)unit_price\s*:\s*[\d.]+', line)
                    if m:
                        new_price = product_price_map[block_prod_code]
                        lines[i] = f"{m.group(1)}unit_price: {new_price}\n"
                        updated_count += 1
                        log.info("config.yaml: product_code=%d unit_price → %.2f",
                                 block_prod_code, new_price)

            with open(config_path, 'w') as _f:
                _f.writelines(lines)

            log.info("config.yaml saved — %d unit_price line(s) updated", updated_count)

        except Exception as exc:
            log.error("Failed to persist prices to config.yaml: %s", exc)

    # ── Price update handler for Tatsuno ──────────────────────────────────────
    def _tatsuno_price_update(updates: list) -> dict:
        # Assign a monotonically-increasing batch ID.  When a later
        # batch arrives, queued older batches will see they're no
        # longer the latest and skip themselves.
        _LATEST_BATCH_ID[0] += 1
        _my_batch_id = _LATEST_BATCH_ID[0]

        """
        Handle TMS price-update received via MQTT.

        Full flow:
          1. Match each update to nozzle(s) by product_code or product alias (e.g. "HSD", "MS")
          2. Update in-memory unit price immediately (used for next TDC 78 auth)
          3. Send TDC 36 to MPD hardware in a background thread — one write per SA
             so price is updated on the physical pump display too

        Args:
            updates: list of dicts from cloud_publisher, each with keys:
                       product_code (int), unit_price (float), alias (str, optional)
        Returns:
            {'applied': [...], 'skipped': [...]} — returned to cloud_publisher for ACK to TMS
        """
        import threading as _threading

        # Detect source: CPCL/IOCL updates carry an 'eff_from' or 'received_at'
        # field (added by the CPCL price subscriber + pending-file logic).
        # TMS website-initiated updates don't have these.
        _is_iocl = any(
            'eff_from' in u or 'received_at' in u for u in (updates or [])
        )

        if _is_iocl:
            log.info("  *** Price Update by IOCL Central Server ***")
            # Echo the exact MQTT response that arrived from IOCL (saved
            # in the pending file when the message first came in).  De-duplicate
            # in case multiple products were in the same response.
            _seen_raw = set()
            for _u in updates:
                _raw = _u.get('raw_response', '')
                if _raw and _raw not in _seen_raw:
                    _seen_raw.add(_raw)
                    log.info("  Exact MQTT response from IOCL: %s", _raw)
        else:
            log.info("  *** Price update by Manual ***")
        log.info("  Updates: %s", updates)

        applied = []
        skipped = []

        # Group nozzle updates per controller (one TDC 36 per SA)
        # ctrl_updates: {TatsunoController: {nozzle_number: new_price}}
        ctrl_updates: dict = {}

        for upd in updates:
            product_code = int(upd.get('product_code') or upd.get('grade_code') or 0)
            new_price    = float(upd.get('unit_price') or upd.get('price') or 0.0)
            alias        = str(upd.get('alias') or '').upper().strip()

            if new_price <= 0:
                log.warning("Tatsuno price update: skipping invalid price %.2f "
                            "for product_code=%d", new_price, product_code)
                skipped.append({'product_code': product_code, 'reason': 'invalid price'})
                continue

            matched = False
            # Matching rule (CRITICAL — fixes "both prices end up at HSD" bug):
            #
            # TMS uses the BPCL FCC product-code convention (1=MS, 4=HSD), but the
            # Tatsuno pump (and our config.yaml) uses the Tatsuno internal codes
            # (4=MS, 5=HSD).  These two numbering systems CONFLICT — TMS's "4=HSD"
            # collides with our config's "4=MS".
            #
            # Therefore: when the TMS payload provides an alias ("MS"/"HSD"), the
            # alias is the SOURCE OF TRUTH and the numeric code MUST BE IGNORED.
            # Only when alias is missing (legacy / hand-crafted payloads) do we
            # fall back to numeric product_code matching.
            for ctrl in controllers:
                for nozzle_num, nc in ctrl._nozzles.items():
                    if alias:
                        # Alias present → exclusive match by name only.
                        if alias != nc.product.upper():
                            continue
                    else:
                        # No alias → fall back to numeric product_code (legacy).
                        if not (product_code > 0 and nc.product_code == product_code):
                            continue

                    if ctrl not in ctrl_updates:
                        ctrl_updates[ctrl] = {}
                    ctrl_updates[ctrl][nozzle_num] = new_price
                    matched = True
                    log.info("  SA 0x%02X nozzle %d [%s]  cfg product_code=%d → Rs %.2f  (matched by %s)",
                             ctrl.sa, nozzle_num, nc.product, nc.product_code, new_price,
                             "alias='%s'" % alias if alias else "product_code=%d" % product_code)

            if matched:
                applied.append({'product_code': product_code, 'unit_price': new_price})
            else:
                log.warning("  No nozzle matched product_code=%d alias='%s' — skipped",
                            product_code, alias)
                skipped.append({'product_code': product_code, 'reason': 'no nozzle matched'})

        if not ctrl_updates:
            log.warning("Tatsuno price update: nothing matched — check product_code or alias in TMS payload")
            return {'applied': applied, 'skipped': skipped}

        # Step 1: update in-memory price immediately (next TDC 78 will use new price)
        for ctrl, nozzle_prices in ctrl_updates.items():
            for nozzle_num, price in nozzle_prices.items():
                ctrl.set_unit_price(nozzle_num, price)

        # Step 2: write to MPD hardware (TDC 36) in background thread
        # One write per SA — write_unit_price_to_mpd() handles TDC 76 confirmation.
        # After ALL SAs confirm, persist new prices to config.yaml so they
        # survive service restarts and TDC 78 authorization always uses correct price.
        def _write_to_mpd():
            # Serialize batches — wait for any prior price-write to
            # fully finish before starting this one.
            with _MPD_WRITE_LOCK:
                # Supersede check — if a newer price-update arrived
                # while we were waiting for the lock, skip this batch.
                if _my_batch_id < _LATEST_BATCH_ID[0]:
                    log.info("MPD batch #%d superseded WHILE QUEUED by #%d — skipping",
                             _my_batch_id, _LATEST_BATCH_ID[0])
                    return
                log.info("MPD batch #%d: starting writes", _my_batch_id)
                all_ok = True
                sa_results = {}     # ctrl → bool (TDC 36 write succeeded?)
                for ctrl, nozzle_prices in ctrl_updates.items():
                    try:
                        log.info("SA 0x%02X: writing price to MPD hardware via TDC 36: %s",
                                 ctrl.sa, nozzle_prices)
                        ok = ctrl.write_unit_price_to_mpd(nozzle_prices)
                        sa_results[ctrl] = ok
                        if ok:
                            log.info("SA 0x%02X: TDC 36 price write confirmed OK", ctrl.sa)
                        else:
                            log.error("SA 0x%02X: TDC 36 price write FAILED "
                                      "(select succeeded but pump rejected — check price range)",
                                      ctrl.sa)
                            all_ok = False
                    except Exception as exc:
                        log.error("SA 0x%02X: TDC 36 price write error: %s", ctrl.sa, exc)
                        sa_results[ctrl] = False
                        all_ok = False

                # ── PRICE/STATUS publish to IOCL (AFTER MPD write completes) ──
                # IOCL only gets the ACK after we've actually written to the pump
                # and got TDC 76 confirmation.  Per-nozzle status reflects the
                # real per-SA result: 'ok' if the SA's write succeeded, else 'fail'.
                try:
                    _ps_pub = _PRICE_STATUS_PUB_HOLDER[0]
                    if _ps_pub is not None and 'CPCLPriceStatusPublisher' in str(type(_ps_pub)):
                        from cpcl_price_status_publisher import build_status_entry
                        pcode_eff = {}
                        for upd in (updates or []):
                            pc = int(upd.get('product_code') or 0)
                            if pc > 0:
                                pcode_eff[pc] = str(upd.get('eff_from') or '')
                        entries = []
                        for ctrl, nozzle_prices in ctrl_updates.items():
                            ok = bool(sa_results.get(ctrl, False))
                            for nz_num, new_price in nozzle_prices.items():
                                nc = ctrl._nozzles.get(nz_num)
                                if not nc:
                                    continue
                                entries.append(build_status_entry(
                                    du_no       = _cfg_mpd_no or 0,
                                    pump_id     = int(ctrl.du_id),
                                    nozzle      = int(nz_num),
                                    product_code= int(nc.product_code),
                                    price       = float(new_price),
                                    eff_from    = pcode_eff.get(int(nc.product_code), ''),
                                    status      = 'ok' if ok else 'fail',
                                ))
                        if entries:
                            _ps_pub.publish_status(entries)
                            log.info("PRICE/STATUS published after MPD write — %d nozzle(s) (ok=%d, fail=%d)",
                                     len(entries),
                                     sum(1 for e in entries if e.get('status')=='ok'),
                                     sum(1 for e in entries if e.get('status')=='fail'))
                except Exception as _ex:
                    log.warning("PRICE/STATUS post-write publish skipped: %s", _ex)

                # Build product_code → price map from all updated nozzles across all SAs
                product_price_map: dict = {}
                for ctrl, nozzle_prices in ctrl_updates.items():
                    for nozzle_num, price in nozzle_prices.items():
                        nc = ctrl._nozzles.get(nozzle_num)
                        if nc:
                            product_price_map[nc.product_code] = price

                # Always persist to config.yaml (even if one SA failed) so in-memory
                # and config stay in sync — operator can correct failed SA separately.
                if product_price_map:
                    _persist_prices_to_config(product_price_map)
                    log.info("Prices persisted to config.yaml: %s", product_price_map)

        _threading.Thread(
            target=_write_to_mpd,
            daemon=True,
            name="tats-price-write",
        ).start()

        log.info("  *** PRICE UPDATE COMPLETE — in-memory updated; TDC 36 dispatched to %d SA(s) ***",
                 len(ctrl_updates))

        # Audit log — append one JSON line per price-update event.
        # `_is_iocl` was computed earlier in this function to distinguish
        # IOCL feed (with eff_from / received_at fields) from TMS UI.
        try:
            from update_history import append_price_history
            _ro_for_audit = (
                _cpcl_tx_cfg.get('ro_code') if isinstance(_cpcl_tx_cfg, dict) else None
            ) or _ro_code_fb or str(cfg.get('site', {}).get('id', ''))
            _serial_for_audit = str(cfg.get('site', {}).get('serial_number', ''))
            # Carry the IOCL receive timestamp through if we have it (otherwise now())
            _recv_at = None
            for _u in (updates or []):
                if _u.get('received_at'):
                    _recv_at = str(_u['received_at'])
                    break
            append_price_history(
                updates,
                source         = "IOCL" if _is_iocl else "TMS_UI",
                ro_code        = _ro_for_audit,
                mpd_no         = _cfg_mpd_no,
                device_serial  = _serial_for_audit,
                controllers    = controllers,
                result_applied = applied,
                result_skipped = skipped,
                received_at    = _recv_at,
            )
        except Exception as _hx:
            log.debug("price history append skipped: %s", _hx)

        # ── Log MPD price write into monthly Price/Density log ──────────
        try:
            _parts   = []
            _details = []
            _total_nz = 0
            _total_sa = set()
            for _u in (updates or []):
                _al = str(_u.get("alias") or _u.get("product") or "").upper().strip()
                _pc = int(_u.get("product_code") or 0)
                _pr = _u.get("unit_price") or _u.get("price")
                if not (_al and _pr is not None):
                    continue
                _matched = 0
                for _ctrl in controllers:
                    _nc_map = getattr(_ctrl, "_nozzles", {}) or {}
                    for _nz_num, _nc in _nc_map.items():
                        _nc_prod = str(getattr(_nc, "product", "")).upper()
                        _nc_pc   = int(getattr(_nc, "product_code", 0))
                        _hit = False
                        _reason = ""
                        if _al and _nc_prod == _al:
                            _hit = True
                            _reason = f"matched by alias='{_al}'"
                        elif _pc > 0 and _nc_pc == _pc:
                            _hit = True
                            _reason = f"matched by product_code={_pc}"
                        if _hit:
                            _details.append(
                                f"  SA 0x{int(getattr(_ctrl, 'sa', 0)):02X} nozzle {int(_nz_num)} [{_nc_prod}]  → Rs {float(_pr):.2f}  ({_reason})"
                            )
                            _matched  += 1
                            _total_nz += 1
                            _total_sa.add(int(getattr(_ctrl, 'sa', 0)))
                if _matched > 0:
                    _parts.append(f"{_al} @ Rs {float(_pr):.2f} ({_matched} nozzle{'s' if _matched!=1 else ''})")
                else:
                    _details.append(f"  No nozzle matched product_code={_pc} alias='{_al}' — skipped")
                    _parts.append(f"{_al} @ Rs {float(_pr):.2f} (skipped — no matching nozzle)")
            _details.append(f"  *** PRICE UPDATE COMPLETE — in-memory updated; TDC 36 dispatched to {len(_total_sa)} SA(s) ***")
            _summary = ", ".join(_parts) if _parts else "(no products)"
            log_mpd_update("Price", _summary,
                           source=("IOCL" if _is_iocl else "TMS_UI"),
                           details=_details)
        except Exception as _exc:
            log.debug("log_mpd_update price failed: %s", _exc)

        return {'applied': applied, 'skipped': skipped}

    publisher.set_price_update_callback(_tatsuno_price_update)

    # ── Tatsuno density update handler ───────────────────────────────────────
    def _tatsuno_density_update(updates: list) -> dict:
        """
        Handle TMS density-update received via MQTT.

        Same flow as _tatsuno_price_update but writes TDC 36 Info Class 171
        (Density Setting Extended) instead of 153.

        Args:
            updates: list of dicts: product_code (int), density (float), rsp_id (int)
        Returns:
            {'applied': [...], 'failed': [...]} — returned to cloud_publisher for ACK to TMS
        """
        import threading as _threading

        log.info("  *** TMS DENSITY UPDATE RECEIVED via MQTT (Tatsuno) ***")
        log.info("  Updates: %s", updates)

        applied = []
        failed  = []

        # ctrl_updates: {TatsunoController: {nozzle_number: new_density}}
        ctrl_updates: dict = {}
        rsp_map:      dict = {}  # product_code -> rsp_id

        for upd in updates:
            product_code = int(upd.get('product_code') or 0)
            new_density  = float(upd.get('density') or 0.0)
            rsp_id       = int(upd.get('rsp_id') or 0)
            alias        = str(upd.get('alias') or '').upper().strip()

            # MPD TDC 36 IC 171 wire format is kg/m³ × 100 (XXXXX.XX).
            # Accept either unit from upstream:
            #   kg/m³   (industry / CPCL / TMS UI)   typical range 600 – 1500
            #   g/mL    (legacy / scientific)        typical range 0.6 – 1.5
            # Anything < 2 is assumed g/mL and converted to kg/m³.
            if 0 < new_density < 2:
                converted = new_density * 1000.0
                log.info("Tatsuno density update: %.5f g/mL → %.2f kg/m³ (product_code=%d alias=%s)",
                         new_density, converted, product_code, alias)
                new_density = converted

            if new_density < 500 or new_density > 1500:
                log.warning("Tatsuno density update: skipping out-of-range density %.2f kg/m³ "
                            "for product_code=%d alias=%s (expected 500–1500)",
                            new_density, product_code, alias)
                failed.append({'product_code': product_code, 'rsp_id': rsp_id,
                               'error': 'density out of range'})
                continue

            rsp_map[product_code] = rsp_id

            matched = False
            for ctrl in controllers:
                for nozzle_num, nc in ctrl._nozzles.items():
                    # When alias is provided (CPCL feed), match by alias only.
                    # Otherwise (TMS feed) match by numeric product_code.
                    if alias:
                        if alias != nc.product.upper():
                            continue
                    elif not (product_code > 0 and nc.product_code == product_code):
                        continue
                    if ctrl not in ctrl_updates:
                        ctrl_updates[ctrl] = {}
                    ctrl_updates[ctrl][nozzle_num] = new_density
                    matched = True
                    log.info("  SA 0x%02X nozzle %d [%s] product_code=%d → %.3f g/mL",
                             ctrl.sa, nozzle_num, nc.product, nc.product_code, new_density)

            if matched:
                applied.append({'product_code': product_code, 'density': new_density,
                                'rsp_id': rsp_id})
            else:
                log.warning("  No nozzle matched product_code=%d — skipped", product_code)
                failed.append({'product_code': product_code, 'rsp_id': rsp_id,
                               'error': 'no nozzle matched'})

        if not ctrl_updates:
            return {'applied': applied, 'failed': failed}

        def _write_density_to_mpd():
            for ctrl, nozzle_densities in ctrl_updates.items():
                try:
                    log.info("SA 0x%02X: writing density to MPD via TDC 36 IC 171: %s",
                             ctrl.sa, nozzle_densities)
                    ok = ctrl.write_unit_density_to_mpd(nozzle_densities)
                    if ok:
                        log.info("SA 0x%02X: TDC 36 density write confirmed OK", ctrl.sa)
                        # Cache the new density on each nozzle's config so the
                        # DU_STATUS heartbeat reflects the live value instead
                        # of the YAML default, and persist to disk for reboot.
                        for nz_num, new_d in nozzle_densities.items():
                            nc = ctrl._nozzles.get(nz_num)
                            if nc:
                                nc.density = float(new_d)
                        _save_live_densities()
                    else:
                        log.error("SA 0x%02X: TDC 36 density write FAILED", ctrl.sa)
                except Exception as exc:
                    log.error("SA 0x%02X: TDC 36 density write error: %s", ctrl.sa, exc)

        _threading.Thread(
            target=_write_density_to_mpd,
            daemon=True,
            name="tats-density-write",
        ).start()

        log.info("  *** DENSITY UPDATE — TDC 36 IC 171 dispatched to %d SA(s) ***",
                 len(ctrl_updates))

        # Audit log — append one JSON line per density-update event.
        # IOCL feed entries carry an 'alias' field but no rsp_id.  TMS UI
        # carries rsp_id (>0) so we use that to distinguish source.
        try:
            from update_history import append_density_history
            any_rsp_id = any(int(u.get('rsp_id') or 0) > 0 for u in (updates or []))
            _ro_for_audit = (
                _cpcl_tx_cfg.get('ro_code') if isinstance(_cpcl_tx_cfg, dict) else None
            ) or _ro_code_fb or str(cfg.get('site', {}).get('id', ''))
            _serial_for_audit = str(cfg.get('site', {}).get('serial_number', ''))
            append_density_history(
                updates,
                source         = "TMS_DENSITY_PAGE" if any_rsp_id else "IOCL",
                ro_code        = _ro_for_audit,
                mpd_no         = _cfg_mpd_no,
                device_serial  = _serial_for_audit,
                controllers    = controllers,
                result_applied = applied,
                result_failed  = failed,
            )
        except Exception as _hx:
            log.debug("density history append skipped: %s", _hx)

        # ── Log MPD density write into monthly Price/Density log ────────
        try:
            _parts   = []
            _details = []
            _total_sa = set()
            for _u in (updates or []):
                _al = str(_u.get("alias") or _u.get("product") or "").upper().strip()
                _pc = int(_u.get("product_code") or 0)
                _de = _u.get("density")
                if not (_al and _de is not None):
                    continue
                _matched = 0
                for _ctrl in controllers:
                    _nc_map = getattr(_ctrl, "_nozzles", {}) or {}
                    for _nz_num, _nc in _nc_map.items():
                        _nc_prod = str(getattr(_nc, "product", "")).upper()
                        _nc_pc   = int(getattr(_nc, "product_code", 0))
                        _hit = False
                        _reason = ""
                        if _al and _nc_prod == _al:
                            _hit = True
                            _reason = f"matched by alias='{_al}'"
                        elif _pc > 0 and _nc_pc == _pc:
                            _hit = True
                            _reason = f"matched by product_code={_pc}"
                        if _hit:
                            _details.append(
                                f"  SA 0x{int(getattr(_ctrl, 'sa', 0)):02X} nozzle {int(_nz_num)} [{_nc_prod}]  → {float(_de):.2f} kg/m3  ({_reason})"
                            )
                            _matched += 1
                            _total_sa.add(int(getattr(_ctrl, 'sa', 0)))
                if _matched > 0:
                    _parts.append(f"{_al} @ {float(_de):.2f} kg/m3 ({_matched} nozzle{'s' if _matched!=1 else ''})")
                else:
                    _details.append(f"  No nozzle matched product_code={_pc} alias='{_al}' — skipped")
                    _parts.append(f"{_al} @ {float(_de):.2f} kg/m3 (skipped — no matching nozzle)")
            _details.append(f"  *** DENSITY UPDATE COMPLETE — TDC 36 IC 171 dispatched to {len(_total_sa)} SA(s) ***")
            _summary = ", ".join(_parts) if _parts else "(no products)"
            _src = "TMS_DENSITY_PAGE" if any(int(u.get("rsp_id") or 0) > 0 for u in (updates or [])) else "IOCL"
            log_mpd_update("Density", _summary, source=_src, details=_details)
        except Exception as _exc:
            log.debug("log_mpd_update density failed: %s", _exc)

        return {'applied': applied, 'failed': failed}

    publisher.set_density_update_callback(_tatsuno_density_update)

    # ── CPCL scheduled price application ─────────────────────────────────────
    # MQTT price arrives → saved to pending file (not applied immediately).
    # Every morning at price_apply_time (default 06:00) the pending prices are
    # applied to the pump hardware (TDC 36) and config.yaml is updated.
    # Manual TMS price changes still apply immediately via _tatsuno_price_update.
    import os as _os
    import json as _json
    from datetime import datetime as _dt, timedelta as _td

    _cpcl_cfg       = cfg.get('cpcl_price', {})
    _apply_time_str = _cpcl_cfg.get('price_apply_time', '06:00')   # "HH:MM"
    _apply_h, _apply_m = (int(x) for x in _apply_time_str.split(':'))

    # Pending-price file lives next to the transaction count file
    _txn_file     = cfg.get('transaction', {}).get(
                        'transaction_count_file',
                        '/var/lib/fuel-automation/txn_count.json')
    _pending_file = _os.path.join(_os.path.dirname(_txn_file), 'cpcl_pending_prices.json')
    # Tracks the price actually applied to the MPD per product, per calendar day.
    # Used to enforce: apply a product's price at most ONCE per day; a later push
    # the same day is applied only if the price differs (otherwise ignored).
    # Resets automatically at 00:00 (the stored 'date' no longer matches today).
    _applied_file = _os.path.join(_os.path.dirname(_txn_file), 'cpcl_applied_today.json')

    def _load_applied_today() -> dict:
        """Return {product_key: 'price_str'} applied today, or {} if new day/missing."""
        today = _dt.now().strftime('%Y-%m-%d')
        try:
            with open(_applied_file, 'r') as _f:
                data = _json.load(_f)
            if data.get('date') == today:
                return dict(data.get('applied', {}))
        except Exception:
            pass
        return {}

    def _save_applied_today(applied: dict):
        """Persist today's applied prices (date-stamped so it self-resets at midnight)."""
        try:
            with open(_applied_file, 'w') as _f:
                _json.dump({'date': _dt.now().strftime('%Y-%m-%d'),
                            'applied': applied}, _f, indent=2)
        except Exception as exc:
            log.warning("CPCL: failed to save applied-today file: %s", exc)

    def _parse_eff_datetime(eff_from: str, eff_time: str):
        """
        Parse IOCL's `eff_from` (date) + `eff_time` (HH:MM) into a datetime.

        Accepts both DD-MM-YYYY and YYYY-MM-DD date formats.  If eff_time is
        missing or empty, defaults to 00:00 (start of that date).  Returns
        None on parse failure → caller treats as "apply immediately".
        """
        if not eff_from:
            return None
        et = (eff_time or "00:00").strip()
        if ':' not in et:
            et = "00:00"
        try:
            for fmt in ("%Y-%m-%d %H:%M", "%d-%m-%Y %H:%M"):
                try:
                    return _dt.strptime(f"{eff_from.strip()} {et}", fmt)
                except ValueError:
                    continue
        except Exception:
            pass
        return None

    def _cpcl_save_pending(updates: list):
        """
        Called when a price message arrives on the CPCL MQTT topic.
        Saves (or merges) the incoming prices into the pending file along
        with their eff_from + eff_time fields.  The scheduler will apply
        each product when its own effective datetime is reached.
        """
        try:
            existing: dict = {}
            if _os.path.exists(_pending_file):
                with open(_pending_file, 'r') as _f:
                    existing = _json.load(_f)

            for upd in updates:
                key = (upd.get('alias') or str(upd.get('product_code', ''))).upper()
                eff_from = upd.get('eff_from', '')
                eff_time = upd.get('eff_time', '')
                eff_dt   = _parse_eff_datetime(eff_from, eff_time)

                # ── Reject stale retained pushes (eff_from < today) ──────────
                # IOCL retains its daily price; on every reconnect/restart the
                # broker re-delivers YESTERDAY's message until today's push lands.
                # If eff_from's DATE is before today, this is a stale replay and
                # MUST NOT be applied (today should wait for today's push).
                if eff_dt is not None and eff_dt.date() < _dt.now().date():
                    log.info("CPCL: %s eff_from=%s is BEFORE today → "
                             "STALE retained push, ignored "
                             "(MQTT still captured)",
                             key, eff_from)
                    continue
                existing[key] = {
                    'alias':        upd.get('alias', ''),
                    'product_code': upd.get('product_code', 0),
                    'unit_price':   upd.get('unit_price', 0.0),
                    'eff_from':     eff_from,
                    'eff_time':     eff_time,
                    'eff_at':       eff_dt.isoformat(timespec='minutes') if eff_dt else '',
                    'raw_response': upd.get('raw_response', ''),
                    'received_at':  _dt.now().isoformat(timespec='seconds'),
                }

            with open(_pending_file, 'w') as _f:
                _json.dump(existing, _f, indent=2)

            summary = {
                k: f"{v['unit_price']} @ {v.get('eff_at') or 'now'}"
                for k, v in existing.items()
            }
            log.info("CPCL price saved to pending: %s", summary)
        except Exception as exc:
            log.error("CPCL: failed to save pending prices: %s", exc)

    def _apply_due_cpcl_prices():
        """
        Read pending file and apply only those products whose effective
        datetime has been reached.  Products with future eff_at stay in
        the file until their time comes.
        """
        if not _os.path.exists(_pending_file):
            return
        try:
            with open(_pending_file, 'r') as _f:
                pending: dict = _json.load(_f)
        except Exception as exc:
            log.error("CPCL scheduler: cannot read pending file: %s", exc)
            return
        if not pending:
            return

        now = _dt.now()
        due, remaining = [], {}
        for key, entry in pending.items():
            eff_dt = _parse_eff_datetime(entry.get('eff_from', ''),
                                         entry.get('eff_time', ''))
            # Defense in depth: if an entry sat in pending across midnight and
            # its eff_from date is now in the past, treat it as stale and DROP
            # it (don't apply, don't keep) — today's push is what matters.
            if eff_dt is not None and eff_dt.date() < now.date():
                log.info("CPCL: %s eff_from=%s is BEFORE today → "
                         "STALE entry dropped from pending",
                         key, entry.get('eff_from', ''))
                continue
            if eff_dt is None or now >= eff_dt:
                # No eff time given → apply right away (back-compat).
                # OR effective time has passed → apply now.
                due.append(entry)
                eff_label = eff_dt.isoformat(timespec='minutes') if eff_dt else 'no-eff-time'
                log.info("CPCL: %s = %.2f  eff %s  → DUE NOW",
                         key, entry.get('unit_price', 0), eff_label)
            else:
                remaining[key] = entry

        if not due:
            return

        # ── Once-per-day / apply-only-if-changed gate ──────────────────────
        # Rule (per IOCL ops): a product's price is written to the MPD at most
        # ONCE per calendar day.  If a second/third push arrives the same day:
        #   • same price  → IGNORED (no MPD write, no PRICE/STATUS)
        #   • diff price  → applied (MPD write + PRICE/STATUS)
        # The raw MQTT message is ALWAYS captured (log_received in the
        # subscriber) regardless of whether we apply or ignore here.
        applied_today = _load_applied_today()
        to_apply = []
        for entry in due:
            key = (entry.get('alias') or str(entry.get('product_code', ''))).upper()
            try:
                new_price = float(entry.get('unit_price', 0) or 0)
            except (TypeError, ValueError):
                new_price = 0.0
            prev = applied_today.get(key)
            if prev is not None:
                try:
                    same = abs(float(prev) - new_price) < 0.005
                except (TypeError, ValueError):
                    same = False
                if same:
                    log.info("CPCL: %s = %.2f already applied today (unchanged) "
                             "→ IGNORED (MPD/PRICE-STATUS skipped; MQTT still captured)",
                             key, new_price)
                    continue
                log.info("CPCL: %s changed today %s → %.2f → applying again",
                         key, prev, new_price)
            else:
                log.info("CPCL: %s = %.2f first apply today → applying",
                         key, new_price)
            to_apply.append(entry)
            applied_today[key] = f"{new_price:.2f}"

        if not to_apply:
            log.info("CPCL scheduler: %d due price(s) all unchanged today — "
                     "nothing written to MPD", len(due))
            # fall through to pending-file cleanup below (due entries removed)
        else:
            log.info("CPCL scheduler: applying %d price(s) (%d due, %d ignored unchanged)",
                     len(to_apply), len(due), len(due) - len(to_apply))
            _tatsuno_price_update(to_apply)
            _save_applied_today(applied_today)

        # Persist only future-eff entries; due ones are removed once applied
        try:
            if remaining:
                with open(_pending_file, 'w') as _f:
                    _json.dump(remaining, _f, indent=2)
            else:
                _os.remove(_pending_file)
        except Exception as exc:
            log.warning("CPCL: failed to update pending file after apply: %s", exc)

    def _cpcl_price_scheduler():
        """
        Background thread — checks every 30 s whether any pending CPCL
        prices have reached their effective datetime, applies due ones.
        No more fixed-06:00 schedule — driven entirely by eff_from + eff_time
        in each IOCL message.
        """
        log.info("CPCL price scheduler started — applies prices when "
                 "their eff_from + eff_time is reached")
        # Apply once at startup in case any pending prices already passed
        # their effective time while the service was down.
        try:
            _apply_due_cpcl_prices()
        except Exception as exc:
            log.error("CPCL scheduler startup apply error: %s", exc)

        while not _stop_event.is_set():
            if _stop_event.wait(timeout=30):
                break
            try:
                _apply_due_cpcl_prices()
            except Exception as exc:
                log.error("CPCL scheduler: apply error: %s", exc, exc_info=True)

    # ── CPCL / IndianOil price-feed subscriber ────────────────────────────────
    # Second MQTT connection (separate from AWS IoT).
    # Subscribes to IOT/CPCL/<site>/PRICE — prices are SAVED to pending file,
    # NOT applied immediately.  The scheduler applies them at price_apply_time.
    _cpcl_sub = None
    if _cpcl_cfg.get('enabled', False):
        try:
            from cpcl_price_subscriber import CPCLPriceSubscriber
            _cpcl_sub = CPCLPriceSubscriber.from_config(_cpcl_cfg, _cpcl_save_pending)
            log.info(
                "CPCL price subscriber configured: broker=%s:%d  topic=%s  "
                "(each product applies at its own eff_from + eff_time)",
                _cpcl_cfg.get('broker', '?'),
                int(_cpcl_cfg.get('port', 8883)),
                _cpcl_cfg.get('topic', '?'),
            )
        except Exception as _exc:
            log.error("CPCL price subscriber init failed: %s — price feed disabled", _exc)

    # ── CPCL / IndianOil master-data subscriber ───────────────────────────────
    # Listens on IOT/CPCL/<ro_code>/DU_MASTER and IOT/CPCL/<ro_code>/TANK_MASTER.
    # IMPORTANT: these feeds belong on the ATG device, not on each MPD Pi.
    # Default is DISABLED on MPDs; only enable by explicit opt-in via:
    #   cpcl_master:
    #     enabled: true
    # in config.yaml.  Leaving the default off prevents fleet-wide duplication.
    _cpcl_master_sub = None
    _cpcl_master_cfg = cfg.get('cpcl_master', {})
    if _cpcl_master_cfg.get('enabled', False):     # default OFF on MPDs
        try:
            from cpcl_master_subscriber import CPCLMasterSubscriber
            # Borrow broker settings from cpcl_price when missing
            for _k in ('broker', 'port', 'username', 'password',
                       'tls', 'tls_insecure', 'reconnect_delay'):
                if _k not in _cpcl_master_cfg and _k in _cpcl_cfg:
                    _cpcl_master_cfg[_k] = _cpcl_cfg[_k]
            if 'ro_code' not in _cpcl_master_cfg:
                _site_id_str_m = str(cfg.get('site', {}).get('id', ''))
                _ro_code_fb_m  = ''.join(ch for ch in _site_id_str_m if ch.isdigit())
                _cpcl_master_cfg['ro_code'] = (
                    _cpcl_cfg.get('ro_code')
                    or cfg.get('cpcl_tx', {}).get('ro_code')
                    or _ro_code_fb_m
                )
            _cpcl_master_sub = CPCLMasterSubscriber.from_config(_cpcl_master_cfg)
            log.info(
                "CPCL master subscriber configured: broker=%s:%d  "
                "DU_MASTER=IOT/CPCL/%s/DU_MASTER  TANK_MASTER=IOT/CPCL/%s/TANK_MASTER",
                _cpcl_master_cfg.get('broker', '?'),
                int(_cpcl_master_cfg.get('port', 8883)),
                _cpcl_master_cfg['ro_code'],
                _cpcl_master_cfg['ro_code'],
            )
        except Exception as _exc:
            log.error("CPCL master subscriber init failed: %s — master feeds disabled", _exc)

    # ── CPCL DENSITY v2 (per-DU per-nozzle) subscriber ───────────────────────
    _cpcl_density_v2_cfg = cfg.get('cpcl_density_v2', {})
    _cpcl_density_v2_sub = None
    if _cpcl_density_v2_cfg.get('enabled', False):
        try:
            from cpcl_density_v2_subscriber import CPCLDensityV2Subscriber

            # Borrow broker creds from cpcl_price if missing
            for _k in ('broker', 'port', 'username', 'password',
                       'tls', 'tls_insecure', 'reconnect_delay'):
                if _k not in _cpcl_density_v2_cfg and _k in _cpcl_cfg:
                    _cpcl_density_v2_cfg[_k] = _cpcl_cfg[_k]

            _ro_for_v2 = (
                _cpcl_master_cfg.get('ro_code') if isinstance(_cpcl_master_cfg, dict) else None
            ) or ''.join(ch for ch in str(cfg.get('site', {}).get('id', '')) if ch.isdigit())
            _du_for_v2 = int(_cfg_mpd_no or 0)

            def _cpcl_density_v2_callback(updates: list) -> dict:
                """Apply per-nozzle density to the MPD.

                `updates` shape: [{nozzle, density, ro_code, du_no, raw_payload}, ...]
                For each nozzle_no we find the matching controller and write
                TDC 36 IC 171.  Then we cache the new density on the nozzle
                config, persist, and log mpd_update.
                """
                applied, failed = [], []
                # Group nozzle → density per controller (SA)
                by_ctrl = {}
                for u in updates:
                    nz = int(u['nozzle'])
                    de = float(u['density'])
                    for _ctrl in controllers:
                        if nz in (getattr(_ctrl, '_nozzles', {}) or {}):
                            by_ctrl.setdefault(_ctrl, {})[nz] = de
                            break
                    else:
                        failed.append({'nozzle': nz, 'reason': 'no SA owns this nozzle'})

                for _ctrl, nz_map in by_ctrl.items():
                    try:
                        log.info("SA 0x%02X: writing density to MPD via TDC 36 IC 171 (DENSITY-v2): %s",
                                 _ctrl.sa, nz_map)
                        ok = _ctrl.write_unit_density_to_mpd(nz_map)
                        if ok:
                            log.info("SA 0x%02X: TDC 36 density write confirmed OK", _ctrl.sa)
                            for _nz, _de in nz_map.items():
                                _nc = _ctrl._nozzles.get(_nz)
                                if _nc:
                                    _nc.density = float(_de)
                                applied.append({'sa': _ctrl.sa, 'nozzle': _nz, 'density': _de})
                            try: _save_live_densities()
                            except Exception: pass
                        else:
                            for _nz in nz_map:
                                failed.append({'sa': _ctrl.sa, 'nozzle': _nz, 'reason': 'write FAILED'})
                            log.error("SA 0x%02X: TDC 36 density write FAILED (DENSITY-v2)", _ctrl.sa)
                    except Exception as _exc:
                        for _nz in nz_map:
                            failed.append({'sa': _ctrl.sa, 'nozzle': _nz, 'reason': str(_exc)})
                        log.error("SA 0x%02X: density write error: %s", _ctrl.sa, _exc)

                # Monthly Price/Density log entry
                try:
                    from mqtt_log import log_mpd_update
                    _parts = [f"nz{a['nozzle']} @ {a['density']:.2f} kg/m3" for a in applied]
                    _summary = (", ".join(_parts)) + f" ({len(applied)} nozzle(s) applied"
                    if failed:
                        _summary += f", {len(failed)} failed"
                    _summary += ")"
                    log_mpd_update("Density", _summary, source="IOCL")
                except Exception as _exc:
                    log.debug("log_mpd_update density v2 failed: %s", _exc)

                return {'applied': applied, 'failed': failed}

            _cpcl_density_v2_sub = CPCLDensityV2Subscriber.from_config(
                _cpcl_density_v2_cfg,
                ro_code=_ro_for_v2,
                du_no=_du_for_v2,
                callback=_cpcl_density_v2_callback,
            )
            log.info("CPCL DENSITY-v2 subscriber configured: broker=%s:%d  topic=IOT/CPCL/%s/DU_%d/DENSITY",
                     _cpcl_density_v2_cfg.get('broker', '?'),
                     int(_cpcl_density_v2_cfg.get('port', 8883)),
                     _ro_for_v2, _du_for_v2)
        except Exception as _exc:
            log.error("CPCL DENSITY-v2 init failed: %s — feed disabled", _exc)


    # ── CPCL / IndianOil density-feed subscriber ──────────────────────────────
    # Subscribes to IOT/CPCL/<site>/DENSITY.  CPCL densities are NOT applied
    # immediately — they are saved to a pending file and applied at
    # density_apply_time (default 06:00) the next morning, mirroring the
    # CPCL price flow.  The instant-change path from the TMS website still
    # applies immediately (different topic: fuel-automation/density_update/...).
    _cpcl_density_cfg     = cfg.get('cpcl_density', {})
    _density_apply_time_s = _cpcl_density_cfg.get('density_apply_time',
                                                  _apply_time_str)   # default = same as price
    _density_apply_h, _density_apply_m = (int(x) for x in _density_apply_time_s.split(':'))
    _density_pending_file = _os.path.join(
        _os.path.dirname(_txn_file), 'cpcl_pending_densities.json'
    )

    # ── Live per-nozzle density cache ────────────────────────────────────────
    # Tracks the density most recently written to each MPD/nozzle so the
    # DU_STATUS heartbeat reports the real value (not the config default).
    # Persisted to disk so it survives a service / Pi restart.
    _live_density_file = _os.path.join(
        _os.path.dirname(_txn_file), 'nozzle_densities_live.json'
    )

    def _save_live_densities():
        """Snapshot every controller's nc.density into the live-density file."""
        try:
            snap = {}
            for ctrl in controllers:
                sa_key = f"0x{ctrl.sa:02X}"
                snap[sa_key] = {
                    str(nz_num): float(nc.density or 0.0)
                    for nz_num, nc in ctrl._nozzles.items()
                    if getattr(nc, 'density', 0.0)
                }
            tmp = _live_density_file + '.tmp'
            with open(tmp, 'w') as f:
                _json.dump(snap, f, indent=2, sort_keys=True)
                f.write('\n')
            _os.replace(tmp, _live_density_file)
        except Exception as exc:
            log.warning("could not persist live densities: %s", exc)

    def _load_live_densities():
        """Restore live densities from disk into nc.density at startup."""
        if not _os.path.exists(_live_density_file):
            return
        try:
            with open(_live_density_file) as f:
                snap = _json.load(f) or {}
            applied = 0
            for ctrl in controllers:
                sa_key = f"0x{ctrl.sa:02X}"
                per_n = snap.get(sa_key) or {}
                for nz_num_str, d in per_n.items():
                    try:
                        nz_num = int(nz_num_str)
                        nc = ctrl._nozzles.get(nz_num)
                        if nc and d:
                            nc.density = float(d)
                            applied += 1
                    except Exception:
                        continue
            if applied:
                log.info("Restored %d live density value(s) from %s",
                         applied, _live_density_file)
        except Exception as exc:
            log.warning("could not load live densities: %s", exc)

    def _cpcl_save_pending_density(updates: list):
        """
        Called when a density message arrives on the CPCL MQTT topic.
        Saves (or merges) the incoming densities into the pending file.
        Does NOT touch pump hardware — densities are applied at density_apply_time.
        """
        try:
            existing: dict = {}
            if _os.path.exists(_density_pending_file):
                with open(_density_pending_file, 'r') as _f:
                    existing = _json.load(_f)

            for upd in updates:
                key = (upd.get('alias') or str(upd.get('product_code', ''))).upper()
                existing[key] = {
                    'alias':        upd.get('alias', ''),
                    'product_code': upd.get('product_code', 0),
                    'density':      upd.get('density', 0.0),
                    'rsp_id':       upd.get('rsp_id', 0),
                    'received_at':  _dt.now().isoformat(timespec='seconds'),
                }

            with open(_density_pending_file, 'w') as _f:
                _json.dump(existing, _f, indent=2)

            log.info(
                "CPCL density saved to pending (will apply at %s): %s",
                _density_apply_time_s,
                {k: v['density'] for k, v in existing.items()},
            )
        except Exception as exc:
            log.error("CPCL: failed to save pending densities: %s", exc)

    def _apply_pending_cpcl_densities():
        """Read pending file and apply all saved densities to pump hardware."""
        if not _os.path.exists(_density_pending_file):
            log.info("CPCL density scheduler: no pending file — nothing to apply")
            return
        try:
            with open(_density_pending_file, 'r') as _f:
                pending: dict = _json.load(_f)
        except Exception as exc:
            log.error("CPCL density scheduler: cannot read pending file: %s", exc)
            return

        if not pending:
            log.info("CPCL density scheduler: pending file is empty — nothing to apply")
            return

        updates = list(pending.values())
        log.info("CPCL density scheduler (%s): applying %d pending density(ies): %s",
                 _density_apply_time_s,
                 len(updates),
                 {v.get('alias') or v.get('product_code'): v['density'] for v in updates})
        _tatsuno_density_update(updates)

        # Clear the pending file after a successful application so we don't
        # re-apply the same values tomorrow.
        try:
            _os.remove(_density_pending_file)
        except Exception:
            pass

    def _cpcl_density_scheduler():
        """Background thread — wakes at density_apply_time each day and applies pending densities."""
        log.info("CPCL density scheduler started — will apply pending densities daily at %s",
                 _density_apply_time_s)
        while not _stop_event.is_set():
            now        = _dt.now()
            next_apply = now.replace(hour=_density_apply_h, minute=_density_apply_m,
                                     second=0, microsecond=0)
            if now >= next_apply:
                next_apply += _td(days=1)
            wait_secs = (next_apply - now).total_seconds()
            log.info("CPCL density scheduler: next apply at %s (in %.0f s / %.1f h)",
                     next_apply.strftime('%Y-%m-%d %H:%M'), wait_secs, wait_secs / 3600)
            if _stop_event.wait(timeout=wait_secs):
                break
            log.info("CPCL density scheduler: %s — applying pending IndianOil densities",
                     _dt.now().strftime('%H:%M'))
            try:
                _apply_pending_cpcl_densities()
            except Exception as exc:
                log.error("CPCL density scheduler: apply error: %s", exc, exc_info=True)

    _cpcl_density_sub = None
    if _cpcl_density_cfg.get('enabled', False):
        try:
            from cpcl_density_subscriber import CPCLDensitySubscriber

            # ── IMMEDIATE apply mode (default) ────────────────────────────
            # Old behaviour saved IOCL densities to a pending file and
            # applied them at 06:00 the next morning.  We now apply them
            # the moment they arrive — same path TMS UI uses, so the MPD
            # display + nozzle_densities_live.json reflect IOCL values
            # straight away.  Set `cpcl_density.immediate_apply: false`
            # in config.yaml to fall back to the scheduled flow.
            _immediate = bool(_cpcl_density_cfg.get('immediate_apply', True))

            def _cpcl_density_callback_immediate(updates: list) -> dict:
                log.info("CPCL DENSITY (IMMEDIATE) — %d update(s) → "
                         "calling _tatsuno_density_update", len(updates))
                result = _tatsuno_density_update(updates)
                # In immediate-apply mode the pending file should NOT exist.
                # If a stale file is left over from the old scheduled flow,
                # remove it now so /var/lib/fuel-automation stays clean.
                try:
                    if _os.path.exists(_density_pending_file):
                        _os.remove(_density_pending_file)
                        log.info("CPCL DENSITY: removed stale pending file %s",
                                 _density_pending_file)
                except Exception as _exc:
                    log.warning("CPCL DENSITY: could not remove pending file: %s", _exc)
                return result

            _cpcl_density_callback = (
                _cpcl_density_callback_immediate if _immediate
                else _cpcl_save_pending_density
            )

            _cpcl_density_sub = CPCLDensitySubscriber.from_config(
                _cpcl_density_cfg, _cpcl_density_callback,
            )
            log.info(
                "CPCL density subscriber configured: broker=%s:%d  topic=%s  mode=%s",
                _cpcl_density_cfg.get('broker', '?'),
                int(_cpcl_density_cfg.get('port', 8888)),
                _cpcl_density_cfg.get('topic', '?'),
                "IMMEDIATE" if _immediate else f"scheduled at {_density_apply_time_s}",
            )
        except Exception as _exc:
            log.error("CPCL density subscriber init failed: %s — density feed disabled", _exc)

    # ── CPCL / IndianOil per-transaction PUBLISHER ────────────────────────────
    # Publishes every successful txn to IOT/CPCL/<ro_code>/DU_<du_no>/TX so
    # IOCL's upstream sees real-time fuel sales. Topic+payload spec from IOCL.
    _cpcl_tx_cfg = cfg.get('cpcl_tx', {})
    _cpcl_tx_pub = None
    if _cpcl_tx_cfg.get('enabled', False) or _cpcl_tx_cfg.get('broker'):
        try:
            from cpcl_tx_publisher import CPCLTxPublisher
            # If cpcl_tx block is missing fields, borrow from cpcl_price
            for k in ('broker', 'port', 'username', 'password',
                      'tls', 'tls_insecure', 'reconnect_delay'):
                if k not in _cpcl_tx_cfg and k in _cpcl_cfg:
                    _cpcl_tx_cfg[k] = _cpcl_cfg[k]
            # ro_code defaults to numeric part of site.id (e.g. "CPCL_392203" → "392203")
            _site_id_str = str(cfg.get('site', {}).get('id', ''))
            _ro_code_fb = ''.join(ch for ch in _site_id_str if ch.isdigit())
            _cpcl_tx_pub = CPCLTxPublisher.from_config(
                _cpcl_tx_cfg, ro_code_fallback=_ro_code_fb,
                mpd_no=_cfg_mpd_no,   # for the ACK subscription topic
            )
            log.info(
                "CPCL TX publisher configured: broker=%s:%d  ro_code=%s",
                _cpcl_tx_cfg.get('broker', '?'),
                int(_cpcl_tx_cfg.get('port', 8883)),
                _cpcl_tx_cfg.get('ro_code') or _ro_code_fb,
            )
        except Exception as _exc:
            log.error("CPCL TX publisher init failed: %s — IOCL txn feed disabled", _exc)

    # ── Tank-alert subscriber (ATG → MPD interlock) ───────────────────────────
    # ATG (e.g. 192.168.2.101) publishes {"product":"MS","action":"block"} to
    # fuel-automation/tank_alert/<ro_code> when a tank is empty/low.  This
    # subscriber maps product → nozzles via config.yaml and writes them into
    # nozzle_locks.json — the running service then refuses TDC 78 authorize
    # for those nozzles (existing interlock mechanism).
    #
    # Config block (optional — falls back to AWS IoT cpcl_tx settings or
    # cpcl_price settings if missing):
    #   tank_alert:
    #     enabled: true
    #     broker: "<aws-iot-endpoint>"     # or upkaran.indianoil.co.in
    #     port: 8883
    #     ro_code: "392203"                # auto-derived from site.id if missing
    _tank_alert_cfg = cfg.get('tank_alert', {})
    _tank_alert_sub = None
    if _tank_alert_cfg.get('enabled', False):
        try:
            from tank_alert_subscriber import TankAlertSubscriber
            # Borrow broker settings from cpcl_tx if not specified
            for _k in ('broker', 'port', 'username', 'password',
                       'tls', 'tls_insecure', 'reconnect_delay'):
                if _k not in _tank_alert_cfg and _k in _cpcl_tx_cfg:
                    _tank_alert_cfg[_k] = _cpcl_tx_cfg[_k]
                elif _k not in _tank_alert_cfg and _k in _cpcl_cfg:
                    _tank_alert_cfg[_k] = _cpcl_cfg[_k]

            _ta_ro_code = (_tank_alert_cfg.get('ro_code')
                            or _cpcl_tx_cfg.get('ro_code')
                            or _ro_code_fb)
            _ta_serial  = str(cfg.get('site', {}).get('serial_number', ''))
            _ta_ack_topic = (f"fuel-automation/tank_alert_ack/{_ta_ro_code}/{_ta_serial}"
                              if _ta_serial else None)

            _tank_alert_sub = TankAlertSubscriber(
                broker         = _tank_alert_cfg.get('broker', ''),
                port           = int(_tank_alert_cfg.get('port', 8883)),
                ro_code        = _ta_ro_code,
                dispensers_cfg = cfg.get('dispensers', []) or [],
                mpd_no         = _cfg_mpd_no,    # from top-level config — lets ATG target this MPD
                client_id      = f"tank_alert_sub_{_ta_serial or _ta_ro_code}",
                username       = _tank_alert_cfg.get('username', ''),
                password       = _tank_alert_cfg.get('password', ''),
                tls            = bool(_tank_alert_cfg.get('tls', True)),
                tls_insecure   = bool(_tank_alert_cfg.get('tls_insecure', True)),
                ack_topic      = _ta_ack_topic,
                reconnect_delay= int(_tank_alert_cfg.get('reconnect_delay', 15)),
            )
            log.info(
                "Tank-alert subscriber configured: broker=%s:%d  topic=fuel-automation/tank_alert/%s  ack_topic=%s",
                _tank_alert_cfg.get('broker', '?'),
                int(_tank_alert_cfg.get('port', 8883)),
                _ta_ro_code,
                _ta_ack_topic or '(none)',
            )
        except Exception as _exc:
            log.error("Tank-alert subscriber init failed: %s — ATG interlock disabled", _exc)

    # ── Build per-SA controller list ───────────────────────────────────────────
    controllers: List[TatsunoController] = []
    transport: Optional[TatsunoTransport] = None

    if dry_run:
        log.info("[DRY-RUN] Tatsuno mode: no serial port opened")
    else:
        transport = TatsunoTransport(
            port    = serial_cfg['port'],
            timeout = serial_cfg.get('timeout', 0.5),
            gap     = serial_cfg.get('inter_command_gap', 0.1),
        )
        transport.connect()

    for du in cfg.get('dispensers', []):
        sa     = du.get('sa')
        du_id  = int(du['du_id'])   # cast to int: YAML parses "09" as string (invalid octal)
        label  = du.get('label', f'DU-{du_id}')

        if sa is None:
            log.warning("Dispenser du_id=%d has no 'sa' field — skipping (tatsuno requires sa)", du_id)
            continue

        nozzle_cfgs: List[TatsunoNozzleConfig] = []
        for nozzle in du.get('nozzles', []):
            if not nozzle.get('enabled', True):
                continue
            product_code   = nozzle.get('product_code', 1)
            explicit_prod  = nozzle.get('product', '').strip()
            product        = explicit_prod if explicit_prod else get_product_alias(product_code)
            raw_auth_slot = nozzle.get('auth_slot')
            auth_slot     = int(raw_auth_slot) if raw_auth_slot is not None else None

            nozzle_cfgs.append(TatsunoNozzleConfig(
                nozzle_number = nozzle['nozzle_number'],
                nozzle_id     = nozzle['nozzle_id'],
                product       = product,
                product_code  = product_code,
                unit_price    = float(nozzle.get('unit_price', 0.0)),
                enabled       = True,
                preset_type   = nozzle.get('preset_type', 'amount'),
                preset_value  = float(nozzle.get('preset_value', 0.0)),
                auth_slot     = auth_slot,
            ))
            slot_info = f" auth_slot={auth_slot}" if auth_slot else ""
            log.info("Registered Tatsuno nozzle: du_id=%d sa=0x%02X nozzle_number=%d "
                     "nozzle_id=%d product=%s preset=%s=%.2f%s",
                     du_id, sa, nozzle['nozzle_number'], nozzle['nozzle_id'], product,
                     nozzle.get('preset_type', 'amount'), float(nozzle.get('preset_value', 0.0)),
                     slot_info)

        if not nozzle_cfgs:
            log.warning("Dispenser du_id=%d sa=0x%02X has no enabled nozzles — skipping", du_id, sa)
            continue

        ctrl = TatsunoController(
            sa             = sa,
            du_id          = du_id,
            nozzle_configs = nozzle_cfgs,
            transport      = transport,
            protocol_cfg   = protocol_cfg,
            on_transaction   = make_on_transaction(du_id),
            on_nozzle_status = on_nozzle_status,
        )
        controllers.append(ctrl)
        log.info("Created TatsunoController: %s  sa=0x%02X  nozzles=%d",
                 label, sa, len(nozzle_cfgs))

    if not controllers:
        log.critical("No Tatsuno controllers created — check config.yaml dispensers. Exiting.")
        sys.exit(1)

    # ── Shutdown handling ──────────────────────────────────────────────────────
    _stop_event = threading.Event()

    # ── Poll-status writer ────────────────────────────────────────────────────
    # Every 5 seconds, write each SA's last-poll-response timestamp to
    # /var/lib/fuel-automation/poll_status.json.  Used by check_nozzles.sh and
    # any external monitoring to know which nozzles are in AUTOMATION (pump
    # responding) vs MANUAL (silent for > online_threshold_sec).
    def _poll_status_writer():
        import os as _os, json as _json, time as _t
        from datetime import datetime as _dt
        STATUS_FILE = "/var/lib/fuel-automation/poll_status.json"
        THRESHOLD = 180   # seconds — match cpcl_du_status_publisher.is_sa_online() window
        while not _stop_event.is_set():
            try:
                now_ts = _t.time()
                now_str = _dt.now().strftime("%Y-%m-%d %H:%M:%S")
                sas = {}
                for ctrl in controllers:
                    last = float(getattr(ctrl, '_last_response_at', 0.0) or 0.0)
                    if last > 0:
                        age = int(now_ts - last)
                        last_str = _dt.fromtimestamp(last).strftime("%Y-%m-%d %H:%M:%S")
                    else:
                        age = -1
                        last_str = "never"
                    sas[f"0x{ctrl.sa:02X}"] = {
                        "last_response_at": last_str,
                        "age_sec":          age,
                        "online":           (last > 0 and age < THRESHOLD),
                        "du_id":            int(getattr(ctrl, 'du_id', 0)),
                        "nozzles":          sorted(list(getattr(ctrl, '_nozzles', {}).keys())),
                    }
                data = {
                    "updated_at":           now_str,
                    "online_threshold_sec": THRESHOLD,
                    "sas":                  sas,
                }
                tmp = STATUS_FILE + ".tmp"
                with open(tmp, "w") as _f:
                    _json.dump(data, _f, indent=2)
                _os.replace(tmp, STATUS_FILE)
            except Exception as _exc:
                log.warning("poll_status writer error: %s", _exc)
            if _stop_event.wait(5):
                break

    threading.Thread(
        target=_poll_status_writer,
        name="poll-status-writer",
        daemon=True,
    ).start()
    log.info("Poll status writer started — /var/lib/fuel-automation/poll_status.json (5s tick)")

    def _tatsuno_shutdown(signum, frame):
        log.info("Shutdown signal %d received - stopping Tatsuno controllers ...", signum)
        _stop_event.set()
        for c in controllers:
            c.stop_polling()
        if _cpcl_sub:
            _cpcl_sub.stop()
        if _cpcl_density_sub:
            _cpcl_density_sub.stop()
        if _cpcl_tx_pub:
            _cpcl_tx_pub.stop()
        if transport:
            transport.disconnect()
        publisher.disconnect()
        sys.exit(0)

    signal.signal(signal.SIGINT,  _tatsuno_shutdown)
    signal.signal(signal.SIGTERM, _tatsuno_shutdown)

    # ── Power-on handshake per controller ─────────────────────────────────────
    if not dry_run:
        for ctrl in controllers:
            log.info("Running power-on handshake for DU-%d (SA=0x%02X) ...", ctrl.du_id, ctrl.sa)
            ok = ctrl.power_on_handshake()
            if ok:
                log.info("Power-on handshake OK for DU-%d (SA=0x%02X)", ctrl.du_id, ctrl.sa)
            else:
                log.warning("Power-on handshake FAILED for DU-%d (SA=0x%02X) — continuing anyway "
                            "(dispenser may not require it or already initialised)",
                            ctrl.du_id, ctrl.sa)

    # ── Start background polling on each controller ────────────────────────────
    for ctrl in controllers:
        ctrl.start_polling()
        log.info("Polling started for DU-%d (SA=0x%02X)", ctrl.du_id, ctrl.sa)

    # Restore per-nozzle live densities written previously (so the DU_STATUS
    # heartbeat starts with the real values rather than the YAML defaults).
    _load_live_densities()

    log.info("Tatsuno automation running — %d controller(s) active. "
             "Press Ctrl+C to stop.", len(controllers))

    # ── Start CPCL price subscriber + daily price scheduler ───────────────────
    if _cpcl_sub:
        _cpcl_sub.start()
        log.info("CPCL price subscriber started — prices applied per-product when "
                 "eff_from + eff_time reached (scheduler checks every 30 s)")

    if _cpcl_density_sub:
        _cpcl_density_sub.start()
        log.info("CPCL density subscriber started — densities saved to pending, applied daily at %s",
                 _density_apply_time_s)

    if _cpcl_tx_pub:
        _cpcl_tx_pub.start()
        log.info("CPCL TX publisher started — every txn will be published to IOCL CPCL feed")

    if _cpcl_master_sub:
        _cpcl_master_sub.start()
        log.info("CPCL master subscriber started — listening on DU_MASTER + TANK_MASTER")
    if _cpcl_density_v2_sub:
        _cpcl_density_v2_sub.start()
        log.info("CPCL DENSITY-v2 subscriber started — per-nozzle density applied immediately on receipt")

    # ── PRICE/STATUS publisher (IOCL feedback after MPD price write) ──────
    _price_status_cfg = cfg.get('cpcl_price_status', {})
    _price_status_pub = None
    if (_price_status_cfg.get('enabled', _cpcl_tx_cfg.get('enabled', False))):
        try:
            from cpcl_price_status_publisher import (
                CPCLPriceStatusPublisher, build_status_entry,
            )
            for _k in ('broker', 'port', 'username', 'password',
                       'tls', 'tls_insecure', 'reconnect_delay'):
                if _k not in _price_status_cfg and _k in _cpcl_tx_cfg:
                    _price_status_cfg[_k] = _cpcl_tx_cfg[_k]
                elif _k not in _price_status_cfg and _k in _cpcl_cfg:
                    _price_status_cfg[_k] = _cpcl_cfg[_k]
            _price_status_cfg.setdefault('ro_code',
                                          _cpcl_tx_cfg.get('ro_code') or _ro_code_fb)
            _price_status_pub = CPCLPriceStatusPublisher.from_config(_price_status_cfg, du_no=int(_cfg_mpd_no or 0))
            _PRICE_STATUS_PUB_HOLDER[0] = _price_status_pub
            _price_status_pub.start()
            log.info("CPCL PRICE/STATUS publisher started — topic=IOT/CPCL/%s/DU_%d/PRICE/STATUS",
                     _price_status_cfg['ro_code'], int(_cfg_mpd_no or 0))

            # ── PERIODIC TEST-PUBLISH LOOP REMOVED ────────────────────────
            # This used to re-publish the current price to PRICE/STATUS every
            # ~120s for IOCL format verification.  It made it look like the
            # price was being pushed repeatedly even when nothing changed.
            #
            # PRICE/STATUS is EVENT-DRIVEN ONLY: it is published exactly once
            # per real price write, from _tatsuno_price_update() AFTER the
            # TDC 36 write to the MPD completes (see _PRICE_STATUS_PUB_HOLDER
            # usage above).  No periodic/heartbeat publishing.
            #
            # If you ever need a one-off format test, call
            # _price_status_pub.test_publish() manually — but never on a timer.
            if _price_status_cfg.get('test_publish_on_start', False):
                log.warning("PRICE/STATUS: test_publish_on_start is set — sending "
                            "ONE test payload (no periodic loop)")
                try:
                    _price_status_pub.test_publish()
                except Exception as _ex:
                    log.warning("PRICE/STATUS one-off test publish error: %s", _ex)
        except Exception as _exc:
            log.error("CPCL PRICE/STATUS publisher init failed: %s",
                      _exc, exc_info=True)

    # ── DEVICE_PARAMS periodic publisher (every 5 minutes) ────────────────
    # Defaults to DRY-RUN (log-only) on first deploy so we can inspect
    # values before going live with IOCL.  Flip dry_run: false in config
    # when ready to publish.
    _dev_params_cfg = cfg.get('cpcl_device_params', {})
    if (_dev_params_cfg.get('enabled', _cpcl_tx_cfg.get('enabled', False))
            and _cfg_mpd_no is not None):
        try:
            from cpcl_device_params_publisher import CPCLDeviceParamsPublisher
            for _k in ('broker', 'port', 'username', 'password',
                       'tls', 'tls_insecure', 'reconnect_delay'):
                if _k not in _dev_params_cfg and _k in _cpcl_tx_cfg:
                    _dev_params_cfg[_k] = _cpcl_tx_cfg[_k]
                elif _k not in _dev_params_cfg and _k in _cpcl_cfg:
                    _dev_params_cfg[_k] = _cpcl_cfg[_k]
            _dev_params_cfg.setdefault('ro_code',
                                       _cpcl_tx_cfg.get('ro_code') or _ro_code_fb)
            # Default: dry_run=true so payloads are LOGGED but not published.
            _dev_params_cfg.setdefault('dry_run', True)
            _dev_params_pub = CPCLDeviceParamsPublisher.from_config(
                _dev_params_cfg, mpd_no=_cfg_mpd_no,
            )
            # Mirror every snapshot to TMS via the AWS IoT cloud publisher.
            if hasattr(publisher, 'publish_device_params'):
                _dev_params_pub.set_tms_callback(publisher.publish_device_params)
                log.info("DEVICE_PARAMS: TMS fan-out bridged via cloud_publisher")
            _dev_params_pub.start()
            log.info("CPCL DEVICE_PARAMS publisher started — interval=%ds  "
                     "dry_run=%s  topic=IOT/CPCL/%s/DU_%d/DEVICE_PARAMS",
                     int(_dev_params_cfg.get('publish_interval_sec', 300)),
                     bool(_dev_params_cfg.get('dry_run', True)),
                     _dev_params_cfg['ro_code'], _cfg_mpd_no)
        except Exception as _exc:
            log.error("CPCL DEVICE_PARAMS publisher init failed: %s",
                      _exc, exc_info=True)

    # ── DU_STATUS periodic publisher (every 5 minutes) ────────────────────
    # Pushes a per-nozzle snapshot to IOT/CPCL/<ro>/DU_<mpd>/DU_STATUS so
    # IOCL central has live state without needing to poll.
    _du_status_cfg = cfg.get('cpcl_du_status', {})
    if (_du_status_cfg.get('enabled', _cpcl_tx_cfg.get('enabled', False))
            and _cfg_mpd_no is not None):
        try:
            from cpcl_du_status_publisher import CPCLDuStatusPublisher
            # Borrow broker settings from cpcl_tx
            for _k in ('broker', 'port', 'username', 'password',
                       'tls', 'tls_insecure', 'reconnect_delay'):
                if _k not in _du_status_cfg and _k in _cpcl_tx_cfg:
                    _du_status_cfg[_k] = _cpcl_tx_cfg[_k]
                elif _k not in _du_status_cfg and _k in _cpcl_cfg:
                    _du_status_cfg[_k] = _cpcl_cfg[_k]
            _du_status_cfg.setdefault('ro_code',
                                       _cpcl_tx_cfg.get('ro_code') or _ro_code_fb)
            _du_status_pub = CPCLDuStatusPublisher.from_config(
                _du_status_cfg,
                mpd_no=_cfg_mpd_no,
                controllers_provider=lambda: controllers,
            )
            _du_status_pub.start()
            # Event-driven publish: when any nozzle's status changes
            # (idle→authorized→fueling→complete) publish DU_STATUS immediately,
            # in addition to the periodic 5-min heartbeat.
            def _on_nz_status_change(ctrl, nz, val, _p=_du_status_pub):
                try:
                    _p.publish_now(reason=f"SA 0x{ctrl.sa:02X} nz{nz}={val}")
                except Exception:
                    pass
            for _c in controllers:
                _c.on_du_status_change = _on_nz_status_change
            log.info("CPCL DU_STATUS publisher started — periodic every %ds + "
                     "event-driven on nozzle status change → IOT/CPCL/%s/DU_%d/DU_STATUS",
                     int(_du_status_cfg.get('publish_interval_sec', 300)),
                     _du_status_cfg['ro_code'], _cfg_mpd_no)
        except Exception as _exc:
            log.error("CPCL DU_STATUS publisher init failed: %s", _exc, exc_info=True)

    if _tank_alert_sub:
        _tank_alert_sub.start()
        log.info("Tank-alert subscriber started — ATG block/release commands "
                 "will lock matching nozzles via nozzle_locks.json")

    # Local ATG interlock subscriber (on-site broker — no internet).
    # Placed here so `controllers` is fully built before we map nozzle->SA.
    _local_il_sub = None
    _local_il_cfg = cfg.get('local_interlock', {})
    if _local_il_cfg.get('enabled', False):
        try:
            from local_interlock_subscriber import LocalInterlockSubscriber
            _local_il_sub = LocalInterlockSubscriber.from_config(
                _local_il_cfg, mpd_no=int(_cfg_mpd_no or 0), controllers=controllers)
            _local_il_sub.start()
            log.info("Local interlock subscriber started — topic fuel-automation/interlock/MPD_%s", _cfg_mpd_no)
        except Exception as _exc:
            log.error("Local interlock subscriber init failed: %s", _exc)
        # ALSO route AWS-IoT tank_alert messages from TMS through the same
        # handler so TMS UI lock/unlock works (TMS publishes via AWS IoT, not
        # the IOCL broker that the IOCL subscriber is connected to).
        try:
            if publisher is not None and hasattr(publisher, 'set_tank_alert_callback'):
                publisher.set_tank_alert_callback(_tank_alert_sub.process_payload)
                log.info("Tank-alert: AWS-IoT bridge → cloud_publisher will forward "
                         "TMS-side messages to TankAlertSubscriber.process_payload")
        except Exception as _exc:
            log.warning("Tank-alert AWS-IoT bridge wire-up failed: %s", _exc)

    if _cpcl_cfg.get('enabled', False):
        threading.Thread(
            target=_cpcl_price_scheduler,
            name="cpcl-price-scheduler",
            daemon=True,
        ).start()

    # Density scheduler only runs in scheduled mode.  In immediate-apply mode
    # the IOCL subscriber writes densities to the MPD on receipt, so there's
    # no pending file for the scheduler to process — starting it would just
    # tick uselessly every minute.
    if (_cpcl_density_cfg.get('enabled', False)
            and not _cpcl_density_cfg.get('immediate_apply', True)):
        threading.Thread(
            target=_cpcl_density_scheduler,
            name="cpcl-density-scheduler",
            daemon=True,
        ).start()
        log.info("CPCL density scheduler started (apply at %s)", _density_apply_time_s)
    else:
        # Also clean up any stale pending file from earlier scheduled runs.
        try:
            if _os.path.exists(_density_pending_file):
                _os.remove(_density_pending_file)
                log.info("CPCL density: removed stale pending file %s "
                         "(immediate-apply mode active)", _density_pending_file)
        except Exception as _exc:
            log.warning("CPCL density: could not remove stale pending file: %s", _exc)

    # Block main thread — all work happens in controller daemon threads
    _stop_event.wait()


def main():
    _acquire_instance_lock()

    parser = argparse.ArgumentParser(
        description='RPi4 Fuel Automation - TQCL/Tatsuno RS485 driver'
    )
    parser.add_argument('--config',   default='config.yaml',
                        help='Path to config.yaml')
    parser.add_argument('--dry-run',  action='store_true',
                        help='Simulate RS485 without hardware')
    args = parser.parse_args()

    cfg      = load_config(args.config)
    cfg      = apply_topic_gates(cfg)
    mpd_type = cfg.get('mpd_type', 'tokheim').lower().strip()

    # ── Auto-set baud rate and parity based on MPD type ───────────────────────
    # These override whatever is written in config.yaml [serial] section so
    # the operator never needs to change serial settings when switching MPD type.
    #   tokheim  →  9600 baud, No parity   (TQCL v2.06 Rev.7)
    #   tatsuno  → 19200 baud, Even parity (JISEDAI POS)
    serial_cfg = cfg.setdefault('serial', {})
    if mpd_type == 'tatsuno':
        serial_cfg['baud_rate'] = 19200
        serial_cfg['parity']    = 'N'   # Site MPD confirmed 8N1 (not 8E1) — Even parity caused MPD silence
        print(f"[fuel-automation] MPD type: TATSUNO  →  serial: 19200 baud, No parity")
    else:
        serial_cfg['baud_rate'] = 9600
        serial_cfg['parity']    = 'N'
        print(f"[fuel-automation] MPD type: TOKHEIM  →  serial: 9600 baud, No parity")

    if mpd_type == 'tatsuno':
        run_tatsuno(cfg, dry_run=args.dry_run, config_path=args.config)
    else:
        # Default: Tokheim TQCL path (existing FuelAutomation)
        if mpd_type not in ('tokheim',):
            log.warning("Unknown mpd_type=%r — defaulting to tokheim", mpd_type)
        engine = FuelAutomation(cfg, dry_run=args.dry_run, config_path=args.config)
        engine.start()


if __name__ == '__main__':
    main()
