package com.ims.bpcluat.fragment;

import static com.ims.bpcluat.Helper.getPresetType;
import static com.ims.bpcluat.Helper.getProductNameById;
import static com.ims.bpcluat.Helper.txnArrayList;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.ims.bpcluat.Helper;
import com.ims.bpcluat.PaymentActivity;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.model.OnlineTxnModel;
import com.ims.bpcluat.model.TxnListModel;
import com.ims.bpcluat.adapter.TxnListRecyclerAdapter;
import com.ims.bpcluat.TxnListRecyclerViewInterface;
import com.ims.bpcluat.conversion.HexToDecimal;
import com.ims.bpcluat.databinding.FragmentOnlineTxnListBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class OnlineTxnListFragment extends Fragment implements TxnListRecyclerViewInterface{

    FragmentOnlineTxnListBinding binding;
    ArrayList<TxnListModel> txnListModalArrayList = new ArrayList<>();
    OnlineTxnModel onlineTxnModel = new OnlineTxnModel();
    public OnlineTxnListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOnlineTxnListBinding.inflate(inflater, container, false);
        binding.pumpNumberTextView.setText(Helper.txnReleatedPumpNo);
        if(txnArrayList.size() > 0){
            for(int i =0; i <txnArrayList.size(); i++){
                Object myobj = txnArrayList.get(i);
                Gson gson = new Gson();
                String json = gson.toJson(myobj);
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    String pumpNo = HexToDecimal.convert(jsonObject.getString("PumpNumber"));
                    String date = HexToDecimal.convert(jsonObject.getString("Day"));
                    String month = HexToDecimal.convert(jsonObject.getString("Month"));
                    String year = HexToDecimal.convert(jsonObject.getString("Year"));
                    String hour =  HexToDecimal.convert(jsonObject.getString("Hour"));
                    String min = HexToDecimal.convert(jsonObject.getString("Minute"));
                    String second = HexToDecimal.convert(jsonObject.getString("TxnStartSecond"));
                    String netAmount = HexToDecimal.convertAmount(jsonObject.getString("NetAmount"));
                    String qty = HexToDecimal.convertLitre(jsonObject.getString("Volume"));
                    String product = HexToDecimal.convertProduct(jsonObject.getString("ProductID"));
                    String txnDateTime = "";

                    if(date.length() == 1){
                        date = "0"+date;
                    }
                    if(month.length() == 1){
                        month = "0"+month;
                    }
                    if(year.length() == 2){
                        year = "20"+year;
                    }
                    if(hour.length() == 1){
                        hour = "0"+hour;
                    }
                    if(min.length() == 1){
                        min = "0"+min;
                    }
                    if(second.length() == 1){
                        second = "0"+second;
                    }

                    txnDateTime = date + "/" + month + "/" + year + " " + hour + ":" + min + ":" + second;
                    binding.pumpNumberTextView.setText(pumpNo);
                    txnListModalArrayList.add(new TxnListModel(qty,getProductNameById(product),netAmount));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        binding.txnListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        TxnListRecyclerAdapter adapter = new TxnListRecyclerAdapter(getActivity(),txnListModalArrayList,this);
        binding.txnListRecyclerView.setAdapter(adapter);

        binding.pumpLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new PumpFragment());
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onPayBtnClick(int position) {
        Helper.txnListPostionSelected = position;
        Log.d("positionClick", String.valueOf(position));
        Bundle bundle = new Bundle();
        bundle.putInt("index", position);
        ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, new OnlineSingleTransactionFragment());
    }
}