package com.ims.bpcluat.ufill;

import static com.ims.bpcluat.Helper.authCodeForUfillTxn;
import static com.ims.bpcluat.Helper.cashChargeslipDate;
import static com.ims.bpcluat.Helper.cashChargeslipTime;
import static com.ims.bpcluat.Helper.cashNotificationDate;
import static com.ims.bpcluat.Helper.cashNotificationTime;
import static com.ims.bpcluat.Helper.errorCodeForUfillTxn;
import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.presetValueForUfillTxn;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.conversion.BleNotificationRequest.createRequest;
import static com.ims.bpcluat.conversion.BleNotificationRequest.twentyEightRequest;
import static com.ims.bpcluat.helper.BleDeviceHelper.commandProtocol;
import static com.ims.bpcluat.helper.BleDeviceHelper.createRequestForTwentyNine;
import static com.ims.bpcluat.helper.BleDeviceHelper.retryOneMoreTime;
import static com.ims.bpcluat.service.BleService.addCrcInTwentyEightRequest;
import static com.ims.bpcluat.service.BleService.createRequestFor27h;
import static com.ims.bpcluat.validation.VehicleNoValidation.bharatVehicleNoValidation;
import static com.ims.bpcluat.validation.VehicleNoValidation.validateVehicleNo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.TwentyNineTxnParser;
import com.ims.bpcluat.conversion.DecimalToHex;
import com.ims.bpcluat.conversion.MobileToHex;
import com.ims.bpcluat.conversion.TextToHex;
import com.ims.bpcluat.conversion.VehicleToHex;
import com.ims.bpcluat.databinding.ActivityUfillPresetBinding;
import com.ims.bpcluat.dialog.BluetoothConnectionDialog;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.BleDeviceHelper;
import com.ims.bpcluat.helper.NozzleIDMapper;
import com.ims.bpcluat.model.UfillModel;
import com.ims.bpcluat.validation.MobileNoValidation;

public class UfillPresetActivity extends AppCompatActivity {

    ActivityUfillPresetBinding binding;
    Context context;
    BluetoothAdapter bluetoothAdapter;
    ProgressDialog progress;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private UfillModel ufillModel;
    String pumpNo = "", nozzleNo = "", voucherAmt = "", authCode = "";
    String currentApiCall = "";

    boolean isTwentySevenCall = false;
    boolean isTwentySevenByteSend = false;
    boolean isTwentySevenByteReceive = false;
    boolean isTwentySevenSuccess = false;

    boolean isTwentyEightCall = false;
    boolean isTwentyEightByteSend = false;
    boolean isTwentyEightByteReceive = false;
    boolean isPresetSuccess = false;
    String twentyNineResponse = "", bleTxnId = "";

    boolean isTwentyNineCall = false;
    boolean isTwentyNineByteSend = false;
    boolean isTwentyNineByteReceive = false;
    boolean isTxnSuccess = false;

    String mob = ""; // change
    String veh = ""; // change
    boolean showPopupValue = false;
    
    private Handler handler = new Handler();
    Handler secondHandler = new Handler();
    private BleDeviceHelper bleDeviceHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            binding = ActivityUfillPresetBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            context = this;

            progress = new ProgressDialog(this);
            progress.setTitle("Loading");
            progress.setMessage("Wait while loading...");
            progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
            progress.show();

