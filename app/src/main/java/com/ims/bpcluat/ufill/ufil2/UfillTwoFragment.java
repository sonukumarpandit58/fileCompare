package com.ims.bpcluat.ufill.ufil2;

import static com.ims.bpcluat.Helper.appVersion;

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
import static com.ims.bpcluat.helper.BleDeviceHelper.retryOneMoreTime;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.ims.bpcluat.Helper;
//import com.ims.bpcluat.adapter.VoucherAdapter;
import com.ims.bpcluat.adapter.ufil_adapter.VoucherAdapter;
import com.ims.bpcluat.databinding.FragmentUfillTwoBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.helper.BleDeviceHelper;
import com.ims.bpcluat.interfaces.VoucherRecycerViewInterface;
import com.ims.bpcluat.model.UfillModel;
import com.ims.bpcluat.model.VoucherModel;
import com.ims.bpcluat.receiver.ConnectivityReceiver;
import com.ims.bpcluat.ufill.VoucherRedeemActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class UfillTwoFragment extends Fragment implements ApiHelper.NetworkingApiCallBack, VoucherRecycerViewInterface {
    FragmentUfillTwoBinding binding;
    ApiHelper api;
    ArrayList<VoucherModel> VoucherModelArrayList = new ArrayList<>();
    ProgressDialog progress;
    String txnId = "", dateTime = "", ufillTxnId = "", pumpNo = "", voucherAmount = "", utrNo = "";
    UfillModel ufillModel = new UfillModel();
    String currentApiCall = "";
    private ConnectivityReceiver connectivityReceiver = new ConnectivityReceiver();
    Context context;
    Date date = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
    String todayDate = formatter.format(date);
    private BleDeviceHelper bleDeviceHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUfillTwoBinding.inflate(inflater, container, false);
        api = new ApiHelper();

        context = getActivity();
        fileWrite(context, todayDate + ".txt", "LandingPage : ", "UfillTwoFrag");
        bleDeviceHelper = bleDeviceHelper.getInstance(getActivity());
        retryOneMoreTime = false;
        bleDeviceHelper.disconnect();

        binding.lastTxnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (binding.bayid.getText().toString().trim().isEmpty()) {
                    Toast.makeText(getActivity(), "Please Enter Bay Id", Toast.LENGTH_SHORT).show();
                } else {
                    binding.lastTxnBtn.setEnabled(false);
                    if (connectivityReceiver.isConnected(getContext())) {
                        Helper.closeKeyboard(getActivity());
                        voucherListApi();
                    } else {
                        binding.lastTxnBtn.setEnabled(true);
                        MessagesDialog.showDialog(requireContext(), "No internet connection", 0,null, null);
                    }
                }
            }
        });

        binding.redembtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isAnySelected = false;
                for (VoucherModel model : VoucherModelArrayList) {
                    if (model.isSelected()) {
                        isAnySelected = true;
                        break;
                    }
                }

                if (isAnySelected) {
                    voucherRedeemApi(ufillTxnId);
                    if(pumpNo.length() == 1){
                        pumpNo = "0"+pumpNo;
                    }
                    ufillModel.setPumpNo(pumpNo);
                    ufillModel.setNozzleNo("00");
                    ufillModel.setTxnId(txnId);
                    ufillModel.setVoucherAmt(voucherAmount);
                    ufillModel.setUtrNo(utrNo);
                } else {
                    Toast.makeText(getActivity(), "Please Select a Voucher", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return binding.getRoot();
    }

    //voucher api
    private void voucherListApi() {
        currentApiCall = "fetchVoucher";
        hideKeyboard();
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();
        String url = "ufill";
        String reqDate = requestDate();
        String reqTime = requestTime();
        txnId = Helper.createTxnIdForOfflineTxn();
        dateTime = reqDate + reqTime;
        String pumpNo = binding.bayid.getText().toString().trim();
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
            jsonObject.put("txnType", "AOT");
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnId", txnId);
            jsonObject.put("pumpNo", pumpNo);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "0");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);
            Log.d("voucherListRequest=", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void voucherRedeemApi(String ufillTxnId) {
        currentApiCall = "voucherReedem";
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();
        String url = "ufill";
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("userName", username);
            jsonObject.put("channel", "BPCL");
            jsonObject.put("tid", tid);
            jsonObject.put("mid", mid);
            jsonObject.put("source", "Mobile");
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("dateTime", dateTime);
            jsonObject.put("txnType", "AUF");
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnId", txnId);
            jsonObject.put("rrn", ufillTxnId); //add txnId
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "0");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", appVersion);
            Log.d("voucherReedemRequest=", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void inquiryVoucherRedeemApi(String ufillTxnId) {
        currentApiCall = "inquiryVoucherRedeem";
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();
        String url = "ufill";
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("userName", username);
            jsonObject.put("channel", "BPCL");
            jsonObject.put("tid", tid);
            jsonObject.put("mid", mid);
            jsonObject.put("source", "Mobile");
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("dateTime", dateTime);
            jsonObject.put("txnType", "IUF");
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnId", txnId);
            jsonObject.put("rrn", ufillTxnId); //add txnId
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "0");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", appVersion);
            Log.d("inquiryVoucherRequest=", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void apiResult(String res, String apiName) {
        if (currentApiCall.equals("fetchVoucher")) {
            getActivity().runOnUiThread(() -> {
                binding.lastTxnBtn.setEnabled(true);  //Re-enable submit button
            });
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                    });
                } else {
                    Log.d("voucherFetchResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");

                    if (respCode.equals("200")) {
                        JSONArray outputArray = payLoad.getJSONArray("output");
                        Log.d("ufilloutput", String.valueOf(outputArray));
                        String pumpNo = payLoad.getString("pumpNo");
                        //binding.baybutton.setText("Bay " + pumpNo);

                        VoucherModelArrayList.clear(); // Clear previous data

                        for (int i = 0; i < outputArray.length(); i++) {
                            JSONObject outputObject = outputArray.getJSONObject(i);
                            Log.d("outputObject", outputObject.toString());

                            JSONArray vmsActiveTransactions = outputObject.getJSONArray("vmsActiveTransactions");

                            for (int j = 0; j < vmsActiveTransactions.length(); j++) {
                                JSONObject voucherObject = vmsActiveTransactions.getJSONObject(j);
                                Log.d("VoucherObject", voucherObject.toString());

                                if (voucherObject.has("utrNo") && voucherObject.has("amount") && voucherObject.has("amtAuthorizedRs")) {
                                    String utrNo = voucherObject.getString("utrNo");
                                    String amount = voucherObject.getString("amount");
                                    String amtAuthorizedRs = voucherObject.getString("amtAuthorizedRs");
                                    String ufillTxnId = voucherObject.getString("ufillTxnId");
                                    String localBayID = voucherObject.getString("localBayID");
                                    String issueTimeStamp = voucherObject.getString("issueTimeStamp");

                                    VoucherModel voucherModel = new VoucherModel(utrNo, issueTimeStamp, amount, amtAuthorizedRs, ufillTxnId, pumpNo, localBayID);
                                    VoucherModelArrayList.add(voucherModel);
                                } else {
                                    Log.e("VoucherObjectError", "Missing keys in voucherObject: " + voucherObject.toString());
                                }
                            }
                        }

                        getActivity().runOnUiThread(() -> {
                            Log.d("VoucherListSize", "Size: " + VoucherModelArrayList.size());
                            binding.baybutton.setText("Bay " + pumpNo);
                            binding.rvVoucher.setLayoutManager(new LinearLayoutManager(getActivity()));
                            VoucherAdapter adapter = new VoucherAdapter((Context) getActivity(), VoucherModelArrayList, (VoucherRecycerViewInterface) UfillTwoFragment.this);
                            binding.rvVoucher.setAdapter(adapter);
                            binding.baybutton.setVisibility(View.VISIBLE);
                            binding.redembtn.setVisibility(View.VISIBLE);
                            progress.dismiss();
                        });
                    } else {
                        getActivity().runOnUiThread(() -> {
                            VoucherModelArrayList.clear(); // Clear previous data
                            progress.dismiss();
                            MessagesDialog.showDialog(requireContext(), respDesc, 0,null, null);
                            // Toast.makeText(getActivity(), respDesc + " - " + respCode, Toast.LENGTH_SHORT).show();
                            binding.baybutton.setVisibility(View.GONE);
                            binding.redembtn.setVisibility(View.GONE);
                        });
                    }
                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "voucherFetchResponse", e.toString());
                getActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_SHORT).show();
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    MessagesDialog.showDialog(requireContext(), e.toString(), 0,null, null);

                });
          /*      getActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    e.printStackTrace();
                });*/
            }
        } else if (currentApiCall.equals("voucherReedem")) {
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        //progress.dismiss();
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);

                       // Toast.makeText(getActivity(), "Server Time Out", Toast.LENGTH_SHORT).show();
                        inquiryVoucherRedeemApi(ufillTxnId);
                    });
                } else {
                    Log.d("ReedemRes = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");

                    if (respCode.equals("200")) {
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
                        progress.dismiss();
                        ufillModel.setTxnType("UFILL2");
                        Intent intent = new Intent(getActivity(), VoucherRedeemActivity.class);
                        intent.putExtra("ufillModel", ufillModel);
                        startActivity(intent);
                    } else {
                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            MessagesDialog.showDialog(requireContext(), respDesc, 0,null, null);

                            // Toast.makeText(getActivity(), respDesc + " - " + respCode, Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "ReedemRes", e.toString());
                getActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_SHORT).show();
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    MessagesDialog.showDialog(requireContext(), e.toString(), 0,null, null);

                });
             /*   progress.dismiss();
                e.printStackTrace();*/
            }
        } else if (currentApiCall.equals("inquiryVoucherRedeem")) {
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);

                       // Toast.makeText(getActivity(), "Server Time Out", Toast.LENGTH_SHORT).show();
                        inquiryVoucherRedeemApi(ufillTxnId);
                    });
                } else {
                    Log.d("inquiryVoucherRes = ", res);
                }
            } catch (Exception e) {

            }
        }
    }

    public void hideKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onclick(int position) {
        if (position != -1 && position < VoucherModelArrayList.size()) {
            Helper.txnListPostionSelected = position;
            ufillTxnId = VoucherModelArrayList.get(position).ufillTxnId;
            pumpNo = VoucherModelArrayList.get(position).pumpNo;
            voucherAmount = VoucherModelArrayList.get(position).amtAuthorizedRs;
            utrNo = VoucherModelArrayList.get(position).utrNo;
            Log.d("ufillTxnId", ufillTxnId);
        } else {
            // Handle invalid position case (optional)
            Log.e("Error", "Invalid position: " + position);
        }
    }

}
