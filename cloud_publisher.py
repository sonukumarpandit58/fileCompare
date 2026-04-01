"""
cloud_publisher.py - Azure IoT Hub MQTT transaction publisher
=============================================================

PURPOSE
-------
This module handles everything AFTER a transaction is completed by
the pump controller. Its responsibilities are:

  1. PSV Formatting      : Convert a TransactionRecord into a 21-field
                           pipe-separated string matching the existing
                           C1 firmware output format.

  2. JSON Envelope       : Wrap the PSV string in the JSON format expected
                           by the Azure IoT Hub backend:
                           {"BPCL_181846_1_HSD_NOZZLE_1": [{"d": "<PSV>"}]}

  3. Transaction Counter : Maintain a persistent, incrementing sequence
                           number across restarts by writing to a JSON file
                           on the SD card.

  4. Local SQLite Store  : Every transaction is written to a local SQLite
                           database immediately on completion with
                           mqtt_sent=False.  On successful MQTT publish the
                           row is marked mqtt_sent=True.  Rows older than
                           LOCAL_RETENTION_DAYS (7) are purged automatically
                           so storage never grows unbounded.

  5. MQTT Publishing     : Connect to Azure IoT Hub over TLS (port 8883)
                           using paho-mqtt, publish each transaction with
                           QoS 1, and handle reconnection automatically.

  6. Background Retry    : A daemon thread wakes every RETRY_INTERVAL
                           seconds (default 60 s), queries the local DB for
                           any rows where mqtt_sent=False, and re-publishes
                           them.  This guarantees every transaction is
                           eventually delivered even across long outages -
                           no manual restart required.

PSV FORMAT (21 pipe-separated fields)
--------------------------------------
    Field  1: seq            - transaction sequence number
    Field  2: serial         - site serial number (from config.yaml)
    Field  3: pump_char      - pump letter (du_id=1->'A', 2->'B', etc.)
    Field  4: nozzle_id      - nozzle number within this site
    Field  5: unit_price     - price per litre at time of sale
    Field  6: payment_mode   - 1=cash, 2=card, etc. (default 1)
    Field  7: discount       - discount amount (default 0)
    Field  8: net_amount     - amount charged (after discount)
    Field  9: gross_amount   - amount before discount
    Field 10: density        - fuel density g/mL
    Field 11: stot_volume    - start volume totalizer (litres)
    Field 12: stot_amount    - start amount totalizer (INR)
    Field 13: etot_volume    - end volume totalizer (litres)
    Field 14: etot_amount    - end amount totalizer (INR)
    Field 15: start_time     - ISO-8601 timestamp when nozzle was lifted
    Field 16: end_time       - ISO-8601 timestamp when fueling completed
    Field 17: transaction_id - unique transaction ID string
    Field 18: type           - always 'transaction'
    Field 19: serial         - site serial (repeated, matches C1 firmware)
    Field 20: product        - fuel product name (e.g. 'HSD', 'EBMS')
    Field 21: count          - transaction sequence number (same as seq)

LOCAL SQLITE SCHEMA
-------------------
    Table: transactions
        txn_id      TEXT PRIMARY KEY
        envelope    TEXT NOT NULL          -- full JSON string for MQTT
        psv         TEXT NOT NULL          -- human-readable PSV
        created_at  TEXT NOT NULL          -- ISO-8601 timestamp
        mqtt_sent   INTEGER NOT NULL       -- 0=pending, 1=sent
        sent_at     TEXT                   -- ISO-8601 when sent (NULL until sent)

RETENTION POLICY
----------------
    Rows where created_at < (NOW - 7 days) are deleted during every
    startup and every retry cycle.  Only rows with mqtt_sent=1 are
    deleted in bulk; unsent rows (mqtt_sent=0) are kept regardless of
    age until they are successfully delivered.

BACKGROUND RETRY
----------------
    Thread wakes every RETRY_INTERVAL seconds.
    Queries: SELECT * FROM transactions WHERE mqtt_sent=0
    For each row: attempt publish → on success mark sent.
    Also calls purge_old() to clean up stale sent rows.
    Thread is a daemon so it exits automatically with the main process.

AZURE IOT HUB MQTT DETAILS
----------------------------
    Broker    : <iothub_name>.azure-devices.net
    Port      : 8883 (MQTT over TLS)
    Client ID : Device ID (e.g. BPCL_181846_RPI4)
    Username  : {broker}/{device_id}/?api-version=2021-04-12
    Password  : SAS token (set via MQTT_PASSWORD environment variable)
    Topic     : devices/{device_id}/messages/events/
    QoS       : 1 (at-least-once delivery)
"""

