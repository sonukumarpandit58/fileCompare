"""
tqcl_protocol.py - Tokheim ELC Fuel Dispenser Communication Protocol v2.06 Rev.7
==================================================================================

Reference document: TQCL COMMUNICATION PROTOCOL V02.06_REV_07.pdf
                    COPYRIGHT TOKHEIM INDIA PVT.LTD. 2024

PURPOSE
-------
This module is the lowest-level protocol layer. It handles ONLY the
serialisation/deserialisation of TQCL frames - it has no knowledge of
business logic, state machines, or serial hardware.

All public functions accept plain Python integers/floats/bytes and return
typed dataclass instances or raw bytes. Nothing in this file does I/O.

PROTOCOL OVERVIEW
-----------------
TQCL (Tokheim Queue Communication Link) is a master/slave binary protocol
where the Raspberry Pi (master) polls Tokheim ELC dispensers (slaves) over
an RS485 half-duplex bus.

Frame layout (Non-Secured Mode):

  Command frame (master -> dispenser):
    [NZA] [DUA] [CMD] [...optional data bytes...] [EOC=0x7F] [BCC]

  Response frame (dispenser -> master):
    [NZA] [DUA] [...response data bytes...] [EOC=0x7F] [BCC]

  Field definitions:
    NZA  = Nozzle Address (1-based integer, 1 byte)
           Identifies which nozzle on the dispenser is being addressed.
           Example: 0x01 = Nozzle 1, 0x02 = Nozzle 2

    DUA  = Dispenser Unit Address (1 byte, format: 010xxxxx binary)
           Encodes the dispenser unit number: DUA = 0x40 | unit_number
           Example: unit 2 = 0x42, unit 3 = 0x43
           Valid range: 0x41 (unit 1) to 0x5F (unit 31)

    CMD  = Single ASCII command byte (e.g. 'S'=0x53 for Status Poll)

    EOC  = End Of Command marker, always 0x7F

    BCC  = Block Check Character = XOR of all bytes from NZA through EOC
           Used to detect transmission errors.

BCC CALCULATION EXAMPLE
-----------------------
  Status Poll for NZA=0x01, DUA=0x42:
    Payload bytes (before BCC): 01 42 53 7F
    BCC = 0x01 ^ 0x42 ^ 0x53 ^ 0x7F = 0x6F
    Complete frame: 01 42 53 7F 6F  <-- verified

MODULES USED
------------
  struct          : for byte packing (available for future extension)
  logging         : debug-level frame traces
  dataclass       : typed response containers
  enum.IntEnum    : pump state constants
  typing.Optional : type hints

DEPENDENCIES
------------
  None (pure Python, no external packages)
"""

import struct
import logging
from dataclasses import dataclass, field
from enum import IntEnum
from typing import Optional

log = logging.getLogger(__name__)

# ── Protocol constants ────────────────────────────────────────────────────────

# End Of Command marker - every TQCL frame ends with this byte before the BCC
EOC = 0x7F

# Acknowledgement bytes used in 5-byte ACK responses
ACK_POS = ord('Y')   # 0x59 - Positive Acknowledge: command accepted
ACK_NEG = ord('X')   # 0x58 - Negative Acknowledge: command rejected or failed


# ── Pump state enumeration ────────────────────────────────────────────────────

class PumpState(IntEnum):
    """
    Represents the operational state of a single nozzle/pump unit.

    These values appear in the Status1 byte of every Status Poll response
    (TQCL Section 8.1.1). The dispenser transitions between these states
    as the customer interacts with the pump.

    State Transition Summary:
        IDLE
          |-- (nozzle lifted by customer)
          v
        CALL                    <- nozzle is off-hook, waiting for authorisation
          |-- (Set Preset + Authorize sent by master)
          v
        PRESET_READY / AUTHORIZED
          |-- (fueling starts)
          v
        FUELING / STARTED
          |-- (customer returns nozzle / preset reached)
          v
        PAYABLE                 <- dispensing complete, ready for transaction read
          |-- (Clear Sale sent by master)
          v
        IDLE

    Additional states:
        SUSPENDED       : fueling temporarily paused (e.g. customer pressed pause)
        STOPPED         : pump stopped (nozzle returned before authorisation, or error)
        INOPERATIVE     : hardware fault or pump taken out of service
        WAIT_FOR_PRESET : pump waiting for a preset from the master
        UNKNOWN         : used internally when the state byte is not recognised
    """

    IDLE            = 0x30   # '0' - Nozzle on hook, pump idle, ready for next customer
    CALL            = 0x31   # '1' - Nozzle lifted by customer, waiting for authorisation
    PRESET_READY    = 0x32   # '2' - Preset has been accepted, ready for Authorize command
    FUELING         = 0x33   # '3' - Fuel is actively flowing
    PAYABLE         = 0x34   # '4' - Fueling complete, transaction data available for read
    SUSPENDED       = 0x35   # '5' - Fueling temporarily suspended
    STOPPED         = 0x36   # '6' - Pump stopped (nozzle returned or error)
    INOPERATIVE     = 0x38   # '8' - Hardware fault, pump out of service
    AUTHORIZED      = 0x39   # '9' - Authorisation accepted, awaiting fuel flow
    STARTED         = 0x3B   # ';' - Pump motor started
    SUSPEND_STARTED = 0x3D   # '=' - Suspended but motor still engaged
    WAIT_FOR_PRESET = 0x3E   # '>' - Waiting for master to send preset value
    UNKNOWN         = 0xFF   # Internal sentinel for unrecognised state bytes


