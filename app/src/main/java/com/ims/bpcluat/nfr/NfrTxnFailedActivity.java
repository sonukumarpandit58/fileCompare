package com.ims.bpcluat.nfr;

import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.cngHomePage;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.nfrHomePage;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.tid;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ims.bpcluat.R;
import com.ims.bpcluat.cng.CngTxnFailedActivity;
import com.ims.bpcluat.databinding.ActivityCngTxnFailedBinding;
import com.ims.bpcluat.databinding.ActivityNfrTxnFailedBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;

import org.json.JSONException;
import org.json.JSONObject;

public class NfrTxnFailedActivity extends AppCompatActivity implements ApiHelper.NetworkingApiCallBack {

    ActivityNfrTxnFailedBinding binding;
    ProgressDialog progress;
    ApiHelper api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNfrTxnFailedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d("Open Page Name = ", "NfrTxnFailedActivity");
        api = new ApiHelper();

        Intent intent = getIntent();
        // Extract the data passed from the first activity
        if (intent != null && intent.hasExtra("upiFailed")) {
           // TxntimeoutApi();
        }

        binding.homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nfrHomePage(NfrTxnFailedActivity.this);
            }
        });

        binding.retryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void TxntimeoutApi() {
        progress = new ProgressDialog(NfrTxnFailedActivity.this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();
        String url = "raiseTxnDispute";
        JSONObject jsonObject = new JSONObject();
        Intent intent = getIntent();
        String mobileNum = intent.getStringExtra("mobileNum");
        String amount = intent.getStringExtra("transaction_amount");
        String txnid = intent.getStringExtra("txnid");
        try {

            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("userName", mobileNum);
            jsonObject.put("source", "POS");
            jsonObject.put("channel", channelName);
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("id", "98980912");
            jsonObject.put("rrn", "");
            jsonObject.put("mop", "BQR");
            jsonObject.put("amt", amount);
            jsonObject.put("mobNo", mobileNum);
            jsonObject.put("status", "Fail-Inquiry Timeout");
            jsonObject.put("txnId", txnid);
            jsonObject.put("dateTime", requestDate());

            Log.d("txnamountRequest=", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack((ApiHelper.NetworkingApiCallBack) NfrTxnFailedActivity.this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void apiResult(String res, String apiName) {
        if (apiName.equals("raiseTxnDispute")) {
            try {
                if (res.equals("Server Time Out")) {
                    progress.dismiss();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(NfrTxnFailedActivity.this, "Server Time Out", 0,null, null);
                        }
                    });
                } else {
                    Log.d("txnamountRequest = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respCode = payLoad.getString("respCode");
                    if (respCode.equals("200")) {
                        progress.dismiss();

                    } else {
                        runOnUiThread(new Runnable() {
                            String errorMsg = payLoad.getString("respCode");
                            String respDesc = payLoad.getString("respDesc");

                            @Override
                            public void run() {
                                progress.dismiss();
                                MessagesDialog.showDialog(NfrTxnFailedActivity.this, errorMsg +" - "+ respDesc, 0,null, null);

                                //  Toast.makeText(NfrTxnFailedActivity.this, errorMsg + " - " + respDesc, Toast.LENGTH_SHORT).show();
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