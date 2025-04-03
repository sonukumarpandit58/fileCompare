package com.ims.bpcluat.alp.alpOperations.cardManagement.enroll_additional;

import static com.ims.bpcluat.Helper.channelName;

import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.alp.alpOperations.cardManagement.CardManagementFragment;
import com.ims.bpcluat.databinding.FragmentEnrolManagerPinBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.receiver.ConnectivityReceiver;
import com.ims.bpcluat.utils.SharedPrefHelper;

import org.json.JSONException;
import org.json.JSONObject;

public class EnrolAddCardManagerPinFrag extends Fragment implements ApiHelper.NetworkingApiCallBack {
    FragmentEnrolManagerPinBinding binding;
    ApiHelper api;
    ProgressDialog progress;
    Context context;
    private ConnectivityReceiver connectivityReceiver = new ConnectivityReceiver();
    SharedPrefHelper sharedPrefHelper;
    String shredValue;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEnrolManagerPinBinding.inflate(inflater, container, false);
        context = getActivity();
        api = new ApiHelper();
        connectivityReceiver = new ConnectivityReceiver();
        sharedPrefHelper = new SharedPrefHelper(requireContext());

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new CardManagementFragment());
            }
        });

        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateFields();
            }
        });

        hideKeyboard();

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

    public void hideKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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
            api.setApiCallBack(EnrolAddCardManagerPinFrag.this);
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
                    Log.d("validateMangerPinResponse = ", res);
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
                                ((SideBarActivity) requireActivity()).loadFragement(new EnrolSubmitCardFragment());
                            }
                        });

                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("elseVoidrespCode", respCode);
                                progress.dismiss();
                                MessagesDialog.showDialog(requireContext(), respDesc, 0,null, null);
                            }
                        });
                    }

                } catch (Exception e) {
                    progress.dismiss();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(requireContext(),  e.toString(), 0,null, null);
                        }
                    });
                }

            }
        }
    }
}

