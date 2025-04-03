package com.ims.bpcluat;

import static com.ims.bpcluat.Helper.*;
import static com.ims.bpcluat.ReadWriteHelper.checkAndUploadStoredFiles;
import static com.ims.bpcluat.ReadWriteHelper.testUpload;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.firstdata.merchantservicessdk.MSApi;
import com.google.gson.Gson;
import com.ims.bpcluat.databinding.ActivityMainBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.receiver.ConnectivityReceiver;
import com.ims.bpcluat.receiver.InternetCheckService;
import com.ims.bpcluat.utils.Cache;
import com.ims.bpcluat.validation.MobileNoValidation;
import com.pax.fdms.opensdk.base24.Base24Constant;
import com.pax.fdms.opensdk.base24.Base24Request;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.ims.bpcluat.helper.ApiHelper.NetworkingApiCallBack;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements NetworkingApiCallBack {
    ActivityMainBinding binding;
    ApiHelper api;
    ProgressDialog progress;
    private Context mContext;
    int[] blankArray;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private ConnectivityReceiver connectivityReceiver = new ConnectivityReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        startService(new Intent(this, InternetCheckService.class));
        mContext = this;

        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(this));
        api = new ApiHelper();
        Helper.pumpArray.clear();
        myDeviceInfo = Build.MODEL;
        Cache.listCacheFiles(getApplicationContext());
        Helper.pumpFetch = "";

        //REQUEST FILE CHECKING...
        checkAndUploadStoredFiles(mContext);

        // Call the helper method to get the serial number
        Helper.getHardwareSerialNumber(this, new Helper.PermissionCallback() {
            @Override
            public void onPermissionGranted(String serialNumber) {
                Log.d("MainActivity", "SerialNo4: " + Helper.serialNumber);
              //  Helper.serialNumber = "2840552875";   // Sonu - 2840552875 , Kavi - 1491845037
            }

            @Override
            public void onPermissionDenied() {
                Log.d("MainActivity", "Permission was denied.");
            }
        });

        SharedPreferences nfrData = getSharedPreferences("nfrSharedPreferencesData", Context.MODE_PRIVATE);
        nfrData.edit().clear().commit();

        MyApplication app = (MyApplication) getApplicationContext();
        app.setBackButtonEnabled(false);

        if (mid.isEmpty() && tid.isEmpty()) {
            Base24Request request = new Base24Request();
            request.setFunctionCode(Base24Constant.NAC);
            JSONObject requestParams = new JSONObject();
            try {
                requestParams.put("base24Request", new Gson().toJson(request));
                MSApi.getInstance().doPayment(this, 123, requestParams);
            } catch (JSONException e) {
                Log.d("sslogError", e.toString());
                // throw new RuntimeException(e);
            }
        }

        binding.loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    // Permission is already granted, proceed with file writing
                    ReadWriteHelper.createRequestFile(MainActivity.this,"");
                    File bpclFolder = new File(Environment.getExternalStorageDirectory(), "BPCL Log Data");
                    if (!bpclFolder.exists()) {
                        bpclFolder.mkdirs();
                    }
                } else {
                    // Request necessary permissions
                    askPermissionForStorage();
                }
                validateFields();
            }
        });

        binding.adminLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AdminLoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void validateFields() {
        String mobileNumber = binding.mobileNumberEditText.getText().toString().trim();
        String tpin = binding.tpinEditText.getText().toString().trim();
        if (TextUtils.isEmpty(mobileNumber) && TextUtils.isEmpty(tpin)) {
            binding.mobileNumberEditText.setError("Please enter mobile number");
            binding.tpinEditText.setError("Please enter tpin");
            binding.mobileNumberEditText.requestFocus();
            binding.tpinEditText.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(mobileNumber)) {
            binding.mobileNumberEditText.setError("Please enter mobile number");
            binding.mobileNumberEditText.requestFocus();
            return;
        }
        if (mobileNumber.length() != 10) {
            binding.mobileNumberEditText.setError("Mobile Number must be 10 digits");
            binding.mobileNumberEditText.requestFocus();
            return;
        }
        if (mobileNumber.startsWith("0")) {
            binding.mobileNumberEditText.setError("Mobile Number cannot start with zero");
            binding.mobileNumberEditText.requestFocus();
            return;
        }
        if (mobileNumber.equals("1234567890")) {
            binding.mobileNumberEditText.setError("Please enter valid mobile number");
            binding.mobileNumberEditText.requestFocus();
            return;
        }
        if (MobileNoValidation.hasSameNumber(mobileNumber)) {
            binding.mobileNumberEditText.setError("All digits of mobile number cannot be same.");
            binding.mobileNumberEditText.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(tpin)) {
            binding.tpinEditText.setError("Please enter tpin");
            binding.tpinEditText.requestFocus();
            return;
        }
        if (connectivityReceiver.isConnected(this)) {
            // Internet connection is available, proceed with API call
            binding.loginBtn.setEnabled(false);  // disable submit button
            loginApi();
        } else {
            MessagesDialog.showDialog(MainActivity.this, "No internet connection", 0,null, null);

           // Toast.makeText(mContext, "No internet connection", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("myLogrequestCode", String.valueOf(requestCode));
        Log.d("myLogresultCode", String.valueOf(resultCode));
        Log.d("myLogdata", String.valueOf(data));

        if (requestCode == 123) {
            if (resultCode == RESULT_OK) {
                String response = data.getStringExtra("response");
                Log.d("myLogResponse", response);
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONObject base24Response = jsonObject.getJSONObject("base24Response");
                    mid = base24Response.getString("fdMID");
                    tid = base24Response.getString("fdTID");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void loginApi() {
        username = binding.mobileNumberEditText.getText().toString().trim();
        String tpin = binding.tpinEditText.getText().toString().trim();
        progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();
        String url = "userLogin";
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("source", "Mobile");
            jsonObject.put("channel", channelName);
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("userType", "Operator");
            jsonObject.put("userName", username);
            jsonObject.put("password", tpin);
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("operatorDetail", blankArray);
            jsonObject.put("result", blankArray);
            jsonObject.put("billerTranList", blankArray);

            Log.d("LoginRequest = ", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void sapConfigApi() {
        String url = "getSapConfig";
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("userType", "Operator");
            jsonObject.put("userName", username);
            jsonObject.put("sapCode", sapCode);
            jsonObject.put("source", "Mobile");
            jsonObject.put("channel", channelName);
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            Log.d("SapConfigRequest = ", String.valueOf(jsonObject));
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
                binding.loginBtn.setEnabled(true);  //Re-enable submit button
            }
        });
        if (apiName.equals("userLogin")) {
            try {
                if (res.equals("Server Time Out")) {
                    progress.dismiss();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(MainActivity.this, "Server Time Out",0, null, null);
                        }
                    });
                } else {
                    Log.d("LoginResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");
                    if (respCode.equals("200")) {
                        if (payLoad.getString("response").equals("SUCCESS")) {
                            if (payLoad.getString("userType").equals("Operator")) {
                                String firstName = payLoad.getString("firstName");
                                String lastName = payLoad.getString("lastName");
                                if (!firstName.isEmpty()) {
                                    operatorFirstName = firstName;
                                }
                                if (!lastName.isEmpty()) {
                                    operatorLastName = lastName;
                                }
                                sapCode = payLoad.getString("sapCode");
                                loginType = "Opeartor";
                                operatorLoginTime = requestTime();
                                operatorLoginDate = requestDate();
                                if (payLoad.getString("userStatus").equals("Deactivate")) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            progress.dismiss();
                                            MessagesDialog.showDialog(MainActivity.this, "You are deactivated user please contact the Admin", 0,null, null);

                                            //Toast.makeText(mContext, "You are deactivated user please contact the Admin", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    if (payLoad.getString("resetFlag").equals("0")) {
                                        progress.dismiss();
                                        Intent intent = new Intent(MainActivity.this, FirstTimeOperatorLoginActivity.class);
                                        startActivity(intent);
                                        finish();
                                    } else if (payLoad.getString("resetFlag").equals("1")) {
                                        if (payLoad.getString("userStatus").equals("Activate")) {
                                            sapConfigApi();
                                        }
                                    } else if (payLoad.getString("resetFlag").equals("2")) {
                                        sapConfigApi();
                                    }
                                }
                            }
                        } else {
                            progress.dismiss();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    MessagesDialog.showDialog(MainActivity.this, respDesc + " - " + respCode, 0,null, null);

                                   // Toast.makeText(mContext, respDesc + " - " + respCode, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        progress.dismiss();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MessagesDialog.showDialog(MainActivity.this, respDesc + " - " + respCode, 0,null, null);
                            }
                        });
                    }
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        } else if (apiName.equals("getSapConfig")) {
            try {
                if (res.equals("Server Time Out")) {
                    progress.dismiss();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(MainActivity.this, "Server Time Out", 0,null, null);

                            // Toast.makeText(mContext, "Server Time Out", Toast.LENGTH_SHORT).show();

                        }
                    });
                } else {
                    Log.d("SapConfigResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    if (payLoad.getString("respCode").equals("200")) {
                        sapCode = payLoad.getString("sapCode");
                        client = payLoad.getString("client");
                        instId = payLoad.getString("instId");

                        JSONArray outputArray = payLoad.getJSONArray("output");
                        JSONObject outputObject = outputArray.getJSONObject(0);
                        JSONArray roMasterArray = outputObject.getJSONArray("roMaster");
                        JSONObject roMasterObject = roMasterArray.getJSONObject(0);

                        roOnlineStatus = roMasterObject.getString("roOnlineStatus");
                        Log.d("roOnlineStatus",roOnlineStatus);
                        coverage = roMasterObject.getString("COVERAGE");
                        roName = roMasterObject.getString("RO_Name");
                        address1 = roMasterObject.getString("Address1");
                        city = roMasterObject.getString("City");
                        if(roMasterObject.has("FooterMessage")){
                            footerMessage = roMasterObject.getString("FooterMessage");
                        }

                        if(roMasterObject.has("MobileNo")){
                            dealerContactNumber = roMasterObject.getString("MobileNo");
                        }

                        JSONObject metaHosObj = outputObject.getJSONObject("otherConfig").getJSONObject("bpclConfig").getJSONObject("metaHOS");
                        if(metaHosObj.has("secret_key")){
                            metaHosSecretKey = metaHosObj.getString("secret_key");
                        }
                        if(metaHosObj.has("vendor_id")){
                            metaHosVendorId = metaHosObj.getString("vendor_id");
                        }
                        if(metaHosObj.has("tokenUrl")){
                            metaHosTokenUrl = metaHosObj.getString("tokenUrl");
                        }
                        if(metaHosObj.has("baseUrl")){
                            metaHosPumpUrl = metaHosObj.getString("baseUrl");
                        }
                        progress.dismiss();

                        productsArray = outputObject.getJSONObject("otherConfig").getJSONObject("bpclConfig").getJSONArray("fuelProductList");

                        for(int i =0 ;i<productsArray.length(); i++){
                            JSONObject productJson = productsArray.getJSONObject(i);
                            String id = productJson.getString("id");
                            String productAlias = productJson.getString("productAlias");
                            String productName = productJson.getString("productName");
                            addProduct(fuelProductList, id, productName, productAlias);
                        }
                        //  JSONArray productListArray = outputObject.getJSONObject("otherConfig").getJSONObject("bpclConfig").getJSONArray("fuelProductList");
                        Log.d("productListArray", String.valueOf(productsArray));
                        Intent intent = new Intent(MainActivity.this, SideBarActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        String errorMsg = payLoad.getString("respCode");
                        String respDesc = payLoad.getString("respDesc");
                        progress.dismiss();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MessagesDialog.showDialog(mContext, errorMsg +" - "+ respDesc, 0,null, null);

                               // Toast.makeText(mContext, errorMsg + " - " + respDesc, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            } catch (JSONException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(progress.isShowing()){
                            progress.dismiss();
                        }
                        MessagesDialog.showDialog(mContext, e.toString(), 0,null, null);

                       // Toast.makeText(mContext, e.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private void getDeviceIdentifiers() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

//        String imei = null;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            // For Android 10 and above, getting the IMEI directly is restricted.
//            // You can use other identifiers like the hardware serial number.
//            imei = Build.getSerial(); // Requires READ_PHONE_STATE permission.
//        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            imei = telephonyManager.getImei();
//        } else {
//            imei = telephonyManager.getDeviceId();
//        }

        // Toast.makeText(this, "Device Identifier: " + imei, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d("methodCalled","onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with writing to the file
                ReadWriteHelper.createRequestFile(MainActivity.this,"");
                File bpclFolder = new File(Environment.getExternalStorageDirectory(), "BPCL Log Data");
                if (!bpclFolder.exists()) {
                    bpclFolder.mkdirs();
                }
            } else {
                // Permission denied
                Toast.makeText(MainActivity.this, "Permission denied. Cannot write to file.", Toast.LENGTH_SHORT).show();
            }
        }else{
            Helper.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyApplication app = (MyApplication) getApplicationContext();
        app.setBackButtonEnabled(true);
    }

    private void askPermissionForStorage() {
        ActivityCompat.requestPermissions((Activity) MainActivity.this, new String[]{
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }, 100);
    }
}