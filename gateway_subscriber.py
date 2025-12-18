import base64
import binascii
import json
import logging
import time
from urllib.parse import parse_qs  # kept (even if currently unused)
from hashlib import sha256

import psycopg2
from psycopg2.extras import execute_values  # kept (even if currently unused)

from AWSIoTPythonSDK.MQTTLib import AWSIoTMQTTClient

# =========================
# Config
# =========================
CLIENT_ID = "sdk-nodejs-131220251013"
ENDPOINT = "ar7fthu9bpn11-ats.iot.ap-south-1.amazonaws.com"
TOPIC = "gateway/status"

ROOT_CA = "AmazonRootCA1.pem"
PRIVATE_KEY = "MyThing.private.key"
CERT_FILE = "MyThing.cert.pem"

# Dedup cache settings (prevents duplicate DB inserts if same message is re-delivered)
DEDUP_ENABLED = True
DEDUP_TTL_SECONDS = 120  # keep hashes for 2 minutes

# =========================
# PostgreSQL DB Config
# =========================
DB_HOST = "127.0.0.1"
DB_PORT = 5432
DB_NAME = "nwaresmart"
DB_USER = "myuser"
DB_PASS = "Nwaresoft@2025NwareSmart"

TABLE_NAME = "public.device_gateway_data_logs"
DATA_TABLE_NAME = "public.device_gateway_data"
DEVICES_TABLE_NAME = "public.devices"

# Table columns (excluding id, deleted_at; we set created_at/updated_at ourselves)
TABLE_COLUMNS = {
    "model", "type", "imei", "header",
    "cum_eb_kwh", "cum_dg_kwh",
    "relay_status", "eb_dg_status",
    "eb_load_setting", "dg_load_setting",
    "meter_serial_number",
    "rtc_date_ddmmyy", "rtc_time_hhmmss",
    "eb_terriff_setting", "dg_terrif_setting",
    "balance_amount", "daily_charge_setting",
    "no_of_over_load_check",
    "over_load_delay_between_two_attemps",
    "over_load_check_time_in_second",
    "last_balance_update",
    "2nd_last_balance_update", "3rd_last_balance_update", "4th_last_balance_update",
    "5th_last_balance_update", "6th_last_balance_update", "7th_last_balance_update",
    "8th_last_balance_update", "9th_last_balance_update", "10th_last_balance_update",
    "11th_last_balance_update", "12th_last_balance_update",
    "frequency",
    "voltage_r", "voltage_y", "voltage_b",
    "current_r", "current_y", "current_b",
    "pf",
    "kw_load_r", "kw_load_y", "kw_load_b",
    "kva_load_r", "kva_load_y", "kva_load_b",
    "kvar_load_r", "kvar_load_y", "kvar_load_b",
    "last_balance_deduction",
    "2nd_last_balance_deduction", "3rd_last_balance_deduction", "4th_last_balance_deduction",
    "5th_last_balance_deduction", "6th_last_balance_deduction", "7th_last_balance_deduction",
    "8th_last_balance_deduction", "9th_last_balance_deduction", "10th_last_balance_deduction",
    "cum_kvah", "cum_kvah_dg", "cum_kvarh", "cum_kvarh_dg",
    "cum_eb_kwh_40060", "cum_dg_kwh_40061",
    "total_kw", "total_kva", "total_kvar",
    "na_40065", "na_40066", "na_40067", "na_40068", "na_40069", "na_40070",
    "induvisal_relay_status_dg", "induvisal_relay_status_eb",
    "over_aattp_eb", "over_aattp_dg",
    "na_40075",
    "version",
    "crc",
    "signal",
    "error",
}

# int4 columns in schema
INT_COLUMNS = {
    "relay_status", "eb_dg_status",
    "no_of_over_load_check",
    "over_load_delay_between_two_attemps",
    "over_load_check_time_in_second",
    "induvisal_relay_status_dg", "induvisal_relay_status_eb",
    "over_aattp_eb", "over_aattp_dg",
}

