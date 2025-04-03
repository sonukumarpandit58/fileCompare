package com.ims.bpcluat.alp.alpOperations.utility;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.alp.alpOperations.AlpOperationsFragment;
import com.ims.bpcluat.alp.alpOperations.utility.DuplicateShiftSummary.DuplicateShiftSummaryFragment;
import com.ims.bpcluat.alp.alpOperations.utility.ShiftSummary.ShiftSummaryFragment;
import com.ims.bpcluat.alp.alpOperations.utility.alpVoid.AlpVoidFragment;
import com.ims.bpcluat.alp.alpOperations.utility.reprint.ReprintMobileNumber;
import com.ims.bpcluat.databinding.FragmentUtilityBinding;

public class UtilityFragment extends Fragment {
    FragmentUtilityBinding binding;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentUtilityBinding.inflate(inflater, container, false);

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new AlpOperationsFragment());

            }
        });

        binding.voidBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new AlpVoidFragment());
            }
        });

        binding.reprintBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new ReprintMobileNumber());
            }
        });

        binding.shiftsummaryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new ShiftSummaryFragment());
            }
        });

        binding.duplicateshiftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new DuplicateShiftSummaryFragment());
            }
        });

        return binding.getRoot();
    }
}