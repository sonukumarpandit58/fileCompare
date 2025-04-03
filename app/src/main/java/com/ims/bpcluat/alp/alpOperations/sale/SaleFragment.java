package com.ims.bpcluat.alp.alpOperations.sale;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;
import static com.ims.bpcluat.Helper.appVersion;
import static com.ims.bpcluat.Helper.cashNotificationDate;
import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.createJsonObject;
import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.fuelProductList;

import static com.ims.bpcluat.Helper.getCurrentDateTime;
import static com.ims.bpcluat.Helper.getProductId;
import static com.ims.bpcluat.Helper.logLongMessage;
import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
//import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.roName;
import static com.ims.bpcluat.Helper.sapCode;
//import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.Helper.username;
import static com.ims.bpcluat.helper.ApiHelper.alpEndpoint;
import static com.ims.bpcluat.utils.Navigation.BackWithData;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.fiserv.alpsdk.data.AlpRequest;
import com.fiserv.alpsdk.wrapper.AlpApi;
import com.ims.bpcluat.Helper;
import com.ims.bpcluat.PaymentActivity;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.SuccessActivity;
import com.ims.bpcluat.alp.alpOperations.sale.loyalitypayqr.ScanQRFragment;
import com.ims.bpcluat.alp.alpOperations.sale.loyaltyotp.MobileNumberFragment;
import com.ims.bpcluat.alp.alpOperations.sale.preAuth.PreAuthMobileNumberFragment;
import com.ims.bpcluat.cng.CngFragment;
import com.ims.bpcluat.cng.CngPaymentActivity;
import com.ims.bpcluat.cng.CngPaymentActivity;
import com.ims.bpcluat.cng.CngSuccessActivity;
import com.ims.bpcluat.databinding.FragmentSaleBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProductModel;
import com.ims.bpcluat.model.CngModel;
import com.ims.bpcluat.model.NfrModel;
import com.ims.bpcluat.model.OnlineTxnModel;
import com.ims.bpcluat.nfr.NfrPaymentActivity;
import com.ims.bpcluat.nfr.NfrSuccessActivity;
import com.ims.bpcluat.utils.SharedPrefHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SaleFragment extends Fragment implements ApiHelper.NetworkingApiCallBack {
    FragmentSaleBinding binding;
    ProgressDialog progress;
    ApiHelper api;
    String txnId = "", dateTime = "";
    String programListApiCall = "";
    SharedPrefHelper sharedPrefHelper;
    private Context context;
    List<ProductModel> productList = new ArrayList<>();
    private CngModel cngModel;
    String amount = "", moblieNum = "";
    private OnlineTxnModel onlineTxnModel;
    String setIsTxnOnline="no";
    private NfrModel nfrModel;
    String pumpNo = "", nozzleNo = "", localMPDId = "", localProductID = "", qty = "", vehId = "", unitAmt = "", field1 = "",field3 ="",field7 ="",field9 ="", rrn = "";
    String amountInPaise = "", clientTxnId = "", fcctxnID = "";
    private static final String LOYALTY_CARD = "LOYALTY_CARD";
    private static final int CARD = 1;
    String mobileNum = "", message = "";
    String programID = "", walletID = "", odometerReading = "", quantityLitres = "",cardNumber = "",isTxnOnline= "";
    String alpVehicleNumber = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSaleBinding.inflate(inflater, container, false);
        DrawerLayout drawerLayout = getActivity().findViewById(R.id.drawerLayout);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        api = new ApiHelper();
        context = getActivity();

        sharedPrefHelper = new SharedPrefHelper(requireContext());

        if (getArguments() != null) {
            clientTxnId =  tid + getCurrentDateTime();
            field3 = tid + getCurrentDateTime();
            cngModel = getArguments().getParcelable("cngModel");
            onlineTxnModel = getArguments().getParcelable("onlineTxnModel");
            nfrModel = getArguments().getParcelable("nfrModel");
            if (cngModel != null) {
                amount = cngModel.getTotalAmt();
                unitAmt = cngModel.getPerAmt();
                moblieNum = cngModel.getMobileNumber();
                txnId = cngModel.getTxnId();
                qty = cngModel.getQty();
                vehId = cngModel.getVehicleNumber();
                field1 = "Offline";
                localProductID = getProductId("CNG", fuelProductList);
                fcctxnID = "";
            } else if (onlineTxnModel != null) {
                amount = onlineTxnModel.getAmount();
                unitAmt = onlineTxnModel.getUnitPrice();
                moblieNum = onlineTxnModel.getMobileNumber();
                pumpNo = onlineTxnModel.getPumpNo();
                nozzleNo = onlineTxnModel.getPumpNo();
                localMPDId = onlineTxnModel.getLocalMPDId();
                localProductID = getProductId(onlineTxnModel.getProductName(), fuelProductList);
                txnId = onlineTxnModel.getTxnId();
                qty = onlineTxnModel.getQty();
                vehId = onlineTxnModel.getVehicleNumber();
                isTxnOnline = onlineTxnModel.getIsTxnOnline();
                if(isTxnOnline.equals("yes")){
                    field1 = "Online";
                    fcctxnID = onlineTxnModel.getTxnId();
                }else{
                    field1 = "Offline";
                    fcctxnID = "";
                }
            } else if (nfrModel != null) {
                amount = nfrModel.getAmt();
                moblieNum = nfrModel.getMobileNumber();
                SharedPreferences shared = getActivity().getSharedPreferences("nfrSharedPreferencesData", MODE_PRIVATE);
                qty = (shared.getString("totalQty", ""));
                txnId = nfrModel.getTxnId();

                vehId = nfrModel.getVehicleNumber();
                field1 = "Offline";
                localProductID = getProductId("LUBES", fuelProductList);
                unitAmt = nfrModel.getAmt();
                fcctxnID = "";

            }
            int amtInPaise = (int) (Double.parseDouble(amount) * 100);
            amountInPaise = String.valueOf(amtInPaise);
        }

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cngModel != null) {
                    TxnFailFragment fragment = new TxnFailFragment();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("cngModel", cngModel);
                    bundle.putString("txnStatus", "Transaction Failed!");

                    ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);

                } else if (onlineTxnModel != null) {
                    TxnFailFragment fragment = new TxnFailFragment();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("onlineTxnModel", onlineTxnModel);
                    bundle.putString("istxnoffline", "no");
                    bundle.putString("txnStatus", "Transaction Failed!");

                    ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);

                } else if (nfrModel != null) {
                    TxnFailFragment fragment = new TxnFailFragment();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("nfrModel", nfrModel);
                    bundle.putString("txnStatus", "Transaction Failed!");

                    ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);
                }
            }
        });

        binding.loyalitycardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sharedPrefHelper.setString("smartpayBtn", "loyalitycardBtn");
