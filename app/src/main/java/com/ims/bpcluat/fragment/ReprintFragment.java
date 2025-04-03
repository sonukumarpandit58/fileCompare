package com.ims.bpcluat.fragment;

import static com.ims.bpcluat.Helper.*;
import static com.ims.bpcluat.Helper.city;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.MainActivity;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SuccessActivity;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.databinding.FragmentReprintBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ims.bpcluat.helper.ApiHelper.NetworkingApiCallBack;
import com.ims.bpcluat.helper.ChargeslipHelper;
import com.ims.bpcluat.interfaces.PrintResponseCallBack;
import com.ims.bpcluat.interfaces.RePrintResponseCallBack;
import com.ims.bpcluat.receiver.ConnectivityReceiver;

import java.util.Locale;

public class ReprintFragment extends Fragment implements NetworkingApiCallBack, RePrintResponseCallBack {
    FragmentReprintBinding binding;
    ProgressDialog progress;
    ApiHelper api;
    Context context;
    String txnId = "";
    private ConnectivityReceiver connectivityReceiver = new ConnectivityReceiver();

    public ReprintFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReprintBinding.inflate(inflater, container, false);
        api = new ApiHelper();
        context = getActivity();

        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog

        binding.lastTxnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectivityReceiver.isConnected(getContext())) {
                    binding.lastTxnBtn.setEnabled(false);  // disable submit button
                    fetchLastTxnId();
                } else {
                    MessagesDialog.showDialog(requireContext(), "No internet connection", 0,null, null);
                }
            }
        });

        binding.otherTxnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopup();
            }
        });
        return binding.getRoot();
    }

    private void showPopup() {
        // Create an AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Inflate the custom layout/view
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.ereceipt_popup_layout, null);

        // Find the EditText, Button, Title, and Close Icon in the custom layout
        EditText editTextPopup = dialogView.findViewById(R.id.editTextPopup);
        editTextPopup.setHint("Enter transaction Id");
        Button buttonSubmit = dialogView.findViewById(R.id.buttonSubmit);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        dialogTitle.setText("Transaction Reprint");
        ImageView closeIcon = dialogView.findViewById(R.id.closeIcon);

        // Set the custom layout to the AlertDialog builder
        builder.setView(dialogView);

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        // Set the onClickListener for the close icon
        closeIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        // Set the onClickListener for the submit button
        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txnId = editTextPopup.getText().toString();
                if (txnId.isEmpty()) {
                    editTextPopup.setError("Please enter txnId");
                } else {
                    alertDialog.dismiss();
                    if (connectivityReceiver.isConnected(getContext())) {
                        reprintApi(txnId);
                    } else {
                        MessagesDialog.showDialog(requireContext(), "No internet connection", 0,null, null);
                    }
                }
                // Handle the input text
            }
        });
    }

    private void fetchLastTxnId() {
        progress.show();
        String url = "getBillerTxn";
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("userName", username);
            jsonObject.put("channel", channelName);
            jsonObject.put("tid", tid);
            jsonObject.put("source", "TERMINAL");
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("userType", "Operator");
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this::apiResult);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void reprintApi(String txnId) {
        progress.show();
        String url = "getBillerTxnDetails";
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("userName", username);
            jsonObject.put("channel", channelName);
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("txnId", txnId);
            jsonObject.put("source", "TERMINAL");
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());

            Log.d("ReprintApiRequest", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this::apiResult);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void apiResult(String res, String apiName) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.lastTxnBtn.setEnabled(true);  //Re-enable submit button
            }
        });
        if (apiName.equals("getBillerTxn")) {
            // find last txn Id
            // reprintApi("24060200070926145705");
            if (res.equals("Server Time Out")) {
                progress.dismiss();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                    }
                });
            } else {
                try {
                    Log.d("TxnSummaryResult", res);
                    JSONObject billerTxnObj = new JSONObject(res);
                    JSONObject payLoad = billerTxnObj.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                   // String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");
                    if (respCode.equals("200")) {
                        JSONArray outputArray = payLoad.getJSONArray("output");
                        JSONObject firstObj = outputArray.getJSONObject(0);
                        String txnId = firstObj.getString("ftNumber");
                        Log.d("LastTxnId", txnId);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                reprintApi(txnId);
                            }
                        });
                    }else{
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                MessagesDialog.showDialog(context, "No Txn Found", 0,null, null);
                                // Toast.makeText(getActivity(), "No Txn Found" + " - " + respCode, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    fileWrite(getContext(), todayDate + ".txt", "TxnSummaryResult", e.toString());
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_SHORT).show();
                        if (progress.isShowing()) {
                            progress.dismiss();
                        }
                        MessagesDialog.showDialog(context, e.toString(), 0,null, null);

                    });
                }
            }
        } else if (apiName.equals("getBillerTxnDetails")) {
            // Perform print and error for cash txn
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
                    Log.d("getBillerTxnDetailsResponse = ", res);
                    JSONObject billerTxnObj = new JSONObject(res);
                    JSONObject payLoad = billerTxnObj.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respCode = payLoad.getString("respCode");
                    if (respCode.equals("200")) {
                        JSONObject jsonObject = new JSONObject();
//                        progress.dismiss();
                        createBitMapAndPrint(res);
                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                MessagesDialog.showDialog(context, "Txn not found", 0,null, null);
                            }
                        });
                    }
                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "getBillerTxnDetailsResponse", e.toString());
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

    public void createBitMapAndPrint(String res) {
        Helper.logLongMessage("ReprintRes",res);
        String  mid = "", tid = "", address = "", outputRespDesc = "", authCode = "", outputMid = "";
        String outputTid = "", rrn = "", transType = "", cardExpiry = "", authAmt = "", payMethod = "", cardLast = "", transAmt = "";
        String txnTime = "", txnDate = "", outputRespCode = "", ftNumber = "", cardFirst = "", city = "";
        String product = "",cardNo = "",qty = "",attendantName = "", unitPrice = "",cardType = "";
        String cardPaymentVersionNo = "",transactionCertificate = "", tsi = "", atc = "",tvr = "", aid = "";
        String pumpNo = "", nozzleNo = "",mobileNumber = "",vehicleNumber = "",vehicleType = "",batchNo = "",terminalInvoiceNo = "";
        try {
            JSONObject root = new JSONObject(res);
            JSONObject nameValuePairs = root.getJSONObject("nameValuePairs");
            JSONObject payload = nameValuePairs.getJSONObject("PAYLOAD");
            mid = payload.getString("mid");
            tid = payload.getString("tid");
            if(payload.has("address")){
                address = payload.getString("address");
            }
            client = payload.getString("client");
            txnId = payload.getString("txnId");

            // Parsing operatorDetail array
            JSONArray operatorDetail = payload.getJSONArray("operatorDetail");
            for (int i = 0; i < operatorDetail.length(); i++) {
                // Handle each element in the array
            }

            // Parsing result array
            JSONArray result = payload.getJSONArray("result");
            for (int i = 0; i < result.length(); i++) {
                // Handle each element in the array
            }

            // Parsing billerTranList array
            JSONArray billerTranList = payload.getJSONArray("billerTranList");
            for (int i = 0; i < billerTranList.length(); i++) {
                // Handle each element in the array
            }

            // Parsing output array
            JSONArray output = payload.getJSONArray("output");
            for (int i = 0; i < output.length(); i++) {
                JSONObject outputItem = output.getJSONObject(i);
                payMethod = outputItem.getString("payMethod");
                transAmt = outputItem.getString("transAmt");
                // Parsing paramList array
                JSONArray paramList = outputItem.getJSONArray("paramList");
                for (int j = 0; j < paramList.length(); j++) {
                    JSONObject paramItem = paramList.getJSONObject(j);
                    String paramValue = paramItem.getString("param");
                    String paramLit = paramItem.getString("paramLit");
                    // Handle param and paramLit

                    switch (paramLit) {
                        case "QUANTITY":
                            qty = paramValue;
                            break;
                        case "UNIT_PRICE":
                            unitPrice = paramValue;
                            break;
                        case "Attendant Name":
                            attendantName = paramValue;
                            break;
                        case "PUMP_NO":
                            pumpNo = paramValue;
                            break;
                        case "NOZZLE":
                            nozzleNo = paramValue;
                            break;
                        case "PROD_NAME":
                            product = paramValue;
                            break;
                        case "Customer Mobile":
                            mobileNumber = paramValue;
                            break;
                        case "Vehicle ID":
                            vehicleNumber = paramValue;
                            break;
                        case "Vehicle_Type":
                            vehicleType = paramValue;
                            break;
                        case "cardType":
                            cardType = paramValue;
                            break;
                        case "CUSTOMER_DISC":
                            break;
                    }

                    if(payMethod.equals("CARD")){
                        cardFirst = outputItem.getString("cardFirst");
                        cardLast = outputItem.getString("cardLast");
                        switch (paramLit) {
                            case "batchNo":
                                batchNo = paramValue;
                                break;
                            case "terminalInvoiceNo":
                                terminalInvoiceNo = paramValue;
                                break;
                        }
                    }
                }

                txnTime = outputItem.getString("txnTime");
                txnDate = outputItem.getString("txnDate");
                if(outputItem.has("authCode")){
                    authCode = outputItem.getString("authCode");
                }
            }
            instId = payload.getString("instId");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(payMethod.equals("CARD")){
            if(!batchNo.isEmpty()){
                batchNo = padWithZeroes(Integer.parseInt(batchNo), 6);
            }
            if(!terminalInvoiceNo.isEmpty()){
                terminalInvoiceNo = padWithZeroes(Integer.parseInt(terminalInvoiceNo), 6);
            }
            if(!cardFirst.isEmpty() && !cardLast.isEmpty()){
                cardNo = cardFirst + "*****" + cardLast;
            }
        }

        if(payMethod.equals("ALP")){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progress.dismiss();
                    MessagesDialog.showDialog(getActivity(), "FAILURE- ALP MOP Not allowed for the txnId: " + txnId,0, null, null);
                }
            });
        }
        else{
            JSONObject jsonObject = new JSONObject();
            Log.d("txnDate",txnDate);
            try {
                String productName = product;
                if(product.equals("MS")){
                    productName = "PETROL";
                }else if(product.equals("HSD")){
                    productName = "DIESEL";
                }
                jsonObject.put("agencyName", roName);
                jsonObject.put("address", address1);
                jsonObject.put("city", city);
                jsonObject.put("dealerContactNo",dealerContactNumber);
                jsonObject.put("date", reprintDate(txnDate));
                jsonObject.put("time", txnTime);
                jsonObject.put("bayNo", "");
                jsonObject.put("nozzleNo", "");
                jsonObject.put("product", productName);
                jsonObject.put("payMode", payMethod);
                jsonObject.put("txnId", txnId);
                jsonObject.put("attendentName", Helper.operatorFirstName + " " + Helper.operatorLastName);
                jsonObject.put("txnStart", "");
                jsonObject.put("txnEnd", "");
                jsonObject.put("rate", unitPrice);
                jsonObject.put("volume", qty);
                jsonObject.put("amount", transAmt);
                jsonObject.put("presetType", "");
                jsonObject.put("presetValue", "");
                jsonObject.put("vehicleNo", vehicleNumber);
                jsonObject.put("mobileNo", mobileNumber);
                jsonObject.put("nfrProductName","");
                jsonObject.put("nfrUnitPrice","");
                jsonObject.put("nfrVolume","");
                jsonObject.put("nfrTotalAmount","");
                jsonObject.put("batchNo",batchNo);
                jsonObject.put("terminalInvoiceNo",terminalInvoiceNo);
                jsonObject.put("cardNo",cardNo);
                jsonObject.put("authCode",authCode);
                jsonObject.put("cardTxnCustomerName","");

                ChargeslipHelper chargeslipHelper = new ChargeslipHelper();
                Bitmap reprintChargeSlip= chargeslipHelper.chargeslip(getActivity(), jsonObject,"DUPLICATE COPY");
                chargeslipHelper.rePrint(getActivity(),reprintChargeSlip);
                chargeslipHelper.setCallbackReprint(this);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

    }

    @Override
    public void printSuccess() {
        Log.d("printSuccess","method Called..");
        progress.dismiss();
    }

    @Override
    public void printFail(String errorResponse) {
        Log.d("printFail","method Called..");
        progress.dismiss();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), errorResponse, Toast.LENGTH_SHORT).show();
            }
        });
    }
}