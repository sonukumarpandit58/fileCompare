package com.ims.bpcluat.ufill;

import static com.ims.bpcluat.Helper.appVersion;
import static com.ims.bpcluat.Helper.channelName;

import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.Helper.username;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.R;
import com.ims.bpcluat.databinding.FragmentUfillRegistrationBinding;
import com.ims.bpcluat.databinding.FragmentVoidBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;

import org.json.JSONException;
import org.json.JSONObject;

public class UfillRegistrationFragment extends Fragment implements ApiHelper.NetworkingApiCallBack {
    ApiHelper api;
    FragmentUfillRegistrationBinding binding;
    Context context;
    ProgressDialog progress;
    String txnId = "", dateTime = "", otp = "";
    String currentApiCall = "";

    public UfillRegistrationFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentUfillRegistrationBinding.inflate(inflater, container, false);
        context = getActivity();
        api = new ApiHelper();
        txnId = Helper.createTxnIdForOfflineTxn();
        dateTime = requestDate() + requestTime();

        aposRegistrationInitiateApi();
        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                otp = binding.otp.getText().toString().trim();
                if (TextUtils.isEmpty(otp)) {
                    binding.otp.setError("Please enter otp");
                    binding.otp.requestFocus();
                    return;
                }
                aposRegistrationConfirmApi();
            }
        });
        return binding.getRoot();
    }

    private void aposRegistrationInitiateApi() {
        currentApiCall = "aposInitiate";
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();
        String url = "ufill";
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("channel", channelName);
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("userName", username);
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnType", "ARG");
            jsonObject.put("dateTime", dateTime);
            jsonObject.put("txnId", txnId);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "0");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", appVersion);

            Log.d("initiateApi Request = ", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void aposRegistrationConfirmApi() {
        currentApiCall = "aposConfirm";
        String otp = binding.otp.getText().toString().trim();
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();
        String url = "ufill";
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("channel", channelName);
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("userName", username);
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnType", "ARC");
            jsonObject.put("dateTime", dateTime);
            jsonObject.put("txnId", txnId);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "0");
            jsonObject.put("password", otp);
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", appVersion);

            Log.d("confirmApi Request = ", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void apiResult(String res, String apiName) {
        if (currentApiCall.equals("aposInitiate")) {
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                    });
                } else {
                    Log.d("aposInitiateResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");
                    if (respCode.equals("200")) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                binding.submitBtn.setEnabled(true);
                                binding.submitBtn.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.topBar));
                                Toast.makeText(context, "OTP is sent to register Mobile Number", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }else{
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                binding.submitBtn.setEnabled(false);
                                binding.submitBtn.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.btnGrayy));
                                MessagesDialog.showDialog(context, respDesc, 0,null, null);
                                //Toast.makeText(getActivity(), respDesc + " - " + respCode, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }catch (Exception e){
                fileWrite(getContext(), todayDate + ".txt", "aposInitiateResponse", e.toString());
                getActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_SHORT).show();
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    MessagesDialog.showDialog(context, e.toString(), 0,null, null);

                });
            /*    Log.d("OtpSendException",e.toString());
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(progress.isShowing()){
                            progress.dismiss();
                            Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });*/
            }
        } else if (currentApiCall.equals("aposConfirm")) {
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);

                       // Toast.makeText(getActivity(), "Server Time Out", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    Log.d("aposConfirmResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");
                    if (respCode.equals("200")) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                Toast.makeText(context, respDesc + " - " + respCode, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }else{
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                MessagesDialog.showDialog(context, respDesc +" "+ respCode, 0,null, null);
                                //Toast.makeText(context, "OTP not matched", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }catch (Exception e){
                fileWrite(getContext(), todayDate + ".txt", "getshiftsummaryResponse", e.toString());
                getActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_SHORT).show();
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    MessagesDialog.showDialog(context, e.toString(), 0,null, null);

                });
                //Log.d("OtpConfrimException",e.toString());
            }
        }
    }
}