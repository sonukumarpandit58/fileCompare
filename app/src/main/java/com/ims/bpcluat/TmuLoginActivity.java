package com.ims.bpcluat;

import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.username;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.ims.bpcluat.databinding.ActivityTmuLoginBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.receiver.ConnectivityReceiver;
import com.ims.bpcluat.validation.MobileNoValidation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TmuLoginActivity extends AppCompatActivity {

    ActivityTmuLoginBinding binding;
    ApiHelper api;
    ProgressDialog progress;
    private Context mContext;
    int[] blankArray;
    private static final int PERMISSION_REQUEST_CODE = 1;
    String deviceId;
    private ConnectivityReceiver connectivityReceiver = new ConnectivityReceiver();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTmuLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mContext = this;
        api = new ApiHelper();

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_PHONE_STATE}, PERMISSION_REQUEST_CODE);
//            } else {
//                getDeviceId();
//            }
//        } else {
//            getDeviceId();
//        }

        binding.adminLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TmuLoginActivity.this,AdminLoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        binding.operatorLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TmuLoginActivity.this,MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        binding.tmuLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateFields();
            }
        });
    }

    private void validateFields() {
        String mobileNumber = binding.tmuMobileEditText.getText().toString().trim();
        String tpin = binding.tmuTpinEditText.getText().toString().trim();
        if (TextUtils.isEmpty(mobileNumber) && TextUtils.isEmpty(tpin)) {
            binding.tmuMobileEditText.setError("Please enter mobile number");
            binding.tmuTpinEditText.setError("Please enter tpin");
            binding.tmuMobileEditText.requestFocus();
            binding.tmuTpinEditText.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(mobileNumber)) {
            binding.tmuMobileEditText.setError("Please enter mobile number");
            binding.tmuMobileEditText.requestFocus();
            return;
        }
        if (mobileNumber.length() != 10) {
            binding.tmuMobileEditText.setError("Mobile Number must be 10 digits");
            binding.tmuMobileEditText.requestFocus();
            return;
        }
        if (mobileNumber.startsWith("0")) {
            binding.tmuMobileEditText.setError("Mobile Number cannot start with zero");
            binding.tmuMobileEditText.requestFocus();
            return;
        }
        if (mobileNumber.equals("1234567890")) {
            binding.tmuMobileEditText.setError("Please enter valid mobile number");
            binding.tmuMobileEditText.requestFocus();
            return;
        }
        if (MobileNoValidation.hasSameNumber(mobileNumber)) {
            binding.tmuMobileEditText.setError("All digits of mobile number cannot be same.");
            binding.tmuMobileEditText.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(tpin)) {
            binding.tmuTpinEditText.setError("Please enter tpin");
            binding.tmuTpinEditText.requestFocus();
            return;
        }
        if (connectivityReceiver.isConnected(this)) {
            binding.tmuLoginBtn.setEnabled(false);  // disable submit button
            loginApi(mobileNumber,tpin);
        } else {
            MessagesDialog.showDialog(TmuLoginActivity.this, "No internet connection", 0,null, null);

          //  Toast.makeText(TmuLoginActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
        }
    }

    private void loginApi(String mobileNumber, String tpin) {
        progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);
        progress.show();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        JSONObject jsonBody = new JSONObject();
        JSONArray loginDetailArray = new JSONArray();
        JSONObject loginDetail = new JSONObject();
        try {
            loginDetail.put("mobileNumber", mobileNumber);
            loginDetail.put("password", tpin);
            loginDetail.put("clientKey", "FDAPMT");
            loginDetail.put("deviceId", Helper.getDeviceUuid(this));
            loginDetailArray.put(loginDetail);
            jsonBody.put("loginDetail", loginDetailArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String jsonString = jsonBody.toString();
        Log.d("TmuLoginRequest", jsonString);
        RequestBody body = RequestBody.create(jsonString, JSON);

        Request request = new Request.Builder()
                .url("https://atsmobileapi.firstdata.com/mobileapi/api/User/login?context=fe")
                .post(body)
                .header("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progress.dismiss();
                    binding.tmuLoginBtn.setEnabled(true);
                    Toast.makeText(getApplicationContext(), "Request Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                String responseData = response.body().string();
                String responseCode = String.valueOf(response.code());
                Log.d("TmuLoginResponse", responseData);
                Log.d("tmuResponse", "Code: " + responseCode);
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        binding.tmuLoginBtn.setEnabled(true);
                        progress.dismiss();
                        Intent intent = new Intent(TmuLoginActivity.this, AdminSideBarActivity.class);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    Log.e("Login Error", responseData);
                    try{
                        if(!responseCode.equals("200")){
                            String messageArray = response.message();
                            JSONArray jsonArray = new JSONArray(messageArray);
                            JSONObject jsonObject = jsonArray.getJSONObject(0);
                            String status = jsonObject.getString("status");
                            String message = jsonObject.getString("message");
                            runOnUiThread(() -> {
                                binding.tmuLoginBtn.setEnabled(true);
                                progress.dismiss();
                                MessagesDialog.showDialog(TmuLoginActivity.this, message + " - " + status,0, null, null);
                            });
                        }
                    }catch (JSONException e){
                        Log.d("TmuLoginException",e.toString());
                    }
                }
            }
        });
    }

}