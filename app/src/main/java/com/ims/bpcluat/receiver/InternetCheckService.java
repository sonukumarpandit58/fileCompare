package com.ims.bpcluat.receiver;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

public class InternetCheckService extends Service {

    private ConnectivityReceiver connectivityReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        connectivityReceiver = new ConnectivityReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(connectivityReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(connectivityReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
