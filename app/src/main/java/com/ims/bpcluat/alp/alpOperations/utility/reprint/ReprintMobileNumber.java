package com.ims.bpcluat.alp.alpOperations.utility.reprint;

import static android.app.Activity.RESULT_OK;
import static com.ims.bpcluat.Helper.appVersion;
import static com.ims.bpcluat.Helper.cardChargeslipDate;
import static com.ims.bpcluat.Helper.cardChargeslipTime;
import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.getCurrentDateTime;

import static com.ims.bpcluat.Helper.logLongMessage;
import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
//import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.sapCode;
//import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.username;
import static com.ims.bpcluat.Helper.version;
import static com.ims.bpcluat.helper.ApiHelper.alpEndpoint;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.fiserv.alpsdk.data.AlpRequest;
import com.fiserv.alpsdk.wrapper.AlpApi;
import com.ims.bpcluat.Helper;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.alp.alpOperations.utility.UtilityFragment;
import com.ims.bpcluat.alp.alpOperations.utility.alpVoid.AlpVoidFragment;
import com.ims.bpcluat.alp.alpOperations.utility.alpVoid.AlpVoidTransactions;
import com.ims.bpcluat.alp.alpOperations.utility.alpVoid.VoidMobileNumFragment;
import com.ims.bpcluat.databinding.FragmentReprintMobileNumberBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.model.AlpModels.ReprintTxnModel;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProductModel;
import com.ims.bpcluat.model.AlpModels.VoidModels.AlpTxnModel;
import com.ims.bpcluat.model.AlpModels.VoidModels.Output;
import com.ims.bpcluat.model.AlpModels.VoidModels.TxnList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ReprintMobileNumber extends Fragment implements ApiHelper.NetworkingApiCallBack {
    ApiHelper api;
    ProgressDialog progress;
    Context context;
    FragmentReprintMobileNumberBinding binding;
    String message = "", id = "", mobNo = "";
    private static final int CARD = 1;
    private static final String REPRINT_PIN = "REPRINT_PIN";
    String amount = "", mobileNum = "", amountInPaise = "";
    String pumpNo = "", nozzleNo = "", localMPDId = "", localProductID = "", qty = "", vehId = "", unitAmt = "";
    String field1 = "", field3 = "", txnId = "", clientTxnId = "", fcctxnID = "";
    String programID = "", walletID = "", odometerReading = "", quantityLitres = "";
    String checkResult = "";

    public ReprintMobileNumber() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReprintMobileNumberBinding.inflate(inflater, container, false);
        context = getActivity();
        api = new ApiHelper();

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new UtilityFragment());
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
                doRePrintPinCardRequest();
            }
        });

        clientTxnId =  tid + getCurrentDateTime();
        return binding.getRoot();
    }

    private void validateFields() {
        String mobileNum = binding.mobileNum.getText().toString().trim();
        if (TextUtils.isEmpty(mobileNum)) {
            binding.mobileNum.setError("Please enter Mobile Number");
            binding.mobileNum.requestFocus();
            return;
        }
        initiateOtpApi(mobileNum);
    }

    private void initiateOtpApi(String mobileNum) {
        checkResult = "initiateOtpApi";
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while fetching details...");
        progress.setCancelable(false);
        progress.show();

        String url = alpEndpoint;
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("channel", channelName);
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("userName", username);
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnType", "ATO");
            jsonObject.put("reportType", "reprint");
            jsonObject.put("tranChannel", "VC");
            jsonObject.put("dateTime", requestDate() + requestTime());
            jsonObject.put("mobNo", mobileNum);
            jsonObject.put("txnId", Helper.createTxnIdForOfflineTxn());
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", version);

            Log.d("validateMangerPinApi", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(ReprintMobileNumber.this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void doRePrintPinCardRequest() {
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
            alpRequest.setReqType(REPRINT_PIN);
            alpRequest.setTxnId(txnId);
            alpRequest.setTxnType("APC");
            alpRequest.setUserName(username);
            alpRequest.setWalletID("");
            
            Log.d("alpRequestParams", alpRequest.toString());
            AlpApi.doRePrintPinRequest((Activity) context, CARD, alpRequest);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void physicalTransactionReprintAPI() {
        checkResult = "validatePhysicalStatusApi";
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while fetching details...");
        progress.setCancelable(false);
        progress.show();

        String url = alpEndpoint;
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("channel", channelName);
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("userName", username);
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnType", "ATP");
            jsonObject.put("tranChannel", "PC");
            jsonObject.put("dateTime", requestDate() + requestTime());
            jsonObject.put("txnId", clientTxnId);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", version);

            Log.d("validatePhysicalRequest", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(ReprintMobileNumber.this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void apiResult(String res, String apiName) {
        if(checkResult.equals("initiateOtpApi")){
            try {
                if (res.equals("Server Time Out")) {
                    Log.d("timeout_problem", res);
                    progress.dismiss();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                        }
                    });
                } else {
                    Log.d("initiateOtpApiResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respCode = payLoad.getString("respCode");
                    String respDesc = payLoad.getString("respDesc");

                    if (respCode.equals("200")) {
                        JSONArray outputArray = payLoad.getJSONArray("output");


                        if (outputArray.length() > 0) {
                            JSONObject outputObject = outputArray.getJSONObject(0);
                            message = outputObject.getString("message");
                        }

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                Log.d("initiateOtpData@#", payLoad.toString());
                                Log.d("initiateOtpData@@@@", respCode.toString());
                                try {
                                    id = payLoad.getString("id");
                                    mobNo = payLoad.getString("mobNo");

                                    ReprintOtpFragment fragment = new ReprintOtpFragment();
                                    Bundle bundle = new Bundle();
                                    bundle.putString("id", id);
                                    bundle.putString("message", message);
                                    bundle.putString("mobNo", mobNo);
                                    fragment.setArguments(bundle);
                                    ((SideBarActivity) context).loadFragement(fragment);

                                } catch (JSONException e) {
                                    Log.d("TAG", "run: " +e);
                                }

                            }
                        });


                    } else {
                        Log.d("reprintRespCode", respCode);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                MessagesDialog.showDialog(context, respDesc, 0,null, null);
                            }
                        });
                    }

                }
            } catch (JSONException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.dismiss();
                        Log.d("Exception = ", e.toString());
                        MessagesDialog.showDialog(context, e.toString(), 0,null, null);
                    }
                });
            }
        }
        else {
            try {
                if (res.equals("Server Time Out")) {
                    Log.d("timeout_problem", res);
                    progress.dismiss();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                        }
                    });
                } else {
                    Log.d("validateOtpApiResponse = ", res);
                    logLongMessage("reprintResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respCode = payLoad.getString("respCode");
                    String respDesc = payLoad.getString("respDesc");
                    if (respCode.equals("200")) {
                        Log.d("fetchResponseCode3", respCode);

                        JSONArray outputArray = payLoad.getJSONArray("output");
                        String txnId = payLoad.getString("txnId");
                        String mobNo = payLoad.getString("mobNo");
                        String dateTime = payLoad.getString("dateTime");

                        String date = cardChargeslipDate(dateTime);
                        String time = cardChargeslipTime(dateTime);

                        JSONObject outputObject = outputArray.getJSONObject(0);
                        JSONArray txnListArray = outputObject.getJSONArray("txnList");

                        List<ReprintTxnModel> reprintTxnModelList = new ArrayList<>();

                        for (int i = 0; i < txnListArray.length(); i++) {
                            JSONObject txnObject = txnListArray.getJSONObject(i);

                            ReprintTxnModel txnModel = new ReprintTxnModel(
                                    txnObject.getString("alpTransactionId"),
                                    txnObject.optString("originalAlpTransactionId", ""),
                                    txnObject.getString("ROName"),
                                    txnObject.getString("roMobileNo"),
                                    txnObject.getString("reportID"),
                                    txnObject.optString("originalClientTxnId", ""),
                                    txnObject.getString("dealerID"),
                                    txnObject.getString("mobileNumber"),
                                    txnObject.getString("txnProduct"),
                                    txnObject.getString("discount"),
                                    txnObject.getString("txnType"),
                                    txnObject.getString("customerCardNumber"),
                                    txnObject.getString("roCity"),
                                    txnObject.getString("fuelAmount"),
                                    txnObject.getString("petroMilesEarned"),
                                    txnObject.optString("noOfRequestedCard", ""),
                                    txnObject.getString("txnDiscount"),
                                    txnObject.getString("txnStatus"),
                                    txnObject.getString("amountPaid"),
                                    txnObject.getString("clientTxnId"),
                                    txnObject.optString("paymentReferenceNumber", ""),
                                    txnObject.getString("programName"),
                                    txnObject.getString("txnSource"),
                                    txnObject.getString("cardBalance"),
                                    txnObject.getString("vehicleNumber"),
                                    txnObject.getBoolean("voided"),
                                    txnObject.getString("chargeSlipNumber"),
                                    txnObject.optString("batchNumber", ""),
                                    txnObject.getString("timestamp"),
                                    txnObject.optString("customerDisclaimer", ""),
                                    txnObject.getDouble("txnMEShare"),
                                    txnObject.getString("odometerReading"),
                                    txnObject.getString("aposTerminalID"),
                                    txnObject.getString("netAmount"),
                                    txnObject.optString("chargeSlipFooter", ""),
                                    txnObject.getString("txnMode"),
                                    txnObject.getString("customerAccountNumber"),
                                    txnObject.getString("txnQuantity"),
                                    txnObject.getString("customerName"),
                                    txnObject.getString("txnBayId"),
                                    txnObject.getString("productRate"),
                                    txnObject.getString("currencyCode"),
                                    txnObject.optString("chargeSlipHeader", ""),
                                    txnObject.optString("merchantDisclaimer", ""),
                                    txnObject.getBoolean("reversed"),
                                    txnObject.getString("tcsAmount"),
                                    txnObject.getString("txnAmount"),
                                    mobNo,
                                    date,
                                    time,
                                    txnId
                            );

                            reprintTxnModelList.add(txnModel);
                        }

                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            Log.d("programResponse = ", res);

                            ReprintTxnList fragment = new ReprintTxnList();
                            Bundle bundle = new Bundle();
                            bundle.putString("txnId", txnId);
                            bundle.putSerializable("reprintTxnModelList",(Serializable) reprintTxnModelList);
                            fragment.setArguments(bundle);
                            ((SideBarActivity) requireActivity()).loadFragement(fragment);
                        });

                    } else {
                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            Log.d("programResponseException = ", res);
                            MessagesDialog.showDialog(context, respDesc, 0,null, null);
                        });
                    }

                }
            } catch (JSONException e) {

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.dismiss();
                        Log.d("reprintRespCode", "Exception error");
                        MessagesDialog.showDialog(context, e.toString(), 0,null, null);
                    }
                });
            }
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        try {
            Log.d("reprintResponseTest1", String.valueOf(data));
            super.onActivityResult(requestCode, resultCode, data);
            if (data != null) {
                if (requestCode == CARD) {
                    if (resultCode == RESULT_OK) {
                        Bundle extras = data.getExtras();
                        if (extras != null) {
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
                            Log.d("reprintResponse", response);
                            try {
                                JSONObject jsonResponse = new JSONObject(response);
                                String code = String.valueOf(jsonResponse.getInt("code"));
                                if(Helper.isAlpCodeExistsForStatusApiCall(jsonResponse.getString("code"))){

                                    JSONObject dataObject = jsonResponse.getJSONObject("data");
                                    JSONObject physicalCard = dataObject.getJSONObject("PhysicalCard");
                                    String cardName = physicalCard.getString("cardName");

                                    Log.d("CardDetails", "Card Name: " + cardName);

                                    JSONObject alpRequest = dataObject.getJSONObject("alpRequest");
                                    txnId = alpRequest.getString("txnId");
                                    programID = alpRequest.getString("programID");
                                    walletID = alpRequest.getString("walletID");
                                    odometerReading = alpRequest.getString("odometerReading");
                                    quantityLitres = alpRequest.getString("quantityLitres");

                                    Log.d("AlpRequestDetails", "Transaction ID: " + txnId);

                                    getActivity().runOnUiThread(() -> {
                                        progress.dismiss();
                                        physicalTransactionReprintAPI();
                                    });
                                } else {
                                    Log.e("loyaltyCardResponse1", "Error parsing JSON: " + code);
                                    message = jsonResponse.getString("message");
                                    MessagesDialog.showDialog(context, message, 0,null, null);
                                }
                            } catch (JSONException e) {
                                Log.e("loyaltyCardResponse2", "Error parsing JSON: " + e.getMessage());
                            }
                        } else {
                            Log.d("loyaltyCardResponse3", "No response received");
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
                Log.d("loyaltyCardResponse@", "No intent data received");
            }
        } catch (Exception e) {
            Log.d("loyaltyCardResponse@", e.toString());
        }
    }
}