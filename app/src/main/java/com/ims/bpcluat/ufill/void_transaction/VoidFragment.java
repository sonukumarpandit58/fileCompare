package com.ims.bpcluat.ufill.void_transaction;

import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.closeKeyboard;
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
import static com.ims.bpcluat.Helper.version;
import static com.ims.bpcluat.helper.ApiHelper.uFillEndpoint;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.databinding.FragmentVoidBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.receiver.ConnectivityReceiver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VoidFragment extends Fragment implements ApiHelper.NetworkingApiCallBack {
    ApiHelper api;
    ProgressDialog progress;
    FragmentVoidBinding binding;
    Context context;
    private ConnectivityReceiver connectivityReceiver = new ConnectivityReceiver();

    public VoidFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVoidBinding.inflate(inflater, container, false);
        context = getActivity();
        api = new ApiHelper();

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
        closeKeyboard(getActivity());
        if (connectivityReceiver.isConnected(getContext())) {
            fetchVoidTransaction(managerPin);
        } else {
            MessagesDialog.showDialog(requireContext(), "No internet connection", 0,null, null);
        }
    }


    private void fetchVoidTransaction(String ManagerPin) {
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while fetching details...");
        progress.setCancelable(false);
        progress.show();

        String url = uFillEndpoint;
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("channel", channelName);
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("userName", username);
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnType", "AVI");
            jsonObject.put("dateTime", requestDate() + requestTime());
            jsonObject.put("txnId", Helper.createTxnIdForOfflineTxn());
            jsonObject.put("password", ManagerPin);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", version);

            Log.d("voidApiRequest", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(VoidFragment.this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void apiResult(String res, String apiName) {
        if (apiName.equals("ufill")) {
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
                    Log.d("voidApiResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respCode = payLoad.getString("respCode");
                    String respDesc = payLoad.getString("respDesc");
                    if (respCode.equals("200")) {
                        JSONArray outputArray = payLoad.getJSONArray("output");
                        JSONObject outputObject = outputArray.getJSONObject(0);
                        JSONArray vmsSuspectTransactionsArray = outputObject.getJSONArray("vmsSuspectTransactions");
                        progress.dismiss();
                        if(vmsSuspectTransactionsArray.length() == 0){
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "No Transaction Found", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }else{
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Intent intent = new Intent(requireContext(), VoidTransaction.class);
                                    intent.putExtra("payLoad", payLoad.toString());
                                    startActivity(intent);
                                }
                            });
                        }
                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                MessagesDialog.showDialog(context, respDesc, 0,null, null);
                                // Toast.makeText(getActivity(), respDesc+" - "+respCode, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "voidApiResponse", e.toString());
                getActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_SHORT).show();
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    MessagesDialog.showDialog(context, e.toString(), 0,null, null);

                });
               // throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}