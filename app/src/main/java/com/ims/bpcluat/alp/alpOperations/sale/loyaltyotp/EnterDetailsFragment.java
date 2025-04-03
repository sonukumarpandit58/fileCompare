package com.ims.bpcluat.alp.alpOperations.sale.loyaltyotp;

import static com.ims.bpcluat.Helper.appVersion;
import static com.ims.bpcluat.Helper.cashNotificationDate;
import static com.ims.bpcluat.Helper.createJsonObject;
import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.fuelProductList;

import static com.ims.bpcluat.Helper.getProductId;
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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.PaymentActivity;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.SuccessActivity;
import com.ims.bpcluat.alp.alpOperations.sale.SaleFragment;
import com.ims.bpcluat.alp.alpOperations.sale.TxnFailFragment;
import com.ims.bpcluat.alp.alpOperations.sale.postAuth.postAuthMobileNummberFragment;
import com.ims.bpcluat.alp.alpOperations.sale.preAuth.PreAuthMobileNumberFragment;
import com.ims.bpcluat.cng.CngPaymentActivity;
import com.ims.bpcluat.cng.CngSuccessActivity;
import com.ims.bpcluat.databinding.FragmentEnterDetailsBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.model.AlpModels.SaleModel.Program;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProgramOutput;
import com.ims.bpcluat.model.AlpModels.SaleModel.VirtualCardProgramModel;
import com.ims.bpcluat.model.CngModel;
import com.ims.bpcluat.model.NfrModel;
import com.ims.bpcluat.model.OnlineTxnModel;
import com.ims.bpcluat.nfr.NfrPaymentActivity;
import com.ims.bpcluat.nfr.NfrSuccessActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class EnterDetailsFragment extends Fragment implements ApiHelper.NetworkingApiCallBack {
    FragmentEnterDetailsBinding binding;
    ProgressDialog progress;
    Context ctx;
    ApiHelper api;
    String txnId = "", dateTime = "", otppass = "", vehNum = "", odometer = "";
    String vartualcarinitiateotp = "";
    private CngModel cngModel;
    String amount = "", mobileNumber, walletId = "", programName = "";
    private OnlineTxnModel onlineTxnModel;
    private NfrModel nfrModel;
    int index = -1, walletIndex = -1, data = -1;
    ArrayList<VirtualCardProgramModel> virtualCardProgramModelArrayList = new ArrayList<>();
    String ft_number = "", field3 = "", field6 = "", field7 = "", cust_id = "", field9 = "", balanceAmt = "", authAmt = "";
    String unitPrice = "", qty = "", field1 = "", productId = "";
    String pumpNo = "", nozzleNo = "", localMPDId = "", vehId = "";
    String tran_date = "", tran_time = "", reqDate = "", reqTime = "";
    private CountDownTimer countDownTimer;
    int attempt = 0;
    String isTxnOnline = "", alpVehicleNumber = "", drive = "";
    String preauthID = "", message = "";
    String requestCheck = "no";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEnterDetailsBinding.inflate(inflater, container, false);
        api = new ApiHelper();
        ctx = getContext();
        showDialog();
        hideKeyboard();

        if (getArguments() != null) {
            cngModel = getArguments().getParcelable("cngModel");
            onlineTxnModel = getArguments().getParcelable("onlineTxnModel");
            nfrModel = getArguments().getParcelable("nfrModel");
            index = getArguments().getInt("index");
            drive = getArguments().getString("Drive");
            data = getArguments().getInt("wallet_index");
            if (data != -1) {
                walletIndex = getArguments().getInt("wallet_index");
            }

            virtualCardProgramModelArrayList = (ArrayList<VirtualCardProgramModel>) getArguments().getSerializable("virtualProgramList");
            if (virtualCardProgramModelArrayList != null) {
                for (VirtualCardProgramModel virtualCardProgramModel : virtualCardProgramModelArrayList) {
                    if (virtualCardProgramModel.getOutput() != null) {
                        for (ProgramOutput output : virtualCardProgramModel.getOutput()) {
                            for (Program program : output.getPrograms()) {
                                programName = output.getPrograms().get(index).getProgramID();
                                if (program.getProgramWallet() != null) {
                                    if (!program.getProgramWallet().get(walletIndex).getWalletId().isEmpty()) {
                                        walletId = program.getProgramWallet().get(walletIndex).getWalletId();
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (cngModel != null) {
                amount = cngModel.getTotalAmt();
                mobileNumber = cngModel.getMobileNumber();
                vehNum = cngModel.getVehicleNumber();
                unitPrice = cngModel.getPerAmt();
                qty = cngModel.getQty();
                field1 = "Offline";
                productId = getProductId("CNG", fuelProductList);
            } else if (onlineTxnModel != null) {
                amount = onlineTxnModel.getAmount();
                mobileNumber = onlineTxnModel.getMobileNumber();
                vehNum = onlineTxnModel.getVehicleNumber();
                pumpNo = onlineTxnModel.getPumpNo();
                nozzleNo = onlineTxnModel.getNozzleNo();
                localMPDId = onlineTxnModel.getLocalMPDId();
//                productId = onlineTxnModel.getProductId();
                productId = getProductId(onlineTxnModel.getProductName(), fuelProductList);
                qty = onlineTxnModel.getQty();
                vehId = onlineTxnModel.getVehicleNumber();
                field1 = "Online";
                unitPrice = onlineTxnModel.getUnitPrice();
                isTxnOnline = onlineTxnModel.getIsTxnOnline();
            } else {
                amount = nfrModel.getAmt();
                mobileNumber = nfrModel.getMobileNumber();
                vehNum = nfrModel.getVehicleNumber();
                qty = "0";
                unitPrice = "0";
                field1 = "Offline";
                productId = getProductId("LUBES", fuelProductList);
            }
        }

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redirectToFailedPage();
            }
        });

        binding.entrotpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                otppass = String.valueOf(binding.editotp.getText());
                odometer = binding.odometerReading.getText().toString().trim();
                Log.d("odometerValue", odometer);
                if (otppass.isEmpty()) {
                    binding.editotp.setError("Enter OTP");
                    binding.editotp.requestFocus();
                } else {
                    validateOtpApi();
                }
            }
        });


        binding.resendotp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                virtualCardInitiateOTPAPI();
                Log.d("serrrrrt=", productId);

            }
        });


        return binding.getRoot();
    }

    private void virtualCardInitiateOTPAPI() {
        vartualcarinitiateotp = "initiateOtp";
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
        JSONArray billerTranList = new JSONArray();
        JSONObject billerTranItem = new JSONObject();
        JSONArray paramList = new JSONArray();
        try {
            jsonObject.put("userName", username);
            jsonObject.put("channel", "BPCL");
            jsonObject.put("tid", tid);
            jsonObject.put("mid", mid);
            jsonObject.put("reqDate", reqDate);
            jsonObject.put("reqTime", reqTime);
            jsonObject.put("txnType", "AVO");
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);

            billerTranItem.put("mid", mid);
            billerTranItem.put("tid", tid);
            billerTranItem.put("trans_status", "PENDING");
            billerTranItem.put("tran_amt", amount);
            billerTranItem.put("tran_date", reqDate);
            billerTranItem.put("tran_time", reqTime);
            billerTranItem.put("ft_number", txnId);
            billerTranItem.put("cust_id", username);
            billerTranItem.put("pay_method", "ALP");
            billerTranItem.put("authAmt", amount);
            billerTranItem.put("refundAmt", "0");
            billerTranItem.put("balanceAmt", amount);
            billerTranItem.put("field1", field1);

            billerTranList.put(billerTranItem);
            billerTranItem.put("paramList", paramList);

            paramList.put(createJsonObject(pumpNo, "PUMP_NO"));
            paramList.put(createJsonObject(localMPDId, "localMPDId"));
            paramList.put(createJsonObject(nozzleNo, "NOZZLE"));
            paramList.put(createJsonObject(productId, "localProductID"));
            paramList.put(createJsonObject(mobileNumber, "Customer Mobile"));
            paramList.put(createJsonObject(sapCode, "SAP CODE"));
            paramList.put(createJsonObject(programName, "ProgramId"));
            paramList.put(createJsonObject(walletId, "WalletId"));
            paramList.put(createJsonObject(odometer, "odometerReading"));
            paramList.put(createJsonObject(qty, "QUANTITY"));
            paramList.put(createJsonObject(roName, "MERCH NAME"));
            paramList.put(createJsonObject("", "Attendant ID"));
            paramList.put(createJsonObject(unitPrice, "UNIT_PRICE"));
            paramList.put(createJsonObject(vehNum, "Vehicle ID"));
            paramList.put(createJsonObject("0", "CUSTOMER_DISC"));
            paramList.put(createJsonObject(reqDate + reqTime, "FCC TIMESTAMP"));
            paramList.put(createJsonObject("", "discountID"));
            jsonObject.put("billerTranList", billerTranList);

            Log.d("ApiName", "AVO");
            Log.d("FileName", "EnterDetailsFragment");
            Log.d("ApiRequest", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void validateOtpApi() {
        requestCheck = "yes";
        vartualcarinitiateotp = "validateOtp";
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
        JSONArray billerTranListArray = new JSONArray();
        JSONObject billerTranListObject = new JSONObject();
        JSONArray paramListArray = new JSONArray();
        try {
            jsonObject.put("userName", username);
            jsonObject.put("channel", "BPCL");
            jsonObject.put("tid", tid);
            jsonObject.put("mid", mid);
            jsonObject.put("source", "Mobile");
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("txnType", "AVT");
            jsonObject.put("password", otppass);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "0");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);

            billerTranListObject.put("mid", mid);
            billerTranListObject.put("tid", tid);
            billerTranListObject.put("trans_status", "PENDING");
            billerTranListObject.put("tran_amt", amount);
            billerTranListObject.put("tran_date", requestDate());
            billerTranListObject.put("tran_time", requestTime());
            billerTranListObject.put("ft_number", ft_number);
            billerTranListObject.put("cust_id", cust_id);
            billerTranListObject.put("pay_method", "ALP");
            billerTranListObject.put("authAmt", amount);
            billerTranListObject.put("refundAmt", "0");
            billerTranListObject.put("balanceAmt", amount);
            billerTranListObject.put("field1", field1);
            billerTranListObject.put("field3", field3);

            billerTranListArray.put(billerTranListObject);
            billerTranListObject.put("paramList", paramListArray);

            paramListArray.put(createJsonObject(username, "Attendant ID"));
            paramListArray.put(createJsonObject("0", "PUMP_NO"));
            paramListArray.put(createJsonObject("0", "localMPDId"));
            paramListArray.put(createJsonObject("0", "NOZZLE"));
            paramListArray.put(createJsonObject(productId, "localProductID"));
            paramListArray.put(createJsonObject(mobileNumber, "Customer Mobile"));
            paramListArray.put(createJsonObject(sapCode, "SAP CODE"));
            paramListArray.put(createJsonObject(programName, "ProgramId"));
            paramListArray.put(createJsonObject(walletId, "WalletId"));
            paramListArray.put(createJsonObject(odometer, "odometerReading"));
            paramListArray.put(createJsonObject(qty, "QUANTITY"));//
            paramListArray.put(createJsonObject(roName, "MERCH NAME"));
            paramListArray.put(createJsonObject("", "Attendant ID"));
            paramListArray.put(createJsonObject(unitPrice, "UNIT_PRICE"));//
            paramListArray.put(createJsonObject(vehNum, "Vehicle ID"));
            paramListArray.put(createJsonObject("0", "CUSTOMER_DISC"));
            paramListArray.put(createJsonObject(dateTime, "FCC TIMESTAMP"));
            paramListArray.put(createJsonObject("", "discountID"));

            jsonObject.put("billerTranList", billerTranListArray);

            Log.d("ApiName", "Virtual Card Transaction API  : AVT");
            Log.d("Request", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void preAuthTransactionExistsAPI() {
        vartualcarinitiateotp = "preAuthTransactionExistsAPI";
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
            jsonObject.put("txnType", "APF");
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnId", txnId);
            jsonObject.put("mobNo", mobileNumber);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);

            Log.d("Request", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void postAuthInitiateOtpApi(String mobileNumber) {
        vartualcarinitiateotp = "postAuthInitiateOtpApi";
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while fetching details...");
        progress.setCancelable(false);
        progress.show();
        String url = "alpreq";
        String reqDate = requestDate();
        String reqTime = requestTime();
        String transDate = cashNotificationDate();
        Log.d("txnIdsss", "txnIdssssss: "+txnId);


        txnId = Helper.createTxnIdForOfflineTxn();
        JSONObject jsonObject = new JSONObject();
        JSONArray billerTranList = new JSONArray();
        JSONObject billerTranItem = new JSONObject();
        JSONArray paramList = new JSONArray();
        try {
            Log.d("txnIdsss", "txnIdssssss11111: "+txnId);
            jsonObject.put("userName", username);
            jsonObject.put("channel", "BPCL");
            jsonObject.put("tid", tid);
            jsonObject.put("mid", mid);
            jsonObject.put("reqDate", reqDate);
            jsonObject.put("reqTime", reqTime);
            jsonObject.put("txnType", "AEO");
//            jsonObject.put("tranChannel", "VC");
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);

            billerTranItem.put("mid", mid);
            billerTranItem.put("tid", tid);
            billerTranItem.put("trans_status", "PENDING");
            billerTranItem.put("tran_amt", amount);
            billerTranItem.put("tran_date", transDate);
            billerTranItem.put("tran_time", reqTime);
            billerTranItem.put("ft_number", txnId);
            billerTranItem.put("cust_id", username);
            billerTranItem.put("pay_method", "ALP");
            billerTranItem.put("authAmt", amount);
            billerTranItem.put("refundAmt", "0");
            billerTranItem.put("balanceAmt", amount);
            billerTranItem.put("field1", field1);
            billerTranList.put(billerTranItem);
            billerTranItem.put("paramList", paramList);

            paramList.put(createJsonObject(pumpNo, "PUMP_NO"));
            paramList.put(createJsonObject(localMPDId, "localMPDId"));
            paramList.put(createJsonObject(nozzleNo, "NOZZLE"));
            paramList.put(createJsonObject(productId, "localProductID"));
            paramList.put(createJsonObject(preauthID, "preAuthId"));
            paramList.put(createJsonObject(mobileNumber, "Customer Mobile"));
            paramList.put(createJsonObject(sapCode, "SAP CODE"));
            paramList.put(createJsonObject("SmartDrive", "ProgramId"));
            paramList.put(createJsonObject("", "WalletId"));
            paramList.put(createJsonObject("", "odometerReading"));
            paramList.put(createJsonObject(qty, "QUANTITY"));
            paramList.put(createJsonObject(roName, "MERCH NAME"));
            paramList.put(createJsonObject("", "Attendant ID"));
            paramList.put(createJsonObject(unitPrice, "UNIT_PRICE"));
            paramList.put(createJsonObject(vehId, "Vehicle ID"));
            paramList.put(createJsonObject("", "CUSTOMER_DISC"));
            paramList.put(createJsonObject(reqDate + reqTime, "FCC TIMESTAMP"));
            paramList.put(createJsonObject("", "discountID"));

            jsonObject.put("billerTranList", billerTranList);

            Log.d("ApiName", "Check Transaction Status : ATS");
            Log.d("Request", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void vCardTransactionStatusAPI() {
        vartualcarinitiateotp = "vCardTransactionStatusAPI";
        String url = "alpreq";
        String reqDate = requestDate();
        String reqTime = requestTime();
        JSONObject jsonObject = new JSONObject();
        JSONArray billerTranList = new JSONArray();
        JSONObject billerTranItem = new JSONObject();
        JSONArray paramList = new JSONArray();
        try {
            jsonObject.put("userName", username);
            jsonObject.put("channel", "BPCL");
            jsonObject.put("tid", tid);
            jsonObject.put("mid", mid);
            jsonObject.put("reqDate", reqDate);
            jsonObject.put("reqTime", reqTime);
            jsonObject.put("txnType", "ATS");
            jsonObject.put("tranChannel", "VC");
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);

            billerTranItem.put("mid", mid);
            billerTranItem.put("tid", tid);
            billerTranItem.put("trans_status", "PENDING");
            billerTranItem.put("tran_amt", amount);
            billerTranItem.put("tran_date", tran_date);
            billerTranItem.put("tran_time", tran_time);
            billerTranItem.put("ft_number", ft_number);
            billerTranItem.put("cust_id", cust_id);
            billerTranItem.put("pay_method", "ALP");
            billerTranItem.put("authAmt", amount);
            billerTranItem.put("refundAmt", "0");
            billerTranItem.put("balanceAmt", amount);
            billerTranItem.put("field1", field1);
            billerTranItem.put("field3", field3);
            billerTranList.put(billerTranItem);
            billerTranItem.put("paramList", paramList);

            paramList.put(createJsonObject(pumpNo, "PUMP_NO"));
            paramList.put(createJsonObject(localMPDId, "localMPDId"));
            paramList.put(createJsonObject(nozzleNo, "NOZZLE"));
            paramList.put(createJsonObject(localMPDId, "localProductID"));
            paramList.put(createJsonObject(mobileNumber, "Customer Mobile"));
            paramList.put(createJsonObject(sapCode, "SAP CODE"));
            paramList.put(createJsonObject("SmartDrive", "ProgramId"));
            paramList.put(createJsonObject("", "WalletId"));
            paramList.put(createJsonObject("", "odometerReading"));
            paramList.put(createJsonObject(qty, "QUANTITY"));
            paramList.put(createJsonObject(roName, "MERCH NAME"));
            paramList.put(createJsonObject("", "Attendant ID"));
            paramList.put(createJsonObject(unitPrice, "UNIT_PRICE"));
            paramList.put(createJsonObject(vehId, "Vehicle ID"));
            paramList.put(createJsonObject("", "CUSTOMER_DISC"));
            paramList.put(createJsonObject(reqDate + reqTime, "FCC TIMESTAMP"));
            paramList.put(createJsonObject("", "discountID"));

            jsonObject.put("billerTranList", billerTranList);

            Log.d("ApiName", "Check Transaction Status : ATS");
            Log.d("Request", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void apiResult(String res, String apiName) {
        if (vartualcarinitiateotp.equals("initiateOtp")) {
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0, null, null);

                    });
                } else {
                    Log.d("initiateOtpResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");

                    JSONArray outputArray = payLoad.getJSONArray("output");
                    JSONObject outputArrayJSONObject = outputArray.getJSONObject(0);
                    String successmsg = outputArrayJSONObject.getString("message");


                    if (respCode.equals("200")) {
                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            binding.textmsg.setText(successmsg);
                            binding.textmsg.setVisibility(View.VISIBLE);
                            binding.resendotp.setVisibility(View.GONE);

                            countDownTimer = new CountDownTimer(45000, 1000) {
                                public void onTick(long millisUntilFinished) {
                                    binding.timerTxt.setText(millisUntilFinished / 1000 + " seconds remaining");
                                }

                                public void onFinish() {
                                    binding.timerTxt.setText("");
                                    getActivity().runOnUiThread(() -> {
                                        if(requestCheck.equals("no")){
                                            redirectToTimeOutPage();
                                        }else{
                                            Log.d("requestCheck", "onFinish: "+requestCheck);
                                        }
                                    });
                                }
                            }.start();

                        });

                        JSONArray billerTranList = payLoad.getJSONArray("billerTranList");

                        JSONObject transaction = billerTranList.getJSONObject(0);
                        ft_number = transaction.getString("ft_number");
                        field3 = transaction.getString("field3");
                        cust_id = transaction.getString("cust_id");
//                        String trans_status = transaction.getString("trans_status");
                        Log.d("field3_statuss = ", field3);

                    } else {
                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            binding.textmsg.setText(respDesc);
                            MessagesDialog.showDialog(getContext(), respDesc, 0, null, null);
                        });
                    }
                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "virtualCardInitiateOTPAPI", e.toString());
                getActivity().runOnUiThread(() -> {
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    MessagesDialog.showDialog(getContext(), e.toString(), 0, null, null);

                    e.printStackTrace();
                });
            }
        } else if (vartualcarinitiateotp.equals("validateOtp")) {
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0, null, null);
                    });
                } else {
                    Log.d("validateOtpRes = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");

                    if (respCode.equals("200")) {
                        JSONArray billerTranList = payLoad.getJSONArray("billerTranList");
                        JSONObject transaction = billerTranList.getJSONObject(0);

                        String trans_status = transaction.getString("trans_status");

                        tran_date = transaction.getString("tran_date");
                        tran_time = transaction.getString("tran_time");

                        ft_number = transaction.getString("ft_number");
                        authAmt = transaction.getString("authAmt");
                        balanceAmt = transaction.getString("balanceAmt");

                        if (transaction.has("field6")) {
                            field6 = transaction.getString("field6");
                        }
                        if (transaction.has("field9")) {
                            field9 = transaction.getString("field9");
                        }

                        if (transaction.has("field7")) {
                            field7 = transaction.getString("field7");
                        }

                        String rrn = transaction.getString("rrn");

                        if (cngModel != null) {
                            cngModel.setRrn(rrn);
                            cngModel.setField3(field3);
                            cngModel.setField7(field7);
                            cngModel.setField9(field9);
                            cngModel.setField13("VC");

                        } else if (nfrModel != null) {
                            nfrModel.setRrn(rrn);
                            nfrModel.setField3(field3);
                            nfrModel.setField7(field7);
                            nfrModel.setField9(field9);
                            nfrModel.setField13("VC");
                        } else {
                            onlineTxnModel.setRrn(rrn);
                            onlineTxnModel.setField3(field3);
                            onlineTxnModel.setField7(field7);
                            onlineTxnModel.setField9(field9);
                            onlineTxnModel.setField13("VC");
                        }

                        if (programName != null && programName.equals("SmartFleet") || programName != null && programName.equals("PetroCorp")) {
                            vCardTransactionStatusAPI();
                            Log.d("petroPoints1", "NumberFormatException: " + programName);

                        } else {
                            if (trans_status.equals("failure") || trans_status.equals("SUCCESS")) {
                                vCardTransactionStatusAPI();
                                Log.d("petroPoints22", "NumberFormatException: " + trans_status);
                                Log.d("petroPoints2", "NumberFormatException: " + programName);

                            } else {
                                try {
                                    int petroPoints = Integer.parseInt(field9);
                                    double petroBalance = Double.parseDouble(field7);
                                    if (petroPoints > 0 || petroBalance > 0.0) {
                                        Log.d("petroPoints3", "NumberFormatException: " + petroPoints);
                                        getActivity().runOnUiThread(() -> {
                                            progress.dismiss();
                                            Bundle bundle = new Bundle();
                                            bundle.putString("field9", field9);
                                            bundle.putString("Drive", programName);
                                            bundle.putString("ft_number", ft_number);
                                            bundle.putString("field3", field3);
                                            bundle.putString("field6", field6);
                                            Log.d("cehcking", "field6 value: " + field6);
                                            Log.d("cehckingfield7", "field7 value: " + field7);
                                            bundle.putString("field7", field7);
                                            bundle.putString("cust_id", cust_id);
                                            bundle.putString("authAmt", authAmt);
                                            bundle.putString("balanceAmt", balanceAmt);
                                            bundle.putParcelable("cngModel", cngModel);
                                            bundle.putParcelable("nfrModel", nfrModel);
                                            bundle.putParcelable("onlineTxnModel", onlineTxnModel);
                                            bundle.putSerializable("virtualProgramList", (ArrayList<VirtualCardProgramModel>) virtualCardProgramModelArrayList);
                                            ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, new OtpFragment());
                                            Log.d("field9d", field9);
                                        });
                                    } else {
                                        Log.d("petroPoints4", "NumberFormatException: " + petroPoints);
                                        vCardTransactionStatusAPI();
                                    }
                                } catch (NumberFormatException e) {
                                    Log.d("petroPoints5", "NumberFormatException: " + e);
                                }
                            }
                        }
                    } else if (respCode.equals("457")) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                progress.dismiss();
                                preAuthTransactionExistsAPI();
                            });
                        }
                    } else {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                progress.dismiss();
                                binding.editotp.setText("");
                                Log.d("validateOtException = ", res);

                                attempt++;

                                if (attempt > 4) {
                                    Intent intent = new Intent();
                                    Log.d("TAG", "apiResult: " + attempt);
                                    attempt = 0;
                                    if (cngModel != null) {
                                        intent = new Intent(getActivity(), CngPaymentActivity.class);
                                        intent.putExtra("cngModel", cngModel);
                                        intent.putExtra("Insertcard", "Insertcard");
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                        Log.d("TAG", "cngModel: hhhhhheyy");

                                    } else if (onlineTxnModel != null) {
                                        if (isTxnOnline.equals("no")) {
                                            intent = new Intent(getActivity(), PaymentActivity.class);
                                            intent.putExtra("onlineTxnModel", onlineTxnModel);
                                            intent.putExtra("isTxnOnline", "isTxnOnline");
                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                        } else {
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
                                    MessagesDialog.showDialog(getContext(), respDesc, 0, intent, null);
                                } else {
                                    MessagesDialog.showDialog(getContext(), respDesc, 0, null, null);
                                }

                            });
                        }
                    }

                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "validateOtpApi", e.toString());
                getActivity().runOnUiThread(() -> {
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    Log.d("validateOtpException = ", e.toString());
                    binding.editotp.setText("");
                    MessagesDialog.showDialog(getContext(), e.toString(), 0, null, null);
                });
            }
        } else if (vartualcarinitiateotp.equals("vCardTransactionStatusAPI")) {
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0, null, null);

                    });
                } else {
                    Log.d("vCardTRES = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");

                    if (respCode.equals("200")) {
                        JSONArray billerTranList = payLoad.getJSONArray("billerTranList");
                        JSONObject transaction = billerTranList.getJSONObject(0);
                        JSONArray outputArray = payLoad.getJSONArray("output");
                        JSONObject outputArrayJSONObject = outputArray.getJSONObject(0);
                        String ROName = outputArrayJSONObject.getString("ROName");
                        String roCity = outputArrayJSONObject.getString("roCity");
                        String roMobileNo = outputArrayJSONObject.getString("roMobileNo");
                        String alpTid = outputArrayJSONObject.getString("aposTerminalID");
                        String alpTxnId = outputArrayJSONObject.getString("alpTransactionId");
                        Log.d("alpTxnId", "apiResult: " + alpTxnId);
                        String alpSlipNo = outputArrayJSONObject.getString("chargeSlipNumber");
                        String alpReportId = outputArrayJSONObject.getString("reportID");
                        String alpType = outputArrayJSONObject.getString("txnType");
                        String alpTxnSource = outputArrayJSONObject.getString("txnSource");
                        String alpCustName = outputArrayJSONObject.getString("customerName");
                        String alpAccNo = outputArrayJSONObject.getString("customerAccountNumber");
                        String alpCardId = outputArrayJSONObject.getString("customerCardNumber");
                        String alpVechCard = "";
                        String alpOdometer = outputArrayJSONObject.getString("odometerReading");
                        String alpWallet = outputArrayJSONObject.getString("txnMode");
                        String alpProduct = outputArrayJSONObject.getString("txnProduct");
                        String alpRate = outputArrayJSONObject.getString("productRate");
                        String alpVol = "";
                        String alpFuelAmount = outputArrayJSONObject.getString("fuelAmount");
                        String alpTcsAmount = outputArrayJSONObject.getString("tcsAmount");
                        String alpTxnAmount = outputArrayJSONObject.getString("txnAmount");
                        String alpPmEarn = outputArrayJSONObject.getString("petroMilesEarned");
                        String alpMeShare = outputArrayJSONObject.getString("txnMEShare");
                        String alpCardBalance = outputArrayJSONObject.getString("cardBalance");
                        if (outputArrayJSONObject.has("vehicleNumber")) {
                            alpVehicleNumber = outputArrayJSONObject.getString("vehicleNumber");
                        }
                        if (cngModel != null) {
                            cngModel.setROName(ROName);
                            cngModel.setRoCity(roCity);
                            cngModel.setRoMobileNo(roMobileNo);
                            cngModel.setVehicleNumber(alpVehicleNumber);
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
                            nfrModel.setVehicleNumber(alpVehicleNumber);
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
                            onlineTxnModel.setVehicleNumber(alpVehicleNumber);
                            onlineTxnModel.setAlpTid(alpTid);
                            onlineTxnModel.setAlpTxnId(alpTxnId);
                            onlineTxnModel.setField3(field3);
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
                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            redirectToSuccessPage();
//                            if (programName != null && programName.equals("SmartFleet") || programName != null && programName.equals("PetroCorp")) {
//                                if (cngModel != null) {
//                                    Log.d("cngModellllllllll", "");
//                                    Intent intent = new Intent(ctx, CngSuccessActivity.class);
//                                    intent.putExtra("cngModel", cngModel);
//                                    startActivity(intent);
//                                } else if (nfrModel != null) {
//                                    Log.d("nfrModelllllllllll", "");
//                                    Intent intent = new Intent(ctx, NfrSuccessActivity.class);
//                                    intent.putExtra("nfrModel", nfrModel);
//                                    startActivity(intent);
//                                } else {
//                                    Log.d("onlineTxnModellllll", "");
//                                    Intent intent = new Intent(ctx, SuccessActivity.class);
//                                    intent.putExtra("onlineTxnModel", onlineTxnModel);
//                                    startActivity(intent);
//                                }
//                            }
                        });

                    } else {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Log.d("petroPoints8 = ", res);

                                progress.dismiss();
                                MessagesDialog.showDialog(getContext(), respDesc, 0, null, null);
                            });
                        }
                    }

                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "validateOtpApi", e.toString());
                getActivity().runOnUiThread(() -> {
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    Log.d("petroPoints9 = ", res);

                    MessagesDialog.showDialog(getContext(), e.toString(), 0, null, null);


                });
            }
        } else if (vartualcarinitiateotp.equals("preAuthTransactionExistsAPI")) {
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0, null, null);
                    });
                } else {
                    Log.d("preAuthTransacRES = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");

                    if (respCode.equals("200")) {
//                        JSONArray billerTranList = payLoad.getJSONArray("billerTranList");
//                        JSONObject transaction = billerTranList.getJSONObject(0);
                        JSONArray outputArray = payLoad.getJSONArray("output");
                        JSONObject outputArrayJSONObject = outputArray.getJSONObject(0);

//                        String alpTransactionId = outputArrayJSONObject.getString("alpTransactionId");
//                        String preAuthAmount = outputArrayJSONObject.getString("preAuthAmount");
//                        String cardID = outputArrayJSONObject.getString("cardID");

                        if (outputArrayJSONObject.has("preauthID")) {
                            preauthID = outputArrayJSONObject.getString("preauthID");
                        }

                        getActivity().runOnUiThread(() -> {
                            if (progress.isShowing()) {
                                progress.dismiss();
                            }
                            postAuthInitiateOtpApi(mobileNumber);
                        });

                    } else {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Log.d("petroPoints8 = ", res);

                                if (progress.isShowing()) {
                                    progress.dismiss();
                                }
                                MessagesDialog.showDialog(getContext(), respDesc, 0, null, null);
                            });
                        }
                    }

                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "validateOtpApi", e.toString());
                getActivity().runOnUiThread(() -> {
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    Log.d("Exception = ", e.toString());
                    MessagesDialog.showDialog(getContext(), e.toString(), 0, null, null);
                });
            }
        }else if (vartualcarinitiateotp.equals("postAuthInitiateOtpApi")) {
            try {
                if (res.equals("Server Time Out")) {
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(requireContext(), "Server Time Out", 0, null, null);
                        }
                    });
                } else {
                    Log.d("postAuthInitiateResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respCode = payLoad.getString("respCode");
                    String respDesc = payLoad.getString("respDesc");

                    if (respCode.equals("200")) {
                        JSONArray outputArray = payLoad.getJSONArray("output");

                        if (outputArray.length() > 0) {
                            JSONObject outputObject = outputArray.getJSONObject(0);
                            message = outputObject.getString("message");
                        }

                        JSONArray billerTranListArray = payLoad.getJSONArray("billerTranList");
                        if (billerTranListArray.length() > 0) {
                            JSONObject transaction = billerTranListArray.getJSONObject(0);

                            tran_date = transaction.getString("tran_date");
                            tran_time = transaction.getString("tran_time");
                            ft_number = transaction.getString("ft_number");
                            field3 = transaction.getString("field3");
                            cust_id = transaction.getString("cust_id");

                            JSONArray paramListArray = transaction.getJSONArray("paramList");

                            for (int j = 0; j < paramListArray.length(); j++) {
                                JSONObject param = paramListArray.getJSONObject(j);
                                if ("Customer Mobile".equals(param.getString("param_lit"))) {
                                    mobileNumber = param.getString("param");
                                    Log.d("Custmoner", "Customer Mobile: " + mobileNumber);
                                    break;
                                }
                            }
                        }

                        reqDate = payLoad.getString("reqDate");
                        reqTime = payLoad.getString("reqTime");

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if (progress.isShowing()) {
                                    progress.dismiss();
                                }
                                Bundle bundle = new Bundle();
                                if (cngModel != null) {
                                    cngModel.setMobileNumber(mobileNumber);
                                } else if (nfrModel != null) {
                                    nfrModel.setMobileNumber(mobileNumber);
                                } else {
                                    onlineTxnModel.setMobileNumber(mobileNumber);
                                }
                                bundle.putString("preauthID", preauthID);
                                bundle.putString("tran_date", tran_date);
                                bundle.putString("tran_time", tran_time);
                                bundle.putString("ft_number", ft_number);
                                bundle.putString("field3", field3);
                                bundle.putString("cust_id", cust_id);
                                bundle.putString("reqDate", reqDate);
                                bundle.putString("reqTime", reqTime);
                                bundle.putString("msg",message);
                                bundle.putParcelable("cngModel", cngModel);
                                bundle.putParcelable("nfrModel", nfrModel);
                                bundle.putParcelable("onlineTxnModel", onlineTxnModel);
                                ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, new postAuthMobileNummberFragment());

                            }
                        });

                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (progress.isShowing()) {
                                    progress.dismiss();
                                }
                                MessagesDialog.showDialog(getActivity(), respDesc, 0, null, null);
                            }
                        });
                    }

                }
            } catch (JSONException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progress.isShowing()) {
                            progress.dismiss();
                        }
                        Log.d("respCode", e.toString());
                        fileWrite(ctx, todayDate + ".txt", "validateOtpApi", e.toString());
                        MessagesDialog.showDialog(getActivity(), e.toString(), 0, null, null);
                    }
                });
            }
        }
    }

    private void redirectToSuccessPage() {
        if (cngModel != null) {
            Log.d("cngModellllllllll", "");
            Intent intent = new Intent(ctx, CngSuccessActivity.class);
            intent.putExtra("cngModel", cngModel);
            startActivity(intent);
        } else if (nfrModel != null) {
            Log.d("nfrModelllllllllll", "");
            Intent intent = new Intent(ctx, NfrSuccessActivity.class);
            intent.putExtra("nfrModel", nfrModel);
            startActivity(intent);
        } else {
            Log.d("onlineTxnModellllll", "");
            Intent intent = new Intent(ctx, SuccessActivity.class);
            intent.putExtra("onlineTxnModel", onlineTxnModel);
            startActivity(intent);
        }

    }

    private void redirectToFailedPage() {
        if (cngModel != null) {
            TxnFailFragment fragment = new TxnFailFragment();
            Bundle bundle = new Bundle();
            bundle.putString("txnStatus", "Transaction Cancelled!");
            bundle.putParcelable("cngModel", cngModel);
            ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);

        } else if (onlineTxnModel != null) {
            TxnFailFragment fragment = new TxnFailFragment();
            Bundle bundle = new Bundle();
            bundle.putString("txnStatus", "Transaction Cancelled!");
            bundle.putParcelable("onlineTxnModel", onlineTxnModel);
            ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);

        } else if (nfrModel != null) {
            TxnFailFragment fragment = new TxnFailFragment();
            Bundle bundle = new Bundle();
            bundle.putString("txnStatus", "Transaction Cancelled!");
            bundle.putParcelable("nfrModel", nfrModel);
            ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);

        }
    }

    private void redirectToTimeOutPage() {
        if (cngModel != null) {
            TxnFailFragment fragment = new TxnFailFragment();
            Bundle bundle = new Bundle();
            bundle.putString("txnStatus", "Transaction TimeOut!");
            bundle.putParcelable("cngModel", cngModel);
            ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);

        } else if (onlineTxnModel != null) {
            TxnFailFragment fragment = new TxnFailFragment();
            Bundle bundle = new Bundle();
            bundle.putString("txnStatus", "Transaction TimeOut!");
            bundle.putParcelable("onlineTxnModel", onlineTxnModel);
            ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);

        } else if (nfrModel != null) {
            TxnFailFragment fragment = new TxnFailFragment();
            Bundle bundle = new Bundle();
            bundle.putString("txnStatus", "Transaction TimeOut!");
            bundle.putParcelable("nfrModel", nfrModel);
            ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);

        }
    }

    public void hideKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_generate_otp, null);
        TextView textView = dialogView.findViewById(R.id.text_messageotp);
        Button yesButton = dialogView.findViewById(R.id.button_yes);
        Button otpbutton_no = dialogView.findViewById(R.id.otpbutton_no);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        textView.setText("Do you want to generate \n OTP?");

        yesButton.setOnClickListener(v -> {
            dialog.dismiss();
            virtualCardInitiateOTPAPI();
        });

        otpbutton_no.setOnClickListener(v -> {
            String manualField3 = Helper.getClientTxnId();
            field3 = manualField3;
            if (cngModel != null) {
                cngModel.setField3(manualField3);
            } else if (onlineTxnModel != null) {
                onlineTxnModel.setField3(manualField3);
            } else if (nfrModel != null) {
                nfrModel.setField3(manualField3);
            }

            dialog.dismiss();
            countDownTimer = new CountDownTimer(45000, 1000) {
                public void onTick(long millisUntilFinished) {
                    binding.timerTxt.setText(millisUntilFinished / 1000 + " seconds remaining");
                }

                public void onFinish() {
                    binding.timerTxt.setText("");
                    getActivity().runOnUiThread(() -> {
                        if(requestCheck.equals("no")){
                            redirectToTimeOutPage();
                        }else{
                            Log.d("requestCheck", "onFinish: "+requestCheck);
                        }
                    });
                }
            }.start();
        });

        dialog.setCancelable(false);
        dialog.show();

    }

    @Override
    public void onPause() {
        super.onPause();
        MessagesDialog.dismissDialog();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}