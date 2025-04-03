package com.ims.bpcluat.alp.alpOperations.cardManagement.balanceEnquiry;

import static android.app.Activity.RESULT_OK;
import static com.ims.bpcluat.Helper.appVersion;
import static com.ims.bpcluat.Helper.cashNotificationDate;
import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.createJsonObject;
import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.getCurrentDateTime;

import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.roName;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.Helper.username;
import static com.ims.bpcluat.helper.ApiHelper.alpEndpoint;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.fiserv.alpsdk.data.AlpRequest;
import com.fiserv.alpsdk.wrapper.AlpApi;
import com.ims.bpcluat.Helper;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.alp.alpOperations.cardManagement.CardManagementFragment;
import com.ims.bpcluat.databinding.FragmentMobileNumberBalanceBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.model.AlpModels.SaleModel.Program;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProgramWallet;
import com.ims.bpcluat.validation.MobileNoValidation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MobileNumberBalanceFragment extends Fragment implements ApiHelper.NetworkingApiCallBack {
    FragmentMobileNumberBalanceBinding binding;
    ProgressDialog progress;
    ApiHelper api;
    String txnId = "", dateTime = "";
    String programlist = "";
    String mobileNumber = "";
    String balanceenquiry = "";
    String id, cardNumber = "";
    private static final int CARD = 1;
    private static final String BALANCE_ENQUIRY = "BALANCE_ENQUIRY";
    private ActivityResultLauncher<Intent> activityResultLauncher;
    String amount = "", mobileNum = "", message = "", amountInPaise = "";
    String pumpNo = "", nozzleNo = "", localMPDId = "", localProductID = "", qty = "", vehId = "", unitAmt = "";
    String field1 = "", field3 = "", clientTxnId = "", fcctxnID = "";
    String programID = "", walletID = "", odometerReading = "", quantityLitres = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentMobileNumberBalanceBinding.inflate(inflater, container, false);
        api = new ApiHelper();

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new CardManagementFragment());
            }
        });

        binding.submitMobBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateFields();
            }
        });

        binding.insertcardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPhysicalCard();
            }
        });

        clientTxnId = tid + getCurrentDateTime();

        return binding.getRoot();

    }

    private void doPhysicalCard() {
        progress = new ProgressDialog(requireContext());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);
        progress.show();
        String reqDate = requestDate();
        String reqTime = requestTime();
        String dateTime = reqDate + reqTime;
        AlpRequest alpRequest = new AlpRequest();

        try {
            progress.dismiss();
            alpRequest.setAmountRs(amountInPaise);
            alpRequest.setAposTerminalId("");
            alpRequest.setTid(tid);
            alpRequest.setRoCode(sapCode);
            alpRequest.setHwSrNo(Helper.serialNumber);
            alpRequest.setDealerID(sapCode);
            alpRequest.setAppVersion(appVersion);
            alpRequest.setChannel("");
            alpRequest.setClient(manualGetClientId());
            alpRequest.setClientTxnId(clientTxnId);
            alpRequest.setCurrencyCode("");
            alpRequest.setDateTime(dateTime);
            alpRequest.setDiscountAmount("");
            alpRequest.setDiscountID("");
            alpRequest.setFcctxnID(fcctxnID);
            alpRequest.setGeotagRange("10");
            alpRequest.setInstId(manualGetInstId());
            alpRequest.setIpsMarker("1");
            alpRequest.setLatitude("0");
            alpRequest.setLocalBayID("");
            alpRequest.setLocalMPD_ID("");
            alpRequest.setLocalNozzleID("");
            alpRequest.setLocalProductID(localProductID);
            alpRequest.setLongitude("0");
            alpRequest.setMid(mid);
            alpRequest.setMobNo("");
            alpRequest.setNetAmountRs(amountInPaise);
            alpRequest.setOdometerReading("");
            alpRequest.setOriginalAlpTransactionId("");
            alpRequest.setOtp("");
            alpRequest.setPayInstrument("");
            alpRequest.setProgramID("");
            alpRequest.setPurpose("");
            alpRequest.setQuantityLitres(qty);
            alpRequest.setReasonForVoid("");
            alpRequest.setReqDate(requestDate());
            alpRequest.setReqTime(reqTime);
            alpRequest.setReqType(BALANCE_ENQUIRY);
            alpRequest.setTxnId(Helper.createTxnIdForOfflineTxn());
            alpRequest.setTxnType("APC");
            alpRequest.setUserName(username);
            alpRequest.setWalletID("");
            Log.d("alpRequestParams", alpRequest.toString());

            AlpApi.doBalanceInquiryPinRequest(requireActivity(), CARD, alpRequest);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        try {
            Log.d("sonuTest1", String.valueOf(data));
            super.onActivityResult(requestCode, resultCode, data);
            if (data != null) {
                if (requestCode == CARD) {
                    if (resultCode == RESULT_OK) {
                        Bundle extras = data.getExtras();
                        if (extras != null) {
                            // Loop through the extras and print all keys and values
                            for (String key : extras.keySet()) {
                                Object value = extras.get(key);
                                Log.d("IntentExtra", "Key: " + key + " Value: " + value);
                            }
                        } else {
                            Log.d("IntentExtra", "No extras found in the intent.");
                        }

                        String response = data.getStringExtra("alpResponse");
                        Log.d("sonuTest", response);
                        if (response != null) {
                            Log.d("loyaltyCardResponse", response);
                            try {
                                JSONObject jsonResponse = new JSONObject(response);
                                if(Helper.isAlpCodeExistsForStatusApiCall(jsonResponse.getString("code"))){
                                    JSONObject dataObject = jsonResponse.getJSONObject("data");
                                    JSONObject physicalCard = dataObject.getJSONObject("PhysicalCard");
                                    String cardName = physicalCard.getString("cardName");
                                    cardNumber = physicalCard.getString("cardNumber");

                                    Log.d("CardDetails", "Card Name: " + cardName);

                                    JSONObject alpRequest = dataObject.getJSONObject("alpRequest");
                                    txnId = alpRequest.getString("txnId");
                                    programID = alpRequest.getString("programID");
                                    walletID = alpRequest.getString("walletID");
                                    odometerReading = alpRequest.getString("odometerReading");
                                    quantityLitres = alpRequest.getString("quantityLitres");

                                    getActivity().runOnUiThread(() -> {
                                        progress.dismiss();
                                        fetchloyalitybalanceApi();
                                    });
                                } else {
                                    message = jsonResponse.getString("message");
                                    MessagesDialog.showDialog(getContext(), message, 0,null, null);
                                }
                            } catch (JSONException e) {
                                Log.e("loyaltyCardResponse", "Error parsing JSON: " + e.getMessage());
                            }
                        } else {
                            Log.d("loyaltyCardResponse", "No response received");
                        }
                    }
                } else {
                    progress.dismiss();
                    String ocrResult = data.getStringExtra("ocrResult");
                    if (ocrResult != null) {
                        ocrResult = ocrResult.replaceAll("\\s", "");
                        Log.d("loyaltyCardResponse@", ocrResult);
                    } else {
                        Log.d("loyaltyCardResponse@", "No OCR result received");
                    }
                }
            } else {
                progress.dismiss();
                // Log if data itself is null
                Log.d("loyaltyCardResponse@", "No intent data received");
            }
        } catch (Exception e) {
            Log.d("loyaltyCardResponse@", e.toString());
        }
    }

    private void fetchloyalitybalanceApi() {
        programlist = "balanceenquiryphysical";
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);
        progress.show();
        String url = "alpreq";
        String reqDate = requestDate();
        String reqTime = requestTime();
        dateTime = reqDate + reqTime;

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("channel", "BPCL");
            jsonObject.put("reqDate", reqDate);
            jsonObject.put("reqTime", reqTime);
            jsonObject.put("userName", username);
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("source", "Mobile");
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnType", "ACB");
            jsonObject.put("dateTime", dateTime);
            jsonObject.put("tranChannel", "PC");
            jsonObject.put("mobNo", "");
            jsonObject.put("id", clientTxnId);
            jsonObject.put("txnId", txnId);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);

            JSONArray billerTranList = new JSONArray();
            JSONObject billerTranItem;

            billerTranItem = new JSONObject();
            billerTranItem.put("field13", programID);
            billerTranItem.put("field14", "");
            billerTranItem.put("field15", cardNumber);
            billerTranList.put(billerTranItem);
            jsonObject.put("billerTranList", billerTranList);

            Log.d("validateOtpRequest=", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    //For Virtual Card

    private void validateFields() {
        mobileNumber = binding.entermobilenum.getText().toString().trim();
        if (TextUtils.isEmpty(mobileNumber)) {
            binding.entermobilenum.setError("Please enter mobile number");
            binding.entermobilenum.requestFocus();

            return;
        }
        if (TextUtils.isEmpty(mobileNumber)) {
            binding.entermobilenum.setError("Please enter mobile number");
            binding.entermobilenum.requestFocus();
            return;
        }
        if (mobileNumber.length() != 10) {
            binding.entermobilenum.setError("Mobile Number must be 10 digits");
            binding.entermobilenum.requestFocus();
            return;
        }
        if (mobileNumber.startsWith("0")) {
            binding.entermobilenum.setError("Mobile Number cannot start with zero");
            binding.entermobilenum.requestFocus();
            return;
        }
        if (mobileNumber.equals("1234567890")) {
            binding.entermobilenum.setError("Please enter valid mobile number");
            binding.entermobilenum.requestFocus();
            return;
        }
        if (MobileNoValidation.hasSameNumber(mobileNumber)) {
            binding.entermobilenum.setError("All digits of mobile number cannot be same.");
            binding.entermobilenum.requestFocus();
            return;
        }
        fetchVirtualCardProgramApi(mobileNumber);

    }


    private void fetchVirtualCardProgramApi(String mobileNumber) {
        programlist = "fetchVirtualCardProgramApi";
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
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("roCode", sapCode);
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("dateTime", dateTime);
            jsonObject.put("txnType", "AVC");
            jsonObject.put("mobNo", mobileNumber);
            jsonObject.put("txnId", txnId);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);
            Log.d("programListResponse=", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }


    public void apiResult(String res, String apiName) {
        if (programlist.equals("fetchVirtualCardProgramApi")) {
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                    });
                } else {
                    Log.d("programListResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payload = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    JSONArray outputArray = payload.optJSONArray("output");
                    String respDesc = payload.getString("respDesc");
                    String respCode = payload.getString("respCode");
                    if (payload.has("id")) {
                        id = payload.getString("id");
                    }

                    String mobNo = payload.getString("mobNo");

                    if (respCode.equals("200")) {
                        progress.dismiss();
                        if (outputArray != null && outputArray.length() > 0) {
                            JSONObject outputObject = outputArray.getJSONObject(0);

                            JSONArray programsArray = outputObject.getJSONArray("programs");
                            List<Program> programsList = new ArrayList<>();

                            for (int j = 0; j < programsArray.length(); j++) {
                                JSONObject programObject = programsArray.getJSONObject(j);

                                Program program = new Program("", "", "");
                                program.setProgram(programObject.getString("program"));
                                program.setAccountNumber(programObject.getString("accountNumber"));
                                program.setCardNumber(programObject.getString("cardNumber"));
                                program.setProgramID(programObject.getString("programID"));

                                programsList.add(program);
                            }

                            Bundle bundle = new Bundle();
                            bundle.putString("id", id);
                            bundle.putString("mobNo", mobNo);
                            bundle.putSerializable("programs", (Serializable) programsList);
                            ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, new ProgramListBalanceEnquiryFragment());
                        } else {
                            getActivity().runOnUiThread(() -> {
                                MessagesDialog.showDialog(getActivity(), respDesc, 0,null, null);
                            });
                        }
                    } else {
                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            MessagesDialog.showDialog(getActivity(), respDesc, 0,null, null);
                        });
                    }
                }

            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "balancefetchVirtualCardProgramApi", e.toString());
                getActivity().runOnUiThread(() -> {
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    MessagesDialog.showDialog(getActivity(), e.toString(), 0,null, null);
                });
            }
        } else {
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                    });
                } else {
                    Log.d("validateOtpRequest = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");

                    if (respCode.equals("200")) {
                        progress.dismiss();
                        Intent intent = new Intent(requireContext(), BalanceEnquiryReciept.class);
                        intent.putExtra("payload", payLoad.toString());
                        requireActivity().startActivity(intent);
                    } else {
                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            MessagesDialog.showDialog(getActivity(), respDesc, 0,null, null);
                        });
                    }
                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "fetchloyalitybalanceApiResponse", e.toString());
                getActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    Log.d("TAG", "JSONException: " + e);
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    MessagesDialog.showDialog(getActivity(), e.toString(), 0,null, null);

                    e.printStackTrace();
                });
            }
        }
    }


}
