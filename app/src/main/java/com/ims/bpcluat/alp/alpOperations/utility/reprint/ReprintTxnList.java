package com.ims.bpcluat.alp.alpOperations.utility.reprint;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.adapter.alp_adapters.ProductAdapter;
import com.ims.bpcluat.adapter.alp_adapters.ReprintTxnAdapter;
import com.ims.bpcluat.alp.alpOperations.sale.SaleFragment;
import com.ims.bpcluat.databinding.FragmentReprintTxnListBinding;
import com.ims.bpcluat.model.AlpModels.ReprintTxnModel;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProductModel;

import java.util.ArrayList;

public class ReprintTxnList extends Fragment {
    FragmentReprintTxnListBinding binding;
    RecyclerView recyclerView;
    ReprintTxnAdapter reprintTxnAdapter;
    ArrayList<ReprintTxnModel> reprintTxnModelArrayList = new ArrayList<>();
    Context context;
    String txnId = "";

    public ReprintTxnList() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReprintTxnListBinding.inflate(inflater, container, false);
        context = getContext();
        binding.toolbar.backButton.setOnClickListener( view ->
                ((SideBarActivity) requireActivity()).loadFragement(new ReprintOtpFragment())
        );

        Bundle bundle = getArguments();
        if (bundle != null) {
            reprintTxnModelArrayList = (ArrayList<ReprintTxnModel>) bundle.getSerializable("reprintTxnModelList");
        }

        recyclerView = binding.reprintList;
        reprintTxnAdapter = new ReprintTxnAdapter(context, reprintTxnModelArrayList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(reprintTxnAdapter);

        return binding.getRoot();
    }
}