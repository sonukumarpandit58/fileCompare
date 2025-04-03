package com.ims.bpcluat.alp.alpOperations.cardManagement.balanceEnquiry;

import static com.ims.bpcluat.Helper.appVersion;
import static com.ims.bpcluat.Helper.fileWrite;

import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.Helper.username;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.databinding.FragmentEnterOtpBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.model.AlpModels.SaleModel.Program;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProgramOutput;
import com.ims.bpcluat.model.AlpModels.SaleModel.VirtualCardProgramModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EnterOtpFragment extends Fragment implements ApiHelper.NetworkingApiCallBack {
    FragmentEnterOtpBinding binding;
    ProgressDialog progress;
    ApiHelper api;
    String txnId = "", dateTime = "";
    String validateOtp = "";
    Context context;
    List<Program> programList = new ArrayList<>();
    private CountDownTimer countDownTimer;

    private String mobNo = "";
    private String otp = "";
    private String id = "";
    private String position = "";
    private String programID = "";
    private String accountNumber = "";
    private String cardNumber = "";
    private int indexPosition = -1;
    List<VirtualCardProgramModel> virtualCardProgramModelArrayList = new ArrayList<>();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEnterOtpBinding.inflate(inflater, container, false);
        context = getContext();
        api = new ApiHelper();

        Bundle bundle = getArguments();
        if (bundle != null) {
            mobNo = bundle.getString("mobNo");
            id = bundle.getString("id");
            position = bundle.getString("position");
            indexPosition = bundle.getInt("indexPosition");
            programList = (ArrayList<Program>) bundle.getSerializable("programs");


            if (programList != null && indexPosition >= 0 && indexPosition < programList.size()) {
                // Get the Program object at the specified index
                Program program = programList.get(indexPosition);

                programID = program.getProgramID();
                accountNumber = program.getAccountNumber();
                cardNumber = program.getCardNumber();

            } else {
            }
        }


        Bundle finalBundle = new Bundle();
        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finalBundle.putString("id", id);
                finalBundle.putString("mobNo", mobNo);
                finalBundle.putSerializable("programs", (Serializable) programList);

                ProgramListBalanceEnquiryFragment fragment = new ProgramListBalanceEnquiryFragment();
                fragment.setArguments(finalBundle);
                ((SideBarActivity) requireActivity()).loadFragement(fragment);
            }
        });

        binding.submitotp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                otp = binding.editTextotp.getText().toString().trim();
                if (otp.isEmpty()) {
                    binding.editTextotp.setError("Please Enter OTP");
                    binding.editTextotp.requestFocus();
                } else {
                    fetchloyalitybalanceApi();
                }
            }
        });

        countDownTimer = new CountDownTimer(45000, 1000) {
            public void onTick(long millisUntilFinished) {
                binding.timerTxt.setText(millisUntilFinished / 1000 + " seconds remaining");
            }

            public void onFinish() {
                getActivity().runOnUiThread(() -> {
                    binding.timerTxt.setText("Time's up!");

                    finalBundle.putString("id", id);
                    finalBundle.putString("mobNo", mobNo);
                    finalBundle.putSerializable("programs", (Serializable) programList);

                    ProgramListBalanceEnquiryFragment fragment = new ProgramListBalanceEnquiryFragment();
                    fragment.setArguments(finalBundle);
                    ((SideBarActivity) requireActivity()).loadFragement(fragment);
                });

            }
        }.start();

        return binding.getRoot();
    }

    private void fetchloyalitybalanceApi() {
        validateOtp = "fetchloyalitybalanceApi";
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
            jsonObject.put("channel", "BPCL");
            jsonObject.put("reqDate", reqDate);
            jsonObject.put("reqTime", reqTime);
            jsonObject.put("userName", username);
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("source", "Mobile");
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnType", "ACB");
            jsonObject.put("dateTime", dateTime);
            jsonObject.put("mobNo", mobNo);
            jsonObject.put("password", otp);
            jsonObject.put("id", id);
            jsonObject.put("txnId", txnId);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);

            JSONArray billerTranList = new JSONArray();
            JSONObject billerTranItem;

            billerTranItem = new JSONObject();
            billerTranItem.put("field13", programID);
            billerTranItem.put("field14", accountNumber);
            billerTranItem.put("field15", cardNumber);
            billerTranList.put(billerTranItem);
            jsonObject.put("billerTranList", billerTranList);

            Log.e("checkdatainrequest", programID);

            Log.d("validateOtpRequest=", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void apiResult(String res, String apiName) {
        if (validateOtp.equals("fetchloyalitybalanceApi")) {
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                    });
                } else {
                    Log.d("validateOtpRequest = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");
                    // String output = payLoad.getString("output");

                    if (respCode.equals("200")) {
                        progress.dismiss();

                        Intent intent = new Intent(context, BalanceEnquiryReciept.class);
                        intent.putExtra("payload", payLoad.toString());
                        context.startActivity(intent);
                    } else {
                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            binding.editTextotp.setText("");

                            MessagesDialog.showDialog(getActivity(), respDesc, 0,null, null);
                        });
                    }
                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "fetchloyalitybalanceApiResponse", e.toString());
                getActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_SHORT).show();
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    binding.editTextotp.setText("");
                    MessagesDialog.showDialog(getActivity(), e.toString(), 0,null, null);
                });
            }
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d("TAGonDestroyView", "onDestroyView: ");
        MessagesDialog.dismissDialog();
        if (countDownTimer != null) {
            countDownTimer.cancel();

        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("TAGonPause", "onPause: ");
        MessagesDialog.dismissDialog();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}