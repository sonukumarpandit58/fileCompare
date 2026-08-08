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

// Max RS485 frame buffered before BLE send.
// Each byte becomes "XX " (3 chars): 64 bytes -> 192 chars, within MTU 512.
#define RS485_BUF_SIZE         64
#define HEX_BUF_SIZE           (RS485_BUF_SIZE * 3 + 1)

// Minimum non-zero bytes required to treat a frame as valid.
// Filters out idle-bus noise (all 0x00) and single-byte glitches.
#define MIN_VALID_BYTES        2

// =====================================================

BLECharacteristic *txCharacteristic;

volatile int connectedCount = 0;

// =====================================================

// RS485 UART
// RX = GPIO26
// TX = GPIO27
// =====================================================

HardwareSerial rs485(2);

// Drain and discard everything in the RS485 RX FIFO.
static void flushRS485()
{
    while (rs485.available()) rs485.read();
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
        // No delay() here -- runs inside the BLE stack task.
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
// RX CALLBACK  (BLE -> RS485)
// =====================================================

class RXCallbacks : public BLECharacteristicCallbacks
{
    void onWrite(BLECharacteristic *pCharacteristic)
    {
        String value = pCharacteristic->getValue();

        if (value.length() > 0)
        {
            Serial.print("BLE RX: ");

            for (int i = 0; i < (int)value.length(); i++)
            {
                uint8_t c = (uint8_t)value[i];
                Serial.write(c);
                rs485.write(c);
            }

            Serial.println();
        }
    }
};

// =====================================================

void setup()
{
    Serial.begin(115200);

    rs485.begin(9600, SERIAL_8N1, 26, 27);

    // Discard any noise that accumulated during UART startup.
    delay(50);
    flushRS485();

    Serial.println();
    Serial.println("====================================");
    Serial.println(" ESP32 BLE RS485 HEX ");
    Serial.println("====================================");

    BLEDevice::init(BLE_DEVICE_NAME);
    BLEDevice::setMTU(512);

    BLEServer *pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());

    BLEService *pService = pServer->createService(SERVICE_UUID);

    // TX characteristic (RS485 -> BLE notify)
    txCharacteristic = pService->createCharacteristic(
        CHARACTERISTIC_UUID_TX,
        BLECharacteristic::PROPERTY_NOTIFY
    );
    txCharacteristic->addDescriptor(new BLE2902());

    // RX characteristic (BLE -> RS485 write)
    BLECharacteristic *rxCharacteristic = pService->createCharacteristic(
        CHARACTERISTIC_UUID_RX,
        BLECharacteristic::PROPERTY_WRITE
    );
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
    static uint8_t rawBuf[RS485_BUF_SIZE];
    static char    hexBuf[HEX_BUF_SIZE];
    int            byteCount = 0;

    // Collect a full RS485 frame: read until bus quiet for 5 ms.
    if (rs485.available())
    {
        unsigned long lastByte = millis();

        while (byteCount < RS485_BUF_SIZE)
        {
            if (rs485.available())
            {
                rawBuf[byteCount++] = rs485.read();
                lastByte = millis();
            }
            else if (millis() - lastByte >= 5)
            {
                // Bus idle for 5 ms -- end of frame.
                break;
            }
        }
    }

    // Validate frame: count non-zero bytes.
    int nonZero = 0;
    for (int i = 0; i < byteCount; i++)
    {
        if (rawBuf[i] != 0x00) nonZero++;
    }

    if (byteCount > 0 && nonZero < MIN_VALID_BYTES)
    {
        // Idle-bus noise or glitch -- flush FIFO so CPU isn't spun reading zeros.
        flushRS485();
        byteCount = 0;
    }

    // Build hex string and forward to BLE.
    if (byteCount > 0)
    {
        int pos = 0;

        for (int i = 0; i < byteCount; i++)
        {
            uint8_t b = rawBuf[i];

            hexBuf[pos++] = "0123456789abcdef"[b >> 4];
            hexBuf[pos++] = "0123456789abcdef"[b & 0x0F];
            hexBuf[pos++] = ' ';

            Serial.print(hexBuf[pos - 3]);
            Serial.print(hexBuf[pos - 2]);
            Serial.print(' ');
        }

        hexBuf[pos] = '\0';
        Serial.println();

        if (connectedCount > 0)
        {
            txCharacteristic->setValue((uint8_t*)hexBuf, pos);
            txCharacteristic->notify();
            delay(20);
        }
    }

    delay(5);
}
