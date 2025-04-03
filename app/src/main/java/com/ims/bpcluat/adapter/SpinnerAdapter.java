package com.ims.bpcluat.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

public class SpinnerAdapter extends ArrayAdapter<String> {
    private int hidingItemIndex;
    public SpinnerAdapter(@NonNull Context context, int resource, List<String> objects, int hidingItemIndex) {
        super(context, resource,objects);
        this.hidingItemIndex = hidingItemIndex;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = null;
        if(position == hidingItemIndex){
            TextView textView = new TextView(getContext());
            textView.setVisibility(View.GONE);
            view = textView;
        }else{
            view = super.getDropDownView(position, null, parent);
        }
        return view;
    }
}


