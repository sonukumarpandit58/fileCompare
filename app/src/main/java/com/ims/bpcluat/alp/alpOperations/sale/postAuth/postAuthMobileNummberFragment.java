package com.ims.bpcluat.alp.alpOperations.sale.postAuth;

import static android.content.Context.MODE_PRIVATE;
import static com.ims.bpcluat.Helper.appVersion;
import static com.ims.bpcluat.Helper.cashNotificationDate;
import static com.ims.bpcluat.Helper.createJsonObject;
import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.fuelProductList;
import static com.ims.bpcluat.Helper.getCurrentDateTime;
import static com.ims.bpcluat.Helper.getProductId;
import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.roName;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.Helper.username;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.SuccessActivity;
import com.ims.bpcluat.alp.alpOperations.sale.SaleFragment;
import com.ims.bpcluat.alp.alpOperations.sale.TxnFailFragment;
import com.ims.bpcluat.cng.CngSuccessActivity;
import com.ims.bpcluat.databinding.FragmentPostAuthMobileNummberBinding;
import com.ims.bpcluat.databinding.FragmentPreAuthMobileNumberBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.model.CngModel;
import com.ims.bpcluat.model.NfrModel;
import com.ims.bpcluat.model.OnlineTxnModel;
import com.ims.bpcluat.nfr.NfrSuccessActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class postAuthMobileNummberFragment extends Fragment implements ApiHelper.NetworkingApiCallBack {

    FragmentPostAuthMobileNummberBinding binding;
    Context context;
    ApiHelper api;
    boolean otpSent = true;
    private CngModel cngModel;
    private OnlineTxnModel onlineTxnModel;
    private NfrModel nfrModel;
    ProgressDialog progress;
    String checkResult = "";
    String message = "", mobileNumber = "";

    String txnId = "", dateTime = "", isTxnOnline = "";
    String pumpNo = "", nozzleNo = "", localMPDId = "", localProductID = "", qty = "", vehId = "", unitAmt = "", field1 = "",field3 ="",field7 ="",field9 ="", rrn = "";
    String amount = "", moblieNum = "";
    String amountInPaise = "", clientTxnId = "", fcctxnID = "";
    String preauthID = "";
    String reqDate = "", reqTime = "", tran_date = "", tran_time = "";
    String ft_number = "", cust_id = "", balanceAmt = "", authAmt = "";
    private CountDownTimer countDownTimer;
    String requestCheck = "no";

    public postAuthMobileNummberFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPostAuthMobileNummberBinding.inflate(inflater, container, false);
        context = getActivity();
        api = new ApiHelper();
        hideKeyboard();

        if (getArguments() != null) {
            preauthID =  getArguments().getString("preauthID");
            tran_date =  getArguments().getString("tran_date");
            tran_time =  getArguments().getString("tran_time");
            ft_number =  getArguments().getString("ft_number");
            field3 =  getArguments().getString("field3");
            cust_id =  getArguments().getString("cust_id");
            reqDate =  getArguments().getString("reqDate");
            reqTime =  getArguments().getString("reqTime");
            cngModel = getArguments().getParcelable("cngModel");
            onlineTxnModel = getArguments().getParcelable("onlineTxnModel");
            nfrModel = getArguments().getParcelable("nfrModel");
            message = getArguments().getString("msg");
            binding.msg.setText(message);

            if (cngModel != null) {
                moblieNum = cngModel.getMobileNumber();
                amount = cngModel.getTotalAmt();
                unitAmt = cngModel.getPerAmt();
                txnId = cngModel.getTxnId();
                qty = cngModel.getQty();
                vehId = cngModel.getVehicleNumber();
                field1 = "Offline";
                localProductID = getProductId("CNG", fuelProductList);
                fcctxnID = "";
            } else if (onlineTxnModel != null) {
                moblieNum = onlineTxnModel.getMobileNumber();
                amount = onlineTxnModel.getAmount();
                unitAmt = onlineTxnModel.getUnitPrice();
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

            } else if(nfrModel != null){
                moblieNum = nfrModel.getMobileNumber();
                amount = nfrModel.getAmt();
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

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redirectToFailedPage();
            }
        });

        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateFields();
            }
        });

        return binding.getRoot();
    }

    private void validateFields() {
        String otp = binding.otp.getText().toString().trim();
            if (TextUtils.isEmpty(otp)) {
                binding.otp.setError("Please enter OTP");
                binding.otp.requestFocus();
                return;
            }
            postAuthValidateOtpApi(otp);
    }

    private void postAuthValidateOtpApi(String otp) {
        requestCheck = "yes";
        checkResult = "postAuthValidateOtpApi";
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while fetching details...");
        progress.setCancelable(false);
        progress.show();
        String url = "alpreq";
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
            jsonObject.put("txnType", "ACT");
            jsonObject.put("password", otp);
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
            billerTranItem.put("tran_date", tran_date);
            billerTranItem.put("tran_time", tran_time);
            billerTranItem.put("ft_number", ft_number);
            billerTranItem.put("cust_id", cust_id);
            billerTranItem.put("pay_method", "ALP");
            billerTranItem.put("authAmt", amount);
            billerTranItem.put("refundAmt", "0");
            billerTranItem.put("balanceAmt", amount);
            billerTranItem.put("field1", "Offline");
            billerTranItem.put("field3", field3);
            billerTranList.put(billerTranItem);
            billerTranItem.put("paramList", paramList);

            paramList.put(createJsonObject(pumpNo, "PUMP_NO"));
            paramList.put(createJsonObject(localMPDId, "localMPDId"));
            paramList.put(createJsonObject(nozzleNo, "NOZZLE"));
            paramList.put(createJsonObject(localProductID, "localProductID"));
            paramList.put(createJsonObject(preauthID, "preAuthId"));
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

            Log.d("ApiName", "Check Transaction Status : ATS");
            Log.d("Request", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void apiResult(String res, String apiName) {
         if (checkResult.equals("postAuthValidateOtpApi")) {
            try {
                if (res.equals("Server Time Out")) {
                    progress.dismiss();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(requireContext(), "Server Time Out", 0, null, null);
                        }
                    });
                } else {
                    Log.d("postAuthValidateResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respCode = payLoad.getString("respCode");
                    String respDesc = payLoad.getString("respDesc");

                    if (respCode.equals("200")) {
                        progress.dismiss();
                        Log.d("validateOtpApiData", respCode);

                        JSONArray outputArray = payLoad.getJSONArray("output");

                        if (outputArray.length() > 0) {
                            JSONObject outputObject = outputArray.getJSONObject(0);
                            String alpTransactionId = outputObject.optString("alpTransactionId");
                            String requiredPetromilesPoints = outputObject.optString("requiredPetromilesPoints");
                            String message = outputObject.optString("message");

                            JSONObject printObject = outputObject.getJSONObject("print");
                            String ROName = printObject.optString("ROName");
                            String roCity = printObject.optString("roCity");
                            String roMobileNo = printObject.optString("roMobileNo");
                            String mobileNumber = printObject.optString("mobileNumber");
                            String alpTid = printObject.optString("aposTerminalID");
                            String alpTxnId = printObject.optString("alpTransactionId");
                            Log.d("alpTxnId", "apiResult: " + alpTxnId);
                            String alpSlipNo = printObject.optString("chargeSlipNumber");
                            String alpReportId = printObject.optString("reportID");
                            String alpType = "PostAuth";
//                            alpType = printObject.optString("txnType");
                            String alpTxnSource = printObject.optString("txnSource");
                            String alpCustName = printObject.optString("customerName");
                            String alpAccNo = printObject.optString("customerAccountNumber");
                            String alpCardId = printObject.optString("customerCardNumber");
                            String alpVechCard = "";
                            String alpOdometer = printObject.optString("odometerReading");
                            String alpWallet = printObject.optString("txnMode");
                            String alpProduct = printObject.optString("txnProduct");
                            String alpRate = printObject.optString("productRate");
                            String alpVol = "";
                            String alpFuelAmount = printObject.optString("fuelAmount");
                            String alpTcsAmount = printObject.optString("tcsAmount");
                            String alpTxnAmount = printObject.optString("txnAmount");
                            String alpPmEarn = printObject.optString("petroMilesEarned");
                            String alpMeShare = printObject.optString("txnMEShare");
                            String alpCardBalance = printObject.optString("cardBalance");
                            String vehicleNumber = "";
                            if (printObject.has("vehicleNumber")) {
                                vehicleNumber = printObject.optString("vehicleNumber");
                            }

                            JSONArray billerTranList = payLoad.getJSONArray("billerTranList");
                            JSONObject transaction = billerTranList.getJSONObject(0);

                            String trans_status = transaction.optString("trans_status");

                            if (transaction.has("field3")) {
                                field3 = transaction.optString("field3");
                            }
                            if (transaction.has("field9")) {
                                field9 = transaction.optString("field9");
                            }

                            if (transaction.has("field7")) {
                                field7 = transaction.optString("field7");
                            }

                            String rrn = transaction.optString("rrn");

                            if (cngModel != null) {
                                cngModel.setRrn(rrn);
                                cngModel.setField3(field3);
                                cngModel.setField7(field7);
                                cngModel.setField9(field9);
                                cngModel.setField13("VC");

                                cngModel.setROName(ROName);
                                cngModel.setRoCity(roCity);
                                cngModel.setRoMobileNo(roMobileNo);
                                cngModel.setVehicleNumber(vehicleNumber);
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
                                nfrModel.setRrn(rrn);
                                nfrModel.setField3(field3);
                                nfrModel.setField7(field7);
                                nfrModel.setField9(field9);
                                nfrModel.setField13("VC");

                                nfrModel.setROName(ROName);
                                nfrModel.setRoCity(roCity);
                                nfrModel.setRoMobileNo(roMobileNo);
                                nfrModel.setVehicleNumber(vehicleNumber);
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
                                onlineTxnModel.setRrn(rrn);
                                onlineTxnModel.setField3(field3);
                                onlineTxnModel.setField7(field7);
                                onlineTxnModel.setField9(field9);
                                onlineTxnModel.setField13("VC");

                                onlineTxnModel.setROName(ROName);
                                onlineTxnModel.setRoCity(roCity);
                                onlineTxnModel.setRoMobileNo(roMobileNo);
                                onlineTxnModel.setVehicleNumber(vehicleNumber);
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
                        }

                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            redirectToSuccessPage();
                        });

                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                binding.otp.setText("");
                                progress.dismiss();
                                Log.d("respCode", respCode);
                                MessagesDialog.showDialog(getActivity(), respDesc, 0, null, null);
                            }
                        });
                    }

                }
            } catch (JSONException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.dismiss();
                        Log.d("respCode", e.toString());
                        fileWrite(context, todayDate + ".txt", "validateOtpApi", e.toString());
                        MessagesDialog.showDialog(getActivity(), e.toString(), 0, null, null);
                    }
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

    @Override
    public void onPause() {
        super.onPause();
        MessagesDialog.dismissDialog();
        if (countDownTimer != null) {
            countDownTimer.cancel();
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