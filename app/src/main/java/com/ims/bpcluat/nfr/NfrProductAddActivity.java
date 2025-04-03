package com.ims.bpcluat.nfr;

import static com.ims.bpcluat.Helper.txnAmountUpToTwoDecimal;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.ims.bpcluat.databinding.ActivityNfrProductAddBinding;
import com.ims.bpcluat.model.NfrModel;
import com.ims.bpcluat.validation.DecimalDigitsInputFilter;

public class NfrProductAddActivity extends AppCompatActivity {
    ActivityNfrProductAddBinding binding;
    NfrModel nfrModel = new NfrModel();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNfrProductAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        String productName = getIntent().getStringExtra("productName");
        binding.nfrProductName.setText(productName);

        Intent intent = getIntent();
        String value = intent.getStringExtra("productName");
        binding.nfrProductName.setText(value);
        binding.nfrProductAmt.setFilters(new InputFilter[] { new DecimalDigitsInputFilter(2,100000) });
        binding.nfrProductQty.setFilters(new InputFilter[] { new DecimalDigitsInputFilter(0,100) });

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        binding.toolbar.cartIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NfrProductAddActivity.this, NfrCartActivity.class);
                startActivity(intent);
            }
        });

        SharedPreferences shared = getSharedPreferences("nfrSharedPreferencesData", MODE_PRIVATE);
        String nfrProductName = (shared.getString("nfrProductName",""));
        String nfrProductQty = (shared.getString("nfrProductQty",""));
        String nfrProductAmt = (shared.getString("nfrProductAmt",""));
        if (TextUtils.isEmpty(nfrProductName) && TextUtils.isEmpty(nfrProductQty) && TextUtils.isEmpty(nfrProductAmt)) {
            binding.toolbar.cartIcon.setVisibility(View.GONE);
        }

        binding.nfrProductAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String productName = binding.nfrProductName.getText().toString().trim();
                String productAmt = binding.nfrProductAmt.getText().toString().trim();
                String productQty = binding.nfrProductQty.getText().toString().trim();

                if(!productName.isEmpty() && !productAmt.isEmpty() && !productQty.isEmpty()){
                    binding.nfrProductName.setError(null);
                    binding.nfrProductQty.setError(null);
                    binding.nfrProductAmt.setError(null);

                    int value1 = Integer.parseInt(productQty);
                    double value2 = Double.parseDouble(productAmt);
                    try{
                        if (value1 > 0 && value2 > 0) {
                            // Both values are greater than zero
                            productAmt = txnAmountUpToTwoDecimal(productAmt);
                            SharedPreferences sharedPreferences = getSharedPreferences("nfrSharedPreferencesData", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            if(nfrProductName.isEmpty()){
                                editor.putString("nfrProductName",productName);
                                editor.putString("nfrProductQty",productQty);
                                editor.putString("nfrProductAmt",productAmt);
                                editor.commit();
                            }else{
                                String t1 = nfrProductName + ","+ productName;
                                String t2 = nfrProductQty + ","+ productQty;
                                String t3 = nfrProductAmt + ","+ productAmt;
                                editor.putString("nfrProductName",t1);
                                editor.putString("nfrProductQty",t2);
                                editor.putString("nfrProductAmt",t3);
                                editor.commit();
                            }

                            Intent nextActivityIntent = new Intent(NfrProductAddActivity.this,NfrCartActivity.class);
                            nextActivityIntent.putExtra("nfrModel",nfrModel);
                            startActivity(nextActivityIntent);
                            finish();
                        } else {
                            // One or both values are zero or less
                            if (value1 <= 0) {
                                binding.nfrProductQty.setError("Please enter a value greater than zero");
                                binding.nfrProductQty.requestFocus();
                            }
                            if (value2 <= 0) {
                                binding.nfrProductAmt.setError("Please enter a value greater than zero");
                                binding.nfrProductAmt.requestFocus();
                            }
                        }
                    }catch (NumberFormatException e){
                        Toast.makeText(NfrProductAddActivity.this, "Pleas enter valid numbers", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    if(productName.isEmpty()){
                        binding.nfrProductName.setError("Please enter product name");
                        binding.nfrProductName.requestFocus();
                    }

                    if(productQty.isEmpty()){
                        binding.nfrProductQty.setError("Please enter qty");
                        binding.nfrProductQty.requestFocus();
                    }

                    if(productAmt.isEmpty()){
                        binding.nfrProductAmt.setError("Please enter amount");
                        binding.nfrProductAmt.requestFocus();
                    }
                }
            }
        });
    }
}