# varchar columns that you currently produce as floats
VARCHAR_NUM_COLUMNS = {"meter_serial_number", "rtc_date_ddmmyy", "rtc_time_hhmmss"}

# =========================
# Logging
# =========================
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s: %(message)s",
)
log = logging.getLogger("gateway_subscriber")

sdk_logger = logging.getLogger("AWSIoTPythonSDK.core")
sdk_logger.setLevel(logging.WARNING)

# In-memory dedup cache
_seen = {}  # hash -> last_seen_epoch


# =========================
# Small helpers
# =========================
def _get_conn():
    return psycopg2.connect(
        host=DB_HOST,
        port=DB_PORT,
        dbname=DB_NAME,
        user=DB_USER,
        password=DB_PASS,
    )


def _num(val, default=0.0):
    """
    Always return a number for NOT NULL numeric columns.
    Prevents psycopg2 NotNullViolation (load/balance/etc).
    """
    try:
        if val is None:
            return default
        if isinstance(val, str) and val.strip().lower() in ("", "none", "null"):
            return default
        return float(val)
    except Exception:
        return default


def _dedup_cleanup(now: float):
    if not _seen:
        return
    cutoff = now - DEDUP_TTL_SECONDS
    for k in list(_seen.keys()):
        if _seen[k] < cutoff:
            _seen.pop(k, None)


def is_duplicate_message(iot_json: dict) -> bool:
    if not DEDUP_ENABLED:
        return False

    device_id = str(iot_json.get("device_id", ""))
    ts = str(iot_json.get("timestamp", ""))
    gw = str(iot_json.get("gateway", ""))

    h = sha256(f"{device_id}|{ts}|{gw}".encode("utf-8", errors="ignore")).hexdigest()

    now = time.time()
    _dedup_cleanup(now)

    if h in _seen:
        return True

    _seen[h] = now
    return False


# =========================
# Decode helpers
# =========================
def rle_decode(data: bytes) -> bytes:
    out = bytearray()
    i = 0
    n = len(data)
    while i < n:
        ch = data[i]
        i += 1
        if i >= n:
            break
        count = data[i]
        i += 1
        out.extend([ch] * count)
    return bytes(out)


def extract_gateway_b64(gateway_str: str) -> str:
    """
    Remove leading '**' and trailing '##' only, then strip whitespace.
    """
    if not isinstance(gateway_str, str) or not gateway_str:
        return ""

    s = gateway_str.strip()
    if s.startswith("**"):
        s = s[2:]
    if s.endswith("##"):
        s = s[:-2]
    return s.strip()


def safe_b64decode(b64_text: str) -> bytes:
    s = "".join(b64_text.split())
    pad = (-len(s)) % 4
    if pad:
        s += "=" * pad
    try:
        return base64.b64decode(s, validate=False)
    except (binascii.Error, ValueError):
        return base64.b64decode(s)


def decode_gateway_to_params(gateway_str: str) -> str:
    """
    Returns decoded text (hex string).
    """
    b64 = extract_gateway_b64(gateway_str)
    if not b64:
        return ""

    try:
        raw = safe_b64decode(b64)
    except Exception:
        log.exception("Base64 decode failed (after stripping **/##)")
        return ""

    try:
        decoded = rle_decode(raw)
        qs = decoded.decode("utf-8", errors="replace")
        print("decode data")
        print(qs)
        return qs
    except Exception:
        log.exception("RLE/UTF-8 decode failed")
        return ""


# =========================
# HEX parse helpers
# =========================
def normalize_hex_code(hex_code: str) -> str:
    if not isinstance(hex_code, str) or not hex_code:
        return ""

    if "-" in hex_code:
        hex_code = hex_code.split("-", 1)[0]
    if "-" in hex_code:
        hex_code = hex_code.split("-", 1)[0]

    if not hex_code:
        return ""

    parts = hex_code.split("#")
    last_idx = len(parts) - 1
    tmp = []

    for k, value in enumerate(parts):
        if not value:
            continue

        if k == 0:
            tmp.append(value[:-4] if len(value) > 4 else "")
        elif k == last_idx:
            tmp.append(value[6:] if len(value) > 6 else "")
        else:
            v2 = value[6:] if len(value) > 6 else ""
            tmp.append(v2[:-4] if len(v2) > 4 else "")

    return "".join(tmp).strip()