            bleDeviceHelper = bleDeviceHelper.getInstance(this);
            bleDeviceHelper.setBleCallback(new BleDeviceHelper.BleCallback() {
                @Override
                public void onBleConnected() {
                    try {
                        Log.d("UfillPresetActivity", "BLE Connected, ready to send command");
                        if(currentApiCall.equals("28h")){
                            currentApiCall = "28h";
                            commandProtocol = "28h";
                            String terminalId = TextToHex.convert(Helper.tid, 20); // 20 char
                            String mob = "20202020202020202020202020"; // change
                            String veh = "20202020202020202020"; // change
                            String voucherIdOrOrderId = TextToHex.convert(Helper.createTxnIdForOfflineTxn(), 40); //  40 char
                            String extraOrCashMemoNo = "20202020202020202020";
                            String extraOrTransactionReferenceNo = "20202020202020202020202020202020202020202020202020202020202020202020202020202020";  //80 char
                            if (!ufillModel.getAuthCode().isEmpty()) {
                                extraOrTransactionReferenceNo = TextToHex.convert(ufillModel.getAuthCode(), 80);
                            }

                            Log.d("skpPumpDec", pumpNo);
                            String blePump = DecimalToHex.create(Integer.parseInt(pumpNo));
                            if (blePump.length() == 1) {
                                blePump = "0" + blePump;
                            }
                            Log.d("skpPumpHex", pumpNo);

                            isTwentyEightByteSend = true;
                            String twentyEightReq = twentyEightRequest(blePump, "00", "02", presetValueForUfillTxn(voucherAmt), "15",
                                    "06", mob, veh, "00", voucherIdOrOrderId, extraOrCashMemoNo,
                                    terminalId, extraOrTransactionReferenceNo);
                            String finalRequest = addCrcInTwentyEightRequest(twentyEightReq);
                            String commandHex = finalRequest; // Or any dynamic value
                            Log.d("twentyEightRequest", finalRequest);
                            fileWrite(context, todayDate + ".txt", "twentyEightRequest :", finalRequest);
                            byte[] command = BleDeviceHelper.hexStringToByteArray(commandHex);
                            bleDeviceHelper.writeCharacteristic.setValue(command);
                            bleDeviceHelper.bluetoothGatt.writeCharacteristic(bleDeviceHelper.writeCharacteristic);
                        }else if(currentApiCall.equals("29h")){
                            commandProtocol = "29h";
                            String twentyNineReq = createRequestForTwentyNine(pumpNo);
                            fileWrite(context, todayDate + ".txt", "twentyNineRequest :", twentyNineReq);
                            Log.d("twentyNineRequest", twentyNineReq);
                            isTwentyNineCall = true;
                            isTwentyNineByteSend = true;
                            byte[] command = BleDeviceHelper.hexStringToByteArray(twentyNineReq);
                            bleDeviceHelper.writeCharacteristic.setValue(command);
                            bleDeviceHelper.bluetoothGatt.writeCharacteristic(bleDeviceHelper.writeCharacteristic);
                        }else if(currentApiCall.equals("27h")){
                            commandProtocol = "27h";
                            String twentySevenReq = createTwentySevenRequest();
                            fileWrite(context, todayDate + ".txt", "twentySevenRequest Ufill:", twentySevenReq);
                            Log.d("tewntySevenRequest = ", twentySevenReq);
                            isTwentySevenCall = true;
                            isTwentySevenByteSend = true;
                            byte[] command = BleDeviceHelper.hexStringToByteArray(twentySevenReq);
                            bleDeviceHelper.writeCharacteristic.setValue(command);
                            bleDeviceHelper.bluetoothGatt.writeCharacteristic(bleDeviceHelper.writeCharacteristic);
                        }

                    } catch (SecurityException e) {
                        Log.d("UfillPresetSecurity", e.toString());
                    }
                }

                @Override
                public void onBleResponseReceived(String response) {
                    Log.d("UfillPresetActivity", "Received BLE Response: " + response);
                    if(currentApiCall.equals("28h")){
                        isTwentyEightByteReceive = true;
                        String errorCode = errorCodeForUfillTxn(response);
                        authCode = authCodeForUfillTxn(response);
                        String msg = "Preset Successful on Bay No " + pumpNo + "\n Please pick up Nozzle to Start Fueling";
                        Log.d("28hResponse errorCode = ", errorCode);
                        Log.d("28hResponse authCode", authCode);
                        if (errorCode.equals("01")) {
                            // Success Block
                            progress.dismiss();
                            isPresetSuccess = true;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    binding.textMessage.setText(msg);
                                    binding.dialogContainer.setVisibility(View.VISIBLE);
                                }
                            });
                            //PresetSuccessDialog.showDialog(this, msg);
                            Runnable sendRequestRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    if (!isTxnSuccess) {
                                        //sendTwentyNineRequest(); // Call your method sonu
                                        currentApiCall = "29h";
                                        bleDeviceHelper.initiateBleScan();
                                        // Schedule the next execution
                                        secondHandler.postDelayed(this, 5000); // 10000 ms = 10 seconds
                                    }
                                }
                            };
                            secondHandler.post(sendRequestRunnable);
                        } else {
                            progress.dismiss();
                            // Error Block
                            Log.d("redirect", "one");
                            Intent intent = new Intent();
                            String errorMsg = "Connection established\n Failed, Error Code - " + errorCode;
                            intent.putExtra("ufillTxnStatus", errorMsg);
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    }else if(currentApiCall.equals("29h")){
                        Log.d("Preset Page","29h Recevied Block");
                        isTwentyNineByteReceive = true;
                        twentyNineResponse = response;
                            isTxnSuccess = true;
                            fileWrite(context, todayDate + ".txt", "twentyNineResponseAuthCheck:", "matched");
                            bleTxnId = TwentyNineTxnParser.getBleTxnId(response);
                            String txnId = TwentyNineTxnParser.getYearMonthDayUniqueId(response);
                            String productName = TwentyNineTxnParser.getProductName(response);
                            String amt = TwentyNineTxnParser.getAmount(response);
                            String volume = TwentyNineTxnParser.getVolume(response);

                            String fccTimeStamp = TwentyNineTxnParser.fccTimeStamp(response);
                            String productPrice = TwentyNineTxnParser.getProductPrice(response);
                            String txnStartTime = TwentyNineTxnParser.getTxnStartTime(response);
                            String presetType = TwentyNineTxnParser.getPresetType(response);
                            String productId = TwentyNineTxnParser.getProductId(response);

                            NozzleIDMapper nozzleIDMapper = new NozzleIDMapper();
                            String pumpforlocalMpd = pumpNo;
                            if (pumpforlocalMpd.startsWith("0") && pumpforlocalMpd.length() > 1) {
                                pumpforlocalMpd = pumpforlocalMpd.substring(1);
                            }
                            String localMPDID = nozzleIDMapper.getLocalMPDIDForGlobalNozzleID(Helper.metaHosResponse, pumpforlocalMpd);

                            ufillModel.setTxnId(txnId);
                            ufillModel.setProduct(productName);
                            ufillModel.setFuelledAmt(amt);
                            ufillModel.setQty(volume);
                            ufillModel.setFccTimeStamp(fccTimeStamp);
                            ufillModel.setProductPrice(productPrice);
                            ufillModel.setTxnStartTime(txnStartTime);
                            ufillModel.setPresetType(presetType);
                            ufillModel.setProductId(productId);
                            ufillModel.setVolume(volume);
                            ufillModel.setLocalMpdId(localMPDID);
                            fileWrite(context, todayDate + ".txt", "twentyNineResponseTxnId:", txnId);
                            //stopTwentyNineRequestLoop(); // Stop the retry loop
                            String date = cashNotificationDate();
                            String time = cashNotificationTime();
                            ufillModel.setTxnNotificationDate(date);
                            ufillModel.setTxnNotificationTime(time);
                            ufillModel.setTxnChargselipDate(cashChargeslipDate());
                            ufillModel.setTxnChargeslipTime(cashChargeslipTime());
                            runOnUiThread(() -> showPopup());
                    }else if(currentApiCall.equals("27h")){
                        // Progress we will dismiss in success page
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Intent intent = new Intent(UfillPresetActivity.this, UfillSuccessActivity.class);
                            intent.putExtra("ufillModel", ufillModel);
                            startActivity(intent);
                        });
                    }
                }

                @Override
                public void onBleConnectionFailed() {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Log.d("UfillPresetActivity", "onBleConnectionFailed method");
                        Log.d("currentApiCall",currentApiCall);
                        Log.d("UfillPresetActivity", "BLE connection failed or no matching device found");
//                        if (progress != null && progress.isShowing()) {
//                            progress.dismiss();
//                        }
                        if(currentApiCall.equals("28h")){
                            retryOneMoreTime = true;
                            bleDeviceHelper.disconnect();
                        }else if(currentApiCall.equals("29h")){
                            retryOneMoreTime = true;
                            bleDeviceHelper.disconnect();
                        }else if(currentApiCall.equals("27h")){
                            retryOneMoreTime = true;
                            bleDeviceHelper.disconnect();
                        }
//                        BluetoothConnectionDialog.showDialog(getApplicationContext());
                    });
                }

                @Override
                public void onBluetoothTurnedOff() {
                    Log.d("UfillPresetActivity", "Bluetooth is manually off by user.");
                    try{
                        runOnUiThread(() -> Toast.makeText(context, "Bluetooth is off.", Toast.LENGTH_LONG).show());
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }catch (SecurityException e){
                        Log.d("SecurityException onBluetoothTurnedOff",e.toString());
                    }
                }
            });

            fileWrite(context, todayDate + ".txt", "Landing Page : ", "UfillPresetAct");

            binding.cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //dialogContainer.setVisibility(View.GONE);
                    try {
                        //bleHelper.stopScanning();
//                        bleHelper.closeConnection();
//                        bleHelper.forceDisconnectExistingConnections();
                    } catch (Exception e) {
                        Log.e("UfillPresetActivity", "Error stopping BLE scan in onDestroy: " + e.getMessage(), e);
                        fileWrite(context, todayDate + ".txt", "UfillPresetActivity Error stopping BLE scan in onDestroy", e.toString());
                    }

                    Intent intent = new Intent(UfillPresetActivity.this, SideBarActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            });

            ufillModel = getIntent().getParcelableExtra("ufillModel");
            if (ufillModel != null) {
                pumpNo = ufillModel.getPumpNo();
                nozzleNo = ufillModel.getNozzleNo();
                voucherAmt = ufillModel.getVoucherAmt();
                Helper.txnReleatedPumpNo = pumpNo;
                Log.d("voucherAmtValue", voucherAmt);
                fileWrite(context, todayDate + ".txt", "UfillPresetActivity ", "Voucher Amt :" + voucherAmt);
            }

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                MessagesDialog.showDialog(UfillPresetActivity.this, "Device does not support Bluetooth.", 0, null, null);
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
        } catch (Exception e) {
            Log.e("UfillPresetActivity", "Error in onCreate: " + e.getMessage(), e);
            fileWrite(context, todayDate + ".txt", "UfillPresetActivity onCreate Exception", e.toString());
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
                connectBluetooth();
            }
        } catch (Exception e) {
            Log.e("UfillPresetActivity", "Error in checkBluetoothPermission: " + e.getMessage(), e);
            fileWrite(context, todayDate + ".txt", "UfillPresetActivity checkBluetoothPermission Exception", e.toString());
        }
    }

    public void connectBluetooth() {
        try {
            Log.d("methodCalled", "UfillPresetActivity connectBluetooth");
            fileWrite(context, todayDate + ".txt", "UfillPresetAct:method Called = ", "connectBluetooth");

            if (!isTwentyEightCall && !isPresetSuccess) {
                Log.d("connectBluetoothMethod", "If Block");
                commandProtocol = "28h";
                isTwentyEightCall = true;
                currentApiCall = "28h";
            } else if (isTxnSuccess) {
                Log.d("connectBluetoothMethod", "Else If Block");
                currentApiCall = "27h";
            } else {
                Log.d("connectBluetoothMethod", "else Block");
                currentApiCall = "29h";
            }
            bleDeviceHelper.initiateBleScan();
        } catch (Exception e) {
            Log.e("UfillPresetActivity", "Error in connectBluetooth: " + e.getMessage(), e);
            fileWrite(context, todayDate + ".txt", "UfillPresetActivity connectBluetooth Exception", e.toString());
        }
    }

    private String createTwentySevenRequest() {
        Log.d("bleNotification", "bleNotification method called");
        String blePump = DecimalToHex.create(Integer.parseInt(pumpNo));
        if (blePump.length() == 1) {
            blePump = "0" + blePump;
        }
        String netAmount = presetValueForUfillTxn(ufillModel.getFuelledAmt());
        Log.d("myLognetAmount", netAmount);
        String isMopChange = "01";
        String isTrxPrinted = "01";  // change
        String isDiscountApply = "00";
        String discount = "00000000";
        String terminalId = "20202020202020202020";  // change
        String bleTxnMop = "15";
        String blePaymentMode = "06";
        String vehicleTypeSegment = "00"; // Four wheeler // change
        String voucherIdOrOrderId = "3038323233333535373133333939383934353800";
        String extraOrCashMemoNo = "20202020202020202020";
        String extraOrTransactionReferenceNo = "20202020202020202020202020202020202020202020202020202020202020202020202020202020";  //80 char
        if (!ufillModel.getAuthCode().isEmpty()) {
            extraOrTransactionReferenceNo = TextToHex.convert(ufillModel.getAuthCode(), 80);
        }


        ufillModel.setMobileNumber(mob);
        ufillModel.setVehicleNumber(veh);

        if(mob.isEmpty()){
            mob = "20202020202020202020202020";
        }else{
            mob = MobileToHex.create(mob);
        }

        if(veh.isEmpty()){
            veh = "20202020202020202020";
            veh = veh.toUpperCase();
        }else{
            veh = VehicleToHex.convert(veh);
        }


        String bleNotificationRequest = createRequestFor27h(createRequest(
                blePump, bleTxnId, isMopChange, isTrxPrinted, isDiscountApply, discount,
                netAmount, terminalId, bleTxnMop, blePaymentMode, mob, veh,
                vehicleTypeSegment, voucherIdOrOrderId, extraOrCashMemoNo, extraOrTransactionReferenceNo));
        Log.d("skpTxnId", bleTxnId);
        return bleNotificationRequest;
    }

    @Override
    public void onDestroy() {
        try {
            super.onDestroy();
            // Remove the callback if the activity is destroyed before 7 seconds
            Log.d("UfillPreset", "onDestroy method called");
            fileWrite(context, todayDate + ".txt", "UfillPresetAct:method Called = ", "onDestroy");
            handler.removeCallbacksAndMessages(null);
            secondHandler.removeCallbacksAndMessages(null);
        } catch (SecurityException e) {
            Log.e("UfillPresetActivity", "Security in onDestroy: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e("UfillPresetActivity", "Error in onDestroy: " + e.getMessage(), e);
            fileWrite(context, todayDate + ".txt", "UfillPresetActivity onDestroy Exception", e.toString());
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
                connectBluetooth();
            } else {
                // Permission denied, show a message to the user
                MessagesDialog.showDialog(UfillPresetActivity.this, "Permission required to perform Bluetooth scan", 0, null, null);
                //Toast.makeText(this, "Permission required to perform Bluetooth scan", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Log.d("UfillPresetSuccess", "Bluetooth on ho gya hai");
                //Toast.makeText(getActivity(), "Bluetooth was on", Toast.LENGTH_SHORT).show();
                checkBluetoothPermission();
                // Bluetooth was enabled
            } else {
                // Bluetooth was not enabled
                Log.d("skpResult", "Bluetooth off ho gya hai");
                MessagesDialog.showDialog(context, "Bluetooth is off", 0, null, null);
                // Toast.makeText(getActivity(), "Bluetooth is off", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showPopup() {
        if(showPopupValue){
            return;
        }
        // Create an AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        // Inflate the custom layout/view
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.ufill_customer_details, null);

        // Find the EditText, Button, Title, and Close Icon in the custom layout
        EditText mobileNumberEditText = dialogView.findViewById(R.id.mobileNumberEditText);
        EditText vehicleNumberEditText = dialogView.findViewById(R.id.vehicleNumberEditText);

        mobileNumberEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
        vehicleNumberEditText.setFilters(new InputFilter[]{new InputFilter.AllCaps()});

        Button buttonSubmit = dialogView.findViewById(R.id.buttonSubmit);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        ImageView closeIcon = dialogView.findViewById(R.id.closeIcon);

        // Set the custom layout to the AlertDialog builder
        builder.setView(dialogView);

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.setCancelable(false); // Prevent closing on back press
        alertDialog.setCanceledOnTouchOutside(false); // Prevent closing when clicking outside
        alertDialog.show();
        showPopupValue = true;

        // Set the onClickListener for the close icon
        closeIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (alertDialog.isShowing()) {
                        alertDialog.dismiss();
                    }
                    currentApiCall = "27h";
                    bleDeviceHelper.initiateBleScan();
                    if (!((Activity) context).isFinishing()) {
                        progress.show();
                    }
                });
            }
        });

        // Set the onClickListener for the submit button
        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mobileNumber = mobileNumberEditText.getText().toString().trim();
                String vehicleNumber = vehicleNumberEditText.getText().toString().trim();

                if (mobileNumber.isEmpty() && vehicleNumber.isEmpty()) {
                    Toast.makeText(UfillPresetActivity.this, "Please enter at least Mobile Number or Vehicle Number", Toast.LENGTH_SHORT).show();
                } else {
                    // Proceed with your logic
                    if (!mobileNumber.isEmpty()) {
                        if (mobileNumber.length() != 10) {
                            mobileNumberEditText.setError("Mobile Number must be 10 digits");
                            mobileNumberEditText.requestFocus();
                            return;
                        }
                        if (mobileNumber.startsWith("0")) {
                            mobileNumberEditText.setError("Mobile Number cannot start with zero");
                            mobileNumberEditText.requestFocus();
                            return;
                        }
                        if (mobileNumber.equals("1234567890")) {
                            mobileNumberEditText.setError("Please enter valid mobile number");
                            mobileNumberEditText.requestFocus();
                            return;
                        }
                        if (MobileNoValidation.hasSameNumber(mobileNumber)) {
                            mobileNumberEditText.setError("All digits of mobile number cannot be same.");
                            mobileNumberEditText.requestFocus();
                            return;
                        }
                    }

                    if(!vehicleNumber.isEmpty()){
                        if (vehicleNumber.length() >= 9) {
                            if (validateVehicleNo(vehicleNumber) || bharatVehicleNoValidation(vehicleNumber)) {
                                //vehicleNumberEditText.setError(null); // No error
                            } else {
                                vehicleNumberEditText.setError("Invalid vehicle number");
                                return;
                            }
                        }else{
                            vehicleNumberEditText.setError("Invalid vehicle number");
                            return;
                        }
                    }
                    alertDialog.dismiss();
                    mob = mobileNumber;
                    veh = vehicleNumber;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (alertDialog.isShowing()) {
                            alertDialog.dismiss();
                        }
                        mob = mobileNumber;
                        veh = vehicleNumber;
                        if (!((Activity) context).isFinishing()) {
                            progress.show();
                        }
                        currentApiCall = "27h";
                        bleDeviceHelper.initiateBleScan();
                    });

                }
                // Handle the input text
            }
        });
    }
}