import json
import logging
import os
import queue
import secrets
import sqlite3
import threading
import time
from datetime import datetime, timedelta
from typing import List, Optional, Tuple

try:
    import paho.mqtt.client as mqtt
    MQTT_AVAILABLE = True
except ImportError:
    MQTT_AVAILABLE = False

from pump_controller import TransactionRecord

log = logging.getLogger(__name__)

# ── Constants ─────────────────────────────────────────────────────────────────

LOCAL_RETENTION_DAYS = 7     # delete sent rows older than this
RETRY_INTERVAL       = 60    # seconds between background retry cycles
DB_PATH              = "/var/lib/fuel-automation/transactions.db"


# ── PSV formatter ─────────────────────────────────────────────────────────────

def format_psv(
    record: TransactionRecord,
    seq: int,
    site_serial: str,
    du_id: int,
    count: int = 1,
    payment_mode: int = 1,
    discount: float = 0.0,
) -> str:
    """Build the 21-field pipe-separated transaction string (PSV format)."""
    pump_char = chr(ord('A') + du_id - 1)
    fields = [
        str(seq),
        site_serial,
        pump_char,
        str(record.nozzle_id),
        f"{record.unit_price:.2f}",
        str(payment_mode),
        str(int(discount)),
        f"{record.amount:.2f}",
        f"{record.amount:.2f}",
        f"{record.density:.2f}",
        f"{record.stot_volume:.2f}",
        f"{record.stot_amount:.2f}",
        f"{record.etot_volume:.2f}",
        f"{record.etot_amount:.2f}",
        record.start_time,
        record.end_time,
        record.transaction_id,
        "transaction",
        site_serial,
        record.product,
        str(count),
    ]
    return "|".join(fields)


def format_json_envelope(
    record: TransactionRecord,
    site_id: str,
    du_id: int,
    psv: str,
) -> str:
    """Wrap a PSV string in the JSON envelope expected by the Azure IoT Hub backend."""
    key     = f"{site_id}_{du_id}_{record.product}_NOZZLE_{record.nozzle_id}"
    payload = {key: [{"d": psv}]}
    return json.dumps(payload)


# ── Transaction counter ───────────────────────────────────────────────────────

class TransactionCounter:
    """
    Persistent, thread-safe transaction sequence number counter.
    Stored in a JSON file so numbers survive process restarts.
    """

    def __init__(self, path: str):
        self._path  = path
        self._lock  = threading.Lock()
        self._count = self._load()

    def _load(self) -> int:
        try:
            with open(self._path) as f:
                data = json.load(f)
                val  = int(data.get("transaction_count", 0))
                if val <= 0:
                    log.warning("Invalid txn count %d - resetting to 1", val)
                    return 1
                return val
        except FileNotFoundError:
            log.info("Txn count file not found - starting at 1")
            return 1
        except Exception as exc:
            log.error("Failed to load txn count: %s - resetting to 1", exc)
            return 1

    def _save(self):
        os.makedirs(os.path.dirname(self._path), exist_ok=True)
        with open(self._path, 'w') as f:
            json.dump({"transaction_count": self._count}, f)

    def next(self) -> int:
        with self._lock:
            val = self._count
            self._count += 1
            self._save()
            return val


# ── Local SQLite transaction store ────────────────────────────────────────────

