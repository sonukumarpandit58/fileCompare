package com.ims.bpcluat.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import com.ims.bpcluat.R;
import com.ims.bpcluat.adapter.ViewPageOperatorAdapter;
import com.ims.bpcluat.adapter.ViewPageTxnAdapter;
import com.ims.bpcluat.databinding.FragmentAppInfoBinding;
import com.ims.bpcluat.databinding.FragmentOperatorViewAddBinding;

public class OperatorViewAddFragment extends Fragment {

    FragmentOperatorViewAddBinding binding;

    public OperatorViewAddFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOperatorViewAddBinding.inflate(inflater, container, false);
        ViewPageOperatorAdapter adapter = new ViewPageOperatorAdapter(getActivity());
        binding.viewPager.setAdapter(adapter);
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                binding.viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                binding.tabLayout.getTabAt(position).select();
            }
        });
        return binding.getRoot();
    }
}