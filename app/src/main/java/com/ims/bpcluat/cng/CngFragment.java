package com.ims.bpcluat.cng;

import static com.ims.bpcluat.Helper.fuelProductList;
import static com.ims.bpcluat.Helper.getProductId;
import static com.ims.bpcluat.Helper.txnAmountUpToTwoDecimal;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.ims.bpcluat.R;
import com.ims.bpcluat.databinding.FragmentCngBinding;
import com.ims.bpcluat.model.CngModel;
import com.ims.bpcluat.validation.DecimalDigitsInputFilter;

import java.math.BigDecimal;

public class CngFragment extends Fragment {

    FragmentCngBinding binding;
    CngModel cngModel = new CngModel();

    public CngFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCngBinding.inflate(inflater, container, false);
        binding.amt.setFilters(new InputFilter[] { new DecimalDigitsInputFilter(2,100000) });
        binding.qty.setFilters(new InputFilter[] { new DecimalDigitsInputFilter(2,100000) });
        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String qty = binding.qty.getText().toString().trim();
                String amt = binding.amt.getText().toString().trim();

                if (qty.isEmpty()) {
                    qty = "1";
//                    binding.qty.setError("Enter Qty");
                }

                if (amt.isEmpty()) {
                    binding.amt.setError("Enter amount");
                }

                if (!qty.isEmpty() && !amt.isEmpty()) {
                    BigDecimal quantity = new BigDecimal(qty);
                    BigDecimal amount = new BigDecimal(amt);
                    BigDecimal threshold = new BigDecimal("100000");
                    BigDecimal zeroCheck = new BigDecimal("0");
                    boolean amtValueOk = false;
                    boolean qtyValueOk = false;

                    if (quantity.compareTo(zeroCheck) <= 0) {
                        qtyValueOk = false;
                        binding.qty.setError("Qty must be greater than zero");
                        binding.qty.requestFocus();
                    } else {
                        if (quantity.compareTo(threshold) >= 0) {
                            qtyValueOk = false;
                            binding.qty.setError("Please enter qty less than 100000");
                            binding.qty.requestFocus();
                        } else {
                            qtyValueOk = true;
                            binding.qty.setError(null); // Clear error
                        }
                    }

                    if (amount.compareTo(zeroCheck) <= 0) {
                        amtValueOk = false;
                        binding.amt.setError("Amount must be greater than zero");
                        binding.amt.requestFocus();
                    }else{
                        if (amount.compareTo(threshold) >= 0) {
                            amtValueOk = false;
                            binding.amt.setError("Please enter amount less than 100000");
                            binding.amt.requestFocus();
                        } else {
                            amtValueOk = true;
                            binding.amt.setError(null); // Clear error
                        }
                    }

                    if (amtValueOk && qtyValueOk) {
                        BigDecimal result = quantity.multiply(amount);
                        String totalAmt = txnAmountUpToTwoDecimal(String.valueOf(result));
                        cngModel.setQty(qty);
                        cngModel.setPerAmt(amt);
                        cngModel.setProductName("CNG");
                        cngModel.setTotalAmt(totalAmt);
                        Intent intent = new Intent(getActivity(), CngPaymentActivity.class);
                        intent.putExtra("cngModel", cngModel);
                        startActivity(intent);
                    }
                }
            }
        });
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}