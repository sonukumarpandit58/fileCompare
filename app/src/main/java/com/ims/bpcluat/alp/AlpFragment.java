package com.ims.bpcluat.alp;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.alp.alpConfiguration.ConfigurationsFragment;
import com.ims.bpcluat.alp.alpOperations.AlpOperationsFragment;
import com.ims.bpcluat.databinding.FragmentAlpBinding;

public class AlpFragment extends Fragment {
    FragmentAlpBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentAlpBinding.inflate(inflater, container, false);


        binding.configurationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new ConfigurationsFragment());
            }
        });

        binding.operationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((SideBarActivity) requireActivity()).loadFragement(new AlpOperationsFragment());
            }
        });
        return binding.getRoot();
    }
}