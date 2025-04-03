package com.ims.bpcluat;

import static android.app.PendingIntent.getActivity;
import static com.ims.bpcluat.Helper.*;
import static com.ims.bpcluat.validation.VehicleNoValidation.bharatVehicleNoValidation;
import static com.ims.bpcluat.validation.VehicleNoValidation.validateVehicleNo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Toast;

import com.firstdata.merchantservicessdk.MSApi;
import com.google.gson.Gson;
import com.ims.bpcluat.adapter.SpinnerAdapter;
import com.ims.bpcluat.cng.CngPaymentActivity;
import com.ims.bpcluat.conversion.CreateTransactionId;
import com.ims.bpcluat.conversion.DecimalToHex;
import com.ims.bpcluat.conversion.HexToDecimal;
import com.ims.bpcluat.databinding.ActivityPaymentBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.interfaces.NetworkCallback;
import com.ims.bpcluat.model.OnlineTxnModel;
import com.ims.bpcluat.helper.NozzleIDMapper;
import com.ims.bpcluat.interfaces.NetworkCallback;
import com.ims.bpcluat.model.OnlineTxnModel;
import com.ims.bpcluat.nfr.NfrPaymentActivity;
import com.ims.bpcluat.utils.NetworkUtils;
import com.ims.bpcluat.validation.MobileNoValidation;
import com.pax.fdms.opensdk.base24.Base24Constant;
import com.pax.fdms.opensdk.base24.Base24Request;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class PaymentActivity extends AppCompatActivity implements NetworkCallback {
    ActivityPaymentBinding binding;
    private static final int REQUEST_CAMERA_CODE = 100;
    RadioButton cashRadioButton, cardRadioButton, upiRadioButton,saleRadioButton;
    private static final int CARD = 1;
    private static final int UPI = 2;
    private static final int FASTAG = 3;
    private OnlineTxnModel onlineTxnModel;
    String amount,isTxnOnline,chargeslipTxnId;
    Date date = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
    String todayDate = formatter.format(date);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        cashRadioButton = findViewById(R.id.cashRadioButton);
        cardRadioButton = findViewById(R.id.cardRadioButton);
        upiRadioButton = findViewById(R.id.upiRadioButton);
        saleRadioButton = findViewById(R.id.saleRadioButton);

        List<String> vehicleType = new ArrayList<>();
        vehicleType.add("Select Vehicle Type");
        vehicleType.add("2 W");
        vehicleType.add("3 W");
        vehicleType.add("4 W");
        vehicleType.add("LCV");
        vehicleType.add("HCV");
        vehicleType.add("Others");

        int hidingItemIndex = 0;
        SpinnerAdapter customAdapter = new SpinnerAdapter(this, android.R.layout.simple_spinner_item, vehicleType, hidingItemIndex);
        customAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.vehicleTypeSpinner.setAdapter(customAdapter);

        onlineTxnModel = getIntent().getParcelableExtra("onlineTxnModel");
        if (onlineTxnModel != null) {
            amount = onlineTxnModel.getAmount();
            binding.pumpNo.setText(onlineTxnModel.getPumpNo());
            binding.product.setText(onlineTxnModel.getProductName());
            binding.amount.setText(amount);
            binding.qty.setText(onlineTxnModel.getQty() + " L");
            isTxnOnline = onlineTxnModel.getIsTxnOnline();
            if(isTxnOnline.equals("yes")){
                chargeslipTxnId = createTxnId();
                onlineTxnModel.setTxnId(chargeslipTxnId);
            }
        }

        NozzleIDMapper nozzleIDMapper = new NozzleIDMapper();
        String pumpforlocalMpd = onlineTxnModel.getPumpNo();
        if (pumpforlocalMpd.startsWith("0") && pumpforlocalMpd.length() > 1) {
            pumpforlocalMpd = pumpforlocalMpd.substring(1);
        }
        String localMPDID = nozzleIDMapper.getLocalMPDIDForGlobalNozzleID(Helper.metaHosResponse, pumpforlocalMpd);
        onlineTxnModel.setLocalMPDId(localMPDID);

        Intent intent = getIntent();
        String Insertcard = intent.getStringExtra("Insertcard");
        String isTxnOnline = intent.getStringExtra("isTxnOnline");

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PaymentActivity.this, SideBarActivity.class);
                if (Objects.equals(Insertcard, "Insertcard")) {
                    intent.putExtra("redirect", "OnlineSingleTransactionFragment");
                    startActivity(intent);
                    finish();
                } else if (Objects.equals(isTxnOnline, "isTxnOnline")) {
                    intent.putExtra("redirect", "offlinefragment");
                    startActivity(intent);
                    finish();
                } else{
                    finish();
                }
            }
        });

        binding.vehicleTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != 0) {
                    // setSpinnerError(null);  // Hide error if a valid item is selected
                    binding.spinnerError.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        mobileNumberListener();
        vehicleNumberListener();
        handleMopRadioButtonEvent();

        binding.customerDeclineCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                paymentEvent();
            }
        });

        binding.vehicleNo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_RIGHT = 2; // Index 2 for drawableEnd

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (binding.vehicleNo.getRight() - binding.vehicleNo.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        cameraOpen();
                        return true; // Consume the touch event
                    }
                }
                return false; // No click detected on the drawable
            }
        });

