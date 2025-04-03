package com.ims.bpcluat.model.AlpModels.VoidModels;

import android.os.Parcel;
import android.os.Parcelable;

public class TxnList implements Parcelable {
    private String alpTransactionId;
    private String txnAmount;
    private String clientTxnId;

    public TxnList() {
    }

    public String getAlpTransactionId() {
        return alpTransactionId;
    }

    public void setAlpTransactionId(String alpTransactionId) {
        this.alpTransactionId = alpTransactionId;
    }

    public String getTxnAmount() {
        return txnAmount;
    }

    public void setTxnAmount(String txnAmount) {
        this.txnAmount = txnAmount;
    }

    public String getClientTxnId() {
        return clientTxnId;
    }

    public void setClientTxnId(String clientTxnId) {
        this.clientTxnId = clientTxnId;
    }

    protected TxnList(Parcel in) {
        alpTransactionId = in.readString();
        txnAmount = in.readString();
        clientTxnId = in.readString();
    }

    public static final Creator<TxnList> CREATOR = new Creator<TxnList>() {
        @Override
        public TxnList createFromParcel(Parcel in) {
            return new TxnList(in);
        }

        @Override
        public TxnList[] newArray(int size) {
            return new TxnList[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(alpTransactionId);
        dest.writeString(txnAmount);
        dest.writeString(clientTxnId);
    }
}

