package com.ims.bpcluat.ufill.utr;

import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.closeKeyboard;
import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.reportDateFormatInUtr;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.Helper.username;
import static com.ims.bpcluat.Helper.version;
import static com.ims.bpcluat.helper.ApiHelper.uFillEndpoint;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.Toast;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.databinding.FragmentUtrBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.model.UfillModel;
import com.ims.bpcluat.ufill.ufil1.UfillOneFragment;
import com.ims.bpcluat.ufill.ufil1.UfillOnePumpFragment;
import com.ims.bpcluat.validation.DecimalDigitsInputFilter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Calendar;

public class UtrFragment extends Fragment implements ApiHelper.NetworkingApiCallBack {
    FragmentUtrBinding binding;
    Context context;
    ApiHelper api;
    ProgressDialog progress;
    DatePickerDialog picker;
    UfillModel ufillModel = new UfillModel();
    String utrNo = "";

    public UtrFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        api = new ApiHelper();
        context = getActivity();
        binding = FragmentUtrBinding.inflate(inflater, container, false);
        binding.payAmount.setFilters(new InputFilter[] { new DecimalDigitsInputFilter(2,10000000) });
        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new UfillOneFragment());
            }
        });
        binding.utrFetchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeKeyboard(getActivity());
                validateFields();
            }
        });

        binding.voucherDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar calendar = Calendar.getInstance();
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                int month = calendar.get(Calendar.MONTH);
                int year = calendar.get(Calendar.YEAR);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Log.d("dateCheck","if block");
                    picker = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                            month = month + 1;
                            String monthString = String.valueOf(month);
                            if (monthString.length() == 1) {
                                monthString = "0" + monthString;
                            }
                            String dayString = String.valueOf(dayOfMonth);
                            if (dayString.length() == 1) {
                                dayString = "0" + dayString;
                            }
                            binding.voucherDate.setText(new StringBuilder().append(dayString).append("-").append(monthString).append("-").append(year).append(" "));
                        }
                    }, year, month, day);
                    picker.getDatePicker().setMaxDate(calendar.getTimeInMillis()); // Future all date disable
                    picker.show();
                } else {
                    Log.d("dateCheck","else block");
                    DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                            month = month + 1;
                            String monthString = String.valueOf(month);
                            if (monthString.length() == 1) {
                                monthString = "0" + monthString;
                            }
                            String dayString = String.valueOf(dayOfMonth);
                            if (dayString.length() == 1) {
                                dayString = "0" + dayString;
                            }
                            binding.voucherDate.setText(new StringBuilder().append(dayString).append("-").append(monthString).append("-").append(year).append(" "));
                        }
                    };

                    Calendar calendar1 = Calendar.getInstance();
                    new DatePickerDialog(getActivity(), listener, calendar1.get(Calendar.YEAR), calendar1.get(Calendar.MONTH), calendar1.get(Calendar.DAY_OF_MONTH)).show();
                }
            }
        });

        return binding.getRoot();
    }

    private void validateFields() {
        String utrNumber = binding.utrNumber.getText().toString().trim();
        String voucherDate = binding.voucherDate.getText().toString().trim();
        String payAmount = binding.payAmount.getText().toString().trim();

        if (TextUtils.isEmpty(utrNumber)) {
            binding.utrNumber.setError("Please enter UTR number");
            binding.utrNumber.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(voucherDate)) {
            binding.voucherDate.setError("Please enter voucher date");
            binding.voucherDate.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(payAmount) || Double.parseDouble(payAmount) <= 0) {
            binding.payAmount.setError(TextUtils.isEmpty(payAmount) ? "Please enter pay amount" : "amount must be greater than 0");
            binding.payAmount.requestFocus();
            return;
        }

        String reportDate = reportDateFormatInUtr(voucherDate);
        fetchUtrDetails(utrNumber, reportDate, payAmount);
    }

    private void fetchUtrDetails(String utrNum, String reportDate, String amount) {
        utrNo = utrNum;
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while fetching details...");
        progress.setCancelable(false);
        progress.show();
        String txnId = Helper.createTxnIdForOfflineTxn();
        String dateTime = requestDate() + requestTime();
        ufillModel.setTxnId(txnId);
        ufillModel.setDateTime(dateTime);

        String url = uFillEndpoint;
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("channel", channelName);
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("userName", username);
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnType", "AUS");
            jsonObject.put("rrn", utrNum);
            jsonObject.put("amt", amount);
            jsonObject.put("reportDate", reportDate);
            jsonObject.put("dateTime", dateTime);
            jsonObject.put("txnId", txnId);
            jsonObject.put("hwSrNo",Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", version);

            Log.d("utrRequest", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(UtrFragment.this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void apiResult(String res, String apiName) {
        if (apiName.equals("ufill")) {
            try {
                if (res.equals("Server Time Out")) {
                    progress.dismiss();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                        }
                    });
                } else {
                    Log.d("utrResponse = ", res);
                    fileWrite(context, todayDate + ".txt", "UtrFragmentResponse :", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");
                    if (respCode.equals("200")) {
                        progress.dismiss();
                        JSONArray outputArray = payLoad.getJSONArray("output");
                        JSONObject outputObject = outputArray.getJSONObject(0);
                        JSONArray vouchersArray = outputObject.getJSONArray("vouchers");
                        JSONObject vouchersObject = vouchersArray.getJSONObject(0);

                        String amount = vouchersObject.getString("amount");
                        String qrCodeUrl = vouchersObject.getString("qrCodeUrl");
                        String voucherCode = vouchersObject.getString("voucherCode");
                        String mobileNumber = vouchersObject.getString("mobileNumber");
                        String voucherStatus = vouchersObject.getString("voucherStatus");

                        ufillModel.setTxnType("UFILLUTR");
                        ufillModel.setUtrNo(utrNo);
                        ufillModel.setVoucherAmt(amount);
                        ufillModel.setQrCodeUrl(qrCodeUrl);
                        ufillModel.setVoucherNo(voucherCode);
                        ufillModel.setMobileNumber(mobileNumber);
                        ufillModel.setVoucherStatus(voucherStatus);

                        Bundle bundle = new Bundle();
                        bundle.putParcelable("ufillModel", ufillModel);
                        ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle,new UtrDetailsFragment());
                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                MessagesDialog.showDialog(context, respDesc, 0,null, null);
                                //Toast.makeText(getActivity(), respDesc + " - " + respCode, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "utrResponse", e.toString());
                getActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_SHORT).show();
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    MessagesDialog.showDialog(context, e.toString(), 0,null, null);

                });
               // throw new RuntimeException(e);
            }
        }
    }

    public void onResume() {
        super.onResume();
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
    }


    @Override
    public void onStop() {
        super.onStop();
        ((AppCompatActivity) getActivity()).getSupportActionBar().show();
    }
}