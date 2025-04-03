package com.ims.bpcluat.alp.alpOperations.utility.alpVoid;

import static com.ims.bpcluat.Helper.channelName;

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
import static com.ims.bpcluat.Helper.username;
import static com.ims.bpcluat.Helper.version;
import static com.ims.bpcluat.helper.ApiHelper.alpEndpoint;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.MainActivity;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.alp.alpOperations.utility.UtilityFragment;
import com.ims.bpcluat.databinding.FragmentAlpVoidBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.receiver.ConnectivityReceiver;

import org.json.JSONException;
import org.json.JSONObject;

public class AlpVoidFragment extends Fragment implements ApiHelper.NetworkingApiCallBack {
    ApiHelper api;
    ProgressDialog progress;
    FragmentAlpVoidBinding binding;
    Context context;
    private ConnectivityReceiver connectivityReceiver = new ConnectivityReceiver();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAlpVoidBinding.inflate(inflater, container, false);
        context = getActivity();
        api = new ApiHelper();
        connectivityReceiver = new ConnectivityReceiver();

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
//                ((SideBarActivity) requireActivity()).loadFragement(new VoidMobileNumFragment());
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
        showProgressDialog();
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
            jsonObject.put("txnType", "AMP");
            jsonObject.put("dateTime", requestDate() + requestTime());
            jsonObject.put("txnId", Helper.createTxnIdForOfflineTxn());
            jsonObject.put("hwSrNo", Helper.serialNumber);
//          jsonObject.put("hwSrNo", serialNo);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("password", ManagerPin);
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", version);

            Log.d("validateMangerPinApi", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(AlpVoidFragment.this);
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


    public void apiResult(String res, String apiName) {
        if (apiName.equals("alpreq")) {
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
                                ((SideBarActivity) requireActivity()).loadFragement(new VoidMobileNumFragment());
                            }
                        });

                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("elseVoidrespCode", respCode);
                                progress.dismiss();
                                MessagesDialog.showDialog(getActivity(), respDesc, 0,null, null);
                            }
                        });
                    }

                } catch (Exception e) {
                    progress.dismiss();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(getActivity(), e.toString(), 0,null, null);
                        }
                    });
                }

            }
        }
    }
}