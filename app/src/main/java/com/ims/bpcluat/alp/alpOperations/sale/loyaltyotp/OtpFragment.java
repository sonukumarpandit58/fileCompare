package com.ims.bpcluat.alp.alpOperations.sale.loyaltyotp;

import static com.ims.bpcluat.Helper.appVersion;
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
import static com.ims.bpcluat.utils.Navigation.BackWithData;

import android.annotation.SuppressLint;
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
import android.widget.Toast;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.SuccessActivity;
import com.ims.bpcluat.alp.alpOperations.utility.UtilityFragment;
import com.ims.bpcluat.cng.CngSuccessActivity;
import com.ims.bpcluat.databinding.FragmentOtpBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.model.AlpModels.SaleModel.VirtualCardProgramModel;
import com.ims.bpcluat.model.CngModel;
import com.ims.bpcluat.model.NfrModel;
import com.ims.bpcluat.model.OnlineTxnModel;
import com.ims.bpcluat.nfr.NfrSuccessActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OtpFragment extends Fragment implements ApiHelper.NetworkingApiCallBack {
    FragmentOtpBinding binding;
    ProgressDialog progress;
    Context context;
    ApiHelper api;
    String otppetromiles;
    String txnId = "", dateTime = "";
    String checkApiStatus = "";
    private CngModel cngModel;
    String amount = "", unitPrice = "", ft_number = "", cust_id = "", authAmt = "", balanceAmt = "";
    private OnlineTxnModel onlineTxnModel;
    private NfrModel nfrModel;
    String mobileNumber = "", rrn = "";
    String field9 = "", field1 = "", field3 = "", field6 = "", field7 = "";
    String moblieNum = "", pumpNo = "", nozzleNo = "", localMPDId = "", localProductID = "", qty = "", vehId = "", programid = "", walletid = "";
    private int selectedPosition = -1;
    private List<VirtualCardProgramModel> virtualCardProgramModelList;
    String tran_date = "",tran_time = "";
    private CountDownTimer countDownTimer;
    String alpVehicleNumber = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentOtpBinding.inflate(inflater, container, false);
        api = new ApiHelper();
        context = requireContext();
        hideKeyboard();

        if (getArguments() != null) {
            virtualCardProgramModelList = (ArrayList<VirtualCardProgramModel>) getArguments().getSerializable("virtualProgramList");
            programid = getArguments().getString("Drive");
            field9 = getArguments().getString("field9");
            ft_number = getArguments().getString("ft_number");
            cust_id = getArguments().getString("cust_id");
            field3 = getArguments().getString("field3");
           // field6 = getArguments().getString("field6");
            field7 = getArguments().getString("field7");
            balanceAmt = getArguments().getString("balanceAmt");
            authAmt = getArguments().getString("authAmt");

            cngModel = getArguments().getParcelable("cngModel");
            onlineTxnModel = getArguments().getParcelable("onlineTxnModel");
            nfrModel = getArguments().getParcelable("nfrModel");

            if (cngModel != null) {
                amount = cngModel.getTotalAmt();
                unitPrice = cngModel.getPerAmt();
                mobileNumber = cngModel.getMobileNumber();
                qty = cngModel.getQty();
                vehId = cngModel.getVehicleNumber();
                rrn = cngModel.getRrn();
                field1 = "Offline";
                localProductID = getProductId("CNG",fuelProductList);
            } else if (onlineTxnModel != null) {
                amount = onlineTxnModel.getAmount();
                mobileNumber = onlineTxnModel.getMobileNumber();
                pumpNo = onlineTxnModel.getPumpNo();
                nozzleNo = onlineTxnModel.getPumpNo();
                localMPDId = onlineTxnModel.getLocalMPDId();
                localProductID = getProductId(onlineTxnModel.getProductName(), fuelProductList);
                qty = onlineTxnModel.getQty();
                vehId = onlineTxnModel.getVehicleNumber();
                rrn = onlineTxnModel.getRrn();
                field1 = "Online";


            } else {
                amount = nfrModel.getAmt();
                mobileNumber = nfrModel.getMobileNumber();
               // qty = nfrModel.getQty();
                qty = "0";
                unitPrice = "0";
                vehId = nfrModel.getVehicleNumber();
                rrn = nfrModel.getRrn();
                field1 = "Offline";

                localProductID = getProductId("LUBES",fuelProductList);

            }

        }

        showDialog();

    /*    binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putInt("index", selectedPosition);
                bundle.putString("Drive", programid);
                bundle.putParcelable("cngModel", cngModel);
                bundle.putParcelable("nfrModel", nfrModel);
                bundle.putParcelable("onlineTxnModel", onlineTxnModel);
                bundle.putSerializable("virtualProgramList", (ArrayList<VirtualCardProgramModel>) virtualCardProgramModelList);
                ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, new EnterDetailsFragment());
                Log.d("sdsdsd", "Field9 value: " + field9);
            }
        });
*/

        binding.cancelbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BackWithData(getContext(), cngModel, nfrModel, onlineTxnModel);
            }
        });

        binding.submitmobnumbBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                otppetromiles = String.valueOf(binding.otppass.getText());
                if (otppetromiles.isEmpty()) {
                    binding.otppass.setError("Enter OTP");
                    binding.otppass.requestFocus();
                } else {
                    petromilesvalidateOtpApi(otppetromiles);
                }
            }
        });

        return binding.getRoot();
    }

    // Initiate otp API

    private void petromilesgenerateOtpApi() {
        checkApiStatus = "initiateOtp";
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
            jsonObject.put("txnType", "AVP");
            jsonObject.put("tranChannel", "VC");
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);
            JSONArray billerTranList = new JSONArray();
            JSONObject billerTranItem = new JSONObject();
            billerTranItem.put("mid", mid);
            billerTranItem.put("tid", tid);
            billerTranItem.put("trans_status", "PENDING");
            billerTranItem.put("tran_amt", amount);
            billerTranItem.put("tran_date", reqDate);
            billerTranItem.put("tran_time", reqTime);
            billerTranItem.put("ft_number", ft_number);
            billerTranItem.put("cust_id", cust_id);
            billerTranItem.put("pay_method", "ALP");
            billerTranItem.put("authAmt", authAmt);
            billerTranItem.put("refundAmt", "0");
            billerTranItem.put("balanceAmt", balanceAmt);
            billerTranItem.put("field1", field1);//
            billerTranItem.put("field3", field3);

            JSONArray paramList = new JSONArray();
            JSONObject paramItem;
            paramItem = new JSONObject();
            paramItem.put("param_lit", "PUMP_NO");
            paramItem.put("param", pumpNo);

            paramList.put(paramItem);
            paramItem = new JSONObject();
            paramItem.put("param_lit", "localMPDId");
            paramItem.put("param", localMPDId);

            paramList.put(paramItem);
            paramItem = new JSONObject();
            paramItem.put("param_lit", "NOZZLE");
            paramItem.put("param", nozzleNo);

            paramList.put(paramItem);
            paramItem = new JSONObject();
            paramItem.put("param_lit", "localProductID");
            paramItem.put("param", localProductID);

            paramList.put(paramItem);
            paramItem = new JSONObject();
            paramItem.put("param_lit", "Customer Mobile");
            paramItem.put("param", mobileNumber);

            paramList.put(paramItem);
            paramItem = new JSONObject();
            paramItem.put("param_lit", "SAP CODE");
            paramItem.put("param", sapCode);

            paramList.put(paramItem);
            paramItem = new JSONObject();
            paramItem.put("param_lit", "ProgramId");
            paramItem.put("param", programid);

            paramList.put(paramItem);
            paramItem = new JSONObject();
            paramItem.put("param_lit", "WalletId");
            paramItem.put("param", walletid);

            paramList.put(paramItem);
            paramItem = new JSONObject();
            paramItem.put("param_lit", "odometerReading");
            paramItem.put("param", "");

            paramList.put(paramItem);
            paramItem = new JSONObject();
            paramItem.put("param_lit", "QUANTITY");
            paramItem.put("param", qty);

            paramList.put(paramItem);
            paramItem = new JSONObject();
            paramItem.put("param_lit", "MERCH NAME");
            paramItem.put("param", Helper.roName);//

            paramList.put(paramItem);
            paramItem = new JSONObject();
            paramItem.put("param_lit", "Attendant ID");
            paramItem.put("param", "");

            paramList.put(paramItem);
            paramItem = new JSONObject();
            paramItem.put("param_lit", "UNIT_PRICE");
            paramItem.put("param", unitPrice);//

            paramList.put(paramItem);
            paramItem = new JSONObject();
            paramItem.put("param_lit", "Vehicle ID");
            paramItem.put("param", vehId);

            paramList.put(paramItem);
            paramItem = new JSONObject();
            paramItem.put("param_lit", "CUSTOMER_DISC");
            paramItem.put("param", "");

            paramList.put(paramItem);
            paramItem = new JSONObject();
            paramItem.put("param_lit", "FCC TIMESTAMP");
            paramItem.put("param", reqDate + reqTime);

            paramList.put(paramItem);
            paramItem = new JSONObject();
            paramItem.put("param_lit", "discountID");
            paramItem.put("param", "");

            paramList.put(paramItem);
            billerTranItem.put("paramList", paramList);
            billerTranList.put(billerTranItem);
            jsonObject.put("billerTranList", billerTranList);


            Log.d("getOtpRequest=", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void petromilesvalidateOtpApi(String otp) {
        checkApiStatus = "validateOtp";
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
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
            jsonObject.put("channel", "BPCL");
            jsonObject.put("reqDate", reqDate);
            jsonObject.put("reqTime", reqTime);
            jsonObject.put("userName", username);
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("txnType", "AVS");
            jsonObject.put("password", otp);
            jsonObject.put("tranChannel", "VC");
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);

            billerTranListObject.put("mid", mid);
            billerTranListObject.put("tid", tid);
            billerTranListObject.put("trans_status", "PENDING");
            billerTranListObject.put("tran_amt", amount);
            billerTranListObject.put("tran_date", reqDate);
            billerTranListObject.put("tran_time", reqTime);
            billerTranListObject.put("ft_number", ft_number);
            billerTranListObject.put("cust_id", cust_id);
            billerTranListObject.put("pay_method", "ALP");
            billerTranListObject.put("authAmt", amount);
            billerTranListObject.put("refundAmt", "0");
            billerTranListObject.put("balanceAmt", amount);
            billerTranListObject.put("field1", field1);//
            billerTranListObject.put("field3", field3);
            billerTranListObject.put("field6", field6);
            billerTranListObject.put("field7", field7);
            billerTranListObject.put("field9", field9);

            billerTranListArray.put(billerTranListObject);
            billerTranListObject.put("paramList", paramListArray);

            paramListArray.put(createJsonObject(pumpNo, "PUMP_NO"));
            paramListArray.put(createJsonObject(localMPDId, "localMPDId"));
            paramListArray.put(createJsonObject(nozzleNo, "NOZZLE"));
            paramListArray.put(createJsonObject(localProductID, "productId"));
            paramListArray.put(createJsonObject(sapCode, "SAP CODE"));
            paramListArray.put(createJsonObject(mobileNumber, "Customer Mobile"));;
            paramListArray.put(createJsonObject(programid, "ProgramId"));
            paramListArray.put(createJsonObject(walletid, "WalletId"));
            paramListArray.put(createJsonObject(unitPrice, "UNIT_PRICE"));
            paramListArray.put(createJsonObject(qty, "QUANTITY"));
            paramListArray.put(createJsonObject("", "odometerReading"));
            paramListArray.put(createJsonObject(vehId, "Vehicle ID"));
            paramListArray.put(createJsonObject("", "CUSTOMER_DISC"));
            paramListArray.put(createJsonObject("", "discountID"));
            paramListArray.put(createJsonObject(reqDate + reqTime, "FCC TIMESTAMP"));

            jsonObject.put("billerTranList", billerTranListArray);

            Log.d("getOtpRequest=", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void vCardTransactionStatusAPI() {
        checkApiStatus = "vCardTransactionStatusAPI";
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

            Log.d("getOtpRequest=", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void apiResult(String res, String apiName) {
        if (checkApiStatus.equals("initiateOtp")) {
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);

                    });
                } else {
                    Log.d("validateOtpResponse = ", res);
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
                            binding.textpetromilemsg.setText(successmsg);

                            countDownTimer = new CountDownTimer(45000, 1000) {
                                public void onTick(long millisUntilFinished) {
                                    binding.timerTxt.setText(millisUntilFinished / 1000 + " seconds remaining");
                                }
                                public void onFinish() {
                                    binding.timerTxt.setText("Time's up!");
                                    getActivity().runOnUiThread(() -> {
                                        BackWithData(getContext(), cngModel, nfrModel, onlineTxnModel);
                                    });
                                }
                            }.start();
                        });

                    } else {
                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            binding.textpetromilemsg.setText(respDesc);
                            binding.submitmobnumbBtn.setEnabled(false);
                            MessagesDialog.showDialog(context, respDesc, 0,null, null);
                        });
                    }
                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "petromilesgenerateOtpApi", e.toString());
                getActivity().runOnUiThread(() -> {
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    MessagesDialog.showDialog(context, e.toString(), 0,null, null);
                });
            }
        } else if (checkApiStatus.equals("validateOtp")) {
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                    });
                } else {
                    Log.d("validateOtpResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");

                    if (respCode.equals("200")) {

                        JSONArray billerTranList = payLoad.getJSONArray("billerTranList");
                        JSONObject transaction = billerTranList.getJSONObject(0);
                         tran_date = transaction.getString("tran_date");
                         tran_time = transaction.getString("tran_time");
                        String trans_status = transaction.getString("trans_status");
                        Log.d("trans_statuseeer", trans_status);

                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            if(!trans_status.equals("PENDING")){
                                vCardTransactionStatusAPI();
                            }else {
                                MessagesDialog.showDialog(context, "Your transaction is currently " +trans_status, 0,null, null);
                            }
                        });

                        progress.dismiss();

                    } else {
                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            binding.otppass.setText("");
                            MessagesDialog.showDialog(context, respDesc, 0,null, null);
                        });
                    }
                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "petromilesvalidateOtpApi", e.toString());
                getActivity().runOnUiThread(() -> {
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    binding.otppass.setText("");

                    MessagesDialog.showDialog(context, e.toString(), 0,null, null);
                });
            }
        }else if (checkApiStatus.equals("vCardTransactionStatusAPI")) {
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                    });
                } else {
                    Log.d("validateOtpResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");

                    if (respCode.equals("200")) {
                        JSONArray outputArray = payLoad.getJSONArray("output");
                        JSONObject outputArrayJSONObject = outputArray.getJSONObject(0);
                        String ROName = outputArrayJSONObject.getString("ROName");
                        String roCity = outputArrayJSONObject.getString("roCity");
                        String roMobileNo = outputArrayJSONObject.getString("roMobileNo");
                        String alpTid = outputArrayJSONObject.getString("aposTerminalID");
                        String alpTxnId = outputArrayJSONObject.getString("alpTransactionId");
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
                        if(outputArrayJSONObject.has("vehicleNumber")){
                            alpVehicleNumber = outputArrayJSONObject.getString("vehicleNumber");
                        }

                        if (cngModel != null) {
                            cngModel.setROName(ROName);
                            cngModel.setRoCity(roCity);
                            cngModel.setRoMobileNo(roMobileNo);
                            cngModel.setField3(field3);
                            cngModel.setField7(field7);
                            cngModel.setField9(field9);
                            cngModel.setField13("VC");
                            cngModel.setVehicleNumber(alpVehicleNumber);
                            cngModel.setRrn(rrn);
                            cngModel.setAlpTid(alpTid);
                            cngModel.setAlpSlipNo(alpSlipNo);
                            cngModel.setAlpTxnId(alpTxnId);
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
                            nfrModel.setField13("VC");
                            nfrModel.setVehicleNumber(alpVehicleNumber);
                            nfrModel.setRrn(rrn);
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
                            onlineTxnModel.setField13("VC");
                            onlineTxnModel.setVehicleNumber(alpVehicleNumber);
                            onlineTxnModel.setRrn(rrn);
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


                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            redirectToSuccessPage();
                        });

                        progress.dismiss();

                    } else {
                        getActivity().runOnUiThread(() -> {
                            Log.d("TAG", "respCode: "+ respCode);

                            progress.dismiss();
                            MessagesDialog.showDialog(context, respDesc, 0,null, null);
                        });
                    }
                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "petromilesvalidateOtpApi", e.toString());
                getActivity().runOnUiThread(() -> {
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    Log.d("TAG", "JSONException: "+ e);
                    MessagesDialog.showDialog(context, e.toString(), 0,null, null);

                });
            }
        }
    }

    private void redirectToSuccessPage() {
        if (cngModel != null) {
            Log.d("cngModellllllllll", "");
            Intent intent = new Intent(context, CngSuccessActivity.class);
            intent.putExtra("cngModel", cngModel);
            startActivity(intent);
        } else if (nfrModel != null) {
            Log.d("nfrModelllllllllll", "");
            Intent intent = new Intent(context, NfrSuccessActivity.class);
            intent.putExtra("nfrModel", nfrModel);
            startActivity(intent);
        } else {
            Log.d("onlineTxnModellllll", "");
            Intent intent = new Intent(context, SuccessActivity.class);
            intent.putExtra("onlineTxnModel", onlineTxnModel);
            startActivity(intent);
        }

    }

    @SuppressLint("SetTextI18n")
    private void showDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_generate_otp, null);
        TextView textView = dialogView.findViewById(R.id.text_messageotp);
        Button yesButton = dialogView.findViewById(R.id.button_yes);
        Button otpbutton_no = dialogView.findViewById(R.id.otpbutton_no);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        textView.setText("Do you want to redeem\nPetromiles? Your current Petromile Points are " + field9);

        yesButton.setOnClickListener(v -> {
            dialog.dismiss();
            petromilesgenerateOtpApi();
            field6= "true";
        });

        otpbutton_no.setOnClickListener(v -> {
            dialog.dismiss();
//            petromilesgenerateOtpApi();
//            field6= "false";
            petromilesvalidateOtpApi("");
        });

        dialog.setCancelable(false);
        dialog.show();

    }

    public void hideKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
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