def parse_state(byte_val: int) -> PumpState:
    """
    Safely convert a raw status byte to a PumpState enum value.

    If the byte does not match any known state, returns PumpState.UNKNOWN
    instead of raising an exception. This prevents crashes when the dispenser
    sends an undocumented state code.

    Args:
        byte_val (int): Raw Status1 byte from a TQCL Status Poll response.

    Returns:
        PumpState: Matching enum member, or PumpState.UNKNOWN if not found.

    Example:
        >>> parse_state(0x30)
        <PumpState.IDLE: 48>
        >>> parse_state(0xAA)   # unknown byte
        <PumpState.UNKNOWN: 255>
    """
    try:
        return PumpState(byte_val)
    except ValueError:
        return PumpState.UNKNOWN


# ── Preset type enumeration ───────────────────────────────────────────────────

class PresetType(IntEnum):
    """
    Specifies whether a preset is expressed as a monetary amount or a volume.

    Used in the Set Preset ('P') command (TQCL Section 8.2.4).
    The dispenser uses this to know when to stop fueling.

    AMOUNT: Stop when the monetary value dispensed reaches the preset.
            Value is in the local currency (e.g. Indian Rupees).
            Example: preset of Rs. 500 stops the pump at Rs. 500 dispensed.

    VOLUME: Stop when the volume dispensed reaches the preset.
            Value is in litres.
            Example: preset of 20.000 L stops at 20 litres.
    """

    AMOUNT = 0x30   # '0' - Preset is a monetary amount (rupees, etc.)
    VOLUME = 0x31   # '1' - Preset is a fuel volume (litres)


# ── BCC calculation ───────────────────────────────────────────────────────────

def _bcc(frame_without_bcc: bytes) -> int:
    """
    Calculate the Block Check Character (BCC) for a TQCL frame.

    The BCC is an 8-bit XOR of all bytes from NZA through EOC inclusive.
    It is appended as the last byte of every frame (command or response)
    to detect single-bit transmission errors on the RS485 bus.

    This is a private function; external code should use build_frame()
    and verify_response() which call _bcc() internally.

    Algorithm:
        BCC = byte[0] XOR byte[1] XOR ... XOR byte[N]
        where byte[0] = NZA, byte[N] = EOC (0x7F)

    Args:
        frame_without_bcc (bytes): All frame bytes EXCEPT the final BCC byte.
                                   Must include NZA, DUA, optional CMD/data, and EOC.

    Returns:
        int: Single byte BCC value (0x00 to 0xFF).

    Example:
        Status Poll frame bytes before BCC: 01 42 53 7F
        _bcc(b'\\x01\\x42\\x53\\x7F') == 0x6F  # verified
    """
    result = 0
    for b in frame_without_bcc:
        result ^= b        # XOR each byte cumulatively
    return result


# ── Frame builder ─────────────────────────────────────────────────────────────

def build_frame(nza: int, dua: int, cmd: int, data: bytes = b'') -> bytes:
    """
    Construct a complete TQCL command frame ready to send over RS485.

    Assembles the frame in this order:
        [NZA] [DUA] [CMD] [data...] [EOC=0x7F] [BCC]

    The BCC is calculated automatically from the assembled payload.

    Args:
        nza  (int)  : Nozzle Address (1-based). Example: 1 for Nozzle 1.
        dua  (int)  : Dispenser Unit Address. Formula: DUA = 0x40 | unit_number.
                      Example: 0x42 for unit 2, 0x43 for unit 3.
        cmd  (int)  : Single command byte as integer. Use ord('S') for 'S', etc.
        data (bytes): Optional payload bytes inserted between CMD and EOC.
                      Empty for simple commands like Status Poll.

    Returns:
        bytes: Complete frame including BCC, ready for serial.write().

    Examples:
        # Status Poll for NZA=1, DUA=0x42 (unit 2)
        build_frame(0x01, 0x42, ord('S'))
        -> b'\\x01\\x42\\x53\\x7F\\x6F'   # verified BCC = 0x6F

        # Pump Start for NZA=1, DUA=0x42
        build_frame(0x01, 0x42, ord('O'))
        -> b'\\x01\\x42\\x4F\\x7F\\x73'   # verified BCC = 0x73
    """
    # Assemble everything except BCC
    payload = bytes([nza, dua, cmd]) + data + bytes([EOC])
    # Append calculated BCC as final byte
    return payload + bytes([_bcc(payload)])


# ── Response verifier ─────────────────────────────────────────────────────────

def verify_response(raw: bytes, expected_nza: int, expected_dua: int) -> bool:
    """
    Validate a raw response frame from the dispenser.

    Performs four integrity checks in order:
      1. Minimum length (at least 5 bytes: NZA + DUA + 1 data + EOC + BCC)
      2. NZA match - response must echo back the same NZA we sent
      3. DUA match - response must echo back the same DUA we sent
      4. EOC present at position [-2] (second-to-last byte must be 0x7F)
      5. BCC valid - XOR of bytes [0:-1] must equal last byte

    If any check fails, a DEBUG log is emitted with the specific reason,
    which helps diagnose RS485 wiring issues or protocol mismatches.

    Args:
        raw          (bytes): Raw bytes received from serial.read().
        expected_nza (int)  : NZA value we sent in the command (for echo check).
        expected_dua (int)  : DUA value we sent in the command (for echo check).

    Returns:
        bool: True if all checks pass, False if any check fails.

    Notes:
        - A False return does NOT raise an exception; callers handle the failure.
        - In a noisy RS485 environment, occasional verify failures are expected.
          The retry logic in NozzleController._retry() handles this.
        - This function is called by every parse_*() function before extracting data.
    """
    # Check 1: minimum viable length
    if len(raw) < 5:
        return False

    # Check 2: NZA echo
    if raw[0] != expected_nza:
        log.debug("BCC verify: NZA mismatch got 0x%02X expected 0x%02X", raw[0], expected_nza)
        return False

    # Check 3: DUA echo
    if raw[1] != expected_dua:
        log.debug("BCC verify: DUA mismatch got 0x%02X expected 0x%02X", raw[1], expected_dua)
        return False

    # Check 4: EOC must be second-to-last byte
    if raw[-2] != EOC:
        log.debug("BCC verify: EOC missing at pos %d, got 0x%02X", len(raw)-2, raw[-2])
        return False

    # Check 5: BCC validity - XOR all bytes except final BCC, compare to final byte
    expected_bcc = _bcc(raw[:-1])
    if raw[-1] != expected_bcc:
        log.debug("BCC verify: BCC mismatch got 0x%02X expected 0x%02X", raw[-1], expected_bcc)
        return False

    return True


