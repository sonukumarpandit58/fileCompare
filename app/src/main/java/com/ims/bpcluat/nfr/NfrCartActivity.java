package com.ims.bpcluat.nfr;

import static com.ims.bpcluat.Helper.txnAmountUpToTwoDecimal;

import static java.security.AccessController.getContext;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.ims.bpcluat.Helper;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.adapter.NfrCartRecyclerViewAdapter;
import com.ims.bpcluat.adapter.TxnHistoryRecyclerViewAdapter;
import com.ims.bpcluat.databinding.ActivityMainBinding;
import com.ims.bpcluat.databinding.ActivityNfrCartBinding;
import com.ims.bpcluat.interfaces.NfrCartInterface;
import com.ims.bpcluat.model.NfrCartModel;
import com.ims.bpcluat.model.NfrModel;

import java.lang.reflect.Type;

import com.google.gson.reflect.TypeToken;
import com.ims.bpcluat.model.TxnHistoryModel;

import org.json.JSONArray;

import java.util.ArrayList;

public class NfrCartActivity extends AppCompatActivity implements NfrCartInterface {

    ActivityNfrCartBinding binding;
    ArrayList<NfrCartModel> cartModelArrayList = new ArrayList<>();
    NfrCartRecyclerViewAdapter adapter;
    String totalAmountStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNfrCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        cartDataSet();
        binding.addProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NfrCartActivity.this, SideBarActivity.class);
                intent.putExtra("redirect", "NfrFragment");
                startActivity(intent);
            }
        });

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NfrCartActivity.this, SideBarActivity.class);
                intent.putExtra("redirect", "NfrFragment");
                startActivity(intent);
            }
        });

        binding.proceedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NfrCartActivity.this, NfrPaymentActivity.class);
                startActivity(intent);
            }
        });
    }

    private void cartDataSet() {
        SharedPreferences shared = getSharedPreferences("nfrSharedPreferencesData", MODE_PRIVATE);
        String nfrProductName = (shared.getString("nfrProductName", ""));
        String nfrProductQty = (shared.getString("nfrProductQty", ""));
        String nfrProductAmt = (shared.getString("nfrProductAmt", ""));

        Log.d("nfrProductName",nfrProductName);
        Log.d("nfrProductQty",nfrProductQty);
        Log.d("nfrProductAmt",nfrProductAmt);

        if (!TextUtils.isEmpty(nfrProductName) && !TextUtils.isEmpty(nfrProductQty) && !TextUtils.isEmpty(nfrProductAmt)) {
            String[] nfrProductNameArray = nfrProductName.split(",");
            String[] nfrProductQtyArray = nfrProductQty.split(",");
            String[] nfrProductAmtArray = nfrProductAmt.split(",");

            double totalAmount = 0.00;
            int totalQty = 0;

            if (nfrProductNameArray.length == 1) {
                cartModelArrayList.add(new NfrCartModel(nfrProductNameArray[0], nfrProductAmtArray[0], nfrProductQtyArray[0]));
                totalAmount = Double.parseDouble(nfrProductQtyArray[0]) * Double.parseDouble(nfrProductAmtArray[0]);
                totalQty = Integer.parseInt(nfrProductQtyArray[0]);
            } else {
                for (int i = 0; i < nfrProductNameArray.length; i++) {
                    cartModelArrayList.add(new NfrCartModel(nfrProductNameArray[i], nfrProductAmtArray[i], nfrProductQtyArray[i]));
                    Double multiplyCal = Double.parseDouble(nfrProductQtyArray[i]) * Double.parseDouble(nfrProductAmtArray[i]);
                    totalAmount = totalAmount + multiplyCal;
                    totalQty = totalQty + Integer.parseInt(nfrProductQtyArray[i]);
                }
            }

            if (totalAmount > 100000) {
                // Code to execute if the amount is greater than 100000
                binding.proceedButton.setEnabled(false);
                binding.proceedButton.setVisibility(View.VISIBLE);
                binding.proceedButton.setBackgroundColor(ContextCompat.getColor(this, R.color.btnGrayy));
                binding.totalAmountErrorMsg.setVisibility(View.VISIBLE);
            } else {
                // Code to execute if the amount is not greater than 100000
                binding.proceedButton.setEnabled(true);
                binding.proceedButton.setBackgroundColor(ContextCompat.getColor(this, R.color.topBar));
                binding.totalAmountErrorMsg.setVisibility(View.GONE);
            }

            totalAmountStr = txnAmountUpToTwoDecimal(Double.toString(totalAmount));
            binding.totalAmount.setText("Total Amount " + totalAmountStr);
            SharedPreferences.Editor editor = shared.edit();
            editor.putString("totalAmount", totalAmountStr);
            editor.putString("totalQty", String.valueOf(totalQty));
            editor.commit();

            binding.cartRecycler.setLayoutManager(new LinearLayoutManager(this));
            adapter = new NfrCartRecyclerViewAdapter(this, cartModelArrayList, this);
            binding.cartRecycler.setAdapter(adapter);
            binding.proceedButton.setVisibility(View.VISIBLE);
        } else {
            totalAmountStr = "0.00";
            binding.totalAmount.setText("Total Amount " + totalAmountStr);
            SharedPreferences.Editor editor = shared.edit();
            editor.putString("totalAmount", "0.00");
            editor.putString("totalQty", "0");
            editor.commit();
            binding.proceedButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDeleteClick(int position) {
        SharedPreferences shared = getSharedPreferences("nfrSharedPreferencesData", MODE_PRIVATE);
        String nfrProductName = (shared.getString("nfrProductName", ""));
        String nfrProductQty = (shared.getString("nfrProductQty", ""));
        String nfrProductAmt = (shared.getString("nfrProductAmt", ""));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.print_popup, null);
        builder.setView(dialogView);

        AlertDialog alertDialog = builder.create();
        TextView alertMessage = dialogView.findViewById(R.id.alert_title);
        alertMessage.setText("Are you sure, you want to delete the product from the list?");
        // Customize dialog buttons if needed
        Button noBtn = dialogView.findViewById(R.id.alert_cancel_button);
        Button yesBtn = dialogView.findViewById(R.id.alert_ok_button);

        noBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                String updateName = removeElementAtIndex(nfrProductName, position);
                String updateQty = removeElementAtIndex(nfrProductQty, position);
                String updateAmt = removeElementAtIndex(nfrProductAmt, position);
                SharedPreferences.Editor editor = shared.edit();
                editor.putString("nfrProductName", updateName);
                editor.putString("nfrProductQty", updateQty);
                editor.putString("nfrProductAmt", updateAmt);
                editor.commit();
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        });

        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    @Override
    public void onPriceClick(int position, EditText editText) {
        Log.d("sonu1", "price");
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
                Log.d("beforeTextChanged","beforeTextChanged");
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Update data list if needed
                Log.d("onTextChanged","onTextChanged");
                cartModelArrayList.get(position).setPrice(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Do nothing
                Log.d("afterTextChanged","afterTextChanged");
               // updatePrice(position,s.toString());
            }
        };
        editText.addTextChangedListener(textWatcher);
    }

    @Override
    public void onPlusClick(int position) {
        Helper.closeKeyboard(this);
        String qty = cartModelArrayList.get(position).getQty();
        int qtyInt = Integer.parseInt(qty); // Convert string to integer
        if (qtyInt < 99) {
            qtyInt = qtyInt + 1;          // Subtract one
            qty = Integer.toString(qtyInt); // Convert integer back to string
            adapter.updateQty(position, qty);
            updateQty(position,qty);
        }
    }

    @Override
    public void onMinusClick(int position) {
        Helper.closeKeyboard(this);
        String qty = cartModelArrayList.get(position).getQty();
        int qtyInt = Integer.parseInt(qty); // Convert string to integer
        if (qtyInt > 1) {
            qtyInt = qtyInt - 1;          // Subtract one
            qty = Integer.toString(qtyInt); // Convert integer back to string
            adapter.updateQty(position, qty);
            updateQty(position,qty);
        }
    }

    public static String removeElementAtIndex(String input, int index) {
        String[] elements = input.split(",");
        if (index < 0 || index >= elements.length) {
            throw new IllegalArgumentException("Index out of bounds");
        }
        if (elements.length == 1) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < elements.length; i++) {
            if (i != index) {
                if (result.length() > 0) {
                    result.append(",");
                }
                result.append(elements[i]);
            }
        }
        return result.toString();
    }

    private void updateQty(int index, String newValue) {
        SharedPreferences shared = getSharedPreferences("nfrSharedPreferencesData", MODE_PRIVATE);
        String nfrProductName = (shared.getString("nfrProductName", ""));
        String nfrProductQty = (shared.getString("nfrProductQty", ""));
        String nfrProductAmt = (shared.getString("nfrProductAmt", ""));
        String[] parts = nfrProductQty.split(",");

        // Check if the index is valid
        if (index >= 0 && index < parts.length) {
            // Update the value at the specified index
            parts[index] = newValue;
        } else if (index == 0 && parts.length == 1) {
            // Special case: if there's only one value and index is 0
            parts[0] = newValue;
        } else {
            throw new IndexOutOfBoundsException("Index out of bounds for the given qty string.");
        }
        // Join the parts back into a single string

        String updatedQty = String.join(",", parts);

        SharedPreferences.Editor editor = shared.edit();
        editor.putString("nfrProductName", nfrProductName);
        editor.putString("nfrProductQty", updatedQty);
        editor.putString("nfrProductAmt", nfrProductAmt);
        editor.commit();
        adapter.clearData();
        cartDataSet();
    }

    private void updatePrice(int index, String newValue) {
        SharedPreferences shared = getSharedPreferences("nfrSharedPreferencesData", MODE_PRIVATE);
        String nfrProductName = (shared.getString("nfrProductName", ""));
        String nfrProductQty = (shared.getString("nfrProductQty", ""));
        String nfrProductAmt = (shared.getString("nfrProductAmt", ""));
        String[] parts = nfrProductAmt.split(",");

        // Check if the index is valid
        if (index >= 0 && index < parts.length) {
            // Update the value at the specified index
            parts[index] = newValue;
        } else if (index == 0 && parts.length == 1) {
            // Special case: if there's only one value and index is 0
            parts[0] = newValue;
        } else {
            throw new IndexOutOfBoundsException("Index out of bounds for the given qty string.");
        }
        // Join the parts back into a single string

        String updatedPrice = String.join(",", parts);

        SharedPreferences.Editor editor = shared.edit();
        editor.putString("nfrProductName", nfrProductName);
        editor.putString("nfrProductQty", nfrProductQty);
        editor.putString("nfrProductAmt", updatedPrice);
        editor.commit();
        adapter.clearData();
        cartDataSet();
    }

}