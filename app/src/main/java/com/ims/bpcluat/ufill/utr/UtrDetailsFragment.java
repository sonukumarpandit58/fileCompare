package com.ims.bpcluat.ufill.utr;

import static com.ims.bpcluat.Helper.appVersion;
import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.client;
import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.instId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.Helper.username;
import static com.ims.bpcluat.helper.ApiHelper.uFillEndpoint;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.databinding.FragmentCngBinding;
import com.ims.bpcluat.databinding.FragmentUtrDetailsBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.model.UfillModel;
import com.ims.bpcluat.ufill.ufil1.UfillOnePumpFragment;

import org.json.JSONException;
import org.json.JSONObject;

public class UtrDetailsFragment extends Fragment implements ApiHelper.NetworkingApiCallBack {

    FragmentUtrDetailsBinding binding;
    Context context;
    ApiHelper api;
    ProgressDialog progress;
    private UfillModel ufillModel;
    String txnId = "",dateTime = "",utrNo = "",voucherCode = "", mobileNumber = "", amount = "",voucherStatus = "",qrCodeUrl = "";

    public UtrDetailsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            ufillModel= getArguments().getParcelable("ufillModel");
            if (ufillModel != null) {
                ufillModel.setTxnType("UFILLUTR");
                txnId = ufillModel.getTxnId();
                dateTime = ufillModel.getDateTime();
                utrNo = ufillModel.getUtrNo();
                voucherCode = ufillModel.getVoucherNo();
                mobileNumber = ufillModel.getMobileNumber();
                amount = ufillModel.getVoucherAmt();
                voucherStatus = ufillModel.getVoucherStatus();
                qrCodeUrl = ufillModel.getQrCodeUrl();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUtrDetailsBinding.inflate(inflater, container, false);
        context =  getActivity();
        api = new ApiHelper();

        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);

        binding.utrNum.setText("UTR- " + utrNo);
        binding.voucherCode.setText("Code: " + voucherCode);
        binding.mobileNo.setText("Mobile: " + mobileNumber);
        binding.paidAmount.setText("Paid Amount: ₹" + amount);
        binding.voucherAmnt.setText("Voucher Amount: ₹" + amount);
        binding.status.setText("Status: " + voucherStatus);
        if(voucherStatus.equals("Active")){
            binding.utrRedeemBtn.setVisibility(View.VISIBLE);
        }else{
            binding.utrRedeemBtn.setVisibility(View.GONE);
        }

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new UtrFragment());
            }
        });

        binding.utrRedeemBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                utrRedemptionApi();
            }
        });
        return binding.getRoot();
    }


    private void utrRedemptionApi() {
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
            jsonObject.put("txnType", "AUR");
            jsonObject.put("rrn", qrCodeUrl);
            jsonObject.put("dateTime", dateTime);
            jsonObject.put("txnId", txnId);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "0");
            jsonObject.put("client", client);
            jsonObject.put("instId", instId);
            jsonObject.put("appVersion", appVersion);
            Log.d("utrRedemptionData", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            Log.d("UtrJsonExceptionRequest",e.toString());
        }
    }

    @Override
    public void apiResult(String res, String apiName) {
        try {
            if (res.equals("Server Time Out")) {
                progress.dismiss();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //inquiryUtrRedemption();
                        Toast.makeText(context, "Serve time out", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.d("UtrApiResponse",res);
                JSONObject jsonObject = new JSONObject(res);
                JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                String respCode = payLoad.getString("respCode");
                String respDesc = payLoad.getString("respDesc");
                if (respCode.equals("200")) {
                    progress.dismiss();
                    if(payLoad.has("authCode")){
                        String authCode = payLoad.getString("authCode");
                        Log.d("vmsTxnIdAuthCode",authCode);
                        ufillModel.setPrebookTxn(authCode);
                        ufillModel.setAuthCode(authCode);
                    }else{
                        ufillModel.setPrebookTxn("");
                    }

                    String dateTime = payLoad.getString("dateTime");
                    String rrn = payLoad.getString("rrn");
                    String id = payLoad.getString("id");
                    ufillModel.setRrn(rrn);
                    ufillModel.setId(id);
                    ufillModel.setPrebookTxnTime(dateTime);
                    getActivity().runOnUiThread(() -> {
                        Bundle bundle = new Bundle();
                        bundle.putParcelable("ufillModel", ufillModel);
                        ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle,new UfillOnePumpFragment());
                    });

                } else {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(context, respDesc +" "+ respCode, 0,null, null);
                        //Toast.makeText(context, respDesc+" - "+respCode, Toast.LENGTH_SHORT).show();
                    });
                }

            }
        } catch (JSONException e) {
            fileWrite(getContext(), todayDate + ".txt", "UtrApiResponse", e.toString());
            getActivity().runOnUiThread(() -> {
                progress.dismiss();
                Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_SHORT).show();
                if (progress.isShowing()) {
                    progress.dismiss();
                }
                MessagesDialog.showDialog(context, e.toString(), 0,null, null);

            });
            //throw new RuntimeException(e);
        }
    }


    public void onResume() {
        super.onResume();
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
    }


    @Override
    public void onStop() {
        super.onStop();
        ((AppCompatActivity) getActivity()).getSupportActionBar().show();
    }
}