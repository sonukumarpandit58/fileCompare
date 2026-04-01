"""
ble_protocol.py - BLE Protocol v2.5 packet builder and parser
=============================================================

Reference: BLE-PROTOCOL v2.5, RELCON SYSTEMS, 30/07/2021

PURPOSE
-------
Lowest-level BLE protocol layer. Handles ONLY the serialisation /
deserialisation of BLE protocol frames. Contains no I/O, no threading,
no business logic, and no hardware access.

All public functions accept plain Python integers / floats / bytes and return
typed dataclass instances or raw bytes. Nothing in this file does I/O.

PACKET FORMAT (Normal case, total size T <= MTU M)
--------------------------------------------------
  [Length L (2 Bytes)] [Command No (1 Byte)] [ACK/NACK (1 Byte)]
  [Data (N Bytes)] [Modbus CRC-16 (2 Bytes)]

  Where:
    L   = 1 (Command No) + 1 (ACK/NACK) + N (Data bytes)
    T   = 2 (Length field) + L + 2 (CRC field) = L + 4
    CRC = Modbus CRC-16 calculated over bytes [0 .. L+1] (Length..Data)

PACKET FORMAT (Chunked, T > MTU)
---------------------------------
  Triggered when first two bytes of a received message are 0x23, 0x23.
  Total PKT count = (T / M) + 1  where M = negotiated MTU size.

  Each chunk:
    [0x23 (1B)] [0x23 (1B)] [Total PKT Count (1B)] [Current PKT No (1B)]
    [Chunk Data ((M-4) bytes  OR  (T % M) bytes for last chunk)]

  PacketAssembler.feed() reassembles all chunks into the original packet.

ACK / NACK VALUES
-----------------
  In requests  : ACK/NACK field = 0x01
  In responses :
    0x01 = ACK (success)
    0x97 = NACK - Service offline
    0x98 = NACK - Timeout (retry same request)

CRC ALGORITHM
-------------
  Modbus CRC-16: polynomial 0x8005, initial value 0xFFFF, reflected in/out.
  Applied over all bytes from Length field up to (and including) Data field.

NUMERIC ENCODING
----------------
  Volume, Amount, Price are transmitted as integer * 100.
  e.g. 60.59 litres -> 6059 -> 0x000017AB
  Divide received value by 100 to get actual value.

VALUE ENCODING IN FRAMES
-------------------------
  Multi-byte integers are big-endian (e.g. 4-byte volume field).
  The 2-byte Length field is little-endian (as seen in doc examples).
  CRC is little-endian.

COMMANDS COVERED (31 total, v2.5)
----------------------------------
  0x01  Get Status
  0x02  Pump Preset
  0x03  Get Last Transaction
  0x04  Get Site Details
  0x05  Get Receipt Header and Footer
  0x06  Get Product Details
  0x07  Mop Change
  0x09  Pump Locking
  0x0A  Get Last 3 Transactions
  0x15  Get Last Transaction Extended
  0x16  Get Last 3 Transactions Extended
  0x17  Mop Change Extended
  0x18  Shift Status
  0x19  Shift End
  0x1A  Fetch Lube Details
  0x1B  Lube Sale
  0x1C  Lube Stock Update
  0x1D  Get Local Account Details
  0x1E  Local Account Payment
  0x1F  Update Product Price
  0x20  Pump Preset Extended
  0x21  Get Attendant Details
  0x22  Terminal Status Update
  0x23  Fetch Transaction By Transaction ID
  0x24  RO Master Data
  0x25  Detailed Product Configuration
  0x26  Get Last 5 Transactions Extended
  0x27  Mop Change Extended With Extra Fields
  0x28  Preset Extended With Extra Field
  0x29  Get Last Transaction Extended With Extra Field
  0x2A  Pump Status (Remote Pump)
"""

import struct
import logging
from enum import IntEnum
from dataclasses import dataclass, field
from typing import Optional, List, Tuple

logger = logging.getLogger(__name__)


# ── GATT UUIDs ────────────────────────────────────────────────────────────────