class LocalTransactionStore:
    """
    SQLite-backed local store for every transaction.

    Guarantees:
      - Every completed transaction is written locally before MQTT is attempted.
      - Rows are kept for LOCAL_RETENTION_DAYS (7) after being successfully sent.
      - Unsent rows are kept indefinitely until delivered.
      - Thread-safe via a per-instance Lock (multiple nozzle threads may call
        save() concurrently).

    Schema:
        transactions(txn_id TEXT PK, envelope TEXT, psv TEXT,
                     created_at TEXT, mqtt_sent INTEGER, sent_at TEXT)
    """

    def __init__(self, db_path: str = DB_PATH):
        self._path = db_path
        self._lock = threading.Lock()
        self._init_db()

    def _connect(self) -> sqlite3.Connection:
        os.makedirs(os.path.dirname(self._path), exist_ok=True)
        conn = sqlite3.connect(self._path, check_same_thread=False)
        conn.row_factory = sqlite3.Row
        return conn

    def _init_db(self):
        """Create table if it does not exist (idempotent)."""
        with self._lock:
            conn = self._connect()
            try:
                conn.execute("""
                    CREATE TABLE IF NOT EXISTS transactions (
                        txn_id     TEXT PRIMARY KEY,
                        envelope   TEXT    NOT NULL,
                        psv        TEXT    NOT NULL,
                        created_at TEXT    NOT NULL,
                        mqtt_sent  INTEGER NOT NULL DEFAULT 0,
                        sent_at    TEXT
                    )
                """)
                conn.execute(
                    "CREATE INDEX IF NOT EXISTS idx_sent ON transactions(mqtt_sent)"
                )
                conn.commit()
                log.info("LocalTransactionStore ready: %s", self._path)
            finally:
                conn.close()

    def save(self, txn_id: str, envelope: str, psv: str):
        """
        Write a new transaction row with mqtt_sent=0.
        Ignored silently if txn_id already exists (idempotent).
        """
        with self._lock:
            conn = self._connect()
            try:
                conn.execute(
                    """INSERT OR IGNORE INTO transactions
                       (txn_id, envelope, psv, created_at, mqtt_sent)
                       VALUES (?, ?, ?, ?, 0)""",
                    (txn_id, envelope, psv, datetime.now().isoformat(timespec='seconds'))
                )
                conn.commit()
                log.debug("LocalStore saved txn=%s", txn_id)
            except Exception as exc:
                log.error("LocalStore save error txn=%s: %s", txn_id, exc)
            finally:
                conn.close()

    def mark_sent(self, txn_id: str):
        """Mark a transaction as successfully published to MQTT."""
        with self._lock:
            conn = self._connect()
            try:
                conn.execute(
                    "UPDATE transactions SET mqtt_sent=1, sent_at=? WHERE txn_id=?",
                    (datetime.now().isoformat(timespec='seconds'), txn_id)
                )
                conn.commit()
                log.debug("LocalStore marked sent txn=%s", txn_id)
            except Exception as exc:
                log.error("LocalStore mark_sent error txn=%s: %s", txn_id, exc)
            finally:
                conn.close()

    def get_unsent(self) -> List[Tuple[str, str]]:
        """
        Return list of (txn_id, envelope) for all rows where mqtt_sent=0.
        Ordered by created_at ascending (oldest first).
        """
        with self._lock:
            conn = self._connect()
            try:
                rows = conn.execute(
                    "SELECT txn_id, envelope FROM transactions "
                    "WHERE mqtt_sent=0 ORDER BY created_at ASC"
                ).fetchall()
                return [(r["txn_id"], r["envelope"]) for r in rows]
            except Exception as exc:
                log.error("LocalStore get_unsent error: %s", exc)
                return []
            finally:
                conn.close()

    def purge_old(self):
        """
        Delete sent rows older than LOCAL_RETENTION_DAYS days.
        Unsent rows are NEVER deleted regardless of age.
        """
        cutoff = (datetime.now() - timedelta(days=LOCAL_RETENTION_DAYS)) \
                     .isoformat(timespec='seconds')
        with self._lock:
            conn = self._connect()
            try:
                cur = conn.execute(
                    "DELETE FROM transactions WHERE mqtt_sent=1 AND created_at < ?",
                    (cutoff,)
                )
                conn.commit()
                if cur.rowcount:
                    log.info("LocalStore purged %d sent rows older than %d days",
                             cur.rowcount, LOCAL_RETENTION_DAYS)
            except Exception as exc:
                log.error("LocalStore purge error: %s", exc)
            finally:
                conn.close()

    def stats(self) -> dict:
        """Return dict with total, sent, unsent row counts for logging."""
        with self._lock:
            conn = self._connect()
            try:
                total  = conn.execute("SELECT COUNT(*) FROM transactions").fetchone()[0]
                sent   = conn.execute(
                    "SELECT COUNT(*) FROM transactions WHERE mqtt_sent=1").fetchone()[0]
                unsent = conn.execute(
                    "SELECT COUNT(*) FROM transactions WHERE mqtt_sent=0").fetchone()[0]
                return {"total": total, "sent": sent, "unsent": unsent}
            except Exception as exc:
                log.error("LocalStore stats error: %s", exc)
                return {}
            finally:
                conn.close()


