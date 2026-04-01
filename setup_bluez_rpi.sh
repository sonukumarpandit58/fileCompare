#!/usr/bin/env bash
# =============================================================================
# setup_bluez_rpi.sh
# Fix BlueZ on RPi for PAX A920 BLE GATT / ATT MTU negotiation
# =============================================================================
#
# PROBLEM
# -------
# PAX A920 (Android BLE) connects to the RPi GATT server, discovers the FCC
# services (0xABF0/ABF1/ABF2), then calls requestMtu(500).  Android sends an
# ATT_EXCHANGE_MTU_REQ to the RPi.  BlueZ never replies with
# ATT_EXCHANGE_MTU_RSP.  After ~17 seconds the PAX times out and disconnects.
#
# ROOT CAUSE
# ----------
# BlueZ GATT server (bless / D-Bus application) requires the --experimental
# flag on bluetoothd for the ATT bearer to be properly initialized on each
# new LE connection.  Without --experimental:
#   - BlueZ accepts the physical LE connection (HCI LE Connection Complete OK)
#   - BlueZ exposes the GATT service database (service discovery works)
#   - But the ATT L2CAP bearer for this specific connection is never fully
#     opened -> ATT_EXCHANGE_MTU_REQ goes unanswered -> PAX timeout
#
# ADDITIONAL FIXES
# ----------------
# 1. Cache = no in [GATT] section of main.conf
#    Prevents BlueZ from serving a stale cached GATT database to PAX on
#    reconnect without re-establishing the ATT bearer from scratch.
#    Symptom: PAX "already knows" the services (from cache) but the ATT
#    bearer for the *new* connection is never set up -> MTU exchange fails.
#
# 2. LE Privacy disabled (PrivacyMode = off)
#    Ensures the RPi always advertises the same BD_ADDR so PAX can reliably
#    identify and reconnect to the device.
#
# 3. BLE MIDI plugin disabled
#    BlueZ ships a MIDI plugin that auto-registers a BLE MIDI GATT service
#    (UUID 03B80E5A-...).  PAX sometimes discovers this extra service and
#    gets confused about which ATT bearer to use for MTU negotiation.
#    Disabling it keeps the GATT service list clean (only 0xABF0).
#
# USAGE
# -----
#   chmod +x setup_bluez_rpi.sh
#   sudo ./setup_bluez_rpi.sh
#   # Reboot or restart services as prompted
#
# =============================================================================

set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

info()    { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; }
section() { echo -e "\n${GREEN}==== $* ====${NC}"; }

if [[ $EUID -ne 0 ]]; then
    error "This script must be run as root (sudo $0)"
    exit 1
fi

# ---------------------------------------------------------------------------
section "Step 1 - Enable bluetoothd --experimental mode"
# ---------------------------------------------------------------------------
OVERRIDE_DIR="/etc/systemd/system/bluetooth.service.d"
OVERRIDE_FILE="${OVERRIDE_DIR}/experimental.conf"

mkdir -p "${OVERRIDE_DIR}"

# Auto-detect bluetoothd binary path - differs by distro/RPi OS version:
#   Raspberry Pi OS Bookworm : /usr/libexec/bluetooth/bluetoothd
#   Raspberry Pi OS Bullseye : /usr/lib/bluetooth/bluetoothd
BLUETOOTHD_BIN=""
for candidate in /usr/libexec/bluetooth/bluetoothd /usr/lib/bluetooth/bluetoothd; do
    if [[ -x "${candidate}" ]]; then
        BLUETOOTHD_BIN="${candidate}"
        break
    fi
done
if [[ -z "${BLUETOOTHD_BIN}" ]]; then
    BLUETOOTHD_BIN=$(which bluetoothd 2>/dev/null || true)
fi
if [[ -z "${BLUETOOTHD_BIN}" ]]; then
    error "Cannot find bluetoothd binary - checked /usr/libexec/bluetooth, /usr/lib/bluetooth, PATH"
    exit 1
fi
info "bluetoothd binary found: ${BLUETOOTHD_BIN}"

# Always write the correct path - overwrite whatever was there before
cat > "${OVERRIDE_FILE}" <<EOF
[Service]
ExecStart=
ExecStart=${BLUETOOTHD_BIN} --experimental
EOF
info "Written ${OVERRIDE_FILE}  (ExecStart=${BLUETOOTHD_BIN} --experimental)"
cat "${OVERRIDE_FILE}"

# ---------------------------------------------------------------------------
section "Step 2 - Configure /etc/bluetooth/main.conf"
# ---------------------------------------------------------------------------
MAIN_CONF="/etc/bluetooth/main.conf"

if [[ ! -f "${MAIN_CONF}" ]]; then
    warn "${MAIN_CONF} not found - creating minimal config ..."
    cat > "${MAIN_CONF}" <<'EOF'
[Policy]
AutoEnable=true

[GATT]
Cache = no

[LE]
# Disable random address rotation so PAX always sees the same BD_ADDR
MinConnectionInterval=7
MaxConnectionInterval=9
ConnectionLatency=0
SupervisionTimeout=100
EOF
    info "Created ${MAIN_CONF}"
