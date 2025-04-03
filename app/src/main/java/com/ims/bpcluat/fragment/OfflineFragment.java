package com.ims.bpcluat.fragment;

import static com.ims.bpcluat.Helper.*;
import static com.ims.bpcluat.helper.OfflineProductHelper.getProductName;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.PaymentActivity;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.SuccessActivity;
import com.ims.bpcluat.databinding.FragmentOfflineBinding;
import com.ims.bpcluat.databinding.FragmentPumpBinding;
import com.ims.bpcluat.model.OnlineTxnModel;
import com.ims.bpcluat.validation.DecimalDigitsInputFilter;
import com.ims.bpcluat.validation.DecimalDigitsWithoutMaxValue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OfflineFragment extends Fragment {
    FragmentOfflineBinding binding;
    public static List<String> productArray = new ArrayList<>();
    OnlineTxnModel onlineTxnModel = new OnlineTxnModel();
    public OfflineFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentOfflineBinding.inflate(inflater, container, false);
        binding.offlineAmount.setFilters(new InputFilter[] { new DecimalDigitsWithoutMaxValue() });
        productArray.clear();
        String productList = metaHosProduct;
        try{
            JSONArray jsonArray = new JSONArray(productList);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                int localProductID = jsonObject.getInt("localProductID");
                String productName = getProductName(localProductID);
                Log.d("localProductID", String.valueOf(localProductID));
                Log.d("productName",productName);
                productArray.add(productName);
            }
        }catch (JSONException e){

        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, productArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.productSpinner.setAdapter(adapter);

        binding.proceedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String offlineProduct = binding.productSpinner.getSelectedItem().toString();
                String offlineAmount = binding.offlineAmount.getText().toString().trim();
                if(offlineAmount.isEmpty()){
                    binding.offlineAmount.setError("Please enter amount");
                    binding.offlineAmount.requestFocus();
                }else{
                    String selectedProductId = getProductId(offlineProduct,fuelProductList);
                    String unitPrice = getPriceByLocalProductID(metaHosProduct,selectedProductId);
                    String quantity = calculateFuelQuantity(unitPrice, offlineAmount);

                    double amountValue = Double.parseDouble(offlineAmount);
                    if (amountValue > 0) {
                        onlineTxnModel.setPumpNo("");
                        onlineTxnModel.setNozzleNo("");
                        onlineTxnModel.setQty(quantity);
                        onlineTxnModel.setProductId("");
                        onlineTxnModel.setProductName(offlineProduct);
                        onlineTxnModel.setAmount(offlineAmount);
                        onlineTxnModel.setIsTxnOnline("no");
                        onlineTxnModel.setLocalMPDId("");
                        onlineTxnModel.setUnitPrice(unitPrice);
                        onlineTxnModel.setTxnId(createTxnIdForOfflineTxn());
                        onlineTxnModel.setPresetType("");
                        onlineTxnModel.setTxnStartDateTime("");
                        onlineTxnModel.setTxnEndDateTime("");
                        Intent intent = new Intent(getActivity(), PaymentActivity.class);
                        intent.putExtra("onlineTxnModel", onlineTxnModel);
                        startActivity(intent);
                    } else {
                        binding.offlineAmount.setError("Entered amount must be greater than zero");
                        binding.offlineAmount.requestFocus();
                    }
                }
            }
        });
        return binding.getRoot();
    }

}