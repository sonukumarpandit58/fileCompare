package com.ims.bpcluat.alp.alpOperations.sale.loyalitypayqr;

import static com.ims.bpcluat.Helper.appVersion;
import static com.ims.bpcluat.Helper.createJsonObject;
import static com.ims.bpcluat.Helper.fuelProductList;

import static com.ims.bpcluat.Helper.getProductId;
import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.roName;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.username;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.SuccessActivity;
import com.ims.bpcluat.alp.alpOperations.sale.TxnFailFragment;
import com.ims.bpcluat.cng.CngSuccessActivity;
import com.ims.bpcluat.databinding.FragmentScanQRBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.model.CngModel;
import com.ims.bpcluat.model.NfrModel;
import com.ims.bpcluat.model.OnlineTxnModel;
import com.ims.bpcluat.nfr.NfrSuccessActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ScanQRFragment extends Fragment implements ApiHelper.NetworkingApiCallBack {
    FragmentScanQRBinding binding;
    ApiHelper api;
    ProgressDialog progress;
    Context context;
    private Handler handler;
    private Runnable runnable;
    private final int delay = 10000;
    private CngModel cngModel;
    private NfrModel nfrModel;
    private OnlineTxnModel onlineTxnModel;
    private final int totalDuration = 180000;
    private long startTime;
    private CountDownTimer countDownTimer;

    String programListApiCall;
    String amount = "", mobileNum, unitPrice = "";
    String tran_date = "", tran_time = "", ft_number = "", cust_id = "", field3 = "";
    String alpVehicleNumber = "";
    String pumpNo = "", nozzleNo = "", localMPDId = "", localProductID = "", qty = "", vehId = "", field1 = "", field7 = "", field9 = "", field13 = "",isTxnOnline= "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentScanQRBinding.inflate(inflater, container, false);
        api = new ApiHelper();
        context = getContext();
        handler = new Handler();
        startTime = System.currentTimeMillis();

        argData();

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.removeCallbacks(runnable);
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }
                if (cngModel != null) {
                    TxnFailFragment fragment = new TxnFailFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("txnStatus", "Transaction Cancelled!");
                    bundle.putParcelable("cngModel", cngModel);
                    ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);
                } else if (nfrModel != null) {
                    TxnFailFragment fragment = new TxnFailFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("txnStatus", "Transaction Cancelled!");
                    bundle.putParcelable("nfrModel", nfrModel);
                    ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);
                } else if (onlineTxnModel != null) {
                    TxnFailFragment fragment = new TxnFailFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("txnStatus", "Transaction Cancelled!");
                    bundle.putParcelable("onlineTxnModel", onlineTxnModel);
                    ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);
                }

                // BackWithData(getContext(), cngModel, nfrModel, onlineTxnModel);
            }
        });

        countDownTimer = new CountDownTimer(180000, 1000) {

            public void onTick(long millisUntilFinished) {
                binding.timerTextView.setText(millisUntilFinished / 1000 + " seconds remaining");
            }

            public void onFinish() {
                binding.timerTextView.setText("Time's up!");
            }

        }.start();

        runnable = new Runnable() {
            @Override
            public void run() {
                long elapsedTime = System.currentTimeMillis() - startTime;
                if (elapsedTime < totalDuration) {
                    Log.d("", "transactionStatusCheckApi");
                    transactionStatusCheckApi();
                    handler.postDelayed(this, delay);
                } else {
                    Log.d("Runnable", "Stopped after 180 seconds.");
                    stopRepeatingTask();
                }

            }
        };

        handler.postDelayed(runnable, delay);
        return binding.getRoot();
    }

    private void argData() {
        if (getArguments() != null) {
            String payLoadString = getArguments().getString("payLoad");
            cngModel = getArguments().getParcelable("cngModel");
            nfrModel = getArguments().getParcelable("nfrModel");
            onlineTxnModel = getArguments().getParcelable("onlineTxnModel");
            if (cngModel != null) {
                amount = cngModel.getTotalAmt();
                unitPrice = cngModel.getPerAmt();
                mobileNum = cngModel.getMobileNumber();
                qty = cngModel.getQty();
                vehId = cngModel.getVehicleNumber();
                localProductID = getProductId("CNG", fuelProductList);
                field1 = "Offline";
            } else if (onlineTxnModel != null) {
                amount = onlineTxnModel.getAmount();
                mobileNum = onlineTxnModel.getMobileNumber();
                pumpNo = onlineTxnModel.getPumpNo();
                nozzleNo = onlineTxnModel.getPumpNo();
                localMPDId = onlineTxnModel.getLocalMPDId();
                localProductID = onlineTxnModel.getProductId();
                qty = onlineTxnModel.getQty();
                vehId = onlineTxnModel.getVehicleNumber();
                isTxnOnline = onlineTxnModel.getIsTxnOnline();
                if(isTxnOnline.equals("no")){
                    field1 = "Offline";
                }else{
                    field1 = "Online";
                }
            } else if (nfrModel != null) {
                amount = nfrModel.getAmt();
                mobileNum = nfrModel.getMobileNumber();
                qty = nfrModel.getQty();
                vehId = nfrModel.getVehicleNumber();
                localProductID = getProductId("LUBES", fuelProductList);
                field1 = "Offline";
            }

            try {
                JSONObject payLoad = new JSONObject(payLoadString);
                JSONArray billerTranList = payLoad.getJSONArray("billerTranList");
                for (int i = 0; i < billerTranList.length(); i++) {
                    JSONObject transaction = billerTranList.getJSONObject(i);
                    String mid = transaction.getString("mid");
                    String tid = transaction.getString("tid");
                    String transStatus = transaction.getString("trans_status");
                    String tranAmt = transaction.getString("tran_amt");
                    String rrn = transaction.getString("rrn");
                    tran_date = transaction.getString("tran_date");
                    tran_time = transaction.getString("tran_time");
                    ft_number = transaction.getString("ft_number");
                    cust_id = transaction.getString("cust_id");
                    field3 = transaction.getString("field3");
                }


                if (payLoad.has("output")) {
                    JSONArray outputArray = payLoad.getJSONArray("output");
                    JSONObject outputObj = outputArray.getJSONObject(0);
                    String encodedImage = outputObj.getString("encodedImage");

                    byte[] decodedImage = Base64.decode(encodedImage, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.length);

                    ImageView imageView = binding.qrCode;
                    imageView.setImageBitmap(bitmap);
                    Log.d("bitmap#######", bitmap.toString());
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.d("getArguments@#", "No arguments passed");
        }

    }

    private void transactionStatusCheckApi() {
        programListApiCall = "transactionStatusCheckApi";
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
            billerTranItem.put("tran_time", tran_date);
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
            paramList.put(createJsonObject(mobileNum, "Customer Mobile"));
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

            Log.d("ApiName","Check Transaction Status - ATS");
            Log.d("Request", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void virtualCardReversalApi() {
        programListApiCall = "virtualCardReversalApi";
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
            jsonObject.put("txnType", "ARV");
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
//            billerTranItem.put("field3", field3);
            billerTranList.put(billerTranItem);
            billerTranItem.put("paramList", paramList);

            paramList.put(createJsonObject(pumpNo, "PUMP_NO"));
            paramList.put(createJsonObject(localMPDId, "localMPDId"));
            paramList.put(createJsonObject(nozzleNo, "NOZZLE"));
            paramList.put(createJsonObject(localMPDId, "localProductID"));
            paramList.put(createJsonObject(mobileNum, "Customer Mobile"));
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

            Log.d("ApiName","Reversal - ARV");
            Log.d("Request", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void apiResult(String res, String apiName) {
        if (programListApiCall.equals("transactionStatusCheckApi")) {
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                    });
                } else {
                    Log.d("transacsRese = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");

                    if (respCode.equals("200")) {
                        Log.d("respCodeQrrrrr", respCode);
                        JSONArray billerTranList = payLoad.getJSONArray("billerTranList");
                        JSONObject transaction = billerTranList.getJSONObject(0);
                        String rrn = "";
                        if(transaction.has("rrn")){
                            rrn = transaction.getString("rrn");
                        }

                        field1 = transaction.getString("field1");
                        field7 = transaction.getString("field7");
                        field9 = transaction.getString("field9");

                        String trans_status = "";
                        if (transaction.has("trans_status")) {
                            trans_status = transaction.getString("trans_status");
                        }

                        if (trans_status.equals("SUCCESS")) {
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
                                cngModel.setField7(field7);
                                cngModel.setField9(field9);
                                cngModel.setField13("VC");
                                cngModel.setVehicleNumber(alpVehicleNumber);
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
                            } else if (onlineTxnModel != null) {
                                onlineTxnModel.setROName(ROName);
                                onlineTxnModel.setRoCity(roCity);
                                onlineTxnModel.setRoMobileNo(roMobileNo);
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
                            } else if (nfrModel != null) {
                                nfrModel.setROName(ROName);
                                nfrModel.setRoCity(roCity);
                                nfrModel.setRoMobileNo(roMobileNo);
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
                            }
                            getActivity().runOnUiThread(() -> {
                                redirectToSuccessPage();
                            });
                        }


                    } else {
                        getActivity().runOnUiThread(() -> {
                            Log.d("apiResult", ": " + respDesc + " - " + respCode);
                        });
                    }
                }
            } catch (JSONException e) {
                getActivity().runOnUiThread(() -> {
                    e.printStackTrace();
                });
            }
        }
    }

    private void redirectToSuccessPage() {
        handler.removeCallbacks(runnable);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
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

    private void stopRepeatingTask() {
        virtualCardReversalApi();
        handler.removeCallbacks(runnable);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (cngModel != null) {
            TxnFailFragment fragment = new TxnFailFragment();
            Bundle bundle = new Bundle();
            bundle.putParcelable("cngModel", cngModel);
            bundle.putString("txnStatus", "Transaction TimeOut!");

            ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);
        } else if (nfrModel != null) {
            TxnFailFragment fragment = new TxnFailFragment();
            Bundle bundle = new Bundle();
            bundle.putParcelable("nfrModel", nfrModel);
            bundle.putString("txnStatus", "Transaction TimeOut!");

            ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);
        } else if (onlineTxnModel != null) {
            TxnFailFragment fragment = new TxnFailFragment();
            Bundle bundle = new Bundle();
            bundle.putParcelable("onlineTxnModel", onlineTxnModel);
            bundle.putString("txnStatus", "Transaction TimeOut!");

            ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);
        }
//        handler.removeCallbacks(runnable);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("Runnable", "onStop");
        handler.removeCallbacks(runnable);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("Runnable", "onDestroy");
        handler.removeCallbacks(runnable);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

}