def parse_hex_code(hex_code: str) -> dict:
    hex_code = normalize_hex_code(hex_code)
    if not hex_code:
        return {}

    fields = [
        ("header", 3, 1),
        ("cum_eb_kwh", 4, 1000),
        ("cum_dg_kwh", 4, 1000),
        ("relay_status", 4, 1),
        ("eb_dg_status", 4, 1),
        ("eb_load_setting", 4, 100),
        ("dg_load_setting", 4, 100),
        ("meter_serial_number", 4, 1),
        ("rtc_date_ddmmyy", 4, 1),
        ("rtc_time_hhmmss", 4, 1),
        ("eb_terriff_setting", 4, 100),
        ("dg_terrif_setting", 4, 100),
        ("balance_amount", 4, 100),
        ("daily_charge_setting", 4, 100),
        ("no_of_over_load_check", 4, 1),
        ("over_load_delay_between_two_attemps", 4, 1),
        ("over_load_check_time_in_second", 4, 1),
        ("last_balance_update", 4, 1),
        ("2nd_last_balance_update", 4, 1),
        ("3rd_last_balance_update", 4, 1),
        ("4th_last_balance_update", 4, 1),
        ("5th_last_balance_update", 4, 1),
        ("6th_last_balance_update", 4, 1),
        ("7th_last_balance_update", 4, 1),
        ("8th_last_balance_update", 4, 1),
        ("9th_last_balance_update", 4, 1),
        ("10th_last_balance_update", 4, 1),
        ("11th_last_balance_update", 4, 1),
        ("12th_last_balance_update", 4, 1),
        ("frequency", 4, 10),
        ("voltage_r", 4, 10),
        ("voltage_y", 4, 10),
        ("voltage_b", 4, 10),
        ("current_r", 4, 1000),
        ("current_y", 4, 1000),
        ("current_b", 4, 1000),
        ("pf", 4, 100),
        ("kw_load_r", 4, 1000),
        ("kw_load_y", 4, 1000),
        ("kw_load_b", 4, 1000),
        ("kva_load_r", 4, 1000),
        ("kva_load_y", 4, 1000),
        ("kva_load_b", 4, 1000),
        ("kvar_load_r", 4, 1000),
        ("kvar_load_y", 4, 1000),
        ("kvar_load_b", 4, 1000),
        ("last_balance_deduction", 4, 1),
        ("2nd_last_balance_deduction", 4, 1),
        ("3rd_last_balance_deduction", 4, 1),
        ("4th_last_balance_deduction", 4, 1),
        ("5th_last_balance_deduction", 4, 1),
        ("6th_last_balance_deduction", 4, 1),
        ("7th_last_balance_deduction", 4, 1),
        ("8th_last_balance_deduction", 4, 1),
        ("9th_last_balance_deduction", 4, 1),
        ("10th_last_balance_deduction", 4, 1),
        ("cum_kvah", 4, 1000),
        ("cum_kvah_dg", 4, 1000),
        ("cum_kvarh", 4, 1000),
        ("cum_kvarh_dg", 4, 1000),
        ("cum_eb_kwh_40060", 4, 1000),
        ("cum_dg_kwh_40061", 4, 1000),
        ("total_kw", 4, 1000),
        ("total_kva", 4, 1000),
        ("total_kvar", 4, 1000),
        ("na_40065", 4, 100),
        ("na_40066", 4, 100),
        ("na_40067", 4, 100),
        ("na_40068", 4, 100),
        ("na_40069", 4, 100),
        ("na_40070", 4, 100),
        ("induvisal_relay_status_dg", 4, 1),
        ("induvisal_relay_status_eb", 4, 1),
        ("over_aattp_eb", 4, 1),
        ("over_aattp_dg", 4, 1),
        ("na_40075", 4, 1),
        ("version", 4, 100),
        ("crc", 2, 1),
    ]

    parsed = {}
    start = 0
    INT_FIELDS = {"relay_status", "induvisal_relay_status_eb"}

    for name, byte_len, div in fields:
        seg_len = byte_len * 2
        seg = hex_code[start:start + seg_len]

        if len(seg) < seg_len:
            parsed[name] = None
        else:
            if name in ("header", "crc"):
                parsed[name] = seg.upper()
            else:
                try:
                    if name in INT_FIELDS:
                        parsed[name] = int(seg)
                    else:
                        parsed[name] = int(seg, 16) / div
                except Exception:
                    parsed[name] = None

        start += seg_len

    return parsed