//                Intent intent = new Intent(getActivity(), LoyalityInsertCard.class);
//                intent.putExtra("cngModel", cngModel);
//                intent.putExtra("nfrModel", nfrModel);
//                intent.putExtra("onlineTxnModel", onlineTxnModel);
//                startActivity(intent);
                doPhysicalCard();
            }
        });

        binding.loyalityotpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sharedPrefHelper.setString("smartpayBtn", "loyalityotpBtn");
                MobileNumberFragment fragment = new MobileNumberFragment();
                Bundle bundle = new Bundle();
                bundle.putParcelable("cngModel", cngModel);
                bundle.putParcelable("nfrModel", nfrModel);
                bundle.putParcelable("onlineTxnModel", onlineTxnModel);

                Log.d("onlineTxnMsdsdsdodel", String.valueOf(onlineTxnModel));

                ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);
            }
        });

        binding.smartpayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sharedPrefHelper.setString("smartpayBtn", "smartpayBtn");
                smartPayQrApi(amount);
            }
        });

        binding.preAuthId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PreAuthMobileNumberFragment fragment = new PreAuthMobileNumberFragment();
                Bundle bundle = new Bundle();
                bundle.putParcelable("cngModel", cngModel);
                bundle.putParcelable("nfrModel", nfrModel);
                bundle.putParcelable("onlineTxnModel", onlineTxnModel);

                Log.d("onlineTxnMsdsdsdodel", String.valueOf(onlineTxnModel));

                ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);
            }
        });

        return binding.getRoot();
    }

    private void smartPayQrApi(String amnt) {
        programListApiCall = "smartPayQrApi";
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);
        progress.show();
        String url = "alpreq";
        String reqDate = requestDate();
        String reqTime = requestTime();
        txnId = Helper.createTxnIdForOfflineTxn();

        JSONObject jsonObject = new JSONObject();
        JSONArray billerTranList = new JSONArray();
        JSONObject billerTranItem = new JSONObject();
        JSONArray paramList = new JSONArray();
        try {

            jsonObject.put("channel", "BPCL");
            jsonObject.put("reqDate", reqDate);
            jsonObject.put("reqTime", reqTime);
            jsonObject.put("userName", username);
            jsonObject.put("tid", tid);
            jsonObject.put("mid", mid);
            jsonObject.put("txnType", "ASQ");
            jsonObject.put("tranChannel", "VC");
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL1.0.0");
            billerTranItem.put("mid", mid);
            billerTranItem.put("tid", tid);
            billerTranItem.put("trans_status", "PENDING");
            billerTranItem.put("tran_amt", amnt);
            billerTranItem.put("tran_date", reqDate);
            billerTranItem.put("tran_time", reqTime);
            billerTranItem.put("ft_number", txnId);
            billerTranItem.put("cust_id", username);
            billerTranItem.put("pay_method", "ALP");
            billerTranItem.put("authAmt", amnt);
            billerTranItem.put("refundAmt", "0");
            billerTranItem.put("balanceAmt", amnt);
            billerTranItem.put("field1", field1);

            billerTranList.put(billerTranItem);
            billerTranItem.put("paramList", paramList);

            paramList.put(createJsonObject(pumpNo, "PUMP_NO"));
            paramList.put(createJsonObject(localMPDId, "localMPDId"));
            paramList.put(createJsonObject(nozzleNo, "NOZZLE"));
            paramList.put(createJsonObject(localProductID, "localProductID"));
            paramList.put(createJsonObject(moblieNum, "Customer Mobile"));
            paramList.put(createJsonObject(sapCode, "SAP CODE"));
            paramList.put(createJsonObject("SmartDrive", "ProgramId"));
            paramList.put(createJsonObject("", "WalletId"));
            paramList.put(createJsonObject("", "odometerReading"));
            paramList.put(createJsonObject(qty, "QUANTITY"));
            paramList.put(createJsonObject(roName, "MERCH NAME"));
            paramList.put(createJsonObject("", "Attendant ID"));
            paramList.put(createJsonObject(unitAmt, "UNIT_PRICE"));
            paramList.put(createJsonObject(vehId, "Vehicle ID"));
            paramList.put(createJsonObject("", "CUSTOMER_DISC"));
            paramList.put(createJsonObject(reqDate + reqTime, "FCC TIMESTAMP"));
            paramList.put(createJsonObject("", "discountID"));

            jsonObject.put("billerTranList", billerTranList);

            Log.d("ApiName","Loyalty Smart Pay QR - ASQ");
            Log.d("Request", String.valueOf(jsonObject));
            Log.d("localProductID", localProductID);
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void fetchConfigurationAPI() {
        programListApiCall = "fetchConfigurationAPI";
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
            jsonObject.put("reqDate", reqDate);
            jsonObject.put("reqTime", reqTime);
            jsonObject.put("dateTime", dateTime);
            jsonObject.put("txnType", "ACF");
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnId", txnId);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);

            Log.d("ApiName","ALP Configuration - ACF");
            Log.d("Request", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void doPhysicalCard() {
        progress = new ProgressDialog(context);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);
        progress.show();
        String reqDate = requestDate();
        String reqTime = requestTime();
        String dateTime = reqDate + reqTime;
        AlpRequest alpRequest = new AlpRequest();
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
            alpRequest.setReqType(LOYALTY_CARD);
            alpRequest.setTxnId(txnId);
            alpRequest.setTxnType("APC");
            alpRequest.setUserName(username);
            alpRequest.setWalletID("");

            Log.d("LoyaltyCardRequest = ", alpRequest.toString());
            AlpApi.doLoyaltyCardRequest(getActivity(), CARD, alpRequest);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        try {
            Log.d("SaleFragment : onActivityResult method", String.valueOf(data));
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
                        progress.dismiss();
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
                                    txnId = alpRequest.optString("txnId");
                                    programID = alpRequest.optString("programID");
                                    walletID = alpRequest.optString("walletID");
                                    odometerReading = alpRequest.optString("odometerReading");
                                    quantityLitres = alpRequest.optString("quantityLitres");

                                    Log.d("AlpRequestDetails", "Transaction ID: " + txnId);

                                   getActivity().runOnUiThread(() -> {
                                        cardTransactionStatusApi();
                                    });
                                } else {
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
            progress.dismiss();
            Log.d("loyaltyCardResponse@", e.toString());
        }
    }

    private void cardTransactionStatusApi() {
        programListApiCall = "cardTransactionStatusApi";
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);
        progress.show();
        String url = alpEndpoint;

        String reqDate = requestDate();
        String reqTime = requestTime();

        JSONObject jsonObject = new JSONObject();
        JSONArray billerTranListArray = new JSONArray();
        JSONObject billerTranListObject = new JSONObject();
        JSONArray paramListArray = new JSONArray();
        try {
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("source", "Mobile");
            jsonObject.put("channel", channelName);
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("userName", username);
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("txnType", "ATS");
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("appVersion", "BPCL" + appVersion);

            billerTranListObject.put("mid", mid);
            billerTranListObject.put("tid", tid);
            billerTranListObject.put("trans_status", "PENDING");
            billerTranListObject.put("tran_amt", amount);
            billerTranListObject.put("tran_date", cashNotificationDate());
            billerTranListObject.put("tran_time", requestTime());
            billerTranListObject.put("ft_number", txnId);
            billerTranListObject.put("cust_id", username);
            billerTranListObject.put("pay_method", "ALP");
            billerTranListObject.put("authAmt", amount);
            billerTranListObject.put("refundAmt", "0");
            billerTranListObject.put("balanceAmt", amount);
            billerTranListObject.put("field1", field1);
            billerTranListObject.put("field3", field3);
            billerTranListObject.put("field13", "PC");

            billerTranListArray.put(billerTranListObject);
            billerTranListObject.put("paramList", paramListArray);
            paramListArray.put(createJsonObject(pumpNo, "PUMP_NO"));
            paramListArray.put(createJsonObject(localMPDId, "localMPDId"));
            paramListArray.put(createJsonObject(nozzleNo, "NOZZLE"));
            paramListArray.put(createJsonObject(localProductID, "localProductID"));
            paramListArray.put(createJsonObject(mobileNum, "Customer Mobile"));
            paramListArray.put(createJsonObject(sapCode, "SAP CODE"));
            paramListArray.put(createJsonObject(programID, "ProgramId"));
            paramListArray.put(createJsonObject(walletID, "WalletId"));
            paramListArray.put(createJsonObject(odometerReading, "odometerReading"));
            paramListArray.put(createJsonObject(qty, "QUANTITY"));
            paramListArray.put(createJsonObject(roName, "MERCH NAME"));
            paramListArray.put(createJsonObject("", "Attendant ID"));
            paramListArray.put(createJsonObject(unitAmt, "UNIT_PRICE"));
            paramListArray.put(createJsonObject(vehId, "Vehicle ID"));
            paramListArray.put(createJsonObject("", "CUSTOMER_DISC"));
            paramListArray.put(createJsonObject(reqDate + reqTime, "FCC TIMESTAMP"));
            paramListArray.put(createJsonObject("", "discountID"));
            jsonObject.put("billerTranList", billerTranListArray);

            Log.d("StatusCheckRequest = ", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void apiResult(String res, String apiName) {
        if (programListApiCall.equals("smartPayQrApi")) {
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                    });
                } else {
                    Log.d("getSmartPayQrApiResponse = ", res);
                    logLongMessage("getSmartP", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");

                    if (respCode.equals("200")) {
                        JSONArray billerTranList = payLoad.getJSONArray("billerTranList");
                        JSONObject transaction = billerTranList.getJSONObject(0);
                        field3 = transaction.getString("field3");

                        if (cngModel != null) {
                            cngModel.setField3(field3);
                        } else if (nfrModel != null) {
                            nfrModel.setField3(field3);
                        } else {
                            onlineTxnModel.setField3(field3);
                        }

                        getActivity().runOnUiThread(() -> {
                            Log.d("respCode", respCode);
                            progress.dismiss();
                            ScanQRFragment fragment = new ScanQRFragment();
                            Bundle bundle = new Bundle();
                            bundle.putString("payLoad", payLoad.toString());
                            bundle.putParcelable("cngModel", cngModel);
                            bundle.putParcelable("nfrModel", nfrModel);
                            bundle.putParcelable("onlineTxnModel", onlineTxnModel);
                            ((SideBarActivity) context).loadFragmentWithData(bundle, fragment);
                        });

                    } else {
                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            Log.d("Exception", res);
                            Intent intent = new Intent();
                            Log.d("TAG", "apiResult: hhhhhheyy");
                            if (cngModel != null) {
                                intent = new Intent(getActivity(), CngPaymentActivity.class);
                                intent.putExtra("cngModel", cngModel);
                                intent.putExtra("Insertcard", "Insertcard");
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                Log.d("TAG", "cngModel: hhhhhheyy");

                            } else if (onlineTxnModel != null) {
                                if(isTxnOnline.equals("no")){
                                    intent = new Intent(getActivity(), PaymentActivity.class);
                                    intent.putExtra("onlineTxnModel", onlineTxnModel);
                                    intent.putExtra("isTxnOnline", "isTxnOnline");
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                }else{
                                    intent = new Intent(getActivity(), PaymentActivity.class);
                                    intent.putExtra("onlineTxnModel", onlineTxnModel);
                                    intent.putExtra("Insertcard", "Insertcard");
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                }
                            } else if (nfrModel != null) {
                                intent = new Intent(getActivity(), NfrPaymentActivity.class);
                                intent.putExtra("nfrModel", nfrModel);
                                intent.putExtra("Insertcard", "Insertcard");
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                Log.d("TAG", "nfrModel: hhhhhheyy");
                            }
                            MessagesDialog.showDialog(context, respDesc ,0, intent, null);
                        });
                    }
                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "smartPayQrApi", e.toString());
                getActivity().runOnUiThread(() -> {
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }

                    Intent intent = new Intent();
                    Log.d("TAG", "apiResult: hhhhhheyy");
                    if (cngModel != null) {
                        intent = new Intent(getActivity(), CngPaymentActivity.class);
                        intent.putExtra("cngModel", cngModel);
                        intent.putExtra("Insertcard", "Insertcard");
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        Log.d("TAG", "cngModel: hhhhhheyy");

                    } else if (onlineTxnModel != null) {
                        if(isTxnOnline.equals("no")){
                            intent = new Intent(getActivity(), PaymentActivity.class);
                            intent.putExtra("onlineTxnModel", onlineTxnModel);
                            intent.putExtra("isTxnOnline", "isTxnOnline");
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        }else{
                            intent = new Intent(getActivity(), PaymentActivity.class);
                            intent.putExtra("onlineTxnModel", onlineTxnModel);
                            intent.putExtra("Insertcard", "Insertcard");
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        }
                    } else if (nfrModel != null) {
                        intent = new Intent(getActivity(), NfrPaymentActivity.class);
                        intent.putExtra("nfrModel", nfrModel);
                        intent.putExtra("Insertcard", "Insertcard");
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        Log.d("TAG", "nfrModel: hhhhhheyy");
                    }
                    MessagesDialog.showDialog(context, e.toString(),0, intent, null);
                });
            }
        }else {
            try {
                if (res.equals("Server Time Out")) {
                   getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(context, "Server Time Out", 0,null, null);
                    });
                } else {
                    Log.d("cardTransactionStatusApiResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");

                    JSONArray billerTranListArray = payLoad.getJSONArray("billerTranList");
                    JSONObject billerTran = billerTranListArray.getJSONObject(0);
                    String trans_status = billerTran.getString("trans_status");
                    Log.d("trans_statusSS",trans_status);

                    // String rrn = billerTran.getString("rrn");
                    String ft_number = billerTran.getString("ft_number");
                    if (respCode.equals("200") && trans_status.equals("SUCCESS")) {
                        progress.dismiss();
                        field7 = billerTran.optString("field7");
                        field9 = billerTran.optString("field9");
                        rrn = billerTran.optString("rrn");
                        JSONArray outputArray = payLoad.getJSONArray("output");
                        JSONObject outputArrayJSONObject = outputArray.getJSONObject(0);
                        String ROName = outputArrayJSONObject.optString("ROName","");
                        String roCity = outputArrayJSONObject.optString("roCity");
                        String roMobileNo = outputArrayJSONObject.optString("roMobileNo");
                        String alpTid = outputArrayJSONObject.optString("aposTerminalID");
                        String alpTxnId = outputArrayJSONObject.optString("alpTransactionId");
                        String alpSlipNo = outputArrayJSONObject.optString("chargeSlipNumber");
                        Log.d("alpSlipNo",alpSlipNo);
                        String alpReportId = outputArrayJSONObject.optString("reportID");
                        String alpType = outputArrayJSONObject.optString("txnType");
                        String alpTxnSource = outputArrayJSONObject.optString("txnSource");
                        String alpCustName = outputArrayJSONObject.optString("customerName");
                        String alpAccNo = outputArrayJSONObject.optString("customerAccountNumber");
                        String alpCardId = outputArrayJSONObject.optString("customerCardNumber");
                        String alpVechCard = "";
                        String alpOdometer = outputArrayJSONObject.optString("odometerReading");
                        String alpWallet = outputArrayJSONObject.optString("txnMode");
                        String alpProduct = outputArrayJSONObject.optString("txnProduct");
                        String alpRate = outputArrayJSONObject.optString("productRate");
                        String alpVol = "";
                        String alpFuelAmount = outputArrayJSONObject.optString("fuelAmount");
                        String alpTcsAmount = outputArrayJSONObject.optString("tcsAmount");
                        String alpTxnAmount = outputArrayJSONObject.optString("txnAmount");
                        String alpPmEarn = outputArrayJSONObject.optString("petroMilesEarned");
                        String alpMeShare = outputArrayJSONObject.optString("txnMEShare");
                        String alpCardBalance = outputArrayJSONObject.optString("cardBalance");
                        if(outputArrayJSONObject.has("vehicleNumber")){
                            alpVehicleNumber = outputArrayJSONObject.optString("vehicleNumber");
                        }

                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            if (trans_status.equals("SUCCESS")) {
                                if (cngModel != null) {
                                    cngModel.setROName(ROName);
                                    cngModel.setRoCity(roCity);
                                    cngModel.setRoMobileNo(roMobileNo);
                                    cngModel.setField3(field3);
                                    cngModel.setField7(field7);
                                    cngModel.setField9(field9);
                                    cngModel.setField13("PC");
                                    cngModel.setVehicleNumber(alpVehicleNumber);
                                    cngModel.setTxnId(ft_number);
                                    cngModel.setRrn(rrn);
                                    cngModel.setAlpTid(alpTid);
                                    cngModel.setAlpTxnId(alpTxnId);
                                    cngModel.setAlpSlipNo(alpSlipNo);
                                    cngModel.setAlpReportId(alpReportId);
                                    cngModel.setAlpType(alpType);
                                    cngModel.setAlpTxnSource(alpTxnSource);
                                    cngModel.setAlpCustName(alpCustName);
                                    cngModel.setAlpAccNo(alpAccNo);
                                    cngModel.setAlpCardId(alpCardId);
                                    cngModel.setAlpVechCard(cngModel.getVehicleNumber());
                                    cngModel.setAlpOdometer(alpOdometer);
                                    cngModel.setAlpWallet(alpWallet);
                                    cngModel.setAlpProduct(alpProduct);
                                    cngModel.setAlpRate(alpRate);
                                    cngModel.setAlpVol(cngModel.getQty());
                                    cngModel.setAlpFuelAmount(alpFuelAmount);
                                    cngModel.setAlpTcsAmount(alpTcsAmount);
                                    cngModel.setAlpTxnAmount(alpTxnAmount);
                                    cngModel.setAlpPmEarn(alpPmEarn);
                                    cngModel.setAlpMeShare(alpMeShare);
                                    cngModel.setAlpCardBalance(alpCardBalance);
                                } else if (nfrModel != null) {
                                    nfrModel.setROName(ROName);
                                    nfrModel.setRoCity(roCity);
                                    nfrModel.setRoMobileNo(roMobileNo);
                                    nfrModel.setField3(field3);
                                    nfrModel.setField7(field7);
                                    nfrModel.setField9(field9);
                                    nfrModel.setField13("PC");
                                    nfrModel.setVehicleNumber(alpVehicleNumber);
                                    nfrModel.setRrn(rrn);
                                    nfrModel.setTxnId(ft_number);
                                    nfrModel.setAlpTid(alpTid);
                                    nfrModel.setAlpTxnId(alpTxnId);
                                    nfrModel.setAlpSlipNo(alpSlipNo);
                                    nfrModel.setAlpReportId(alpReportId);
                                    nfrModel.setAlpType(alpType);
                                    nfrModel.setAlpTxnSource(alpTxnSource);
                                    nfrModel.setAlpCustName(alpCustName);
                                    nfrModel.setAlpAccNo(alpAccNo);
                                    nfrModel.setAlpCardId(alpCardId);
                                    nfrModel.setAlpVechCard(nfrModel.getVehicleNumber());
                                    nfrModel.setAlpOdometer(alpOdometer);
                                    nfrModel.setAlpWallet(alpWallet);
                                    nfrModel.setAlpProduct(alpProduct);
                                    nfrModel.setAlpRate(alpRate);
                                    nfrModel.setAlpVol(nfrModel.getQty());
                                    nfrModel.setAlpFuelAmount(alpFuelAmount);
                                    nfrModel.setAlpTcsAmount(alpTcsAmount);
                                    nfrModel.setAlpTxnAmount(alpTxnAmount);
                                    nfrModel.setAlpPmEarn(alpPmEarn);
                                    nfrModel.setAlpMeShare(alpMeShare);
                                    nfrModel.setAlpCardBalance(alpCardBalance);
                                } else {
                                    onlineTxnModel.setROName(ROName);
                                    onlineTxnModel.setRoCity(roCity);
                                    onlineTxnModel.setRoMobileNo(roMobileNo);
                                    onlineTxnModel.setField3(field3);
                                    onlineTxnModel.setField7(field7);
                                    onlineTxnModel.setField9(field9);
                                    onlineTxnModel.setField13("PC");
                                    onlineTxnModel.setVehicleNumber(alpVehicleNumber);
                                    onlineTxnModel.setRrn(rrn);
                                    onlineTxnModel.setTxnId(ft_number);
                                    onlineTxnModel.setAlpTid(alpTid);
                                    onlineTxnModel.setAlpTxnId(alpTxnId);
                                    onlineTxnModel.setAlpSlipNo(alpSlipNo);
                                    onlineTxnModel.setAlpReportId(alpReportId);
                                    onlineTxnModel.setAlpType(alpType);
                                    onlineTxnModel.setAlpTxnSource(alpTxnSource);
                                    onlineTxnModel.setAlpCustName(alpCustName);
                                    onlineTxnModel.setAlpAccNo(alpAccNo);
                                    onlineTxnModel.setAlpCardId(alpCardId);
                                    onlineTxnModel.setAlpVechCard(onlineTxnModel.getVehicleNumber());
                                    onlineTxnModel.setAlpOdometer(alpOdometer);
                                    onlineTxnModel.setAlpWallet(alpWallet);
                                    onlineTxnModel.setAlpProduct(alpProduct);
                                    onlineTxnModel.setAlpRate(alpRate);
                                    onlineTxnModel.setAlpVol(onlineTxnModel.getQty());
                                    onlineTxnModel.setAlpFuelAmount(alpFuelAmount);
                                    onlineTxnModel.setAlpTcsAmount(alpTcsAmount);
                                    onlineTxnModel.setAlpTxnAmount(alpTxnAmount);
                                    onlineTxnModel.setAlpPmEarn(alpPmEarn);
                                    onlineTxnModel.setAlpMeShare(alpMeShare);
                                    onlineTxnModel.setAlpCardBalance(alpCardBalance);
                                }
                                redirectToSuccessPage();
                            } else {
                                MessagesDialog.showDialog(context, trans_status, 0,null, null);

                                //  Toast.makeText(context, trans_status + " - " + respCode, Toast.LENGTH_SHORT).show();
                            }
                        });

                    } else {
                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            Intent intent = new Intent();
                            Log.d("TAG", "apiResult: hhhhhheyy");
                            if (cngModel != null) {
                                 intent = new Intent(getActivity(), CngPaymentActivity.class);
                                intent.putExtra("cngModel", cngModel);
                                intent.putExtra("Insertcard", "Insertcard");
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                Log.d("TAG", "cngModel: hhhhhheyy");

                            } else if (onlineTxnModel != null) {
                                if(isTxnOnline.equals("no")){
                                     intent = new Intent(getActivity(), PaymentActivity.class);
                                    intent.putExtra("onlineTxnModel", onlineTxnModel);
                                    intent.putExtra("isTxnOnline", "isTxnOnline");
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                }else{
                                     intent = new Intent(getActivity(), PaymentActivity.class);
                                    intent.putExtra("onlineTxnModel", onlineTxnModel);
                                    intent.putExtra("Insertcard", "Insertcard");
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                }
                            } else if (nfrModel != null) {
                                 intent = new Intent(getActivity(), NfrPaymentActivity.class);
                                intent.putExtra("nfrModel", nfrModel);
                                intent.putExtra("Insertcard", "Insertcard");
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                Log.d("TAG", "nfrModel: hhhhhheyy");
                            }

                            MessagesDialog.showDialog(context, respDesc,0, intent, null);

                        });
                    }
                }
            } catch (JSONException e) {
                fileWrite(context, todayDate + ".txt", "getshiftsummaryResponse", e.toString());
                getActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    Intent intent = new Intent();
                    Log.d("TAG", "apiResult: hhhhhheyy");
                    if (cngModel != null) {
                        intent = new Intent(getActivity(), CngPaymentActivity.class);
                        intent.putExtra("cngModel", cngModel);
                        intent.putExtra("Insertcard", "Insertcard");
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        Log.d("TAG", "cngModel: hhhhhheyy");

                    } else if (onlineTxnModel != null) {
                        if(isTxnOnline.equals("no")){
                            intent = new Intent(getActivity(), PaymentActivity.class);
                            intent.putExtra("onlineTxnModel", onlineTxnModel);
                            intent.putExtra("isTxnOnline", "isTxnOnline");
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        }else{
                            intent = new Intent(getActivity(), PaymentActivity.class);
                            intent.putExtra("onlineTxnModel", onlineTxnModel);
                            intent.putExtra("Insertcard", "Insertcard");
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        }
                    } else if (nfrModel != null) {
                        intent = new Intent(getActivity(), NfrPaymentActivity.class);
                        intent.putExtra("nfrModel", nfrModel);
                        intent.putExtra("Insertcard", "Insertcard");
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        Log.d("TAG", "nfrModel: hhhhhheyy");
                    }
                    MessagesDialog.showDialog(context, e.toString(),0, intent, null);

                    // Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
                });
            }
        }

    }

    private void redirectToSuccessPage() {
        if (cngModel != null) {
            Intent intent = new Intent(context, CngSuccessActivity.class);
            intent.putExtra("cngModel", cngModel);
            startActivity(intent);
        } else if (nfrModel != null) {
            Intent intent = new Intent(context, NfrSuccessActivity.class);
            intent.putExtra("nfrModel", nfrModel);
            startActivity(intent);
        } else {
            Intent intent = new Intent(context, SuccessActivity.class);
            intent.putExtra("onlineTxnModel", onlineTxnModel);
            startActivity(intent);
        }

    }

}