# ── MQTT publisher ────────────────────────────────────────────────────────────

class MQTTPublisher:
    """
    Azure IoT Hub MQTT client for publishing completed transactions.

    FLOW FOR EACH TRANSACTION
    -------------------------
    1. Save to local SQLite with mqtt_sent=0  (guaranteed local backup)
    2. Try MQTT publish (3 immediate attempts)
       - Success  → mark mqtt_sent=1 in SQLite
       - Failure  → leave mqtt_sent=0 (background retry thread will handle it)

    BACKGROUND RETRY THREAD
    -----------------------
    Daemon thread wakes every RETRY_INTERVAL seconds:
      - Calls purge_old() to remove sent rows older than 7 days
      - Fetches all rows where mqtt_sent=0
      - Publishes each one; marks sent on success
    Result: every transaction is eventually delivered, even after days of outage.
    No restart required.

    LOCAL STORAGE GUARANTEE
    -----------------------
    Even if MQTT never recovers, all transaction data is safely stored in
    SQLite for 7 days after the sale for manual recovery if needed.
    """

    def __init__(self, cfg: dict, site_cfg: dict):
        self._cfg      = cfg
        self._site     = site_cfg
        self._client: Optional['mqtt.Client'] = None
        self._connected    = False
        self._running      = False

        # Publish queue: fire-and-forget from pump threads.
        # Items: (txn_id, envelope) tuples.
        # _publish_worker drains this queue independently of the pump flow.
        self._publish_queue: queue.Queue = queue.Queue()

        # Generate a unique client ID for this process instance.
        #
        # Problem: if the same fixed client ID (e.g. "BPCL_222459_RPI4889") is
        # used by any other device or stray process, AWS IoT Core performs a
        # session takeover — it sends DISCONNECT (rc=7) to the older connection,
        # which then reconnects and kicks the newer one, creating an infinite loop.
        #
        # Fix: take the base from config and append a fresh 4-hex-char random
        # suffix on every process start.  Each restart produces a guaranteed-unique
        # client ID so session takeover is impossible.
        #
        # Format: <config_client_id>_<4 random hex chars>
        # Example: BPCL_222459_RPI4889_a3f9
        #
        # AWS IoT policy must allow  "client/BPCL_222459_RPI4889_*"  (wildcard).
        # Update the IoT policy if it currently allows only the exact fixed ID.
        base_id = cfg.get('client_id', 'fuel-device')
        rand_suffix = secrets.token_hex(2)          # 4 hex chars, e.g. "a3f9"
        self._client_id = f"{base_id}_{rand_suffix}"
        log.info("MQTT client_id this session: %s", self._client_id)

        txn_cfg    = site_cfg.get('transaction', {})
        count_file = txn_cfg.get(
            'transaction_count_file',
            '/var/lib/fuel-automation/txn_count.json'
        )
        self._counter = TransactionCounter(count_file)
        self._store   = LocalTransactionStore()

    # ── Lifecycle ─────────────────────────────────────────────────────────────

    def connect(self):
        """
        Start all background threads and initiate MQTT connection.

        IMPORTANT: This method returns IMMEDIATELY regardless of network
        availability.  The MQTT client setup (including DNS resolution which
        can block 10-30s when offline) runs in a separate daemon thread so
        the pump RS485 flow is never delayed.

        Thread summary after connect() returns:
          mqtt-init    - one-shot: creates paho client, calls connect_async()
                         and loop_start().  Exits once connection is handed off.
          mqtt-publish - persistent: drains the publish queue, waits up to 30s
                         per item for MQTT to be ready.
          mqtt-retry   - persistent: every 60s retries unsent rows from SQLite.
        """
        self._running = True

        # ── Background retry thread (every 60s, picks up unsent from SQLite) ──
        t_retry = threading.Thread(target=self._retry_loop, name="mqtt-retry", daemon=True)
        t_retry.start()
        log.info("MQTT background retry thread started (interval=%ds)", RETRY_INTERVAL)

        # ── Publish worker thread (drains queue immediately, no pump blocking) ─
        t_pub = threading.Thread(target=self._publish_worker, name="mqtt-publish", daemon=True)
        t_pub.start()
        log.info("MQTT publish worker thread started")

        if not MQTT_AVAILABLE:
            log.warning("paho-mqtt not installed - MQTT publishing disabled")
            return
        if not self._cfg.get('enabled', False):
            log.info("MQTT publishing disabled in config")
            return

        # ── MQTT init in background thread ────────────────────────────────────
        # connect_async() performs synchronous DNS resolution before returning.
        # When there is no internet this blocks for the full OS DNS timeout
        # (10-30s), which would delay pump startup and cause dispensers to miss
        # the initial Pump Start command, leaving them stuck in CALL state on
        # the first nozzle lift.  Running in a daemon thread avoids all of that.
        t_init = threading.Thread(target=self._mqtt_init, name="mqtt-init", daemon=True)
        t_init.start()
        log.info("MQTT init thread started (connecting in background)")

    def _mqtt_init(self):
        """
        One-shot background thread: create the paho client and start connecting.

        Runs entirely in the background so connect() returns instantly even
        when DNS is slow or the network is offline.
        """
        try:
            # Instantiate paho client with v1/v2 compatibility.
            # Use the per-session unique client_id (base + random suffix).
            try:
                client = mqtt.Client(
                    callback_api_version=mqtt.CallbackAPIVersion.VERSION1,
                    client_id=self._client_id,
                )
            except AttributeError:
                client = mqtt.Client(client_id=self._client_id)

            client.on_connect    = self._on_connect
            client.on_disconnect = self._on_disconnect

            # Exponential backoff: 5s → 10s → 20s → ... → 120s max.
            # Prevents TLS-handshake storm on repeated reconnects.
            client.reconnect_delay_set(min_delay=5, max_delay=120)

            auth_mode = self._cfg.get('auth_mode', 'sas')

            if auth_mode == 'cert':
                # ── AWS IoT Core: X.509 mutual TLS ───────────────────────────
                ca        = self._cfg.get('ca_cert',   '/etc/fuel-automation/certs/AmazonRootCA1.pem')
                cert_file = self._cfg.get('cert_file', '/etc/fuel-automation/certs/device.crt')
                key_file  = self._cfg.get('key_file',  '/etc/fuel-automation/certs/device.key')
                for f, label in [(ca, 'ca_cert'), (cert_file, 'cert_file'), (key_file, 'key_file')]:
                    if not os.path.exists(f):
                        log.error("MQTT cert file missing (%s): %s", label, f)
                        return
                client.tls_set(ca_certs=ca, certfile=cert_file, keyfile=key_file)
                log.info("MQTT auth_mode=cert (AWS IoT X.509)")
            else:
                # ── Azure IoT Hub: SAS token ──────────────────────────────────
                password = self._cfg.get('password') or os.environ.get('MQTT_PASSWORD', '')
                client.username_pw_set(self._cfg.get('username', ''), password)
                ca = self._cfg.get('ca_cert', '/etc/ssl/certs/ca-certificates.crt')
                if os.path.exists(ca):
                    client.tls_set(ca_certs=ca)
                else:
                    log.warning("CA cert not found at %s - using system default TLS", ca)
                    client.tls_set()
                log.info("MQTT auth_mode=sas (Azure IoT Hub)")

            # Store client reference BEFORE connect_async so _publish_worker
            # can access it once the connection comes up.
            self._client = client

            # connect_async() may block here for DNS resolution (up to 30s
            # when offline) - that is fine because we are in a daemon thread.
            client.connect_async(self._cfg['broker'], self._cfg['port'])
            client.loop_start()
            log.info("MQTT connecting to %s:%d (background)",
                     self._cfg['broker'], self._cfg['port'])

        except Exception as exc:
            log.error("MQTT init error: %s - transactions saved locally, "
                      "retry thread will publish when network recovers", exc)

    def disconnect(self):
        """Stop all threads and disconnect MQTT cleanly."""
        self._running = False
        # Unblock _publish_worker's queue.get() if it's waiting
        try:
            self._publish_queue.put_nowait(None)
        except Exception:
            pass
        if self._client:
            self._client.loop_stop()
            self._client.disconnect()

    # ── MQTT callbacks ────────────────────────────────────────────────────────

    def _on_connect(self, client, userdata, flags, rc):
        if rc == 0:
            self._connected = True
            log.info("MQTT connected")
        else:
            log.error("MQTT connect failed rc=%d", rc)

    def _on_disconnect(self, client, userdata, rc):
        self._connected = False
        log.warning("MQTT disconnected rc=%d - will reconnect", rc)

    # ── Primary publish entry point ───────────────────────────────────────────

    def publish_transaction(self, record: TransactionRecord, du_id: int):
        """
        Called by FuelAutomation._handle_on_hook() after each completed sale.

        FIRE-AND-FORGET: This method returns in milliseconds regardless of
        MQTT connectivity.  The pump flow is NEVER blocked by MQTT state.

        Steps:
          1. Assign sequence number and build PSV + JSON envelope.
          2. Save to local SQLite immediately (mqtt_sent=0).
             Transaction is safe on disk before this returns.
          3. Enqueue (txn_id, envelope) for the _publish_worker thread.
             _publish_worker attempts MQTT publish asynchronously.
             On failure, _retry_loop picks it up every 60 seconds.
        """
        seq      = self._counter.next()
        site_id  = self._site['site']['id']
        serial   = self._site['site']['serial_number']
        density  = self._site.get('transaction', {}).get('density_default', 0.830)

        record.density = density

        # Generate txn_id if not already set
        pump_char = chr(ord('A') + du_id - 1)
        if not record.transaction_id:
            record.transaction_id = (
                datetime.now().strftime('%Y%m%d%H%M%S')
                + pump_char + str(record.nozzle_id)
            )

        psv      = format_psv(record, seq=seq, site_serial=serial, du_id=du_id, count=seq)
        envelope = format_json_envelope(record, site_id=site_id, du_id=du_id, psv=psv)

        log.info("Transaction [%s]: vol=%.3fL  amt=%.2f  product=%s  seq=%d",
                 record.transaction_id, record.volume, record.amount,
                 record.product, seq)
        log.debug("PSV: %s", psv)

        # ── Step 1: Always save locally first (guaranteed, instant) ───────────
        self._store.save(record.transaction_id, envelope, psv)

        # ── Step 2: Enqueue for async publish (non-blocking, returns now) ──────
        self._publish_queue.put((record.transaction_id, envelope))
        log.info("Txn [%s] queued for MQTT publish (pump thread free)", record.transaction_id)

    # ── Publish helper ────────────────────────────────────────────────────────

    def _try_publish(self, txn_id: str, envelope: str, attempts: int = 1) -> bool:
        """
        Attempt to publish envelope to MQTT broker.

        Args:
            txn_id   : transaction ID (for logging and mark_sent)
            envelope : JSON string payload
            attempts : number of attempts before giving up

        Returns:
            True if published and acknowledged successfully, False otherwise.
        """
        if not MQTT_AVAILABLE or not self._cfg.get('enabled', False):
            # MQTT disabled - treat as "sent" so we don't retry forever
            # (data is still in SQLite for manual recovery)
            self._store.mark_sent(txn_id)
            return True

        site_id = self._site['site']['id']
        topic = self._cfg['topic_template'].format(
            client_id=self._cfg['client_id'],
            site_id=site_id,
        )

        for attempt in range(1, attempts + 1):
            if not self._connected:
                log.warning("Txn [%s] MQTT not connected (attempt %d/%d)",
                            txn_id, attempt, attempts)
                if attempt < attempts:
                    time.sleep(5)
                continue

            try:
                info = self._client.publish(
                    topic, envelope,
                    qos=self._cfg.get('qos', 1),
                    retain=self._cfg.get('retain', False),
                )
                if info.rc == mqtt.MQTT_ERR_SUCCESS:
                    self._store.mark_sent(txn_id)
                    return True
                log.warning("Txn [%s] publish rc=%d (attempt %d/%d)",
                            txn_id, info.rc, attempt, attempts)
            except Exception as exc:
                log.warning("Txn [%s] publish error (attempt %d/%d): %s",
                            txn_id, attempt, attempts, exc)

            if attempt < attempts:
                time.sleep(5)

        return False

    # ── Async publish worker thread ───────────────────────────────────────────

    def _publish_worker(self):
        """
        Background daemon thread: drain the publish queue and send to MQTT.

        Completely decoupled from pump/RS485 threads — the pump never waits
        for MQTT.  This thread:
          1. Blocks on queue.get() until a (txn_id, envelope) arrives.
          2. Attempts to publish immediately if MQTT is connected.
          3. If not connected, waits up to 30s (polling every 2s) for connection.
          4. If still not connected, leaves mqtt_sent=0 so _retry_loop picks it up.
          5. Loops back to wait for next queued item.

        All timing waits happen ONLY in this thread — zero impact on pump flow.
        """
        while self._running:
            try:
                item = self._publish_queue.get(timeout=2)
                if item is None:          # sentinel from disconnect()
                    break
                txn_id, envelope = item
            except queue.Empty:
                continue

            # Wait for MQTT connection (up to 30s), then publish
            wait_secs = 0
            while not self._connected and wait_secs < 30 and self._running:
                time.sleep(2)
                wait_secs += 2

            if self._try_publish(txn_id, envelope, attempts=1):
                log.info("Txn [%s] published to MQTT", txn_id)
            else:
                log.warning("Txn [%s] MQTT not available - saved locally, "
                            "retry thread will resend in %ds", txn_id, RETRY_INTERVAL)

    # ── Background retry thread ───────────────────────────────────────────────

    def _retry_loop(self):
        """
        Background daemon thread: retry unsent transactions every RETRY_INTERVAL s.

        Cycle:
          1. purge_old() - remove sent rows older than 7 days
          2. get_unsent() - fetch all unsent rows (oldest first)
          3. For each unsent row: _try_publish() → mark sent on success
          4. Log summary if any unsent rows were found
          5. Sleep RETRY_INTERVAL seconds
        """
        log.info("MQTT retry loop started")
        while self._running:
            time.sleep(RETRY_INTERVAL)
            if not self._running:
                break

            try:
                # Purge stale sent rows (keep storage clean)
                self._store.purge_old()

                # Fetch all pending (unsent) transactions
                unsent = self._store.get_unsent()
                if not unsent:
                    continue

                log.info("Retry loop: %d unsent transaction(s) found", len(unsent))
                sent_count = 0

                for txn_id, envelope in unsent:
                    if not self._running:
                        break
                    if self._try_publish(txn_id, envelope, attempts=1):
                        sent_count += 1
                        log.info("Retry: txn [%s] published OK", txn_id)
                    else:
                        log.warning("Retry: txn [%s] still failing - will retry later",
                                    txn_id)

                remaining = len(unsent) - sent_count
                log.info("Retry cycle complete: sent=%d  still_pending=%d",
                         sent_count, remaining)

                # Log store stats periodically
                stats = self._store.stats()
                log.info("LocalStore stats: total=%d  sent=%d  unsent=%d",
                         stats.get('total', 0),
                         stats.get('sent',  0),
                         stats.get('unsent', 0))

            except Exception as exc:
                log.error("Retry loop error: %s", exc, exc_info=True)

    # ── Legacy method (kept for compatibility) ────────────────────────────────

    def replay_pending(self):
        """
        Legacy method - previously replayed file-based pending transactions.
        Now handled automatically by the background retry thread.
        Kept so existing call in main.py start() does not break.
        """
        stats = self._store.stats()
        unsent = stats.get('unsent', 0)
        if unsent:
            log.info("Startup: %d unsent transaction(s) in local store "
                     "- retry thread will publish them", unsent)
        else:
            log.info("Startup: local store is clean (no unsent transactions)")
