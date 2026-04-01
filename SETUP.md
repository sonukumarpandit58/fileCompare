# RPi4 Fuel Automation - Setup & Deployment Guide

## Overview

This program runs on a **Raspberry Pi 4B (4GB)** and replaces the ESP32-based C1 + DU device
stack. It talks directly to Tokheim ELC fuel dispensers over RS485 using the
**TQCL v2.06 Rev.7** protocol, and publishes transaction data to Azure IoT Hub via MQTT.

```
Raspberry Pi 4B
    |
    | USB-RS485 adapter (/dev/ttyUSB0)
    |
Tokheim ELC Dispenser (DUA 0x42)
    |-- Nozzle 1 (NZA 0x01) - HSD
    |-- Nozzle 2 (NZA 0x02) - EBMS

Tokheim ELC Dispenser (DUA 0x43)
    |-- Nozzle 3 (NZA 0x01) - HSD
    |-- Nozzle 4 (NZA 0x02) - EBMS (disabled)
```

---

## Hardware Requirements

- Raspberry Pi 4B (4GB RAM)
- USB to RS485 adapter (CH340 or FT232 chipset recommended)
- RS485 cable connected to Tokheim dispenser FIP port
- MicroSD card (16GB+)
- Raspbian OS (tested on kernel 6.12.47, Python 3.13.5)

---

## Site Details

| Field            | Value                        |
|------------------|------------------------------|
| Site ID          | BPCL_181846                  |
| Site Name        | BPCL Retail Outlet 181846    |
| RPi Hostname     | nware1                       |
| RPi IP           | 192.168.1.225                |
| RPi User         | nware                        |
| Protocol         | TQCL v2.06 Rev.7             |
| Baud Rate        | 9600                         |
| Serial Port      | /dev/ttyUSB0                 |

---

## File Structure

```
rpi_automation/
    main.py              # Main orchestrator - entry point
    tqcl_protocol.py     # TQCL frame builder, BCC, command encoders, response parsers
    pump_controller.py   # Per-nozzle state machine (full transaction cycle)
    cloud_publisher.py   # Azure IoT Hub MQTT publisher + local retry cache
    config.yaml          # Site, pump, MQTT, and protocol configuration
    requirements.txt     # Python package dependencies
    SETUP.md             # This file
```

---

## First-Time Installation on RPi

### 1. SSH into the RPi
```bash
ssh nware@192.168.1.225
# password: nware
```

### 2. Clone or copy files
```bash
# If deploying from dev machine via rsync:
rsync -avz --exclude='__pycache__' \
  ./rpi_automation/ \
  nware@192.168.1.225:~/fuel-automation/rpi_automation/

# Or clone from GitHub:
git clone https://github.com/<your-repo>/fuel-automation.git
cd fuel-automation/rpi_automation
```

### 3. Install Python dependencies
```bash
cd ~/fuel-automation/rpi_automation
pip3 install -r requirements.txt --break-system-packages
```

Packages installed:
- `pyserial>=3.5` - RS485/UART serial communication
- `paho-mqtt>=1.6.1` - MQTT client for Azure IoT Hub
- `PyYAML>=6.0` - config file parsing

### 4. Create required directories
```bash
sudo mkdir -p /var/log/fuel-automation /var/lib/fuel-automation/pending
sudo chown -R nware:nware /var/log/fuel-automation /var/lib/fuel-automation
```

### 5. Verify serial port
```bash
# Plug in USB-RS485 adapter, then:
ls /dev/ttyUSB*
# Expected output: /dev/ttyUSB0

# Check user is in dialout group (required for serial access)
groups
# Should include: dialout
```

---

## Configuration (config.yaml)

### Key fields to update before going live:

#### Serial port
```yaml
serial:
  port: "/dev/ttyUSB0"    # change if adapter appears as ttyUSB1, ttyACM0, etc.
  baud_rate: 9600          # Tokheim TQCL default
```

#### Dispenser addressing
```yaml
dispensers:
  - du_id: 1
    dua: 0x42              # DUA byte: range 0x41-0x5F (units 1-31)
    nozzles:
      - nza: 0x01          # NZA = Nozzle Address (1-based)
        nozzle_id: 1
        product: "HSD"
        enabled: true
```

DUA formula: `DUA = 0x40 | unit_number` (e.g., unit 2 = 0x42, unit 3 = 0x43)

#### MQTT / Azure IoT Hub
```yaml
mqtt:
  enabled: true
  broker: "your-iothub.azure-devices.net"
  port: 8883
  client_id: "BPCL_181846_RPI4"
  username: "your-iothub.azure-devices.net/BPCL_181846_RPI4/?api-version=2021-04-12"
  password: ""             # leave blank - use MQTT_PASSWORD env var instead
  tls: true
```

