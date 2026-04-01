#!/usr/bin/env python3
"""
ble_test_tool.py - Interactive BLE diagnostic and test tool
============================================================

Tests:
  1. Scan  - Find all nearby BLE devices broadcasting REL* or ESP_SPP_SERVER
  2. Info  - Connect and read: device name, site details, products, shift, status
  3. Preset - Send a pump preset (amount or volume) to a nozzle
  4. Txns  - Fetch last 3 transactions for a pump

Usage (run on RPi or any machine with BLE + bleak installed):
    python3 ble_test_tool.py

Requirements:
    pip install bleak
"""

import asyncio
import struct
import sys
from typing import Optional

# -- Try importing bleak -------------------------------------------------------

try:
    from bleak import BleakScanner, BleakClient
except ImportError:
    print("\n[ERROR] bleak not installed. Run:  pip install bleak\n")
    sys.exit(1)

# -- Protocol constants --------------------------------------------------------

SERVICE_UUID     = "0000abf0-0000-1000-8000-00805f9b34fb"
WRITE_CHAR_UUID  = "0000abf1-0000-1000-8000-00805f9b34fb"
NOTIFY_CHAR_UUID = "0000abf2-0000-1000-8000-00805f9b34fb"

# Short 16-bit UUID forms as reported by CoreBluetooth on macOS
# (bleak on macOS returns "ABF0" not the full 128-bit form)
SERVICE_UUID_SHORT  = "abf0"
WRITE_UUID_SHORT    = "abf1"
NOTIFY_UUID_SHORT   = "abf2"
CHUNK_MARKER     = 0x23
ACK              = 0x01
NACK_SERVICE_OFFLINE = 0x97
NACK_TIMEOUT         = 0x98

# All known advertising name prefixes for fuel controllers.
# adv_prefix is configurable in config.yaml (e.g. "REL", "IOT").
# NOTE: must be a tuple (trailing comma) - NOT a bare string.
BLE_ADV_PREFIXES  = ("REL", "IOT")
FALLBACK_ADV_NAME = "ESP_SPP_SERVER"

PUMP_STATE_MAP = {
    0: "Idle",        1: "Calling",       2: "Auth pending",
    3: "Fuelling",    4: "Idle",          5: "Offline",
    6: "Locked",      7: "Stopped",       8: "Closed",
    9: "Error",      11: "Paused",       13: "Auth",
   14: "Auth",
}

PRESET_ERROR_MAP = {
    0:  "FAILED",               1:  "SUCCESS",
    4:  "Invalid nozzle",       5:  "Nozzle lock/interlock",
    6:  "Pump busy",            8:  "Invalid pump",
    11: "Error",               12:  "Auth store fail",
    14: "Auth generation fail", 15: "Invalid data",
    16: "Already preseted",    17:  "Locked by other device",
}

MOP_MAP = {
    1: "Cash",        2: "Credit Card",    3: "Local Account",
    4: "Loyalty",     5: "Wallet",         6: "Fleet POS",
    7: "Testing",     8: "Sampling",       9: "Own Use",
   10: "Others",     11: "Debit Card",    22: "Mobile Wallet",
   24: "UPI",        50: "Mobile App",
}


# -- CRC -----------------------------------------------------------------------

def modbus_crc16(data: bytes) -> int:
    crc = 0xFFFF
    for byte in data:
        crc ^= byte
        for _ in range(8):
            if crc & 1:
                crc = (crc >> 1) ^ 0xA001
            else:
                crc >>= 1
    return crc


# -- Packet builder ------------------------------------------------------------

def build_request(cmd: int, data: bytes = b"") -> bytes:
    inner   = bytes([cmd, ACK]) + data
    length  = len(inner)
    payload = struct.pack("<H", length) + inner
    crc     = modbus_crc16(payload)
    return payload + struct.pack("<H", crc)


