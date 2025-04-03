package com.ims.bpcluat.alp.alpOperations.sale;

import static com.ims.bpcluat.Helper.cngHomePage;
import static com.ims.bpcluat.utils.Navigation.BackWithData;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.ims.bpcluat.PaymentActivity;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.alp.alpOperations.sale.loyaltyotp.MobileNumberFragment;
import com.ims.bpcluat.cng.CngFragment;
import com.ims.bpcluat.cng.CngPaymentActivity;
import com.ims.bpcluat.cng.CngTxnFailedActivity;
import com.ims.bpcluat.databinding.ActivityCngTxnFailedBinding;
import com.ims.bpcluat.databinding.FragmentSaleBinding;
import com.ims.bpcluat.databinding.FragmentTxnFailBinding;
import com.ims.bpcluat.model.AlpModels.SaleModel.VirtualCardProgramModel;
import com.ims.bpcluat.model.CngModel;
import com.ims.bpcluat.model.NfrModel;
import com.ims.bpcluat.model.OnlineTxnModel;
import com.ims.bpcluat.nfr.NfrFragment;
import com.ims.bpcluat.nfr.NfrPaymentActivity;

import java.util.ArrayList;

public class TxnFailFragment extends Fragment {
    FragmentTxnFailBinding binding;
    private CngModel cngModel;
    String amount = "", mobileNum;
    private OnlineTxnModel onlineTxnModel;
    private NfrModel nfrModel;
    String isTxnOnline ="";
    String txnStatus = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTxnFailBinding.inflate(getLayoutInflater());
        DrawerLayout drawerLayout = getActivity().findViewById(R.id.drawerLayout);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        hideKeyboard();

        Bundle bundle = getArguments();
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
        if (bundle != null) {
            cngModel = getArguments().getParcelable("cngModel");
            onlineTxnModel = getArguments().getParcelable("onlineTxnModel");
            nfrModel = getArguments().getParcelable("nfrModel");

            txnStatus = getArguments().getString("txnStatus");
            if(txnStatus != null || !txnStatus.isEmpty()) {
                if (txnStatus.equals("Transaction Cancelled!")) {
                    txnStatus = "Transaction Cancelled!";
                } else if (txnStatus.equals("Transaction TimeOut!")) {
                    txnStatus = "Transaction TimeOut!";
                } else {
                    txnStatus = "Transaction Failed!";
                }
            }

            binding.textView3.setText(txnStatus);
        }

        if(onlineTxnModel !=null){
            isTxnOnline = onlineTxnModel.getIsTxnOnline();
        }

        binding.retryalpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cngModel != null) {
                    Intent intent = new Intent(getActivity(), CngPaymentActivity.class);
                    intent.putExtra("cngModel", cngModel);
                    intent.putExtra("Insertcard", "Insertcard");
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }

                } else if (onlineTxnModel != null) {
                    if(isTxnOnline.equals("no")){
                        Intent intent = new Intent(getActivity(), PaymentActivity.class);
                        intent.putExtra("onlineTxnModel", onlineTxnModel);
                        intent.putExtra("isTxnOnline", "isTxnOnline");
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        if (getActivity() != null) {
                            getActivity().finish();
                        }
                        startActivity(intent);
                    }else{
                        Intent intent = new Intent(getActivity(), PaymentActivity.class);
                        intent.putExtra("onlineTxnModel", onlineTxnModel);
                        intent.putExtra("Insertcard", "Insertcard");
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        if (getActivity() != null) {
                            getActivity().finish();
                        }
                        startActivity(intent);
                    }
                } else if (nfrModel != null) {
                    Intent intent = new Intent(getActivity(), NfrPaymentActivity.class);
                    intent.putExtra("nfrModel", nfrModel);
                    intent.putExtra("Insertcard", "Insertcard");
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                    startActivity(intent);
                }
            }
        });

        binding.homealpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BackWithData(getContext(), cngModel, nfrModel, onlineTxnModel);
            }
        });

        return binding.getRoot();
    }

    public void hideKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

}