package com.ims.bpcluat.cng;

import static com.ims.bpcluat.Helper.cardChargeslipDate;
import static com.ims.bpcluat.Helper.cardChargeslipTime;
import static com.ims.bpcluat.Helper.cardNotificationDate;
import static com.ims.bpcluat.Helper.cardNotificationTime;
import static com.ims.bpcluat.Helper.cashChargeslipDate;
import static com.ims.bpcluat.Helper.cashChargeslipTime;
import static com.ims.bpcluat.Helper.cashNotificationDate;
import static com.ims.bpcluat.Helper.cashNotificationTime;
import static com.ims.bpcluat.Helper.createCashRrn;
import static com.ims.bpcluat.Helper.padWithZeroes;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.upiNotificationDate;
import static com.ims.bpcluat.Helper.upiPrintDate;
import static com.ims.bpcluat.Helper.upiPrintTime;
import static com.ims.bpcluat.validation.VehicleNoValidation.bharatVehicleNoValidation;
import static com.ims.bpcluat.validation.VehicleNoValidation.validateVehicleNo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Toast;

import com.firstdata.merchantservicessdk.MSApi;
import com.google.gson.Gson;
import com.ims.bpcluat.CustomCameraActivity;
import com.ims.bpcluat.Helper;
import com.ims.bpcluat.PaymentActivity;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.TxnFailedActivity;
import com.ims.bpcluat.adapter.SpinnerAdapter;
import com.ims.bpcluat.databinding.ActivityCngPaymentBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.model.CngModel;
import com.ims.bpcluat.model.NfrModel;
import com.ims.bpcluat.model.OnlineTxnModel;
import com.ims.bpcluat.nfr.NfrPaymentActivity;
import com.ims.bpcluat.ufill.void_transaction.VoidReceipt;
import com.ims.bpcluat.validation.MobileNoValidation;
import com.ims.bpcluat.validation.VehicleNoValidation;
import com.pax.fdms.opensdk.base24.Base24Constant;
import com.pax.fdms.opensdk.base24.Base24Request;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CngPaymentActivity extends AppCompatActivity {

    ActivityCngPaymentBinding binding;
    private static final int REQUEST_CAMERA_CODE = 100;
    RadioButton cashRadioButton, cardRadioButton, upiRadioButton, saleRadioButton;
    private static final int CARD = 1;
    private static final int UPI = 2;
    private static final int FASTAG = 3;
    private CngModel cngModel;
    private OnlineTxnModel onlineTxnModel;
    private NfrModel nfrModel;
    String qty, product, amount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCngPaymentBinding.inflate(getLayoutInflater());
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

        cngModel = getIntent().getParcelableExtra("cngModel");
        if (cngModel != null) {
            qty = cngModel.getQty();
            product = cngModel.getProductName();
            amount = cngModel.getTotalAmt();
            binding.qty.setText(qty);
            binding.product.setText(product);
            binding.amount.setText(amount);
        }
        Intent intent = getIntent();
        String Insertcard = intent.getStringExtra("Insertcard");

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CngPaymentActivity.this, SideBarActivity.class);
                if (Objects.equals(Insertcard, "Insertcard")) {
                    intent.putExtra("redirect", "CngFragment");
                    startActivity(intent);
                    finish();
                }
                else{
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
                    MessagesDialog.showDialog(CngPaymentActivity.this, "Please select one payment option", 0,null, null);

                    //Toast.makeText(CngPaymentActivity.this, "Please select one payment option", Toast.LENGTH_SHORT).show();
                }
            }
        });

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
        } else if (saleRadioButton.isChecked()) {
            return "SALES";
        }
        return null; // If none are checked
    }

    private void payment(String selectedMop) {
        try {
            String vType = binding.vehicleTypeSpinner.getSelectedItem().toString();
            if (vType.equals("Select Vehicle Type")) {
                //cngModel.setVehicleType(binding.vehicleTypeSpinner.getSelectedItem().toString());
                binding.spinnerError.setText("Please select vehicle type");
                binding.spinnerError.setVisibility(View.VISIBLE);
            } else {
                binding.spinnerError.setVisibility(View.GONE);
                cngModel.setVehicleType(vType);

                cngModel.setMobileNumber(binding.mobileNo.getText().toString().trim());
                cngModel.setVehicleNumber(binding.vehicleNo.getText().toString().trim());
                cngModel.setTxnType(selectedMop);
                cngModel.setTxnId(Helper.createTxnIdForOfflineTxn());

                JSONObject requestParams = new JSONObject();
                if (selectedMop.equals("CASH")) {
                    String date = cashNotificationDate();
                    String time = cashNotificationTime();
                    cngModel.setTxnNotificationDate(date);
                    cngModel.setTxnNotificationTime(time);
                    cngModel.setTxnChargselipDate(cashChargeslipDate());
                    cngModel.setTxnChargeslipTime(cashChargeslipTime());
                    cngModel.setRrn(createCashRrn(date, time, amount));
                    cngModel.setAuthCode("");
                    cngModel.setBatchNo("");
                    cngModel.setField3("");
                    redirectToSuccessPage();
                } else if (selectedMop.equals("CARD")) {
                    Base24Request request = new Base24Request();
                    request.setFunctionCode(Base24Constant.TYPE_SALE); //sale
                    request.setTotalTxnAmount(amount);
                    request.setSuppressPrintChargeSlips("y");
                    request.setMrn(cngModel.getTxnId());
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
                } else {
                    Log.d("printResult", "merchantPrintNo");
                    String authCode = "";
                    String date = cashNotificationDate();
                    String time = cashNotificationTime();
                    cngModel.setTxnNotificationDate(date);
                    cngModel.setTxnNotificationTime(time);
                    cngModel.setTxnChargselipDate(cashChargeslipDate());
                    cngModel.setTxnChargeslipTime(cashChargeslipTime());
                    cngModel.setAuthCode(authCode);
                    Intent intent = new Intent(CngPaymentActivity.this, SideBarActivity.class);
                    intent.putExtra("redirect", "SalesFragment");
                    intent.putExtra("cngModel", cngModel);
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
//            Log.d("paymentResponse", data.getStringExtra("response"));
//            Log.d("paymentRequestCoe", String.valueOf(requestCode));
//            Log.d("paymentResultCode", String.valueOf(resultCode));
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
                            Intent intent = new Intent(CngPaymentActivity.this, CngTxnFailedActivity.class);
                            startActivity(intent);
                        } else {
                            String dateTime = base24Response.getString("dateTime");
                            String posEntryMode = base24Response.getString("posEntryMode");
                            cngModel.setField3("");
                            cngModel.setAuthCode(authCode);
                            cngModel.setTxnNotificationDate(cardNotificationDate(dateTime));
                            cngModel.setTxnNotificationTime(cardNotificationTime(dateTime));
                            cngModel.setTxnChargselipDate(cardChargeslipDate(dateTime));
                            cngModel.setTxnChargeslipTime(cardChargeslipTime(dateTime));
                            cngModel.setCardType(base24Response.optString("cardType", ""));
                            cngModel.setAid(base24Response.optString("AID", ""));
                            cngModel.setTsi(base24Response.optString("TSI", ""));
                            cngModel.setTvr(base24Response.optString("TVR", ""));
                            cngModel.setCardNo(base24Response.optString("maskedCardNo",""));
                            cngModel.setTransactionCertificate(base24Response.optString("transactionCertificate", ""));
                            cngModel.setRrn(base24Response.optString("rrn", ""));
                            cngModel.setTvr(base24Response.optString("TVR", ""));
                            cngModel.setCardPaymentVersionNo(base24Response.optString("appVersionNo", ""));
                            cngModel.setTerminalInvoiceNo(base24Response.optString("terminalInvoiceNo", ""));
                            cngModel.setBatchNo(base24Response.optString("batchNo", ""));
                            cngModel.setAuthBank("");
                            cngModel.setAuthTid("");
                            cngModel.setAtc("******");
                            cngModel.setCardFirst(Helper.getCardFirst(base24Response.getString("maskedCardNo")));
                            cngModel.setCardLast(Helper.getCardLast(base24Response.getString("maskedCardNo")));
                            cngModel.setCardTxnCustomerName(base24Response.optString("customerName", "").trim());
                            if (posEntryMode.equals("INSERT")) {
                                cngModel.setPosEntryMode("CHIP");
                            } else {
                                cngModel.setPosEntryMode(posEntryMode);
                            }
                            redirectToSuccessPage();
                        }
                    } else {
                        Intent intent = new Intent(CngPaymentActivity.this, CngTxnFailedActivity.class);
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
                            cngModel.setField3("");
                            cngModel.setTxnChargselipDate(upiPrintDate(date));
                            cngModel.setTxnChargeslipTime(upiPrintTime(time));
                            cngModel.setTxnNotificationDate(upiNotificationDate(date));
                            cngModel.setTxnNotificationTime(time);
                            if (jsonObject.has("authCode")) {
                                cngModel.setAuthCode(jsonObject.optString("authCode", ""));
                            } else {
                                cngModel.setAuthCode("");
                            }
                            cngModel.setRrn(jsonObject.getString("transaction_id"));
                            cngModel.setCardType("Transaction");
                            cngModel.setCardNo(" -  BQR");
                            redirectToSuccessPage();
                        } else {
                            if (originalStatus.equals("TRANSACTION_TIMEDOUT")) {
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(CngPaymentActivity.this, CngTxnFailedActivity.class);
                                intent.putExtra("upiFailed", "TRANSACTION_TIMEDOUT");
                                intent.putExtra("transaction_amount", amount);
                                intent.putExtra("mobileNum", cngModel.getMobileNumber());
                                intent.putExtra("txnid", cngModel.getTxnId());

                                startActivity(intent);
                            } else {
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(CngPaymentActivity.this, CngTxnFailedActivity.class);
                                startActivity(intent);
                            }
                        }
                    } else {
                        Intent intent = new Intent(CngPaymentActivity.this, CngTxnFailedActivity.class);
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
                            cngModel.setField3("");
                            String date = cashNotificationDate();
                            String time = cashNotificationTime();
                            cngModel.setTxnNotificationDate(date);
                            cngModel.setTxnNotificationTime(time);
                            cngModel.setTxnChargselipDate(cashChargeslipDate());
                            cngModel.setTxnChargeslipTime(cashChargeslipTime());
                            if (jsonObject.has("authCode")) {
                                cngModel.setAuthCode(jsonObject.optString("authCode", ""));
                            } else {
                                cngModel.setAuthCode("");
                            }
                            if (jsonObject.has("transaction_id")) {
                                cngModel.setRrn(jsonObject.optString("transaction_id", ""));
                            } else {
                                cngModel.setRrn("");
                            }
                            redirectToSuccessPage();
                        } else {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(CngPaymentActivity.this, CngTxnFailedActivity.class);
                            startActivity(intent);
                        }
                    } else {
                        Intent intent = new Intent(CngPaymentActivity.this, CngTxnFailedActivity.class);
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
        Intent intent = new Intent(CngPaymentActivity.this, CngSuccessActivity.class);
        intent.putExtra("cngModel", cngModel);
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
            MessagesDialog.showDialog(CngPaymentActivity.this, "Either enter mobile or vehicle number", 0,null, null);

            // Toast.makeText(CngPaymentActivity.this, "Either enter mobile or vehicle number", Toast.LENGTH_SHORT).show();
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

    public void cameraOpen() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            askCameraPermission();
        } else {
            Intent intent = new Intent(this, CustomCameraActivity.class);
            startActivityForResult(intent, REQUEST_CAMERA_CODE);
        }
    }

    public void askCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA
        }, REQUEST_CAMERA_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            cameraOpen();
        }
    }



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("qty", qty);
        outState.putString("amount", amount);
        outState.putString("product", product);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            qty = savedInstanceState.getString("qty");
            amount = savedInstanceState.getString("amount");
            product = savedInstanceState.getString("product");
            binding.qty.setText(qty);
            binding.amount.setText(amount);
            binding.product.setText(product);
        }
    }

}