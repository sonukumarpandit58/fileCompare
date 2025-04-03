package com.ims.bpcluat.cng;

import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.cngHomePage;
import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.username;

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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.TxnFailedActivity;
import com.ims.bpcluat.adapter.ActiveOperatorListAdapter;
import com.ims.bpcluat.databinding.ActivityAdminLoginBinding;
import com.ims.bpcluat.databinding.ActivityCngTxnFailedBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.fragment.ActiveOperatorFragment;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.model.OperatorListModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CngTxnFailedActivity extends AppCompatActivity implements ApiHelper.NetworkingApiCallBack {

    ActivityCngTxnFailedBinding binding;
    ProgressDialog progress;
    ApiHelper api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCngTxnFailedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d("Open Page Name = ", "CngTxnFailedActivity");
        api = new ApiHelper();

        Intent intent = getIntent();
        // Extract the data passed from the first activity
        if (intent != null && intent.hasExtra("upiFailed")) {
           // TxntimeoutApi();
        }

        binding.homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cngHomePage(CngTxnFailedActivity.this);
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
        progress = new ProgressDialog(CngTxnFailedActivity.this);
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
            api.setApiCallBack((ApiHelper.NetworkingApiCallBack) CngTxnFailedActivity.this);
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
                            MessagesDialog.showDialog(CngTxnFailedActivity.this, "Server Time Out", 0,null, null);
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
                                Toast.makeText(CngTxnFailedActivity.this, errorMsg + " - " + respDesc, Toast.LENGTH_SHORT).show();
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