# =========================
# DB helpers
# =========================
def _to_db_value(col: str, val):
    if val is None:
        return None

    if isinstance(val, str):
        if val.strip() == "" or val.strip().lower() == "none":
            return None
        return val

    if col in INT_COLUMNS:
        try:
            return int(val)
        except Exception:
            return None

    if col in VARCHAR_NUM_COLUMNS:
        try:
            return str(int(val))
        except Exception:
            return str(val)

    return val


def _quote_col(col: str) -> str:
    if col == "type" or col == "version" or col[:1].isdigit():
        return f'"{col}"'
    return col


def insert_to_db_safe(final_row: dict) -> bool:
    """
    Inserts into device_gateway_data_logs
    """
    try:
        imei_val = final_row.get("imei")
        if imei_val in (None, "", "none"):
            imei_val = final_row.get("device_id")

        if imei_val in (None, "", "none"):
            log.warning("Skipping insert: imei/device_id missing")
            return False

        final_row = dict(final_row)
        final_row["imei"] = str(imei_val)

        insert_cols = []
        insert_vals = []

        for col in sorted(TABLE_COLUMNS):
            if col in final_row:
                insert_cols.append(col)
                insert_vals.append(_to_db_value(col, final_row.get(col)))

        if "imei" not in insert_cols:
            insert_cols.append("imei")
            insert_vals.append(_to_db_value("imei", final_row["imei"]))

        if "signal" not in insert_cols and "signal" in final_row:
            insert_cols.append("signal")
            insert_vals.append(_to_db_value("signal", final_row.get("signal")))

        if "error" not in insert_cols and "error" in final_row:
            insert_cols.append("error")
            insert_vals.append(_to_db_value("error", final_row.get("error")))

        cols_sql = ", ".join(_quote_col(c) for c in insert_cols)
        placeholders = ", ".join(["%s"] * len(insert_cols))

        sql = f"""
            INSERT INTO {TABLE_NAME} ({cols_sql}, created_at, updated_at)
            VALUES ({placeholders}, NOW(), NOW())
        """

        conn = _get_conn()
        try:
            with conn.cursor() as cur:
                cur.execute("SET TIME ZONE 'Asia/Kolkata';")
                cur.execute(sql, insert_vals)
            conn.commit()
        finally:
            conn.close()

        return True

    except Exception:
        log.exception("DB insert failed (device_gateway_data_logs)")
        return False


def get_device_id_by_imei(imei: str):
    """
    devices.id lookup using imei OR serial_number (safer).
    """
    if not imei:
        return None

    sql = f"""
        SELECT id
        FROM {DEVICES_TABLE_NAME}
        WHERE deleted_at IS NULL
          AND (imei = %s OR serial_number = %s)
        LIMIT 1
    """

    conn = _get_conn()
    try:
        with conn.cursor() as cur:
            cur.execute("SET TIME ZONE 'Asia/Kolkata';")
            cur.execute(sql, (str(imei), str(imei)))
            row = cur.fetchone()
            return row[0] if row else None
    finally:
        conn.close()