# 16-bit short UUIDs expanded to full 128-bit Bluetooth Base UUID format
SERVICE_UUID     = "0000abf0-0000-1000-8000-00805f9b34fb"
WRITE_CHAR_UUID  = "0000abf1-0000-1000-8000-00805f9b34fb"   # client -> server
NOTIFY_CHAR_UUID = "0000abf2-0000-1000-8000-00805f9b34fb"   # server -> client (notify)

# ── BLE Advertising Name Format (Section 1.2, BLE-PROTOCOL v2.5) ─────────────
#
#   State 1 — FCC bound to site (normal operation):
#     Format  : PREFIX + SAPCODE + Space + DUNO1 + DUNO2 + ...
#     Prefix  : "REL"           (3 bytes ASCII,  fixed)
#     SAPCODE : e.g. "222459"   (site SAP/RO code, ASCII bytes)
#     Space   : " "             (1 byte, ASCII 0x20 separator)
#     DUNOs   : 2-digit DU numbers concatenated as ASCII bytes
#               "05" = 0x30 0x35
#               "06" = 0x30 0x36
#     Example : "REL222459 0506"
#                          ^ space (0x20)
#                           ^^^^ DU-05 (0x30 0x35) + DU-06 (0x30 0x36)
#
#   State 2 — FCC NOT yet bound to any site (factory default):
#     Name    : "ESP_SPP_SERVER"

BLE_ADV_PREFIX    = "REL"   # Fixed 3-char ASCII prefix in every bound FCC broadcast name
BLE_ADV_SEPARATOR = " "     # Single space (0x20) between SAPCODE and DU numbers
FALLBACK_ADV_NAME = "ESP_SPP_SERVER"   # Unbound / unconfigured FCC device


# ── Protocol Constants ────────────────────────────────────────────────────────

ACK                  = 0x01
NACK_SERVICE_OFFLINE = 0x97
NACK_TIMEOUT         = 0x98
CHUNK_MARKER         = 0x23   # first two bytes of a chunked packet are 0x23 0x23


# ── Command Codes ─────────────────────────────────────────────────────────────

class BLECommand(IntEnum):
    GET_STATUS                          = 0x01
    PUMP_PRESET                         = 0x02
    GET_LAST_TRANSACTION                = 0x03
    GET_SITE_DETAILS                    = 0x04
    GET_RECEIPT_HEADER_FOOTER           = 0x05
    GET_PRODUCT_DETAILS                 = 0x06
    MOP_CHANGE                          = 0x07
    PUMP_LOCKING                        = 0x09
    GET_LAST_3_TRANSACTIONS             = 0x0A
    GET_LAST_TRANSACTION_EXTENDED       = 0x15
    GET_LAST_3_TRANSACTIONS_EXTENDED    = 0x16
    MOP_CHANGE_EXTENDED                 = 0x17
    SHIFT_STATUS                        = 0x18
    SHIFT_END                           = 0x19
    FETCH_LUBE_DETAILS                  = 0x1A
    LUBE_SALE                           = 0x1B
    LUBE_STOCK_UPDATE                   = 0x1C
    GET_LOCAL_ACCOUNT_DETAILS           = 0x1D
    LOCAL_ACCOUNT_PAYMENT               = 0x1E
    UPDATE_PRODUCT_PRICE                = 0x1F
    PUMP_PRESET_EXTENDED                = 0x20
    GET_ATTENDANT_DETAILS               = 0x21
    TERMINAL_STATUS_UPDATE              = 0x22
    FETCH_TRANSACTION_BY_ID             = 0x23
    RO_MASTER_DATA                      = 0x24
    DETAILED_PRODUCT_CONFIG             = 0x25
    GET_LAST_5_TRANSACTIONS_EXTENDED    = 0x26
    MOP_CHANGE_EXTENDED_EXTRA           = 0x27
    PRESET_EXTENDED_EXTRA               = 0x28
    GET_LAST_TXN_EXTENDED_EXTRA         = 0x29
    PUMP_STATUS_REMOTE                  = 0x2A


# ── Lookup Tables ─────────────────────────────────────────────────────────────

PUMP_STATE_MAP: dict = {
    0:  "Pump idle",
    1:  "Pump calling",
    2:  "Pump auth",
    3:  "Pump fuelling",
    4:  "Pump idle",
    5:  "Pump offline",
    6:  "Pump lock",
    7:  "Pump stop",
    8:  "Pump close",
    9:  "Pump error",
    11: "Pump paused",
    13: "Pump auth",
    14: "Pump auth",
}

