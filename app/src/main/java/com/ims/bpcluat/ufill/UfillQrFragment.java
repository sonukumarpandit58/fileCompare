package com.ims.bpcluat.ufill;

import static android.app.Activity.RESULT_OK;

import static com.ims.bpcluat.Helper.upiNotificationDate;
import static com.ims.bpcluat.Helper.upiPrintDate;
import static com.ims.bpcluat.Helper.upiPrintTime;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.firstdata.merchantservicessdk.MSApi;
import com.ims.bpcluat.Helper;
import com.ims.bpcluat.PaymentActivity;
import com.ims.bpcluat.TxnFailedActivity;
import com.ims.bpcluat.databinding.FragmentUfillQrBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.model.UfillModel;
import com.ims.bpcluat.validation.DecimalDigitsWithoutMaxValue;

import org.json.JSONObject;

public class UfillQrFragment extends Fragment  {

    FragmentUfillQrBinding binding;
    String pumpNo, amount;
    private static final int UPI = 2;
    UfillModel ufillModel = new UfillModel();
    public UfillQrFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUfillQrBinding.inflate(inflater, container, false);
        binding.amount.setFilters(new InputFilter[] { new DecimalDigitsWithoutMaxValue() });
        if (Helper.pumpArray.size() == 0) {
            binding.duSpinner.setVisibility(View.GONE);
            binding.manuallyPumpNo.setVisibility(View.VISIBLE);
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, Helper.pumpArray);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.duSpinner.setAdapter(adapter);

            binding.duSpinner.setVisibility(View.VISIBLE);
            binding.manuallyPumpNo.setVisibility(View.GONE);
        }

        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                amount = binding.amount.getText().toString().trim();
                if (TextUtils.isEmpty(amount)) {
                    binding.amount.setError("Please enter the amount");
                    binding.amount.requestFocus();
                    return;
                }
                if (binding.duSpinner.getVisibility() == View.VISIBLE) {
                    int selectedItemPosition = binding.duSpinner.getSelectedItemPosition();
                    if (selectedItemPosition == AdapterView.INVALID_POSITION) {
                        // No item is selected
                        Log.d("SpinnerCheck", "No item selected");
                        Helper helper = new Helper();
                        helper.showToastMessage(getActivity(), "Please fetch pump first");
                    } else {
                        Log.d("SpinnerCheck", "Item selected: " + binding.duSpinner.getSelectedItem().toString());
                        String selectedValue = binding.duSpinner.getSelectedItem().toString();
                        int index = selectedValue.indexOf('-');
                        pumpNo = selectedValue.substring(index + 1);
                        if (pumpNo.length() == 1) {
                            pumpNo = "0" + pumpNo;
                        }
                        upiPayment();
                    }
                }
                if (binding.manuallyPumpNo.getVisibility() == View.VISIBLE) {
                    pumpNo = binding.manuallyPumpNo.getText().toString().trim();
                    if (TextUtils.isEmpty(pumpNo)) {
                        binding.manuallyPumpNo.setError("please enter pump no");
                        binding.manuallyPumpNo.requestFocus();
                    } else {
                        Helper.closeKeyboard(getActivity());
                        if (pumpNo.length() == 1) {
                            pumpNo = "0" + pumpNo;
                        }
                        Helper.closeKeyboard(getActivity());
                        upiPayment();
                    }
                }
            }
        });
        return binding.getRoot();
    }

    private void upiPayment() {
        try{
            JSONObject requestParams = new JSONObject();
            requestParams.put("transaction_amount", amount);
            requestParams.put("transaction_type", "upi_qr");
            Log.d(" upiRequest = ", String.valueOf(requestParams));
            MSApi.getInstance().doQRCodeTransaction(getActivity(), UPI, requestParams);
        }catch (Exception e){
            Log.d("UpiPaymentException",e.toString());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d("onActivityResult","127 lines");
        try {
        super.onActivityResult(requestCode, resultCode, data);
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
//                    onlineTxnModel.setTxnChargselipDate(upiPrintDate(date));
//                    onlineTxnModel.setTxnChargeslipTime(upiPrintTime(time));
//                    onlineTxnModel.setTxnNotificationDate(upiNotificationDate(date));
//                    onlineTxnModel.setTxnNotificationTime(time);
                    if (jsonObject.has("authCode")) {
                      //  onlineTxnModel.setAuthCode(jsonObject.optString("authCode", ""));
                    } else {
                      //  onlineTxnModel.setAuthCode("");
                    }
//                    onlineTxnModel.setRrn(jsonObject.getString("transaction_id"));
//                    onlineTxnModel.setCardType("Transaction");
//                    onlineTxnModel.setCardNo(" -  BQR");
//                    onlineTxnModel.setBleTxnMop("18");
//                    onlineTxnModel.setBlePaymentMode("05");
                 //   redirectToSuccessPage();

                    ufillModel.setPumpNo(pumpNo);
                    ufillModel.setVoucherAmt(amount);
                    Intent intent = new Intent(getActivity(),VoucherRedeemActivity.class);
                    intent.putExtra("ufillModel",ufillModel);
                    startActivity(intent);
                } else {
                    if (originalStatus.equals("TRANSACTION_TIMEDOUT")) {
                        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                MessagesDialog.showDialog(requireContext(), "Txn Failed", 0,null, null);

                //Toast.makeText(getActivity(), "Txn Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }catch (Exception e){

        }
    }

}