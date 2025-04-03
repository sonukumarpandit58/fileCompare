package com.ims.bpcluat.alp.alpConfiguration.alpFeatures;

import static com.ims.bpcluat.Helper.appVersion;
import static com.ims.bpcluat.Helper.channelName;
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

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ims.bpcluat.AdminLoginActivity;
import com.ims.bpcluat.Helper;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.alp.alpConfiguration.ConfigurationsFragment;
import com.ims.bpcluat.alp.alpOperations.sale.loyalitypayqr.ScanQRFragment;
import com.ims.bpcluat.alp.alpOperations.utility.UtilityFragment;
import com.ims.bpcluat.databinding.FragmentMangerPinBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.receiver.ConnectivityReceiver;
import org.json.JSONException;
import org.json.JSONObject;

public class MangerPinFragment extends Fragment implements ApiHelper.NetworkingApiCallBack {
    ApiHelper api;
    ProgressDialog progress;
    FragmentMangerPinBinding binding;
    Context context;
    String txnId = "", dateTime = "";
    private ConnectivityReceiver connectivityReceiver = new ConnectivityReceiver();
    String programListApiCall = "";

    public MangerPinFragment() {

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMangerPinBinding.inflate(inflater, container, false);
        context = getActivity();
        api = new ApiHelper();
        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new ConfigurationsFragment());
            }
        });

        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateFields();
            }
        });

        return binding.getRoot();
    }

    private void validateFields() {
        String managerPin = binding.managerPin.getText().toString().trim();

        if (TextUtils.isEmpty(managerPin)) {
            binding.managerPin.setError("Please enter manager pin");
            binding.managerPin.requestFocus();
            return;
        }
        if (connectivityReceiver.isConnected(context)) {
            validateMangerPinApi(managerPin);
        } else {
            MessagesDialog.showDialog(requireContext(), "No internet connection", 0,null, null);
        }
    }

    private void validateMangerPinApi(String ManagerPin) {
        programListApiCall = "validateMangerPinApi";
        showProgressDialog();
        String url = alpEndpoint;
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("channel", channelName);
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("userName", username);
            jsonObject.put("mid", Helper.mid);
            jsonObject.put("tid", tid);
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnType", "AMP");
            jsonObject.put("dateTime", requestDate() + requestTime());
            jsonObject.put("txnId", Helper.createTxnIdForOfflineTxn());
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("password", ManagerPin);
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", version);

            Log.d("validateMangerPinApi", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(MangerPinFragment.this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void showProgressDialog() {
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while fetching details...");
        progress.setCancelable(false);
        progress.show();
    }

    private void fetchConfigurationAPI() {
        programListApiCall = "fetchConfigurationAPI";
        String url = "alpreq";
        String reqDate = requestDate();
        String reqTime = requestTime();
        txnId = Helper.createTxnIdForOfflineTxn();
        dateTime = reqDate + reqTime;

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("userName", username);
            jsonObject.put("channel", "BPCL");
            jsonObject.put("tid", tid);
            jsonObject.put("mid", mid);
            jsonObject.put("source", "Mobile");
            jsonObject.put("reqDate", reqDate);
            jsonObject.put("reqTime", reqTime);
            jsonObject.put("dateTime", dateTime);
            jsonObject.put("txnType", "ACF");
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnId", txnId);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);
            Log.d("getOtpRequest=", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void apiResult(String res, String apiName) {
        if (programListApiCall.equals("validateMangerPinApi")) {
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

                try {
                    Log.d("ufiltwoListResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);

                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respCode = payLoad.getString("respCode");
                    String respDesc = payLoad.getString("respDesc");
                    if (respCode.equals("200")) {
                        progress.dismiss();
                        Log.d("validateMangerPinApi#", payLoad.toString());
                        Log.d("validateMangerPinApi@", respCode.toString());
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                fetchConfigurationAPI();
                            }
                        });

                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("elseVoidrespCode", respCode);
                                progress.dismiss();
                                MessagesDialog.showDialog(context, respDesc, 0,null, null);
                            }
                        });
                    }

                } catch (Exception e) {
                    progress.dismiss();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(context, e.toString(), 0,null, null);
                        }
                    });
                }

            }
        } else {
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
                try {
                    JSONObject jsonObject = new JSONObject(res);

                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respCode = payLoad.getString("respCode");
                    String respDesc = payLoad.getString("respDesc");
                    if (respCode.equals("200")) {

                        getActivity().runOnUiThread(() -> {

                            Log.d("fetchConfigurationAPIApi#", payLoad.toString());
                            configurationFragment fragment = new configurationFragment();
                            Bundle bundle = new Bundle();
                            bundle.putString("payLoad", payLoad.toString());
                            fragment.setArguments(bundle);
                            ((SideBarActivity) context).loadFragmentWithData(bundle, fragment);

                        });

                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("elseVoidrespCode", respCode);
                                progress.dismiss();
                                MessagesDialog.showDialog(context, respDesc, 0,null, null);
                            }
                        });
                    }

                } catch (Exception e) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progress.dismiss();
                            MessagesDialog.showDialog(context, e.toString(), 0,null, null);
                        }
                    });
                }

            }

        }
    }
}