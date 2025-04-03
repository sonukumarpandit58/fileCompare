package com.ims.bpcluat.ufill.void_transaction;

import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.reportDate;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.version;
import static com.ims.bpcluat.helper.ApiHelper.uFillEndpoint;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.adapter.ufil_adapter.VoidTransactionAdapter;
import com.ims.bpcluat.databinding.ActivityVoidTransactionBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.interfaces.VoidTxnRecyclerViewInterface;
import com.ims.bpcluat.model.VoidReason;
import com.ims.bpcluat.model.VoidTransactionModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class VoidTransaction extends AppCompatActivity implements ApiHelper.NetworkingApiCallBack, VoidTxnRecyclerViewInterface {

    ActivityVoidTransactionBinding binding;
    RecyclerView recyclerView;
    Context context;
    VoidTransactionAdapter voidTransactionAdapter;
    ArrayList<VoidTransactionModel> voidTransactionModels = new ArrayList<>();
    ApiHelper api;
    ProgressDialog progress;
    String mobileNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVoidTransactionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        context = this;
        api = new ApiHelper();
        recyclerView = binding.recyclerView;

        getArgumentsData();

        voidTransactionAdapter = new VoidTransactionAdapter(context, voidTransactionModels, this);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(voidTransactionAdapter);

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    public void getArgumentsData() {
        String voidData = getIntent().getStringExtra("payLoad");
        Log.d("DataReceived", voidData != null ? voidData : "No Data");
        if (voidData != null) {
            Log.d("getArguments", "Data Found");
            Log.d("getArguments@#", voidData);
            try {
                JSONObject payLoad = new JSONObject(voidData);

                String channel = payLoad.getString("channel");
                String reqDate = payLoad.getString("reqDate");
                String reqTime = payLoad.getString("reqTime");
                String response = payLoad.getString("response");
                String respCode = payLoad.getString("respCode");
                String resDate = payLoad.getString("resDate");
                String resTime = payLoad.getString("resTime");
                String userName = payLoad.getString("userName");
                String mid = payLoad.getString("mid");
                String tid = payLoad.getString("tid");
                String client = payLoad.getString("client");
                String respDesc = payLoad.getString("respDesc");
                String id = payLoad.getString("id");
                String txnId = payLoad.getString("txnId");
                String roCode = payLoad.getString("roCode");
                String dateTime = payLoad.getString("dateTime");
                String txnType = payLoad.getString("txnType");
                String instId = payLoad.getString("instId");


                JSONArray outputArray = payLoad.getJSONArray("output");
                if (outputArray.length() > 0) {
                    JSONObject outputObject = outputArray.getJSONObject(0);

                    JSONArray transactionsArray = outputObject.getJSONArray("vmsSuspectTransactions");
                    if (transactionsArray.length() > 0) {
                        for (int i = 0; i < transactionsArray.length(); i++) {
                            JSONObject transaction = transactionsArray.getJSONObject(i);

                            String vmsTxnID = transaction.getString("vmsTxnID");
                            String voucherCode = transaction.getString("voucherCode");
                            String customerMobileNoMasked = "";
                            if(transaction.has("customerMobileNoMasked")){
                                customerMobileNoMasked = transaction.getString("customerMobileNoMasked");
                            }
                            String voucherAmt = transaction.getString("voucherAmt");

                            List<VoidReason> voidReasonsList = new ArrayList<>();
                            if (outputObject.has("voidReasons")) {
                                JSONArray voidReasonsArray = outputObject.getJSONArray("voidReasons");
                                for (int j = 0; j < voidReasonsArray.length(); j++) {
                                    JSONObject voidReasonObject = voidReasonsArray.getJSONObject(j);

                                    String voidCode = voidReasonObject.getString("voidCode");
                                    String voidReasonText = voidReasonObject.getString("voidReasonText");

                                    VoidReason voidReason = new VoidReason(voidCode, voidReasonText);
                                    Log.d("VoidReason", voidReason.toString());
                                    voidReasonsList.add(voidReason);
                                }
                            }

                            VoidTransactionModel voidTransactionModel = new VoidTransactionModel(channel, instId, client, txnId, vmsTxnID, txnType, tid, roCode, mid, userName, reqDate, reqTime, dateTime, voucherCode, customerMobileNoMasked, voucherAmt, voidReasonsList);
                            voidTransactionModels.add(voidTransactionModel);
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClick(int position, List<VoidReason> voidReasons, List<VoidTransactionModel> voidTransactionModelList) {
        if (voidReasons != null && !voidReasons.isEmpty()) {
            showVoidReasonsPopup(position, voidReasons, voidTransactionModelList);
        } else {
            Toast.makeText(context, "No void reasons available", Toast.LENGTH_SHORT).show();
        }

    }

    private void showVoidReasonsPopup(int position, List<VoidReason> voidReasons, List<VoidTransactionModel> voidTransactionModelList) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Void Reason");
        RadioGroup radioGroup = getRadioGroup(voidReasons);
        builder.setView(radioGroup);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int selectedId = radioGroup.getCheckedRadioButtonId();
                if (selectedId != -1) {
                    RadioButton selectedRadioButton = radioGroup.findViewById(selectedId);
                    String selectedReason = selectedRadioButton.getText().toString();
                    String selectedVoidCode = (String) selectedRadioButton.getTag();
                    voidConfirmApi(position, selectedVoidCode, voidTransactionModelList);
                    Toast.makeText(context, "Selected: " + selectedReason + " (Code: " + selectedVoidCode + ")", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private @NonNull RadioGroup getRadioGroup(List<VoidReason> voidReasons) {
        RadioGroup radioGroup = new RadioGroup(context);
        radioGroup.setOrientation(RadioGroup.VERTICAL);
        for (VoidReason reason : voidReasons) {
            RadioButton radioButton = new RadioButton(context);
            radioButton.setText(reason.getVoidReasonText());
            radioButton.setTag(reason.getVoidCode());

            radioButton.setGravity(Gravity.CENTER);
            radioButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            radioButton.setPadding(20, 20, 20, 20);
            radioGroup.addView(radioButton);
        }
        return radioGroup;
    }


    private void voidConfirmApi(int position, String reportType, List<VoidTransactionModel> voidTransactionModelList) {
        progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while fetching details...");
        progress.setCancelable(false);
        progress.show();

        String url = uFillEndpoint;
        JSONObject jsonObject = new JSONObject();
        try {
            extracted(position, reportType, voidTransactionModelList, jsonObject);
            Log.d("utr1111", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private static void extracted(int position, String reportType, List<VoidTransactionModel> voidTransactionModelList, JSONObject jsonObject) throws JSONException {
        jsonObject.put("channel", voidTransactionModelList.get(position).getChannel());
        jsonObject.put("reqDate", voidTransactionModelList.get(position).getReqDate());
        jsonObject.put("reqTime", voidTransactionModelList.get(position).getReqTime());
        jsonObject.put("userName", voidTransactionModelList.get(position).getUserName());
        jsonObject.put("mid", voidTransactionModelList.get(position).getMid());
        jsonObject.put("tid", voidTransactionModelList.get(position).getTid());
        jsonObject.put("roCode", voidTransactionModelList.get(position).getRoCode());
        jsonObject.put("txnType", "AVC");
        jsonObject.put("rrn", voidTransactionModelList.get(position).getVmsTxnID());
        jsonObject.put("reportType", reportType);
        jsonObject.put("reportDate", reportDate());
        jsonObject.put("amt", voidTransactionModelList.get(position).getVoucherAmt());
        jsonObject.put("dateTime", voidTransactionModelList.get(position).getDateTime());
        jsonObject.put("txnId", voidTransactionModelList.get(position).getTxnId());
        jsonObject.put("hwSrNo", Helper.serialNumber);
        jsonObject.put("latitude", "0");
        jsonObject.put("longitude", "0");
        jsonObject.put("geotagRange", "10");
        jsonObject.put("client", voidTransactionModelList.get(position).getClient());
        jsonObject.put("instId", voidTransactionModelList.get(position).getInstId());
        jsonObject.put("appVersion", version);
    }

    public void apiResult(String res, String apiName) {
        if (apiName.equals("ufill")) {
            try {
                if (res.equals("Server Time Out")) {
                    Log.d("timeout_problem", res);
                    progress.dismiss();
                    VoidTransaction.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(VoidTransaction.this, "Server Time Out", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Log.d("voidConfirmApi", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respCode = payLoad.getString("respCode");
                    String respDesc = payLoad.getString("respDesc");
                    if (respCode.equals("200")) {
                        progress.dismiss();
                            Intent intent = new Intent(VoidTransaction.this, VoidReceipt.class);
                            intent.putExtra("voidResponse", payLoad.toString());
                            intent.putExtra("mobileNum", mobileNum);
                            startActivity(intent);
                            finish();
                    } else {
                        this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                MessagesDialog.showDialog(context, respDesc, 0,null, null);

                                // Toast.makeText(context, respDesc+" - "+respCode, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }
}