# ── Response dataclasses ──────────────────────────────────────────────────────

@dataclass
class StatusResponse:
    """
    Parsed result of a Status Poll ('S') command response.

    The Status Poll is the most frequently used command - it is sent
    every ~1 second per nozzle to detect state changes (e.g. when a
    customer lifts the nozzle, the state changes from IDLE to CALL).

    Response frame layout (6 bytes total):
        [NZA] [DUA] [Status0] [Status1] [EOC=0x7F] [BCC]

    Attributes:
        state          (PumpState) : Decoded pump state from Status1 byte.
                                     This is the primary field used by the
                                     state machine in pump_controller.py.
        raw_status0    (int)       : Raw Status0 byte. Contains bit flags
                                     for nozzle position and motor state.
        raw_status1    (int)       : Raw Status1 byte (same as state value).
        nozzle_on_hook (bool)      : True if nozzle is physically on the holster.
                                     Derived from Status0 bit 0 (LSB).
                                     True = nozzle is hung up (idle),
                                     False = nozzle is lifted (customer fueling).
        motor_on       (bool)      : True if the pump motor is running.
                                     Derived from Status0 bit 1.
                                     Useful for confirming fuel is flowing.

    Example raw response for IDLE state (NZA=0x01, DUA=0x42):
        01 42 01 30 7F 0D
        Status0=0x01 -> nozzle_on_hook=True, motor_on=False
        Status1=0x30 -> PumpState.IDLE
    """

    state: PumpState
    raw_status0: int
    raw_status1: int
    nozzle_on_hook: bool
    motor_on: bool

    @classmethod
    def from_bytes(cls, data: bytes) -> 'StatusResponse':
        """
        Create a StatusResponse from the payload slice of a raw response.

        The payload is the portion of the response after NZA and DUA,
        and before EOC and BCC. For a Status Poll response, this is
        exactly 2 bytes: [Status0, Status1].

        Args:
            data (bytes): Payload bytes. Must be at least 2 bytes.
                          Caller (parse_status_poll) extracts raw[2:-2].

        Returns:
            StatusResponse: Populated dataclass instance.

        Status0 bit layout:
            bit 0 (0x01): nozzle_on_hook  (1 = on hook, 0 = lifted)
            bit 1 (0x02): motor_on        (1 = motor running)
            bit 2-7     : reserved / dispenser-specific flags
        """
        s0, s1 = data[0], data[1]
        return cls(
            state=parse_state(s1),       # Status1 encodes the pump state
            raw_status0=s0,
            raw_status1=s1,
            nozzle_on_hook=bool(s0 & 0x01),   # isolate bit 0
            motor_on=bool(s0 & 0x02),          # isolate bit 1
        )


@dataclass
class TransactionResponse:
    """
    Parsed result of a Read Transaction ('R') command response.

    Called after fueling completes (pump in PAYABLE or STOPPED state) to
    retrieve the details of the transaction just completed. This data forms
    the core of the PSV record sent to Azure IoT Hub.

    Response frame layout (31 bytes total):
        [NZA][DUA][Status0][Status1][UnitPrice×7][Volume×9][Amount×9][EOC][BCC]

    All numeric fields are ASCII-encoded with embedded decimal points.

    Attributes:
        state      (PumpState) : Current pump state at time of read.
        raw_status0 (int)      : Raw Status0 byte.
        unit_price  (float)    : Price per litre at time of fueling.
                                 Example: b'0093.50' -> 93.50
        volume      (float)    : Total volume dispensed in litres.
                                 Example: b'000032.100' -> 32.1
        amount      (float)    : Total monetary amount dispensed.
                                 Example: b'000747.900' -> 747.90

    Notes:
        - unit_price comes from the dispenser's own configured rate.
          If the dispenser returns 0.00, the NozzleConfig.unit_price is used.
        - volume and amount are net values for this transaction only
          (not cumulative totalizers).
    """

    state: PumpState
    raw_status0: int
    unit_price: float
    volume: float
    amount: float

    @classmethod
    def from_bytes(cls, data: bytes) -> 'TransactionResponse':
        """
        Parse the 27-byte payload of a Read Transaction response.

        The payload structure (after stripping NZA, DUA, EOC, BCC):
            data[0]     = Status0
            data[1]     = Status1 (pump state)
            data[2:9]   = UnitPrice  (7 ASCII bytes, e.g. b'0093.50')
            data[9:18]  = Volume     (9 ASCII bytes, e.g. b'000032.10')
            data[18:27] = Amount     (9 ASCII bytes, e.g. b'000747.90')

        Args:
            data (bytes): 27-byte payload slice from the raw response.

        Returns:
            TransactionResponse: Populated dataclass with parsed floats.

        Parsing strategy:
            Strip whitespace from ASCII fields, then check if the resulting
            string is numeric (digits + at most one decimal point) before
            converting to float. Falls back to 0.0 on malformed data.
        """
        s0, s1 = data[0], data[1]
        # Decode 7-byte unit price field (e.g. b'0093.50' -> '93.50' -> 93.5)
        price_str  = data[2:9].decode('ascii', errors='replace').strip()
        # Decode 9-byte volume field (e.g. b'000032.10' -> '32.10' -> 32.1)
        volume_str = data[9:18].decode('ascii', errors='replace').strip()
        # Decode 9-byte amount field (e.g. b'000747.90' -> '747.90' -> 747.9)
        amount_str = data[18:27].decode('ascii', errors='replace').strip()
        return cls(
            state=parse_state(s1),
            raw_status0=s0,
            # Guard against non-numeric/empty strings before float conversion
            unit_price=float(price_str)  if price_str.replace('.', '').isdigit()  else 0.0,
            volume=float(volume_str)     if volume_str.replace('.', '').isdigit() else 0.0,
            amount=float(amount_str)     if amount_str.replace('.', '').isdigit() else 0.0,
        )


