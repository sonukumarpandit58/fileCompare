package com.ims.bpcluat.alp.alpOperations.cardManagement.balanceEnquiry;

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
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.AlpReceiptHelper;
import com.ims.bpcluat.databinding.ActivityBalanceEnquiryRecieptBinding;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.interfaces.PrintResponseCallBack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BalanceEnquiryReciept extends AppCompatActivity implements PrintResponseCallBack, ApiHelper.NetworkingApiCallBack  {
    ActivityBalanceEnquiryRecieptBinding binding;
    Context context;
    ApiHelper api;
    ProgressDialog progress;
    Bitmap merchantTxnChargeSlip, customerTxnChargeSlip,fuelBillSlip;
    AlpReceiptHelper alpReceiptHelper = new AlpReceiptHelper();
    Date date = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
    String todayDate = formatter.format(date);
    String roMobileNo = "";
    String myDeviceInfo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBalanceEnquiryRecieptBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
        context = this;
        api = new ApiHelper();

        myDeviceInfo = Build.MODEL;
        Log.d("myDeviceInfo", myDeviceInfo);
        if (myDeviceInfo.equals("A50")) {
            binding.printBtn.setVisibility(View.GONE);
        }

        progress = new ProgressDialog(BalanceEnquiryReciept.this);
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
        String argData = getIntent().getStringExtra("payload");
        Log.d("balanceEnquiryData", argData);

        if (argData != null) {
            try {
                JSONObject payLoad = new JSONObject(argData);
                JSONArray outputArray = payLoad.getJSONArray("output");
                JSONObject outputObject = outputArray.getJSONObject(0);
//                roMobileNo = outputObject.getString("roMobileNo");

                customerTxnChargeSlip= alpReceiptHelper.balanceInquirySlip(BalanceEnquiryReciept.this ,payLoad,"CUSTOMER COPY");
                binding.balanceEnquirySlip.setImageBitmap(customerTxnChargeSlip);

                binding.printBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        progress = new ProgressDialog(BalanceEnquiryReciept.this);
                        progress.setTitle("Loading");
                        progress.setMessage("Wait while loading...");
                        progress.setCancelable(false);
                        progress.show();
                        alpReceiptHelper.printReceipt(BalanceEnquiryReciept.this, customerTxnChargeSlip, "CUSTOMER COPY");

//                        alpReceiptHelper.customerDialog(BalanceEnquiryReciept.this, customerTxnChargeSlip);
                        alpReceiptHelper.setCallback((PrintResponseCallBack) context);
                    }
                });

                binding.homeBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        balanceEnquiryHomePage(BalanceEnquiryReciept.this);
                    }
                });

                shiftNotificationSend(payLoad);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void askPermissionForStorage() {
        ActivityCompat.requestPermissions((Activity) context, new String[]{
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }, 100);
    }

    public void shiftNotificationSend(JSONObject jsonObjectData) {
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
            paramListArray.put(createJsonObject(roMobileNo, "Customer Mobile"));
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

    @Override
    public void apiResult(String res, String apiName) {
        if (apiName.equals("saveBillerTxn")) {
            try {
                if (res.equals("Server Time Out")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(BalanceEnquiryReciept.this, "Server Time Out", 0,null, null);
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

    public static void balanceEnquiryHomePage(final Activity mActivity) {
        Intent intent = new Intent(mActivity, SideBarActivity.class);
        intent.putExtra("redirect", "CardManagementFragment");
        mActivity.startActivity(intent);
        mActivity.finish();
    }

    @Override
    public void merchantPrintNo() {
        progress.dismiss();
        balanceEnquiryHomePage(this);
        Log.d("printResult", "merchantPrintNo");
    }

    @Override
    public void merchantPrintYes() {
        Log.d("printResult","merchantPrintYes");
        progress.dismiss();
        alpReceiptHelper.customerDialog(BalanceEnquiryReciept.this, customerTxnChargeSlip);
        alpReceiptHelper.setCallback((PrintResponseCallBack) context);
    }

    @Override
    public void customerPrintNo() {
        Log.d("printResult","customerPrintNo");
        progress.dismiss();
        balanceEnquiryHomePage(this);
    }

    @Override
    public void customerPrintYes() {
        Log.d("printResult","customerPrintNo");
        progress.dismiss();
        balanceEnquiryHomePage(this);
    }

    @Override
    public void fuelBillPrintNo() {
        Log.d("printResult","fuelBillPrintNo");
        progress.dismiss();
        balanceEnquiryHomePage(this);
    }

    @Override
    public void fuelBillPrintYes() {
        Log.d("printResult","fuelBillPrintYes");
        progress.dismiss();
        balanceEnquiryHomePage(this);
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
}