Set the SAS token as an environment variable (never hardcode in config):
```bash
export MQTT_PASSWORD="SharedAccessSignature sr=..."
```

Or add to `/etc/environment` for persistence:
```bash
echo 'MQTT_PASSWORD="SharedAccessSignature sr=..."' | sudo tee -a /etc/environment
```

---

## Running the Program

### Test mode (no hardware needed)
```bash
cd ~/fuel-automation/rpi_automation
python3 main.py --dry-run
```
Dry-run simulates RS485 responses and prints all TX/RX frames without opening the serial port.

### Production mode
```bash
cd ~/fuel-automation/rpi_automation
python3 main.py
# or with explicit config path:
python3 main.py --config config.yaml
```

---

## Autostart with systemd

The service file is installed at `/etc/systemd/system/fuel-automation.service`.

### Enable and start
```bash
sudo systemctl enable fuel-automation    # start on every boot
sudo systemctl start fuel-automation     # start now immediately
```

### Common service commands
```bash
sudo systemctl status fuel-automation    # check if running
sudo systemctl stop fuel-automation      # stop
sudo systemctl restart fuel-automation   # restart after config change
sudo systemctl disable fuel-automation   # disable autostart
```

### View live logs
```bash
sudo journalctl -u fuel-automation -f           # follow live
sudo journalctl -u fuel-automation --since today # today only
sudo journalctl -u fuel-automation -n 100        # last 100 lines
```

Log file is also written to: `/var/log/fuel-automation/rpi_automation.log`
```bash
tail -f /var/log/fuel-automation/rpi_automation.log
```

---

## TQCL Protocol Reference

### Frame format (Non-Secured Mode)
```
Command:   [NZA] [DUA] [CMD] [...data...] [EOC=0x7F] [BCC]
Response:  [NZA] [DUA] [...data...]       [EOC=0x7F] [BCC]

BCC = XOR of all bytes from NZA through EOC (inclusive)
```

### Key command bytes
| Char | Hex  | Command              | Response size |
|------|------|----------------------|---------------|
| 'S'  | 0x53 | Status Poll          | 6 bytes       |
| 'R'  | 0x52 | Read Transaction     | 31 bytes      |
| 'T'  | 0x54 | Read Volume Totalizer| 20 bytes      |
| 'M'  | 0x4D | Read Amount Totalizer| 20 bytes      |
| 'H'  | 0x48 | Read Preset          | 18 bytes      |
| 'C'  | 0x43 | Check Preset         | 5 bytes       |
| 'A'  | 0x41 | Authorize            | 5 bytes (ACK) |
| 'P'  | 0x50 | Set Preset           | 5 bytes (ACK) |
| 'E'  | 0x45 | Cancel Preset        | 5 bytes (ACK) |
| 'F'  | 0x46 | Clear Sale           | 5 bytes (ACK) |
| 'O'  | 0x4F | Pump Start           | 5 bytes (ACK) |
| 'Z'  | 0x5A | Pump Stop            | 5 bytes (ACK) |
| 'D'  | 0x44 | Suspend Sale         | 5 bytes (ACK) |
| 'U'  | 0x55 | Resume Sale          | 5 bytes (ACK) |
| 'B'  | 0x42 | Set Rate             | 5 bytes (ACK) |

ACK responses: `'Y'` (0x59) = success, `'X'` (0x58) = failure

### Pump states (Status1 byte)
| Hex  | Char | State           |
|------|------|-----------------|
| 0x30 | '0'  | IDLE            |
| 0x31 | '1'  | CALL            |
| 0x32 | '2'  | PRESET_READY    |
| 0x33 | '3'  | FUELING         |
| 0x34 | '4'  | PAYABLE         |
| 0x35 | '5'  | SUSPENDED       |
| 0x36 | '6'  | STOPPED         |
| 0x38 | '8'  | INOPERATIVE     |
| 0x39 | '9'  | AUTHORIZED      |
| 0x3B | ';'  | STARTED         |
| 0x3E | '>'  | WAIT_FOR_PRESET |

### Example frames (verified BCC)
```
Status Poll  NZA=0x01 DUA=0x42:  01 42 53 7F 6F
Pump Start   NZA=0x01 DUA=0x42:  01 42 4F 7F 73
Authorize    NZA=0x01 DUA=0x42:  01 42 41 7F 7D
Pump Stop    NZA=0x01 DUA=0x42:  01 42 5A 7F 66
Clear Sale   NZA=0x01 DUA=0x42:  01 42 46 7F 78

IDLE response NZA=0x01 DUA=0x42: 01 42 01 30 7F 0D
  (Status0=0x01: nozzle on-hook, Status1=0x30: IDLE)
```

