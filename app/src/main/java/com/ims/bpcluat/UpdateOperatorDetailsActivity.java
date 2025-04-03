package com.ims.bpcluat;

import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.tid;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ims.bpcluat.databinding.ActivityUpdateOperatorDetailsBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.model.OperatorListModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class UpdateOperatorDetailsActivity extends AppCompatActivity implements ApiHelper.NetworkingApiCallBack {
    @NonNull
    ActivityUpdateOperatorDetailsBinding binding;
    ProgressDialog progress;
    private Context mContext;
    int[] blankArray;
    ApiHelper api;
    private OperatorListModel operatorListModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUpdateOperatorDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mContext = this;
        api = new ApiHelper();
        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });



        Intent intent = getIntent();
        String inactive = intent.getStringExtra("inactive");
        String firstName = intent.getStringExtra("firstName");
        String lastName = intent.getStringExtra("lastName");
        String mobileNumber = intent.getStringExtra("mobileNumber");
        String emailId = intent.getStringExtra("emailId");

        binding.etFirstname.setText(firstName);
        binding.etLastname.setText(lastName);
        binding.etMobilenum.setText(mobileNumber);
        binding.etEmailid.setText(emailId);

        if (Objects.equals(inactive, "inactive")){
            binding.tvActive.setVisibility(View.VISIBLE);
            binding.tvDeactvie.setVisibility(View.GONE);
            binding.btSubmit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String firstName = binding.etFirstname.getText().toString().trim();
                    String lastName = binding.etLastname.getText().toString().trim();
                    String mobileNumber = binding.etMobilenum.getText().toString().trim();
                    String emailid = binding.etEmailid.getText().toString().trim();

                    String status = "Activate";
                    if (binding.chkbox.isChecked()) {
                        status = "Activate";
                    } else {
                        status = "Deactivate";
                    }

                    if (firstName.isEmpty()) {
                        binding.etFirstname.setError("Please enter first name");
                    } else {
                        getupdateOperatorListApi(firstName, lastName, mobileNumber, emailid, status);
                    }
                }
            });
        } else {
            binding.btSubmit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String firstName = binding.etFirstname.getText().toString().trim();
                    String lastName = binding.etLastname.getText().toString().trim();
                    String mobileNumber = binding.etMobilenum.getText().toString().trim();
                    String emailid = binding.etEmailid.getText().toString().trim();

                    String status = "Activate";
                    if (binding.chkbox.isChecked()) {
                        status = "Deactivate";
                    } else {
                        status = "Activate";
                    }

                    if (firstName.isEmpty()) {
                        binding.etFirstname.setError("Please enter first name");
                    } else {
                        getupdateOperatorListApi(firstName, lastName, mobileNumber, emailid, status);
                    }
                }
            });
        }



    }

    private void getupdateOperatorListApi(String firstName, String lastName, String mobileNumber, String emailid, String status) {
        progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();
        String url = "updateUserStatus";
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("userName", mobileNumber);
            jsonObject.put("statusUpdate", status);
            jsonObject.put("firstName", firstName);
            jsonObject.put("lastName", lastName);
            jsonObject.put("mobileNumber", mobileNumber);
            jsonObject.put("emailId", emailid);
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("webAddress", "");
            jsonObject.put("address", "");
            jsonObject.put("pin", "");
            jsonObject.put("source", "Mobile");
            jsonObject.put("channel", channelName);
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            Log.d("updateUserStatus = ", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack((ApiHelper.NetworkingApiCallBack) this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void apiResult(String res, String apiName) {
        if (apiName.equals("updateUserStatus")) {
            try {
                if (res.equals("Server Time Out")) {
                    progress.dismiss();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(UpdateOperatorDetailsActivity.this, "Server Time Out", 0,null, null);
                        }
                    });
                } else {
                    Log.d("UpdateResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respCode = payLoad.getString("respCode");
                    if (respCode.equals("200")) {
                        if (payLoad.getString("response").equals("SUCCESS")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progress.dismiss();
                                    Intent intent = new Intent(UpdateOperatorDetailsActivity.this,AdminSideBarActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                        }
                    } else {
                        progress.dismiss();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MessagesDialog.showDialog(UpdateOperatorDetailsActivity.this, respCode, 0,null, null);

                               // Toast.makeText(mContext, respCode, Toast.LENGTH_SHORT).show();
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