def chunk_packet(packet: bytes, mtu: int) -> list:
    if len(packet) <= mtu:
        return [packet]
    cs    = mtu - 4
    total = (len(packet) // cs) + 1
    chunks = []
    for i in range(total):
        chunk = packet[i * cs:(i + 1) * cs]
        if not chunk:
            break
        chunks.append(bytes([CHUNK_MARKER, CHUNK_MARKER, total, i + 1]) + chunk)
    return chunks


# -- Packet parser -------------------------------------------------------------

def parse_response(raw: bytes):
    """Returns (cmd, ack, data) or (None, None, None) on error."""
    if len(raw) < 6:
        return None, None, None
    length   = struct.unpack("<H", raw[0:2])[0]
    expected = length + 4
    if len(raw) < expected:
        return None, None, None
    payload  = raw[:length + 2]
    recv_crc = struct.unpack("<H", raw[length + 2:length + 4])[0]
    calc_crc = modbus_crc16(payload)
    if recv_crc != calc_crc:
        print(f"  [CRC ERROR] recv=0x{recv_crc:04X}  calc=0x{calc_crc:04X}")
        return None, None, None
    return raw[2], raw[3], raw[4:length + 2]


# -- Chunk assembler -----------------------------------------------------------

class ChunkAssembler:
    def __init__(self):
        self._total = 0
        self._parts = {}

    def feed(self, raw: bytes) -> Optional[bytes]:
        if len(raw) >= 2 and raw[0] == CHUNK_MARKER and raw[1] == CHUNK_MARKER:
            total   = raw[2]
            current = raw[3]
            self._total = total
            self._parts[current] = raw[4:]
            if len(self._parts) == self._total:
                assembled = b"".join(self._parts[i] for i in range(1, self._total + 1))
                self._total = 0
                self._parts = {}
                return assembled
            return None
        self._total = 0
        self._parts = {}
        return raw


# -- BLE send/receive helper ---------------------------------------------------

async def ble_send(client: BleakClient, cmd: int, data: bytes = b"", timeout: float = 8.0):
    """Write a BLE command and wait for the notify response.
    Returns (cmd, ack, data) or (None, None, None) on error/timeout.
    """
    assembler = ChunkAssembler()
    loop      = asyncio.get_event_loop()
    result    = loop.create_future()

    def on_notify(_char, raw):
        assembled = assembler.feed(bytes(raw))
        if assembled and not result.done():
            result.set_result(assembled)

    await client.start_notify(NOTIFY_CHAR_UUID, on_notify)

    packet = build_request(cmd, data)
    mtu    = getattr(client, '_mtu_size', 23) or 23
    for chunk in chunk_packet(packet, mtu):
        await client.write_gatt_char(WRITE_CHAR_UUID, chunk, response=False)
        await asyncio.sleep(0.02)

    try:
        raw = await asyncio.wait_for(result, timeout=timeout)
    except asyncio.TimeoutError:
        print(f"  [TIMEOUT] No response for cmd=0x{cmd:02X} after {timeout:.0f}s")
        await client.stop_notify(NOTIFY_CHAR_UUID)
        return None, None, None

    await client.stop_notify(NOTIFY_CHAR_UUID)
    return parse_response(raw)


# -- Display helpers -----------------------------------------------------------

def sep(title: str = ""):
    w = 60
    if title:
        side = (w - len(title) - 2) // 2
        print(f"\n{'-' * side} {title} {'-' * (w - side - len(title) - 2)}")
    else:
        print("-" * w)

def ok(msg):   print(f"  [OK]  {msg}")
def err(msg):  print(f"  [!!]  {msg}")
def info(msg): print(f"        {msg}")


# =============================================================================
#  TEST 1 - BLE SCAN
# =============================================================================

def _is_fuel_device(name: str) -> bool:
    """Return True if the advertising name belongs to a fuel controller."""
    return any(name.startswith(p) for p in BLE_ADV_PREFIXES) or name == FALLBACK_ADV_NAME


def _has_fuel_uuid(service_uuids) -> bool:
    """Return True if the advertisement contains our service UUID.

    On macOS CoreBluetooth, bleak returns 16-bit UUIDs in SHORT form:
      "ABF0"  not  "0000abf0-0000-1000-8000-00805f9b34fb"
    We match both forms so the device is found regardless of platform.
    """
    if not service_uuids:
        return False
    for u in service_uuids:
        u_norm = u.lower().replace("-", "")
        # match short form "abf0" or full 128-bit without dashes
        if u_norm == SERVICE_UUID_SHORT or SERVICE_UUID.replace("-", "") in u_norm:
            return True
    return False


def _decode_du_raw(du_raw: str) -> str:
    """Decode the DU portion of the advertising name into readable DU numbers.

    Handles two encoding formats:
      BCD  (RPi ble_controller.py): each byte is a packed BCD DU number.
           e.g. b'\\x03\\x04' -> "DU-03, DU-04"
      ASCII (legacy FCC hardware): 2-char ASCII pairs.
           e.g. "3404"         -> "DU-34, DU-04"
    """
    du_labels = []
    raw_bytes = du_raw.encode('latin-1')
    for b in raw_bytes:
        if b < 0x20:
            # Non-printable byte - BCD encoded DU number
            high = (b >> 4) & 0x0F
            low  = b & 0x0F
            du_labels.append(f"DU-{high:01d}{low:01d}")
        else:
            # Printable ASCII - collect 2 chars per DU number
            du_labels = []
            for j in range(0, len(du_raw), 2):
                chunk = du_raw[j:j+2]
                if chunk:
                    du_labels.append(f"DU-{chunk}")
            break
    return ", ".join(du_labels) if du_labels else "none"


async def test_scan(scan_secs: int = 10):
    sep("BLE SCAN")
    prefixes_str = "/".join(f"{p}*" for p in BLE_ADV_PREFIXES)
    print(f"  Scanning {scan_secs}s for {prefixes_str} / {FALLBACK_ADV_NAME} devices ...")
    print(f"  (also matching service UUID {SERVICE_UUID})\n")

    # -- Pass 1: active scan - sends SCAN_REQ to get SCAN_RSP name -------------
    # bleak default is PASSIVE on Linux (no SCAN_REQ → never sees SCAN_RSP name).
    # Force ACTIVE so device name arrives even when only in SCAN_RSP.
    # Also supply service_uuids so devices that include the UUID in ADV_IND are
    # found even when the name is still hidden in SCAN_RSP.
    try:
        discovered = await BleakScanner.discover(
            timeout=scan_secs,
            return_adv=True,
            scanning_mode="active",
        )
    except TypeError:
        # Older bleak versions don't accept scanning_mode - fall back gracefully
        discovered = await BleakScanner.discover(timeout=scan_secs, return_adv=True)

    fuel_devs   = []
    rssi_map    = {}   # address -> rssi (from AdvertisementData)
    other_devs  = []

    for d, adv in discovered.values():
        name = d.name or adv.local_name or ""
        # Match by name prefix  OR  by our service UUID (handles both short
        # "ABF0" form from macOS CoreBluetooth and full 128-bit form from Linux)
        if _is_fuel_device(name) or _has_fuel_uuid(adv.service_uuids):
            fuel_devs.append(d)
            rssi_map[d.address] = adv.rssi
        else:
            other_devs.append((d, adv))

    if not fuel_devs:
        err("No FCC fuel devices found nearby.")
        print(f"  ({len(other_devs)} other BLE device(s) in range, none matched REL/IOT prefix or UUID ABF0)")
        print()
        print("  Diagnostics:")
        print("    RPi BT address : 88:A2:9E:9D:1D:D5")
        print("    RPi adv name   : REL222459 34  (in SCAN_RSP)")
        print("    RPi service    : UUID 0xABF0   (in ADV_IND)")
        print()
        print("  Common causes:")
        print("    - Mac is not close enough to RPi (BT range ~10m open space)")
        print("    - RPi is in a metal enclosure blocking BT signal")
        print("    - macOS BT cache: toggle Mac BT off/on and retry")
        print()
        if other_devs:
            print(f"  Scanning {len(other_devs)} other device(s) for RPi address 88:A2:9E:9D:1D:D5 ...")
            # Check by raw address (on Linux bleak shows real MAC; macOS shows UUID)
            rpi_addr = "88:A2:9E:9D:1D:D5".lower()
            rpi_match = [(d, adv) for d, adv in other_devs
                         if d.address.lower() == rpi_addr or
                         any("abf0" in u.lower() for u in (adv.service_uuids or []))]
            if rpi_match:
                print()
                ok("RPi found by address/UUID even though name did not match!")
                for d, adv in rpi_match:
                    name = d.name or adv.local_name or "(name not received)"
                    print(f"  Address : {d.address}  RSSI={adv.rssi:+d}")
                    print(f"  Name    : {name}")
                    print(f"  UUIDs   : {adv.service_uuids}")
                    fuel_devs.append(d)
                    rssi_map[d.address] = adv.rssi
                if fuel_devs:
                    return fuel_devs  # found via address fallback
            else:
                show = input("\n  Show ALL discovered devices for debugging? [y/N]: ").strip().lower()
                if show == "y":
                    sep("ALL DEVICES (debug dump)")
                    all_sorted = sorted(other_devs, key=lambda x: -(x[1].rssi or -100))
                    for d, adv in all_sorted:
                        name = d.name or adv.local_name or "(unnamed)"
                        uuids = ", ".join(adv.service_uuids) if adv.service_uuids else "none"
                        print(f"  {d.address}  RSSI={adv.rssi:+d:4}  {name!r}")
                        if adv.service_uuids:
                            print(f"    UUIDs: {uuids}")
        return None

    print(f"  Found {len(fuel_devs)} fuel controller(s):\n")
    for i, d in enumerate(fuel_devs):
        name = d.name or "(name hidden in SCAN_RSP - use active scan)"
        rssi = rssi_map.get(d.address, 0)

        matched_prefix = next((p for p in BLE_ADV_PREFIXES if name.startswith(p)), None)
        if matched_prefix and len(name) > len(matched_prefix):
            rest   = name[len(matched_prefix):]
            parts  = rest.split(" ", 1)
            sap    = parts[0]
            du_raw = parts[1] if len(parts) > 1 else ""
            du_str = _decode_du_raw(du_raw) if du_raw else "none"
            bound_str = f"BOUND   SAP/RO={sap}   DUs=[{du_str}]"
        elif name == FALLBACK_ADV_NAME:
            bound_str = "UNBOUND (factory default - not configured)"
        elif "(name hidden" in name:
            bound_str = "Found via service UUID 0xABF0 (name in SCAN_RSP only)"
        else:
            bound_str = "Unknown format"

        # Display printable portion of name only (BCD bytes are non-printable)
        try:
            display_name = name.encode('latin-1').decode('ascii', errors='replace')
        except Exception:
            display_name = name
        rssi_label = 'strong' if rssi > -60 else 'moderate' if rssi > -75 else 'weak'
        print(f"  [{i}]  Adv Name : {display_name}")
        print(f"       Address  : {d.address}")
        print(f"       RSSI     : {rssi:+d} dBm  ({rssi_label})")
        print(f"       Status   : {bound_str}")
        print()

    return fuel_devs


# =============================================================================
#  TEST 2 - DEVICE INFO
# =============================================================================

async def test_info(address: str):
    sep(f"DEVICE INFO")
    print(f"  Connecting to {address} ...")

    async with BleakClient(address, timeout=10) as client:
        if not client.is_connected:
            err("Connection failed")
            return
        ok(f"Connected to {address}")
        print()

        # -- Site Details (0x04) -----------------------------------------------
        cmd, ack, data = await ble_send(client, 0x04)
        if ack == ACK and data and len(data) >= 32:
            site_code = data[0:12].decode("ascii", errors="ignore").rstrip("\x00 ")
            site_name = data[12:32].decode("ascii", errors="ignore").rstrip("\x00 ")
            ok(f"Site Code   : {site_code}")
            ok(f"Site Name   : {site_name}")
        else:
            err(f"Site details failed  ack=0x{ack:02X}" if ack is not None else "Site details: no response")
        print()

        # -- Product Details (0x06) --------------------------------------------
        cmd, ack, data = await ble_send(client, 0x06)
        if ack == ACK and data:
            idx   = 0
            count = data[idx]; idx += 1
            ok(f"Products    : {count} product(s) configured")
            for _ in range(count):
                if idx + 15 > len(data):
                    break
                pid       = data[idx]; idx += 1
                price_raw = struct.unpack(">I", data[idx:idx + 4])[0]; idx += 4
                name      = data[idx:idx + 10].decode("ascii", errors="ignore").rstrip("\x00 "); idx += 10
                info(f"  ID={pid}  Name={name!r:<12}  Price=Rs.{price_raw/100:.2f}/L")
        else:
            err("Product details: no response")
        print()

        # -- Shift Status (0x18) -----------------------------------------------
        cmd, ack, data = await ble_send(client, 0x18)
        if ack == ACK and data and len(data) >= 11:
            shift_status = data[0]
            shift_no     = struct.unpack(">I", data[1:5])[0]
            yr, mo, dy   = data[5], data[6], data[7]
            hr, mn, sc   = data[8], data[9], data[10]
            s_str        = "Running" if shift_status == 1 else f"code=0x{shift_status:02X}"
            ok(f"Shift       : #{shift_no}  Status={s_str}  "
               f"Started {yr+2000:04d}-{mo:02d}-{dy:02d} {hr:02d}:{mn:02d}:{sc:02d}")
        else:
            err("Shift status: no response")
        print()

        # -- Get Status all pumps (0x01) ---------------------------------------
        cmd, ack, data = await ble_send(client, 0x01)
        if ack == ACK and data:
            idx      = 0
            fp_count = data[idx]; idx += 1
            ok(f"Pump Status : {fp_count} FP(s) found")
            for _ in range(fp_count):
                if idx + 14 > len(data):
                    break
                fp_no     = data[idx]; idx += 1
                pump_st   = data[idx]; idx += 1
                active_nz = data[idx]; idx += 1
                vol_raw   = struct.unpack(">I", data[idx:idx + 4])[0]; idx += 4
                amt_raw   = struct.unpack(">I", data[idx:idx + 4])[0]; idx += 4
                pri_raw   = struct.unpack(">I", data[idx:idx + 4])[0]; idx += 4
                nz_count  = data[idx]; idx += 1
                nozzles   = []
                for _ in range(nz_count):
                    if idx + 3 > len(data):
                        break
                    nz_no  = data[idx]; idx += 1
                    nz_st  = data[idx]; idx += 1
                    prod   = data[idx]; idx += 1
                    state  = PUMP_STATE_MAP.get(nz_st, f"0x{nz_st:02X}")
                    nozzles.append(f"Nz{nz_no}[{state},prod={prod}]")
                state_str = PUMP_STATE_MAP.get(pump_st, f"0x{pump_st:02X}")
                info(f"  FP{fp_no}  State={state_str:<16}  ActiveNz={active_nz}  "
                     f"Vol={vol_raw/100:.3f}L  Amt=Rs.{amt_raw/100:.2f}  "
                     f"Price=Rs.{pri_raw/100:.2f}/L")
                if nozzles:
                    info(f"       Nozzles: {', '.join(nozzles)}")
        else:
            err(f"Get status failed  ack=0x{ack:02X}" if ack is not None else "Get status: no response")


# =============================================================================
#  TEST 3 - SEND PRESET
# =============================================================================

async def test_preset(address: str, pump_no: int, nozzle_no: int,
                      preset_type: int, preset_value: float,
                      mop: int = 1, txn_id: str = ""):
    """
    preset_type : 1=Volume (litres)  2=Amount (INR)
    mop         : 1=Cash  2=Credit  24=UPI  50=App
    """
    sep("SEND PRESET")
    type_str = "Volume" if preset_type == 1 else "Amount"
    unit_str = "L"      if preset_type == 1 else "Rs."
    print(f"  FP={pump_no}  Nozzle={nozzle_no}  "
          f"Type={type_str}  Value={unit_str}{preset_value:.2f}  "
          f"MOP={MOP_MAP.get(mop, mop)}")
    if txn_id:
        info(f"Payment TXN ID: {txn_id}")
    print()

    preset_raw    = int(preset_value * 100)
    mobile_bytes  = b" " * 13
    vehicle_bytes = b" " * 10
    trx_id_bytes  = txn_id.encode("ascii").ljust(20)[:20]

    payload = (
        bytes([pump_no, nozzle_no, preset_type])
        + struct.pack(">I", preset_raw)
        + bytes([mop, 0x07])    # mop + payment_type=Other
        + mobile_bytes
        + vehicle_bytes
        + bytes([0x04])         # vehicle_type=4W default
        + trx_id_bytes
        + struct.pack(">H", 0)  # random_number
    )

    print(f"  Connecting to {address} ...")
    async with BleakClient(address, timeout=10) as client:
        if not client.is_connected:
            err("Connection failed")
            return

        ok("Connected - sending preset command (0x02) ...")
        cmd, ack, data = await ble_send(client, 0x02, payload, timeout=10)

        if ack is None:
            err("No response from device (timeout)")
            return

        if ack == ACK and data and len(data) >= 5:
            pump_back  = data[0]
            error_code = data[1]
            rand_num   = struct.unpack(">H", data[2:4])[0]
            auth_num   = struct.unpack(">H", data[4:6])[0] if len(data) >= 6 else 0
            error_str  = PRESET_ERROR_MAP.get(error_code, f"code=0x{error_code:02X}")

            if error_code == 1:
                ok(f"PRESET SUCCESS")
                info(f"Pump returned : FP{pump_back}")
                info(f"Auth Number   : {auth_num}  (use this to fetch the transaction)")
                info(f"Random Number : {rand_num}")
                print()
                info("Next steps:")
                info("  1. Attendant lifts the nozzle -> fuelling starts automatically")
                info("  2. After nozzle replaced -> fetch transaction with auth number above")
            else:
                err(f"PRESET FAILED : {error_str}")
                info(f"Raw: pump={pump_back}  error=0x{error_code:02X}  "
                     f"rand={rand_num}  auth={auth_num}")

        elif ack == NACK_SERVICE_OFFLINE:
            err("NACK: Service offline (FCC not ready / shift not started)")
        elif ack == NACK_TIMEOUT:
            err("NACK: Timeout (FCC busy - retry in a few seconds)")
        else:
            err(f"Unexpected response: ack=0x{ack:02X}  data={data.hex().upper() if data else 'None'}")


# =============================================================================
#  TEST 4 - LAST 3 TRANSACTIONS
# =============================================================================

async def test_transactions(address: str, pump_no: int):
    sep(f"LAST 3 TRANSACTIONS  FP{pump_no}")
    print(f"  Connecting to {address} ...")

    # Cmd 0x0A payload: pump_number (1 byte) + 6 padding bytes
    payload = bytes([pump_no]) + b"\x00" * 6

    async with BleakClient(address, timeout=10) as client:
        if not client.is_connected:
            err("Connection failed")
            return

        ok("Connected - fetching transactions (cmd 0x0A) ...")
        cmd, ack, data = await ble_send(client, 0x0A, payload, timeout=10)

        if ack is None:
            err("No response (timeout)")
            return

        if ack == NACK_SERVICE_OFFLINE:
            err("NACK: Service offline")
            return

        if ack != ACK:
            err(f"NACK ack=0x{ack:02X}")
            return

        if not data or len(data) < 2:
            err("Empty response data")
            return

        count = data[1]
        info(f"Device reports {count} transaction(s) for FP{pump_no}")
        print()

        if count == 0:
            info("No transactions found. Has a sale been completed on this pump?")
            return

        RECORD_SIZE = 19
        idx = 2
        for i in range(count):
            if idx + RECORD_SIZE > len(data):
                break
            rec = data[idx:idx + RECORD_SIZE]
            idx += RECORD_SIZE

            pump_n = rec[0]
            nz_n   = rec[1]
            yr     = rec[2] + 2000
            mo     = rec[3]
            dy     = rec[4]
            uid    = struct.unpack(">H", rec[5:7])[0]
            hr     = rec[7]
            mn     = rec[8]
            sc     = rec[9]
            vol    = struct.unpack(">I", rec[10:14])[0] / 100.0
            amt    = struct.unpack(">I", rec[14:18])[0] / 100.0
            prod   = rec[18] if len(rec) > 18 else 0

            print(f"  -- Transaction {i + 1} of {count} ----------------------")
            info(f"Date / Time   : {yr:04d}-{mo:02d}-{dy:02d}  {hr:02d}:{mn:02d}:{sc:02d}")
            info(f"Pump / Nozzle : FP{pump_n} / Nz{nz_n}")
            info(f"Volume        : {vol:.3f} L")
            info(f"Amount        : Rs.{amt:.2f}")
            info(f"Product ID    : {prod}")
            info(f"Unique ID     : {uid}")
            print()


# =============================================================================
#  INTERACTIVE MENU
# =============================================================================

async def main():
    print()
    print("=" * 60)
    print("  BLE FCC Test Tool  |  Fuel Controller Diagnostic")
    print("  Protocol: BLE GATT (ABF0/ABF1/ABF2)  v2.5")
    print("=" * 60)

    selected_address = None

    while True:
        sep("MENU")
        addr_display = f"  (device: {selected_address})" if selected_address else "  (no device selected)"
        print(f"  1  Scan       - find nearby BLE fuel controllers{addr_display}")
        print(f"  2  Info       - site details, products, shift, pump status")
        print(f"  3  Preset     - send amount/volume preset to a nozzle")
        print(f"  4  Transactions - fetch last 3 transactions for a pump")
        print(f"  5  Set address - enter MAC address manually")
        print(f"  6  Scan ALL   - raw dump of every visible BLE device (diagnostic)")
        print(f"  q  Quit")
        sep()

        choice = input("\n  Choice [1/2/3/4/5/6/q]: ").strip().lower()

        if choice == "q":
            print("\n  Done.\n")
            break

        elif choice == "1":
            secs_str = input("  Scan duration seconds [10]: ").strip()
            secs     = int(secs_str) if secs_str.isdigit() else 10
            devices  = await test_scan(secs)
            if devices:
                idx_str = input(f"\n  Select device index [0-{len(devices)-1}] (Enter to skip): ").strip()
                if idx_str.isdigit() and int(idx_str) < len(devices):
                    selected_address = devices[int(idx_str)].address
                    ok(f"Selected: {devices[int(idx_str)].name}  ({selected_address})")

        elif choice == "5":
            addr = input("  Enter MAC address (e.g. AA:BB:CC:DD:EE:FF): ").strip()
            if addr:
                selected_address = addr
                ok(f"Address set to: {selected_address}")

        elif choice == "6":
            secs_str = input("  Scan duration seconds [10]: ").strip()
            secs     = int(secs_str) if secs_str.isdigit() else 10
            sep("RAW BLE SCAN (all devices)")
            print(f"  Scanning {secs}s - showing EVERY device bleak sees ...\n")
            try:
                raw_devs = await BleakScanner.discover(
                    timeout=secs, return_adv=True, scanning_mode="active"
                )
            except TypeError:
                raw_devs = await BleakScanner.discover(timeout=secs, return_adv=True)
            if not raw_devs:
                err("No BLE devices found at all - check Bluetooth is on")
            else:
                all_sorted = sorted(raw_devs.values(), key=lambda x: -(x[1].rssi or -100))
                print(f"  {len(all_sorted)} device(s) found:\n")
                for d, adv in all_sorted:
                    name = d.name or adv.local_name or "(unnamed)"
                    uuids = ", ".join(adv.service_uuids) if adv.service_uuids else "none"
                    rssi = adv.rssi or 0
                    fuel_marker = " <-- FUEL" if (_is_fuel_device(name) or _has_fuel_uuid(adv.service_uuids)) else ""
                    rpi_marker  = " <-- RPi?" if "88:A2:9E:9D:1D:D5".lower() in d.address.lower() else ""
                    print(f"  {d.address}  RSSI={rssi:+d}  {name!r}{fuel_marker}{rpi_marker}")
                    if adv.service_uuids:
                        print(f"    UUIDs: {uuids}")
                print()
                # Ask if user wants to select one
                idx_str = input("  Enter index to select (or Enter to skip): ").strip()
                if idx_str.isdigit():
                    idx = int(idx_str)
                    if 0 <= idx < len(all_sorted):
                        d, adv = all_sorted[idx]
                        selected_address = d.address
                        ok(f"Selected: {d.name or adv.local_name or '(unnamed)'}  ({selected_address})")

        elif choice in ("2", "3", "4"):
            if not selected_address:
                addr = input("  No device selected. Enter MAC address: ").strip()
                if not addr:
                    err("Address required. Run scan first (option 1).")
                    continue
                selected_address = addr

            if choice == "2":
                await test_info(selected_address)

            elif choice == "3":
                print(f"\n  Device : {selected_address}")
                pump_str   = input("  Pump (FP) number      [1]: ").strip()
                pump_no    = int(pump_str)   if pump_str.isdigit()   else 1
                nozzle_str = input("  Nozzle number         [1]: ").strip()
                nozzle_no  = int(nozzle_str) if nozzle_str.isdigit() else 1

                type_str   = input("  Preset type  1=Volume  2=Amount  [2]: ").strip()
                preset_type = int(type_str) if type_str in ("1", "2") else 2

                unit = "litres" if preset_type == 1 else "INR"
                val_str = input(f"  Preset value ({unit}): ").strip()
                try:
                    preset_value = float(val_str)
                except ValueError:
                    err("Invalid number")
                    continue

                print("  MOP options: 1=Cash  2=Card  24=UPI  50=App")
                mop_str = input("  MOP [1]: ").strip()
                mop     = int(mop_str) if mop_str.isdigit() else 1

                txn_id = input("  Payment TXN ID (optional, Enter to skip): ").strip()

                await test_preset(selected_address, pump_no, nozzle_no,
                                  preset_type, preset_value, mop, txn_id)

            elif choice == "4":
                pump_str = input("  Pump (FP) number [1]: ").strip()
                pump_no  = int(pump_str) if pump_str.isdigit() else 1
                await test_transactions(selected_address, pump_no)

        else:
            err("Invalid choice - enter 1, 2, 3, 4, 5, 6 or q")


if __name__ == "__main__":
    asyncio.run(main())
