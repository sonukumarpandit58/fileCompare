package com.ims.bpcluat.model.AlpModels;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;


public class TempModel implements Parcelable {
    String amount;

    public TempModel() {
        // Required empty public constructor
    }

    protected TempModel(Parcel in) {
        amount = in.readString();
    }

    public static final Creator<TempModel> CREATOR = new Creator<TempModel>() {
        @Override
        public TempModel createFromParcel(Parcel in) {
            return new TempModel(in);
        }

        @Override
        public TempModel[] newArray(int size) {
            return new TempModel[size];
        }
    };

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(amount);
    }
}
