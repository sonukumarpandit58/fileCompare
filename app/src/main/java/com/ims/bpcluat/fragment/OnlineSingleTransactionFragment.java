package com.ims.bpcluat.fragment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.ims.bpcluat.Helper;
import com.ims.bpcluat.PaymentActivity;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.conversion.HexToDecimal;
import com.ims.bpcluat.databinding.FragmentOnlineSingleTransactionBinding;
import com.ims.bpcluat.model.OnlineTxnModel;

import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.getPresetType;
import static com.ims.bpcluat.Helper.getProductNameById;
import static com.ims.bpcluat.Helper.txnArrayList;
import static com.ims.bpcluat.Helper.txnReleatedPumpNo;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class OnlineSingleTransactionFragment extends Fragment{
    FragmentOnlineSingleTransactionBinding binding;
    OnlineTxnModel onlineTxnModel = new OnlineTxnModel();
    Context context;
    ProgressDialog progress;
    int index = 0;
    ArrayList<OnlineTxnModel> onlineTxnModelArrayList = new ArrayList<>();
    Date date = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
    String todayDate = formatter.format(date);

    public OnlineSingleTransactionFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOnlineSingleTransactionBinding.inflate(inflater, container, false);
        context = getActivity();
        binding.pumpNumberTextView.setText(txnReleatedPumpNo);

        fileWrite(context, todayDate + ".txt", "Landing Page : ","Single Transaction Screen");

        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);
        progress.show();

        Bundle bundle = getArguments();
        if (bundle != null) {
            index = getArguments().getInt("index");
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (progress.isShowing()) {
                    progress.dismiss();
                }
            }
        }, 10000);

        if (txnArrayList.size() > 0) {
            Object myobj = txnArrayList.get(index);
            Gson gson = new Gson();
            String json = gson.toJson(myobj);
            try {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonObject = new JSONObject(json);
                            String pumpNo = HexToDecimal.convert(jsonObject.getString("PumpNumber"));
                            String endDate = HexToDecimal.convert(jsonObject.getString("Day"));
                            String endMonth = HexToDecimal.convert(jsonObject.getString("Month"));
                            String endYear = HexToDecimal.convert(jsonObject.getString("Year"));
                            String endHour = HexToDecimal.convert(jsonObject.getString("Hour"));
                            String endMin = HexToDecimal.convert(jsonObject.getString("Minute"));
                            String endSecond = HexToDecimal.convert(jsonObject.getString("Second"));
                            String startDate = HexToDecimal.convert(jsonObject.getString("TxnStartDay"));
                            String startMonth = HexToDecimal.convert(jsonObject.getString("TxnStartMonth"));
                            String startYear = HexToDecimal.convert(jsonObject.getString("TxnStartYear"));
                            String startHour = HexToDecimal.convert(jsonObject.getString("TxnStartHour"));
                            String startMin = HexToDecimal.convert(jsonObject.getString("TxnStartMinute"));
                            String startSecond = HexToDecimal.convert(jsonObject.getString("TxnStartSecond"));
                            String netAmount = HexToDecimal.convertAmount(jsonObject.getString("NetAmount"));
                            String qty = HexToDecimal.convertLitre(jsonObject.getString("Volume"));
                            String product = HexToDecimal.convertProduct(jsonObject.getString("ProductID"));
                            String unitPrice = HexToDecimal.convertLitre(jsonObject.getString("ProductPrice"));
                            String txnEndDateTime = "";
                            String txnStartDateTime = "";
                            String presetType = HexToDecimal.convert(jsonObject.getString("TxnPresetType"));
                            String presetValue = HexToDecimal.convertAmount(jsonObject.getString("TxnPresetValue"));
                            String charegeslipBayNo = HexToDecimal.convert(jsonObject.getString("PumpNumber"));
                            String chargeslipNozzleNo = HexToDecimal.convert(jsonObject.getString("NozzleNumber"));

                            if (endDate.length() == 1) {
                                endDate = "0" + endDate;
                            }
                            if (endMonth.length() == 1) {
                                endMonth = "0" + endMonth;
                            }
                            if (endYear.length() == 2) {
                                endYear = "20" + endYear;
                            }
                            if (endHour.length() == 1) {
                                endHour = "0" + endHour;
                            }
                            if (endMin.length() == 1) {
                                endMin = "0" + endMin;
                            }
                            if (endSecond.length() == 1) {
                                endSecond = "0" + endSecond;
                            }

                            txnEndDateTime = endDate + "/" + endMonth + "/" + endYear + " " + endHour + ":" + endMin + ":" + endSecond;

                            if (startDate.length() == 1) {
                                startDate = "0" + startDate;
                            }
                            if (startMonth.length() == 1) {
                                startMonth = "0" + startMonth;
                            }
                            if (startYear.length() == 2) {
                                startYear = "20" + startYear;
                            }
                            if (startHour.length() == 1) {
                                startHour = "0" + startHour;
                            }
                            if (startMin.length() == 1) {
                                startMin = "0" + startMin;
                            }
                            if (startSecond.length() == 1) {
                                startSecond = "0" + startSecond;
                            }

                            txnStartDateTime = startDate + "/" + startMonth + "/" + startYear + " " + startHour + ":" + startMin + ":" + startSecond;

                            onlineTxnModel.setPresetType(getPresetType(presetType));
                            onlineTxnModel.setPresetValue(presetValue);
                            onlineTxnModel.setCharegeslipBayNo(charegeslipBayNo);
                            onlineTxnModel.setChargeslipNozzleNo(chargeslipNozzleNo);
                            onlineTxnModel.setTxnStartDateTime(txnStartDateTime);
                            onlineTxnModel.setTxnEndDateTime(txnEndDateTime);
                            onlineTxnModel.setPumpNo(pumpNo);
                            onlineTxnModel.setAmount(netAmount);
                            onlineTxnModel.setQty(qty);
                            onlineTxnModel.setProductId(product);
                            onlineTxnModel.setProductName(getProductNameById(product));
                            onlineTxnModel.setUnitPrice(unitPrice);

                            binding.pumpNumberTextView.setText(pumpNo);
                            binding.amount.setText(getString(R.string.rupees_symbol) + " " + netAmount);
                            binding.txnDateTime.setText(txnEndDateTime);
                            binding.qty.setText("Qty(ltr) : " + qty);
                            binding.product.setText("Product : " + getProductNameById(product));

                            binding.linear.setVisibility(View.VISIBLE);
                          //  fileWrite(context,todayDate+".txt",response);
                              progress.dismiss();
                        } catch (JSONException e) {
                            Log.d("JSONException11", e.toString());
                        }
                    }
                });

            } catch (NullPointerException exception) {
                exception.printStackTrace();
            }
        }

        binding.fetchMoreTxn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new OnlineTxnListFragment());
            }
        });

        binding.pumpLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileWrite(context, todayDate + ".txt", "Redirect to : ","Pump Fragment");
                ((SideBarActivity) requireActivity()).loadFragement(new PumpFragment());
            }
        });

        binding.onlineTxnPayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.txnListPostionSelected = index;
                Intent intent = new Intent(getActivity(), PaymentActivity.class);
                onlineTxnModel.setIsTxnOnline("yes");
                intent.putExtra("onlineTxnModel", onlineTxnModel);
                startActivity(intent);
            }
        });
        return binding.getRoot();
    }

    public void noTxnFoundAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("No Transaction Found");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(getActivity(),SideBarActivity.class);
                startActivity(intent);
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}