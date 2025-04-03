package com.ims.bpcluat.alp.alpOperations.utility.reprint;

import static com.ims.bpcluat.Helper.cardChargeslipDate;
import static com.ims.bpcluat.Helper.cardChargeslipTime;
import static com.ims.bpcluat.Helper.channelName;

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

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.alp.alpConfiguration.ConfigurationsFragment;
import com.ims.bpcluat.alp.alpOperations.utility.UtilityFragment;
import com.ims.bpcluat.alp.alpOperations.utility.alpVoid.AlpVoidFragment;
import com.ims.bpcluat.databinding.FragmentReprintOtpBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.model.AlpModels.ReprintTxnModel;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProductModel;
import com.ims.bpcluat.model.AlpModels.SaleModel.Program;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProgramOutput;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProgramWallet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ReprintOtpFragment extends Fragment implements ApiHelper.NetworkingApiCallBack {
    FragmentReprintOtpBinding binding;
    Context context;
    ApiHelper api;
    ProgressDialog progress;
    String message = "", id = "", mobNo= "";
    private CountDownTimer countDownTimer;
    String requestCheck = "no";

    public ReprintOtpFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReprintOtpBinding.inflate(inflater, container, false);
        context = getContext();
        api = new ApiHelper();

        Bundle bundle = getArguments();
        if (bundle != null) {
            id = bundle.getString("id");
            message = bundle.getString("message");
            mobNo = bundle.getString("mobNo");
        }

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

        countDownTimer = new CountDownTimer(45000, 1000) {
            public void onTick(long millisUntilFinished) {
                binding.timerTxt.setText(millisUntilFinished / 1000 + " seconds remaining");
            }
            public void onFinish() {
                binding.timerTxt.setText("");
                getActivity().runOnUiThread(() -> {
                    if(requestCheck.equals("no")){
                        ((SideBarActivity) requireActivity()).loadFragement(new UtilityFragment());
                    }else{
                        Log.d("requestCheck", "onFinish: "+requestCheck);
                    }
                });
            }
        }.start();

        return binding.getRoot();
    }

    private void validateFields() {
        String otp = binding.reprintOtp.getText().toString().trim();

        if (TextUtils.isEmpty(otp)) {
            binding.reprintOtp.setError("Please enter OTP");
            binding.reprintOtp.requestFocus();
            return;
        }
            transactionReprintAPI(otp);
    }


    private void transactionReprintAPI(String otp) {
        requestCheck = "yes";
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
            jsonObject.put("tranChannel", "VC");
            jsonObject.put("dateTime", requestDate() + requestTime());
            jsonObject.put("mobNo", mobNo);
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
            api.setApiCallBack(ReprintOtpFragment.this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void apiResult(String res, String apiName) {
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
                logLongMessage("reprintResponse = ", res);
                JSONObject jsonObject = new JSONObject(res);
                JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                String respCode = payLoad.getString("respCode");
                String respDesc = payLoad.getString("respDesc");
                if (respCode.equals("200")) {
                    Log.d("fetchResponseCode1", respCode);

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
                               time, txnId
                        );

                        reprintTxnModelList.add(txnModel);
                    }

                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        Log.d("programResponse = ", res);
                        ReprintTxnList fragment = new ReprintTxnList();
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("reprintTxnModelList",(Serializable) reprintTxnModelList);
                        fragment.setArguments(bundle);
                        ((SideBarActivity) requireActivity()).loadFragement(fragment);
                    });

                } else {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        Log.d("programResponseException = ", res);
                        binding.reprintOtp.setText("");
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
                    binding.reprintOtp.setText("");
                    MessagesDialog.showDialog(context, e.toString(), 0,null, null);
                }
            });
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