package com.ims.bpcluat;

import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.*;
import static com.ims.bpcluat.Helper.username;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.ims.bpcluat.databinding.ActivityAdminLoginBinding;
import com.ims.bpcluat.databinding.ActivityMainBinding;
import com.ims.bpcluat.databinding.ActivitySuccessBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.receiver.ConnectivityReceiver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AdminLoginActivity extends AppCompatActivity implements ApiHelper.NetworkingApiCallBack {

    ActivityAdminLoginBinding binding;
    ProgressDialog progress;
    private Context mContext;
    int[] blankArray;
    ApiHelper api;
    private ConnectivityReceiver connectivityReceiver = new ConnectivityReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mContext = this;
        binding.midEditText.setText(mid);
        api = new ApiHelper();
        binding.operatorLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminLoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        binding.tmuLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminLoginActivity.this, TmuLoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        binding.adminLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mid = binding.midEditText.getText().toString().trim();
                String tpin = binding.tpinEditText.getText().toString().trim();
                if (mid.isEmpty() && tpin.isEmpty()) {
                    binding.midEditText.setError("");
                    binding.tpinEditText.setError("Please enter tpin");
                } else {
                    if (tpin.isEmpty()) {
                        binding.tpinEditText.setError("Please enter tpin");
                        binding.tpinEditText.requestFocus();
                    } else if (tpin.length() != 4) {
                        binding.tpinEditText.setError("Tpin length must be 4 digits");
                        binding.tpinEditText.requestFocus();
                    } else {
                        if (connectivityReceiver.isConnected(getApplicationContext())) {
                            binding.adminLoginBtn.setEnabled(false); // disable submit button
                            loginApi(mid, tpin);
                        } else {
                            MessagesDialog.showDialog(AdminLoginActivity.this, "No internet connection", 0,null, null);

                            //Toast.makeText(AdminLoginActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });
    }

    private void loginApi(String mid, String tpin) {
        progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();
        String url = "userLogin";
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("source", "Mobile");
            jsonObject.put("channel", channelName);
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("userType", "Admin");
            jsonObject.put("userName", mid);
            jsonObject.put("password", tpin);
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("operatorDetail", blankArray);
            jsonObject.put("result", blankArray);
            jsonObject.put("billerTranList", blankArray);

            Log.d("AdminLoginRequest =", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void apiResult(String res, String apiName) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.adminLoginBtn.setEnabled(true);  //Re-enable submit button
            }
        });

        try {
            if (res.equals("Server Time Out")) {
                progress.dismiss();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MessagesDialog.showDialog(AdminLoginActivity.this, "Server Time Out", 0,null, null);

                        //Toast.makeText(AdminLoginActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();

                    }
                });
            } else {
                Log.d("AdminLoginResponse = ", res);
                JSONObject jsonObject = new JSONObject(res);
                JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                String respDesc = payLoad.getString("respDesc");
                String respCode = payLoad.getString("respCode");
                if (respCode.equals("200")) {
                    progress.dismiss();
                    Intent intent = new Intent(AdminLoginActivity.this, AdminSideBarActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    progress.dismiss();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(AdminLoginActivity.this, respDesc,0, null, null);

                            //Toast.makeText(mContext, respDesc + " - " + respCode, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}