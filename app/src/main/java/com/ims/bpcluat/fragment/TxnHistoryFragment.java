package com.ims.bpcluat.fragment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import static com.ims.bpcluat.Helper.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.adapter.TxnHistoryRecyclerViewAdapter;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.model.TxnHistoryModel;
import com.ims.bpcluat.model.TxnListModel;
import com.ims.bpcluat.databinding.FragmentTxnHistoryBinding;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.helper.ApiHelper.NetworkingApiCallBack;
import com.ims.bpcluat.receiver.ConnectivityReceiver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

public class TxnHistoryFragment extends Fragment implements NetworkingApiCallBack {
    FragmentTxnHistoryBinding binding;
    ApiHelper api;
    ArrayList<TxnHistoryModel> txnHistoryModelArrayList = new ArrayList<>();
    ProgressDialog progress;
    private ConnectivityReceiver connectivityReceiver = new ConnectivityReceiver();
    public TxnHistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTxnHistoryBinding.inflate(inflater, container, false);
        api = new ApiHelper();
        binding.mid.setText("MID : "+mid);
        binding.tid.setText("TID : "+tid);
        if (connectivityReceiver.isConnected(getContext())) {
            TxnHistoryApi();
        } else {
            MessagesDialog.showDialog(requireContext(), "No internet connection", 0,null, null);
        }
        binding.homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SideBarActivity.class);
                startActivity(intent);
            }
        });
        return binding.getRoot();
    }

    private void TxnHistoryApi() {
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();
        String url = "getBillerTxn";
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("client",client);
            jsonObject.put("instId",instId);
            jsonObject.put("userName", username);
            jsonObject.put("channel", channelName);
            jsonObject.put("tid", tid);
            jsonObject.put("source", "TERMINAL");
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            Log.d("txnHistoryRequest = ", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void apiResult(String res, String apiName) {
        try {
            if(res.equals("Server Time Out")){
                progress.dismiss();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);

                       // Toast.makeText(getActivity(), "Server Time Out", Toast.LENGTH_SHORT).show();
                    }
                });
            }else{
                Log.d("txnHistoryResponse",res);
                JSONObject jsonObject = new JSONObject(res);
                JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                String respCode = payLoad.getString("respCode");
                if (respCode.equals("200")) {
                    JSONArray outputArray = payLoad.getJSONArray("output");
                    Log.d("outputArray", String.valueOf(outputArray));
                    Log.d("outputArraySize", String.valueOf(outputArray.length()));
                    String pumpNo = "";
                    String qty = "";
                    String dateTime = "";
                    String price = "";
                    String txnType= "";
                    for(int i=0; i<outputArray.length(); i++){
                        //JSONArray paramListMappingArray = outputArray.getJSONArray(i);
                        //Log.d("paramListMappingArray", String.valueOf(paramListMappingArray));
                        JSONObject outputObj = outputArray.getJSONObject(i);
                        txnType = outputObj.getString("transType") + " - "+ outputObj.getString("payMethod");
                        price = "INR " + outputObj.getString("transAmt");
                        dateTime = outputObj.getString("txnDate") + " " + outputObj.getString("txnTime");
                        Log.d("txnType",txnType);
                        Log.d("myData", String.valueOf(outputObj));
                        JSONArray paramListMappingArray = outputObj.optJSONArray("paramListMapping");
                        Log.d("paramListMapping", String.valueOf(paramListMappingArray));
                        Log.d("paramListMappingSize", String.valueOf(paramListMappingArray.length()));
                        Log.d("mymapArray", String.valueOf(paramListMappingArray.length()));


                        for(int j=0; j<paramListMappingArray.length(); j++){
                            JSONObject paramObj = paramListMappingArray.getJSONObject(j);
                            int k = j +1;
                            //Log.d("paramObj", String.valueOf(paramObj));
                            String param = "param"+k+"Lit";
                            if (paramObj.has(param)) {
                                String key = paramObj.getString(param);
                                String value = paramObj.getString("param"+k);
                                if(key.equals("PUMP_NO")){
                                    if(!value.isEmpty()){
                                        pumpNo = "Pump Number - "+value;
                                    }
                                }
                                if(key.equals("PROD_NAME")){
                                    qty = value;
                                    Log.d("qty",qty);
                                }
                            }
                        }
                        txnHistoryModelArrayList.add(new TxnHistoryModel(pumpNo,qty,dateTime,price,txnType));
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binding.txnHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                            TxnHistoryRecyclerViewAdapter adapter = new TxnHistoryRecyclerViewAdapter(getActivity(),txnHistoryModelArrayList);
                            binding.txnHistoryRecyclerView.setAdapter(adapter);
                            progress.dismiss();
                        }
                    });
                } else {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(requireContext(), respCode, 0,null, null);
                    });
                   // progress.dismiss();
                }
            }
        } catch (JSONException e) {
            fileWrite(getContext(), todayDate + ".txt", "getshiftsummaryResponse", e.toString());
            getActivity().runOnUiThread(() -> {
                progress.dismiss();
                Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_SHORT).show();
                if (progress.isShowing()) {
                    progress.dismiss();
                }
                        MessagesDialog.showDialog(requireContext(), e.toString(), 0,null, null);

            });
            //throw new RuntimeException(e);
        }
    }
}