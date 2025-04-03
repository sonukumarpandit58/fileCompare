package com.ims.bpcluat.alp.alpOperations.cardManagement.enroll_additional;

import static com.ims.bpcluat.Helper.appVersion;
import static com.ims.bpcluat.Helper.getCurrentDateTime;
import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.username;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.databinding.FragmentEnrolSubmitCardBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.receiver.ConnectivityReceiver;
import com.ims.bpcluat.utils.SharedPrefHelper;
import com.ims.bpcluat.validation.MobileNoValidation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EnrolSubmitCardFragment extends Fragment implements ApiHelper.NetworkingApiCallBack {
    FragmentEnrolSubmitCardBinding binding;
    ApiHelper api;
    ProgressDialog progress;
    Context context;
    String mobileNumber = "", faNumber = "", cardnumber = "";
    String txnId = "", dateTime = "";
    String id = "";
    private boolean isFaRequired;
    String shredValue;
    SharedPrefHelper sharedPrefHelper;
    String checkApiCall;


    private ConnectivityReceiver connectivityReceiver = new ConnectivityReceiver();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEnrolSubmitCardBinding.inflate(getLayoutInflater());
        context = getActivity();
        api = new ApiHelper();
        connectivityReceiver = new ConnectivityReceiver();
        sharedPrefHelper = new SharedPrefHelper(requireContext());

        shredValue = sharedPrefHelper.getString("cardManagementBtn", "");

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new EnrolAddCardManagerPinFrag());

            }
        });


        if (shredValue.equals("aditionalBtn")) {
            binding.faNumber.setVisibility(View.VISIBLE);
            binding.faField.setVisibility(View.GONE);
            binding.mobilenum.setVisibility(View.GONE);
        }

        binding.submitMobBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (shredValue.equals("newenrolBtn")) {
                    enrollValidateFields();
                } else if (shredValue.equals("aditionalBtn")) {
                    addCardValidateFields(); //update
                } else {
                    Log.d("TAG", "codrikaz");
                }
            }
        });



        binding.switchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Store the value of the switch state in the class-level variable
                isFaRequired = isChecked;
            }
        });

        hideKeyboard();

        return binding.getRoot();

    }

    private void enrollValidateFields() {
        mobileNumber = binding.mobilenum.getText().toString().trim();
        cardnumber = binding.numberofcard.getText().toString().trim();
        if (TextUtils.isEmpty(mobileNumber)) {
            binding.mobilenum.setError("Please enter mobile number");
            binding.mobilenum.requestFocus();

            return;
        }
        if (TextUtils.isEmpty(mobileNumber)) {
            binding.mobilenum.setError("Please enter mobile number");
            binding.mobilenum.requestFocus();
            return;
        }
        if (mobileNumber.length() != 10) {
            binding.mobilenum.setError("Mobile Number must be 10 digits");
            binding.mobilenum.requestFocus();
            return;
        }
        if (mobileNumber.startsWith("0")) {
            binding.mobilenum.setError("Mobile Number cannot start with zero");
            binding.mobilenum.requestFocus();
            return;
        }
        if (mobileNumber.equals("1234567890")) {
            binding.mobilenum.setError("Please enter valid mobile number");
            binding.mobilenum.requestFocus();
            return;
        }
        if (MobileNoValidation.hasSameNumber(mobileNumber)) {
            binding.mobilenum.setError("All digits of mobile number cannot be same.");
            binding.mobilenum.requestFocus();
            return;
        }
        if (cardnumber.isEmpty()) {
            binding.numberofcard.setError("Please enter number of card");
            binding.numberofcard.requestFocus();

        }
        newEnrolmentApi();

    }

    private void addCardValidateFields() {
        faNumber = binding.faNumber.getText().toString().trim();
        cardnumber = binding.numberofcard.getText().toString().trim();
        if (TextUtils.isEmpty(faNumber)) {
            binding.faNumber.setError("Please enter mobile number");
            binding.faNumber.requestFocus();
            return;
        }

        if (faNumber.length() != 12) {
            binding.faNumber.setError("FA Number must be 12 digits");
            binding.faNumber.requestFocus();
            return;
        }
        if (faNumber.startsWith("0")) {
            binding.faNumber.setError("FA Number cannot start with zero");
            binding.faNumber.requestFocus();
            return;
        }
        if (faNumber.equals("1234567890")) {
            binding.faNumber.setError("Please enter valid FA number");
            binding.faNumber.requestFocus();
            return;
        }
        if (MobileNoValidation.hasSameNumber(faNumber)) {
            binding.faNumber.setError("All digits of FA number cannot be same.");
            binding.faNumber.requestFocus();
            return;
        }
        if (cardnumber.isEmpty()) {
            binding.numberofcard.setError("Please enter number of card");
            binding.numberofcard.requestFocus();
        }
        additionalCardApi();
    }

    private void newEnrolmentApi() {
        checkApiCall = "newEnrolmentApi";
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);
        progress.show();

        String url = "alpreq";
        String reqDate = requestDate();
        String reqTime = requestTime();
