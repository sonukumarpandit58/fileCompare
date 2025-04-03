package com.ims.bpcluat.ufill.ufil1;

import static android.app.Activity.RESULT_OK;
import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.fileWrite;
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
import static com.ims.bpcluat.helper.ApiHelper.uFillEndpoint;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.TwentyNineTxnParser;
import com.ims.bpcluat.databinding.FragmentUfillOneBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.helper.NozzleIDMapper;
import com.ims.bpcluat.model.UfillModel;
import com.ims.bpcluat.ufill.utr.UtrFragment;

import org.json.JSONException;
import org.json.JSONObject;

public class UfillOneFragment extends Fragment implements ApiHelper.NetworkingApiCallBack {

    FragmentUfillOneBinding binding;
    Context context;
    private ActivityResultLauncher<Intent> qrCodeLauncher;
    ApiHelper api;
    ProgressDialog progress;
    private static final int REQUEST_CAMERA_CODE = 100;
    String txnId = "", barCode = "", dateTym = "";
    UfillModel ufillModel = new UfillModel();
    String currentApiCall = "";
    public UfillOneFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUfillOneBinding.inflate(inflater, container, false);
        api = new ApiHelper();
        context = getActivity();

        binding.scanQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), UfillScannerActivity.class);
                startActivityForResult(intent, REQUEST_CAMERA_CODE);
            }
        });

        binding.utrBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new UtrFragment());
            }
        });

        clear();

        return binding.getRoot();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAMERA_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                String qrResult = data.getStringExtra("qrResult");

                boolean timeoutCompleted = data.getBooleanExtra("timeoutCompleted", false);
                if (timeoutCompleted) {
                    Toast.makeText(context, "Scan QR Timeout. Please try with UTR number or rescan QR.", Toast.LENGTH_SHORT).show();
                    binding.utrBtn.setVisibility(View.VISIBLE);
                }

                if (qrResult != null && !qrResult.isEmpty()) {
                    uFillOneVoucherRedemption(qrResult);
                }
            }else{
                Log.d("HKKK", "QR result is null or no data received.");
            }
        }
    }

    private void uFillOneVoucherRedemption(String barcode) {
        currentApiCall = "voucherReedem";
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);
        progress.show();

        String url = uFillEndpoint;
        JSONObject jsonObject = new JSONObject();
        try {
            Log.d("QRCode", barcode);
            jsonObject.put("channel", channelName);
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("userName", username);
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnType", "AQR");
            jsonObject.put("rrn", barcode);
            jsonObject.put("dateTime", requestDate() + requestTime());
            jsonObject.put("txnId", Helper.createTxnIdForOfflineTxn());
            jsonObject.put("hwSrNo",Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", version);

            txnId = Helper.createTxnIdForOfflineTxn();
            barCode = barcode;
            dateTym = requestDate() + requestTime();
            Log.d("txnId", txnId);
            Log.d("ufillScanRequest", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(UfillOneFragment.this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void inquiryUFillOneRedemption() {
        currentApiCall = "inquiryVoucherRedeem";
        String url = uFillEndpoint;
        JSONObject jsonObject = new JSONObject();
        try {
            Log.d("QRCode", barCode);
            jsonObject.put("channel", channelName);
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("userName", username);
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnType", "IQR");
            jsonObject.put("rrn", barCode);
            jsonObject.put("dateTime", dateTym);
            jsonObject.put("txnId", txnId);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", version);

            Log.d("inquiryVoucherRequest=", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(UfillOneFragment.this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void apiResult(String res, String apiName) {
        if (currentApiCall.equals("voucherReedem")) {
            Log.d("apiResult","Vouche Reedem");
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            inquiryUFillOneRedemption();
                           // Toast.makeText(getActivity(), "Server Time Out", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Log.d("ReedemResponse = ",res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");
                    if (respCode.equals("200")) {
                        String amt = payLoad.getString("amt");
                        if(payLoad.has("authCode")){
                            String authCode = payLoad.getString("authCode");
                            Log.d("vmsTxnIdAuthCode",authCode);
                            ufillModel.setPrebookTxn(authCode);
                            ufillModel.setAuthCode(authCode);
                        }else{
                            ufillModel.setPrebookTxn("");
                        }
                        String dateTime = payLoad.getString("dateTime");
                        String rrn = payLoad.getString("rrn");
                        String id = payLoad.getString("id");
                        ufillModel.setRrn(rrn);
                        ufillModel.setId(id);
                        ufillModel.setPrebookTxnTime(dateTime);
                        ufillModel.setNozzleNo("00");
                        ufillModel.setTxnId(txnId);
                        ufillModel.setVoucherAmt(amt);
                        ufillModel.setTxnType("UFILL1");
                        progress.dismiss();
                        Bundle bundle = new Bundle();
                        bundle.putParcelable("ufillModel", ufillModel);
                        ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle,new UfillOnePumpFragment());
                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                MessagesDialog.showDialog(context, respDesc +" "+ respCode, 0,null, null);
                                // Toast.makeText(getActivity(), respDesc + " - " + respCode, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "ReedemResponse", e.toString());
                getActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_SHORT).show();
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    MessagesDialog.showDialog(context, e.toString(), 0,null, null);

                });
               // Log.d("UfillOneFragment",e.toString());
            }
        }else if (currentApiCall.equals("inquiryVoucherRedeem")) {
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progress.dismiss();
                            MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);

                           // Toast.makeText(getActivity(), "Server Time Out", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Log.d("inquiryResponse = ",res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");
                    if (respCode.equals("200")) {
                        String amt = payLoad.getString("amt");
                        if(payLoad.has("authCode")){
                            String authCode = payLoad.getString("authCode");
                            Log.d("vmsTxnIdAuthCode",authCode);
                            ufillModel.setPrebookTxn(authCode);
                            ufillModel.setAuthCode(authCode);
                        }else{
                            ufillModel.setPrebookTxn("");
                        }
                        String dateTime = payLoad.getString("dateTime");
                        String rrn = payLoad.getString("rrn");
                        String id = payLoad.getString("id");
                        ufillModel.setRrn(rrn);
                        ufillModel.setId(id);
                        ufillModel.setPrebookTxnTime(dateTime);
                        ufillModel.setNozzleNo("00");
                        ufillModel.setTxnId(txnId);
                        ufillModel.setVoucherAmt(amt);
                        ufillModel.setTxnType("UFILL1");
                        progress.dismiss();
                        Bundle bundle = new Bundle();
                        bundle.putParcelable("ufillModel", ufillModel);
                        ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle,new UfillOnePumpFragment());
                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                MessagesDialog.showDialog(context, respDesc +" "+ respCode, 0,null, null);

                                //Toast.makeText(getActivity(), respDesc + " - " + respCode, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "inquiryResponse", e.toString());
                getActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_SHORT).show();
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    MessagesDialog.showDialog(context, e.toString(), 0,null, null);

                });
                //Log.d("UfillOneFragment",e.toString());
            }
        }
    }

    public void clear(){
        txnId = "";
        dateTym = "";
        barCode = "";
    }
}