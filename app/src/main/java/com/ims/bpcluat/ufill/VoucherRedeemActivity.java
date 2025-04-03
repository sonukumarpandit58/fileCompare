package com.ims.bpcluat.ufill;

import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.todayDate;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ims.bpcluat.databinding.ActivityVoucherRedeemBinding;
import com.ims.bpcluat.dialog.BluetoothConnectionDialog;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.BleDeviceHelper;
import com.ims.bpcluat.model.UfillModel;

public class VoucherRedeemActivity extends AppCompatActivity {
    ActivityVoucherRedeemBinding binding;
    Context context;
    private static final int VOUCHER_REEDEM_CODE = 201;
    private UfillModel ufillModel;
    BluetoothAdapter bluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private BleDeviceHelper bleDeviceHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVoucherRedeemBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        context = this;

//        Helper.bleStatus = "";
//        if(BLEHelper.foundDevices != null && !BLEHelper.foundDevices.isEmpty()){
//            BLEHelper.foundDevices.clear();
//        }

        fileWrite(context, todayDate + ".txt", "Landing Page : ", "VoucherReedemAct");

        ufillModel = getIntent().getParcelableExtra("ufillModel");
        if (ufillModel != null) {
            binding.voucheramount.setText("Voucher Amount: " + ufillModel.getVoucherAmt());
            binding.bayTxnBtn.setText("Bay No: " + ufillModel.getPumpNo());
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            MessagesDialog.showDialog(VoucherRedeemActivity.this, "Device does not support Bluetooth.", 0, null, null);
        } else {
            if (bluetoothAdapter.isEnabled()) {
                // Bluetooth is enabled
                checkBluetoothPermission();
            } else {
                // Bluetooth is disabled
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        binding.authorizeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (bluetoothAdapter.isEnabled()) {
                        Log.d("BluetoothTag", "Already bluetooth ON");
                        redirectToPresetPage();
                    } else {
                        Log.d("BluetoothTag", "Bluetooth Off & Request to On");
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }
                } catch (SecurityException e) {
                    Log.d("BluetoothException", e.toString());
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOUCHER_REEDEM_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                String ufillTxnStatus = data.getStringExtra("ufillTxnStatus");
                Log.d("ufillTxnStatus", ufillTxnStatus);
                if (ufillTxnStatus.equals("failed")) {
                    BluetoothConnectionDialog.showDialog(this);
                } else {
                    MessagesDialog.showDialog(VoucherRedeemActivity.this, ufillTxnStatus, 0, null, null);
                }
            }
        }

        if (requestCode == REQUEST_ENABLE_BT) {  // 1 is the request code you passed
            if (resultCode == Activity.RESULT_OK) {
                // User enabled Bluetooth, perform your desired operations
                Log.d("BluetoothTag", "Bluetooth enabled by user.");
                // You can perform any operation here, e.g., start scanning, connect to devices, etc.
                redirectToPresetPage();
            } else {
                Log.d("BluetoothTag", "User denied enabling Bluetooth.");
            }
        }
    }

    private void checkBluetoothPermission() {
        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted, request it
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_REQUEST_CODE);
            } else {
                // Permission already granted, proceed with the scan
            }
        } catch (Exception e) {
            Log.e("UfillPresetActivity", "Error in checkBluetoothPermission: " + e.getMessage(), e);
            fileWrite(context, todayDate + ".txt", "VoucherReeedemActitiy checkBluetoothPermission Exception", e.toString());
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d("methodCalled", "onRequestPermissionsResult Ufill PresetActivity");
        Log.d("requestCode", String.valueOf(requestCode));
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // If request is cancelled, the result arrays are empty
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with the scan
                Log.d("BluetoothTag", "108 Lines");
            } else {
                // Permission denied, show a message to the user
                MessagesDialog.showDialog(VoucherRedeemActivity.this, "Permission required to perform Bluetooth scan", 0, null, null);
                //Toast.makeText(this, "Permission required to perform Bluetooth scan", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void redirectToPresetPage(){
        Intent intent = new Intent(context, UfillPresetActivity.class);
        intent.putExtra("ufillModel", ufillModel);
        startActivityForResult(intent, VOUCHER_REEDEM_CODE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}