@dataclass
class TotalizerResponse:
    """
    Parsed result of a Read Volume Totalizer ('T') or Read Amount Totalizer ('M') command.

    Totalizers are cumulative counters stored in the dispenser's non-volatile memory.
    They count total fuel dispensed (volume) or total money collected (amount) across
    ALL transactions since the dispenser was commissioned. They never reset.

    Usage in transaction flow:
        - Read BEFORE fueling -> stot_volume / stot_amount  (start totalizer)
        - Read AFTER  fueling -> etot_volume / etot_amount  (end totalizer)
        - Difference = actual volume/amount for THIS transaction
          (provides an independent cross-check against the transaction data)

    Response frame layout (20 bytes total):
        [NZA][DUA][Status0][Status1][Totalizer×14][EOC][BCC]

    Attributes:
        state       (PumpState) : Current pump state.
        raw_status0 (int)       : Raw Status0 byte.
        totalizer   (float)     : Cumulative counter value.
                                  Volume: in litres (e.g. 463547.300)
                                  Amount: in currency (e.g. 5137.880)

    Note:
        The same dataclass is used for both volume and amount totalizers since
        the response structure is identical. The caller knows which one it
        requested based on the command sent ('T' vs 'M').
    """

    state: PumpState
    raw_status0: int
    totalizer: float

    @classmethod
    def from_bytes(cls, data: bytes) -> 'TotalizerResponse':
        """
        Parse the 16-byte payload of a Read Totalizer response.

        Payload structure:
            data[0]     = Status0
            data[1]     = Status1 (pump state)
            data[2:16]  = Totalizer (14 ASCII bytes, e.g. b'000463547.300 ')

        Args:
            data (bytes): 16-byte payload slice (raw[2:-2] from response).

        Returns:
            TotalizerResponse: Populated dataclass with totalizer as float.
        """
        s0, s1 = data[0], data[1]
        # Decode 14-byte totalizer field, strip whitespace padding
        tot_str = data[2:16].decode('ascii', errors='replace').strip()
        return cls(
            state=parse_state(s1),
            raw_status0=s0,
            totalizer=float(tot_str) if tot_str.replace('.', '').isdigit() else 0.0,
        )


@dataclass
class PresetReadResponse:
    """
    Parsed result of a Read Preset ('H') command response.

    Used to read back the preset value currently stored in the dispenser,
    and to check whether a preset has been set and what mode it is in.

    Response frame layout (18 bytes total):
        [NZA][DUA][Status0][Status1][Mode×1][Type×1][PresetData×10][EOC][BCC]

    Attributes:
        state        (PumpState) : Current pump state.
        raw_status0  (int)       : Raw Status0 byte.
        mode         (str)       : Preset source mode:
                                     'N' = No preset (dispenser will run freely)
                                     'M' = Manual preset (set by operator at keypad)
                                     'R' = Remote preset (set by this master via 'P' command)
        preset_type  (str)       : '0' = amount preset, '1' = volume preset.
        preset_value (float)     : The actual preset value (amount or volume).
    """

    state: PumpState
    raw_status0: int
    mode: str
    preset_type: str
    preset_value: float

    @classmethod
    def from_bytes(cls, data: bytes) -> 'PresetReadResponse':
        """
        Parse the 14-byte payload of a Read Preset response.

        Payload structure:
            data[0]     = Status0
            data[1]     = Status1 (pump state)
            data[2]     = Mode byte (0x4E='N', 0x4D='M', 0x52='R')
            data[3]     = Type byte (0x30='0'=amount, 0x31='1'=volume)
            data[4:14]  = PresetData (10 ASCII bytes with decimal)

        Args:
            data (bytes): 14-byte payload slice.

        Returns:
            PresetReadResponse: Populated dataclass.
        """
        s0, s1 = data[0], data[1]
        # Map raw mode bytes to human-readable characters
        mode_map = {0x4E: 'N', 0x4D: 'M', 0x52: 'R'}
        # Map raw type bytes to type characters
        type_map = {0x30: '0', 0x31: '1'}
        mode_byte = data[2]
        type_byte = data[3]
        preset_str = data[4:14].decode('ascii', errors='replace').strip()
        return cls(
            state=parse_state(s1),
            raw_status0=s0,
            mode=mode_map.get(mode_byte, '?'),      # '?' if unrecognised byte
            preset_type=type_map.get(type_byte, '?'),
            preset_value=float(preset_str) if preset_str.replace('.', '').isdigit() else 0.0,
        )


# ── Preset and rate encoding ──────────────────────────────────────────────────

