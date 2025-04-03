package com.ims.bpcluat.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import com.ims.bpcluat.CRC16Modbus;
import com.ims.bpcluat.conversion.DecimalToHex;
import com.ims.bpcluat.conversion.StringToHexadecimal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BleService extends Service {
    private static final String TAG = "BleService";
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private List<BluetoothDevice> scannedDevices = new ArrayList<>();
    private ScanCallback scanCallback;
    private Handler handler;
    private byte[] savedVariableValue; // Variable to store the command

    private BluetoothGattCharacteristic writeCharacteristic;
    private BluetoothGattCharacteristic notifyCharacteristic;
    private NotificationCallback notificationCallback;

    public static final UUID MY_SERVICE_UUID = UUID.fromString("0000abf0-0000-1000-8000-00805f9b34fb");
    public static final UUID MY_CHARACTERISTIC_UUID = UUID.fromString("0000abf1-0000-1000-8000-00805f9b34fb");
    private static final UUID NOTIFY_CHAR_UUID = UUID.fromString("0000abf2-0000-1000-8000-00805f9b34fb");

    private final IBinder binder = new LocalBinder();
    private BluetoothGatt bluetoothGatt; // Make bluetoothGatt a class-level variable

    public interface NotificationCallback {
        void onNotificationReceived(String response);
    }

    public void setNotificationCallback(NotificationCallback callback) {
        this.notificationCallback = callback;
    }

    public class LocalBinder extends Binder {
        public BleService getService() {
            return BleService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        handler = new Handler();
    }

    public boolean initialize() {
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    public void startScan() {
        scannedDevices.clear();

        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(MY_SERVICE_UUID))
                .build();
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(filter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                try {
                    if (!scannedDevices.contains(result.getDevice())) {
                        scannedDevices.add(result.getDevice());
                        Log.i(TAG, "Device found: " + result.getDevice().getName() + " - " + result.getDevice().getAddress());
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException: " + e.getMessage());
                }

            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                try {
                    for (ScanResult result : results) {
                        if (!scannedDevices.contains(result.getDevice())) {
                            scannedDevices.add(result.getDevice());
                            Log.i(TAG, "Batch Device found: " + result.getDevice().getName() + " - " + result.getDevice().getAddress());
                        }
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException: " + e.getMessage());
                }

            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e(TAG, "Scan failed with error: " + errorCode);
            }
        };

        try {
            bluetoothLeScanner.startScan(filters, settings, scanCallback);
            handler.postDelayed(() -> bluetoothLeScanner.stopScan(scanCallback), 5000);  // Increase scan duration to 5 seconds
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: " + e.getMessage());
        }
    }

    public List<BluetoothDevice> getScannedDevices() {
        return scannedDevices;
    }

    public void connectToDevice(BluetoothDevice device) {
        new ConnectTask().execute(device);
    }

    private class ConnectTask extends AsyncTask<BluetoothDevice, Void, Void> {
        @Override
        protected Void doInBackground(BluetoothDevice... devices) {
            BluetoothDevice device = devices[0];
            try {
                if (device == null) {
                    Log.w(TAG, "BluetoothDevice not specified.");
                    return null;
                }
                if (bluetoothGatt != null) {
                    bluetoothGatt.close();
                }
                Log.d(TAG, "Connecting to device: " + device.getAddress());
                bluetoothGatt = device.connectGatt(BleService.this, false, gattCallback);
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException: " + e.getMessage());
            }
            return null;
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            try {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Connected to GATT server.");
                    // Request larger MTU size after connection
                    gatt.requestMtu(512);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Disconnected from GATT server.");
                    bluetoothGatt.close();
                    bluetoothGatt = null;
                }
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException: " + e.getMessage());
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            try {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "MTU size changed to: " + mtu);
                    bluetoothGatt.discoverServices();
                } else {
                    Log.e(TAG, "Failed to change MTU size, status: " + status);
                    bluetoothGatt.discoverServices();
                }
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException: " + e.getMessage());
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            try {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Services discovered");
                    List<BluetoothGattService> services = gatt.getServices();
                    for (BluetoothGattService service : services) {
                        Log.d(TAG, "Service UUID: " + service.getUuid());
                        if (service.getUuid().equals(MY_SERVICE_UUID)) {
                            writeCharacteristic = service.getCharacteristic(MY_CHARACTERISTIC_UUID);
                            notifyCharacteristic = service.getCharacteristic(NOTIFY_CHAR_UUID);
                            if (notifyCharacteristic != null) {
                                boolean notificationSet = gatt.setCharacteristicNotification(notifyCharacteristic, true);
                                Log.d(TAG, "Notification set: " + notificationSet);
                                BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                                if (descriptor != null) {
                                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                    boolean writeResult = gatt.writeDescriptor(descriptor);
                                    Log.d(TAG, "Descriptor write result: " + writeResult);
                                } else {
                                    Log.e(TAG, "Descriptor not found for characteristic: " + NOTIFY_CHAR_UUID);
                                }

                                // Send command after setting up notification
                                handler.postDelayed(() -> sendCommand(savedVariableValue), 100); // Delay to ensure descriptor is written
                            } else {
                                Log.e(TAG, "Notify characteristic not found: " + NOTIFY_CHAR_UUID);
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "onServicesDiscovered received: " + status);
                }
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException: " + e.getMessage());
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite: " + characteristic.getUuid() + ", status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic write successful");
            } else {
                Log.e(TAG, "Characteristic write failed, status: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(MY_CHARACTERISTIC_UUID)) {
                byte[] data = characteristic.getValue();
                String response = bytesToHex(data);
                Log.d("bleResponse", response);
                // Handle the data
            } else if (characteristic.getUuid().equals(NOTIFY_CHAR_UUID)) {
                byte[] data = characteristic.getValue();
                String response = bytesToHex(data);
                Log.d("notifyResponse", response);
                // Handle the notify characteristic data
                handleNotifyResponse(data);
            } else {
                Log.d(TAG, "Unknown characteristic changed: " + characteristic.getUuid());
            }
        }
    };

    private void handleNotifyResponse(byte[] data) {
        // Process the notification response
        String response = bytesToHex(data);
        Log.d("ProcessedNotifyResponse", response);
        if (notificationCallback != null) {
            notificationCallback.onNotificationReceived(response);
        }
        // Implement further processing as needed
    }

    @Override
    public void onDestroy() {
        try {
            super.onDestroy();
            if (bluetoothGatt != null) {
                bluetoothGatt.close();
                bluetoothGatt = null;
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: " + e.getMessage());
        }
    }

    public String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String createRequestCommand() {
        String request = "00032601";
        String pumpNo = "01"; // Replace with actual pump number if needed
        if (pumpNo.length() == 1) {
            pumpNo = "0" + pumpNo;
        }
        request = request + pumpNo;
        byte[] data = CRC16Modbus.hexStringToByteArray(request);
        int crc = CRC16Modbus.calculateCRC(data);
        String crcHex = String.format("%04X", crc);
        return request + crcHex;
    }

    public void sendCommand(byte[] commandStr) {
        savedVariableValue = commandStr; // Save the command to use it later
        try {
            if (writeCharacteristic != null && bluetoothGatt != null) {
                Log.d(TAG, "Sending command: " + commandStr);
                writeCharacteristic.setValue(commandStr);
                boolean result = bluetoothGatt.writeCharacteristic(writeCharacteristic);
                Log.d(TAG, "Write result: " + result);
                if (!result) {
                    Log.e(TAG, "Write failed, retrying...");
                    handler.postDelayed(() -> bluetoothGatt.writeCharacteristic(writeCharacteristic), 1000); // Retry after 1 second
                }
            } else {
                Log.w(TAG, "writeCharacteristic or bluetoothGatt is null");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: " + e.getMessage());
        }
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
        if(pumpHexString.length() ==1){
            pumpHexString = "0"+pumpHexString;
        }
        request = request + pumpHexString;
        byte[] data = CRC16Modbus.hexStringToByteArray(request);
        int crc = CRC16Modbus.calculateCRC(data);
        String crcHex = String.format("%04X", crc);
        return request + crcHex;
    }

    public static String createRequestFor27h(String request) {
        byte[] data = CRC16Modbus.hexStringToByteArray(request);
        int crc = CRC16Modbus.calculateCRC(data);
        String crcHex = String.format("%04X", crc);
        Log.d("crcHex", crcHex);
        return request + crcHex;
    }

    public static String addCrcInTwentyEightRequest(String request) {
        byte[] data = CRC16Modbus.hexStringToByteArray(request);
        int crc = CRC16Modbus.calculateCRC(data);
        String crcHex = String.format("%04X", crc);
        Log.d("crcHex", crcHex);
        return request + crcHex;
    }
}
