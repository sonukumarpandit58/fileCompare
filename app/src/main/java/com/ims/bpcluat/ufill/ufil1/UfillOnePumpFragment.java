package com.ims.bpcluat.ufill.ufil1;

import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.helper.BleDeviceHelper.retryOneMoreTime;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.databinding.FragmentUfillOnePumpBinding;
import com.ims.bpcluat.helper.BleDeviceHelper;
import com.ims.bpcluat.model.UfillModel;
import com.ims.bpcluat.ufill.VoucherRedeemActivity;

import java.text.SimpleDateFormat;
import java.util.Date;

public class UfillOnePumpFragment extends Fragment {
    FragmentUfillOnePumpBinding binding;
    Context context;
    String pumpNo = "";
    UfillModel ufillModel;
    Date date = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
    String todayDate = formatter.format(date);
    private BleDeviceHelper bleDeviceHelper;

    public UfillOnePumpFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUfillOnePumpBinding.inflate(inflater, container, false);
        context = getActivity();

        fileWrite(context, todayDate + ".txt", "LandingPage : ", "UfillOnePumpFrag");
        bleDeviceHelper = bleDeviceHelper.getInstance(getActivity());
        retryOneMoreTime = false;
        bleDeviceHelper.disconnect();

        if (Helper.pumpArray.size() == 0) {
            binding.duSpinner.setVisibility(View.GONE);
            binding.manuallyPumpNo.setVisibility(View.VISIBLE);
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, Helper.pumpArray);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.duSpinner.setAdapter(adapter);

            binding.duSpinner.setVisibility(View.VISIBLE);
            binding.manuallyPumpNo.setVisibility(View.GONE);
        }

        if (getArguments() != null) {
            ufillModel = getArguments().getParcelable("ufillModel");
        }

        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.duSpinner.getVisibility() == View.VISIBLE) {
                    int selectedItemPosition = binding.duSpinner.getSelectedItemPosition();
                    if (selectedItemPosition == AdapterView.INVALID_POSITION) {
                        // No item is selected
                        Log.d("SpinnerCheck", "No item selected");
                        Helper helper = new Helper();
                        helper.showToastMessage(getActivity(), "Please fetch pump first");
                    } else {
                        Log.d("SpinnerCheck", "Item selected: " + binding.duSpinner.getSelectedItem().toString());
                        String selectedValue = binding.duSpinner.getSelectedItem().toString();
                        int index = selectedValue.indexOf('-');
                        pumpNo = selectedValue.substring(index + 1);
                        if (pumpNo.length() == 1) {
                            pumpNo = "0" + pumpNo;
                        }
                        Intent intent = new Intent(getActivity(), VoucherRedeemActivity.class);
                        ufillModel.setPumpNo(pumpNo);
                        ufillModel.setNozzleNo("00");
                        intent.putExtra("ufillModel",ufillModel);
                        startActivity(intent);
                    }
                }
                if (binding.manuallyPumpNo.getVisibility() == View.VISIBLE) {
                    pumpNo = binding.manuallyPumpNo.getText().toString().trim();
                    if (TextUtils.isEmpty(pumpNo)) {
                        binding.manuallyPumpNo.setError("please enter pump no");
                        binding.manuallyPumpNo.requestFocus();
                    } else {
                        Helper.closeKeyboard(getActivity());
                        if (pumpNo.length() == 1) {
                            pumpNo = "0" + pumpNo;
                        }
                        Helper.closeKeyboard(getActivity());
                        Intent intent = new Intent(getActivity(), VoucherRedeemActivity.class);
                        ufillModel.setPumpNo(pumpNo);
                        ufillModel.setNozzleNo("00");
                        intent.putExtra("ufillModel",ufillModel);
                        startActivity(intent);
                    }
                }
            }
        });

        return binding.getRoot();
    }

    public void onResume() {
        super.onResume();
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
    }


    @Override
    public void onStop() {
        super.onStop();
        ((AppCompatActivity) getActivity()).getSupportActionBar().show();
    }
}