### Full transaction flow
```
1. Pump Start ('O')         -> remote mode
2. Poll Status ('S')        -> wait for CALL (0x31) = nozzle lifted
3. Set Preset ('P')         -> amount or volume
4. Poll Status ('S')        -> wait for PRESET_READY (0x32)
5. Authorize ('A')          -> 5 retries x 100ms
6. Poll Status ('S')        -> wait for AUTHORIZED (0x39) / FUELING (0x33)
7. Read Volume Total ('T')  -> stot_volume (start totalizer)
8. Read Amount Total ('M')  -> stot_amount (start totalizer)
9. Poll Status ('S')        -> wait for PAYABLE (0x34) or STOPPED (0x36)
10. Read Transaction ('R')  -> volume, amount, unit price
11. Read Volume Total ('T') -> etot_volume (end totalizer)
12. Read Amount Total ('M') -> etot_amount (end totalizer)
13. Clear Sale ('F')        -> with transaction ID
14. Publish to Azure IoT Hub via MQTT
```

---

## Transaction Output Format

### JSON envelope (matches existing C1 firmware format)
```json
{"BPCL_181846_1_HSD_NOZZLE_1": [{"d": "<PSV_string>"}]}
```

### PSV string (21 pipe-separated fields)
```
seq|serial|pump_char|nozzle_id|unit_price|payment_mode|discount|
net_amount|gross_amount|density|stot_volume|stot_amount|
etot_volume|etot_amount|start_time|end_time|transaction_id|type|serial|product|count
```

Example:
```
1|1234567|A|1|93.50|1|0|747.90|747.90|0.83|463547.30|5137.88|463579.40|5885.78|
2026-03-28T10:00:00|2026-03-28T10:05:30|20260328100000A1|transaction|1234567|HSD|1
```

### Pending transactions (offline retry)
If MQTT publish fails, the transaction is saved locally at:
```
/var/lib/fuel-automation/pending/<txn_id>.json
```
On next startup, all pending files are automatically replayed.

---

## Deployment from Dev Machine

From your Mac (where source code lives):

```bash
# One-command deploy to RPi
rsync -avz --exclude='__pycache__' --exclude='*.pyc' \
  /Users/nishantkumar/Documents/GitHub/fuel-automation/rpi_automation/ \
  nware@192.168.1.225:~/fuel-automation/rpi_automation/

# Restart service after deploy
ssh nware@192.168.1.225 'sudo systemctl restart fuel-automation'
```

---

## Troubleshooting

### Serial port not found
```bash
lsusb                  # check USB-RS485 adapter is detected
ls /dev/ttyUSB*        # check device node
dmesg | tail -20       # check kernel messages after plugging in
```

If adapter appears as `/dev/ttyACM0`:
```bash
# Edit config.yaml
serial:
  port: "/dev/ttyACM0"
```

### Permission denied on serial port
```bash
sudo usermod -aG dialout nware
# log out and log back in for group change to take effect
```

### No response from dispenser
1. Check RS485 wiring (A+, B-, GND)
2. Verify baud rate matches dispenser setting (default 9600)
3. Confirm DUA address matches dispenser FIP configuration
4. Enable DEBUG logging in config.yaml to see raw frames:
   ```yaml
   logging:
     level: "DEBUG"
   ```

### MQTT not connecting
```bash
# Test MQTT_PASSWORD is set
echo $MQTT_PASSWORD

# Test broker reachability
openssl s_client -connect your-iothub.azure-devices.net:8883

# Check logs
sudo journalctl -u fuel-automation | grep -i mqtt
```

### Check transaction count file
```bash
cat /var/lib/fuel-automation/txn_count.json
# {"transaction_count": 42}
```

---

## Retry Policies (from TQCL analysis + wayne.bin reverse engineering)

| Command       | Retries | Delay   | Total max wait |
|---------------|---------|---------|----------------|
| Status Poll   | 6       | 250ms   | 1.5 sec        |
| Authorize     | 5       | 100ms   | 500ms          |
| Totalizer     | 5       | 250ms   | 1.25 sec       |
| Preset timeout| -       | -       | 90 sec         |
| Payable wait  | -       | 1 sec   | 300 sec        |

---

## Version History

| Date       | Change                                     |
|------------|--------------------------------------------|
| 2026-03-28 | Initial deployment to RPi4 nware@192.168.1.225 |
|            | TQCL v2.06 Rev.7 protocol implemented      |
|            | 3 active nozzles (HSD x2, EBMS x1)        |
|            | systemd service installed                  |
|            | Dry-run verified on Python 3.13.5 / aarch64|