//        SdkHelper sdkHelper =  new SdkHelper();
//        sdkHelper.cardPayment(this);
//        binding.back.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Toast.makeText(PaymentActivity.this, "Back button pressed", Toast.LENGTH_SHORT).show();
//            }
//        });

        binding.payBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selectedMop = getSelectedRadioButton();
                String mob = binding.mobileNo.getText().toString().trim();
                String veh = binding.vehicleNo.getText().toString().trim();
                if (selectedMop != null) {
                    if (selectedMop.equals("FASTAG")) {
                        if (veh.isEmpty()) {
                            binding.vehicleNo.setError("For FASTag, Vehicle Number must be entered.");
                            binding.vehicleNo.requestFocus();
                        } else {
                            if (ValidationErrorShow(mob, veh)) {
                                payment(selectedMop);
                            }
                        }
                    }
                    else {
                        if (binding.customerDeclineCheckBox.isChecked()) {
                            payment(selectedMop);
                        } else {
                            if (ValidationErrorShow(mob, veh)) {
                                payment(selectedMop);
                            }
                        }
                    }
                } else {
                     MessagesDialog.showDialog(PaymentActivity.this, "Please select one payment option", 0,null, null);

                   // Toast.makeText(PaymentActivity.this, "Please select one payment option", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void handleMopRadioButtonEvent() {
        cashRadioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    cardRadioButton.setChecked(false);
                    upiRadioButton.setChecked(false);
                    saleRadioButton.setChecked(false);
                    paymentEvent();
                }
            }
        });

        cardRadioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    cashRadioButton.setChecked(false);
                    upiRadioButton.setChecked(false);
                    saleRadioButton.setChecked(false);
                    paymentEvent();
                }
            }
        });

        upiRadioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    cashRadioButton.setChecked(false);
                    cardRadioButton.setChecked(false);
                    saleRadioButton.setChecked(false);
                    paymentEvent();
                }
            }
        });

        //for sale button

        saleRadioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    cashRadioButton.setChecked(false);
                    cardRadioButton.setChecked(false);
                    upiRadioButton.setChecked(false);
                    paymentEvent();
                }
            }
        });
    }

    private String getSelectedRadioButton() {
        if (cashRadioButton.isChecked()) {
            return "CASH";
        } else if (cardRadioButton.isChecked()) {
            return "CARD";
        } else if (upiRadioButton.isChecked()) {
            return "BQR";
        } else if (saleRadioButton.isChecked()){
            return "SALES";
        }
        return null; // If none are checked
    }

    private void mobileNumberListener() {
        binding.mobileNo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                paymentEvent();
            }
        });
    }

    private void vehicleNumberListener() {
        binding.vehicleNo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                paymentEvent();
            }
        });
    }

    private void payment(String selectedMop) {
        try {
            String vType = binding.vehicleTypeSpinner.getSelectedItem().toString();
            if (vType.equals("Select Vehicle Type")) {
                binding.spinnerError.setText("Please select vehicle type");
                binding.spinnerError.setVisibility(View.VISIBLE);
            } else {
                binding.spinnerError.setVisibility(View.GONE);
                onlineTxnModel.setVehicleType(vType);

                onlineTxnModel.setMobileNumber(binding.mobileNo.getText().toString().trim());
                onlineTxnModel.setVehicleNumber(binding.vehicleNo.getText().toString().trim());
                onlineTxnModel.setTxnType(selectedMop);

                JSONObject requestParams = new JSONObject();
                if (selectedMop.equals("CASH")) {
                    String date = cashNotificationDate();
                    String time = cashNotificationTime();
                    onlineTxnModel.setTxnNotificationDate(date);
                    onlineTxnModel.setTxnNotificationTime(time);
                    onlineTxnModel.setTxnChargselipDate(cashChargeslipDate());
                    onlineTxnModel.setTxnChargeslipTime(cashChargeslipTime());
                    onlineTxnModel.setRrn("");
                    onlineTxnModel.setAuthCode("");
                    onlineTxnModel.setBatchNo("");
                    onlineTxnModel.setBleTxnMop("01");
                    onlineTxnModel.setBlePaymentMode("07");
                    onlineTxnModel.setField3("");
                    redirectToSuccessPage();
                } else if (selectedMop.equals("CARD")) {
                    Base24Request request = new Base24Request();
                    request.setFunctionCode(Base24Constant.TYPE_SALE); //sale
                    request.setTotalTxnAmount(txnAmountUpToTwoDecimal(amount));
                    request.setSuppressPrintChargeSlips("y"); //can be “y” or “n”
                    request.setMrn(onlineTxnModel.getTxnId());
                    requestParams.put("base24Request", new Gson().toJson(request));
                    Log.d(" cardRequest = ", String.valueOf(requestParams));
                    MSApi.getInstance().doPayment(this, CARD, requestParams);
                } else if (selectedMop.equals("BQR")) {
                    requestParams.put("transaction_amount", amount);
                    requestParams.put("transaction_type", "upi_qr");
                    Log.d(" upiRequest = ", String.valueOf(requestParams));
                    MSApi.getInstance().doQRCodeTransaction(this, UPI, requestParams);
                } else if (selectedMop.equals("FASTAG")) {
                    requestParams.put("transaction_amount", amount);
                    requestParams.put("vehicle_number", binding.vehicleNo.getText().toString().trim());
                    requestParams.put("mrn", "23423423");
                    requestParams.put("sap_code", sapCode);
                    Log.d("fastagRequest = ", String.valueOf(requestParams));
                    MSApi.getInstance().doFastagTransaction(this, FASTAG, requestParams);
                }else {
                    Log.d("printResult","merchantPrintNo");
                    String authCode = "";
                    String date = cashNotificationDate();
                    String time = cashNotificationTime();
                    onlineTxnModel.setTxnNotificationDate(date);
                    onlineTxnModel.setTxnNotificationTime(time);
                    onlineTxnModel.setTxnChargselipDate(cashChargeslipDate());
                    onlineTxnModel.setTxnChargeslipTime(cashChargeslipTime());
                    onlineTxnModel.setAuthCode(authCode);
                    onlineTxnModel.setAmount(amount);
                    onlineTxnModel.setBleTxnMop("04");
                    onlineTxnModel.setBlePaymentMode("07");
                    Intent intent = new Intent(PaymentActivity.this, SideBarActivity.class);
                    intent.putExtra("redirect", "SalesFragment");
                    intent.putExtra("onlineTxnModel", onlineTxnModel);
                    startActivity(intent);
                    finish();
                }
            }
        } catch (Exception e) {
            Log.d("paymentException", e.toString());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        try {
            super.onActivityResult(requestCode, resultCode, data);
            // Log.d("paymentResponse", data.getStringExtra("response"));
            // Log.d("paymentRequestCoe", String.valueOf(requestCode));
            //Log.d("paymentResultCode", String.valueOf(resultCode));
            if (requestCode == CARD) {
                if (resultCode == RESULT_OK) {
                    String response = data.getStringExtra("response");
                    Log.d("cardPaymentResponse", response);
                    JSONObject jsonObject = new JSONObject(response);
                    JSONObject base24Response = jsonObject.getJSONObject("base24Response");
                    String responseCode = base24Response.getString("responseCode");
                    String authCode = "";
                    if (base24Response.has("authCode")) {
                        authCode = base24Response.getString("authCode");
                    }
                    responseCode = responseCode.replaceAll("\\s", "");
                    responseCode = responseCode.toLowerCase();
                    if (responseCode.equals("transactionsuccess")) {
                        if (authCode.isEmpty()) {
                            Toast.makeText(this, "Auth Code is not available", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(PaymentActivity.this, TxnFailedActivity.class);
                            startActivity(intent);
                        } else {
                            fileWrite(this,todayDate+".txt","Card Payment Response :", String.valueOf(base24Response));
                            String dateTime = base24Response.getString("dateTime");
                            String posEntryMode = base24Response.getString("posEntryMode");
                            onlineTxnModel.setField3("");
                            onlineTxnModel.setAuthCode(authCode);
                            onlineTxnModel.setTxnNotificationDate(cardNotificationDate(dateTime));
                            onlineTxnModel.setTxnNotificationTime(cardNotificationTime(dateTime));
                            onlineTxnModel.setTxnChargselipDate(cardChargeslipDate(dateTime));
                            onlineTxnModel.setTxnChargeslipTime(cardChargeslipTime(dateTime));
                            onlineTxnModel.setCardType(base24Response.optString("cardType", ""));
                            onlineTxnModel.setAid(base24Response.optString("AID", ""));
                            onlineTxnModel.setTsi(base24Response.optString("TSI", ""));
                            onlineTxnModel.setTvr(base24Response.optString("TVR", ""));
                            onlineTxnModel.setCardNo(base24Response.optString("maskedCardNo",""));
                            onlineTxnModel.setTransactionCertificate(base24Response.optString("transactionCertificate", ""));
                            onlineTxnModel.setRrn(base24Response.optString("rrn", ""));
                            onlineTxnModel.setTvr(base24Response.optString("TVR", ""));
                            onlineTxnModel.setCardPaymentVersionNo(base24Response.optString("appVersionNo", ""));
                            onlineTxnModel.setTerminalInvoiceNo(base24Response.optString("terminalInvoiceNo", ""));
                            onlineTxnModel.setBatchNo(base24Response.optString("batchNo", ""));
                            onlineTxnModel.setAuthBank("");
                            onlineTxnModel.setAuthTid("");
                            onlineTxnModel.setAtc("******");
                            onlineTxnModel.setCardFirst(Helper.getCardFirst(base24Response.getString("maskedCardNo")));
                            onlineTxnModel.setCardLast(Helper.getCardLast(base24Response.getString("maskedCardNo")));
                            onlineTxnModel.setCardTxnCustomerName(base24Response.optString("customerName", "").trim());
                            if (posEntryMode.equals("INSERT")) {
                                onlineTxnModel.setPosEntryMode("CHIP");
                            } else {
                                onlineTxnModel.setPosEntryMode(posEntryMode);
                            }

                            if (base24Response.has("cardProduct")) {
                                String cardProductType = base24Response.optString("cardProduct", "");
                                if (cardProductType.isEmpty()) {
                                    String mop = DecimalToHex.create(10);
                                    if(mop.length() == 1){
                                        mop = "0"+mop;
                                    }
                                    onlineTxnModel.setBleTxnMop(mop);
                                    onlineTxnModel.setBlePaymentMode("07");
                                } else {
                                    if (cardProductType.contains("debit")) {
                                        String debitMop = DecimalToHex.create(11);
                                        if(debitMop.length() == 1){
                                            debitMop = "0"+debitMop;
                                        }
                                        onlineTxnModel.setBleTxnMop(debitMop);
                                        onlineTxnModel.setBlePaymentMode("03");
                                    } else if (cardProductType.contains("credit")) {
                                        onlineTxnModel.setBleTxnMop("02");
                                        onlineTxnModel.setBlePaymentMode("02");
                                    } else {
                                        String mop = DecimalToHex.create(10);
                                        if(mop.length() == 1){
                                            mop = "0"+mop;
                                        }
                                        onlineTxnModel.setBleTxnMop(mop);
                                        onlineTxnModel.setBlePaymentMode("07");
                                    }
                                }
                            } else {
                                String mop = DecimalToHex.create(10);
                                if(mop.length() == 1){
                                    mop = "0"+mop;
                                }
                                onlineTxnModel.setBleTxnMop(mop);
                                onlineTxnModel.setBlePaymentMode("07");
                            }
                            redirectToSuccessPage();
                        }
                    } else {
                        Intent intent = new Intent(PaymentActivity.this, TxnFailedActivity.class);
                        startActivity(intent);
                    }
                }
            } else if (requestCode == UPI) {
                if (resultCode == RESULT_OK) {
                    String response = data.getStringExtra("response");
                    Log.d("upiPaymentResponse", response);
                    JSONObject jsonObject = new JSONObject(response);
                    if (jsonObject.has("status")) {
                        String message = jsonObject.getString("message");
                        String originalStatus = jsonObject.getString("status");
                        String upiStatus = jsonObject.getString("status");
                        upiStatus = upiStatus.toLowerCase();
                        if (upiStatus.contains("success")) {
                            String date = jsonObject.getString("txnDate");
                            String time = jsonObject.getString("txnTime");
                            onlineTxnModel.setField3("");
                            onlineTxnModel.setTxnChargselipDate(upiPrintDate(date));
                            onlineTxnModel.setTxnChargeslipTime(upiPrintTime(time));
                            onlineTxnModel.setTxnNotificationDate(upiNotificationDate(date));
                            onlineTxnModel.setTxnNotificationTime(time);
                            if (jsonObject.has("authCode")) {
                                onlineTxnModel.setAuthCode(jsonObject.optString("authCode", ""));
                            } else {
                                onlineTxnModel.setAuthCode("");
                            }
                            onlineTxnModel.setRrn(jsonObject.getString("transaction_id"));
                            onlineTxnModel.setCardType("Transaction");
                            onlineTxnModel.setCardNo(" -  BQR");
                            onlineTxnModel.setBleTxnMop("18");
                            onlineTxnModel.setBlePaymentMode("05");
                            redirectToSuccessPage();
                        } else {
                            if (originalStatus.equals("TRANSACTION_TIMEDOUT")) {
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(PaymentActivity.this, TxnFailedActivity.class);
                                startActivity(intent);
                            } else {
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(PaymentActivity.this, TxnFailedActivity.class);
                                startActivity(intent);
                            }
                        }
                    } else {
                        Intent intent = new Intent(PaymentActivity.this, TxnFailedActivity.class);
                        startActivity(intent);
                    }
                }
            } else if (requestCode == FASTAG) {
                if (resultCode == RESULT_OK) {
                    String response = data.getStringExtra("response");
                    Log.d("fastTagPaymentResponse", response);
                    JSONObject jsonObject = new JSONObject(response);
                    if (jsonObject.has("status")) {
                        String message = jsonObject.getString("message");
                        String fasTagResponseStatus = jsonObject.getString("status");
                        fasTagResponseStatus = fasTagResponseStatus.toLowerCase();
                        if (fasTagResponseStatus.contains("success")) {
                            String mop = DecimalToHex.create(10);
                            if(mop.length() == 1){
                                mop = "0"+mop;
                            }
                            onlineTxnModel.setField3("");
                            onlineTxnModel.setBleTxnMop(mop);
                            onlineTxnModel.setBlePaymentMode("07");

                            String date = cashNotificationDate();
                            String time = cashNotificationTime();
                            onlineTxnModel.setTxnNotificationDate(date);
                            onlineTxnModel.setTxnNotificationTime(time);
                            onlineTxnModel.setTxnChargselipDate(cashChargeslipDate());
                            onlineTxnModel.setTxnChargeslipTime(cashChargeslipTime());
                            if(jsonObject.has("authCode")){
                                onlineTxnModel.setAuthCode(jsonObject.optString("authCode",""));
                            }else{
                                onlineTxnModel.setAuthCode("");
                            }
                            if(jsonObject.has("transaction_id")){
                                onlineTxnModel.setRrn(jsonObject.optString("transaction_id",""));
                            }else{
                                onlineTxnModel.setRrn("");
                            }
                            redirectToSuccessPage();
                        } else {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(PaymentActivity.this, TxnFailedActivity.class);
                            startActivity(intent);
                        }
                    } else {
                        Intent intent = new Intent(PaymentActivity.this, TxnFailedActivity.class);
                        startActivity(intent);
                    }
                }
            } else if (requestCode == REQUEST_CAMERA_CODE && resultCode == RESULT_OK) {
                if (data != null) {
                    String ocrResult = data.getStringExtra("ocrResult");
                    ocrResult = ocrResult.replaceAll("\\s", "");
                    binding.vehicleNo.setText(ocrResult);
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void redirectToSuccessPage() {
        Intent intent = new Intent(PaymentActivity.this, SuccessActivity.class);
        intent.putExtra("onlineTxnModel", onlineTxnModel);
        startActivity(intent);
        finish();
    }

    private void paymentEvent() {
        String paymentName = getSelectedRadioButton();
        String mobileNumber = binding.mobileNo.getText().toString().trim();
        String vehicleNumber = binding.vehicleNo.getText().toString().trim();
        Boolean customerDecline = binding.customerDeclineCheckBox.isChecked();

        /* Start Checkbox Event */
        if (customerDecline) {
            binding.customerDetailsCardView.setVisibility(View.GONE);
        } else {
            binding.customerDetailsCardView.setVisibility(View.VISIBLE);
        }
        /* End Checkbox Event */

        /* Start Radio Event */
        if (!TextUtils.isEmpty(paymentName)) {
            if (paymentName.equals("CASH") || paymentName.equals("CARD") || paymentName.equals("BQR")) {
                binding.msg.setVisibility(View.GONE);
                if (customerDecline) {
                    if (!TextUtils.isEmpty(mobileNumber)) {
                        binding.mobileNo.setText("");
                    }
                    if (!TextUtils.isEmpty(vehicleNumber)) {
                        binding.vehicleNo.setText("");
                    }
                    binding.customerDetailsCardView.setVisibility(View.GONE);
                    binding.customerDeclineCheckBox.setVisibility(View.VISIBLE);
                } else {
                    binding.customerDetailsCardView.setVisibility(View.VISIBLE);
                    binding.customerDeclineCheckBox.setVisibility(View.VISIBLE);
                }
            } else if (paymentName.equals("FASTAG")) {
                binding.customerDeclineCheckBox.setChecked(false);
                binding.msg.setVisibility(View.VISIBLE);
                binding.customerDeclineCheckBox.setVisibility(View.GONE);
                binding.customerDetailsCardView.setVisibility(View.VISIBLE);
                //binding.customerDeclineCheckBox.setChecked(true);
            }else {

            }
        }
        /* End Radio Event */

        /* Start Mobile Vehicle Event */
        if (vehicleNumber.length() >= 9) {
            if (validateVehicleNo(vehicleNumber) || bharatVehicleNoValidation(vehicleNumber) || mobileNumber.length() >= 10) {
                binding.customerDeclineCheckBox.setVisibility(View.GONE);
            } else {
                binding.customerDeclineCheckBox.setVisibility(View.VISIBLE);
            }
        } else if (validateVehicleNo(vehicleNumber) || mobileNumber.length() >= 10) {
            binding.customerDeclineCheckBox.setVisibility(View.GONE);
        } else {
            binding.customerDeclineCheckBox.setVisibility(View.VISIBLE);
        }
        if (!TextUtils.isEmpty(paymentName)) {
            if (paymentName.equals("FASTAG")) {
                binding.customerDeclineCheckBox.setVisibility(View.GONE);
            }
        }
        /* End Mobile Vehicle Event */
    }

    private boolean ValidationErrorShow(String mob, String veh) {
        if (TextUtils.isEmpty(mob) && TextUtils.isEmpty(veh)) {
            MessagesDialog.showDialog(PaymentActivity.this, "Either enter mobile or vehicle number", 0,null, null);

            // Toast.makeText(PaymentActivity.this, "Either enter mobile or vehicle number", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!TextUtils.isEmpty(mob)) {
            if (TextUtils.isEmpty(mob)) {
                binding.mobileNo.setError("Please enter mobile number");
                binding.mobileNo.requestFocus();
                return false;
            }
            if (mob.length() != 10) {
                binding.mobileNo.setError("Mobile Number must be 10 digits");
                binding.mobileNo.requestFocus();
                return false;
            }
            if (mob.startsWith("0")) {
                binding.mobileNo.setError("Mobile Number cannot start with zero");
                binding.mobileNo.requestFocus();
                return false;
            }
            if (mob.equals("1234567890")) {
                binding.mobileNo.setError("Please enter valid mobile number");
                binding.mobileNo.requestFocus();
                return false;
            }
            if (MobileNoValidation.hasSameNumber(mob)) {
                binding.mobileNo.setError("All digits of mobile number cannot be same.");
                binding.mobileNo.requestFocus();
                return false;
            }
        }
        if (!TextUtils.isEmpty(veh)) {
            if (veh.length() >= 9) {
                if (validateVehicleNo(veh) || bharatVehicleNoValidation(veh)) {
                    return true;
                } else {
                    binding.vehicleNo.setError("Vehicle Number is not in proper format. Please enter correct Vehicle Number");
                    binding.vehicleNo.requestFocus();
                    return false;
                }
            } else {
                if (validateVehicleNo(veh)) {
                    return true;
                } else {
                    binding.vehicleNo.setError("Vehicle Number is not in proper format. Please enter correct Vehicle Number");
                    binding.vehicleNo.requestFocus();
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onSuccess(String response) {
        runOnUiThread(() -> {
            // Update UI elements with the response data
            Toast.makeText(PaymentActivity.this, "Request failed: " + response, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onFailure(IOException e) {
        runOnUiThread(() -> {
            // Show error message
            MessagesDialog.showDialog(PaymentActivity.this, "Request failed: " + e.getMessage(), 0,null, null);

           // Toast.makeText(PaymentActivity.this, "Request failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    public void cameraOpen() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            askCameraPermission();
        } else {
            Intent intent = new Intent(this, CustomCameraActivity.class);
            startActivityForResult(intent, REQUEST_CAMERA_CODE);
        }
    }

    public void askCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            cameraOpen();
        }
    }

    public String createTxnId(){
        //bleNotification(Helper.txnListPostionSelected);
        String txnId = "";
        Object myobj = txnArrayList.get(txnListPostionSelected);
        Gson gson = new Gson();
        String json = gson.toJson(myobj);
        try {
            JSONObject jsonObject = new JSONObject(json);
            String date = HexToDecimal.convert(jsonObject.getString("Day"));
            String month = HexToDecimal.convert(jsonObject.getString("Month"));
            String year = HexToDecimal.convert(jsonObject.getString("Year"));
            String hour = HexToDecimal.convert(jsonObject.getString("Hour"));
            String min = HexToDecimal.convert(jsonObject.getString("Minute"));
            String second = HexToDecimal.convert(jsonObject.getString("TxnStartSecond"));
            String uniqueId = HexToDecimal.convert(jsonObject.getString("UniqueID"));

            Log.d("uniqueIdHex",uniqueId);
            Log.d("uniqueIdWithoutHex",jsonObject.getString("UniqueID"));

            if (date.length() == 1) {
                date = "0" + date;
            }
            if (month.length() == 1) {
                month = "0" + month;
            }
            if (year.length() == 2) {
                year = "20" + year;
            }
            if (hour.length() == 1) {
                hour = "0" + hour;
            }
            if (min.length() == 1) {
                min = "0" + min;
            }
            if (second.length() == 1) {
                second = "0" + second;
            }

            Log.d("uniqueId", uniqueId);
            chargeslipTxnId = CreateTransactionId.chargeslipTxnId(year, month, date, uniqueId);
            return chargeslipTxnId;
        }catch (Exception e){

        }
        return "";
    }


}