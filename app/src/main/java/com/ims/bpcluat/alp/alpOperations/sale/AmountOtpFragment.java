package com.ims.bpcluat.alp.alpOperations.sale;

import static com.ims.bpcluat.Helper.appVersion;

import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
//import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.sapCode;
//import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.Helper.username;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.alp.alpOperations.sale.loyalitypayqr.ScanQRFragment;
import com.ims.bpcluat.alp.alpOperations.sale.loyaltyotp.MobileNumberFragment;
import com.ims.bpcluat.databinding.FragmentAmountOtpBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProductModel;
import com.ims.bpcluat.model.AlpModels.SaleModel.VirtualCardProgramModel;
import com.ims.bpcluat.model.AlpModels.TempModel;
import com.ims.bpcluat.utils.SharedPrefHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;

public class AmountOtpFragment extends Fragment implements ApiHelper.NetworkingApiCallBack {
    FragmentAmountOtpBinding binding;
    SharedPrefHelper sharedPrefHelper;
    String programListApiCall = "";
    ProgressDialog progress;
    ApiHelper api;
    String shredValue;
    ArrayList<ProductModel> productModelArrayList = new ArrayList<>();
    int selectedIndex;
    Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAmountOtpBinding.inflate(inflater, container, false);
        sharedPrefHelper = new SharedPrefHelper(requireContext());
        api = new ApiHelper();
        context = getContext();

