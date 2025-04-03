package com.ims.bpcluat.helper;

import static com.ims.bpcluat.Helper.authCodeForUfillTxn;
import static com.ims.bpcluat.Helper.bleBroadcastingName;
import static com.ims.bpcluat.Helper.errorCodeForUfillTxn;
import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.Helper.twentySixRequest;
import static com.ims.bpcluat.Helper.txnArrayList;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.ims.bpcluat.CRC16Modbus;
import com.ims.bpcluat.Helper;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.TransactionParser;
import com.ims.bpcluat.TwentyNineTxnParser;
import com.ims.bpcluat.conversion.DecimalToHex;
import com.ims.bpcluat.conversion.HexToDecimal;
import com.ims.bpcluat.fragment.OnlineSingleTransactionFragment;
import com.ims.bpcluat.service.BleService;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BleDeviceHelper {
    private static final String TAG = "BleDeviceHelper";
    private static BleDeviceHelper instance;
    private Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private BluetoothAdapter bluetoothAdapter;
    public BluetoothGatt bluetoothGatt;
    private BluetoothLeScanner bluetoothLeScanner;
    public BluetoothGattCharacteristic writeCharacteristic;
    private BluetoothGattCharacteristic notifyCharacteristic;

    public static final UUID MY_SERVICE_UUID = UUID.fromString("0000abf0-0000-1000-8000-00805f9b34fb");
    public static final UUID MY_CHARACTERISTIC_UUID = UUID.fromString("0000abf1-0000-1000-8000-00805f9b34fb");
    private static final UUID NOTIFY_CHAR_UUID = UUID.fromString("0000abf2-0000-1000-8000-00805f9b34fb");

    public static List<ScanResult> foundDevices = new ArrayList<>();
    private boolean isScanStopped = false;
    private boolean isConnected = false;

    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 2;
    public static boolean retryOneMoreTime = false;

    // Timeout Variables
    private Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;
    private boolean responseReceived = false;
    private boolean isReadyToReceive = false;
    private boolean isReceiverRegistered = false;
    private boolean hasBluetoothOffHandled = false;

    public static String ufillTwentyEightAuthCode = "";
    public static boolean ufillTwentyNineResponseSend = false;


    // Date Variables
    Date date = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
    String todayDate = formatter.format(date);

    public static String commandProtocol = "";
    String netAmountCheck;

    private BleCallback bleCallback;

    public interface BleCallback {
        void onBleConnected();               // When device is connected
        void onBleResponseReceived(String response); // When a response is received
        void onBleConnectionFailed(); // when connection is not successful
        void onBluetoothTurnedOff(); //  When Bluetooth is turned OFF
    }

    public void setBleCallback(BleCallback callback) {
        this.bleCallback = callback;
    }

    // Singleton instance
    public static BleDeviceHelper getInstance(Context context) {
        Log.d(TAG, "getInstance method Called");
        if (instance == null) {
            instance = new BleDeviceHelper(context.getApplicationContext());
        }
        return instance;
    }

    public BleDeviceHelper(Context context) {
        Log.d(TAG, "BleDeviceHelper method Called");
        this.context = context;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        // Register Bluetooth state change receiver
        if (!isReceiverRegistered) {
            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            context.registerReceiver(bluetoothStateReceiver, filter);
            isReceiverRegistered = true;
        }

    }

    public void initiateBleScan() {
        try {
            Log.d(TAG, "initiateBleScan method Called");
            if (isConnected) {
                Log.d(TAG, "Already connected. Skipping scan.");
                if (bleCallback != null) {
                    bleCallback.onBleConnected(); // Notify fragment directly
                }
                return;
            }

            foundDevices.clear(); // <-- Move here
            Log.d(TAG, "initiateBleScan Inside");
            if (bluetoothAdapter.isEnabled()) {
                isScanStopped = false; // reset flag
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                if (bluetoothLeScanner != null) {
                    List<ScanFilter> filter = new ArrayList<>();
                    ScanSettings settings = new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Ensures real-time scanning
                            .build();
                    bluetoothLeScanner.startScan(filter, settings, scanCallback);

                    handler.postDelayed(() -> {
                        if (!isScanStopped) {
                            Log.d(TAG, "Auto stop scan after 2 seconds");
                            stopScan();
                            connectToSpecificDevice();
                        }
                    }, 2000); // 2000ms = 2 seconds
                }
            }
        } catch (SecurityException e) {
            Log.d("SecurityException", e.toString());
        }
    }

    public final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //super.onScanResult(callbackType, result);
            Log.d(TAG, "onScanResult method Called");
            try {
                BluetoothDevice device = result.getDevice();
                if (device != null && result.getScanRecord() != null) {
                    List<ParcelUuid> serviceUuids = result.getScanRecord().getServiceUuids();
                    // Make sure the serviceUuids list isn't null
                    if (serviceUuids != null) {
                        for (ParcelUuid parcelUuid : serviceUuids) {
                            if (parcelUuid.getUuid().equals(MY_SERVICE_UUID)) {
//                                if (!foundDevices.contains(result)) {
//                                    foundDevices.add(result);
//                                    Log.d("ScanResult", "Added device: " + result.getDevice().getName());
//                                }
//                                break;

                                boolean alreadyAdded = false;
                                for (ScanResult existingResult : foundDevices) {
                                    if (existingResult.getDevice().getAddress().equals(device.getAddress())) {
                                        alreadyAdded = true;
                                        break;
                                    }
                                }

                                if (!alreadyAdded) {
                                    foundDevices.add(result);
                                    //Log.d("ScanResult", "Added unique device: " + device.getName() + " (" + device.getAddress() + ")");
                                }
                                break;
                            }
                        }
                    }
                }
            } catch (SecurityException e) {
                Log.d("SecurityException", e.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.d(TAG, "onScanFailed method Called");
            // super.onScanFailed(errorCode);
        }
    };

    public void stopScan() {
        try {
            Log.d(TAG, "stopScan method Called");
            if (!isScanStopped) {
                isScanStopped = true;
                bluetoothLeScanner.stopScan(scanCallback);
                Log.d(TAG, "Scan stopped.");
            }
        } catch (SecurityException e) {
            Log.d("SecurityException", e.toString());
        }
    }

    public void connectToSpecificDevice() {
        try {
            Log.d("TotalFoundDevice", String.valueOf(foundDevices.size()));
            int deviceCount = foundDevices.size();
            for (int i = 0; i < deviceCount; i++) {
                ScanResult scanResult = foundDevices.get(i);
                String deviceName = scanResult.getScanRecord().getDeviceName();
                BluetoothDevice device = scanResult.getDevice();

                if (deviceName == null) continue;
                String substring = deviceName.substring(0, 9);
                //String bcd2Str = Helper.bcd2Str(deviceName.substring(deviceName.length() - 4).replaceAll(" ", "").getBytes());
                String bcd2Str = Helper.pumpNameExtractFromDeviceName(deviceName);

                Log.d("deviceName", deviceName);
                Log.d("deviceNameLength", String.valueOf(deviceName.length()));
                Log.d("substring", substring);
                Log.d("bcd2Str", bcd2Str);
                Log.d("sapCode", Helper.sapCode);
                Log.d("SelectedPumpNo", Helper.txnReleatedPumpNo);

                fileWrite(context, todayDate + ".txt", "device name: ", deviceName);
                fileWrite(context, todayDate + ".txt", "device substring: ", substring);
                fileWrite(context, todayDate + ".txt", "sapcode: ", Helper.sapCode);
                fileWrite(context, todayDate + ".txt", "pump No in Decimal: ", Helper.txnReleatedPumpNo);
                fileWrite(context, todayDate + ".txt", "bcd2Str: ", bcd2Str);
                fileWrite(context, todayDate + ".txt", "pump No in Hex: ", DecimalToHex.create(Integer.parseInt(Helper.txnReleatedPumpNo)).toString());

                String blePump = Helper.txnReleatedPumpNo;
                blePump = blePump.replaceFirst("^0+", "");
                blePump = blePump.toString();
                Log.d("blePumpDecimal", blePump);
                Log.d("blePumpHex", DecimalToHex.create(Integer.parseInt(blePump)));

                if (substring.matches("REL" + Helper.sapCode)) {
                    Helper.bleBroadcastingName = "REL";
                    Log.d("bleBroadcastingName", "REL Connected");
                    Log.d("sapcodeMatch", "sapcodeMatch");
                    fileWrite(context, todayDate + ".txt", "check sapcode: ", "sapcode match");
                    if (bcd2Str.contains(DecimalToHex.create(Integer.parseInt(blePump)))) {
                        Helper.bleStatus = "connected";
                        fileWrite(context, todayDate + ".txt", "check pump: ", "pump no check");
                        Log.d("deviceName4", "pump " + Helper.txnReleatedPumpNo + " connected");
                        bluetoothGatt = device.connectGatt(context, false, gattCallback); // Use the stored context here
                        return; // Connect to the first matching device and return
                    }
                }

                if (substring.matches("IOT" + Helper.sapCode)) {
                    Helper.bleBroadcastingName = "IOT";
                    Log.d("bleBroadcastingName", "IOT Connected");
                    fileWrite(context, todayDate + ".txt", "check sapcode: ", "sapcode match");
                    if (bcd2Str.contains(DecimalToHex.create(Integer.parseInt(blePump)))) {
                        Helper.bleStatus = "connected";
                        fileWrite(context, todayDate + ".txt", "check pump: ", "pump no check");
                        Log.d("deviceName4", "pump " + Helper.txnReleatedPumpNo + " connected");
                        bluetoothGatt = device.connectGatt(context, false, gattCallback); // Use the stored context here
                        return; // Connect to the first matching device and return
                    }
                }
            }
            // Device not matched or not connected
            if (bleCallback != null) {
                bleCallback.onBleConnectionFailed(); // Notify fragment of failure
            }
        } catch (SecurityException e) {
            Log.d("SecurityException", e.toString());
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            try {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    Log.d(TAG, "onConnectionStateChange method Called : STATE_CONNECTED");
                    fileWrite(context, todayDate + ".txt", "onConnectionStateChange method Called", "STATE_CONNECTED");
                    isConnected = true;
                    gatt.discoverServices(); // Discover services once connected
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.d(TAG, "onConnectionStateChange method Called : STATE_DISCONNECTED");
                    fileWrite(context, todayDate + ".txt", "onConnectionStateChange method Called", "STATE_DISCONNECTED");
                    isConnected = false;
                    if (gatt != null) {
                        gatt.close();
                    }
                    bluetoothGatt = null;

                    if (retryOneMoreTime) {
                        Log.d(TAG, "Reconnecting BLE - starting scan again");
                        initiateBleScan(); // your method to scan again
                    } else {
                        Log.d(TAG, "No retry requested");
                        if (commandProtocol.equals("26h") && bleCallback != null) {
                            bleCallback.onBleConnectionFailed();
                        }
                    }
                }
            } catch (SecurityException e) {
                Log.d("SecurityException gattCallback", e.toString());
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            try {
                Log.d(TAG, "onServicesDiscovered method Called");
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Services discovered");
                    List<BluetoothGattService> services = gatt.getServices();
                    for (BluetoothGattService service : services) {
                        Log.d(TAG, "Service UUID: " + service.getUuid());
                        if (service.getUuid().equals(MY_SERVICE_UUID)) {
                            writeCharacteristic = service.getCharacteristic(MY_CHARACTERISTIC_UUID);
                            notifyCharacteristic = service.getCharacteristic(NOTIFY_CHAR_UUID);
                            if (notifyCharacteristic != null) {
                                Log.d(TAG, "Notify characteristic found");
                                bluetoothGatt.requestMtu(500); // Request MTU size here
                            } else {
                                Log.e(TAG, "Notify characteristic not found: " + NOTIFY_CHAR_UUID);
                            }
                        }
                    }
                }
            } catch (SecurityException e) {
                Log.d("SecurityException onServicesDiscovered", e.toString());
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            try {
                super.onMtuChanged(gatt, mtu, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "onMtuChanged method Called");
                    // MTU changed successfully, proceed with notifications
                    //enableNotifications(gatt);

                    if (notifyCharacteristic != null) {
                        boolean notificationSet = gatt.setCharacteristicNotification(notifyCharacteristic, true);
                        Log.d(TAG, "Notification set: " + notificationSet);
                        BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                        if (descriptor != null) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            boolean writeResult = gatt.writeDescriptor(descriptor);
                            Log.d(TAG, "Descriptor write result: " + writeResult);
                            if (!writeResult) {
                                Log.e(TAG, "Descriptor write failed (writeResult=false), closing connection.");

                                if (retryCount < MAX_RETRY_COUNT) {
                                    retryCount++;
                                    Log.d(TAG, "Retrying BleDeviceHelper. Attempt: " + retryCount);
                                    retryOneMoreTime = true;
                                    disconnect();
                                } else {
                                    retryCount = 0; // Reset after max attempts
                                    retryOneMoreTime = false;
                                    if (bleCallback != null) {
                                        bleCallback.onBleResponseReceived("NoTxnFound");
                                    }
                                }


//                                if (bleCallback != null) {
//                                    new Handler(Looper.getMainLooper()).post(() -> bleCallback.onBleConnectionFailed());
//                                }
                                //     disconnectAndClose(); // Or just disconnect();
                            }
                        } else {
                            Log.e(TAG, "Descriptor not found for characteristic: " + NOTIFY_CHAR_UUID);
                            if (bleCallback != null) {
                                new Handler(Looper.getMainLooper()).post(() -> bleCallback.onBleConnectionFailed());
                            }
                            //   disconnectAndClose();
                        }
                    } else {
                        Log.e(TAG, "Notify characteristic is null");
                    }
                }
            } catch (SecurityException e) {
                Log.d("SecurityException onMtuChanged", e.toString());
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite method Called");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged method Called");
            if (!isReadyToReceive) return; // Ignore early data
            responseReceived = true; // BLE responded
            timeoutHandler.removeCallbacks(timeoutRunnable); // Cancel timeout

            byte[] response = characteristic.getValue();
            String hexResponse = bytesToHex(response);
            Log.d("BleDeviceHelper", "Response: " + hexResponse);

            if (commandProtocol.equals("26h")) {
                Log.d("BleDeviceHelper", "twentySixResponse: " + hexResponse);
                fileWrite(context, todayDate + ".txt", "twentySixResponse : ", hexResponse);
                processTwentySixResponse(hexResponse);
            }
            if (commandProtocol.equals("27h")) {
                Log.d("BleDeviceHelper", "twentySevenResponse: " + hexResponse);
                fileWrite(context, todayDate + ".txt", "twentySevenResponse : ", hexResponse);
                if (bleCallback != null) {
                    bleCallback.onBleResponseReceived(hexResponse);
                }
            }
            if (commandProtocol.equals("28h")) {
                Log.d("BleDeviceHelper", "twentyEightResponse: " + hexResponse);
                fileWrite(context, todayDate + ".txt", "twentyEightResponse : ", hexResponse);
                if (bleCallback != null) {
                    String errorCode = errorCodeForUfillTxn(hexResponse);
                    ufillTwentyEightAuthCode = authCodeForUfillTxn(hexResponse);
                    fileWrite(context, todayDate + ".txt", "28hResponse errorCode:", errorCode);
                    fileWrite(context, todayDate + ".txt", "28hResponse authCode:", ufillTwentyEightAuthCode);
                    bleCallback.onBleResponseReceived(hexResponse);
                }
            }

            if (commandProtocol.equals("29h")) {
                Log.d("BleDeviceHelper", "twentyNineResponse: " + hexResponse);
                fileWrite(context, todayDate + ".txt", "twentyNineResponse : ", hexResponse);
                if (bleCallback != null) {
                    String authID = TwentyNineTxnParser.AuthId(hexResponse);
                    Log.d("twentyEightAuthCode", ufillTwentyEightAuthCode);
                    Log.d("twentyNineResAuthId", authID);
                    //ufillTwentyEightAuthCode = authID;  // HardCode for txn Success
                    fileWrite(context, todayDate + ".txt", "ufillTwentyEightAuthCode:", ufillTwentyEightAuthCode);
                    fileWrite(context, todayDate + ".txt", "twentyNineResAuthId:", authID);
                    if (!ufillTwentyNineResponseSend) {
                        if (ufillTwentyEightAuthCode.equals(authID)) {
                            Log.d("inside", "471 send data to Preset Page");
                            ufillTwentyNineResponseSend = true;
                            bleCallback.onBleResponseReceived(hexResponse);
                        }
                    }
                }
            }


            // Notify fragment with the response
//            if (bleCallback != null) {
//                bleCallback.onBleResponseReceived(hexResponse);
//            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorWrite method Called");
            try {
                isReadyToReceive = true;
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (bleCallback != null) {
                        bleCallback.onBleConnected(); // Fragment will write command from here
                    }
                } else {
                    Log.e(TAG, "Descriptor write failed with status: " + status);
                    if (bleCallback != null) {
                        bleCallback.onBleConnectionFailed();
                    }
                    //   disconnectAndClose();  // important: clean up if descriptor failed
                }
            } catch (SecurityException e) {
                Log.d("SecurityException onDescriptorWrite", e.toString());
            }
        }
    };

    public void disconnect() {
        try {
            Log.d(TAG, "disconnect() called");
            timeoutHandler.removeCallbacks(timeoutRunnable); // cancel if running
            if (bluetoothGatt != null) {
                try {
                    isConnected = false;
                    bluetoothGatt.disconnect();
//                    foundDevices.clear();
                    Log.d(TAG, "BLE disconnected");
                } catch (Exception e) {
                    Log.e(TAG, "Error during disconnect and close: " + e.toString());
                }
            } else {
                Log.d(TAG, "No active GATT connection to disconnect.");
                if (commandProtocol.equals("28h") && retryOneMoreTime) {
                    initiateBleScan(); // your method to scan again
                }
            }
        } catch (SecurityException e) {
            Log.d("SecurityException disconnect", e.toString());
        }
    }

    public String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String createRequestForFetchTxn(String pumpNo) {
        String request = "00032601";
        String pumpHexString = DecimalToHex.create(Integer.parseInt(pumpNo));
        if (pumpHexString.length() == 1) {
            pumpHexString = "0" + pumpHexString;
        }
        request = request + pumpHexString;
        byte[] data = CRC16Modbus.hexStringToByteArray(request);
        int crc = CRC16Modbus.calculateCRC(data);
        String crcHex = String.format("%04X", crc);
        return request + crcHex;
    }

    public static String createRequestForTwentyNine(String pumpNo) {
        String request = "00032901";
        String pumpHexString = DecimalToHex.create(Integer.parseInt(pumpNo));
        if (pumpHexString.length() == 1) {
            pumpHexString = "0" + pumpHexString;
        }
        request = request + pumpHexString;
        byte[] data = CRC16Modbus.hexStringToByteArray(request);
        int crc = CRC16Modbus.calculateCRC(data);
        String crcHex = String.format("%04X", crc);
        return request + crcHex;
    }


    public void processTwentySixResponse(String response) {
        try {
            if (response.isEmpty()) {
                Log.d("txnList", "no Transaction Found 1");
                if (retryCount < MAX_RETRY_COUNT) {
                    retryCount++;
                    Log.d(TAG, "Retrying BleDeviceHelper. Attempt: " + retryCount);
                    retryOneMoreTime = true;
                    disconnect();
                } else {
                    retryCount = 0; // Reset after max attempts
                    retryOneMoreTime = false;
                    if (bleCallback != null) {
                        bleCallback.onBleResponseReceived("NoTxnFound");
                    }
                }
            } else {
                String finalResponse = "";
                if (response.startsWith("80")) {
                    finalResponse = response.substring(2);
                } else {
                    finalResponse = response;
                }
                String errorCode = finalResponse.substring(6, 8);
                if (errorCode.equals("01")) {
                    String positionValue = finalResponse.substring(12, 14);
                    if ("00".equals(positionValue)) {
                        Log.d("txnList", "no Transaction Found 2");
                        if (retryCount < MAX_RETRY_COUNT) {
                            retryCount++;
                            Log.d(TAG, "Retrying BleDeviceHelper. Attempt: " + retryCount);
                            retryOneMoreTime = true;
                            disconnect();
                        } else {
                            retryCount = 0; // Reset after max attempts
                            retryOneMoreTime = false;
                            if (bleCallback != null) {
                                bleCallback.onBleResponseReceived("NoTxnFound");
                            }
                        }
                    } else {
                        String lastFourChars = finalResponse.substring(finalResponse.length() - 4);
                        String modifiedRes = response.substring(0, response.length() - 4);
                        Log.d("lastFourChars", lastFourChars);
                        Log.d("modifiedRes", modifiedRes);

                        byte[] data = CRC16Modbus.hexStringToByteArray(modifiedRes);
                        int crc = CRC16Modbus.calculateCRC(data);
                        String crcHex = String.format("%04X", crc);
                        Log.d("responsecrcHex", crcHex);

                        try {
                            if (crcHex.toLowerCase().equals(lastFourChars.toLowerCase())) {
                                fileWrite(context, todayDate + ".txt", "twentySixResponse crc :", "matched");
                                Map<String, Object> result = TransactionParser.txnParser(finalResponse);
                                txnArrayList = new ArrayList<>();
                                txnArrayList = (ArrayList) result.get("Transactions");

                                // Filter Transaction
                                ArrayList filtertxnList = new ArrayList<>();
                                if (bleBroadcastingName.equals("REL")) {
                                    // IF BLE Device name is REL
                                    try {
                                        for (int i = 0; i < txnArrayList.size(); i++) {
                                            Object object = txnArrayList.get(i);
                                            Gson gson = new Gson();
                                            String json = gson.toJson(object);
                                            JSONObject jsonObject = new JSONObject(json);
                                            if (jsonObject.has("EnableOptionByte")) {
                                                if (jsonObject.getString("EnableOptionByte").equals("00")) {
                                                    continue;
                                                }
                                            }
                                            if (jsonObject.has("PaymentMode")) {
                                                if (!jsonObject.getString("PaymentMode").equals("00")) {
                                                    continue;
                                                }
                                            } else {
                                                continue;
                                            }

                                            if (jsonObject.has("NetAmount")) {
                                                netAmountCheck = jsonObject.getString("NetAmount");
                                            }

                                            if (!TextUtils.isEmpty(netAmountCheck)) {
                                                netAmountCheck = HexToDecimal.convertAmount(netAmountCheck);
                                                if (!Helper.isZero(netAmountCheck)) {
                                                    filtertxnList.add(object);
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.d("Exception", e.toString());
                                    }
                                } else {
                                    // IF BLE Device name is IOT
                                    try {
                                        for (int i = 0; i < txnArrayList.size(); i++) {
                                            Object object = txnArrayList.get(i);
                                            Gson gson = new Gson();
                                            String json = gson.toJson(object);
                                            JSONObject jsonObject = new JSONObject(json);
                                            if (jsonObject.has("IsTransactionPrinted")) {
                                                // If "IsTransactionPrinted" is "01", skip this iteration
                                                if (jsonObject.getString("IsTransactionPrinted").equals("01")) {
                                                    continue;
                                                }
                                            }

                                            if (jsonObject.has("NetAmount")) {
                                                netAmountCheck = jsonObject.getString("NetAmount");
                                            }

                                            if (!TextUtils.isEmpty(netAmountCheck)) {
                                                netAmountCheck = HexToDecimal.convertAmount(netAmountCheck);
                                                if (!Helper.isZero(netAmountCheck)) {
                                                    filtertxnList.add(object);
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.d("Exception", e.toString());
                                    }
                                }

                                txnArrayList = filtertxnList;
                                Log.d("txnArrayList", String.valueOf(txnArrayList));
                                Log.d("txnArrayList Size", String.valueOf(txnArrayList.size()));
                                fileWrite(context, todayDate + ".txt", "Total No of Txn Fetch = ", String.valueOf(txnArrayList.size()));
                                if (txnArrayList.size() == 0) {
                                    Log.d("txnList", "no Transaction Found 3");
                                    if (retryCount < MAX_RETRY_COUNT) {
                                        retryCount++;
                                        Log.d(TAG, "Retrying BleDeviceHelper. Attempt: " + retryCount);
                                        retryOneMoreTime = true;
                                        disconnect();
                                    } else {
                                        retryCount = 0; // Reset after max attempts
                                        retryOneMoreTime = false;
                                        if (bleCallback != null) {
                                            bleCallback.onBleResponseReceived("NoTxnFound");
                                        }
                                    }
                                } else {
                                    Log.d("twentySixResponse", "PerfectResponse we can proceed");
                                    retryCount = 0; // Reset after max attempts
                                    retryOneMoreTime = false;
                                    if (bleCallback != null) {
                                        bleCallback.onBleResponseReceived(response);
                                    }
                                }
                            } else {
                                fileWrite(context, todayDate + ".txt", "twentySixResponse crc", "not matched");
                                Log.d("txnList", "no Transaction Found 4");
                                if (retryCount < MAX_RETRY_COUNT) {
                                    retryCount++;
                                    Log.d(TAG, "Retrying BleDeviceHelper. Attempt: " + retryCount);
                                    retryOneMoreTime = true;
                                    disconnect();
                                } else {
                                    retryCount = 0; // Reset after max attempts
                                    retryOneMoreTime = false;
                                    if (bleCallback != null) {
                                        bleCallback.onBleResponseReceived("NoTxnFound");
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.d("589Excepion", e.toString());
                            Log.d("txnList", "no Transaction Found 5");
                            if (retryCount < MAX_RETRY_COUNT) {
                                retryCount++;
                                Log.d(TAG, "Retrying BleDeviceHelper. Attempt: " + retryCount);
                                retryOneMoreTime = true;
                                disconnect();
                            } else {
                                retryCount = 0; // Reset after max attempts
                                retryOneMoreTime = false;
                                if (bleCallback != null) {
                                    bleCallback.onBleResponseReceived("NoTxnFound");
                                }
                            }
                        }
                    }
                } else {
                    Log.d("txnList", "no Transaction Found 6");
                    if (retryCount < MAX_RETRY_COUNT) {
                        retryCount++;
                        Log.d(TAG, "Retrying BleDeviceHelper. Attempt: " + retryCount);
                        retryOneMoreTime = true;
                        disconnect();
                    } else {
                        retryCount = 0; // Reset after max attempts
                        retryOneMoreTime = false;
                        if (bleCallback != null) {
                            bleCallback.onBleResponseReceived("NoTxnFound");
                        }
                    }
                }
            }
        } catch (Exception e) {
            fileWrite(context, todayDate + ".txt", "PumpFragment onDataReceived Exception", e.toString());
            Log.d("PumpFragment onDataReceived", e.toString());
            Log.d("txnList", "no Transaction Found 7");
            if (retryCount < MAX_RETRY_COUNT) {
                retryCount++;
                Log.d(TAG, "Retrying BleDeviceHelper. Attempt: " + retryCount);
                retryOneMoreTime = true;
                disconnect();
            } else {
                retryCount = 0; // Reset after max attempts
                retryOneMoreTime = false;
                if (bleCallback != null) {
                    bleCallback.onBleResponseReceived("NoTxnFound");
                }
            }
        }
    }

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                final String action = intent.getAction();
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    if ((state == BluetoothAdapter.STATE_TURNING_OFF || state == BluetoothAdapter.STATE_OFF) && !hasBluetoothOffHandled) {
                        hasBluetoothOffHandled = true; // Mark handled
                        Log.e(TAG, "Bluetooth was turned off by user");
                        fileWrite(context, todayDate + ".txt", "bluetoothStateReceiver", "Bluetooth turned off");
                        handleBluetoothTurnedOff();
                        if (bleCallback != null) {
                            new Handler(Looper.getMainLooper()).post(() -> bleCallback.onBluetoothTurnedOff());
                        }
                    } else if (state == BluetoothAdapter.STATE_ON) {
                        // Reset flag if Bluetooth is turned back on
                        hasBluetoothOffHandled = false;
                        Log.e(TAG, "Bluetooth was turned on");
                    }
                }
            } catch (Exception e) {
                Log.d("bluetoothStateReceiver Exception", e.toString());
            }
        }
    };


    private void handleBluetoothTurnedOff() {
        try {
            if (bluetoothGatt != null) {
                try {
                    bluetoothGatt.disconnect();
                    bluetoothGatt.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error during cleanup on Bluetooth OFF: " + e.toString());
                }
            }
            bluetoothGatt = null;
            isConnected = false;
        } catch (SecurityException e) {
            Log.d("handleBluetoothTurnedOff SecurityException", e.toString());
        }
    }

    public static void clearUfillVariables() {
        ufillTwentyNineResponseSend = false;
        ufillTwentyEightAuthCode = "";
    }


}
