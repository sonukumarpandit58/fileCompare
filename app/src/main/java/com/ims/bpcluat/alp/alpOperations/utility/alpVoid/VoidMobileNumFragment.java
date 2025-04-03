package com.ims.bpcluat.alp.alpOperations.utility.alpVoid;

import static android.app.Activity.RESULT_OK;
import static com.ims.bpcluat.Helper.appVersion;
import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.getCurrentDateTime;

import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
//import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.sapCode;
//import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.Helper.username;
import static com.ims.bpcluat.Helper.version;
import static com.ims.bpcluat.helper.ApiHelper.alpEndpoint;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.fiserv.alpsdk.data.AlpRequest;
import com.fiserv.alpsdk.wrapper.AlpApi;
import com.ims.bpcluat.Helper;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.alp.alpConfiguration.ConfigurationsFragment;
import com.ims.bpcluat.databinding.FragmentVoidMobileNumBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.model.AlpModels.VoidModels.AlpTxnModel;
import com.ims.bpcluat.model.AlpModels.VoidModels.Output;
import com.ims.bpcluat.model.AlpModels.VoidModels.TxnList;
import com.ims.bpcluat.ufill.VoucherRedeemActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class VoidMobileNumFragment extends Fragment implements ApiHelper.NetworkingApiCallBack {

    FragmentVoidMobileNumBinding binding;
    AlpTxnModel alpTxnModel = new AlpTxnModel();
    ApiHelper api;
    private static final int CARD = 1;
    private static final String VOID_PIN = "VOID_PIN";
    String amount = "", mobileNum = "", amountInPaise = "";
    String pumpNo = "", nozzleNo = "", localMPDId = "", localProductID = "", qty = "", vehId = "", unitAmt = "";
    String field1 = "", field3 = "", txnId = "", clientTxnId = "", fcctxnID = "";
    String programID = "", walletID = "", odometerReading = "", quantityLitres = "";
    private CountDownTimer countDownTimer;


    ProgressDialog progress;
    Context context;
    String checkResult = "";
    String mobileNumber = "";
    boolean otpSent = true;
    String message = "";
    String requestCheck = "no";

//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
//    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVoidMobileNumBinding.inflate(inflater, container, false);

        context = getActivity();
        api = new ApiHelper();
        hideKeyboard();


        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new AlpVoidFragment());
            }
        });

        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateFields();
            }
        });

        clientTxnId = tid + getCurrentDateTime();

        binding.physicalCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doVoidCard();
            }
        });

        return binding.getRoot();
    }

    private void validateFields() {
        String mobileNum = binding.mobileNum.getText().toString().trim();
        String otp = binding.otp.getText().toString().trim();
        if (otpSent) {
            if (TextUtils.isEmpty(mobileNum)) {
                binding.mobileNum.setError("Please enter Mobile Number");
                binding.mobileNum.requestFocus();
                return;
            }
            initiateOtpApi(mobileNum);
        } else {
            if (TextUtils.isEmpty(otp)) {
                binding.otp.setError("Please enter OTP");
                binding.otp.requestFocus();
                return;
            }
            validateOtpApi(otp);
        }
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
            jsonObject.put("reportType", "void");
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
            api.setApiCallBack(VoidMobileNumFragment.this);
        } catch (JSONException e) {
            Log.d("initiateOtpApi", e.toString());
        }
    }

    private void validateOtpApi(String otp) {
        requestCheck = "yes";
        checkResult = "validateOtpApi";
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
            jsonObject.put("txnType", "ATF");
            jsonObject.put("tranChannel", "VC");
            jsonObject.put("dateTime", requestDate() + requestTime());
            jsonObject.put("mobNo", mobileNumber);
            jsonObject.put("password", otp);
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
            api.setApiCallBack(VoidMobileNumFragment.this);
        } catch (JSONException e) {
            Log.d("validateOtpApi", e.toString());
        }
    }

    private void validatePhysicalStatusApi() {
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
            jsonObject.put("txnType", "ATF");
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
            Log.d("validateMangerPinApi", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(VoidMobileNumFragment.this);
        } catch (JSONException e) {
            Log.d("validatePhysicalStatusApi", e.toString());

        }
    }

    public void apiResult(String res, String apiName) {
        if (checkResult.equals("initiateOtpApi")) {
            try {
                if (res.equals("Server Time Out")) {
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
                                try {
                                    mobileNumber = payLoad.getString("mobNo");
                                    binding.mobileNum.setVisibility(View.GONE);
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
                                            binding.timerTxt.setText("Time's up!");
                                            getActivity().runOnUiThread(() -> {
                                                if(requestCheck.equals("no")){
                                                    ((SideBarActivity) requireActivity()).loadFragement(new AlpVoidFragment());
                                                }else{
                                                    Log.d("requestCheck", "onFinish: "+requestCheck);
                                                }
                                            });
                                        }
                                    }.start();
                                } catch (JSONException e) {
                                    Log.d("JSONException", "run: " + e);
                                }

                            }
                        });


                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                MessagesDialog.showDialog(getActivity(), respDesc , 0,null, null);
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
                        MessagesDialog.showDialog(getActivity(), e.toString(), 0,null, null);
                    }
                });
            }
        }
        else if (checkResult.equals("validateOtpApi")) {
            try {
                if (res.equals("Server Time Out")) {
                    progress.dismiss();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                        }
                    });
                } else {
                    Log.d("validateOtpApiResponserr = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respCode = payLoad.getString("respCode");
                    String respDesc = payLoad.getString("respDesc");

                    String id = payLoad.getString("id");
                    String txnId = payLoad.getString("txnId");
                    String mobNo = payLoad.getString("mobNo");

                    alpTxnModel.setId(id);
                    alpTxnModel.setTxnId(txnId);
                    alpTxnModel.setMobNo(mobNo);

                    if (respCode.equals("200")) {
                        progress.dismiss();
                        Log.d("validateOtpApiData", respCode);

                        JSONArray outputArray = payLoad.getJSONArray("output");
                        List<Output> outputList = new ArrayList<>();

                        for (int i = 0; i < outputArray.length(); i++) {
                            JSONObject outputObj = outputArray.getJSONObject(i);
                            Output output = new Output();
                            output.setStatusMessage(outputObj.getString("statusMessage"));

                            JSONArray txnListArray = outputObj.getJSONArray("txnList");
                            List<TxnList> txnLists = new ArrayList<>();

                            for (int j = 0; j < txnListArray.length(); j++) {
                                JSONObject txnObj = txnListArray.getJSONObject(j);
                                TxnList txnList = new TxnList();
                                txnList.setAlpTransactionId(txnObj.getString("alpTransactionId"));
                                txnList.setTxnAmount(txnObj.getString("txnAmount"));
                                txnList.setClientTxnId(txnObj.getString("clientTxnId"));

                                txnLists.add(txnList);
                            }

                            output.setTxnList(txnLists);
                            outputList.add(output);
                        }

                        alpTxnModel.setOutput(outputList);

                        ArrayList<AlpTxnModel> alpTxnModelArrayList = new ArrayList<>();
                        alpTxnModelArrayList.add(alpTxnModel);

                        Intent intent = new Intent(getActivity(), AlpVoidTransactions.class);
                        intent.putExtra("alpTxnModel", alpTxnModelArrayList);

                        startActivity(intent);

                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                Log.d("respCode", respCode);
                                binding.otp.setText("");
                                MessagesDialog.showDialog(getActivity(), respDesc, 0,null, null);
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

                        MessagesDialog.showDialog(getActivity(), e.toString(), 0,null, null);
                    }
                });
            }
        } else {
            try {
                if (res.equals("Server Time Out")) {
                    progress.dismiss();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                        }
                    });
                } else {
                    Log.d("validateOtpApiResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respCode = payLoad.getString("respCode");
                    String respDesc = payLoad.getString("respDesc");

                    String id = payLoad.getString("id");
                    String txnId = payLoad.getString("txnId");
                    String mobNo = payLoad.getString("mobNo");

                    alpTxnModel.setId(id);
                    alpTxnModel.setTxnId(txnId);
                    alpTxnModel.setMobNo(mobNo);

                    if (respCode.equals("200")) {
                        progress.dismiss();
                        Log.d("validateOtpApiData", respCode);

                        JSONArray outputArray = payLoad.getJSONArray("output");
                        List<Output> outputList = new ArrayList<>();

                        for (int i = 0; i < outputArray.length(); i++) {
                            JSONObject outputObj = outputArray.getJSONObject(i);
                            Output output = new Output();
//                            output.setStatusMessage(outputObj.getString("statusMessage"));

                            JSONArray txnListArray = outputObj.getJSONArray("txnList");
                            List<TxnList> txnLists = new ArrayList<>();

                            for (int j = 0; j < txnListArray.length(); j++) {
                                JSONObject txnObj = txnListArray.getJSONObject(j);
                                TxnList txnList = new TxnList();
                                txnList.setAlpTransactionId(txnObj.getString("alpTransactionId"));
                                txnList.setTxnAmount(txnObj.getString("txnAmount"));
                                txnList.setClientTxnId(txnObj.getString("clientTxnId"));

                                txnLists.add(txnList);
                            }

                            output.setTxnList(txnLists);
                            outputList.add(output);
                        }

                        alpTxnModel.setOutput(outputList);

                        ArrayList<AlpTxnModel> alpTxnModelArrayList = new ArrayList<>();
                        alpTxnModelArrayList.add(alpTxnModel);

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(getActivity(), AlpVoidTransactions.class);
                                intent.putExtra("alpTxnModel", alpTxnModelArrayList);
                                startActivity(intent);
                            }
                        });

                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                Log.d("respCode", respCode);
                                MessagesDialog.showDialog(getActivity(), respDesc, 0,null, null);
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
                        MessagesDialog.showDialog(getActivity(), e.toString(), 0,null, null);
                    }
                });
            }
        }
    }

    private void doVoidCard() {
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
            alpRequest.setReqType(VOID_PIN);
            alpRequest.setTxnId(txnId);
            alpRequest.setTxnType("APC");
            alpRequest.setUserName(username);
            alpRequest.setWalletID("");
            Log.d("alpRequestParams", alpRequest.toString());

            AlpApi.doVoidPinRequest((Activity) context, CARD, alpRequest);

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
                            Log.d("voidCardResponse", response);
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
                                        validatePhysicalStatusApi();
                                    });
                                } else {
                                      message = jsonResponse.getString("message");
                                    MessagesDialog.showDialog(context, message, 0,null, null);

                                }
                            } catch (JSONException e) {
                                Log.e("loyaltyCardResponse", "Error parsing JSON: " + e.getMessage());
                            }
                        } else {
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
            Log.d("loyaltyCardResponse@", e.toString());
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

    public void hideKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}