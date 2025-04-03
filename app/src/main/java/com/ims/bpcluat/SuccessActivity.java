package com.ims.bpcluat;

import static com.ims.bpcluat.Helper.*;
import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.ReadWriteHelper.createLogFile;
import static com.ims.bpcluat.ReadWriteHelper.createRequestFile;
import static com.ims.bpcluat.conversion.BleNotificationRequest.createRequest;
import static com.ims.bpcluat.helper.BleDeviceHelper.commandProtocol;
import static com.ims.bpcluat.helper.BleDeviceHelper.retryOneMoreTime;
import static com.ims.bpcluat.service.BleService.createRequestFor27h;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import static com.ims.bpcluat.Helper.fileWrite;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.ims.bpcluat.conversion.CreateTransactionId;
import com.ims.bpcluat.conversion.HexToDecimal;
import com.ims.bpcluat.conversion.MobileToHex;
import com.ims.bpcluat.conversion.VehicleToHex;
import com.ims.bpcluat.databinding.ActivitySuccessBinding;
import com.ims.bpcluat.dialog.BluetoothConnectionDialog;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.helper.BleDeviceHelper;
import com.ims.bpcluat.helper.ChargeslipHelper;
import com.ims.bpcluat.interfaces.PrintResponseCallBack;
import com.ims.bpcluat.model.OnlineTxnModel;
import com.ims.bpcluat.utils.Cache;
import com.ims.bpcluat.validation.MobileNoValidation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SuccessActivity extends AppCompatActivity implements PrintResponseCallBack,ApiHelper.NetworkingApiCallBack{

    ActivitySuccessBinding binding;
    BluetoothAdapter bluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private OnlineTxnModel onlineTxnModel;
    ChargeslipHelper chargeslipHelper = new ChargeslipHelper();
    String myDeviceInfo;
    ApiHelper api;
    Bitmap merchantTxnChargeSlip,customerTxnChargeSlip,fuelBillChargeSlip;
    String cashRrn = "";
    Context context;
    ProgressDialog progress;
    String product, mobileNumber = "", mobileNumberWithoutMasked = "",vehicleNumber = "", vehicleType = "", txnType, notificationDate, notificationTime;
    String qty = "", amount = "", chargselipDate,chargselipTime, authBank = "", authCode = "",rrn = "";
    String atc = "", cardType = "", posEntryMode = "", terminalInvoiceNo = "" , batchNo = "" ,aid = "" ,tsi ="";
    String tvr = "",transactionCertificate = "", cardPaymentVersionNo = "", authTid = "", cardNo ="", txnId = "";
    String pumpNo = "", nozzleNo = "",field3 = "",field7 = "",field9 = "",field13 = "",cardTxnCustomerName = "",bleTxnId = "";
    String cardFirst = "", cardLast = "",unitPrice = "";
    String bleNotificationRequest = "";
    String tewntySevenRequest = "";
    String fccAcknowledgement = "No",bleTxnMop = "",blePaymentMode = "",isTxnOnline,field1;
    String presetType = "",presetValue = "",txnStartDateTime = "",txnEndDateTime = "",charegeslipBayNo = "",chargeslipNozzleNo = "";
    String fuelType = "";
    private static boolean oceanApiCalled = false;
    Date date = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
    String todayDate = formatter.format(date);
    private Handler handler = new Handler();
    private boolean txnNotificationSendCount = false;
    String uniqueId = "";
    private boolean isDialogVisible = false;
    String resCode = "";
    private JSONObject requestJsonObject;

    private BleDeviceHelper bleDeviceHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuccessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        context = this;

        progress = new ProgressDialog(SuccessActivity.this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            askPermissionForStorage();
        }

        Cache.listCacheFiles((this));

        fileWrite(context, todayDate + ".txt", "Landing Page : ","Success Act");

        onlineTxnModel = getIntent().getParcelableExtra("onlineTxnModel");
        if(onlineTxnModel != null){
            isTxnOnline = onlineTxnModel.getIsTxnOnline();
            bleTxnMop = onlineTxnModel.getBleTxnMop();
            blePaymentMode = onlineTxnModel.getBlePaymentMode();
            pumpNo = onlineTxnModel.getPumpNo();
            nozzleNo = onlineTxnModel.getPumpNo();
           // txnId = onlineTxnModel.getTxnId();
            qty =  onlineTxnModel.getQty();
            unitPrice = onlineTxnModel.getUnitPrice();
            product = onlineTxnModel.getProductName();
            amount = onlineTxnModel.getAmount();
            mobileNumber = onlineTxnModel.getMobileNumber();
            mobileNumberWithoutMasked = onlineTxnModel.getMobileNumber();
            if (!mobileNumber.isEmpty()) {
                mobileNumber = mobileNumberMasking(mobileNumber);
            }
            vehicleNumber = onlineTxnModel.getVehicleNumber();
            vehicleType = onlineTxnModel.getVehicleType();
            txnType = onlineTxnModel.getTxnType();
            if(txnType.equals("SALES")){
                txnType = "ALP";
                field3 = onlineTxnModel.getField3();
                field7 = onlineTxnModel.getField7();
                field9 = onlineTxnModel.getField9();
                field13 = onlineTxnModel.getField13();
            }
            notificationDate = onlineTxnModel.getTxnNotificationDate();
            notificationTime = onlineTxnModel.getTxnNotificationTime();
            chargselipDate = onlineTxnModel.getTxnChargselipDate();
            chargselipTime = onlineTxnModel.getTxnChargeslipTime();
            if(txnType.equals("CARD")){
                authBank = onlineTxnModel.getAuthBank();
                cardType = onlineTxnModel.getCardType();
                posEntryMode = onlineTxnModel.getPosEntryMode();
                terminalInvoiceNo = onlineTxnModel.getTerminalInvoiceNo();
                batchNo = onlineTxnModel.getBatchNo();
                aid = onlineTxnModel.getAid();
                tsi = onlineTxnModel.getTsi();
                tvr = onlineTxnModel.getTvr();
                transactionCertificate = onlineTxnModel.getTransactionCertificate();
                cardPaymentVersionNo = onlineTxnModel.getCardPaymentVersionNo();
                authTid = onlineTxnModel.getAuthTid();
                atc = onlineTxnModel.getAtc();
                cardNo = onlineTxnModel.getCardNo();
                cardFirst = onlineTxnModel.getCardFirst();
                cardLast = onlineTxnModel.getCardLast();
                cardTxnCustomerName = onlineTxnModel.getCardTxnCustomerName();
            }
            authCode = onlineTxnModel.getAuthCode();
            rrn = onlineTxnModel.getRrn();

            if(isTxnOnline.equals("no")){
                fuelType = "POS SLIP";
                txnId = onlineTxnModel.getTxnId();
                field1 = "Offline";
            }else{
                fuelType = "FUEL BILL";
                presetType = onlineTxnModel.getPresetType();
                presetValue = onlineTxnModel.getPresetValue();
                txnStartDateTime = onlineTxnModel.getTxnStartDateTime();
                txnEndDateTime = onlineTxnModel.getTxnEndDateTime();
                charegeslipBayNo = onlineTxnModel.getCharegeslipBayNo();
                chargeslipNozzleNo = onlineTxnModel.getChargeslipNozzleNo();
                field1 = "Online";
                txnId = createTxnId();
                Log.d("myTxnid",txnId);
                tewntySevenRequest = createRequestFor27h(bleNotification(Helper.txnListPostionSelected));
            }
        }

        api = new ApiHelper();
        myDeviceInfo = Build.MODEL;
        Log.d("myDeviceInfo", myDeviceInfo);
        if (myDeviceInfo.equals("A50")) {
            binding.printBtn.setVisibility(View.GONE);
            binding.nextCopyBtn.setVisibility(View.VISIBLE);
            binding.nextCopyBtn.setText("Customer Copy");
        } else {
//            binding.ereceiptBtn.setVisibility(View.GONE);
        }

//        binding.ereceiptBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(mobileNumber.isEmpty()){
//                    showPopup();
//                }else{
//                    eReceiptApi();
//                }
//            }
//        });

        binding.nextCopyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(txnType.equals("ALP")){
                    if(binding.nextCopyBtn.getText().toString().equals("Customer Copy")){
                        binding.chargeslip.setImageBitmap(customerTxnChargeSlip);
                        binding.nextCopyBtn.setText("POS Clip");
                    }else if(binding.nextCopyBtn.getText().toString().equals("POS Clip")){
                        binding.chargeslip.setImageBitmap(fuelBillChargeSlip);
                        binding.nextCopyBtn.setText("Customer Copy");
                        binding.nextCopyBtn.setVisibility(View.GONE);
                    }
                }else{
                    if(binding.nextCopyBtn.getText().toString().equals("Customer Copy")){
                        binding.chargeslip.setImageBitmap(customerTxnChargeSlip);
                        binding.nextCopyBtn.setText("Customer Copy");
                        binding.nextCopyBtn.setVisibility(View.GONE);
                    }
                }

            }
        });

        JSONObject jsonObject = new JSONObject();
        JSONObject alpSaleObject = new JSONObject();
        try {
            String productName = product;
            if(product.equals("MS")){
                productName = "PETROL";
            }else if(product.equals("HSD")){
                productName = "DIESEL";
            }
            if(txnType.equals("ALP")) {
                jsonObject.put("agencyName", onlineTxnModel.getROName());
                jsonObject.put("city", onlineTxnModel.getRoCity());
                jsonObject.put("dealerContactNo",onlineTxnModel.getRoMobileNo());
                jsonObject.put("type",onlineTxnModel.getAlpType());

            }else {
                jsonObject.put("agencyName", roName);
                jsonObject.put("city", city);
                jsonObject.put("dealerContactNo",dealerContactNumber);
            }
//            jsonObject.put("address", address1);
            jsonObject.put("date", chargselipDate);
            jsonObject.put("time", chargselipTime);
            jsonObject.put("bayNo", charegeslipBayNo);
            jsonObject.put("nozzleNo", chargeslipNozzleNo);
            jsonObject.put("product", productName);
            jsonObject.put("payMode", txnType);
            jsonObject.put("txnId", txnId);
            jsonObject.put("attendentName", Helper.operatorFirstName + " " + Helper.operatorLastName);
            jsonObject.put("txnStart", txnStartDateTime);
            jsonObject.put("txnEnd", txnEndDateTime);
            jsonObject.put("rate", unitPrice);
            jsonObject.put("volume", qty);
            jsonObject.put("amount", amount);
            jsonObject.put("presetType", presetType);
            jsonObject.put("presetValue", presetValue);
            jsonObject.put("vehicleNo", vehicleNumber);
            jsonObject.put("mobileNo", mobileNumber);
            jsonObject.put("nfrProductName","");
            jsonObject.put("nfrUnitPrice","");
            jsonObject.put("nfrVolume","");
            jsonObject.put("nfrTotalAmount","");
            if(batchNo.isEmpty()){
                jsonObject.put("batchNo",batchNo);
            }else{
                jsonObject.put("batchNo",padWithZeroes(Integer.parseInt(batchNo), 6));
            }
            if(terminalInvoiceNo.isEmpty()){
                jsonObject.put("terminalInvoiceNo",terminalInvoiceNo);
            }else{
                jsonObject.put("terminalInvoiceNo",padWithZeroes(Integer.parseInt(terminalInvoiceNo), 6));
            }
            jsonObject.put("cardNo",cardNo);
            jsonObject.put("authCode",authCode);
            jsonObject.put("cardTxnCustomerName",cardTxnCustomerName);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        if(txnType.equals("ALP")){
            try {
                Log.d("txnTypeCheck", txnType);
                alpSaleObject.put("agencyName", onlineTxnModel.getROName());
//                alpSaleObject.put("address", address1);
                alpSaleObject.put("city", onlineTxnModel.getRoCity());
                alpSaleObject.put("roMobileNo", onlineTxnModel.getRoMobileNo());
                alpSaleObject.put("date", chargselipDate);
                alpSaleObject.put("time", chargselipTime);
                alpSaleObject.put("tid", onlineTxnModel.getAlpTid());
                alpSaleObject.put("txnId", onlineTxnModel.getAlpTxnId());
                alpSaleObject.put("slipNo", onlineTxnModel.getAlpSlipNo());
                alpSaleObject.put("reportId", onlineTxnModel.getAlpReportId());
                alpSaleObject.put("type", onlineTxnModel.getAlpType());
                alpSaleObject.put("txnSource", onlineTxnModel.getAlpTxnSource());
                alpSaleObject.put("custName", onlineTxnModel.getAlpCustName());
                alpSaleObject.put("accountNo", onlineTxnModel.getAlpAccNo());
                alpSaleObject.put("cardId", onlineTxnModel.getAlpCardId());
                alpSaleObject.put("vehCard", onlineTxnModel.getAlpVechCard());
                alpSaleObject.put("odometer", onlineTxnModel.getAlpOdometer());
                alpSaleObject.put("wallet", onlineTxnModel.getAlpWallet());
                alpSaleObject.put("product", onlineTxnModel.getAlpProduct());
                alpSaleObject.put("rate", unitPrice);
                alpSaleObject.put("vol", qty);
                alpSaleObject.put("fuelAmount", onlineTxnModel.getAlpFuelAmount());
                alpSaleObject.put("tcsAmount", onlineTxnModel.getAlpTcsAmount());
                alpSaleObject.put("txnAmount", onlineTxnModel.getAlpTxnAmount());
                alpSaleObject.put("pmEarn", onlineTxnModel.getAlpPmEarn());
                alpSaleObject.put("meShare", onlineTxnModel.getAlpMeShare());
                alpSaleObject.put("cardBalance", onlineTxnModel.getAlpCardBalance());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            fuelBillChargeSlip = chargeslipHelper.chargeslip(SuccessActivity.this, jsonObject, fuelType);
            merchantTxnChargeSlip = chargeslipHelper.alpSaleChargeslip(SuccessActivity.this, alpSaleObject, "MERCHANT COPY");
            customerTxnChargeSlip = chargeslipHelper.alpSaleChargeslip(SuccessActivity.this, alpSaleObject, "CUSTOMER COPY");
            binding.chargeslip.setImageBitmap(merchantTxnChargeSlip);
        }else{
            merchantTxnChargeSlip = chargeslipHelper.chargeslip(SuccessActivity.this, jsonObject, "MERCHANT COPY");
            customerTxnChargeSlip = chargeslipHelper.chargeslip(SuccessActivity.this, jsonObject, "CUSTOMER COPY");
            binding.chargeslip.setImageBitmap(merchantTxnChargeSlip);
        }

        binding.printBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progress = new ProgressDialog(SuccessActivity.this);
                progress.setTitle("Loading");
                progress.setMessage("Wait while loading...");
                progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
                progress.show();
                chargeslipHelper.merchantDialog(SuccessActivity.this, merchantTxnChargeSlip);
                chargeslipHelper.setCallback((PrintResponseCallBack) context);
            }
        });

        binding.homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pumpHomePage();
            }
        });
        //connectBluetooth();


        bleDeviceHelper = bleDeviceHelper.getInstance(this);
        bleDeviceHelper.setBleCallback(new BleDeviceHelper.BleCallback() {
            @Override
            public void onBleConnected() {
                try {
                    Log.d("SuccessActivity", "BLE Connected, ready to send command");
                    String commandHex = tewntySevenRequest; // Or any dynamic value
                    Log.d("tewntySevenRequest", tewntySevenRequest);
                    commandProtocol = "27h";
                    byte[] command = BleDeviceHelper.hexStringToByteArray(commandHex);
                    bleDeviceHelper.writeCharacteristic.setValue(command);
                    bleDeviceHelper.bluetoothGatt.writeCharacteristic(bleDeviceHelper.writeCharacteristic);
                } catch (SecurityException e) {
                    Log.d("PumpFragmentSecurity", e.toString());
                }
            }

            @Override
            public void onBleResponseReceived(String response) {
                Log.d("SuccessActivity", "Received BLE Response: " + response);
                fileWrite(context,todayDate+".txt","twentySevenResponse :",response);

                fccAcknowledgement = "YES";
                if(!txnNotificationSendCount){
                    txnNotificationSendCount = true;
                    txnNotificationSend();
                }
                if(progress.isShowing()){
                    progress.dismiss();
                }

                Intent intent = new Intent(getApplicationContext(), SideBarActivity.class);
                txnArrayList.clear();
                startActivity(intent);
                finish();
            }

            @Override
            public void onBleConnectionFailed() {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Log.d("SuccessActivity", "BLE connection failed or no matching device found");
                    if (progress != null && progress.isShowing()) {
                        progress.dismiss();
                    }
                    bleDeviceHelper.disconnect();
                    retryOneMoreTime = false;
                    BluetoothConnectionDialog.showDialog(getApplicationContext());
                });
            }

            @Override
            public void onBluetoothTurnedOff() {
                Log.d("SuccessActivity", "Bluetooth is manually off by user.");
            }
        });
    }

    private String bleNotification(int position) {
        Log.d("bleNotification","bleNotification method called");
        Object myobj = txnArrayList.get(position);
        Gson gson = new Gson();
        String json = gson.toJson(myobj);
        try {
            JSONObject jsonObject = new JSONObject(json);
            String pumpNo = jsonObject.getString("PumpNumber");
            String date = HexToDecimal.convert(jsonObject.getString("Day"));
            String month = HexToDecimal.convert(jsonObject.getString("Month"));
            String year = HexToDecimal.convert(jsonObject.getString("Year"));
            String hour = HexToDecimal.convert(jsonObject.getString("Hour"));
            String min = HexToDecimal.convert(jsonObject.getString("Minute"));
            String second = HexToDecimal.convert(jsonObject.getString("TxnStartSecond"));
            String netAmount = jsonObject.getString("NetAmount");
            uniqueId = HexToDecimal.convert(jsonObject.getString("UniqueID"));

            if (date.length() == 1) {
                date = "0" + date;
            }
            if (month.length() == 1) {
                month = "0" + month;
            }
            if (year.length() == 2) {
                year = "20" + year;
            }
            if (hour.length() == 1) {
                hour = "0" + hour;
            }
            if (min.length() == 1) {
                min = "0" + min;
            }
            if (second.length() == 1) {
                second = "0" + second;
            }

            Log.d("uniqueId", uniqueId);
            Log.d("myLognetAmount",netAmount);
            String isMopChange = "01";
            String isTrxPrinted = "01";  // change
            String isDiscountApply = "00";
            String discount = "00000000";
            String terminalId = "20202020202020202020";  // change
//            Log.d("SuccessActivityMOP",bleTxnMop);
//            Log.d("SuccessActivityPaymentMode",blePaymentMode);

            String mob = mobileNumberWithoutMasked; // change
            String veh = vehicleNumber; // change

            if(mob.isEmpty()){
                mob = "20202020202020202020202020";
            }else{
                mob = MobileToHex.create(mob);
            }

            if(veh.isEmpty()){
                veh = "20202020202020202020";
            }else{
                veh = VehicleToHex.convert(veh);
            }

            String vehicleTypeSegment = "00"; // Four wheeler // change
            if(vehicleType.equals("2 W")){
                vehicleTypeSegment = "02";
            }else if(vehicleType.equals("3 W")) {
                vehicleTypeSegment = "03";
            }else if(vehicleType.equals("4 W")) {
                vehicleTypeSegment = "04";
            }

            String voucherIdOrOrderId = "3038323233333535373133333939383934353800";
            String extraOrCashMemoNo = "20202020202020202020";
            String extraOrTransactionReferenceNo = "20202020202020202020202020202020202020202020202020202020202020202020202020202020";

            bleNotificationRequest = createRequest(
                    pumpNo, bleTxnId, isMopChange, isTrxPrinted, isDiscountApply, discount,
                    netAmount, terminalId, bleTxnMop, blePaymentMode, mob, veh,
                    vehicleTypeSegment, voucherIdOrOrderId, extraOrCashMemoNo, extraOrTransactionReferenceNo);
            Log.d("skpTxnId", bleTxnId);
//            bluetoothHelper.scanAndConnect();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return bleNotificationRequest;
    }

    public void txnNotificationSend() {
        try {
            String url = "saveBillerTxn";
            JSONObject jsonObject = new JSONObject();
            JSONArray billerTranListArray = new JSONArray();
            JSONObject billerTranListObject = new JSONObject();
            JSONArray paramListArray = new JSONArray();

            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("id", "");
            jsonObject.put("channel", channelName);
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("userName", username);
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);

            billerTranListObject.put("mid", mid);
            billerTranListObject.put("tid", tid);
            if(txnType.equals("CASH")){
                billerTranListObject.put("auth_code", "");
            }else{
                billerTranListObject.put("auth_code", authCode);
            }
            billerTranListObject.put("inv_code", "");
            billerTranListObject.put("trans_type", "PURCHASE");
            billerTranListObject.put("trans_status", "SUCCESS");
            billerTranListObject.put("tran_amt", amount);
            billerTranListObject.put("tran_date", notificationDate);
            billerTranListObject.put("tran_time", notificationTime);

            if(txnType.equals("CASH")){
                billerTranListObject.put("rrn", cashRrn);
            }else{
                billerTranListObject.put("rrn", rrn);
            }
            if(txnType.equals("CARD")){
                billerTranListObject.put("card_first", cardFirst);
                billerTranListObject.put("card_last", cardLast);
            }else{
                billerTranListObject.put("card_first", "");
                billerTranListObject.put("card_last", "");
            }
            billerTranListObject.put("ft_number", txnId);
            billerTranListObject.put("session_id", "");

            billerTranListObject.put("cust_id", username);
            billerTranListObject.put("pay_method", txnType);
            billerTranListObject.put("authAmt", amount);
            billerTranListObject.put("refundAmt", "0");
            billerTranListObject.put("balanceAmt", amount);
            billerTranListObject.put("field1", field1);
            billerTranListObject.put("field2", "");
            billerTranListObject.put("field3", field3);
            billerTranListObject.put("field4", "");
            billerTranListObject.put("field5", "");
            billerTranListObject.put("field6", "");
            billerTranListObject.put("field7", field7);
            billerTranListObject.put("field8", "");
            billerTranListObject.put("field9", field9);
            billerTranListObject.put("field10", "");
            billerTranListObject.put("field11", "");
            billerTranListObject.put("field12", "");
            billerTranListObject.put("field13", field13);
            billerTranListObject.put("field14", "");
            billerTranListObject.put("field15", "");

            billerTranListArray.put(billerTranListObject);
            billerTranListObject.put("paramList", paramListArray);
            paramListArray.put(createJsonObject(operatorFirstName + " " + operatorLastName, "Attendant Name"));
            paramListArray.put(createJsonObject(username, "Attendant ID"));
            paramListArray.put(createJsonObject(coverage, "XCoverage"));
            paramListArray.put(createJsonObject("", "CHARGESLIP"));
            paramListArray.put(createJsonObject(sapCode, "SAP CODE"));
            paramListArray.put(createJsonObject(mobileNumber, "Customer Mobile"));
            paramListArray.put(createJsonObject(vehicleNumber, "Vehicle ID"));
            paramListArray.put(createJsonObject(pumpNo, "PUMP_NO"));
            paramListArray.put(createJsonObject(nozzleNo, "NOZZLE"));
            paramListArray.put(createJsonObject(qty, "QUANTITY"));
            paramListArray.put(createJsonObject(product, "PROD_NAME"));
            paramListArray.put(createJsonObject(version, "VERSION"));
            paramListArray.put(createJsonObject(onlineTxnModel.getUnitPrice(), "UNIT_PRICE"));
            paramListArray.put(createJsonObject("0.00", "CUSTOMER_DISC"));
            paramListArray.put(createJsonObject("", "ORDER_ID"));
            paramListArray.put(createJsonObject("", "FCC TIMESTAMP"));
            paramListArray.put(createJsonObject(roName, "MERCH NAME"));
            paramListArray.put(createJsonObject(vehicleType, "Vehicle_Type"));
            if(txnType.equals("CARD")){
                paramListArray.put(createJsonObject(cardType, "cardType"));
                paramListArray.put(createJsonObject(posEntryMode, "posEntryMode"));
                paramListArray.put(createJsonObject(terminalInvoiceNo, "terminalInvoiceNo"));
                paramListArray.put(createJsonObject(batchNo, "batchNo"));
                paramListArray.put(createJsonObject(aid, "AID"));
                paramListArray.put(createJsonObject(tsi, "TSI"));
                paramListArray.put(createJsonObject(tvr, "TVR"));
                paramListArray.put(createJsonObject(transactionCertificate, "transactionCertificate"));
                paramListArray.put(createJsonObject(cardPaymentVersionNo, "appVersionNo"));
                paramListArray.put(createJsonObject("", "Auth_TID"));
                paramListArray.put(createJsonObject("", "Auth_BANK"));
            }
            paramListArray.put(createJsonObject(fccAcknowledgement, "fcc_ack"));
            paramListArray.put(createJsonObject(serialNumber, "hwSrNo"));

            jsonObject.put("billerTranList", billerTranListArray);
            Log.d("notificationRequest = ", String.valueOf(jsonObject));
//            String jsonReq = "Request : "+ String.valueOf(jsonObject);
//            createLogAndRequestFile(context, jsonReq);
            fileWrite(context,todayDate+".txt","Ocean notification request :", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (Exception e) {
            Log.d("notificationException", e.toString());
        }
    }

    private JSONObject createJsonObject(String param, String paramLit) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("param", param);
            jsonObject.put("param_lit", paramLit);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private void showPopup() {
        // Create an AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Inflate the custom layout/view
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.ereceipt_popup_layout, null);

        // Find the EditText, Button, Title, and Close Icon in the custom layout
        EditText editTextPopup = dialogView.findViewById(R.id.editTextPopup);
        Button buttonSubmit = dialogView.findViewById(R.id.buttonSubmit);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        ImageView closeIcon = dialogView.findViewById(R.id.closeIcon);

        // Set the custom layout to the AlertDialog builder
        builder.setView(dialogView);

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        // Set the onClickListener for the close icon
        closeIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        // Set the onClickListener for the submit button
        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mob = editTextPopup.getText().toString();
                if (mob.length() == 10) {
                    if (MobileNoValidation.hasSameNumber(mob)) {
                        editTextPopup.setError("All digits of mobile number cannot be same.");
                    } else if (mob.equals("1234567890")) {
                        editTextPopup.setError("Please enter valid mobile number.");
                    } else if (MobileNoValidation.startsWithZeroNumber(mob)) {
                        editTextPopup.setError("Mobile number cannot start with zero.");
                    }else{
                        mobileNumber = mob;
                        alertDialog.dismiss();
                        eReceiptApi();
                    }
                }else{
                    editTextPopup.setError("Please enter valid mobile number.");
                }
                // Handle the input text
            }
        });
    }

    private void eReceiptApi() {
        progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();
        String payLoad = Helper.BitMapToString(customerTxnChargeSlip);
        String url = "uploadChargeslip";
        String tranChannel = txnType;
        String rrn = "";
        if(txnType.equals("FASTAG")){
            tranChannel = "LPM";
        }else if(txnType.equals("BQR")){
            tranChannel = "LPM";
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("reportDate", requestDate());
            jsonObject.put("payLoad", payLoad);
            jsonObject.put("tranChannel", tranChannel);
            jsonObject.put("csType", "CC");
            jsonObject.put("rrn", cashRrn);
            jsonObject.put("authCode", authCode);
            jsonObject.put("dateTime", requestDate());
            jsonObject.put("source", "Mobile");
            jsonObject.put("channel", channelName);
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("userType", "Operator");
            jsonObject.put("userName", username);
            jsonObject.put("mobNo", mobileNumber);
            jsonObject.put("vehNo", vehicleNumber);

            Log.d("eReceiptApiRequest = ", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void apiResult(String res, String apiName) {
        if (apiName.equals("saveBillerTxn")) {
            try {
                if (res.equals("Server Time Out")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                            MessagesDialog.showDialog(SuccessActivity.this, "Server Time Out", 0,null, null);
                        }
                    });
                } else {
                    Log.d("notifResponse", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    if(payLoad.has("respCode")){
                        resCode =  payLoad.getString("respCode");
                    }
                    if (payLoad.getString("respCode").equals("200")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                if(progress.isShowing()){
//                                    progress.dismiss();
//                                }
                                Log.d("notifResCode", resCode);
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                if(progress.isShowing()){
//                                    progress.dismiss();
//                                }
                                Log.d("notifResCodeEx", resCode);

                                if (requestJsonObject != null) {
                                    try {
                                        createRequestFile(context, requestJsonObject.toString());
                                        createLogFile(context, "Request\n" + requestJsonObject.toString() + "\n\nResponse\n" + new JSONObject(res).toString());
                                    } catch (Exception e) {
                                        Log.d("logFileException", e.toString());
                                    }
                                }
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.d("notificationRespException", e.toString());
            }
        }else if (apiName.equals("uploadChargeslip")) {
            try {
                if (res.equals("Server Time Out")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                            MessagesDialog.showDialog(SuccessActivity.this, "Server Time Out", 0,null, null);
                        }
                    });
                } else {
                    Log.d("eReceiptResponse",res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    if (payLoad.getString("respCode").equals("200")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(progress.isShowing()){
                                    progress.dismiss();
                                }
                                Helper helper = new Helper();
                                helper.showToastMessage((Activity) context,"E-Receipt will be delivered to the given mobile number.");
                                Intent intent = new Intent(SuccessActivity.this, SideBarActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                String jsonRes = "Response : "+ res;
//                                createLogAndRequestFile(context, jsonRes);
                                if(progress.isShowing()){
                                    progress.dismiss();
                                }
                                Helper helper = new Helper();
                                helper.showToastMessage((Activity) context,"E-Receipt not delivered. Redirecting to Home Page.");
                                Intent intent = new Intent(SuccessActivity.this, SideBarActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }
                }
            }catch (Exception e){
                Log.d("notificationRespException", e.toString());
            }
        }
    }

    public String createTxnId(){
        // This method is use for create both Ble txn id and Chargeslip txnid
        Log.d("skptestid", String.valueOf(Helper.txnListPostionSelected));
        Object myobj = txnArrayList.get(txnListPostionSelected);
        Gson gson = new Gson();
        String json = gson.toJson(myobj);
        try {
            JSONObject jsonObject = new JSONObject(json);
            String date = HexToDecimal.convert(jsonObject.getString("Day"));
            String month = HexToDecimal.convert(jsonObject.getString("Month"));
            String year = HexToDecimal.convert(jsonObject.getString("Year"));
//            String hour = HexToDecimal.convert(jsonObject.getString("Hour"));
//            String min = HexToDecimal.convert(jsonObject.getString("Minute"));
//            String second = HexToDecimal.convert(jsonObject.getString("Second"));
            String uniqueId = HexToDecimal.convert(jsonObject.getString("UniqueID"));

            Log.d("uniqueIdHex",uniqueId);
            Log.d("uniqueIdWithoutHex",jsonObject.getString("UniqueID"));

            if (date.length() == 1) {
                date = "0" + date;
            }
            if (month.length() == 1) {
                month = "0" + month;
            }
            if (year.length() == 2) {
                year = "20" + year;
            }
//            if (hour.length() == 1) {
//                hour = "0" + hour;
//            }
//            if (min.length() == 1) {
//                min = "0" + min;
//            }
//            if (second.length() == 1) {
//                second = "0" + second;
//            }

            Log.d("uniqueId", uniqueId);
            bleTxnId = CreateTransactionId.create(year, month, date, uniqueId);
            Log.d("bleTxnId",bleTxnId);
            txnId = CreateTransactionId.chargeslipTxnId(year, month, date, uniqueId);
            fileWrite(context,todayDate+".txt","Txn ID :",txnId);
            return txnId;
        }catch (Exception e){
            Log.d("Exception","create txn exception");
        }
        return "";
    }

    @Override
    public void merchantPrintNo() {
        if(progress.isShowing()){
            progress.dismiss();
        }
        chargeslipHelper.customerDialog(SuccessActivity.this,customerTxnChargeSlip);
        chargeslipHelper.setCallback((PrintResponseCallBack) context);
        Log.d("printResult","merchantPrintNo");
    }

    @Override
    public void merchantPrintYes() {
        Log.d("printResult","merchantPrintYes");
        if(progress.isShowing()){
            progress.dismiss();
        }
        chargeslipHelper.customerDialog(SuccessActivity.this,customerTxnChargeSlip);
        chargeslipHelper.setCallback((PrintResponseCallBack) context);
    }

    @Override
    public void customerPrintNo() {
        Log.d("printResult","customerPrintNo");
        if(txnType.equals("ALP")){
            chargeslipHelper.fuelBillDialog(SuccessActivity.this, fuelBillChargeSlip, fuelType);
            chargeslipHelper.setCallback((PrintResponseCallBack) context);
        }else{
            pumpHomePage();
        }

    }

    @Override
    public void customerPrintYes() {
        if(txnType.equals("ALP")){
            chargeslipHelper.fuelBillDialog(SuccessActivity.this, fuelBillChargeSlip, fuelType);
            chargeslipHelper.setCallback((PrintResponseCallBack) context);
        }else{
            pumpHomePage();
        }
    }

    @Override
    public void fuelBillPrintNo() {
        if(progress.isShowing()){
            progress.dismiss();
        }
        pumpHomePage();
    }

    @Override
    public void fuelBillPrintYes() {
        if(progress.isShowing()){
            progress.dismiss();
        }
        pumpHomePage();
    }

    @Override
    public void merchantPrintError(String errorResponse) {
        if(progress.isShowing()){
            progress.dismiss();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, errorResponse, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void customerPrintError(String errorResponse) {
        if(progress.isShowing()){
            progress.dismiss();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, errorResponse, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkBluetoothPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, proceed with the scan
            Log.d("bluetoothPermission2","Granted Success Activity");
            connectBluetooth();
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // If request is cancelled, the result arrays are empty
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with the scan
                Log.d("bluetoothPermission","Granted Success Activity");
                connectBluetooth();
            } else {
                // Permission denied, show a message to the user
                MessagesDialog.showDialog(SuccessActivity.this, "Permission required to perform Bluetooth scan", 0,null, null);

                //Toast.makeText(this, "Permission required to perform Bluetooth scan", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                File bpclFolder = new File(Environment.getExternalStorageDirectory(), "BPCL Log Data");
                if (!bpclFolder.exists()) {
                    bpclFolder.mkdirs();
                }
            } else {
                askPermissionForStorage();
            }
        }
    }

    public void connectBluetooth() {
        Log.d("SuccessActivity","connectBluetooth method Called");
        bleDeviceHelper.initiateBleScan();
    }

    public void pumpHomePage() {
        if(isTxnOnline.equals("no")){
            fccAcknowledgement = "NO";
            if(!txnNotificationSendCount){
                txnNotificationSendCount = true;
                txnNotificationSend();
            }
            Intent intent = new Intent(this, SideBarActivity.class);
            startActivity(intent);
            finish();
        }else{
            isDialogVisible = false;
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                MessagesDialog.showDialog(SuccessActivity.this, "Device does not support Bluetooth.", 0,null, null);

                //Toast.makeText(this, "Device does not support Bluetooth.", Toast.LENGTH_SHORT).show();
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

            progress = new ProgressDialog(SuccessActivity.this);
            progress.setTitle("Loading");
            progress.setMessage("Wait while loading...");
            progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
            progress.show();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (context != null && !((Activity) context).isFinishing()) {
                        if (progress != null && progress.isShowing()) {
                            progress.dismiss();
                            Toast.makeText(context, "Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }, 15000);
        }
    }

    private void askPermissionForStorage() {
        ActivityCompat.requestPermissions((Activity) context, new String[]{
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }, 100);
    }

    @Override
    public void onDestroy() {
        try {
            super.onDestroy();
            Log.d("SuccessActivity", "onDestroy method called");
            fileWrite(context, todayDate + ".txt", "SuccessAct:method Called = ", "onDestroy");
            handler.removeCallbacksAndMessages(null);
        } catch (Exception e) {
            Log.e("SuccessActivity", "Error in onDestroy: " + e.getMessage(), e);
            fileWrite(context, todayDate + ".txt", "onDestroy Exception", e.toString());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Log.d("skpResultSuccess", "Bluetooth on ho gya hai");
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
}