def encode_preset_data(preset_type: PresetType, value: float) -> bytes:
    """
    Encode a preset value into the 7-byte ASCII format required by the Set Preset command.

    The dispenser expects the preset as a zero-padded 7-digit ASCII string with
    an implicit decimal point. The position of the decimal depends on the preset type.

    Encoding rules (TQCL Section 8.2.4):
        AMOUNT preset:
            Value is in the local currency (e.g. Rupees).
            Multiplied by 100 to shift 2 decimal places into integer.
            Formatted as 7 zero-padded digits.
            Example: Rs. 500.00 -> round(500.00 * 100) = 50000 -> b'0050000'
            Example: Rs. 1000.50 -> round(1000.50 * 100) = 100050 -> b'0100050'

        VOLUME preset:
            Value is in litres.
            Multiplied by 100 (10 ml resolution per TQCL spec).
            Formatted as 7 zero-padded digits.
            Example: 50.000 L -> round(50.000 * 100) = 5000 -> b'0005000'
            Example: 100.000 L -> round(100.000 * 100) = 10000 -> b'0010000'

    Args:
        preset_type (PresetType) : AMOUNT or VOLUME - determines encoding scale.
        value       (float)      : Preset value in rupees (AMOUNT) or litres (VOLUME).

    Returns:
        bytes: 7-byte ASCII encoded preset, e.g. b'0050000'.

    Notes:
        The maximum encodable value in 7 digits is 99999.99 (AMOUNT) or 999.999 L (VOLUME).
        For a "fill tank" operation, use a large AMOUNT preset (e.g. Rs. 5000).
    """
    if preset_type == PresetType.AMOUNT:
        # Amount: 2 implied decimal places (paise/cents resolution)
        encoded = round(value * 100)
        return f"{encoded:07d}".encode('ascii')
    else:
        # Volume: 2 implied decimal places (10ml resolution)
        encoded = round(value * 100)
        return f"{encoded:07d}".encode('ascii')


def encode_rate_data(price_per_litre: float) -> bytes:
    """
    Encode a unit price into the 7-byte ASCII format required by the Set Rate command.

    Unlike preset encoding which uses an implied decimal, the rate field includes
    an explicit decimal point in the ASCII string, formatted with 2 decimal places.

    Format: NNNN.DD (7 chars total including the '.' character)

    Args:
        price_per_litre (float): Price per litre in local currency.
                                 Example: 103.50

    Returns:
        bytes: 7-byte ASCII encoded rate, e.g. b'0103.50'

    Examples:
        encode_rate_data(93.50)  -> b'0093.50'
        encode_rate_data(103.50) -> b'0103.50'
        encode_rate_data(89.00)  -> b'0089.00'
    """
    # {:07.2f} means: total 7 chars wide, 2 decimal places, zero-padded
    return f"{price_per_litre:07.2f}".encode('ascii')


# ── Command frame builders ────────────────────────────────────────────────────
# Each function builds and returns one ready-to-send frame bytes object.
# They all call build_frame() internally.
# The caller (NozzleController) passes these to RS485Transport.send_recv().

def cmd_status_poll(nza: int, dua: int) -> bytes:
    """
    Build a Status Poll command frame (TQCL Section 8.1.1).

    The most frequently sent command. Polls the current operational state
    of a specific nozzle. The dispenser replies with a 6-byte response
    containing Status0 and Status1 bytes.

    This command has no data payload - it is the simplest possible frame:
        [NZA] [DUA] ['S'=0x53] [EOC=0x7F] [BCC]

    Args:
        nza (int): Nozzle Address (1-based). Example: 0x01 for Nozzle 1.
        dua (int): Dispenser Unit Address. Example: 0x42 for unit 2.

    Returns:
        bytes: 5-byte command frame.

    Example:
        cmd_status_poll(0x01, 0x42) -> b'\\x01\\x42\\x53\\x7F\\x6F'
        (BCC = 0x01 ^ 0x42 ^ 0x53 ^ 0x7F = 0x6F)
    """
    return build_frame(nza, dua, ord('S'))


def cmd_check_preset(nza: int, dua: int) -> bytes:
    """
    Build a Check Preset command frame (TQCL Section 8.1.2).

    Asks the dispenser to confirm whether a preset has been accepted.
    Returns a 5-byte ACK response (Y or X).

    Frame: [NZA] [DUA] ['C'=0x43] [EOC=0x7F] [BCC]

    Args:
        nza (int): Nozzle Address.
        dua (int): Dispenser Unit Address.

    Returns:
        bytes: 5-byte command frame.
    """
    return build_frame(nza, dua, ord('C'))


def cmd_read_transaction(nza: int, dua: int) -> bytes:
    """
    Build a Read Transaction command frame (TQCL Section 8.1.3).

    Retrieves the details of the most recently completed transaction:
    unit price, volume dispensed, and amount charged.

    Should be sent AFTER the pump enters PAYABLE state and BEFORE Clear Sale.
    The dispenser holds this data until Clear Sale is sent.

    Frame    : [NZA] [DUA] ['R'=0x52] [EOC=0x7F] [BCC]
    Response : 31 bytes (see TransactionResponse)

    Args:
        nza (int): Nozzle Address.
        dua (int): Dispenser Unit Address.

    Returns:
        bytes: 5-byte command frame.
    """
    return build_frame(nza, dua, ord('R'))


def cmd_read_volume_totalizer(nza: int, dua: int) -> bytes:
    """
    Build a Read Volume Totalizer command frame (TQCL Section 8.1.4).

    Reads the cumulative total volume (in litres) dispensed by this nozzle
    since commissioning. This is a non-resettable counter stored in the
    dispenser's NVRAM.

    Read BEFORE fueling (start totalizer) and AFTER fueling (end totalizer).
    The difference gives an independent measure of volume for this transaction.

    Frame    : [NZA] [DUA] ['T'=0x54] [EOC=0x7F] [BCC]
    Response : 20 bytes (see TotalizerResponse)

    Args:
        nza (int): Nozzle Address.
        dua (int): Dispenser Unit Address.

    Returns:
        bytes: 5-byte command frame.
    """
    return build_frame(nza, dua, ord('T'))


