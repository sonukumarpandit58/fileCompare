package com.ims.bpcluat.fragment;

import static com.ims.bpcluat.Helper.versionDate;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.ims.bpcluat.R;
import com.ims.bpcluat.databinding.FragmentAppInfoBinding;

public class AppInfoFragment extends Fragment {
    FragmentAppInfoBinding binding;
    public AppInfoFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAppInfoBinding.inflate(inflater,container,false);
        try {
            PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            String version = "Version "+ pInfo.versionName;
            binding.appName.setText(R.string.app_name);
            binding.versionName.setText(version);
            binding.versionDate.setText(versionDate);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
        return binding.getRoot();
    }
}