//        txnId = Helper.createTxnIdForOfflineTxn();
        txnId = tid + getCurrentDateTime();
        dateTime = reqDate + reqTime;

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("userName", username);
            jsonObject.put("channel", "BPCL");
            jsonObject.put("tid", tid);
            jsonObject.put("mid", mid);
            jsonObject.put("source", "Mobile");
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("dateTime", dateTime);
            jsonObject.put("txnType", "AEA");
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnId", txnId);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("reportType", "enroll");
            jsonObject.put("walletType", "SmartFleet");
            jsonObject.put("mobNo", mobileNumber);
            jsonObject.put("totalTxnCnt", cardnumber);
            jsonObject.put("tagType", isFaRequired);
            jsonObject.put("payInstrument", "");
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "0");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);

            Log.d("getNewEnrolRequest", String.valueOf(jsonObject));

            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);

        } catch (JSONException e) {
            Log.d("respCode", "respCode");
            progress.dismiss();
        }
    }

    private void additionalCardApi() {
        checkApiCall = "additionalCardApi";
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);
        progress.show();

        String url = "alpreq";
        String reqDate = requestDate();
        String reqTime = requestTime();
        txnId = Helper.createTxnIdForOfflineTxn();
        dateTime = reqDate + reqTime;

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("userName", username);
            jsonObject.put("channel", "BPCL");
            jsonObject.put("tid", tid);
            jsonObject.put("mid", mid);
            jsonObject.put("source", "Mobile");
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("dateTime", dateTime);
            jsonObject.put("txnType", "AEA");
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnId", txnId);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("reportType", "addCard");
            jsonObject.put("walletType", "SmartFleet");
//            jsonObject.put("mobNo", mobilenumber);
            jsonObject.put("totalTxnCnt", cardnumber);
