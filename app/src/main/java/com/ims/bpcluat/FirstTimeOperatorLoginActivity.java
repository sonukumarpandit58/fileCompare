package com.ims.bpcluat;

import static com.ims.bpcluat.Helper.address1;
import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.city;
import static com.ims.bpcluat.Helper.client;
import static com.ims.bpcluat.Helper.coverage;
import static com.ims.bpcluat.Helper.instId;
import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.metaHosPumpUrl;
import static com.ims.bpcluat.Helper.metaHosSecretKey;
import static com.ims.bpcluat.Helper.metaHosTokenUrl;
import static com.ims.bpcluat.Helper.metaHosVendorId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.productsArray;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.roName;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.username;

import android.app.ProgressDialog;
import android.content.Context;
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

import com.ims.bpcluat.databinding.ActivityFirstTimeOperatorLoginBinding;
import com.ims.bpcluat.databinding.ActivityMainBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FirstTimeOperatorLoginActivity extends AppCompatActivity implements ApiHelper.NetworkingApiCallBack {

    ActivityFirstTimeOperatorLoginBinding binding;
    ApiHelper api;
    ProgressDialog progress;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFirstTimeOperatorLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mContext = this;
        api = new ApiHelper();

        binding.tpinSubmitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newTpinEdiText = binding.newTpinEdiText.getText().toString().trim();
                if (newTpinEdiText.isEmpty()) {
                    binding.newTpinEdiText.setError("PLease enter new tpin");
                    binding.newTpinEdiText.requestFocus();
                } else if (newTpinEdiText.length() < 4) {
                    binding.newTpinEdiText.setError("tpin length must be 4 digits");
                    binding.newTpinEdiText.requestFocus();
                } else {
                    resetTpinApi(newTpinEdiText);
                }
            }
        });
    }

    private void resetTpinApi(String newTpinEdiText) {
        progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();
        String url = "resetTPin";
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("userName", username);
            jsonObject.put("password", newTpinEdiText);
            jsonObject.put("source", "Mobile");
            jsonObject.put("channel", channelName);
            jsonObject.put("resetFlag", "1");
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            Log.d("ResetTpinRequest = ", String.valueOf(jsonObject));
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
        if (apiName.equals("resetTPin")) {
            try {
                if (res.equals("Server Time Out")) {
                    progress.dismiss();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(FirstTimeOperatorLoginActivity.this, "Server Time Out",0, null, null);
                            // Toast.makeText(mContext, "Server Time Out", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Log.d("ResetTpinResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");
                    if (respCode.equals("200")) {
                        //progress.dismiss();
                        sapConfigApi();
                    } else {
                        progress.dismiss();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                MessagesDialog.showDialog(FirstTimeOperatorLoginActivity.this, respDesc,0, null, null);

                                //  Toast.makeText(mContext, respDesc + " - " + respCode, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        } else if (apiName.equals("getSapConfig")) {
            try {
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

                    coverage = roMasterObject.getString("COVERAGE");
                    roName = roMasterObject.getString("RO_Name");
                    address1 = roMasterObject.getString("Address1");
                    city = roMasterObject.getString("City");

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
                    productsArray = outputObject.getJSONObject("otherConfig").getJSONObject("bpclConfig").getJSONArray("fuelProductList");

                    //  JSONArray productListArray = outputObject.getJSONObject("otherConfig").getJSONObject("bpclConfig").getJSONArray("fuelProductList");
                    Log.d("productListArray", String.valueOf(productsArray));
                    progress.dismiss();
                    Intent intent = new Intent(FirstTimeOperatorLoginActivity.this, SideBarActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMsg = payLoad.getString("respCode");
                    String respDesc = payLoad.getString("respDesc");
                    progress.dismiss();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(FirstTimeOperatorLoginActivity.this, errorMsg +" - "+ respDesc, 0,null, null);

                         //   Toast.makeText(mContext, errorMsg + " - " + respDesc, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }
}