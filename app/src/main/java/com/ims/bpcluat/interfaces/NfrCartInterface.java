package com.ims.bpcluat.interfaces;

import android.widget.EditText;

public interface NfrCartInterface {
    void onDeleteClick(int position);
    void onPriceClick(int position, EditText editText);
    void onPlusClick(int position);
    void onMinusClick(int position);
}
