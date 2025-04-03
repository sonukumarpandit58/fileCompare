package com.ims.bpcluat.ufill.void_transaction;

import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.coverage;
import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.operatorFirstName;
import static com.ims.bpcluat.Helper.operatorLastName;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.username;
import static com.ims.bpcluat.Helper.version;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.databinding.ActivityVoidReceiptBinding;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.interfaces.PrintResponseCallBack;
import com.ims.bpcluat.model.VoidTransactionModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class VoidReceipt extends AppCompatActivity implements PrintResponseCallBack, ApiHelper.NetworkingApiCallBack {

    ActivityVoidReceiptBinding binding;
    Context context;
    ProgressDialog progress;
    private VoidTransactionModel voidTransactionModel;
    Bitmap merchantTxnChargeSlip;
    VoidReceiptHelper voidReceiptHelper = new VoidReceiptHelper();
    String mobileNum;
    Date date = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
    String todayDate = formatter.format(date);
    ApiHelper api;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVoidReceiptBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        context = this;
        api = new ApiHelper();
        progress = new ProgressDialog(VoidReceipt.this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            askPermissionForStorage();
        } else {
//            imageSet();
//            readSettingFile();
//            setMyIpAddressPort();
//            videoPlay();
        }

        getArgumentsData();

    }

    public void getArgumentsData() {
        String voidResponseData = getIntent().getStringExtra("voidResponse");
        mobileNum = getIntent().getStringExtra("mobileNum");
        Log.d("voidRettrttttt", mobileNum);
        //  Log.d("voidRettrttttt", mobileNum);

        Log.d("voidResponseDataReceived", voidResponseData != null ? voidResponseData : "No Data");
        //Log.d("voidResponseDataReceived", voidResponseData != null ? voidResponseData : "No Data");
        if (voidResponseData != null) {
            Log.d("voidResponseDataReceived#", voidResponseData);
            //  Log.d("voidResponseDataReceived#", voidResponseData);
            try {
                JSONObject payLoad = new JSONObject(voidResponseData);

                String channel = payLoad.getString("channel");
                String reqDate = payLoad.getString("reqDate");
                String reqTime = payLoad.getString("reqTime");
                String response = payLoad.getString("response");
                String respCode = payLoad.getString("respCode");
                String resDate = payLoad.getString("resDate");
                String resTime = payLoad.getString("resTime");
                String userName = payLoad.getString("userName");
                String mid = payLoad.getString("mid");
                String tid = payLoad.getString("tid");
                String client = payLoad.getString("client");
                String respDesc = payLoad.getString("respDesc");
                String id = payLoad.getString("id");
                String txnId = payLoad.getString("txnId");
                String roCode = payLoad.getString("roCode");
                String dateTime = payLoad.getString("dateTime");
                String txnType = payLoad.getString("txnType");
                String instId = payLoad.getString("instId");

                merchantTxnChargeSlip= voidReceiptHelper.voidSlip(VoidReceipt.this, payLoad,"MERCHANT COPY", mobileNum);
                binding.chargeslip.setImageBitmap(merchantTxnChargeSlip);

                binding.printBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        progress = new ProgressDialog(VoidReceipt.this);
                        progress.setTitle("Loading");
                        progress.setMessage("Wait while loading...");
                        progress.setCancelable(false);
                        progress.show();
                        voidReceiptHelper.printReceipt(VoidReceipt.this,merchantTxnChargeSlip,"MERCHANT COPY");
                        voidReceiptHelper.setCallback((PrintResponseCallBack) context);
                    }
                });

                binding.homeBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        merchantPrintNo();
                    }
                });

                voidNotificationSend(payLoad);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void merchantPrintNo() {
        progress.dismiss();
        Log.d("printResult","merchantPrintNo");
        Intent intent = new Intent(VoidReceipt.this, SideBarActivity.class);
        intent.putExtra("redirect", "VoidFragment");
        startActivity(intent);
        finish();
    }

    @Override
    public void merchantPrintYes() {
        Log.d("printResult","merchantPrintYes");
        progress.dismiss();
        voidReceiptHelper.merchantDialog(VoidReceipt.this,merchantTxnChargeSlip);
        voidReceiptHelper.setCallback((PrintResponseCallBack) context);
    }



    @Override
    public void customerPrintNo() {

    }

    @Override
    public void customerPrintYes() {

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

    }

    public void voidNotificationSend(JSONObject jsonObjectData) {
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

            billerTranListObject.put("inv_code", "");
            billerTranListObject.put("trans_type", "PURCHASE");
            billerTranListObject.put("trans_status", "SUCCESS");
            billerTranListObject.put("tran_amt", jsonObjectData.getString("amt"));
            billerTranListObject.put("tran_date",  jsonObjectData.getString("resDate"));
            billerTranListObject.put("tran_time",  jsonObjectData.getString("resTime"));


            billerTranListObject.put("rrn", "");

            billerTranListObject.put("card_first", "");
            billerTranListObject.put("card_last", "");

            billerTranListObject.put("ft_number", jsonObjectData.getString("txnId"));
            billerTranListObject.put("session_id", "");

            billerTranListObject.put("cust_id", username);
            billerTranListObject.put("pay_method", "void");
            billerTranListObject.put("authAmt", jsonObjectData.getString("amt"));
            billerTranListObject.put("refundAmt", "0");
            billerTranListObject.put("balanceAmt",  jsonObjectData.getString("amt"));
            billerTranListObject.put("field1", "Online");
            billerTranListObject.put("field2", "");
            billerTranListObject.put("field3", "");
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
            paramListArray.put(createJsonObject(mobileNum, "Customer Mobile"));
            paramListArray.put(createJsonObject("", "Vehicle ID"));
            paramListArray.put(createJsonObject("", "PUMP_NO"));
            paramListArray.put(createJsonObject("", "NOZZLE"));
            paramListArray.put(createJsonObject("", "QUANTITY"));
            paramListArray.put(createJsonObject("", "PROD_NAME"));
            paramListArray.put(createJsonObject(version, "VERSION"));
            paramListArray.put(createJsonObject("", "UNIT_PRICE"));
            paramListArray.put(createJsonObject("0.00", "CUSTOMER_DISC"));
            paramListArray.put(createJsonObject("", "ORDER_ID"));
            paramListArray.put(createJsonObject("", "FCC TIMESTAMP"));
            paramListArray.put(createJsonObject("", "MERCH NAME"));
            paramListArray.put(createJsonObject("", "Vehicle_Type"));


            jsonObject.put("billerTranList", billerTranListArray);
            Log.d("notificationRequest = ", String.valueOf(jsonObject));
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


    private void askPermissionForStorage() {
        ActivityCompat.requestPermissions((Activity) context, new String[]{
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }, 100);
    }

    @Override
    public void apiResult(String res, String apiName) {
        if (apiName.equals("saveBillerTxn")) {
            try {
                if (res.equals("Server Time Out")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Server Time Out", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Log.d("notificationResponse",res);
                }
            }catch (Exception e){
                Log.d("notificationRespException", e.toString());
            }
        }
    }
}