        Bundle bundle = getArguments();
        if (bundle != null) {
            selectedIndex = bundle.getInt("index", -1);
            productModelArrayList = (ArrayList<ProductModel>) bundle.getSerializable("productList");
            if (selectedIndex != -1) {}
        }

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new ProductListFragment());
            }
        });

        binding.submitamountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String amount = binding.amt.getText().toString().trim();
                validation(amount);
            }
        });

        return binding.getRoot();
    }

    private void validation(String amount) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("amount", amount);
        bundle.putSerializable("index", selectedIndex);
        bundle.putSerializable("productModel", productModelArrayList);

        if (amount.isEmpty()) {
            binding.amt.setError("Enter Amount");
            binding.amt.requestFocus();
        } else {
            BigDecimal amt = new BigDecimal(amount);
            BigDecimal threshold = new BigDecimal("100000");
            BigDecimal zeroCheck = new BigDecimal("0");

            if (amt.compareTo(zeroCheck) <= 0) {
                binding.amt.setError("Amount must be greater than zero");
                binding.amt.requestFocus();
            } else if (amt.compareTo(threshold) >= 0) {
                binding.amt.setError("Please enter amount less than 100000");
                binding.amt.requestFocus();
            } else {
                binding.amt.setError(null);
                shredValue = sharedPrefHelper.getString("smartpayBtn", "");
                if (shredValue.equals("loyalitycardBtn")) {
                    ((SideBarActivity) requireActivity()).loadFragement(new MobileNumberFragment());

                } else if (shredValue.equals("loyalityotpBtn")) {
                    ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, new MobileNumberFragment());
                } else {
                    smartPayQrApi(amount);
//                    ((SideBarActivity) requireActivity()).loadFragement(new ScanQRFragment());

                }

            }
        }
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

        JSONObject jsonObject = new JSONObject();
        try {

            jsonObject.put("channel", "BPCL");
            jsonObject.put("reqDate", reqDate);
            jsonObject.put("reqTime", reqTime);
            jsonObject.put("userName", username);
            jsonObject.put("tid", tid);
            jsonObject.put("mid", mid);
            jsonObject.put("txnType", "ASQ");
//            jsonObject.put("source", "Mobile");
            jsonObject.put("hwSrNo", Helper.serialNumber);

            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);

            JSONArray billerTranList = new JSONArray();
            JSONObject billerTranItem = new JSONObject();
            billerTranItem.put("mid", "470000095359490");
            billerTranItem.put("tid", "33720640");
            billerTranItem.put("trans_status", "PENDING");
            billerTranItem.put("tran_amt", amnt);
            billerTranItem.put("tran_date", reqDate);
            billerTranItem.put("tran_time", reqTime);
            billerTranItem.put("ft_number", "20240828111614000");
            billerTranItem.put("cust_id", "9994879696");
            billerTranItem.put("pay_method", "ALPVC");
            billerTranItem.put("authAmt", amnt);
            billerTranItem.put("refundAmt", "0");
            billerTranItem.put("balanceAmt", amnt);
            billerTranItem.put("field1", "Online");

            JSONArray paramList = new JSONArray();
            JSONObject paramItem;

            paramItem = new JSONObject();
            paramItem.put("param_lit", "PUMP_NO");
            paramItem.put("param", "0");
            paramList.put(paramItem);

            paramItem = new JSONObject();
            paramItem.put("param_lit", "localMPDId");
            paramItem.put("param", "0");
            paramList.put(paramItem);

            paramItem = new JSONObject();
            paramItem.put("param_lit", "NOZZLE");
            paramItem.put("param", "0");
            paramList.put(paramItem);

            paramItem = new JSONObject();
            paramItem.put("param_lit", "localProductID");
            paramItem.put("param", "1");
            paramList.put(paramItem);

            paramItem = new JSONObject();
            paramItem.put("param_lit", "Customer Mobile");
            paramItem.put("param", "8179326748");
            paramList.put(paramItem);

            paramItem = new JSONObject();
            paramItem.put("param_lit", "SAP CODE");
            paramItem.put("param", sapCode);
            paramList.put(paramItem);

            paramItem = new JSONObject();
            paramItem.put("param_lit", "ProgramId");
            paramItem.put("param", "SmartFleet");
            paramList.put(paramItem);

            paramItem = new JSONObject();
            paramItem.put("param_lit", "WalletId");
            paramItem.put("param", "1");
            paramList.put(paramItem);

            paramItem = new JSONObject();
            paramItem.put("param_lit", "odometerReading");
            paramItem.put("param", "");
            paramList.put(paramItem);

            paramItem = new JSONObject();
            paramItem.put("param_lit", "QUANTITY");
            paramItem.put("param", "10");
            paramList.put(paramItem);

            paramItem = new JSONObject();
            paramItem.put("param_lit", "MERCH NAME");
            paramItem.put("param", "BPCL TEST");
            paramList.put(paramItem);

            paramItem = new JSONObject();
            paramItem.put("param_lit", "Attendant ID");
            paramItem.put("param", "");
            paramList.put(paramItem);

            paramItem = new JSONObject();
            paramItem.put("param_lit", "UNIT_PRICE");
            paramItem.put("param", "125");
            paramList.put(paramItem);

            paramItem = new JSONObject();
            paramItem.put("param_lit", "Vehicle ID");
            paramItem.put("param", "");
            paramList.put(paramItem);

            paramItem = new JSONObject();
            paramItem.put("param_lit", "CUSTOMER_DISC");
            paramItem.put("param", "0");
            paramList.put(paramItem);

            paramItem = new JSONObject();
            paramItem.put("param_lit", "FCC TIMESTAMP");
            paramItem.put("param", "20240828111602");
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

            Log.d("getOtpRequest=", String.valueOf(jsonObject));
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
                        MessagesDialog.showDialog(context, "Server Time Out", 0,null, null);
                    });
                } else {
                    Log.d("getSmartPayQrApiResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");

                    if (respCode.equals("200")) {
                        Log.d("respCode", respCode);

//                        // Process operatorDetail and result if needed
//                        JSONArray operatorDetail = payLoad.getJSONArray("operatorDetail");
//                        JSONArray result = payLoad.getJSONArray("result");
//
//                        // Process billerTranList
//                        JSONArray billerTranList = payLoad.getJSONArray("billerTranList");
//                        for (int i = 0; i < billerTranList.length(); i++) {
//                            JSONObject transaction = billerTranList.getJSONObject(i);
//                            String mid = transaction.getString("mid");
//                            String tid = transaction.getString("tid");
//                            String transStatus = transaction.getString("trans_status");
//                            String tranAmt = transaction.getString("tran_amt");
//                            String rrn = transaction.getString("rrn");
//
//                            Log.d("TransactionInfo", "MID: " + mid + ", TID: " + tid + ", Status: " + transStatus);
//                        }


//                        if (payLoad.has("output")) {
//                            JSONArray outputArray = payLoad.getJSONArray("output");
//                            JSONObject outputObj = outputArray.getJSONObject(0);
//                            String encodedImage = outputObj.getString("encodedImage");
//
//                            byte[] decodedImage = Base64.decode(encodedImage, Base64.DEFAULT);
//                            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.length);
//
//                            ImageView imageView = getActivity().findViewById(R.id.imageView);
//                            imageView.setImageBitmap(bitmap);
//                        }

                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();


                            ScanQRFragment fragment = new ScanQRFragment();
                            Bundle bundle = new Bundle();
                            bundle.putString("payLoad", payLoad.toString());
                            fragment.setArguments(bundle);
                            ((SideBarActivity) context).loadFragmentWithData(bundle, fragment);
                        });

                    } else {
                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            MessagesDialog.showDialog(context, respDesc, 0,null, null);
                        });
                    }
                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "getSmartPayQrApiResponse", e.toString());
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
    }

}