def insert_device_gateway_data_real(final_row: dict) -> bool:
    """
    If device_id exists in device_gateway_data → UPDATE
    If not exists → INSERT
    No UPSERT, no UNIQUE constraint required
    """
    try:
        imei = final_row.get("imei")
        if imei in (None, "", "none"):
            imei = final_row.get("device_id")

        if imei in (None, "", "none"):
            log.warning("device_gateway_data: imei missing")
            return False

        device_id = get_device_id_by_imei(str(imei))
        if not device_id:
            log.warning("device_gateway_data: device not found for imei=%s", imei)
            return False

        conn = _get_conn()
        try:
            with conn.cursor() as cur:
                cur.execute("SET TIME ZONE 'Asia/Kolkata';")

                # 1️⃣ CHECK if record exists
                cur.execute(
                    f"SELECT 1 FROM {DATA_TABLE_NAME} WHERE device_id = %s LIMIT 1",
                    (int(device_id),)
                )
                exists = cur.fetchone() is not None

                if exists:
                    # 2️⃣ UPDATE
                    sql = f"""
                        UPDATE {DATA_TABLE_NAME}
                        SET
                            tariff_per_unit = %s,
                            consumed_unit   = %s,
                            load            = %s,
                            balance         = %s,
                            per_unit_charge = %s,

                            voltage_r = %s,
                            voltage_y = %s,
                            voltage_b = %s,

                            current_r = %s,
                            current_y = %s,
                            current_b = %s,

                            pf     = %s,
                            signal = %s,
                            error  = %s,
                            updated_at = NOW()
                        WHERE device_id = %s
                    """

                    values = (
                        _num(final_row.get("eb_terriff_setting")),
                        _num(final_row.get("cum_eb_kwh")),
                        _num(final_row.get("total_kw")),
                        _num(final_row.get("balance_amount")),
                        _num(final_row.get("eb_terriff_setting")),

                        _num(final_row.get("voltage_r")),
                        _num(final_row.get("voltage_y")),
                        _num(final_row.get("voltage_b")),
                        _num(final_row.get("current_r")),
                        _num(final_row.get("current_y")),
                        _num(final_row.get("current_b")),

                        _num(final_row.get("pf")),
                        str(final_row.get("signal")) if final_row.get("signal") is not None else None,
                        final_row.get("error"),

                        int(device_id),
                    )

                    cur.execute(sql, values)
                    log.info("device_gateway_data UPDATED device_id=%s", device_id)

                else:
                    # 3️⃣ INSERT (only if not exists)
                    sql = f"""
                        INSERT INTO {DATA_TABLE_NAME} (
                            device_id,
                            start_unit_date,
                            tariff_per_unit,
                            consumed_unit,
                            load,
                            balance,
                            per_unit_charge,
                            voltage_r, voltage_y, voltage_b,
                            current_r, current_y, current_b,
                            pf,
                            signal,
                            error,
                            created_at,
                            updated_at
                        ) VALUES (
                            %s, NOW(),
                            %s, %s, %s, %s, %s,
                            %s, %s, %s,
                            %s, %s, %s,
                            %s,
                            %s,
                            %s,
                            NOW(), NOW()
                        )
                    """

                    values = (
                        int(device_id),

                        _num(final_row.get("eb_terriff_setting")),
                        _num(final_row.get("cum_eb_kwh")),
                        _num(final_row.get("total_kw")),
                        _num(final_row.get("balance_amount")),
                        _num(final_row.get("eb_terriff_setting")),

                        _num(final_row.get("voltage_r")),
                        _num(final_row.get("voltage_y")),
                        _num(final_row.get("voltage_b")),
                        _num(final_row.get("current_r")),
                        _num(final_row.get("current_y")),
                        _num(final_row.get("current_b")),

                        _num(final_row.get("pf")),
                        str(final_row.get("signal")) if final_row.get("signal") is not None else None,
                        final_row.get("error"),
                    )

                    cur.execute(sql, values)
                    log.info("device_gateway_data INSERTED device_id=%s", device_id)

            conn.commit()
        finally:
            conn.close()

        return True

    except Exception:
        log.exception("DB insert/update failed (device_gateway_data)")
        return False


