package com.ims.bpcluat.alp.alpOperations.cardManagement.enroll_additional;

import static androidx.core.content.ContentProviderCompat.requireContext;
import static com.ims.bpcluat.Helper.appVersion;
import static com.ims.bpcluat.Helper.cardNotificationDate;
import static com.ims.bpcluat.Helper.cardNotificationTime;
import static com.ims.bpcluat.Helper.cashNotificationDate;
import static com.ims.bpcluat.Helper.cashNotificationTime;
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
import static com.ims.bpcluat.Helper.roName;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.Helper.txnAmountUpToTwoDecimal;
import static com.ims.bpcluat.Helper.upiPrintDate;
import static com.ims.bpcluat.Helper.username;
import static com.ims.bpcluat.Helper.version;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.firstdata.merchantservicessdk.MSApi;
import com.google.gson.Gson;
import com.ims.bpcluat.CustomCameraActivity;
import com.ims.bpcluat.Helper;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.TxnFailedActivity;
import com.ims.bpcluat.cng.CngPaymentActivity;
import com.ims.bpcluat.databinding.ActivityEnrollCardPayBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.model.OnlineTxnModel;
import com.ims.bpcluat.receiver.ConnectivityReceiver;
import com.ims.bpcluat.utils.SharedPrefHelper;
import com.pax.fdms.opensdk.base24.Base24Constant;
import com.pax.fdms.opensdk.base24.Base24Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class EnrollCardPayActivity extends AppCompatActivity implements ApiHelper.NetworkingApiCallBack {
    ActivityEnrollCardPayBinding binding;
    ApiHelper api;
    ProgressDialog progress;
    Context context;
    String product = "", mobileNumber = "", vehicleNumber = "", vehicleType = "", txnType = "", notificationDate = "", notificationTime = "";
    String qty = "", amount = "", chargselipDate, chargselipTime, authBank = "", authCode = "", rrn = "";
    String cashRrn = "",cardFirst = "", cardLast = "";
    String fccAcknowledgement = "No",field1 = "",pumpNo = "", nozzleNo = "", field3 = "";
    String atc = "", cardType = "", posEntryMode = "", terminalInvoiceNo = "", batchNo = "", aid = "", tsi = "";
    String tvr = "", transactionCertificate = "", cardPaymentVersionNo = "", authTid = "", cardNo = "";
    private static final int REQUEST_CAMERA_CODE = 100;
    String txnId = "", dateTime = "", amountPayable = "", clientTxnId = "", txnNumber = "",chargeslipTxnId = "";
    private static final int CARD = 1;
    private static final int UPI = 2;
    private ConnectivityReceiver connectivityReceiver = new ConnectivityReceiver();

    private String[] options = {"CASH", "CARD", "UPI"};
    private int selectedOptionIndex = -1;
    private ArrayList<String> selectedOptionsList;
    //String paymentmode = "";
    String checkApiCall = "";
    String shredValue;
    SharedPrefHelper sharedPrefHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEnrollCardPayBinding.inflate(getLayoutInflater());
        context = this;
        api = new ApiHelper();
        connectivityReceiver = new ConnectivityReceiver();
        sharedPrefHelper = new SharedPrefHelper(this);
        shredValue = sharedPrefHelper.getString("cardManagementBtn", "");

        Intent intent = getIntent();
        if (intent.hasExtra("bundle")) {
            Bundle bundle = intent.getBundleExtra("bundle");
            mobileNumber = intent.getStringExtra("mobileNumber");
            dateTime = bundle.getString("dateTime");
            txnId = bundle.getString("txnId");
            txnNumber = bundle.getString("txnNumber");
            clientTxnId = bundle.getString("clientTxnId");
            amountPayable = bundle.getString("amountPayable");

            Log.d("SubmitButamountk", "Current amount: " + amountPayable);
            Log.d("SubmitButamountk1", "Current amount: " + amount);

            binding.payableamt.setText("Payable Amount " + amountPayable);
            amount = amountPayable;

        }

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        selectedOptionsList = new ArrayList<>(Collections.singletonList("Payment Mode"));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(EnrollCardPayActivity.this, android.R.layout.simple_spinner_item, selectedOptionsList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.mySpinner.setAdapter(adapter);

        binding.mySpinner.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    showOptionsDialog();
                }
                return true;
            }
        });

        binding.submitbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("SubmitButtonClick", "Current Payment Mode: " + txnType);

                String selectedMop = txnType;
                if (selectedMop != null) {
                    if (selectedMop.equals("CASH") || selectedMop.equals("CARD") || selectedMop.equals("UPI")) {
                        payment(selectedMop);
                    }
                } else {
                    Toast.makeText(EnrollCardPayActivity.this, "Please select one payment option", Toast.LENGTH_SHORT).show();
                }
            }
        });

        hideKeyboard();
        setContentView(binding.getRoot());
    }

    private void updateSpinnerSelection(String selectedOption) {
        selectedOptionsList.clear();
        selectedOptionsList.add(selectedOption);
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) binding.mySpinner.getAdapter();
        adapter.notifyDataSetChanged();
        txnType = selectedOption;
        Log.d("updatePaymentMode", "Updated Payment Mode: " + txnType);
    }

    private void payment(String selectedMop) {
        try {
            JSONObject requestParams = new JSONObject();
            if (selectedMop.equals("CASH")) {
                String date = cashNotificationDate();
                String time = cashNotificationTime();
                notificationDate = date;
                notificationTime = time;
//                onlineTxnModel.setTxnChargselipDate(cashChargeslipDate());
//                onlineTxnModel.setTxnChargeslipTime(cashChargeslipTime());
//                onlineTxnModel.setRrn("");
                rrn = "";
//                onlineTxnModel.setAuthCode("");
                authCode = "";
                batchNo = "";
                field3 = "";
//                onlineTxnModel.setBatchNo("");
//                onlineTxnModel.setBleTxnMop("01");
//                onlineTxnModel.setBlePaymentMode("07");
//                onlineTxnModel.setField3("");
////                redirectToSuccessPage();
                payEnrolmentApi("0");
            } else if (selectedMop.equals("CARD")) {
                Base24Request request = new Base24Request();
                request.setFunctionCode(Base24Constant.TYPE_SALE);
                request.setTotalTxnAmount(txnAmountUpToTwoDecimal(amount));
                request.setSuppressPrintChargeSlips("y");
                request.setMrn("3434323");
                requestParams.put("base24Request", new Gson().toJson(request));
                Log.d(" cardRequest = ", String.valueOf(requestParams));
                MSApi.getInstance().doPayment(this, CARD, requestParams);
            } else if (selectedMop.equals("UPI")) {
                requestParams.put("transaction_amount", amount);
                requestParams.put("transaction_type", "upi_qr");
                Log.d(" upiRequest = ", String.valueOf(requestParams));
                MSApi.getInstance().doQRCodeTransaction(this, UPI, requestParams);
            }
        } catch (Exception e) {
            Log.d("paymentException", e.toString());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        try {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == CARD) {
                if (resultCode == RESULT_OK) {
                    String response = data.getStringExtra("response");
                    Log.d("cardPaymentResponse", response);
                    JSONObject jsonObject = new JSONObject(response);
                    JSONObject base24Response = jsonObject.getJSONObject("base24Response");
                    String responseCode = base24Response.getString("responseCode");
                    if (base24Response.has("authCode")) {
                        authCode = base24Response.getString("authCode");
                    }
                    responseCode = responseCode.replaceAll("\\s", "");
                    responseCode = responseCode.toLowerCase();
                    if (responseCode.equals("transactionsuccess")) {
                        if (authCode.isEmpty()) {
                            Toast.makeText(this, "Auth Code is not available", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(this, TxnFailedActivity.class);
                            startActivity(intent);
                        } else {
                            fileWrite(this, todayDate + ".txt", "Card Payment Response :", String.valueOf(base24Response));
                            String dateTime = base24Response.getString("dateTime");
                            String posEntryMode = base24Response.getString("posEntryMode");
                            field3 = "";
//                            onlineTxnModel.setField3("");
//                            onlineTxnModel.setAuthCode(authCode);
                            notificationDate = cardNotificationDate(dateTime);
                            notificationTime = cardNotificationTime(dateTime);
                            chargselipDate = dateTime;
                            chargselipTime = dateTime;

//                            onlineTxnModel.setTxnNotificationDate(cardNotificationDate(dateTime));
//                            onlineTxnModel.setTxnNotificationTime(cardNotificationTime(dateTime));
//                            onlineTxnModel.setTxnChargselipDate(cardChargeslipDate(dateTime));
//                            onlineTxnModel.setTxnChargeslipTime(cardChargeslipTime(dateTime));

                            cardType = base24Response.optString("cardType", "");
                            aid = base24Response.optString("AID", "");
                            tsi = base24Response.optString("TSI", "");
                            tvr = base24Response.optString("TVR", "");


//                            onlineTxnModel.setCardType(base24Response.optString("cardType", ""));
//                            onlineTxnModel.setAid(base24Response.optString("AID", ""));
//                            onlineTxnModel.setTsi(base24Response.optString("TSI", ""));
//                            onlineTxnModel.setTvr(base24Response.optString("TVR", ""));

                            cardNo = base24Response.getString("maskedCardNo");
                            transactionCertificate = base24Response.optString("transactionCertificate", "");
                            rrn = base24Response.optString("rrn", "");
                            appVersion = base24Response.optString("appVersionNo", "");
                            terminalInvoiceNo = base24Response.optString("terminalInvoiceNo", "");
                            batchNo = base24Response.optString("batchNo", "");
                            authBank = "";
                            authTid = "";
                            atc = "******";
                            cardFirst = Helper.getCardFirst(base24Response.getString("maskedCardNo"));
                            cardLast = Helper.getCardLast(base24Response.getString("maskedCardNo"));
                            if (posEntryMode.equals("INSERT")) {
                                posEntryMode = "CHIP";
//                                onlineTxnModel.setPosEntryMode("CHIP");
                            } else {
                                posEntryMode = base24Response.getString("posEntryMode");
//                                onlineTxnModel.setPosEntryMode(posEntryMode);
                            }

//                            onlineTxnModel.setCardNo(base24Response.getString("maskedCardNo"));
//                            onlineTxnModel.setTransactionCertificate(base24Response.optString("transactionCertificate", ""));
//                            onlineTxnModel.setRrn(base24Response.optString("rrn", ""));
//                            onlineTxnModel.setTvr(base24Response.optString("TVR", ""));
//                            onlineTxnModel.setCardPaymentVersionNo(base24Response.optString("appVersionNo", ""));
//                            onlineTxnModel.setTerminalInvoiceNo(padWithZeroes(Integer.parseInt(base24Response.optString("terminalInvoiceNo", "")), 6));
//                            onlineTxnModel.setBatchNo(padWithZeroes(Integer.parseInt(base24Response.optString("batchNo", "")), 6));
//                            onlineTxnModel.setAuthBank("");
//                            onlineTxnModel.setAuthTid("");
//                            onlineTxnModel.setAtc("******");
//                            onlineTxnModel.setCardFirst(Helper.getCardFirst(base24Response.getString("maskedCardNo")));
//                            onlineTxnModel.setCardLast(Helper.getCardLast(base24Response.getString("maskedCardNo")));
//                            if (posEntryMode.equals("INSERT")) {
//                                onlineTxnModel.setPosEntryMode("CHIP");
//                            } else {
//                                onlineTxnModel.setPosEntryMode(posEntryMode);
//                            }

//                            if (base24Response.has("cardProduct")) {
//                                String cardProductType = base24Response.optString("cardProduct", "");
//                                if (cardProductType.isEmpty()) {
//                                    String mop = DecimalToHex.create(10);
//                                    if(mop.length() == 1){
//                                        mop = "0"+mop;
//                                    }
//                                    bleTxnMop = mop;
//                                    blePaymentMode = "07";
////                                    onlineTxnModel.setBleTxnMop(mop);
////                                    onlineTxnModel.setBlePaymentMode("07");
//                                } else {
//                                    if (cardProductType.contains("debit")) {
//                                        String debitMop = DecimalToHex.create(11);
//                                        if(debitMop.length() == 1){
//                                            debitMop = "0"+debitMop;
//                                        }
//                                        bleTxnMop = debitMop;
//                                        blePaymentMode = "03";
////                                        onlineTxnModel.setBleTxnMop(debitMop);
////                                        onlineTxnModel.setBlePaymentMode("03");
//                                    } else if (cardProductType.contains("credit")) {
//                                        bleTxnMop = "02";
//                                        blePaymentMode = "02";
////                                        onlineTxnModel.setBleTxnMop("02");
////                                        onlineTxnModel.setBlePaymentMode("02");
//                                    } else {
//                                        String mop = DecimalToHex.create(10);
//                                        if(mop.length() == 1){
//                                            mop = "0"+mop;
//                                        }
//                                        bleTxnMop = mop;
//                                        blePaymentMode = "07";
////                                        onlineTxnModel.setBleTxnMop(mop);
////                                        onlineTxnModel.setBlePaymentMode("07");
//                                    }
//                                }
//                            } else {
//                                String mop = DecimalToHex.create(10);
//                                if(mop.length() == 1){
//                                    mop = "0"+mop;
//                                }
//                                bleTxnMop = mop;
//                                blePaymentMode = "07";
////                                onlineTxnModel.setBleTxnMop(mop);
////                                onlineTxnModel.setBlePaymentMode("07");
//                            }
                            fileWrite(this, todayDate + ".txt", "Card Payment Response :", String.valueOf(base24Response));
                            payEnrolmentApi(authCode);
                        }
                    } else {
                        Intent intent = new Intent(this, TxnFailedActivity.class);
                        startActivity(intent);
                    }
                }
            } else if (requestCode == UPI) {
                if (resultCode == RESULT_OK) {
                    String response = data.getStringExtra("response");
                    Log.d("upiPaymentResponse", response);
                    JSONObject jsonObject = new JSONObject(response);
                    if (jsonObject.has("status")) {
                        String message = jsonObject.getString("message");
                        String originalStatus = jsonObject.getString("status");
                        String upiStatus = jsonObject.getString("status");
                        upiStatus = upiStatus.toLowerCase();
                        if (upiStatus.contains("success")) {
                            String date = jsonObject.getString("txnDate");
                            String time = jsonObject.getString("txnTime");
                            field3 = "";
                            chargselipDate = upiPrintDate(date);
                            chargselipTime = upiPrintDate(date);
                            notificationDate = upiPrintDate(date);
                            notificationTime = upiPrintDate(date);
                            notificationTime = upiPrintDate(date);


//                            onlineTxnModel.setField3("");
//                            onlineTxnModel.setTxnChargselipDate(upiPrintDate(date));
//                            onlineTxnModel.setTxnChargeslipTime(upiPrintTime(time));
//                            onlineTxnModel.setTxnNotificationDate(upiNotificationDate(date));
//                            onlineTxnModel.setTxnNotificationTime(time);
                            if (jsonObject.has("authCode")) {
                                authCode = jsonObject.optString("authCode", "");

//                                onlineTxnModel.setAuthCode(jsonObject.optString("authCode", ""));
                            } else {
                                authCode = "";
//                                onlineTxnModel.setAuthCode("");
                            }
                            rrn = jsonObject.getString("transaction_id");
                            cardType = "Transaction";
                            cardNo = " -  BQR";

//                            onlineTxnModel.setRrn(jsonObject.getString("transaction_id"));
//                            onlineTxnModel.setCardType("Transaction");
//                            onlineTxnModel.setCardNo(" -  BQR");
//                            onlineTxnModel.setBleTxnMop("18");
//                            onlineTxnModel.setBlePaymentMode("05");
                            payEnrolmentApi(authCode);

                        } else {
                            if (originalStatus.equals("TRANSACTION_TIMEDOUT")) {
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(context, TxnFailedActivity.class);
                                startActivity(intent);
                            } else {
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(context, TxnFailedActivity.class);
                                startActivity(intent);
                            }
                        }
                    } else {
                        Intent intent = new Intent(context, TxnFailedActivity.class);
                        startActivity(intent);
                    }
                }
            }
        } catch (JSONException e) {
            Log.d("JSONException", "onActivityResult: " + e.toString());
        }
    }

    private void showOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Payment Mode");

        builder.setSingleChoiceItems(options, selectedOptionIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedOptionIndex = which;
                if (selectedOptionIndex != -1) {
                    String selectedOption = options[selectedOptionIndex];
                    Log.d("SelectedOption", selectedOption);
                    updateSpinnerSelection(selectedOption);
                    dialog.dismiss();
                }
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void payEnrolmentApi(String authCode) {
        checkApiCall = "payEnrolmentApi";
        progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);
        progress.show();
        String url = "alpreq";
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("userName", username);
            jsonObject.put("channel", "BPCL");
            jsonObject.put("tid", tid);
            jsonObject.put("mid", mid);
            jsonObject.put("source", "Mobile");
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("dateTime", dateTime);
            jsonObject.put("txnType", "APP");
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnId", txnId);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("rrn", txnNumber);
            jsonObject.put("mop", txnType);
            jsonObject.put("authCode", authCode);
            jsonObject.put("id", clientTxnId);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "0");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);

            Log.d("ApiName", "loyaltyProgramPayApi");
            Log.d("ApiRequest", String.valueOf(jsonObject));

            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);

        } catch (JSONException e) {
            Log.d("respCode", "respCode");
            progress.dismiss();
        }
    }

    public void txnNotificationSend() {
        checkApiCall = "txnNotificationSend";
        try {
            if(txnType.equals("UPI"))
            {
                txnType = "BQR";
            }
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
            billerTranListObject.put("tran_amt", amount);
            billerTranListObject.put("tran_date", notificationDate);
            billerTranListObject.put("tran_time", notificationTime);

            if (txnType.equals("CASH")) {
                billerTranListObject.put("rrn", cashRrn);
            } else {
                billerTranListObject.put("rrn", rrn);
            }
            if (txnType.equals("CARD")) {
                billerTranListObject.put("card_first", cardFirst);
                billerTranListObject.put("card_last", cardLast);
            } else {
                billerTranListObject.put("card_first", "");
                billerTranListObject.put("card_last", "");
            }
            billerTranListObject.put("ft_number", chargeslipTxnId);
            billerTranListObject.put("session_id", "");

            billerTranListObject.put("cust_id", username);
            billerTranListObject.put("pay_method", txnType);
            billerTranListObject.put("authAmt", amount);
            billerTranListObject.put("refundAmt", "0");
            billerTranListObject.put("balanceAmt", amount);
            billerTranListObject.put("field1", "Offline");
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
            paramListArray.put(createJsonObject(mobileNumber, "Customer Mobile"));
            paramListArray.put(createJsonObject("","Vehicle ID"));
            paramListArray.put(createJsonObject("", "PUMP_NO"));
            paramListArray.put(createJsonObject("", "NOZZLE"));
            paramListArray.put(createJsonObject("", "QUANTITY"));
            paramListArray.put(createJsonObject("", "PROD_NAME"));
            paramListArray.put(createJsonObject(version, "VERSION"));
            paramListArray.put(createJsonObject("", "UNIT_PRICE"));
            paramListArray.put(createJsonObject("0.00", "CUSTOMER_DISC"));
            paramListArray.put(createJsonObject("", "ORDER_ID"));
            paramListArray.put(createJsonObject("", "FCC TIMESTAMP"));
            paramListArray.put(createJsonObject(roName, "MERCH NAME"));
            paramListArray.put(createJsonObject("", "Vehicle_Type"));
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
            paramListArray.put(createJsonObject(fccAcknowledgement, "fcc_ack"));

            jsonObject.put("billerTranList", billerTranListArray);
            Log.d("ApiName", "saveBillerTxn");
            Log.d("FileName", "EnrollCardPayActivity");
            Log.d("ApiRequest", String.valueOf(jsonObject));
            fileWrite(context, todayDate + ".txt", "Ocean notification request :", String.valueOf(jsonObject));
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

    public void apiResult(String res, String apiName) {
        if (checkApiCall.equals("payEnrolmentApi")) {
            if (res.equals("Server Time Out")) {
                Log.d("timeout_problem", res);
                progress.dismiss();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(EnrollCardPayActivity.this, "Server Time Out", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                try {
                    Log.d("enrolResponse3 = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respCode = payLoad.getString("respCode");
                    String respDesc = payLoad.getString("respDesc");

                    if (respCode.equals("200")) {
                        txnNotificationSend();        // if you think about this api then change it location...
                        progress.dismiss();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(EnrollCardPayActivity.this, EnrollReciept.class);
                                intent.putExtra("payload", payLoad.toString());
                                intent.putExtra("mobileNumber", mobileNumber);
                                startActivity(intent);
                                finish();
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("enrolrespcode", respCode);
                                progress.dismiss();
                                MessagesDialog.showDialog(EnrollCardPayActivity.this, respDesc, 0, null, null);
                            }
                        });
                    }

                } catch (Exception e) {
                    Log.e("APIError", "Error parsing JSON response", e);
                    progress.dismiss();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(EnrollCardPayActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        } else {
            if (res.equals("Server Time Out")) {
                progress.dismiss();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(EnrollCardPayActivity.this, "Server Time Out", Toast.LENGTH_SHORT).show();
                    }
                });
            }else{
                Log.d("NotificationApiResponse = ", res);
            }
        }
    }

    public void cameraOpen() {
        if (ContextCompat.checkSelfPermission(EnrollCardPayActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            askCameraPermission();
        } else {
            Intent intent = new Intent(EnrollCardPayActivity.this, CustomCameraActivity.class);
            startActivityForResult(intent, REQUEST_CAMERA_CODE);
        }
    }

    public void askCameraPermission() {
        ActivityCompat.requestPermissions((Activity) this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            cameraOpen();
        }
    }


    public void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) EnrollCardPayActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}