else
    info "${MAIN_CONF} exists - patching as needed ..."

    # ── [GATT] Cache = no ────────────────────────────────────────────────
    if grep -qE "^\s*Cache\s*=" "${MAIN_CONF}"; then
        # Update existing Cache line
        sed -i 's/^\s*Cache\s*=.*$/Cache = no/' "${MAIN_CONF}"
        info "  Updated: Cache = no"
    elif grep -q "^\[GATT\]" "${MAIN_CONF}"; then
        # Inject under existing [GATT] section
        sed -i '/^\[GATT\]/a Cache = no' "${MAIN_CONF}"
        info "  Injected: Cache = no under [GATT]"
    else
        # Append new [GATT] section
        echo -e "\n[GATT]\nCache = no" >> "${MAIN_CONF}"
        info "  Appended: [GATT] section with Cache = no"
    fi

    # ── Disable experimental key (use systemd override instead) ──────────
    # Remove any old Experimental=true/false from main.conf to avoid
    # conflicts with the systemd override approach.
    if grep -qE "^\s*Experimental\s*=" "${MAIN_CONF}"; then
        sed -i '/^\s*Experimental\s*=/d' "${MAIN_CONF}"
        info "  Removed legacy Experimental= line (systemd override used instead)"
    fi
fi

# ---------------------------------------------------------------------------
section "Step 3 - Disable BLE MIDI plugin (prevents phantom GATT service)"
# ---------------------------------------------------------------------------
# The MIDI plugin registers service UUID 03B80E5A-EDE8-4B33-A751-6CE34EC4C700
# which confuses PAX during service discovery.  Disable it by creating a
# symlink to /dev/null (mask approach) or by adding to DisablePlugins.

if grep -qE "^\s*DisablePlugins\s*=" "${MAIN_CONF}"; then
    CURRENT=$(grep -E "^\s*DisablePlugins\s*=" "${MAIN_CONF}" | head -1 | sed 's/.*=\s*//')
    if echo "${CURRENT}" | grep -qi "midi"; then
        info "  MIDI plugin already in DisablePlugins - OK"
    else
        NEW_VAL="${CURRENT:+${CURRENT},}midi"
        sed -i "s/^\s*DisablePlugins\s*=.*/DisablePlugins = ${NEW_VAL}/" "${MAIN_CONF}"
        info "  Added midi to DisablePlugins: ${NEW_VAL}"
    fi
else
    # Inject under [Policy] section or append
    if grep -q "^\[Policy\]" "${MAIN_CONF}"; then
        sed -i '/^\[Policy\]/a DisablePlugins = midi' "${MAIN_CONF}"
        info "  Injected DisablePlugins = midi under [Policy]"
    else
        echo -e "\n[Policy]\nDisablePlugins = midi" >> "${MAIN_CONF}"
        info "  Appended [Policy] / DisablePlugins = midi"
    fi
fi

# ---------------------------------------------------------------------------
section "Step 4 - Reload systemd and restart bluetooth"
# ---------------------------------------------------------------------------
systemctl daemon-reload
info "systemd reloaded"

# reset-failed clears the restart rate-limit counter so systemd allows a
# fresh start even if the service had previously hit the 5-restart cap.
systemctl reset-failed bluetooth 2>/dev/null || true
systemctl restart bluetooth
sleep 2

BT_STATUS=$(systemctl is-active bluetooth 2>/dev/null || true)
if [[ "${BT_STATUS}" == "active" ]]; then
    info "bluetooth service restarted OK (status: active)"
else
    error "bluetooth service status: ${BT_STATUS} - check: sudo journalctl -u bluetooth -n 50"
    exit 1
fi

# Verify --experimental is in the running bluetoothd cmdline
sleep 1
if cat /proc/$(pidof bluetoothd 2>/dev/null || echo "0")/cmdline 2>/dev/null | tr '\0' ' ' | grep -q "experimental"; then
    info "bluetoothd --experimental confirmed active in running process"
else
    warn "Could not confirm --experimental in running bluetoothd cmdline"
    warn "Run: ps aux | grep bluetoothd  - to verify manually"
fi

# ---------------------------------------------------------------------------
section "Step 5 - Restart fuel-automation service (if running)"
# ---------------------------------------------------------------------------
if systemctl is-active --quiet fuel-automation 2>/dev/null; then
    systemctl restart fuel-automation
    sleep 2
    FA_STATUS=$(systemctl is-active fuel-automation 2>/dev/null || true)
    info "fuel-automation restarted (status: ${FA_STATUS})"
else
    warn "fuel-automation service not running - start it manually when ready:"
    warn "  sudo systemctl start fuel-automation"
fi

# ---------------------------------------------------------------------------
section "Summary"
# ---------------------------------------------------------------------------
echo ""
echo "  BlueZ configuration applied:"
echo "    [+] ${OVERRIDE_FILE}"
echo "        ExecStart=/usr/lib/bluetooth/bluetoothd --experimental"
echo "    [+] ${MAIN_CONF}"
echo "        [GATT] Cache = no"
echo "        [Policy] DisablePlugins = midi (or similar)"
echo ""
echo "  Expected result:"
echo "    PAX A920 connects -> discovers 0xABF0 services -> requestMtu(500)"
echo "    BlueZ responds ATT_EXCHANGE_MTU_RSP(512) within 1-2 seconds"
echo "    PAX proceeds to write commands (no 17-second disconnect)"
echo ""
echo "  If PAX still fails after this, capture a btmon trace:"
echo "    sudo btmon -w /tmp/pax.btsnoop &"
echo "    # connect PAX, wait 30s"
echo "    kill %1"
echo "    # share /tmp/pax.btsnoop for analysis"
echo ""
info "setup_bluez_rpi.sh complete"
