package com.ims.bpcluat.alp.alpConfiguration;

import static com.ims.bpcluat.Helper.appVersion;
import static com.ims.bpcluat.Helper.fileWrite;

import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.Helper.username;

import android.app.ProgressDialog;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.alp.AlpFragment;
import com.ims.bpcluat.alp.alpConfiguration.alpFeatures.MangerPinFragment;
import com.ims.bpcluat.databinding.FragmentConfigurationsBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.model.AlpModels.VoidModels.Output;
import com.ims.bpcluat.model.AlpModels.VoidModels.TxnList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class ConfigurationsFragment extends Fragment implements ApiHelper.NetworkingApiCallBack {
    FragmentConfigurationsBinding binding;
    ProgressDialog progress;
    ApiHelper api;
    String txnId = "", dateTime = "";
    String getotpApiCall = "";


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentConfigurationsBinding.inflate(inflater, container, false);
        DrawerLayout drawerLayout = getActivity().findViewById(R.id.drawerLayout);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        api = new ApiHelper();

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((SideBarActivity) requireActivity()).loadFragement(new AlpFragment());
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

            }
        });

        binding.onboardingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBoardingInitiateApi();
            }
        });

        binding.devicedetailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((SideBarActivity) requireActivity()).loadFragement(new DeviceDetailsFragment());
            }
        });



        binding.alpFeatureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new MangerPinFragment());
            }
        });

        return binding.getRoot();
    }

    //Onboarding initiate api

    private void onBoardingInitiateApi() {
        getotpApiCall = "getOtp";

        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);
        progress.show();

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
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("dateTime", dateTime);
            jsonObject.put("txnType", "ABO");
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnId", txnId);
            //jsonObject.put("hwSrNo", Helper.serialNo);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "0");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);

            Log.d("getOtpRequest", String.valueOf(jsonObject));

            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);

        } catch (JSONException e) {
            Log.d("respCode", "respCode");
            progress.dismiss();
        }
    }

    public void apiResult(String res, String apiName) {
        if (getotpApiCall.equals("getOtp")) {

            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                    });
                } else {
                    Log.d("getOtpResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");
                    String outputMessage = "";
                    Log.d("txnId", txnId);

                    if (respCode.equals("200")) {
                        JSONArray outputArray = payLoad.getJSONArray("output");
                        String txnId = payLoad.getString("txnId");
                        String dateTime = payLoad.getString("dateTime");

                        if (outputArray.length() > 0) {
                            JSONObject outputObject = outputArray.getJSONObject(0);
                            outputMessage = outputObject.getString("message");
                            Log.d("outputMessage", outputMessage);
                        }

                        progress.dismiss();
                        Bundle bundle = new Bundle();
                        bundle.putString("otp_message", outputMessage);
                        bundle.putString("txnId", txnId);
                        bundle.putString("dateTime", dateTime);
                        OnBoardingOtpFragment nextFragment = new OnBoardingOtpFragment();
                        nextFragment.setArguments(bundle);
                        ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, new OnBoardingOtpFragment());
                        Log.d("respCode", respCode);

                    } else {
                        String finalOutputMessage = outputMessage;
                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            MessagesDialog.showDialog(getActivity(), respDesc + "\n" + finalOutputMessage, 0,null, null);
//                            Toast.makeText(getActivity(), respDesc + " - " + respCode + "\n" + finalOutputMessage, Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "onBoardingInitiateApi_Response", e.toString());
                getActivity().runOnUiThread(() -> {
                    if(progress.isShowing()){
                        progress.dismiss();
                    }
                    MessagesDialog.showDialog(getActivity(), e.toString(), 0,null, null);
                    e.printStackTrace();
                });
            }
        }
    }

}