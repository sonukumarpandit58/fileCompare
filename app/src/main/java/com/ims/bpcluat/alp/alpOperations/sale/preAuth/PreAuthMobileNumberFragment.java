package com.ims.bpcluat.alp.alpOperations.sale.preAuth;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;
import static com.google.android.material.internal.ViewUtils.hideKeyboard;
import static com.ims.bpcluat.Helper.appVersion;
import static com.ims.bpcluat.Helper.cashNotificationDate;
import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.createJsonObject;
import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.fuelProductList;
import static com.ims.bpcluat.Helper.getCurrentDateTime;
import static com.ims.bpcluat.Helper.getProductId;
import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.roName;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.Helper.username;
import static com.ims.bpcluat.Helper.version;
import static com.ims.bpcluat.helper.ApiHelper.alpEndpoint;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.fiserv.alpsdk.data.AlpRequest;
import com.fiserv.alpsdk.wrapper.AlpApi;
import com.ims.bpcluat.Helper;
import com.ims.bpcluat.PaymentActivity;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.SuccessActivity;
import com.ims.bpcluat.alp.alpOperations.sale.SaleFragment;
import com.ims.bpcluat.alp.alpOperations.sale.TxnFailFragment;
import com.ims.bpcluat.alp.alpOperations.utility.alpVoid.AlpVoidFragment;
import com.ims.bpcluat.alp.alpOperations.utility.alpVoid.AlpVoidTransactions;
import com.ims.bpcluat.alp.alpOperations.utility.alpVoid.VoidMobileNumFragment;
import com.ims.bpcluat.cng.CngPaymentActivity;
import com.ims.bpcluat.cng.CngSuccessActivity;
import com.ims.bpcluat.databinding.FragmentPreAuthMobileNumberBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.model.AlpModels.VoidModels.AlpTxnModel;
import com.ims.bpcluat.model.AlpModels.VoidModels.Output;
import com.ims.bpcluat.model.AlpModels.VoidModels.TxnList;
import com.ims.bpcluat.model.CngModel;
import com.ims.bpcluat.model.NfrModel;
import com.ims.bpcluat.model.OnlineTxnModel;
import com.ims.bpcluat.nfr.NfrPaymentActivity;
import com.ims.bpcluat.nfr.NfrSuccessActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PreAuthMobileNumberFragment extends Fragment implements ApiHelper.NetworkingApiCallBack {

    FragmentPreAuthMobileNumberBinding binding;
    ProgressDialog progress;
    Context context;
    ApiHelper api;
    boolean otpSent = true;
    String checkResult = "";
    String txnId = "", dateTime = "", isTxnOnline = "";
    String message = "";
    String reqDate = "", reqTime = "", tran_date = "", tran_time = "";
    private CngModel cngModel;
    String amount = "", mobileNumber = "";
    private OnlineTxnModel onlineTxnModel;
    private NfrModel nfrModel;
    String ft_number = "", cust_id = "", balanceAmt = "", authAmt = "";
    String pumpNo = "", nozzleNo = "", localMPDId = "", localProductID = "", qty = "", vehId = "", unitAmt = "", field1 = "", field3 = "", field7 = "", field6 = "", field9 = "", rrn = "";
    String amountInPaise = "", clientTxnId = "", fcctxnID = "";
    private static final String PREAUTH_CARD = "PREAUTH_CARD";
    private static final int CARD = 1;
    String programID = "", walletID = "", odometerReading = "", quantityLitres = "", cardNumber = "";
    String requestCheck = "no";

    private CountDownTimer countDownTimer;

    public PreAuthMobileNumberFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPreAuthMobileNumberBinding.inflate(inflater, container, false);
        context = getActivity();
        api = new ApiHelper();
        hideKeyboard();

        if (getArguments() != null) {
            clientTxnId = tid + getCurrentDateTime();
            field3 = tid + getCurrentDateTime();
            cngModel = getArguments().getParcelable("cngModel");
            onlineTxnModel = getArguments().getParcelable("onlineTxnModel");
            nfrModel = getArguments().getParcelable("nfrModel");

            if (cngModel != null) {
                mobileNumber = cngModel.getMobileNumber();
                amount = cngModel.getTotalAmt();
                unitAmt = cngModel.getPerAmt();
                txnId = cngModel.getTxnId();
                qty = cngModel.getQty();
                vehId = cngModel.getVehicleNumber();
                field1 = "Offline";
                localProductID = getProductId("CNG", fuelProductList);
                fcctxnID = "";
            } else if (onlineTxnModel != null) {
                mobileNumber = onlineTxnModel.getMobileNumber();
                amount = onlineTxnModel.getAmount();
                unitAmt = onlineTxnModel.getUnitPrice();
                pumpNo = onlineTxnModel.getPumpNo();
                nozzleNo = onlineTxnModel.getPumpNo();
                localMPDId = onlineTxnModel.getLocalMPDId();
                localProductID = getProductId(onlineTxnModel.getProductName(), fuelProductList);
                txnId = onlineTxnModel.getTxnId();
                qty = onlineTxnModel.getQty();
                vehId = onlineTxnModel.getVehicleNumber();
                isTxnOnline = onlineTxnModel.getIsTxnOnline();
                if (isTxnOnline.equals("yes")) {
                    field1 = "Online";
                    fcctxnID = onlineTxnModel.getTxnId();
                } else {
                    field1 = "Offline";
                    fcctxnID = "";
                }

            } else if (nfrModel != null) {
                mobileNumber = nfrModel.getMobileNumber();
                amount = nfrModel.getAmt();
                SharedPreferences shared = getActivity().getSharedPreferences("nfrSharedPreferencesData", MODE_PRIVATE);
                qty = (shared.getString("totalQty", ""));
                txnId = nfrModel.getTxnId();

                vehId = nfrModel.getVehicleNumber();
                field1 = "Offline";
                localProductID = getProductId("LUBES", fuelProductList);
                unitAmt = nfrModel.getAmt();
                fcctxnID = "";
            }
            int amtInPaise = (int) (Double.parseDouble(amount) * 100);
            amountInPaise = String.valueOf(amtInPaise);
            binding.mobileNum.setText(mobileNumber);
        }

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redirectToFailedPage();
            }
        });

        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateFields();
            }
        });


        binding.physicalCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPhysicalCard();
            }
        });

        return binding.getRoot();
    }

    private void validateFields() {
        String mobileNum = binding.mobileNum.getText().toString().trim();
        String accNo = binding.accountNum.getText().toString().trim();
        String otp = binding.otp.getText().toString().trim();
        if (otpSent) {
            if (TextUtils.isEmpty(mobileNum)) {
                binding.mobileNum.setError("Please enter Mobile Number");
                binding.mobileNum.requestFocus();
                return;
            }
            preAuthInitiateOtpApi(mobileNum, accNo);
        } else {
            if (TextUtils.isEmpty(otp)) {
                binding.otp.setError("Please enter OTP");
                binding.otp.requestFocus();
                return;
            }
            preAuthValidateOtpApi(otp);
        }
    }

    private void preAuthInitiateOtpApi(String mobileNumber, String accNo) {
        checkResult = "preAuthInitiateOtpApi";
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while fetching details...");
        progress.setCancelable(false);
        progress.show();
        String url = "alpreq";
        String reqDate = requestDate();
        String reqTime = requestTime();
        String transDate = cashNotificationDate();
        Log.d("txnIdsss", "txnIdssssss: " + txnId);

        txnId = Helper.createTxnIdForOfflineTxn();
        JSONObject jsonObject = new JSONObject();
        JSONArray billerTranList = new JSONArray();
        JSONObject billerTranItem = new JSONObject();
        JSONArray paramList = new JSONArray();
        try {
            jsonObject.put("userName", username);
            jsonObject.put("channel", "BPCL");
            jsonObject.put("tid", tid);
            jsonObject.put("mid", mid);
            jsonObject.put("reqDate", reqDate);
            jsonObject.put("reqTime", reqTime);
            jsonObject.put("txnType", "AAO");
//            jsonObject.put("tranChannel", "VC");
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);

            billerTranItem.put("mid", mid);
            billerTranItem.put("tid", tid);
            billerTranItem.put("trans_status", "PENDING");
            billerTranItem.put("tran_amt", amount);
            billerTranItem.put("tran_date", transDate);
            billerTranItem.put("tran_time", reqTime);
            billerTranItem.put("ft_number", txnId);
            billerTranItem.put("cust_id", username);
            billerTranItem.put("pay_method", "ALP");
            billerTranItem.put("authAmt", amount);
            billerTranItem.put("refundAmt", "0");
            billerTranItem.put("balanceAmt", amount);
            billerTranItem.put("field1", "Offline");
            billerTranList.put(billerTranItem);
            billerTranItem.put("paramList", paramList);

            paramList.put(createJsonObject(pumpNo, "PUMP_NO"));
            paramList.put(createJsonObject(localMPDId, "localMPDId"));
            paramList.put(createJsonObject(nozzleNo, "NOZZLE"));
            paramList.put(createJsonObject(localProductID, "localProductID"));
            paramList.put(createJsonObject(mobileNumber, "Customer Mobile"));
            paramList.put(createJsonObject(sapCode, "SAP CODE"));
            paramList.put(createJsonObject("SmartDrive", "ProgramId"));
            paramList.put(createJsonObject("", "WalletId"));
            paramList.put(createJsonObject("", "odometerReading"));
            paramList.put(createJsonObject(qty, "QUANTITY"));
            paramList.put(createJsonObject(roName, "MERCH NAME"));
            paramList.put(createJsonObject("", "Attendant ID"));
            paramList.put(createJsonObject(unitAmt, "UNIT_PRICE"));
            paramList.put(createJsonObject(vehId, "Vehicle ID"));
            paramList.put(createJsonObject("", "CUSTOMER_DISC"));
            paramList.put(createJsonObject(reqDate + reqTime, "FCC TIMESTAMP"));
            paramList.put(createJsonObject("", "discountID"));
            paramList.put(createJsonObject(accNo, "accountNumber"));

            jsonObject.put("billerTranList", billerTranList);

            Log.d("ApiName", "Pre-Auth Initiate OTP : AAO");
            Log.d("FileName","PreAuthMobileNumberFragment.java");
            Log.d("Request", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void preAuthValidateOtpApi(String otp) {
        requestCheck = "yes";
        checkResult = "preAuthValidateOtpApi";
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while fetching details...");
        progress.setCancelable(false);
        progress.show();
        String url = "alpreq";
        Log.d("txnIdsss", "txnIdssssss: " + txnId);

        txnId = Helper.createTxnIdForOfflineTxn();
        JSONObject jsonObject = new JSONObject();
        JSONArray billerTranList = new JSONArray();
        JSONObject billerTranItem = new JSONObject();
        JSONArray paramList = new JSONArray();
        try {
            Log.d("txnIdsss", "txnIdssssss11111: " + txnId);
            jsonObject.put("userName", username);
            jsonObject.put("channel", "BPCL");
            jsonObject.put("tid", tid);
            jsonObject.put("mid", mid);
            jsonObject.put("reqDate", reqDate);
            jsonObject.put("reqTime", reqTime);
            jsonObject.put("txnType", "APT");
            jsonObject.put("password", otp);
//            jsonObject.put("tranChannel", "VC");
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);

            billerTranItem.put("mid", mid);
            billerTranItem.put("tid", tid);
            billerTranItem.put("trans_status", "PENDING");
            billerTranItem.put("tran_amt", amount);
            billerTranItem.put("tran_date", tran_date);
            billerTranItem.put("tran_time", tran_time);
            billerTranItem.put("ft_number", ft_number);
            billerTranItem.put("cust_id", cust_id);
            billerTranItem.put("pay_method", "ALP");
            billerTranItem.put("authAmt", amount);
            billerTranItem.put("refundAmt", "0");
            billerTranItem.put("balanceAmt", amount);
            billerTranItem.put("field1", "Offline");
            billerTranItem.put("field3", field3);
            billerTranList.put(billerTranItem);
            billerTranItem.put("paramList", paramList);

            paramList.put(createJsonObject(pumpNo, "PUMP_NO"));
            paramList.put(createJsonObject(localMPDId, "localMPDId"));
            paramList.put(createJsonObject(nozzleNo, "NOZZLE"));
            paramList.put(createJsonObject(localProductID, "localProductID"));
            paramList.put(createJsonObject(mobileNumber, "Customer Mobile"));
            paramList.put(createJsonObject(sapCode, "SAP CODE"));
            paramList.put(createJsonObject("SmartDrive", "ProgramId"));
            paramList.put(createJsonObject("", "WalletId"));
            paramList.put(createJsonObject("", "odometerReading"));
            paramList.put(createJsonObject(qty, "QUANTITY"));
            paramList.put(createJsonObject(roName, "MERCH NAME"));
            paramList.put(createJsonObject("", "Attendant ID"));
            paramList.put(createJsonObject(unitAmt, "UNIT_PRICE"));
            paramList.put(createJsonObject(vehId, "Vehicle ID"));
            paramList.put(createJsonObject("", "CUSTOMER_DISC"));
            paramList.put(createJsonObject(reqDate + reqTime, "FCC TIMESTAMP"));
            paramList.put(createJsonObject("", "discountID"));

            jsonObject.put("billerTranList", billerTranList);

            Log.d("ApiName", "Pre-Auth Initiate Transaction : APT");
            Log.d("FileName","PreAuthMobileNumberFragment.java");
            Log.d("Request", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void doPhysicalCard() {
        progress = new ProgressDialog(context);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);
        progress.show();
        String reqDate = requestDate();
        String reqTime = requestTime();
        String dateTime = reqDate + reqTime;
        AlpRequest alpRequest = new AlpRequest();
        try {
            alpRequest.setAmountRs(amountInPaise);
            alpRequest.setAposTerminalId("");
            alpRequest.setTid(tid);
            alpRequest.setRoCode(sapCode);
            alpRequest.setHwSrNo(Helper.serialNumber);
            alpRequest.setDealerID(sapCode);
            alpRequest.setAppVersion(appVersion);
            alpRequest.setChannel("");
            alpRequest.setClient(manualGetClientId());
            alpRequest.setClientTxnId(clientTxnId);
            alpRequest.setCurrencyCode("");
            alpRequest.setDateTime(dateTime);
            alpRequest.setDiscountAmount("");
            alpRequest.setDiscountID("");
            alpRequest.setFcctxnID(fcctxnID);
            alpRequest.setGeotagRange("10");
            alpRequest.setInstId(manualGetInstId());
            alpRequest.setIpsMarker("1");
            alpRequest.setLatitude("0");
            alpRequest.setLocalBayID("");
            alpRequest.setLocalMPD_ID("");
            alpRequest.setLocalNozzleID("");
            alpRequest.setLocalProductID(localProductID);
            alpRequest.setLongitude("0");
            alpRequest.setMid(mid);
            alpRequest.setMobNo("");
            alpRequest.setNetAmountRs(amountInPaise);
            alpRequest.setOdometerReading("");
            alpRequest.setOriginalAlpTransactionId("");
            alpRequest.setOtp("");
            alpRequest.setPayInstrument("");
            alpRequest.setProgramID("");
            alpRequest.setPurpose("");
            alpRequest.setQuantityLitres(qty);
            alpRequest.setReasonForVoid("");
            alpRequest.setReqDate(requestDate());
            alpRequest.setReqTime(reqTime);
            alpRequest.setReqType(PREAUTH_CARD);
            alpRequest.setTxnId(txnId);
            alpRequest.setTxnType("APC");
            alpRequest.setUserName(username);
            alpRequest.setWalletID("");

            Log.d("alpRequestParams", alpRequest.toString());
            AlpApi.doPreAuthRequest(getActivity(), CARD, alpRequest);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        try {
            Log.d("sonuTest1", String.valueOf(data));
            super.onActivityResult(requestCode, resultCode, data);
            if (data != null) {
                if (requestCode == CARD) {
                    if (resultCode == RESULT_OK) {
                        Bundle extras = data.getExtras();
                        if (extras != null) {
                            // Loop through the extras and print all keys and values
                            for (String key : extras.keySet()) {
                                Object value = extras.get(key);
                                Log.d("IntentExtra", "Key: " + key + " Value: " + value);
                            }
                        } else {
                            Log.d("IntentExtra", "No extras found in the intent.");
                        }
                        progress.dismiss();
                        String response = data.getStringExtra("alpResponse");
                        Log.d("sonuTest", response);
                        if (response != null) {
                            Log.d("loyaltyCardResponse", response);
                            try {
                                JSONObject jsonResponse = new JSONObject(response);
                                if (Helper.isAlpCodeExistsForStatusApiCall(jsonResponse.getString("code"))) {
                                    JSONObject dataObject = jsonResponse.getJSONObject("data");
                                    JSONObject physicalCard = dataObject.getJSONObject("PhysicalCard");
                                    String cardName = physicalCard.optString("cardName");
//                                    cardNumber = physicalCard.getString("cardNumber");
                                    Log.d("CardDetails", "Card Name: " + cardName);

                                    JSONObject alpRequest = dataObject.getJSONObject("alpRequest");
                                    txnId = alpRequest.optString("txnId");
                                    programID = alpRequest.optString("programID");
                                    walletID = alpRequest.optString("walletID");
                                    odometerReading = alpRequest.optString("odometerReading");
                                    quantityLitres = alpRequest.optString("quantityLitres");

                                    Log.d("AlpRequestDetails", "Transaction ID: " + txnId);

                                    getActivity().runOnUiThread(() -> {
                                        cardTransactionStatusApi();
                                    });
                                } else {
                                    message = jsonResponse.getString("message");
                                    getActivity().runOnUiThread(() -> {
                                        MessagesDialog.showDialog(context, message, 0, null, null);
                                    });
                                }
                            } catch (JSONException e) {
                                progress.dismiss();
                                Log.e("loyaltyCardResponse", "Error parsing JSON: " + e.getMessage());
                            }
                        } else {
                            progress.dismiss();
                            Log.d("loyaltyCardResponse", "No response received");
                        }
                    }
                } else {
                    progress.dismiss();
                    String ocrResult = data.getStringExtra("ocrResult");
                    if (ocrResult != null) {
                        ocrResult = ocrResult.replaceAll("\\s", "");
                        Log.d("loyaltyCardResponse@", ocrResult);
                    } else {
                        Log.d("loyaltyCardResponse@", "No OCR result received");
                    }
                }
            } else {
                progress.dismiss();
                // Log if data itself is null
                Log.d("loyaltyCardResponse@", "No intent data received");
            }
        } catch (Exception e) {
            progress.dismiss();
            Log.d("loyaltyCardResponse@", e.toString());
        }
    }

    private void cardTransactionStatusApi() {
        checkResult = "cardTransactionStatusApi";
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);
        progress.show();
        String url = alpEndpoint;

        String reqDate = requestDate();
        String reqTime = requestTime();

        JSONObject jsonObject = new JSONObject();
        JSONArray billerTranListArray = new JSONArray();
        JSONObject billerTranListObject = new JSONObject();
        JSONArray paramListArray = new JSONArray();
        try {
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("source", "Mobile");
            jsonObject.put("channel", channelName);
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("userName", username);
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("txnType", "ATS");
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("appVersion", "BPCL" + appVersion);

            billerTranListObject.put("mid", mid);
            billerTranListObject.put("tid", tid);
            billerTranListObject.put("trans_status", "PENDING");
            billerTranListObject.put("tran_amt", amount);
            billerTranListObject.put("tran_date", cashNotificationDate());
            billerTranListObject.put("tran_time", requestTime());
            billerTranListObject.put("ft_number", txnId);
            billerTranListObject.put("cust_id", username);
            billerTranListObject.put("pay_method", "ALP");
            billerTranListObject.put("authAmt", amount);
            billerTranListObject.put("refundAmt", "0");
            billerTranListObject.put("balanceAmt", amount);
            billerTranListObject.put("field1", field1);
            billerTranListObject.put("field3", field3);
            billerTranListObject.put("field13", "PC");

            billerTranListArray.put(billerTranListObject);
            billerTranListObject.put("paramList", paramListArray);
            paramListArray.put(createJsonObject(pumpNo, "PUMP_NO"));
            paramListArray.put(createJsonObject(localMPDId, "localMPDId"));
            paramListArray.put(createJsonObject(nozzleNo, "NOZZLE"));
            paramListArray.put(createJsonObject(localProductID, "localProductID"));
            paramListArray.put(createJsonObject(mobileNumber, "Customer Mobile"));
            paramListArray.put(createJsonObject(sapCode, "SAP CODE"));
            paramListArray.put(createJsonObject(programID, "ProgramId"));
            paramListArray.put(createJsonObject(walletID, "WalletId"));
            paramListArray.put(createJsonObject(odometerReading, "odometerReading"));
            paramListArray.put(createJsonObject(qty, "QUANTITY"));
            paramListArray.put(createJsonObject(roName, "MERCH NAME"));
            paramListArray.put(createJsonObject("", "Attendant ID"));
            paramListArray.put(createJsonObject(unitAmt, "UNIT_PRICE"));
            paramListArray.put(createJsonObject(vehId, "Vehicle ID"));
            paramListArray.put(createJsonObject("", "CUSTOMER_DISC"));
            paramListArray.put(createJsonObject(reqDate + reqTime, "FCC TIMESTAMP"));
            paramListArray.put(createJsonObject("", "discountID"));
            jsonObject.put("billerTranList", billerTranListArray);

            Log.d("StatusCheckRequest = ", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void apiResult(String res, String apiName) {
        if (checkResult.equals("preAuthInitiateOtpApi")) {
            try {
                if (res.equals("Server Time Out")) {
                    progress.dismiss();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(requireContext(), "Server Time Out", 0, null, null);
                        }
                    });
                } else {
                    Log.d("preAuthInitiateResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respCode = payLoad.getString("respCode");
                    String respDesc = payLoad.getString("respDesc");

                    if (respCode.equals("200")) {
                        JSONArray outputArray = payLoad.getJSONArray("output");

                        if (outputArray.length() > 0) {
                            JSONObject outputObject = outputArray.getJSONObject(0);
                            message = outputObject.optString("message");
                        }

                        JSONArray billerTranListArray = payLoad.getJSONArray("billerTranList");
                        if (billerTranListArray.length() > 0) {
                            JSONObject transaction = billerTranListArray.getJSONObject(0);

                            tran_date = transaction.optString("tran_date");
                            tran_time = transaction.optString("tran_time");
                            ft_number = transaction.optString("ft_number");
                            field3 = transaction.optString("field3");
                            cust_id = transaction.optString("cust_id");

                            JSONArray paramListArray = transaction.getJSONArray("paramList");

                            for (int j = 0; j < paramListArray.length(); j++) {
                                JSONObject param = paramListArray.getJSONObject(j);
                                if ("Customer Mobile".equals(param.getString("param_lit"))) {
                                    mobileNumber = param.getString("param");
                                    Log.d("Custmoner", "Customer Mobile: " + mobileNumber);
                                    break;
                                }
                            }
                        }

                        reqDate = payLoad.getString("reqDate");
                        reqTime = payLoad.getString("reqTime");


                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                try {
                                    binding.mobileNum.setVisibility(View.GONE);
                                    binding.accountNum.setVisibility(View.GONE);
                                    binding.underlineIds.underline.setVisibility(View.GONE);
                                    binding.physicalCard.setVisibility(View.GONE);
                                    binding.otp.setVisibility(View.VISIBLE);
                                    binding.msg.setText(message);
                                    binding.msg.setVisibility(View.VISIBLE);
                                    binding.timerTxt.setVisibility(View.VISIBLE);
                                    binding.submitBtn.setText("SUBMIT OTP");

                                    otpSent = false;

                                    countDownTimer = new CountDownTimer(45000, 1000) {
                                        public void onTick(long millisUntilFinished) {
                                            binding.timerTxt.setText(millisUntilFinished / 1000 + " seconds remaining");
                                        }

                                        public void onFinish() {
                                            binding.timerTxt.setText("");
                                            getActivity().runOnUiThread(() -> {
                                                if(requestCheck.equals("no")){
                                                    redirectToTimeOutPage();
                                                }else{
                                                    Log.d("requestCheck", "onFinish: "+requestCheck);
                                                }
                                            });
                                        }
                                    }.start();
                                } catch (Exception e) {
                                    Log.d("JSONException", "run: " + e);
                                }

                            }
                        });


                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                MessagesDialog.showDialog(getActivity(), respDesc, 0, null, null);
                            }
                        });
                    }

                }
            } catch (JSONException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.dismiss();
                        Log.d("respCode", e.toString());
                        fileWrite(context, todayDate + ".txt", "validateOtpApi", e.toString());
                        binding.otp.setText("");

                        MessagesDialog.showDialog(getActivity(), e.toString(), 0, null, null);
                    }
                });
            }
        } else if (checkResult.equals("preAuthValidateOtpApi")) {
            try {
                if (res.equals("Server Time Out")) {
                    progress.dismiss();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(requireContext(), "Server Time Out", 0, null, null);
                        }
                    });
                } else {
                    Log.d("preAuthValidateResponse = ", res);

//                     res = "{ \"nameValuePairs\": { " +
//                            "\"sub\": \"API_SERVICE\", " +
//                            "\"aud\": \"OCEAN\", " +
//                            "\"iss\": \"MOBILE_APP\", " +
//                            "\"PAYLOAD\": { " +
//                            "\"channel\": \"BPCL\", " +
//                            "\"reqDate\": \"20241104\", " +
//                            "\"reqTime\": \"111547\", " +
//                            "\"response\": \"SUCCESS\", " +
//                            "\"respCode\": \"200\", " +
//                            "\"resDate\": \"20241104\", " +
//                            "\"resTime\": \"111557\", " +
//                            "\"userName\": \"9716325888\", " +
//                            "\"password\": \"123456\", " +
//                            "\"mid\": \"470000099309183\", " +
//                            "\"tid\": \"39287941\", " +
//                            "\"client\": \"47000\", " +
//                            "\"respDesc\": \"SUCCESS\", " +
//                            "\"id\": \"39287941041124111547\", " +
//                            "\"txnType\": \"APT\", " +
//                            "\"appVersion\": \"BPCL1.0.94\", " +
//                            "\"operatorDetail\": [], " +
//                            "\"result\": [], " +
//                            "\"billerTranList\": [ { " +
//                            "\"mid\": \"470000099309183\", " +
//                            "\"tid\": \"39287941\", " +
//                            "\"trans_status\": \"SUCCESS\", " +
//                            "\"tran_amt\": \"8.00\", " +
//                            "\"tran_date\": \"04112024\", " +
//                            "\"tran_time\": \"111547\", " +
//                            "\"rrn\": \"TXN200000758157\", " +
//                            "\"ft_number\": \"24110439287941111547\", " +
//                            "\"cust_id\": \"9716325888\", " +
//                            "\"pay_method\": \"ALP\", " +
//                            "\"authAmt\": \"8.00\", " +
//                            "\"refundAmt\": \"0\", " +
//                            "\"balanceAmt\": \"8.00\", " +
//                            "\"field1\": \"Offline\", " +
//                            "\"field3\": \"39287941041124111547\", " +
//                            "\"field7\": \"0.0\", " +
//                            "\"field9\": \"0\", " +
//                            "\"paramList\": [ { \"param\": \"\", \"param_lit\": \"PUMP_NO\" }, " +
//                            "{ \"param\": \"\", \"param_lit\": \"localMPDId\" }, " +
//                            "{ \"param\": \"\", \"param_lit\": \"NOZZLE\" }, " +
//                            "{ \"param\": \"\", \"param_lit\": \"localProductID\" }, " +
//                            "{ \"param\": \"9716325888\", \"param_lit\": \"Customer Mobile\" }, " +
//                            "{ \"param\": \"158698\", \"param_lit\": \"SAP CODE\" }, " +
//                            "{ \"param\": \"SmartDrive\", \"param_lit\": \"ProgramId\" }, " +
//                            "{ \"param\": \"\", \"param_lit\": \"WalletId\" }, " +
//                            "{ \"param\": \"\", \"param_lit\": \"odometerReading\" }, " +
//                            "{ \"param\": \"2\", \"param_lit\": \"QUANTITY\" }, " +
//                            "{ \"param\": \"pos test1\", \"param_lit\": \"MERCH NAME\" }, " +
//                            "{ \"param\": \"\", \"param_lit\": \"Attendant ID\" }, " +
//                            "{ \"param\": \"4\", \"param_lit\": \"UNIT_PRICE\" }, " +
//                            "{ \"param\": \"\", \"param_lit\": \"Vehicle ID\" }, " +
//                            "{ \"param\": \"\", \"param_lit\": \"CUSTOMER_DISC\" }, " +
//                            "{ \"param\": \"20241104111547\", \"param_lit\": \"FCC TIMESTAMP\" }, " +
//                            "{ \"param\": \"\", \"param_lit\": \"discountID\" } ] } ], " +
//                            "\"output\": [ { \"alpTransactionId\": \"TXN200000758157\", " +
//                            "\"requiredPetromilesPoints\": 0, " +
//                            "\"print\": { \"alpTransactionId\": \"TXN200000758157\", " +
//                            "\"originalAlpTransactionId\": \"\", " +
//                            "\"ROName\": \"BP-INDIRAPURAM BHARAT PETROLEUM CORPN LTD.\", " +
//                            "\"roMobileNo\": \"9661007069\", " +
//                            "\"reportID\": \"\", " +
//                            "\"originalClientTxnId\": \"\", " +
//                            "\"mobileNumber\": \"9716325888\", " +
//                            "\"dealerID\": \"0000158698\", " +
//                            "\"txnProduct\": \"\", " +
//                            "\"discount\": \"0.0\", " +
//                            "\"customerCardNumber\": \"FC3000223523\", " +
//                            "\"roCity\": \"INDIRAPURAM\", " +
//                            "\"fuelAmount\": \"8\", " +
//                            "\"petroMilesEarned\": \"\", " +
//                            "\"noOfRequestedCard\": \"\", " +
//                            "\"txnDiscount\": \"0.0\", " +
//                            "\"txnStatus\": \"Paid\", " +
//                            "\"amountPaid\": \"8.01\", " +
//                            "\"clientTxnId\": \"39287941041124111547\", " +
//                            "\"paymentReferenceNumber\": \"\", " +
//                            "\"programName\": \"SmartFleet\", " +
//                            "\"txnSource\": \"OTP\", " +
//                            "\"cardBalance\": \"0\", " +
//                            "\"vehicleNumber\": \"Sonu\", " +
//                            "\"voided\": false, " +
//                            "\"chargeSlipNumber\": \"\", " +
//                            "\"batchNumber\": \"\", " +
//                            "\"timestamp\": \"04/11/2024 11:15:57\", " +
//                            "\"customerDisclaimer\": \"\", " +
//                            "\"txnMEShare\": \"0.0\", " +
//                            "\"odometerReading\": \"\", " +
//                            "\"aposTerminalID\": \"26062024\", " +
//                            "\"netAmount\": \"8\", " +
//                            "\"chargeSlipFooter\": \"\", " +
//                            "\"txnMode\": \"CMS\", " +
//                            "\"customerAccountNumber\": \"FA3000170478\", " +
//                            "\"txnQuantity\": \"\", " +
//                            "\"customerName\": \"Rohit\", " +
//                            "\"txnBayId\": \"\", " +
//                            "\"productRate\": \"\", " +
//                            "\"currencyCode\": \"INR\", " +
//                            "\"chargeSlipHeader\": \"\", " +
//                            "\"merchantDisclaimer\": \"\", " +
//                            "\"reversed\": false, " +
//                            "\"tcsAmount\": \".01\", " +
//                            "\"txnAmount\": \"8.01\" }, " +
//                            "\"requiredPetromilesValue\": 0, " +
//                            "\"message\": \"Your pre-auth of Rs.8.01(Fuel Amt Rs:8.0 +TCS Amt Rs: .01) is successful. CMS balance is Rs.974596.03\" } ], " +
//                            "\"instId\": \"47\", " +
//                            "\"latitude\": \"0\", " +
//                            "\"longitude\": \"0\", " +
//                            "\"geotagRange\": \"10\", " +
//                            "\"hwSrNo\": \"2840552875\" }, " +
//                            "\"exp\": 1730699757, " +
//                            "\"iat\": 1730699157, " +
//                            "\"jti\": \"BPCL62854d3\" } }";

                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respCode = payLoad.getString("respCode");
                    String respDesc = payLoad.getString("respDesc");

                    if (respCode.equals("200")) {
                        progress.dismiss();
                        Log.d("validateOtpApiData", respCode);

                        JSONArray outputArray = payLoad.getJSONArray("output");

                        if (outputArray.length() > 0) {
                            JSONObject outputObject = outputArray.getJSONObject(0);
                            String alpTransactionId = outputObject.optString("alpTransactionId");
                            String requiredPetromilesPoints = outputObject.optString("requiredPetromilesPoints");
                            String message = outputObject.optString("message");

                            JSONObject printObject = outputObject.getJSONObject("print");
                            String ROName = printObject.optString("ROName");
                            String roCity = printObject.optString("roCity");
                            String roMobileNo = printObject.optString("roMobileNo");
                            String mobileNumber = printObject.optString("mobileNumber");
                            String alpTid = printObject.optString("aposTerminalID");
                            String alpTxnId = printObject.optString("alpTransactionId");
                            Log.d("alpTxnId", "apiResult: " + alpTxnId);
                            String alpSlipNo = printObject.optString("chargeSlipNumber");
                            String alpReportId = printObject.optString("reportID");
                            String alpType = printObject.optString("txnType","PreAuth");
                            String alpTxnSource = printObject.optString("txnSource");
                            String alpCustName = printObject.optString("customerName");
                            String alpAccNo = printObject.optString("customerAccountNumber");
                            String alpCardId = printObject.optString("customerCardNumber");
                            String alpVechCard = "";
                            String alpOdometer = printObject.optString("odometerReading");
                            String alpWallet = printObject.optString("txnMode");
                            String alpProduct = printObject.optString("txnProduct");
                            String alpRate = printObject.optString("productRate");
                            String alpVol = "";
                            String alpFuelAmount = printObject.optString("fuelAmount");
                            String alpTcsAmount = printObject.optString("tcsAmount");
                            String alpTxnAmount = printObject.optString("txnAmount");
                            String alpPmEarn = printObject.optString("petroMilesEarned");
                            String alpMeShare = printObject.optString("txnMEShare");
                            String alpCardBalance = printObject.optString("cardBalance");
                            String vehicleNumber = "";
                            if (printObject.has("vehicleNumber")) {
                                vehicleNumber = printObject.optString("vehicleNumber");
                            }

                            JSONArray billerTranList = payLoad.getJSONArray("billerTranList");
                            JSONObject transaction = billerTranList.getJSONObject(0);

                            String trans_status = transaction.optString("trans_status");

                            tran_date = transaction.optString("tran_date");
                            tran_time = transaction.optString("tran_time");

                            ft_number = transaction.optString("ft_number");
                            authAmt = transaction.optString("authAmt");
                            balanceAmt = transaction.optString("balanceAmt");

                            if (transaction.has("field6")) {
                                field6 = transaction.optString("field6");
                            }
                            if (transaction.has("field9")) {
                                field9 = transaction.optString("field9");
                            }

                            if (transaction.has("field7")) {
                                field7 = transaction.optString("field7");
                            }

                            String rrn = transaction.optString("rrn");

                            if (cngModel != null) {
                                cngModel.setRrn(rrn);
                                cngModel.setField3(field3);
                                cngModel.setField7(field7);
                                cngModel.setField9(field9);
                                cngModel.setField13("VC");

                                cngModel.setROName(ROName);
                                cngModel.setRoCity(roCity);
                                cngModel.setRoMobileNo(roMobileNo);
                                cngModel.setVehicleNumber(vehicleNumber);
                                cngModel.setAlpTid(alpTid);
                                cngModel.setAlpTxnId(alpTxnId);
                                cngModel.setAlpSlipNo(alpSlipNo);
                                cngModel.setAlpReportId(alpReportId);
                                cngModel.setAlpType(alpType);
                                cngModel.setAlpTxnSource(alpTxnSource);
                                cngModel.setAlpCustName(alpCustName);
                                cngModel.setAlpAccNo(alpAccNo);
                                cngModel.setAlpCardId(alpCardId);
                                cngModel.setAlpVechCard(cngModel.getVehicleNumber());
                                cngModel.setAlpOdometer(alpOdometer);
                                cngModel.setAlpWallet(alpWallet);
                                cngModel.setAlpProduct(alpProduct);
                                cngModel.setAlpRate(alpRate);
                                cngModel.setAlpVol(cngModel.getQty());
                                cngModel.setAlpFuelAmount(alpFuelAmount);
                                cngModel.setAlpTcsAmount(alpTcsAmount);
                                cngModel.setAlpTxnAmount(alpTxnAmount);
                                cngModel.setAlpPmEarn(alpPmEarn);
                                cngModel.setAlpMeShare(alpMeShare);
                                cngModel.setAlpCardBalance(alpCardBalance);
                            } else if (nfrModel != null) {
                                nfrModel.setRrn(rrn);
                                nfrModel.setField3(field3);
                                nfrModel.setField7(field7);
                                nfrModel.setField9(field9);
                                nfrModel.setField13("VC");

                                nfrModel.setROName(ROName);
                                nfrModel.setRoCity(roCity);
                                nfrModel.setRoMobileNo(roMobileNo);
                                nfrModel.setVehicleNumber(vehicleNumber);
                                nfrModel.setAlpTid(alpTid);
                                nfrModel.setAlpTxnId(alpTxnId);
                                nfrModel.setAlpSlipNo(alpSlipNo);
                                nfrModel.setAlpReportId(alpReportId);
                                nfrModel.setAlpType(alpType);
                                nfrModel.setAlpTxnSource(alpTxnSource);
                                nfrModel.setAlpCustName(alpCustName);
                                nfrModel.setAlpAccNo(alpAccNo);
                                nfrModel.setAlpCardId(alpCardId);
                                nfrModel.setAlpVechCard(nfrModel.getVehicleNumber());
                                nfrModel.setAlpOdometer(alpOdometer);
                                nfrModel.setAlpWallet(alpWallet);
                                nfrModel.setAlpProduct(alpProduct);
                                nfrModel.setAlpRate(alpRate);
                                nfrModel.setAlpVol(nfrModel.getQty());
                                nfrModel.setAlpFuelAmount(alpFuelAmount);
                                nfrModel.setAlpTcsAmount(alpTcsAmount);
                                nfrModel.setAlpTxnAmount(alpTxnAmount);
                                nfrModel.setAlpPmEarn(alpPmEarn);
                                nfrModel.setAlpMeShare(alpMeShare);
                                nfrModel.setAlpCardBalance(alpCardBalance);
                            } else {
                                onlineTxnModel.setRrn(rrn);
                                onlineTxnModel.setField3(field3);
                                onlineTxnModel.setField7(field7);
                                onlineTxnModel.setField9(field9);
                                onlineTxnModel.setField13("VC");

                                onlineTxnModel.setROName(ROName);
                                onlineTxnModel.setRoCity(roCity);
                                onlineTxnModel.setRoMobileNo(roMobileNo);
                                onlineTxnModel.setVehicleNumber(vehicleNumber);
                                onlineTxnModel.setAlpTid(alpTid);
                                onlineTxnModel.setAlpTxnId(alpTxnId);
                                onlineTxnModel.setField3(field3);
                                onlineTxnModel.setAlpSlipNo(alpSlipNo);
                                onlineTxnModel.setAlpReportId(alpReportId);
                                onlineTxnModel.setAlpType(alpType);
                                onlineTxnModel.setAlpTxnSource(alpTxnSource);
                                onlineTxnModel.setAlpCustName(alpCustName);
                                onlineTxnModel.setAlpAccNo(alpAccNo);
                                onlineTxnModel.setAlpCardId(alpCardId);
                                onlineTxnModel.setAlpVechCard(onlineTxnModel.getVehicleNumber());
                                onlineTxnModel.setAlpOdometer(alpOdometer);
                                onlineTxnModel.setAlpWallet(alpWallet);
                                onlineTxnModel.setAlpProduct(alpProduct);
                                onlineTxnModel.setAlpRate(alpRate);
                                onlineTxnModel.setAlpVol(onlineTxnModel.getQty());
                                onlineTxnModel.setAlpFuelAmount(alpFuelAmount);
                                onlineTxnModel.setAlpTcsAmount(alpTcsAmount);
                                onlineTxnModel.setAlpTxnAmount(alpTxnAmount);
                                onlineTxnModel.setAlpPmEarn(alpPmEarn);
                                onlineTxnModel.setAlpMeShare(alpMeShare);
                                onlineTxnModel.setAlpCardBalance(alpCardBalance);
                            }
                        }

                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            redirectToSuccessPage();
                        });

                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                binding.otp.setText("");
                                progress.dismiss();
                                Log.d("respCode", respCode);
                                MessagesDialog.showDialog(getActivity(), respDesc, 0, null, null);
                            }
                        });
                    }

                }
            } catch (JSONException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.dismiss();
                        Log.d("respCode", e.toString());
                        fileWrite(context, todayDate + ".txt", "validateOtpApi", e.toString());
                        MessagesDialog.showDialog(getActivity(), e.toString(), 0, null, null);
                    }
                });
            }
        } else {
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(context, "Server Time Out", 0, null, null);
                    });
                } else {
                    Log.d("cardTransactionStatusApiResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");

                    JSONArray billerTranListArray = payLoad.getJSONArray("billerTranList");
                    JSONObject billerTran = billerTranListArray.getJSONObject(0);
                    String trans_status = billerTran.getString("trans_status");
                    Log.d("trans_statusSS", trans_status);

                    // String rrn = billerTran.getString("rrn");

                    if (respCode.equals("200") && trans_status.equals("SUCCESS")) {
                        progress.dismiss();
                        String ft_number = billerTran.getString("ft_number");
                        JSONArray outputArray = payLoad.getJSONArray("output");
                        JSONObject outputArrayJSONObject = outputArray.getJSONObject(0);
                        field7 = billerTran.optString("field7");
                        field9 = billerTran.optString("field9");
                        rrn = billerTran.optString("rrn");
                        String ROName = outputArrayJSONObject.optString("ROName");
                        String roCity = outputArrayJSONObject.optString("roCity");
                        String roMobileNo = outputArrayJSONObject.optString("roMobileNo");
                        String alpTid = outputArrayJSONObject.optString("aposTerminalID");
                        String alpTxnId = outputArrayJSONObject.optString("alpTransactionId");
                        String alpSlipNo = outputArrayJSONObject.optString("chargeSlipNumber");
                        Log.d("alpSlipNo", alpSlipNo);
                        String alpReportId = outputArrayJSONObject.optString("reportID");
                        //String alpType = outputArrayJSONObject.getString("txnType");
                        String alpType = "PreAuth";
                        String alpTxnSource = outputArrayJSONObject.optString("txnSource");
                        String alpCustName = outputArrayJSONObject.optString("customerName");
                        String alpAccNo = outputArrayJSONObject.optString("customerAccountNumber");
                        String alpCardId = outputArrayJSONObject.optString("customerCardNumber");
                        String alpVechCard = "";
                        String alpOdometer = outputArrayJSONObject.optString("odometerReading");
                        String alpWallet = outputArrayJSONObject.optString("txnMode");
                        String alpProduct = outputArrayJSONObject.optString("txnProduct");
                        String alpRate = outputArrayJSONObject.optString("productRate");
                        String alpVol = "";
                        String alpFuelAmount = outputArrayJSONObject.optString("fuelAmount");
                        String alpTcsAmount = outputArrayJSONObject.optString("tcsAmount");
                        String alpTxnAmount = outputArrayJSONObject.optString("txnAmount");
                        String alpPmEarn = outputArrayJSONObject.optString("petroMilesEarned");
                        String alpMeShare = outputArrayJSONObject.optString("txnMEShare");
                        String alpCardBalance = outputArrayJSONObject.optString("cardBalance");
                        if (outputArrayJSONObject.has("vehicleNumber")) {
                            vehId = outputArrayJSONObject.optString("vehicleNumber");
                        }

                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            if (trans_status.equals("SUCCESS")) {
                                if (cngModel != null) {
                                    cngModel.setROName(ROName);
                                    cngModel.setRoCity(roCity);
                                    cngModel.setRoMobileNo(roMobileNo);
                                    cngModel.setField3(field3);
                                    cngModel.setField7(field7);
                                    cngModel.setField9(field9);
                                    cngModel.setField13("PC");
                                    cngModel.setVehicleNumber(vehId);
                                    cngModel.setTxnId(ft_number);
                                    cngModel.setRrn(rrn);
                                    cngModel.setAlpTid(alpTid);
                                    cngModel.setAlpTxnId(alpTxnId);
                                    cngModel.setAlpSlipNo(alpSlipNo);
                                    cngModel.setAlpReportId(alpReportId);
                                    cngModel.setAlpType(alpType);
                                    cngModel.setAlpTxnSource(alpTxnSource);
                                    cngModel.setAlpCustName(alpCustName);
                                    cngModel.setAlpAccNo(alpAccNo);
                                    cngModel.setAlpCardId(alpCardId);
                                    cngModel.setAlpVechCard(cngModel.getVehicleNumber());
                                    cngModel.setAlpOdometer(alpOdometer);
                                    cngModel.setAlpWallet(alpWallet);
                                    cngModel.setAlpProduct(alpProduct);
                                    cngModel.setAlpRate(alpRate);
                                    cngModel.setAlpVol(cngModel.getQty());
                                    cngModel.setAlpFuelAmount(alpFuelAmount);
                                    cngModel.setAlpTcsAmount(alpTcsAmount);
                                    cngModel.setAlpTxnAmount(alpTxnAmount);
                                    cngModel.setAlpPmEarn(alpPmEarn);
                                    cngModel.setAlpMeShare(alpMeShare);
                                    cngModel.setAlpCardBalance(alpCardBalance);
                                } else if (nfrModel != null) {
                                    nfrModel.setROName(ROName);
                                    nfrModel.setRoCity(roCity);
                                    nfrModel.setRoMobileNo(roMobileNo);
                                    nfrModel.setField3(field3);
                                    nfrModel.setField7(field7);
                                    nfrModel.setField9(field9);
                                    nfrModel.setField13("PC");
                                    nfrModel.setVehicleNumber(vehId);
                                    nfrModel.setRrn(rrn);
                                    nfrModel.setTxnId(ft_number);
                                    nfrModel.setAlpTid(alpTid);
                                    nfrModel.setAlpTxnId(alpTxnId);
                                    nfrModel.setAlpSlipNo(alpSlipNo);
                                    nfrModel.setAlpReportId(alpReportId);
                                    nfrModel.setAlpType(alpType);
                                    nfrModel.setAlpTxnSource(alpTxnSource);
                                    nfrModel.setAlpCustName(alpCustName);
                                    nfrModel.setAlpAccNo(alpAccNo);
                                    nfrModel.setAlpCardId(alpCardId);
                                    nfrModel.setAlpVechCard(nfrModel.getVehicleNumber());
                                    nfrModel.setAlpOdometer(alpOdometer);
                                    nfrModel.setAlpWallet(alpWallet);
                                    nfrModel.setAlpProduct(alpProduct);
                                    nfrModel.setAlpRate(alpRate);
                                    nfrModel.setAlpVol(nfrModel.getQty());
                                    nfrModel.setAlpFuelAmount(alpFuelAmount);
                                    nfrModel.setAlpTcsAmount(alpTcsAmount);
                                    nfrModel.setAlpTxnAmount(alpTxnAmount);
                                    nfrModel.setAlpPmEarn(alpPmEarn);
                                    nfrModel.setAlpMeShare(alpMeShare);
                                    nfrModel.setAlpCardBalance(alpCardBalance);
                                } else {
                                    onlineTxnModel.setROName(ROName);
                                    onlineTxnModel.setRoCity(roCity);
                                    onlineTxnModel.setRoMobileNo(roMobileNo);
                                    onlineTxnModel.setField3(field3);
                                    onlineTxnModel.setField7(field7);
                                    onlineTxnModel.setField9(field9);
                                    onlineTxnModel.setField13("PC");
                                    onlineTxnModel.setVehicleNumber(vehId);
                                    onlineTxnModel.setRrn(rrn);
                                    onlineTxnModel.setTxnId(ft_number);
                                    onlineTxnModel.setAlpTid(alpTid);
                                    onlineTxnModel.setAlpTxnId(alpTxnId);
                                    onlineTxnModel.setAlpSlipNo(alpSlipNo);
                                    onlineTxnModel.setAlpReportId(alpReportId);
                                    onlineTxnModel.setAlpType(alpType);
                                    onlineTxnModel.setAlpTxnSource(alpTxnSource);
                                    onlineTxnModel.setAlpCustName(alpCustName);
                                    onlineTxnModel.setAlpAccNo(alpAccNo);
                                    onlineTxnModel.setAlpCardId(alpCardId);
                                    onlineTxnModel.setAlpVechCard(onlineTxnModel.getVehicleNumber());
                                    onlineTxnModel.setAlpOdometer(alpOdometer);
                                    onlineTxnModel.setAlpWallet(alpWallet);
                                    onlineTxnModel.setAlpProduct(alpProduct);
                                    onlineTxnModel.setAlpRate(alpRate);
                                    onlineTxnModel.setAlpVol(onlineTxnModel.getQty());
                                    onlineTxnModel.setAlpFuelAmount(alpFuelAmount);
                                    onlineTxnModel.setAlpTcsAmount(alpTcsAmount);
                                    onlineTxnModel.setAlpTxnAmount(alpTxnAmount);
                                    onlineTxnModel.setAlpPmEarn(alpPmEarn);
                                    onlineTxnModel.setAlpMeShare(alpMeShare);
                                    onlineTxnModel.setAlpCardBalance(alpCardBalance);
                                }
                                redirectToSuccessPage();
                            } else {
                                MessagesDialog.showDialog(context, trans_status, 0, null, null);

                                //  Toast.makeText(context, trans_status + " - " + respCode, Toast.LENGTH_SHORT).show();
                            }
                        });

                    } else {
                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            Intent intent = new Intent();
                            Log.d("TAG", "apiResult: hhhhhheyy");
                            if (cngModel != null) {
                                intent = new Intent(getActivity(), CngPaymentActivity.class);
                                intent.putExtra("cngModel", cngModel);
                                intent.putExtra("Insertcard", "Insertcard");
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                Log.d("TAG", "cngModel: hhhhhheyy");

                            } else if (onlineTxnModel != null) {
                                if (isTxnOnline.equals("no")) {
                                    intent = new Intent(getActivity(), PaymentActivity.class);
                                    intent.putExtra("onlineTxnModel", onlineTxnModel);
                                    intent.putExtra("isTxnOnline", "isTxnOnline");
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                } else {
                                    intent = new Intent(getActivity(), PaymentActivity.class);
                                    intent.putExtra("onlineTxnModel", onlineTxnModel);
                                    intent.putExtra("Insertcard", "Insertcard");
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                }
                            } else if (nfrModel != null) {
                                intent = new Intent(getActivity(), NfrPaymentActivity.class);
                                intent.putExtra("nfrModel", nfrModel);
                                intent.putExtra("Insertcard", "Insertcard");
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                Log.d("TAG", "nfrModel: hhhhhheyy");
                            }

                            MessagesDialog.showDialog(context, respDesc, 0, intent, null);

                        });
                    }
                }
            } catch (JSONException e) {
                fileWrite(context, todayDate + ".txt", "getshiftsummaryResponse", e.toString());
                getActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    Intent intent = new Intent();
                    Log.d("TAG", "apiResult: hhhhhheyy");
                    if (cngModel != null) {
                        intent = new Intent(getActivity(), CngPaymentActivity.class);
                        intent.putExtra("cngModel", cngModel);
                        intent.putExtra("Insertcard", "Insertcard");
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        Log.d("TAG", "cngModel: hhhhhheyy");

                    } else if (onlineTxnModel != null) {
                        if (isTxnOnline.equals("no")) {
                            intent = new Intent(getActivity(), PaymentActivity.class);
                            intent.putExtra("onlineTxnModel", onlineTxnModel);
                            intent.putExtra("isTxnOnline", "isTxnOnline");
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        } else {
                            intent = new Intent(getActivity(), PaymentActivity.class);
                            intent.putExtra("onlineTxnModel", onlineTxnModel);
                            intent.putExtra("Insertcard", "Insertcard");
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        }
                    } else if (nfrModel != null) {
                        intent = new Intent(getActivity(), NfrPaymentActivity.class);
                        intent.putExtra("nfrModel", nfrModel);
                        intent.putExtra("Insertcard", "Insertcard");
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        Log.d("TAG", "nfrModel: hhhhhheyy");
                    }
                    MessagesDialog.showDialog(context, e.toString(), 0, intent, null);

                    // Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
                });
            }
        }
    }

    private void redirectToSuccessPage() {
        if (cngModel != null) {
            Log.d("cngModellllllllll", "");
            Intent intent = new Intent(context, CngSuccessActivity.class);
            intent.putExtra("cngModel", cngModel);
            startActivity(intent);
        } else if (nfrModel != null) {
            Log.d("nfrModelllllllllll", "");
            Intent intent = new Intent(context, NfrSuccessActivity.class);
            intent.putExtra("nfrModel", nfrModel);
            startActivity(intent);
        } else {
            Log.d("onlineTxnModellllll", "");
            Intent intent = new Intent(context, SuccessActivity.class);
            intent.putExtra("onlineTxnModel", onlineTxnModel);
            startActivity(intent);
        }

    }

    private void redirectToFailedPage() {
        if (cngModel != null) {
            TxnFailFragment fragment = new TxnFailFragment();
            Bundle bundle = new Bundle();
            bundle.putString("txnStatus", "Transaction Cancelled!");
            bundle.putParcelable("cngModel", cngModel);
            ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);

        } else if (onlineTxnModel != null) {
            TxnFailFragment fragment = new TxnFailFragment();
            Bundle bundle = new Bundle();
            bundle.putString("txnStatus", "Transaction Cancelled!");
            bundle.putParcelable("onlineTxnModel", onlineTxnModel);
            ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);

        } else if (nfrModel != null) {
            TxnFailFragment fragment = new TxnFailFragment();
            Bundle bundle = new Bundle();
            bundle.putString("txnStatus", "Transaction Cancelled!");
            bundle.putParcelable("nfrModel", nfrModel);
            ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);

        }
    }


    private void redirectToTimeOutPage() {
        if (cngModel != null) {
            TxnFailFragment fragment = new TxnFailFragment();
            Bundle bundle = new Bundle();
            bundle.putString("txnStatus", "Transaction TimeOut!");
            bundle.putParcelable("cngModel", cngModel);
            ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);

        } else if (onlineTxnModel != null) {
            TxnFailFragment fragment = new TxnFailFragment();
            Bundle bundle = new Bundle();
            bundle.putString("txnStatus", "Transaction TimeOut!");
            bundle.putParcelable("onlineTxnModel", onlineTxnModel);
            ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);

        } else if (nfrModel != null) {
            TxnFailFragment fragment = new TxnFailFragment();
            Bundle bundle = new Bundle();
            bundle.putString("txnStatus", "Transaction TimeOut!");
            bundle.putParcelable("nfrModel", nfrModel);
            ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);

        }
    }


    public void hideKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        MessagesDialog.dismissDialog();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

}