def cmd_read_amount_totalizer(nza: int, dua: int) -> bytes:
    """
    Build a Read Amount Totalizer command frame (TQCL Section 8.1.5).

    Same as Read Volume Totalizer but returns the cumulative monetary amount
    collected since commissioning, in the local currency.

    Frame    : [NZA] [DUA] ['M'=0x4D] [EOC=0x7F] [BCC]
    Response : 20 bytes (see TotalizerResponse)

    Args:
        nza (int): Nozzle Address.
        dua (int): Dispenser Unit Address.

    Returns:
        bytes: 5-byte command frame.
    """
    return build_frame(nza, dua, ord('M'))


def cmd_read_preset(nza: int, dua: int) -> bytes:
    """
    Build a Read Preset command frame (TQCL Section 8.1.31).

    Reads back the current preset stored in the dispenser.
    Useful for verifying that a Set Preset command was accepted correctly.

    Frame    : [NZA] [DUA] ['H'=0x48] [EOC=0x7F] [BCC]
    Response : 18 bytes (see PresetReadResponse)

    Args:
        nza (int): Nozzle Address.
        dua (int): Dispenser Unit Address.

    Returns:
        bytes: 5-byte command frame.
    """
    return build_frame(nza, dua, ord('H'))


def cmd_authorize(nza: int, dua: int) -> bytes:
    """
    Build an Authorize command frame (TQCL Section 8.2.1).

    Grants permission for the dispenser to start fueling.
    Must be sent after Set Preset ('P') while the pump is in PRESET_READY state.

    The dispenser will transition to AUTHORIZED then FUELING after this command.
    If the pump is not ready for authorisation, the dispenser responds with NAK ('X').

    Frame    : [NZA] [DUA] ['A'=0x41] [EOC=0x7F] [BCC]
    Response : 5 bytes - ACK ('Y') or NAK ('X')

    Retry policy (from TQCL analysis):
        5 retries, 100ms delay between attempts.
        In the NozzleController this maps to pcfg['authorize_retries'] and
        pcfg['authorize_delay'].

    Args:
        nza (int): Nozzle Address.
        dua (int): Dispenser Unit Address.

    Returns:
        bytes: 5-byte command frame.

    Example:
        cmd_authorize(0x01, 0x42) -> b'\\x01\\x42\\x41\\x7F\\x7D'
    """
    return build_frame(nza, dua, ord('A'))


def cmd_clear_sale(nza: int, dua: int, txn_id: Optional[str] = None) -> bytes:
    """
    Build a Clear Sale command frame (TQCL Sections 8.2.2 / 8.2.3).

    Signals to the dispenser that the transaction has been recorded by the master
    and it is safe to clear the transaction data and return to IDLE.
    This is the LAST command in every transaction cycle.

    Two variants:
        Without TxID: [NZA] [DUA] ['F'=0x46] [EOC=0x7F] [BCC]
                      Simple clear, 5-byte frame.

        With TxID:    [NZA] [DUA] ['F'=0x46] [TxID 16 bytes ASCII] [EOC=0x7F] [BCC]
                      Extended clear with 16-byte ASCII transaction ID embedded.
                      The dispenser stores the TxID in its log.
                      21-byte frame.

    Args:
        nza    (int)           : Nozzle Address.
        dua    (int)           : Dispenser Unit Address.
        txn_id (str, optional) : Transaction identifier string (max 16 chars).
                                 If longer than 16 chars, it is truncated.
                                 If shorter, padded with spaces on the right.
                                 If None, the simple clear variant is used.

    Returns:
        bytes: 5-byte frame (no txn_id) or 21-byte frame (with txn_id).

    Example TxID format: '20260328100000A1'  (timestamp + pump_char + nozzle_id)
    """
    if txn_id:
        # Encode, pad to exactly 16 bytes, truncate if over
        data = txn_id.encode('ascii').ljust(16, b' ')[:16]
        return build_frame(nza, dua, ord('F'), data)
    return build_frame(nza, dua, ord('F'))


def cmd_set_preset(nza: int, dua: int, preset_type: PresetType, value: float) -> bytes:
    """
    Build a Set Preset command frame (TQCL Section 8.2.4).

    Programs the dispenser with a preset limit before authorising fueling.
    The dispenser will automatically stop fueling when the preset is reached.

    Frame: [NZA] [DUA] ['P'=0x50] [Type byte] [PresetData 7 bytes] [EOC=0x7F] [BCC]
    Total: 12 bytes.

    The Type byte:
        0x30 ('0') = AMOUNT preset (stop at this monetary value)
        0x31 ('1') = VOLUME preset (stop at this volume in litres)

    Args:
        nza         (int)        : Nozzle Address.
        dua         (int)        : Dispenser Unit Address.
        preset_type (PresetType) : AMOUNT or VOLUME.
        value       (float)      : Preset limit value.
                                   AMOUNT: in local currency (e.g. 5000.00 = Rs.5000)
                                   VOLUME: in litres (e.g. 50.000 = 50 litres)

    Returns:
        bytes: 12-byte command frame.
    """
    preset_data = encode_preset_data(preset_type, value)
    # Type byte followed by 7-byte encoded value
    return build_frame(nza, dua, ord('P'), bytes([preset_type]) + preset_data)


