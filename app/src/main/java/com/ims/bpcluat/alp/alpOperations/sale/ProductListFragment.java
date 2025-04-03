package com.ims.bpcluat.alp.alpOperations.sale;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.adapter.alp_adapters.ProductAdapter;
import com.ims.bpcluat.databinding.FragmentSmartPayProductBinding;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProductModel;

import java.util.ArrayList;

public class ProductListFragment extends Fragment {
    FragmentSmartPayProductBinding binding;
    RecyclerView recyclerView;
    Context context;
    ProductAdapter productAdapter;
    ArrayList<ProductModel> productModelArrayList = new ArrayList<>();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSmartPayProductBinding.inflate(inflater, container, false);
        context = getContext();

        Bundle bundle = getArguments();
        if (bundle != null) {
            productModelArrayList = (ArrayList<ProductModel>) bundle.getSerializable("productList");
        }

        binding.toolbar.backButton.setOnClickListener( view ->
                ((SideBarActivity) requireActivity()).loadFragement(new SaleFragment())
        );

        recyclerView = binding.productList;
        productAdapter = new ProductAdapter(productModelArrayList, context);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(productAdapter);

        return binding.getRoot();
    }

}