# =========================
# Message handling
# =========================
def handle_iot_message(iot_json: dict):
    if is_duplicate_message(iot_json):
        log.info("Duplicate message skipped device_id=%s timestamp=%s",
                 iot_json.get("device_id"), iot_json.get("timestamp"))
        return

    gateway_str = iot_json.get("gateway", "")
    if not gateway_str:
        log.warning("Missing 'gateway' field in message")
        return

    params = decode_gateway_to_params(gateway_str)
    print("decode data 11")
    print(params)

    if not params:
        snippet = str(gateway_str)[:60]
        log.warning("Gateway decode returned empty params. gateway_snippet=%s", snippet)
        return

    hex_data = params

    parsed_hex = parse_hex_code(hex_data)
    if not parsed_hex:
        log.warning("Hex parse returned empty parsed data")
        return

    # 🔒 Skip corrupted frames (the '����' case)
    if not parsed_hex.get("header") or parsed_hex.get("cum_eb_kwh") is None:
        log.warning("Invalid hex frame skipped device_id=%s", iot_json.get("device_id"))
        return

    final_row = {}
    for k in ("device_id", "battery", "signal", "status", "timestamp"):
        if k in iot_json:
            final_row[k] = iot_json[k]

    print("decode data 12")

    final_row.update(parsed_hex)
    print(final_row)

    # 1) Insert logs first
    ok = insert_to_db_safe(final_row)
    if not ok:
        return

    log.info(
        "Logs Inserted OK device_id=%s imei=%s total_kw=%s balance=%s",
        iot_json.get("device_id"),
        final_row.get("imei") or final_row.get("device_id"),
        final_row.get("total_kw"),
        final_row.get("balance_amount"),
    )

    # 2) After logs insert, insert into device_gateway_data
    ok2 = insert_device_gateway_data_real(final_row)
    if ok2:
        log.info(
            "device_gateway_data Inserted OK imei=%s load=%s balance=%s",
            final_row.get("imei") or final_row.get("device_id"),
            _num(final_row.get("total_kw")),
            _num(final_row.get("balance_amount")),
        )


def on_message(client, userdata, message):
    try:
        payload_text = message.payload.decode("utf-8", errors="replace")
        iot_json = json.loads(payload_text)
        print(iot_json)
    except Exception:
        log.exception("Invalid JSON received on topic=%s", getattr(message, "topic", ""))
        return

    try:
        handle_iot_message(iot_json)
    except Exception:
        log.exception("Unhandled error while processing message")


def build_client() -> AWSIoTMQTTClient:
    mqtt = AWSIoTMQTTClient(CLIENT_ID)
    mqtt.configureEndpoint(ENDPOINT, 8883)
    mqtt.configureCredentials(ROOT_CA, PRIVATE_KEY, CERT_FILE)

    mqtt.configureAutoReconnectBackoffTime(1, 32, 20)
    mqtt.configureConnectDisconnectTimeout(10)
    mqtt.configureMQTTOperationTimeout(10)
    mqtt.configureOfflinePublishQueueing(-1)
    mqtt.configureDrainingFrequency(2)
    return mqtt


def main():
    mqtt = build_client()

    while True:
        try:
            log.info("Connecting to AWS IoT...")
            mqtt.connect()
            log.info("Connected. Subscribing to %s", TOPIC)
            mqtt.subscribe(TOPIC, 1, on_message)

            while True:
                time.sleep(1)

        except KeyboardInterrupt:
            log.info("Stopping by user interrupt...")
            try:
                mqtt.disconnect()
            except Exception:
                pass
            return

        except Exception:
            log.exception("Main loop error; will retry connect in 5 seconds")
            time.sleep(5)


if __name__ == "__main__":
    main()
