package com.ims.bpcluat.nfr;

import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.city;
import static com.ims.bpcluat.Helper.coverage;
import static com.ims.bpcluat.Helper.dealerContactNumber;
import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.mobileNumberMasking;
import static com.ims.bpcluat.Helper.nfrHomePage;
import static com.ims.bpcluat.Helper.operatorFirstName;
import static com.ims.bpcluat.Helper.operatorLastName;
import static com.ims.bpcluat.Helper.padWithZeroes;
import static com.ims.bpcluat.Helper.replaceCommaWithHyphen;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.roName;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.serialNumber;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.username;
import static com.ims.bpcluat.Helper.version;
import static com.ims.bpcluat.ReadWriteHelper.createLogFile;
import static com.ims.bpcluat.ReadWriteHelper.createRequestFile;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.databinding.ActivityNfrSuccessBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.helper.ChargeslipHelper;
import com.ims.bpcluat.interfaces.PrintResponseCallBack;
import com.ims.bpcluat.model.NfrModel;
import com.ims.bpcluat.validation.MobileNoValidation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NfrSuccessActivity extends AppCompatActivity implements PrintResponseCallBack, ApiHelper.NetworkingApiCallBack {

    ActivityNfrSuccessBinding binding;
    private NfrModel nfrModel;
    ChargeslipHelper chargeslipHelper = new ChargeslipHelper();
    String myDeviceInfo;
    ApiHelper api;
    Bitmap merchantTxnChargeSlip, customerTxnChargeSlip,fuelBillChargeSlip;
    Context context;
    ProgressDialog progress;
    String product, mobileNumber = "", vehicleNumber = "", vehicleType = "", txnType, notificationDate, notificationTime;
    String chargselipDate, chargselipTime, authBank = "", authCode = "", rrn = "";
    String atc = "", cardType = "", posEntryMode = "", terminalInvoiceNo = "", batchNo = "", aid = "", tsi = "";
    String tvr = "", transactionCertificate = "", cardPaymentVersionNo = "", authTid = "", cardNo = "", txnId = "";
    String totalAmt = "", totalQty, nfrProductName, nfrProductQty, nfrProductAmt;
    String cardFirst = "", cardLast = "",field3 = "",field7 = "",field9 = "",field13 = "",cardTxnCustomerName = "";;
    String fuelType = "POS SLIP";
    String resCode = "";
    private JSONObject requestJsonObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNfrSuccessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        context = this;

        SharedPreferences shared = getSharedPreferences("nfrSharedPreferencesData", MODE_PRIVATE);
        totalAmt = (shared.getString("totalAmount", ""));
        totalQty = (shared.getString("totalQty", ""));
        nfrProductName = (shared.getString("nfrProductName", ""));
        nfrProductQty = (shared.getString("nfrProductQty", ""));
        nfrProductAmt = (shared.getString("nfrProductAmt", ""));

        nfrModel = getIntent().getParcelableExtra("nfrModel");
        if (nfrModel != null) {
            txnId = nfrModel.getTxnId();
            product = "NFR";
            mobileNumber = nfrModel.getMobileNumber();
            if (!mobileNumber.isEmpty()) {
                mobileNumber = mobileNumberMasking(mobileNumber);
            }
            vehicleNumber = nfrModel.getVehicleNumber();
            vehicleType = nfrModel.getVehicleType();
            txnType = nfrModel.getTxnType();
            if(txnType.equals("SALES")){
                txnType = "ALP";
                field3 = nfrModel.getField3();
                field7 = nfrModel.getField7();
                field9 = nfrModel.getField9();
                field13 = nfrModel.getField13();
            }

            notificationDate = nfrModel.getTxnNotificationDate();
            notificationTime = nfrModel.getTxnNotificationTime();
            chargselipDate = nfrModel.getTxnChargselipDate();
            chargselipTime = nfrModel.getTxnChargeslipTime();
            if (txnType.equals("CARD")) {
                authBank = nfrModel.getAuthBank();
                cardType = nfrModel.getCardType();
                posEntryMode = nfrModel.getPosEntryMode();
                terminalInvoiceNo = nfrModel.getTerminalInvoiceNo();
                batchNo = nfrModel.getBatchNo();
                aid = nfrModel.getAid();
                tsi = nfrModel.getTsi();
                tvr = nfrModel.getTvr();
                transactionCertificate = nfrModel.getTransactionCertificate();
                cardPaymentVersionNo = nfrModel.getCardPaymentVersionNo();
                authTid = nfrModel.getAuthTid();
                atc = nfrModel.getAtc();
                cardNo = nfrModel.getCardNo();
                cardFirst = nfrModel.getCardFirst();
                cardLast = nfrModel.getCardLast();
                cardTxnCustomerName = nfrModel.getCardTxnCustomerName();
            }
            authCode = nfrModel.getAuthCode();
            rrn = nfrModel.getRrn();
        }

        api = new ApiHelper();
        myDeviceInfo = Build.MODEL;
        Log.d("myDeviceInfo", myDeviceInfo);
        if (myDeviceInfo.equals("A50")) {
            binding.printBtn.setVisibility(View.GONE);
            binding.nextCopyBtn.setVisibility(View.VISIBLE);
            binding.nextCopyBtn.setText("Customer Copy");
            txnNotificationSend();
        } else {
           // binding.ereceiptBtn.setVisibility(View.GONE);
        }

//        binding.ereceiptBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (mobileNumber.isEmpty()) {
//                    showPopup();
//                } else {
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

        binding.homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!myDeviceInfo.equals("A50")) {
                    txnNotificationSend();
                }
                nfrHomePage((Activity) context);
            }
        });

        JSONObject jsonObject = new JSONObject();
        JSONObject alpSaleObject = new JSONObject();
        try {
            if(txnType.equals("ALP")) {
                jsonObject.put("agencyName", nfrModel.getROName());
                jsonObject.put("city", nfrModel.getRoCity());
                jsonObject.put("dealerContactNo",nfrModel.getRoMobileNo());
                jsonObject.put("type",nfrModel.getAlpType());
            }else {
                jsonObject.put("agencyName", roName);
                jsonObject.put("city", city);
                jsonObject.put("dealerContactNo",dealerContactNumber);

            }
//            jsonObject.put("address", address1);

            jsonObject.put("date", chargselipDate);
            jsonObject.put("time", chargselipTime);
            jsonObject.put("bayNo", "");
            jsonObject.put("nozzleNo", "");
            jsonObject.put("product", "NFR");
            jsonObject.put("payMode", txnType);
            jsonObject.put("txnId", txnId);
            jsonObject.put("attendentName", Helper.operatorFirstName + " " + Helper.operatorLastName);
            jsonObject.put("txnStart", "");
            jsonObject.put("txnEnd", "");
            jsonObject.put("rate", "");
            jsonObject.put("volume", "");
            jsonObject.put("amount", totalAmt);
            jsonObject.put("presetType", "");
            jsonObject.put("presetValue", "");
            jsonObject.put("vehicleNo", vehicleNumber);
            jsonObject.put("mobileNo", mobileNumber);
            jsonObject.put("nfrProductName", nfrProductName);
            jsonObject.put("nfrUnitPrice", nfrProductAmt);
            jsonObject.put("nfrVolume", nfrProductQty);
            jsonObject.put("nfrTotalAmount", totalAmt);
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
                alpSaleObject.put("agencyName", nfrModel.getROName());
//                alpSaleObject.put("address", address1);
                alpSaleObject.put("city", nfrModel.getRoCity());
                alpSaleObject.put("roMobileNo", nfrModel.getRoMobileNo());
                alpSaleObject.put("date", chargselipDate);
                alpSaleObject.put("time", chargselipTime);
                alpSaleObject.put("tid", nfrModel.getAlpTid());
                alpSaleObject.put("txnId", nfrModel.getAlpTxnId());
                alpSaleObject.put("slipNo", nfrModel.getAlpSlipNo());
                alpSaleObject.put("reportId", nfrModel.getAlpReportId());
                alpSaleObject.put("type", nfrModel.getAlpType());
                alpSaleObject.put("txnSource", nfrModel.getAlpTxnSource());
                alpSaleObject.put("custName", nfrModel.getAlpCustName());
                alpSaleObject.put("accountNo", nfrModel.getAlpAccNo());
                alpSaleObject.put("cardId", nfrModel.getAlpCardId());
                alpSaleObject.put("vehCard", nfrModel.getAlpVechCard());
                alpSaleObject.put("odometer", nfrModel.getAlpOdometer());
                alpSaleObject.put("wallet", nfrModel.getAlpWallet());
                alpSaleObject.put("product", nfrModel.getAlpProduct());
                alpSaleObject.put("rate","");
                alpSaleObject.put("vol", nfrProductQty);
                alpSaleObject.put("fuelAmount", nfrModel.getAlpFuelAmount());
                alpSaleObject.put("tcsAmount", nfrModel.getAlpTcsAmount());
                alpSaleObject.put("txnAmount", nfrModel.getAlpTxnAmount());
                alpSaleObject.put("pmEarn", nfrModel.getAlpPmEarn());
                alpSaleObject.put("meShare", nfrModel.getAlpMeShare());
                alpSaleObject.put("cardBalance", nfrModel.getAlpCardBalance());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            fuelBillChargeSlip = chargeslipHelper.chargeslip(NfrSuccessActivity.this, jsonObject, fuelType);
            merchantTxnChargeSlip = chargeslipHelper.alpSaleChargeslip(NfrSuccessActivity.this, alpSaleObject, "MERCHANT COPY");
            customerTxnChargeSlip = chargeslipHelper.alpSaleChargeslip(NfrSuccessActivity.this, alpSaleObject, "CUSTOMER COPY");
            binding.chargeslip.setImageBitmap(merchantTxnChargeSlip);
        }else{
            merchantTxnChargeSlip = chargeslipHelper.chargeslip(NfrSuccessActivity.this, jsonObject, "MERCHANT COPY");
            customerTxnChargeSlip = chargeslipHelper.chargeslip(NfrSuccessActivity.this, jsonObject, "CUSTOMER COPY");
            binding.chargeslip.setImageBitmap(merchantTxnChargeSlip);
        }

        binding.printBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progress = new ProgressDialog(NfrSuccessActivity.this);
                progress.setTitle("Loading");
                progress.setMessage("Wait while loading...");
                progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
                progress.show();
                chargeslipHelper.merchantDialog(NfrSuccessActivity.this, merchantTxnChargeSlip);
                chargeslipHelper.setCallback((PrintResponseCallBack) context);
            }
        });
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
            if (txnType.equals("CASH")) {
                billerTranListObject.put("auth_code", "");
            } else {
                billerTranListObject.put("auth_code", authCode);
            }
            billerTranListObject.put("inv_code", "");
            billerTranListObject.put("trans_type", "PURCHASE");
            billerTranListObject.put("trans_status", "SUCCESS");
            billerTranListObject.put("tran_amt", totalAmt);
            billerTranListObject.put("tran_date", notificationDate);
            billerTranListObject.put("tran_time", notificationTime);
            billerTranListObject.put("rrn", rrn);
            if (txnType.equals("CARD")) {
                billerTranListObject.put("card_first", cardFirst);
                billerTranListObject.put("card_last", cardLast);
            } else {
                billerTranListObject.put("card_first", "");
                billerTranListObject.put("card_last", "");
            }
            billerTranListObject.put("ft_number", txnId);
            billerTranListObject.put("session_id", "");

            billerTranListObject.put("cust_id", username);
            billerTranListObject.put("pay_method", txnType);
            billerTranListObject.put("authAmt", totalAmt);
            billerTranListObject.put("refundAmt", "0");
            billerTranListObject.put("balanceAmt", totalAmt);
            billerTranListObject.put("field1", "Offline");
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
            paramListArray.put(createJsonObject("", "PUMP_NO"));
            paramListArray.put(createJsonObject("", "NOZZLE"));
            paramListArray.put(createJsonObject(nfrProductQty, "QUANTITY"));
            paramListArray.put(createJsonObject("NFR", "PROD_NAME"));
            paramListArray.put(createJsonObject(version, "VERSION"));
            paramListArray.put(createJsonObject("", "UNIT_PRICE"));
            paramListArray.put(createJsonObject("", "CUSTOMER_DISC"));
            paramListArray.put(createJsonObject("", "ORDER_ID"));
            paramListArray.put(createJsonObject("", "FCC TIMESTAMP"));
            paramListArray.put(createJsonObject(roName, "MERCH NAME"));
            if (txnType.equals("CARD")) {
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
            paramListArray.put(createJsonObject(replaceCommaWithHyphen(nfrProductName), "NFR_Product_Name"));
            paramListArray.put(createJsonObject(replaceCommaWithHyphen(nfrProductQty), "NFR_Quantity"));
            paramListArray.put(createJsonObject(replaceCommaWithHyphen(nfrProductAmt), "NFR_AMOUNT"));
            paramListArray.put(createJsonObject(vehicleType, "Vehicle_Type"));
            paramListArray.put(createJsonObject(serialNumber, "hwSrNo"));

            jsonObject.put("billerTranList", billerTranListArray);
            Log.d("notificationRequest = ", String.valueOf(jsonObject));
//            String jsonReq = "Request : "+ String.valueOf(jsonObject);
//            createLogAndRequestFile(context, jsonReq);
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
                    } else {
                        mobileNumber = mob;
                        alertDialog.dismiss();
                        eReceiptApi();
                    }
                } else {
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
        String payLoad = Helper.BitMapToString(merchantTxnChargeSlip);
        String url = "uploadChargeslip";
        String tranChannel = txnType;
        String rrn = "";
        if (txnType.equals("FASTAG")) {
            tranChannel = "LPM";
        } else if (txnType.equals("BQR")) {
            tranChannel = "LPM";
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("reportDate", requestDate());
            jsonObject.put("payLoad", payLoad);
            jsonObject.put("tranChannel", tranChannel);
            jsonObject.put("csType", "CC");
            jsonObject.put("rrn", "");
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
//                            MessagesDialog.showDialog(NfrSuccessActivity.this, "Server Time Out", 0,null, null);
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
        } else if (apiName.equals("uploadChargeslip")) {
            try {
                if (res.equals("Server Time Out")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("notifResponseError!", res);

//                            MessagesDialog.showDialog(NfrSuccessActivity.this, "Server Time Out", 0,null, null);
                        }
                    });
                } else {
                    Log.d("eReceiptResponse", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    if (payLoad.getString("respCode").equals("200")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(progress.isShowing()){
                                    progress.dismiss();
                                }                                Helper helper = new Helper();
                                helper.showToastMessage((Activity) context, "E-Receipt will be delivered to the given mobile number.");
                                Intent intent = new Intent(NfrSuccessActivity.this, SideBarActivity.class);
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
                                helper.showToastMessage((Activity) context, "E-Receipt not delivered. Redirecting to Home Page.");
                                Intent intent = new Intent(NfrSuccessActivity.this, SideBarActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.d("notificationRespException", e.toString());
            }
        }
    }

    @Override
    public void merchantPrintNo() {
        if(progress.isShowing()){
            progress.dismiss();
        }
//        txnNotificationSend();
//        nfrHomePage(this);
//        Log.d("printResult", "merchantPrintNo");
        chargeslipHelper.customerDialog(NfrSuccessActivity.this, customerTxnChargeSlip);
        chargeslipHelper.setCallback((PrintResponseCallBack) context);
    }

    @Override
    public void merchantPrintYes() {
        Log.d("printResult", "merchantPrintYes");
        if(progress.isShowing()){
            progress.dismiss();
        }
        chargeslipHelper.customerDialog(NfrSuccessActivity.this, customerTxnChargeSlip);
        chargeslipHelper.setCallback((PrintResponseCallBack) context);
    }

    @Override
    public void customerPrintNo() {
        Log.d("printResult", "customerPrintNo");
        if(txnType.equals("ALP")){
            chargeslipHelper.fuelBillDialog(NfrSuccessActivity.this, fuelBillChargeSlip, fuelType);
            chargeslipHelper.setCallback((PrintResponseCallBack) context);
        }else{
            txnNotificationSend();
            nfrHomePage(this);
        }
    }

    @Override
    public void customerPrintYes() {
        Log.d("printResult", "customerPrintYes");
        if(txnType.equals("ALP")){
            chargeslipHelper.fuelBillDialog(NfrSuccessActivity.this, fuelBillChargeSlip, fuelType);
            chargeslipHelper.setCallback((PrintResponseCallBack) context);
        }else{
            txnNotificationSend();
            nfrHomePage(this);
        }
    }

    @Override
    public void fuelBillPrintNo() {
        if(progress.isShowing()){
            progress.dismiss();
        }
        txnNotificationSend();
        nfrHomePage(this);
    }

    @Override
    public void fuelBillPrintYes() {
        if(progress.isShowing()){
            progress.dismiss();
        }
        txnNotificationSend();
        nfrHomePage(this);
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
}