//            jsonObject.put("tagType", isFaRequired);
            jsonObject.put("payInstrument", faNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "0");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);

            Log.d("getCardRequest", String.valueOf(jsonObject));

            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);

        } catch (JSONException e) {
            Log.d("respCode", "respCode");
            progress.dismiss();
        }
    }


    public void apiResult(String res, String apiName) {
        if (checkApiCall.equals("newEnrolmentApi")) {
            progress.dismiss();
            if (res.equals("Server Time Out")) {
                Log.d("timeout_problem", res);
                progress.dismiss();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), "Server Time Out", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                try {
                    Log.d("enrolResponse1 = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");

                    String respCode = payLoad.getString("respCode");
                    String respDesc = payLoad.getString("respDesc");

                    if (respCode.equals("200")) {
                        JSONArray outputArray = payLoad.getJSONArray("output");
                        JSONObject outputArrayJSONObject = outputArray.getJSONObject(0);

                        String amountPayable = outputArrayJSONObject.getString("amountPayable");
                        String clientTxnId = outputArrayJSONObject.getString("clientTxnId");
                        String txnNumber = outputArrayJSONObject.getString("txnNumber");
                        id = payLoad.getString("id");

                        progress.dismiss();
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Bundle bundle = new Bundle();
                                bundle.putString("dateTime", dateTime);
                                bundle.putString("txnId", txnId);

                                bundle.putString("txnNumber", txnNumber);
                                bundle.putString("clientTxnId", clientTxnId);
                                bundle.putString("amountPayable", amountPayable);
                                bundle.putString("mobileNumber", mobileNumber);

                                Log.e("dateTime", dateTime);
                                Log.e("clientTxnId", clientTxnId);
                                Log.e("txnNumber", txnNumber);
                                Log.e("txnId", txnId);
                                Log.e("amountPayable", amountPayable);

                                Intent intent = new Intent(context, EnrollCardPayActivity.class);
                                intent.putExtra("bundle",bundle);
                               startActivity(intent);

//                                EnrolPaymentFragment enrolPaymentFragment = new EnrolPaymentFragment();
//                                enrolPaymentFragment.setArguments(bundle);
//                                ((SideBarActivity) requireActivity()).loadFragement(enrolPaymentFragment);
                            }
                        });
                    } else {
                        progress.dismiss();

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("cardrespcode", respCode);
                                MessagesDialog.showDialog(context, respDesc, 0,null, null);;
                            }
                        });
                    }

                } catch (Exception e) {
                    Log.e("APIError", "Error parsing JSON response", e);
                    progress.dismiss();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        } else if(checkApiCall.equals("additionalCardApi")) {
            if (checkApiCall.equals("additionalCardApi")) {
                if (res.equals("Server Time Out")) {
                    Log.d("timeout_problem", res);
                    progress.dismiss();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), "Server Time Out", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    try {
                        Log.d("enrolResponse2 = ", res);
                        JSONObject jsonObject = new JSONObject(res);
                        JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");

                        String respCode = payLoad.getString("respCode");
                        String respDesc = payLoad.getString("respDesc");

                        if (respCode.equals("200")) {
                            JSONArray outputArray = payLoad.getJSONArray("output");
                            JSONObject outputArrayJSONObject = outputArray.getJSONObject(0);

                            String amountPayable = outputArrayJSONObject.getString("amountPayable");
                            String clientTxnId = outputArrayJSONObject.getString("clientTxnId");
                            String txnNumber = outputArrayJSONObject.getString("txnNumber");
                            id = payLoad.getString("id");

                            progress.dismiss();
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Bundle bundle = new Bundle();
                                    bundle.putString("dateTime", dateTime);
                                    bundle.putString("txnId", txnId);

                                    bundle.putString("txnNumber", txnNumber);
                                    bundle.putString("clientTxnId", clientTxnId);
                                    bundle.putString("amountPayable", amountPayable);

                                    Log.e("dateTime", dateTime);
                                    Log.e("clientTxnId", clientTxnId);
                                    Log.e("txnNumber", txnNumber);
                                    Log.e("txnId", txnId);
                                    Log.e("amountPayable", amountPayable);

                                    Intent intent = new Intent(getContext(),EnrollCardPayActivity.class);
                                    intent.putExtra("bundle",bundle);
                                    startActivity(intent);
//
//                                    EnrolPaymentFragment enrolPaymentFragment = new EnrolPaymentFragment();
//                                    enrolPaymentFragment.setArguments(bundle);
//                                    ((SideBarActivity) requireActivity()).loadFragement(enrolPaymentFragment);
                                }
                            });
                        } else {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d("enrolrespcode", respCode);
                                    progress.dismiss();
                                    MessagesDialog.showDialog(context, respDesc, 0,null, null);;
                                }
                            });
                        }

                    } catch (Exception e) {
                        Log.e("APIError", "Error parsing JSON response", e);
                        progress.dismiss();
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }
    }


    public void hideKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

}