def cmd_cancel_preset(nza: int, dua: int) -> bytes:
    """
    Build a Cancel Preset command frame (TQCL Section 8.2.5).

    Cancels a previously set preset, returning the dispenser to no-preset mode.
    Typically sent if authorisation fails and the transaction needs to be aborted.

    Frame    : [NZA] [DUA] ['E'=0x45] [EOC=0x7F] [BCC]
    Response : 5 bytes ACK

    Args:
        nza (int): Nozzle Address.
        dua (int): Dispenser Unit Address.

    Returns:
        bytes: 5-byte command frame.
    """
    return build_frame(nza, dua, ord('E'))


def cmd_suspend_sale(nza: int, dua: int) -> bytes:
    """
    Build a Suspend Sale command frame (TQCL Section 8.2.6).

    Temporarily pauses an ongoing fueling operation.
    The pump state transitions to SUSPENDED.
    Resume with cmd_resume_sale() when ready to continue.

    Frame    : [NZA] [DUA] ['D'=0x44] [EOC=0x7F] [BCC]
    Response : 5 bytes ACK

    Args:
        nza (int): Nozzle Address.
        dua (int): Dispenser Unit Address.

    Returns:
        bytes: 5-byte command frame.
    """
    return build_frame(nza, dua, ord('D'))


def cmd_resume_sale(nza: int, dua: int) -> bytes:
    """
    Build a Resume Sale command frame (TQCL Section 8.2.7).

    Resumes fueling after a Suspend Sale command.
    The pump transitions back from SUSPENDED to FUELING.

    Frame    : [NZA] [DUA] ['U'=0x55] [EOC=0x7F] [BCC]
    Response : 5 bytes ACK

    Args:
        nza (int): Nozzle Address.
        dua (int): Dispenser Unit Address.

    Returns:
        bytes: 5-byte command frame.
    """
    return build_frame(nza, dua, ord('U'))


def cmd_pump_start(nza: int, dua: int) -> bytes:
    """
    Build a Pump Start command frame (TQCL Section 8.2.8).

    Switches the dispenser into Remote Control Mode, giving this master
    full control over the transaction cycle. Must be sent at the beginning
    of each session to establish master control.

    Without this command, the dispenser may operate in standalone (manual)
    mode without waiting for authorisation from the master.

    Frame    : [NZA] [DUA] ['O'=0x4F] [EOC=0x7F] [BCC]
    Response : 5 bytes ACK

    Args:
        nza (int): Nozzle Address.
        dua (int): Dispenser Unit Address.

    Returns:
        bytes: 5-byte command frame.

    Example:
        cmd_pump_start(0x01, 0x42) -> b'\\x01\\x42\\x4F\\x7F\\x73'
    """
    return build_frame(nza, dua, ord('O'))


def cmd_pump_stop(nza: int, dua: int) -> bytes:
    """
    Build a Pump Stop command frame (TQCL Section 8.2.9).

    Immediately stops the fuel pump motor. Used as an emergency stop or
    when a timeout occurs during fueling (e.g. payable_timeout exceeded).

    Frame    : [NZA] [DUA] ['Z'=0x5A] [EOC=0x7F] [BCC]
    Response : 5 bytes ACK

    Args:
        nza (int): Nozzle Address.
        dua (int): Dispenser Unit Address.

    Returns:
        bytes: 5-byte command frame.

    Example:
        cmd_pump_stop(0x01, 0x42) -> b'\\x01\\x42\\x5A\\x7F\\x66'
    """
    return build_frame(nza, dua, ord('Z'))


def cmd_set_rate(nza: int, dua: int, price_per_litre: float) -> bytes:
    """
    Build a Set Rate command frame (TQCL Section 8.2.10).

    Programs the dispenser with the current unit price per litre.
    Typically sent once at startup or when prices change.
    The dispenser uses this rate for the amount calculation during fueling.

    Frame: [NZA] [DUA] ['B'=0x42] [RateData 7 bytes] [EOC=0x7F] [BCC]
    Total: 12 bytes.

    Args:
        nza             (int)  : Nozzle Address.
        dua             (int)  : Dispenser Unit Address.
        price_per_litre (float): Price per litre in local currency.
                                 Example: 93.50 for Rs. 93.50/L

    Returns:
        bytes: 12-byte command frame.

    Example:
        cmd_set_rate(0x01, 0x42, 93.50)
        Rate data: b'0093.50'
    """
    rate_data = encode_rate_data(price_per_litre)
    return build_frame(nza, dua, ord('B'), rate_data)


# ── Response parsers ──────────────────────────────────────────────────────────
# Each parse_*() function:
#   1. Calls verify_response() to validate frame integrity
#   2. Checks the expected byte length
#   3. Extracts the payload slice (strips NZA, DUA, EOC, BCC)
#   4. Delegates to the appropriate dataclass .from_bytes() method
#   5. Returns a typed dataclass on success, or None on any failure
#
# Returning None (rather than raising) allows the retry loop in
# NozzleController._retry() to handle failures gracefully.

def parse_status_poll(raw: bytes, nza: int, dua: int) -> Optional[StatusResponse]:
    """
    Parse a raw Status Poll response into a StatusResponse object.

    Expected response: 6 bytes
        [NZA] [DUA] [Status0] [Status1] [EOC=0x7F] [BCC]

    Args:
        raw (bytes): Raw bytes read from the serial port.
        nza (int)  : Expected NZA (for frame verification).
        dua (int)  : Expected DUA (for frame verification).

    Returns:
        StatusResponse if frame is valid and complete, else None.

    Typical failure reasons:
        - Serial timeout -> raw is empty or too short
        - Line noise -> BCC mismatch
        - Wrong DUA -> NZA/DUA mismatch (addressed wrong device)
    """
    if not verify_response(raw, nza, dua):
        return None
    if len(raw) < 6:
        return None
    # Payload = bytes between address fields and frame trailer
    payload = raw[2:-2]   # strips NZA, DUA (front) and EOC, BCC (back)
    if len(payload) < 2:
        return None
    return StatusResponse.from_bytes(payload)