PRESET_ERROR_MAP: dict = {
    0:  "Preset fail",
    1:  "Preset success",
    4:  "Invalid nozzle",
    5:  "Nozzle lock or interlock",
    6:  "Pump busy",
    8:  "Invalid pump",
    11: "Error",
    12: "Auth store fail",
    14: "Auth generation fail",
    15: "Invalid data in preset command",
    16: "Nozzle already preseted",
    17: "Pump lock by other device",
}

MOP_MAP: dict = {
    1:  "Cash",
    2:  "Credit Card",
    3:  "Local Account",
    4:  "Loyalty",
    5:  "Wallet",
    6:  "Fleet POS",
    7:  "Testing",
    8:  "Sampling",
    9:  "Own Use",
    10: "Others",
    11: "Debit Card",
    21: "Redemption",
    22: "Mobile Wallet",
    23: "Coupons",
    24: "UPI",
    25: "Bharat QR",
    50: "Mobile App",
}

PAYMENT_MODE_MAP: dict = {
    1: "Net Banking",
    2: "Credit Card",
    3: "Debit Card",
    4: "PPI",
    5: "UPI",
    6: "Voucher",
    7: "Other",
}

VEHICLE_TYPE_MAP: dict = {
    0x02: "Two Wheeler",
    0x03: "Three Wheeler",
    0x04: "Four Wheeler",
}

PRESET_TYPE_MAP: dict = {
    0x00: "No preset",
    0x01: "Remote volume",
    0x02: "Remote amount",
    0x03: "Local volume",
    0x04: "Local amount",
}


# ── CRC ───────────────────────────────────────────────────────────────────────

def modbus_crc16(data: bytes) -> int:
    """Calculate Modbus CRC-16 (polynomial 0x8005, init 0xFFFF, reflected).

    Applied over the packet bytes from Length field through end of Data field.
    Returns the 16-bit CRC as an integer (little-endian when packed to bytes).
    """
    crc = 0xFFFF
    for byte in data:
        crc ^= byte
        for _ in range(8):
            if crc & 0x0001:
                crc = (crc >> 1) ^ 0xA001
            else:
                crc >>= 1
    return crc


# ── Packet Builder ────────────────────────────────────────────────────────────

def build_response(command: int, ack_nack: int, data: bytes = b"") -> bytes:
    """Build a complete BLE response packet to send via notify (0xABF2).

    Same wire format as a request; only the ack_nack field differs.

    Args:
        command:   Command code byte (echo of the received command).
        ack_nack:  0x01 = ACK (success), 0x97 = NACK service offline,
                   0x98 = NACK timeout.
        data:      Response payload bytes (command-specific).

    Returns:
        Complete packet bytes ready to write to the Notify characteristic.
    """
    inner = bytes([command, ack_nack]) + data
    length = len(inner)
    header = struct.pack("<H", length)
    payload = header + inner
    crc = modbus_crc16(payload)
    crc_bytes = struct.pack("<H", crc)
    packet = payload + crc_bytes
    logger.debug(
        "BLE RESP  cmd=0x%02X  ack=0x%02X  len=%d  pkt=%s",
        command, ack_nack, length, packet.hex().upper(),
    )
    return packet


def build_request(command: BLECommand, data: bytes = b"") -> bytes:
    """Build a complete BLE request packet ready to write to 0xABF1.

    Format: [L (2B LE)] [CMD (1B)] [0x01 ACK (1B)] [Data (NB)] [CRC (2B LE)]
    L = 1 (CMD) + 1 (ACK) + len(data)

    Args:
        command: BLECommand enum value.
        data:    Command-specific payload bytes (may be empty).

    Returns:
        Complete packet bytes.
    """
    inner = bytes([int(command), ACK]) + data
    length = len(inner)
    header = struct.pack("<H", length)        # little-endian Length field
    payload = header + inner
    crc = modbus_crc16(payload)
    crc_bytes = struct.pack("<H", crc)        # little-endian CRC
    packet = payload + crc_bytes
    logger.debug(
        "BLE TX  cmd=0x%02X  len=%d  pkt=%s",
        int(command), length, packet.hex().upper(),
    )
    return packet


