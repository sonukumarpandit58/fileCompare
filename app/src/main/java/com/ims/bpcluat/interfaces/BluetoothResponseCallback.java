package com.ims.bpcluat.interfaces;

public interface BluetoothResponseCallback {
    void onBleResponseReceived(String response);
    void onWriteCharacteristicReady();
}