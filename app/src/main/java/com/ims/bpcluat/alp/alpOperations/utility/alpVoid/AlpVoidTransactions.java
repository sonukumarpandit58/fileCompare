package com.ims.bpcluat.alp.alpOperations.utility.alpVoid;

import static com.ims.bpcluat.Helper.appVersion;
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
import com.ims.bpcluat.PaymentActivity;
import com.ims.bpcluat.adapter.alp_adapters.AlpTransactionAdapter;
import com.ims.bpcluat.databinding.ActivityAlpVoidTransactionsBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.interfaces.AlpVoidTransactionsInterface;
import com.ims.bpcluat.model.AlpModels.VoidModels.AlpTxnModel;
import com.ims.bpcluat.model.AlpModels.VoidModels.AlpVoidReasons;
import com.ims.bpcluat.model.AlpModels.VoidModels.TxnList;



import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AlpVoidTransactions extends AppCompatActivity implements ApiHelper.NetworkingApiCallBack, AlpVoidTransactionsInterface {
    ActivityAlpVoidTransactionsBinding binding;
    RecyclerView recyclerView;
    Context context;
    AlpTransactionAdapter alpTransactionAdapter;
    ArrayList<AlpTxnModel> alpTxnModelArrayList = new ArrayList<>();
    List<AlpVoidReasons> voidReasonsList = new ArrayList<>();
    ApiHelper api;
    ProgressDialog progress;
    String checkResult = "";
    String id = "", txnId = "", mobNo = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAlpVoidTransactionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        context = this;
        api = new ApiHelper();
        recyclerView = binding.alpVoidTransactionList;


        alpTxnModelArrayList = getIntent().getParcelableArrayListExtra("alpTxnModel");

        fetchConfigurationOrVoidReasonApi();

        alpTransactionAdapter = new AlpTransactionAdapter(context, alpTxnModelArrayList, this);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(alpTransactionAdapter);

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

    }

    @Override
    public void onClick(int position, List<AlpTxnModel> alpTxnModelList) {
        if (voidReasonsList != null && !voidReasonsList.isEmpty()) {
            showVoidReasonsPopup(position, alpTxnModelList);
            Log.d("positionggggggggggggg", String.valueOf(position));
        } else {
            MessagesDialog.showDialog(AlpVoidTransactions.this, "No void reasons available", 0,null, null);

          //  Toast.makeText(context, "No void reasons available", Toast.LENGTH_SHORT).show();
        }

    }

    private void showVoidReasonsPopup(int position, List<AlpTxnModel> alpTxnModelList) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Void Reason");
        RadioGroup radioGroup = getRadioGroup();
        builder.setView(radioGroup);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int selectedId = radioGroup.getCheckedRadioButtonId();
                if (selectedId != -1) {
                    RadioButton selectedRadioButton = radioGroup.findViewById(selectedId);
                    String selectedReason = selectedRadioButton.getText().toString();
                    String selectedVoidCode = (String) selectedRadioButton.getTag();
                    voidConfirmApi(position, selectedVoidCode, alpTxnModelList);
                    Log.d("positionggggggggggggg", String.valueOf(position));
                }else{
                    Toast.makeText(context, "Please select a reason before proceeding", Toast.LENGTH_SHORT).show();
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

    private void fetchConfigurationOrVoidReasonApi() {
        checkResult = "fetchConfigurationAPI";
        progress = new ProgressDialog(context);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);
        progress.show();
        String url = "alpreq";
        String reqDate = requestDate();
        String reqTime = requestTime();

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("userName", username);
            jsonObject.put("channel", "BPCL");
            jsonObject.put("tid", tid);
            jsonObject.put("mid", mid);
            jsonObject.put("source", "Mobile");
            jsonObject.put("reqDate", reqDate);
            jsonObject.put("reqTime", reqTime);
            jsonObject.put("dateTime", requestDate() + requestTime());
            jsonObject.put("txnType", "ACF");
            jsonObject.put("tranChannel", "VC");
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnId", Helper.createTxnIdForOfflineTxn());
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

    private void voidConfirmApi(int position, String voidCode, List<AlpTxnModel> alpTxnModelList) {
        checkResult = "voidConfirmApi";
        progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while fetching details...");
        progress.setCancelable(false);
        progress.show();
        String url = alpEndpoint;
        JSONObject jsonObject = new JSONObject();
        try {
            AlpTxnModel alpTxnModel = alpTxnModelArrayList.get(0);
            id = alpTxnModel.getId();
            txnId = alpTxnModel.getTxnId();
            mobNo = alpTxnModel.getMobNo();

            TxnList txnList = alpTxnModel.getOutput().get(0).getTxnList().get(position);
            jsonObject.put("channel", channelName);
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("userName", username);
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnType", "AVD");
            jsonObject.put("tranChannel", "VC");
            jsonObject.put("rrn", txnList.getAlpTransactionId());
            jsonObject.put("authCode", txnList.getClientTxnId());
            jsonObject.put("reportType", voidCode);
            jsonObject.put("amt", txnList.getTxnAmount());
            jsonObject.put("dateTime", requestDate() + requestTime());
            jsonObject.put("txnId", txnId);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", version);

            Log.d("ApiName","Void Confirm - API");
            Log.d("Request", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void apiResult(String res, String apiName) {
        if (checkResult.equals("fetchConfigurationAPI")) {
            try {
                if (res.equals("Server Time Out")) {
                    Log.d("timeout_problem", res);
                    progress.dismiss();
                    AlpVoidTransactions.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(AlpVoidTransactions.this, "Server Time Out", 0,null, null);
                        }
                    });
                } else {
                    Log.d("fetchConfigurationAPIREs", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respCode = payLoad.getString("respCode");
                    String respDesc = payLoad.getString("respDesc");

                    if (respCode.equals("200")) {
                        progress.dismiss();

                        JSONArray outputArray = payLoad.getJSONArray("output");
                        if (outputArray.length() > 0) {
                            JSONObject firstOutput = outputArray.getJSONObject(0);
                            JSONObject terminalParameter = firstOutput.getJSONObject("terminalParameter");
                            JSONArray reasonForVoid = terminalParameter.getJSONArray("reasonForVoid");

                            RadioGroup radioGroup = new RadioGroup(context);
                            radioGroup.setOrientation(RadioGroup.VERTICAL);

                            for (int i = 0; i < reasonForVoid.length(); i++) {
                                JSONObject reason = reasonForVoid.getJSONObject(i);
                                String reasonText = reason.getString("reason");
                                String reasonID = reason.getString("reasonID");
                                Log.d("VoidReason", "ID: " + reasonID + ", Reason: " + reasonText);

                                AlpVoidReasons voidReason = new AlpVoidReasons(reasonText, reasonID);
                                voidReasonsList.add(voidReason);

                            }

                        }

                    } else {
                        this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                MessagesDialog.showDialog(AlpVoidTransactions.this, respDesc , 0,null, null);
                            }
                        });
                    }

                }
            } catch (JSONException e) {
                Log.d("JSONException", "apiResult: ");
            }
        } else if (checkResult.equals("voidConfirmApi")) {
            try {
                if (res.equals("Server Time Out")) {
                    Log.d("timeout_problem", res);
                    progress.dismiss();
                    AlpVoidTransactions.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(AlpVoidTransactions.this, "Server Time Out",0, null, null);
                        }
                    });
                } else {
                    Log.d("voidConfirmApiRes", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respCode = payLoad.getString("respCode");
                    String respDesc = payLoad.getString("respDesc");

                    if (respCode.equals("200")) {
                        progress.dismiss();

                        this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();

                                Intent intent = new Intent(AlpVoidTransactions.this, AlpVoidReceipt.class);
                                intent.putExtra("payload", payLoad.toString());
                                intent.putExtra("mobileNum", mobNo);

                                startActivity(intent);
                                finish();
                            }
                        });

                    } else {
                        this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                MessagesDialog.showDialog(AlpVoidTransactions.this, respDesc, 0,null, null);
                            }
                        });
                    }

                }
            } catch (JSONException e) {
                Log.d("TAG", "apiResult: ");
            }
        }
    }

    private @NonNull RadioGroup getRadioGroup() {
        RadioGroup radioGroup = new RadioGroup(context);
        radioGroup.setOrientation(RadioGroup.VERTICAL);
        for (AlpVoidReasons reason : voidReasonsList) {
            RadioButton radioButton = new RadioButton(context);
            radioButton.setText(reason.getReason());
            radioButton.setTag(reason.getReasonID());

            radioButton.setGravity(Gravity.CENTER);
            radioButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            radioButton.setPadding(20, 20, 20, 20);
            radioGroup.addView(radioButton);
        }
        return radioGroup;
    }

}