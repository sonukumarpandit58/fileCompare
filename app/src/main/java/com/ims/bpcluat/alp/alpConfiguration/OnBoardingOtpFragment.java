package com.ims.bpcluat.alp.alpConfiguration;

import static com.ims.bpcluat.Helper.appVersion;
import static com.ims.bpcluat.Helper.fileWrite;

import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.Helper.username;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.databinding.FragmentOnBoardingOtpBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class OnBoardingOtpFragment extends Fragment implements ApiHelper.NetworkingApiCallBack {
    FragmentOnBoardingOtpBinding binding;
    ProgressDialog progress;
    ApiHelper api;
    String txnId = "", dateTime = "", otppass;
    String validateotpApiCall = "";
    private String txnid;
    private CountDownTimer countDownTimer;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOnBoardingOtpBinding.inflate(inflater, container, false);
        api = new ApiHelper();

        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((SideBarActivity) requireActivity()).loadFragement(new ConfigurationsFragment());
            }
        });

        countDownTimer = new CountDownTimer(45000, 1000) {
            public void onTick(long millisUntilFinished) {
                binding.timerTxt.setText(millisUntilFinished / 1000 + " seconds remaining");
            }
            public void onFinish() {
                binding.timerTxt.setText("Time's up!");
                getActivity().runOnUiThread(() -> {
                    ((SideBarActivity) requireActivity()).loadFragement(new ConfigurationsFragment());
                });
            }
        }.start();

        // showAlertDialoghome();

        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String otp = binding.etOtp.getText().toString().trim();
                if (otp.isEmpty()) {
                    binding.etOtp.setError("Please Enter OTP");
                    binding.etOtp.requestFocus();
                } else {
                    otppass = String.valueOf(binding.etOtp.getText());
                    hideKeyboard();
                    onBoardingConfirmApi();

                }

            }
        });

        Bundle bundle = getArguments();
        if (bundle != null) {
            String otpMessage = bundle.getString("otp_message");
            txnid = bundle.getString("txnId");
            dateTime = bundle.getString("dateTime");
            assert otpMessage != null;
            binding.otpsetmob.setText(otpMessage);
        }

        return binding.getRoot();

    }

    //validate otp onboarding confirm Api

    private void onBoardingConfirmApi() {
        validateotpApiCall = "validateOtp";
        progress.show();
        String url = "alpreq";
        String reqDate = requestDate();
        String reqTime = requestTime();
        txnId = Helper.createTxnIdForOfflineTxn();
        //dateTime = reqDate + reqTime;

        JSONObject jsonObject = new JSONObject();
        try {

            jsonObject.put("userName", username);
            jsonObject.put("channel", "BPCL");
            jsonObject.put("tid", tid);
            jsonObject.put("mid", mid);
            // jsonObject.put("source", "Mobile");
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("dateTime", dateTime);
            jsonObject.put("txnType", "ABV");
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnId", txnid);
            jsonObject.put("password", otppass);
            // jsonObject.put("hwSrNo", Helper.serialNo);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "0");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);

            Log.d("onboradingconfirm", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            Log.d("respCode", "respCode");
            progress.dismiss();
        }
    }

    private void fetchCongfiuration() {
        validateotpApiCall = "fetchCongfiurationApi";
        String url = "alpreq";
        txnId = Helper.createTxnIdForOfflineTxn();

        JSONObject jsonObject = new JSONObject();
        try {

            jsonObject.put("userName", username);
            jsonObject.put("channel", "BPCL");
            jsonObject.put("tid", tid);
            jsonObject.put("mid", mid);
            // jsonObject.put("source", "Mobile");
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("dateTime", dateTime);
            jsonObject.put("txnType", "ACF");
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnId", txnid);
            jsonObject.put("password", otppass);
            // jsonObject.put("hwSrNo", Helper.serialNo);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "0");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);

            Log.d("onboradingconfirm", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            Log.d("respCode", "respCode");
            progress.dismiss();
        }
    }


    // Onboarding Acknowledge API

    private void onBoardingAcknowledgeAPI() {
        validateotpApiCall = "acknowledgeapi";
        String url = "alpreq";
        String reqDate = requestDate();
        String reqTime = requestTime();
        //txnId = Helper.createTxnIdForOfflineTxn();
        dateTime = reqDate + reqTime;

        JSONObject jsonObject = new JSONObject();
        try {

            jsonObject.put("userName", username);
            jsonObject.put("channel", "BPCL");
            jsonObject.put("tid", tid);
            jsonObject.put("mid", mid);
            // jsonObject.put("source", "Mobile");
            jsonObject.put("reqDate", reqDate);
            jsonObject.put("reqTime", reqTime);
            jsonObject.put("dateTime", dateTime);
            jsonObject.put("txnType", "ABS");
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnId", txnid);
            // jsonObject.put("hwSrNo", Helper.serialNo);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "0");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);

            Log.d("onboardingacknowledge", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            Log.d("respCode", "respCode");
            progress.dismiss();
        }
    }

    public void apiResult(String res, String apiName) {
        if (validateotpApiCall.equals("validateOtp")) {
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                    });
                } else {
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");


                    if (respCode.equals("200")) {

                        JSONArray outputArray = payLoad.getJSONArray("output");
                        JSONObject outputArrayJSONObject = outputArray.getJSONObject(0);
                        String successmsg = outputArrayJSONObject.getString("message");

                        getActivity().runOnUiThread(() -> {
                            Log.d("validateOtpResponse = ", res);
                            //progress.dismiss();
//                            MessagesDialog.showDialog(getActivity(), successmsg, null, null);
                          //  onBoardingAcknowledgeAPI();
                            fetchCongfiuration();
                        });
                    } else {
                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            binding.etOtp.setText("");
                            MessagesDialog.showDialog(getActivity(), respDesc, 0,null, null);
                        });
                    }
                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "onBoardingConfirmApi_Response", e.toString());
                getActivity().runOnUiThread(() -> {
                    if(progress.isShowing()){
                        progress.dismiss();
                    }
                    binding.etOtp.setText("");
                    MessagesDialog.showDialog(getActivity(), e.toString(), 0,null, null);

                    e.printStackTrace();
                });
            }
        }else if (validateotpApiCall.equals("fetchCongfiurationApi")) {
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                    });
                } else {
                    Log.d("ConfigResponse",res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");

                    JSONArray outputArray = payLoad.getJSONArray("output");
                    JSONObject outputArrayJSONObject = outputArray.getJSONObject(0);
                  //  String successmsg = outputArrayJSONObject.getString("message");

                    if (respCode.equals("200")) {
                        getActivity().runOnUiThread(() -> {
                            Log.d("validateOtpResponse = ", res);
                            //progress.dismiss();
                         //   Toast.makeText(getActivity(), successmsg, Toast.LENGTH_SHORT).show();
                            onBoardingAcknowledgeAPI();
                        });
                    } else {
                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            MessagesDialog.showDialog(getActivity(), respDesc + " - " + respCode, 0,null, null);
                        });
                    }
                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "onBoardingConfirmApi_Response", e.toString());
                getActivity().runOnUiThread(() -> {
                    if(progress.isShowing()){
                        progress.dismiss();
                    }
                    MessagesDialog.showDialog(getActivity(), e.toString(), 0,null, null);

                    e.printStackTrace();
                });
            }
        }
        else if (validateotpApiCall.equals("acknowledgeapi")) {
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                    });
                } else {
                    Log.d("OnboardingResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");

                    JSONArray outputArray = payLoad.getJSONArray("output");
                    JSONObject outputArrayJSONObject = outputArray.getJSONObject(0);
                    String message = outputArrayJSONObject.getString("message");

                    if (respCode.equals("200")) {
                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            showAlertDialogonboarding(message);
                        });
                    } else {
                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            MessagesDialog.showDialog(getActivity(), respDesc + " - " + respCode, 0,null, null);
                        });
                    }
                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "onBoardingAcknowledgeApi_Response", e.toString());
                getActivity().runOnUiThread(() -> {
                    if(progress.isShowing()){
                        progress.dismiss();
                    }
                    MessagesDialog.showDialog(getActivity(), e.toString(), 0,null, null);

                });
            }
        }
    }

    private void showAlertDialogonboarding(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setMessage(message).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                ((SideBarActivity) requireActivity()).loadFragement(new ConfigurationsFragment());
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.show();
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
