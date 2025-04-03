package com.ims.bpcluat.model.AlpModels.VoidModels;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class Output implements Parcelable {
    private String statusMessage;
    private List<TxnList> txnList;

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public List<TxnList> getTxnList() {
        return txnList;
    }

    public void setTxnList(List<TxnList> txnList) {
        this.txnList = txnList;
    }


    public Output() {
    }

    protected Output(Parcel in) {
        statusMessage = in.readString();
        txnList = in.createTypedArrayList(TxnList.CREATOR);
    }

    public static final Creator<Output> CREATOR = new Creator<Output>() {
        @Override
        public Output createFromParcel(Parcel in) {
            return new Output(in);
        }

        @Override
        public Output[] newArray(int size) {
            return new Output[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(statusMessage);
        dest.writeTypedList(txnList);
    }
}