def chunk_packet(packet: bytes, mtu: int) -> List[bytes]:
    """Split a packet into MTU-sized chunks when total size exceeds MTU.

    If len(packet) <= mtu, returns [packet] unchanged (no chunking needed).

    Chunk format: [0x23][0x23][TotalPKT (1B)][CurrentPKT (1B)][ChunkData]
    Header overhead = 4 bytes, so payload per chunk = mtu - 4.
    Total PKT count = (len(packet) / (mtu - 4)) + 1  (integer division).

    Args:
        packet: Full packet bytes from build_request().
        mtu:    Negotiated MTU size in bytes.

    Returns:
        List of chunk byte strings ready to write sequentially.
    """
    if len(packet) <= mtu:
        return [packet]

    chunk_data_size = mtu - 4
    total_chunks = (len(packet) // chunk_data_size) + 1
    chunks = []
    for i in range(total_chunks):
        chunk_data = packet[i * chunk_data_size:(i + 1) * chunk_data_size]
        if not chunk_data:
            break
        header = bytes([CHUNK_MARKER, CHUNK_MARKER, total_chunks, i + 1])
        chunks.append(header + chunk_data)
    logger.debug(
        "BLE chunk: split %d bytes into %d chunks (mtu=%d)",
        len(packet), len(chunks), mtu,
    )
    return chunks


# ── Packet Assembler (chunked responses) ──────────────────────────────────────

class PacketAssembler:
    """Reassembles chunked BLE notification packets into one complete packet.

    The FCC server sends chunked packets when the response exceeds MTU size.
    Each chunk is identified by the leading 0x23 0x23 bytes. This class
    accumulates chunks by their sequence number and returns the reassembled
    payload once all chunks are received.

    Non-chunked packets (no leading 0x23 0x23) are passed through immediately.

    Thread-safety: instances are not thread-safe; use one per BLE connection.
    """

    def __init__(self) -> None:
        self._total: int = 0
        self._received: dict = {}

    def reset(self) -> None:
        """Discard any partial assembly state."""
        self._total = 0
        self._received = {}

    def feed(self, raw: bytes) -> Optional[bytes]:
        """Feed one received BLE notification chunk.

        Args:
            raw: Raw bytes received from the NOTIFY characteristic.

        Returns:
            Complete reassembled packet bytes once all chunks arrive.
            None if more chunks are still expected.
        """
        if len(raw) >= 2 and raw[0] == CHUNK_MARKER and raw[1] == CHUNK_MARKER:
            if len(raw) < 4:
                logger.warning("BLE assembler: malformed chunk header (%d bytes)", len(raw))
                return None
            total   = raw[2]
            current = raw[3]
            data    = raw[4:]
            self._total = total
            self._received[current] = data
            if len(self._received) == self._total:
                assembled = b"".join(
                    self._received[i] for i in range(1, self._total + 1)
                )
                self.reset()
                logger.debug("BLE assembler: reassembled %d chunks -> %d bytes", total, len(assembled))
                return assembled
            return None
        else:
            # Non-chunked: return immediately, clear any stale partial state
            self.reset()
            return raw


# ── Packet Parser ─────────────────────────────────────────────────────────────

def parse_response(raw: bytes) -> Tuple[Optional[int], Optional[int], Optional[bytes]]:
    """Parse a complete BLE response packet received via 0xABF2 notify.

    Verifies the Modbus CRC-16 before returning any data.

    Args:
        raw: Complete (possibly reassembled) response bytes.

    Returns:
        (command, ack_nack, data_bytes) on success.
        (None, None, None) on CRC error or malformed packet.
    """
    if len(raw) < 6:
        logger.warning("BLE RX: packet too short (%d bytes)", len(raw))
        return None, None, None

    length = struct.unpack("<H", raw[0:2])[0]
    expected_total = length + 4   # 2-byte length field + L bytes + 2-byte CRC

    if len(raw) < expected_total:
        logger.warning(
            "BLE RX: incomplete packet (need %d, got %d)", expected_total, len(raw)
        )
        return None, None, None

    payload  = raw[:length + 2]
    recv_crc = struct.unpack("<H", raw[length + 2:length + 4])[0]
    calc_crc = modbus_crc16(payload)

    if recv_crc != calc_crc:
        logger.error(
            "BLE RX: CRC mismatch (recv=0x%04X  calc=0x%04X)", recv_crc, calc_crc
        )
        return None, None, None

    command  = raw[2]
    ack_nack = raw[3]
    data     = raw[4:length + 2]

    logger.debug(
        "BLE RX  cmd=0x%02X  ack=0x%02X  data=%s",
        command, ack_nack, data.hex().upper(),
    )
    return command, ack_nack, data


# ── Response Dataclasses ──────────────────────────────────────────────────────

@dataclass
class NozzleStatus:
    """Per-nozzle status within a Fuelling Position (FP)."""
    nozzle_no:    int
    nozzle_state: int
    product_id:   int


@dataclass
class FPStatus:
    """Status of one Fuelling Position (FP / pump) from Get Status (0x01)."""
    fp_no:            int
    pump_state:       int
    pump_state_desc:  str
    active_nozzle_id: int
    volume:           float    # actual litres (raw / 100)
    amount:           float    # actual currency amount (raw / 100)
    price:            float    # actual unit price (raw / 100)
    nozzles:          List[NozzleStatus] = field(default_factory=list)


@dataclass
class GetStatusResponse:
    """Response for Get Status (0x01)."""
    fp_list: List[FPStatus] = field(default_factory=list)


@dataclass
class PumpPresetResponse:
    """Response for Pump Preset (0x02) and Pump Preset Extended (0x20)."""
    pump_number:   int
    nozzle_number: int = 0        # populated by extended variant (0x20)
    error_code:    int = 0
    error_desc:    str = ""
    product_id:    int = 0        # populated by extended variant (0x20)
    product_price: float = 0.0   # populated by extended variant (0x20)
    random_number: int = 0
    auth_number:   int = 0


@dataclass
class TransactionRecord:
    """Single transaction returned by Get Last Transaction (0x03 / 0x15 / 0x29)."""
    pump_number:    int
    nozzle_number:  int
    auth_number:    int
    year:           int
    month:          int
    day:            int
    unique_id:      int
    hour:           int
    minute:         int
    second:         int
    volume:         float   # actual litres
    amount:         float   # actual currency
    price:          float   # actual unit price
    mop:            int
    product_id:     int
    payment_trx_id: str


@dataclass
class SiteDetailsResponse:
    """Response for Get Site Details (0x04)."""
    site_code: str
    site_name: str


@dataclass
class ProductDetail:
    """One product entry within Get Product Details (0x06)."""
    product_id: int
    price:      float   # actual unit price
    name:       str


@dataclass
class GetProductDetailsResponse:
    """Response for Get Product Details (0x06)."""
    products: List[ProductDetail] = field(default_factory=list)


@dataclass
class ShiftStatusResponse:
    """Response for Shift Status (0x18)."""
    shift_status: int    # 0x01 = Running
    shift_number: int
    year:         int
    month:        int
    day:          int
    hour:         int
    minute:       int
    second:       int


@dataclass
class AttendantDetail:
    """One attendant entry within Get Attendant Details (0x21)."""
    attendant_id:   int
    attendant_name: str


@dataclass
class GetAttendantDetailsResponse:
    """Response for Get Attendant Details (0x21)."""
    attendants: List[AttendantDetail] = field(default_factory=list)


@dataclass
class LocalAccountDetailsResponse:
    """Response for Get Local Account Details (0x1D)."""
    status:             int
    account_number:     str
    account_user_name:  str
    outstanding_amount: float   # actual currency
    credit_limit:       float   # actual currency


@dataclass
class LubeDetailsResponse:
    """Response for Fetch Lube Details (0x1A)."""
    lube_code:  str
    lube_name:  str
    status:     int     # 1=Present, 0=Absent
    stock:      int     # available quantity
    price:      float   # actual price


# ── Response Parsers ──────────────────────────────────────────────────────────

def parse_get_status(data: bytes) -> Optional[GetStatusResponse]:
    """Parse data bytes from Get Status (0x01) response."""
    if not data:
        return None
    idx = 0
    fp_count = data[idx]; idx += 1
    fp_list = []
    for _ in range(fp_count):
        if idx + 14 > len(data):
            break
        fp_no            = data[idx]; idx += 1
        pump_state       = data[idx]; idx += 1
        active_nozzle_id = data[idx]; idx += 1
        volume_raw       = struct.unpack(">I", data[idx:idx + 4])[0]; idx += 4
        amount_raw       = struct.unpack(">I", data[idx:idx + 4])[0]; idx += 4
        price_raw        = struct.unpack(">I", data[idx:idx + 4])[0]; idx += 4
        nozzle_count     = data[idx]; idx += 1
        nozzles = []
        for _ in range(nozzle_count):
            if idx + 3 > len(data):
                break
            nz_no    = data[idx]; idx += 1
            nz_state = data[idx]; idx += 1
            prod_id  = data[idx]; idx += 1
            nozzles.append(NozzleStatus(nz_no, nz_state, prod_id))
        fp_list.append(FPStatus(
            fp_no=fp_no,
            pump_state=pump_state,
            pump_state_desc=PUMP_STATE_MAP.get(pump_state, f"Unknown(0x{pump_state:02X})"),
            active_nozzle_id=active_nozzle_id,
            volume=volume_raw / 100.0,
            amount=amount_raw / 100.0,
            price=price_raw / 100.0,
            nozzles=nozzles,
        ))
    return GetStatusResponse(fp_list=fp_list)


def parse_pump_preset(data: bytes) -> Optional[PumpPresetResponse]:
    """Parse data bytes from Pump Preset (0x02) response."""
    if len(data) < 5:
        return None
    pump_number   = data[0]
    error_code    = data[1]
    random_number = struct.unpack(">H", data[2:4])[0]
    auth_number   = struct.unpack(">H", data[4:6])[0] if len(data) >= 6 else 0
    return PumpPresetResponse(
        pump_number=pump_number,
        error_code=error_code,
        error_desc=PRESET_ERROR_MAP.get(error_code, f"Unknown(0x{error_code:02X})"),
        random_number=random_number,
        auth_number=auth_number,
    )


def parse_pump_preset_extended(data: bytes) -> Optional[PumpPresetResponse]:
    """Parse data bytes from Pump Preset Extended (0x20) response."""
    if len(data) < 9:
        return None
    pump_number   = data[0]
    nozzle_number = data[1]
    error_code    = data[2]
    product_id    = data[3]
    price_raw     = struct.unpack(">I", data[4:8])[0]
    auth_number   = struct.unpack(">H", data[8:10])[0] if len(data) >= 10 else 0
    return PumpPresetResponse(
        pump_number=pump_number,
        nozzle_number=nozzle_number,
        error_code=error_code,
        error_desc=PRESET_ERROR_MAP.get(error_code, f"Unknown(0x{error_code:02X})"),
        product_id=product_id,
        product_price=price_raw / 100.0,
        auth_number=auth_number,
    )


def parse_last_transaction(data: bytes) -> Optional[TransactionRecord]:
    """Parse data bytes from Get Last Transaction (0x03) response."""
    if len(data) < 30:
        return None
    idx = 0
    _count        = data[idx]; idx += 1
    pump_number   = data[idx]; idx += 1
    nozzle_number = data[idx]; idx += 1
    auth_number   = struct.unpack(">H", data[idx:idx + 2])[0]; idx += 2
    year          = data[idx]; idx += 1
    month         = data[idx]; idx += 1
    day           = data[idx]; idx += 1
    unique_id     = struct.unpack(">H", data[idx:idx + 2])[0]; idx += 2
    hour          = data[idx]; idx += 1
    minute        = data[idx]; idx += 1
    second        = data[idx]; idx += 1
    volume_raw    = struct.unpack(">I", data[idx:idx + 4])[0]; idx += 4
    amount_raw    = struct.unpack(">I", data[idx:idx + 4])[0]; idx += 4
    price_raw     = struct.unpack(">I", data[idx:idx + 4])[0]; idx += 4
    mop           = data[idx]; idx += 1
    product_id    = data[idx]; idx += 1
    trx_id_bytes  = data[idx:idx + 20]; idx += 20
    trx_id        = trx_id_bytes.decode("ascii", errors="ignore").rstrip()
    return TransactionRecord(
        pump_number=pump_number,
        nozzle_number=nozzle_number,
        auth_number=auth_number,
        year=year, month=month, day=day,
        unique_id=unique_id,
        hour=hour, minute=minute, second=second,
        volume=volume_raw / 100.0,
        amount=amount_raw / 100.0,
        price=price_raw / 100.0,
        mop=mop,
        product_id=product_id,
        payment_trx_id=trx_id,
    )


def parse_site_details(data: bytes) -> Optional[SiteDetailsResponse]:
    """Parse data bytes from Get Site Details (0x04) response."""
    if len(data) < 32:
        return None
    site_code = data[0:12].decode("ascii", errors="ignore").rstrip()
    site_name = data[12:32].decode("ascii", errors="ignore").rstrip()
    return SiteDetailsResponse(site_code=site_code, site_name=site_name)


def parse_product_details(data: bytes) -> Optional[GetProductDetailsResponse]:
    """Parse data bytes from Get Product Details (0x06) response."""
    if not data:
        return None
    idx = 0
    product_count = data[idx]; idx += 1
    products = []
    for _ in range(product_count):
        if idx + 15 > len(data):
            break
        prod_id   = data[idx]; idx += 1
        price_raw = struct.unpack(">I", data[idx:idx + 4])[0]; idx += 4
        name      = data[idx:idx + 10].decode("ascii", errors="ignore").rstrip(); idx += 10
        products.append(ProductDetail(product_id=prod_id, price=price_raw / 100.0, name=name))
    return GetProductDetailsResponse(products=products)


def parse_shift_status(data: bytes) -> Optional[ShiftStatusResponse]:
    """Parse data bytes from Shift Status (0x18) response."""
    if len(data) < 11:
        return None
    shift_status = data[0]
    shift_number = struct.unpack(">I", data[1:5])[0]
    year         = data[5]
    month        = data[6]
    day          = data[7]
    hour         = data[8]
    minute       = data[9]
    second       = data[10]
    return ShiftStatusResponse(
        shift_status=shift_status,
        shift_number=shift_number,
        year=year, month=month, day=day,
        hour=hour, minute=minute, second=second,
    )


def parse_attendant_details(data: bytes) -> Optional[GetAttendantDetailsResponse]:
    """Parse data bytes from Get Attendant Details (0x21) response."""
    if not data:
        return None
    idx = 0
    attendant_count = data[idx]; idx += 1
    attendants = []
    for _ in range(attendant_count):
        if idx + 11 > len(data):
            break
        att_id   = data[idx]; idx += 1
        att_name = data[idx:idx + 10].decode("ascii", errors="ignore").rstrip(); idx += 10
        attendants.append(AttendantDetail(attendant_id=att_id, attendant_name=att_name))
    return GetAttendantDetailsResponse(attendants=attendants)


def parse_local_account_details(data: bytes) -> Optional[LocalAccountDetailsResponse]:
    """Parse data bytes from Get Local Account Details (0x1D) response."""
    if len(data) < 44:
        return None
    status             = data[0]
    account_number     = data[1:16].decode("ascii", errors="ignore").rstrip()
    account_user_name  = data[16:36].decode("ascii", errors="ignore").rstrip()
    outstanding_raw    = struct.unpack(">I", data[36:40])[0]
    credit_limit_raw   = struct.unpack(">I", data[40:44])[0]
    return LocalAccountDetailsResponse(
        status=status,
        account_number=account_number,
        account_user_name=account_user_name,
        outstanding_amount=outstanding_raw / 100.0,
        credit_limit=credit_limit_raw / 100.0,
    )


def parse_lube_details(data: bytes) -> Optional[LubeDetailsResponse]:
    """Parse data bytes from Fetch Lube Details (0x1A) response."""
    if len(data) < 29:
        return None
    lube_code = data[0:10].decode("ascii", errors="ignore").rstrip()
    lube_name = data[10:20].decode("ascii", errors="ignore").rstrip()
    status    = data[20]
    stock     = struct.unpack(">I", data[21:25])[0]
    price_raw = struct.unpack(">I", data[25:29])[0]
    return LubeDetailsResponse(
        lube_code=lube_code,
        lube_name=lube_name,
        status=status,
        stock=stock,
        price=price_raw / 100.0,
    )
