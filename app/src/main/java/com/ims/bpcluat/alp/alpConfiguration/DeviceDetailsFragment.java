package com.ims.bpcluat.alp.alpConfiguration;

import static com.ims.bpcluat.Helper.appVersion;


import android.os.Bundle;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.databinding.FragmentConfigurationsBinding;
import com.ims.bpcluat.databinding.FragmentDeviceDetailsBinding;

public class DeviceDetailsFragment extends Fragment {
    FragmentDeviceDetailsBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDeviceDetailsBinding.inflate(inflater, container, false);

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((SideBarActivity) requireActivity()).loadFragement(new ConfigurationsFragment());

            }
        });

        binding.appversion.setText(appVersion);

        binding.hardwareid.setText(Helper.serialNumber);


        return binding.getRoot();
    }
}