package com.ims.bpcluat.alp.alpOperations.cardManagement;

import static android.app.Activity.RESULT_OK;
import static com.ims.bpcluat.Helper.appVersion;
import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.getClientTxnId;
import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.Helper.username;
import static com.ims.bpcluat.Helper.version;
import static com.ims.bpcluat.helper.ApiHelper.alpEndpoint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.fiserv.alpsdk.data.AlpRequest;
import com.fiserv.alpsdk.wrapper.AlpApi;
import com.ims.bpcluat.Helper;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.alp.alpOperations.AlpOperationsFragment;
import com.ims.bpcluat.alp.alpOperations.cardManagement.balanceEnquiry.MobileNumberBalanceFragment;
import com.ims.bpcluat.alp.alpOperations.cardManagement.enroll_additional.EnrolAddCardManagerPinFrag;
import com.ims.bpcluat.databinding.FragmentCardManagementBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.utils.SharedPrefHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CardManagementFragment extends Fragment implements ApiHelper.NetworkingApiCallBack  {
    FragmentCardManagementBinding binding;
    SharedPrefHelper sharedPrefHelper;
    private Context context;
    ProgressDialog progress;
    ApiHelper api;
    private static int CARD = 1;
    private static final String PIN_CHANGE = "PIN_CHANGE";
    private static final String PIN_RESET = "PIN_RESET";
    String message = "", amountInPaise = "",statusMessage = "",localProductID = "", qty = "",txnId = "", fcctxnID = "";
    String programID = "", walletID = "", odometerReading = "", quantityLitres = "",pinChangeClientTxnId = "",pinResetClientTxnId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCardManagementBinding.inflate(inflater, container, false);
        context = getContext();
        api = new ApiHelper();
        sharedPrefHelper = new SharedPrefHelper(requireContext());
        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new AlpOperationsFragment());
            }
        });

        binding.balanceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((SideBarActivity) requireActivity()).loadFragement(new MobileNumberBalanceFragment());
            }
        });

        binding.pinchangeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doPinChangeCard();
            }
        });

        binding.pinresetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doPinResetCard();
            }
        });

        binding.newenrolBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sharedPrefHelper.setString("cardManagementBtn", "newenrolBtn");
                ((SideBarActivity) requireActivity()).loadFragement(new EnrolAddCardManagerPinFrag());
            }
        });

        binding.aditionalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sharedPrefHelper.setString("cardManagementBtn", "aditionalBtn");
                ((SideBarActivity) requireActivity()).loadFragement(new EnrolAddCardManagerPinFrag());
            }
        });
        return binding.getRoot();
    }

    private void doPinChangeCard() {
        progress = new ProgressDialog(context);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);
        progress.show();
        String reqDate = requestDate();
        String reqTime = requestTime();
        String dateTime = reqDate + reqTime;
        AlpRequest alpRequest = new AlpRequest();
        pinChangeClientTxnId = getClientTxnId();
        CARD = 1;
        try {
            alpRequest.setAmountRs(amountInPaise);
            alpRequest.setAposTerminalId("");
            alpRequest.setTid(tid);
            alpRequest.setRoCode(sapCode);
            alpRequest.setHwSrNo(Helper.serialNumber);
            alpRequest.setDealerID(sapCode);
            alpRequest.setAppVersion(appVersion);
            alpRequest.setChannel("");
            alpRequest.setClient(manualGetClientId());
            alpRequest.setClientTxnId(pinChangeClientTxnId);
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
            alpRequest.setReqType(PIN_CHANGE);
            alpRequest.setTxnId(pinChangeClientTxnId);
            alpRequest.setTxnType("APC");
            alpRequest.setUserName(username);
            alpRequest.setWalletID("");
            AlpApi.doPinChangeRequest(requireActivity(), CARD, alpRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doPinResetCard() {
        progress = new ProgressDialog(context);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);
        progress.show();
        String reqDate = requestDate();
        String reqTime = requestTime();
        String dateTime = reqDate + reqTime;
        CARD = 2;
        AlpRequest alpRequest = new AlpRequest();
        pinResetClientTxnId = getClientTxnId();
        try {
            alpRequest.setAmountRs(amountInPaise);
            alpRequest.setAposTerminalId("");
            alpRequest.setTid(tid);
            alpRequest.setRoCode(sapCode);
            alpRequest.setHwSrNo(Helper.serialNumber);
            alpRequest.setDealerID(sapCode);
            alpRequest.setAppVersion(appVersion);
            alpRequest.setChannel("");
            alpRequest.setClient(manualGetClientId());
            alpRequest.setClientTxnId(pinResetClientTxnId);
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
            alpRequest.setReqType(PIN_RESET);
            alpRequest.setTxnId(pinResetClientTxnId);
            alpRequest.setTxnType("AOP");
            alpRequest.setUserName(username);
            alpRequest.setWalletID("");
            Log.d("alpRequestParams", alpRequest.toString());
            AlpApi.doPinResetRequest(requireActivity(), CARD, alpRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
       if(CARD == 1){
           try {
               super.onActivityResult(requestCode, resultCode, data);
               if (data != null) {
                   if (requestCode == CARD) {
                       if (resultCode == RESULT_OK) {
                           Bundle extras = data.getExtras();
                           if (extras != null) {
                               for (String key : extras.keySet()) {
                                   Object value = extras.get(key);
                                   Log.d("IntentExtra", "Key: " + key + " Value: " + value);
                               }
                           } else {
                               Log.d("IntentExtra", "No extras found in the intent.");
                           }

                           progress.dismiss();
                           String response = data.getStringExtra("alpResponse");
                           Log.d("sonuTest", response);
                           if (response != null) {
                               Log.d("pinChangedAlpResponse", response);
                               try {
                                   JSONObject jsonResponse = new JSONObject(response);
                                   Log.d("cardCode",(jsonResponse.getString("code")));
                                   if(Helper.isAlpCodeExistsForStatusApiCall(jsonResponse.getString("code"))){
                                       JSONObject alpRequest = jsonResponse.getJSONObject("data").getJSONObject("alpRequest");
                                       txnId = alpRequest.getString("txnId");
                                       programID = alpRequest.getString("programID");
                                       walletID = alpRequest.getString("walletID");
                                       odometerReading = alpRequest.getString("odometerReading");
                                       quantityLitres = alpRequest.getString("quantityLitres");
                                       requireActivity().runOnUiThread(() -> {
                                           progress.dismiss();
                                           resetStatusApi(pinChangeClientTxnId);
                                       });
                                   }else{
                                       message = jsonResponse.getString("message");
                                       MessagesDialog.showDialog(context, message, 0,null, null);
                                   }
                               } catch (JSONException e) {
                                   progress.dismiss();
                                   Log.e("loyaltyCardResponse", "Error parsing JSON: " + e.getMessage());
                               }
                           } else {
                               progress.dismiss();
                               Log.d("loyaltyCardResponse", "No response received");
                           }
                       }
                   }
               } else {
                   progress.dismiss();
                   Log.d("loyaltyCardResponse@", "No intent data received");
               }
           } catch (Exception e) {
               progress.dismiss();
               Log.d("loyaltyCardResponse@", e.toString());
           }
       }else {
           try {
               Log.d("pinChangeTest2", String.valueOf(data));
               super.onActivityResult(requestCode, resultCode, data);
               if (data != null) {
                   if (requestCode == CARD) {
                       if (resultCode == RESULT_OK) {
                           Bundle extras = data.getExtras();
                           if (extras != null) {
                               for (String key : extras.keySet()) {
                                   Object value = extras.get(key);
                                   Log.d("IntentExtra", "Key: " + key + " Value: " + value);
                               }
                           } else {
                               Log.d("IntentExtra", "No extras found in the intent.");
                           }

                           progress.dismiss();
                           String response = data.getStringExtra("alpResponse");
                           Log.d("sonuTest", response);
                           if (response != null) {
                               Log.d("pinResetAlpResponse", response);
                               try {
                                   JSONObject jsonResponse = new JSONObject(response);
                                   Log.d("cardCode",(jsonResponse.getString("code")));
                                   if(Helper.isAlpCodeExistsForStatusApiCall(jsonResponse.getString("code"))){
                                       JSONObject alpRequest = jsonResponse.getJSONObject("data").getJSONObject("alpRequest");
                                       txnId = alpRequest.getString("txnId");
                                       programID = alpRequest.getString("programID");
                                       walletID = alpRequest.getString("walletID");
                                       odometerReading = alpRequest.getString("odometerReading");
                                       quantityLitres = alpRequest.getString("quantityLitres");
                                       requireActivity().runOnUiThread(() -> {
                                           progress.dismiss();
                                           resetStatusApi(pinResetClientTxnId);
                                       });
                                   }else{
                                       message = jsonResponse.getString("message");
                                       MessagesDialog.showDialog(context, message, 0,null, null);
                                   }
                               } catch (JSONException e) {
                                   progress.dismiss();
                                   Log.e("loyaltyCardResponse", "Error parsing JSON: " + e.getMessage());
                               }
                           } else {
                               progress.dismiss();
                               Log.d("loyaltyCardResponse", "No response received");
                           }
                       }
                   }
               } else {
                   progress.dismiss();
                   Log.d("loyaltyCardResponse@", "No intent data received");
               }
           } catch (Exception e) {
               progress.dismiss();
               Log.d("loyaltyCardResponse@", e.toString());
           }
       }
    }

    private void resetStatusApi(String cTxnId) {
        progress = new ProgressDialog(context);
        progress.setTitle("Loading");
        progress.setMessage("Wait while fetching details...");
        progress.setCancelable(false);
        progress.show();
        String url = alpEndpoint;
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("channel", channelName);
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("userName", username);
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnType", "APS");
            jsonObject.put("txnId", cTxnId);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", version);

            Log.d("ApiName","APS");
            Log.d("FileName","CardManagementFragment");
            Log.d("ApiRequest", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(CardManagementFragment.this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void apiResult(String res, String apiName) {
        try {
            if (res.equals("Server Time Out")) {
                getActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                });
            } else {
                Log.d("resetStatusApiResponse = ", res);
                JSONObject jsonObject = new JSONObject(res);
                JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                String respDesc = payLoad.getString("respDesc");
                String respCode = payLoad.getString("respCode");

                if (respCode.equals("200")) {
                    JSONArray outputArray = payLoad.getJSONArray("output");
                    if (outputArray.length() > 0) {
                        JSONObject outputObject = outputArray.getJSONObject(0);
                        statusMessage = outputObject.getString("statusMessage");
                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            MessagesDialog.showDialog(context, statusMessage, R.drawable.success, null, null);
                            Log.d("outputMessage", statusMessage);
                        });
                    }
                } else {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(context, respDesc, 0,null, null);
                    });
                }
            }
        } catch (JSONException e) {
            fileWrite(context, todayDate + ".txt", "resetStatusApiResponse", e.toString());
            getActivity().runOnUiThread(() -> {
                progress.dismiss();
                MessagesDialog.showDialog(context, e.toString(), 0,null, null);
            });
        }
    }

}