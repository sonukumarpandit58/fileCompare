package com.ims.bpcluat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ImageDecoder {
    private Context mContext;

    public ImageDecoder(Context context) {
        mContext = context;
    }

    public Bitmap decodeImage(int resourceId) {
        return BitmapFactory.decodeResource(mContext.getResources(), resourceId);
    }
}
