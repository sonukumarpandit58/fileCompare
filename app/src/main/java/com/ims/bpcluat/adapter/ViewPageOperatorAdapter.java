package com.ims.bpcluat.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.ims.bpcluat.fragment.ActiveOperatorFragment;
import com.ims.bpcluat.fragment.InActiveOperatorFragment;
import com.ims.bpcluat.fragment.TxnHistoryFragment;
import com.ims.bpcluat.fragment.TxnSummaryFragment;

public class ViewPageOperatorAdapter extends FragmentStateAdapter {
    public ViewPageOperatorAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if(position == 0){
            return new ActiveOperatorFragment();
        }else{
            return new InActiveOperatorFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // no of tabs
    }

}
