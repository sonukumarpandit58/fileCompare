package com.ims.bpcluat.ufill;

import static com.ims.bpcluat.Helper.address1;
import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.city;
import static com.ims.bpcluat.Helper.coverage;
import static com.ims.bpcluat.Helper.createJsonObject;
import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.operatorFirstName;
import static com.ims.bpcluat.Helper.operatorLastName;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.roName;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.Helper.username;
import static com.ims.bpcluat.Helper.version;
import static com.ims.bpcluat.helper.BleDeviceHelper.clearUfillVariables;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.ims.bpcluat.Helper;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.databinding.ActivityUfillSuccessBinding;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.helper.BleDeviceHelper;
import com.ims.bpcluat.helper.ChargeslipHelper;
import com.ims.bpcluat.interfaces.PrintResponseCallBack;
import com.ims.bpcluat.model.UfillModel;
import com.ims.bpcluat.utils.DateTimeInUTC;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UfillSuccessActivity extends AppCompatActivity implements PrintResponseCallBack,ApiHelper.NetworkingApiCallBack{

    ActivityUfillSuccessBinding binding;
    Context context;
    ChargeslipHelper chargeslipHelper = new ChargeslipHelper();
    Bitmap merchantTxnChargeSlip,customerTxnChargeSlip;
    private UfillModel ufillModel;
    String myDeviceInfo;
    String chargselipDate = "", chargselipTime = "",product = "",chargeslipTxnId = "",pumpNo = "",nozzleNo = "";
    String mobileNumber = "",vehicleNumber = "",vehicleType = "",amount = "", prebookTxn = "", prebookTxnTime = "";
    String qty = "", notificationDate = "", notificationTime = "", txnType = "",localMPDID = "";
    String fccTimeStamp = "", productPrice = "",txnStartTime = "", presetType = "",productId = "",voucherAmount = "",volume ="";
    String id = "",rrn = "",authCode = "";
    ApiHelper api;
    ProgressDialog progress;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUfillSuccessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        context = this;
        api = new ApiHelper();

        clearUfillVariables();
        progress = new ProgressDialog(UfillSuccessActivity.this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();

        // Simulate a short processing delay, or remove this if not needed
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (progress != null && progress.isShowing()) {
                progress.dismiss();
            }

            // Now update the UI or show success details
            // Example: populate views with ufillModel data
        }, 500); // 500ms just to allow a smooth UX transition

        myDeviceInfo = Build.MODEL;
        Log.d("myDeviceInfo", myDeviceInfo);
        if (myDeviceInfo.equals("A50")) {
            binding.printBtn.setVisibility(View.GONE);
            binding.nextCopyBtn.setVisibility(View.VISIBLE);
            binding.nextCopyBtn.setText("Customer Copy");
        }

        ufillModel = getIntent().getParcelableExtra("ufillModel");
        if (ufillModel != null) {
            pumpNo = ufillModel.getPumpNo();
            nozzleNo = ufillModel.getNozzleNo();
            //amount = ufillModel.getVoucherAmt();
            amount = ufillModel.getFuelledAmt();
            chargselipDate = ufillModel.getTxnChargselipDate();
            chargselipTime = ufillModel.getTxnChargeslipTime();
            notificationDate = ufillModel.getTxnNotificationDate();
            notificationTime = ufillModel.getTxnNotificationTime();
            chargeslipTxnId = ufillModel.getTxnId();
            product = ufillModel.getProduct();
            qty = ufillModel.getQty();
            txnType = ufillModel.getTxnType();
            fccTimeStamp = ufillModel.getFccTimeStamp();
            productPrice = ufillModel.getProductPrice();
            txnStartTime = ufillModel.getTxnStartTime();
            presetType = ufillModel.getPresetType();
            productId = ufillModel.getProductId();
            voucherAmount = ufillModel.getVoucherAmt();
            volume = ufillModel.getVolume();
            localMPDID = ufillModel.getLocalMpdId();
            prebookTxn = ufillModel.getPrebookTxn();
            prebookTxnTime = ufillModel.getPrebookTxnTime();
            id = ufillModel.getId();
            rrn = ufillModel.getRrn();
            authCode = ufillModel.getAuthCode();
            mobileNumber = ufillModel.getMobileNumber();
            vehicleNumber = ufillModel.getVehicleNumber();
            if(TextUtils.isEmpty(localMPDID)){
                localMPDID = "0";
            }
        }

        txnNotificationSend();

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("attendentName", Helper.operatorFirstName + " " + Helper.operatorLastName);
            jsonObject.put("agencyName", roName);
            jsonObject.put("address", address1);
            jsonObject.put("city", city);
            jsonObject.put("date", chargselipDate);
            jsonObject.put("time", chargselipTime);
            jsonObject.put("mid", Helper.mid);
            jsonObject.put("tid", Helper.tid);
            jsonObject.put("txnType", txnType);
            jsonObject.put("product", product);
            jsonObject.put("txnId", chargeslipTxnId);
            jsonObject.put("unitPrice", productPrice);
            jsonObject.put("quantity", volume);
            jsonObject.put("pumpNo",pumpNo );
            jsonObject.put("nozzleNo", nozzleNo);
            jsonObject.put("mobileNumber", mobileNumber);
            jsonObject.put("vehicleNumber", vehicleNumber);
            jsonObject.put("vehicleType", vehicleType);
            jsonObject.put("totalSale", amount);
            jsonObject.put("discount", "0.00");
            jsonObject.put("netAmount", amount);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        merchantTxnChargeSlip= chargeslipHelper.ufillChargeslip(UfillSuccessActivity.this, jsonObject,"MERCHANT COPY");
        customerTxnChargeSlip = chargeslipHelper.ufillChargeslip(UfillSuccessActivity.this, jsonObject,"CUSTOMER COPY");
        binding.chargeslip.setImageBitmap(merchantTxnChargeSlip);

        binding.printBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progress.show();
                chargeslipHelper.merchantDialog(UfillSuccessActivity.this,merchantTxnChargeSlip);
                chargeslipHelper.setCallback((PrintResponseCallBack) context);
            }
        });

        binding.nextCopyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(binding.nextCopyBtn.getText().toString().equals("Customer Copy")){
                    binding.chargeslip.setImageBitmap(customerTxnChargeSlip);
                    binding.nextCopyBtn.setText("Customer Copy");
                    binding.nextCopyBtn.setVisibility(View.GONE);
                }

            }
        });

        binding.homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ufillHomePage();
            }
        });
    }

    @Override
    public void merchantPrintNo() {
        progress.dismiss();
        ufillHomePage();
        Log.d("printResult","merchantPrintNo");
    }

    @Override
    public void merchantPrintYes() {
        Log.d("printResult","merchantPrintYes");
        progress.dismiss();
        chargeslipHelper.customerDialog(UfillSuccessActivity.this,customerTxnChargeSlip);
        chargeslipHelper.setCallback((PrintResponseCallBack) context);
    }

    @Override
    public void customerPrintNo() {
        ufillHomePage();
        Log.d("printResult","customerPrintNo");
    }

    @Override
    public void customerPrintYes() {
        ufillHomePage();
        Log.d("printResult","customerPrintYes");
    }

    @Override
    public void fuelBillPrintNo() {

    }

    @Override
    public void fuelBillPrintYes() {

    }

    @Override
    public void merchantPrintError(String errorResponse) {
        progress.dismiss();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, errorResponse, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void customerPrintError(String errorResponse) {
        progress.dismiss();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, errorResponse, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void ufillHomePage(){
        Intent intent = new Intent(getApplicationContext(), SideBarActivity.class);
        startActivity(intent);
        finish();
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
            billerTranListObject.put("auth_code", "");
            billerTranListObject.put("inv_code", authCode);
            billerTranListObject.put("trans_type", "PURCHASE");
            billerTranListObject.put("trans_status", "SUCCESS");
            billerTranListObject.put("tran_amt", amount);
            billerTranListObject.put("tran_date", notificationDate);
            billerTranListObject.put("tran_time", notificationTime);

            billerTranListObject.put("rrn", rrn);
            billerTranListObject.put("card_first", "");
            billerTranListObject.put("card_last", "");
            billerTranListObject.put("ft_number", chargeslipTxnId);
            billerTranListObject.put("session_id", "");

            billerTranListObject.put("cust_id", username);
            billerTranListObject.put("pay_method", txnType);
            billerTranListObject.put("authAmt", amount);
            billerTranListObject.put("refundAmt", "0");
            billerTranListObject.put("balanceAmt", amount);
            billerTranListObject.put("field1", "Online");
            billerTranListObject.put("field2", "");
            billerTranListObject.put("field3", id);
            billerTranListObject.put("field4", "");
            billerTranListObject.put("field5", "");
            billerTranListObject.put("field6", "");
            billerTranListObject.put("field7", "");
            billerTranListObject.put("field8", "");
            billerTranListObject.put("field9", "");
            billerTranListObject.put("field10", "");
            billerTranListObject.put("field11", "");
            billerTranListObject.put("field12", "");
            billerTranListObject.put("field13", "");
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
            paramListArray.put(createJsonObject(volume, "QUANTITY"));
            paramListArray.put(createJsonObject(product, "PROD_NAME"));
            paramListArray.put(createJsonObject(version, "VERSION"));
            paramListArray.put(createJsonObject(productPrice, "UNIT_PRICE"));
            paramListArray.put(createJsonObject("0.00", "CUSTOMER_DISC"));
            paramListArray.put(createJsonObject("", "ORDER_ID"));
            paramListArray.put(createJsonObject(fccTimeStamp, "FCC TIMESTAMP"));
            paramListArray.put(createJsonObject(roName, "MERCH NAME"));
            paramListArray.put(createJsonObject(vehicleType, "Vehicle_Type"));

            paramListArray.put(createJsonObject(chargeslipTxnId, "globalTxnID"));
            paramListArray.put(createJsonObject(chargeslipTxnId, "FCCTxnID"));
            paramListArray.put(createJsonObject(productId, "localProductID"));
            paramListArray.put(createJsonObject(volume, "quantityLitres"));
            paramListArray.put(createJsonObject(amount, "amountRs"));
            paramListArray.put(createJsonObject(amount, "rspRs"));
            paramListArray.put(createJsonObject("", "discountID"));
            paramListArray.put(createJsonObject("0.00", "discountAmountRs"));
            paramListArray.put(createJsonObject(amount, "netAmountRs"));
            paramListArray.put(createJsonObject(String.valueOf(Integer.parseInt(pumpNo)), "localBayID"));
            paramListArray.put(createJsonObject(String.valueOf(Integer.parseInt(nozzleNo)), "localNozzleID"));
            paramListArray.put(createJsonObject(localMPDID, "localMPD_ID"));
            paramListArray.put(createJsonObject("110", "txnType"));
            paramListArray.put(createJsonObject("Amount", "presetType"));
            paramListArray.put(createJsonObject("", "presetQuantity"));
            paramListArray.put(createJsonObject(voucherAmount, "presetAmount"));
            paramListArray.put(createJsonObject(DateTimeInUTC.convertToUTCFormat(fccTimeStamp), "auditInfoReceivedAtAPOS"));
            paramListArray.put(createJsonObject(DateTimeInUTC.convertCurrentDateWithTime(txnStartTime), "txnStartTime"));
            paramListArray.put(createJsonObject(DateTimeInUTC.convertCurrentDateWithTime(requestTime()), "txnEndTime"));
            paramListArray.put(createJsonObject(voucherAmount, "prebookAmount"));
            paramListArray.put(createJsonObject(prebookTxn, "prebookTxn"));
            paramListArray.put(createJsonObject(DateTimeInUTC.convertToUTCFormat(prebookTxnTime), "prebookTxnTime"));

            jsonObject.put("billerTranList", billerTranListArray);
            Log.d("notificationRequest = ", String.valueOf(jsonObject));
            fileWrite(context,todayDate+".txt","Ocean notification request :", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (Exception e) {
            Log.d("notificationException", e.toString());
        }
    }

    @Override
    public void apiResult(String res, String apiName) {

    }
}