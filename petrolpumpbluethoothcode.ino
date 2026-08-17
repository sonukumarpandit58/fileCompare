#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

// =====================================================
// NORDIC UART UUIDs
// =====================================================

#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

#define BLE_DEVICE_NAME        "NWARE01"

#define RS485_BUF_SIZE         64
#define HEX_BUF_SIZE           (RS485_BUF_SIZE * 3 + 1)
#define MIN_VALID_BYTES        2

// USB CDC baud (Pi bridge writes at this baud)
#define USB_BAUD               115200

// =====================================================

BLECharacteristic *txCharacteristic;
volatile int connectedCount = 0;

HardwareSerial rs485(2);

static void flushRS485()
{
    while (rs485.available()) rs485.read();
}

// Emit raw ASCII already-formatted string as BLE notify.
// The Pi bridge sends lines like "RX 01 42 01 30 7F 0D\n" — pass through as-is
// so the phone gets a human-readable line without hex-of-hex re-encoding.
static void bleNotifyRaw(const uint8_t *buf, int len)
{
    if (len <= 0 || connectedCount <= 0) return;
    txCharacteristic->setValue((uint8_t *)buf, len);
    txCharacteristic->notify();
    delay(20);
}

// Hex-encode raw byte buffer and BLE notify (used for RS-485 tap traffic).
static void bleNotifyHex(const uint8_t *raw, int n)
{
    if (n <= 0 || connectedCount <= 0) return;
    static char hexBuf[HEX_BUF_SIZE];
    int pos = 0;
    for (int i = 0; i < n; i++) {
        hexBuf[pos++] = "0123456789abcdef"[raw[i] >> 4];
        hexBuf[pos++] = "0123456789abcdef"[raw[i] & 0x0F];
        hexBuf[pos++] = ' ';
    }
    hexBuf[pos] = '\0';
    txCharacteristic->setValue((uint8_t *)hexBuf, pos);
    txCharacteristic->notify();
    delay(20);
}

// =====================================================
// SERVER CALLBACKS
// =====================================================

class MyServerCallbacks : public BLEServerCallbacks
{
    void onConnect(BLEServer* pServer)
    {
        connectedCount++;
        Serial.print("BLE Connected: ");
        Serial.println(connectedCount);
        BLEDevice::startAdvertising();
    }
    void onDisconnect(BLEServer* pServer)
    {
        if (connectedCount > 0) connectedCount--;
        Serial.print("BLE Disconnected: ");
        Serial.println(connectedCount);
        BLEDevice::startAdvertising();
    }
};

// =====================================================
// RX CALLBACK (BLE -> RS485)
// =====================================================

class RXCallbacks : public BLECharacteristicCallbacks
{
    void onWrite(BLECharacteristic *pCharacteristic)
    {
        String value = pCharacteristic->getValue();
        if (value.length() > 0) {
            for (int i = 0; i < (int)value.length(); i++) {
                rs485.write((uint8_t)value[i]);
            }
        }
    }
};

// =====================================================

void setup()
{
    Serial.begin(USB_BAUD);
    rs485.begin(9600, SERIAL_8N1, 26, 27);
    delay(50);
    flushRS485();

    Serial.println();
    Serial.println("====================================");
    Serial.println(" ESP32 BLE RS485 HEX  v2 (USB+UART) ");
    Serial.println("====================================");

    BLEDevice::init(BLE_DEVICE_NAME);
    BLEDevice::setMTU(512);

    BLEServer *pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());

    BLEService *pService = pServer->createService(SERVICE_UUID);

    txCharacteristic = pService->createCharacteristic(
        CHARACTERISTIC_UUID_TX,
        BLECharacteristic::PROPERTY_NOTIFY);
    txCharacteristic->addDescriptor(new BLE2902());

    BLECharacteristic *rxCharacteristic = pService->createCharacteristic(
        CHARACTERISTIC_UUID_RX,
        BLECharacteristic::PROPERTY_WRITE);
    rxCharacteristic->setCallbacks(new RXCallbacks());

    pService->start();

    BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->setMinPreferred(0x06);
    pAdvertising->setMaxPreferred(0x12);
    pAdvertising->start();

    Serial.print("BLE Started -- Name: ");
    Serial.println(BLE_DEVICE_NAME);
}

// =====================================================

void loop()
{
    // ---------- Path 1: RS-485 (GPIO26/27) tap -> BLE hex ----------
    // Same behaviour as v1; live sites (Vikhroli / KMP) don't change.
    static uint8_t rawBuf[RS485_BUF_SIZE];
    int byteCount = 0;

    if (rs485.available()) {
        unsigned long lastByte = millis();
        while (byteCount < RS485_BUF_SIZE) {
            if (rs485.available()) {
                rawBuf[byteCount++] = rs485.read();
                lastByte = millis();
            } else if (millis() - lastByte >= 5) break;
        }
    }
    int nonZero = 0;
    for (int i = 0; i < byteCount; i++) if (rawBuf[i] != 0x00) nonZero++;
    if (byteCount > 0 && nonZero < MIN_VALID_BYTES) { flushRS485(); byteCount = 0; }
    if (byteCount > 0) bleNotifyHex(rawBuf, byteCount);

    // ---------- Path 2: USB CDC (from Pi) -> BLE pass-through ----------
    // Pi bridge writes ready-to-display ASCII lines terminated by '\n'.
    // We collect one line and notify as-is so the phone terminal shows
    // exactly what the bridge sent (no hex-of-hex re-encoding).
    static uint8_t usbLine[192];
    static int     usbLen = 0;

    while (Serial.available()) {
        uint8_t b = (uint8_t)Serial.read();
        if (b == '\r') continue;
        if (b == '\n' || usbLen >= (int)sizeof(usbLine) - 1) {
            if (usbLen > 0) {
                usbLine[usbLen] = '\0';
                bleNotifyRaw(usbLine, usbLen);
                usbLen = 0;
            }
            if (b != '\n') { usbLine[usbLen++] = b; }
            continue;
        }
        usbLine[usbLen++] = b;
    }

    delay(5);
}