def parse_ack_response(raw: bytes, nza: int, dua: int) -> Optional[bool]:
    """
    Parse a simple 5-byte ACK/NAK response.

    Used for commands that return only a success/failure indication:
    Authorize, Clear Sale, Set Preset, Cancel Preset, Pump Start, Pump Stop,
    Suspend Sale, Resume Sale, Set Rate, Check Preset.

    Expected response: 5 bytes
        [NZA] [DUA] ['Y'=0x59 or 'X'=0x58] [EOC=0x7F] [BCC]

    Args:
        raw (bytes): Raw bytes from serial port.
        nza (int)  : Expected NZA.
        dua (int)  : Expected DUA.

    Returns:
        True  : Dispenser sent 'Y' (0x59) - command accepted.
        False : Dispenser sent 'X' (0x58) - command rejected.
        None  : Frame invalid (BCC error, wrong address, timeout, etc.)

    Note:
        The caller (NozzleController._retry) treats None as "try again"
        and False as "authorisation denied" which may trigger abort logic.
    """
    if not verify_response(raw, nza, dua):
        return None
    if len(raw) < 5:
        return None
    ack_byte = raw[2]           # ACK byte is the 3rd byte (index 2)
    if ack_byte == ACK_POS:     # 0x59 = 'Y' = success
        return True
    if ack_byte == ACK_NEG:     # 0x58 = 'X' = failure
        return False
    return None                 # unexpected byte, treat as comms error


def parse_read_transaction(raw: bytes, nza: int, dua: int) -> Optional[TransactionResponse]:
    """
    Parse a raw Read Transaction response into a TransactionResponse object.

    Expected response: 31 bytes
        [NZA][DUA][S0][S1][Price×7][Volume×9][Amount×9][EOC][BCC]

    Args:
        raw (bytes): 31-byte raw response from serial port.
        nza (int)  : Expected NZA.
        dua (int)  : Expected DUA.

    Returns:
        TransactionResponse if valid, else None.
    """
    if not verify_response(raw, nza, dua):
        return None
    if len(raw) < 31:
        return None
    # 27-byte payload: Status0(1) + Status1(1) + Price(7) + Volume(9) + Amount(9)
    payload = raw[2:-2]
    return TransactionResponse.from_bytes(payload)


def parse_volume_totalizer(raw: bytes, nza: int, dua: int) -> Optional[TotalizerResponse]:
    """
    Parse a raw Read Volume Totalizer response into a TotalizerResponse object.

    Expected response: 20 bytes
        [NZA][DUA][Status0][Status1][VolumeTotalizer×14][EOC][BCC]

    Args:
        raw (bytes): 20-byte raw response from serial port.
        nza (int)  : Expected NZA.
        dua (int)  : Expected DUA.

    Returns:
        TotalizerResponse with cumulative volume in litres, or None on failure.
    """
    if not verify_response(raw, nza, dua):
        return None
    if len(raw) < 20:
        return None
    # 16-byte payload: Status0(1) + Status1(1) + Totalizer(14)
    payload = raw[2:-2]
    return TotalizerResponse.from_bytes(payload)


def parse_amount_totalizer(raw: bytes, nza: int, dua: int) -> Optional[TotalizerResponse]:
    """
    Parse a raw Read Amount Totalizer response into a TotalizerResponse object.

    Identical structure to parse_volume_totalizer() but the totalizer
    value represents cumulative monetary amount instead of volume.

    Expected response: 20 bytes
        [NZA][DUA][Status0][Status1][AmountTotalizer×14][EOC][BCC]

    Args:
        raw (bytes): 20-byte raw response from serial port.
        nza (int)  : Expected NZA.
        dua (int)  : Expected DUA.

    Returns:
        TotalizerResponse with cumulative amount in local currency, or None.
    """
    if not verify_response(raw, nza, dua):
        return None
    if len(raw) < 20:
        return None
    payload = raw[2:-2]
    return TotalizerResponse.from_bytes(payload)


def parse_preset_read(raw: bytes, nza: int, dua: int) -> Optional[PresetReadResponse]:
    """
    Parse a raw Read Preset response into a PresetReadResponse object.

    Handles two response forms that Tokheim dispensers may return:

        Full preset  (18 bytes): [NZA][DUA][S0][S1][Mode][Type][PresetData×10][EOC][BCC]
        No preset     (7 bytes): [NZA][DUA][S0][S1][Mode=N=0x4E][EOC][BCC]

    Some dispensers always return the short 7-byte form when no preset is set
    (Mode='N'). Both forms are validated (BCC + NZA/DUA echo) before parsing.

    Args:
        raw (bytes): Raw response from serial port (7 or 18 bytes).
        nza (int)  : Expected NZA.
        dua (int)  : Expected DUA.

    Returns:
        PresetReadResponse if valid, else None.
    """
    if not verify_response(raw, nza, dua):
        return None

    # Short form: 7 bytes — [NZA][DUA][S0][S1][Mode=N][EOC][BCC]
    # Tokheim dispensers return this when no preset is set (mode='N').
    if len(raw) == 7 and raw[4] == 0x4E:  # 0x4E = 'N' = no preset
        return PresetReadResponse(
            state=parse_state(raw[3]),
            raw_status0=raw[2],
            mode='N',
            preset_type='0',
            preset_value=0.0,
        )

    # Full form: 18 bytes — includes Type and PresetData
    if len(raw) < 18:
        return None
    # 14-byte payload: Status0(1) + Status1(1) + Mode(1) + Type(1) + PresetData(10)
    payload = raw[2:-2]
    return PresetReadResponse.from_bytes(payload)
