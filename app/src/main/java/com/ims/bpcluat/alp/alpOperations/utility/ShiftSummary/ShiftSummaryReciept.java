package com.ims.bpcluat.alp.alpOperations.utility.ShiftSummary;

import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.cngHomePage;
import static com.ims.bpcluat.Helper.coverage;
import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.mobileNumberMasking;
import static com.ims.bpcluat.Helper.operatorFirstName;
import static com.ims.bpcluat.Helper.operatorLastName;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.resizeBitmap;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.username;
import static com.ims.bpcluat.Helper.version;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.databinding.ActivityShiftSummaryRecieptBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.AlpReceiptHelper;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.interfaces.PrintResponseCallBack;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ShiftSummaryReciept extends AppCompatActivity implements PrintResponseCallBack, ApiHelper.NetworkingApiCallBack {
    ActivityShiftSummaryRecieptBinding binding;
    Context context;
    ApiHelper api;
    ProgressDialog progress;
    Bitmap merchantTxnChargeSlip, customerTxnChargeSlip,fuelBillSlip;
    AlpReceiptHelper alpReceiptHelper = new AlpReceiptHelper();
    Date date = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
    String todayDate = formatter.format(date);
    String roMobileNo = "";
    String myDeviceInfo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShiftSummaryRecieptBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
        context = this;
        api = new ApiHelper();

        myDeviceInfo = Build.MODEL;
        Log.d("myDeviceInfo", myDeviceInfo);
        if (myDeviceInfo.equals("A50")) {
            binding.printBtn.setVisibility(View.GONE);
        }


        progress = new ProgressDialog(ShiftSummaryReciept.this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            askPermissionForStorage();
        } else {}
        getArgumentsData();
    }

    public void getArgumentsData() {
        String argData = getIntent().getStringExtra("payload");
        String summary = getIntent().getStringExtra("summary");

        if (argData != null) {
            try {
                Helper.logLongMessage("ShiftSummaryReceiptData",argData);
                Log.d("summary_check", summary);
                JSONObject payLoad = new JSONObject(argData);

                JSONArray outputArray = payLoad.getJSONArray("output");
                JSONObject outputObject = outputArray.getJSONObject(0);
                roMobileNo = outputObject.getString("roMobileNo");

                if (!roMobileNo.isEmpty()) {
                    roMobileNo = mobileNumberMasking(roMobileNo);
                }

                merchantTxnChargeSlip= alpReceiptHelper.shiftSummarySlip(ShiftSummaryReciept.this, payLoad,"MERCHANT COPY", roMobileNo, summary);
                Bitmap resizedBitmap = resizeBitmap(merchantTxnChargeSlip);
                binding.shiftSlip.setImageBitmap(resizedBitmap);

                binding.printBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        progress = new ProgressDialog(ShiftSummaryReciept.this);
                        progress.setTitle("Loading");
                        progress.setMessage("Wait while loading...");
                        progress.setCancelable(false);
                        progress.show();
                        alpReceiptHelper.merchantDialog(ShiftSummaryReciept.this, merchantTxnChargeSlip);
                        alpReceiptHelper.setCallback((PrintResponseCallBack) context);
                    }
                });

                binding.homeBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        merchantPrintNo();
                    }
                });

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void askPermissionForStorage() {
        ActivityCompat.requestPermissions((Activity) context, new String[]{
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }, 100);
    }

    @Override
    public void apiResult(String res, String apiName) {
        if (apiName.equals("saveBillerTxn")) {
            try {
                if (res.equals("Server Time Out")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(ShiftSummaryReciept.this, "Server Time Out", 0,null, null);
                        }
                    });
                } else {
                    Log.d("notificationResponse",res);
                }
            }catch (Exception e){
                Log.d("notificationRespException", e.toString());
            }
        }
    }

    public static void shiftSummaryHomePage(final Activity mActivity) {
        Intent intent = new Intent(mActivity, SideBarActivity.class);
        intent.putExtra("redirect", "UtilityFragment");
        mActivity.startActivity(intent);
        mActivity.finish();
    }

    @Override
    public void merchantPrintNo() {
        progress.dismiss();
        shiftSummaryHomePage(this);
        Log.d("printResult", "merchantPrintNo");
    }

    @Override
    public void merchantPrintYes() {
        Log.d("printResult", "merchantPrintYes");
        progress.dismiss();
        shiftSummaryHomePage(this);
    }

    @Override
    public void customerPrintNo() {

    }

    @Override
    public void customerPrintYes() {
    }

    @Override
    public void fuelBillPrintNo() {
    }

    @Override
    public void fuelBillPrintYes() {
    }

    @Override
    public void merchantPrintError(String errorResponse) {
        progress.dismiss();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, errorResponse, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void customerPrintError(String errorResponse) {
    }
}