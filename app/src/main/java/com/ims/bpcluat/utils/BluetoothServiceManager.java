package com.ims.bpcluat.utils;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.Log;

public class BluetoothServiceManager {

    public void restartBluetoothService(Context context) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            try {
                if (bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.disable();
                    // Wait for Bluetooth to be disabled
                    new Thread(() -> {
                        try {
                            while (bluetoothAdapter.isEnabled()) {
                                Thread.sleep(100);
                            }
                            bluetoothAdapter.enable();
                        } catch (InterruptedException e) {
                            Log.e("MyService", "Error waiting for Bluetooth to disable", e);
                        }
                    }).start();
                } else {
                    bluetoothAdapter.enable();
                }
            } catch (SecurityException e) {
                Log.e("MyService", "Permission issue encountered", e);
            }
        } else {
            Log.e("MyService", "Bluetooth is not supported on this device");
        }
    }
}

