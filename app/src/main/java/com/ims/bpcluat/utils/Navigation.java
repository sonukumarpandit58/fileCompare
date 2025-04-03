package com.ims.bpcluat.utils;

import android.content.Context;
import android.content.Intent;

import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.model.CngModel;
import com.ims.bpcluat.model.NfrModel;
import com.ims.bpcluat.model.OnlineTxnModel;

public class Navigation {
    public static void BackWithData(Context context, CngModel cngModel, NfrModel nfrModel, OnlineTxnModel onlineTxnModel) {
        if (cngModel != null) {
            Intent intent = new Intent(context, SideBarActivity.class);
            intent.putExtra("redirect", "CngFragment");
            context.startActivity(intent);
        } else if (nfrModel != null) {
            Intent intent = new Intent(context, SideBarActivity.class);
            intent.putExtra("redirect", "NfrFragment");
            context.startActivity(intent);
        } else if(onlineTxnModel != null){
            Intent intent = new Intent(context, SideBarActivity.class);
            intent.putExtra("redirect", "");
            context.startActivity(intent);
        }
    }
}
