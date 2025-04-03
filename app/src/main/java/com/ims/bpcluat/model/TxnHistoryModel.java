package com.ims.bpcluat.model;

public class TxnHistoryModel {
    public String pumpNo,quantity, dateTime, price, txnType;

    public String getPumpNo() {
        return pumpNo;
    }

    public void setPumpNo(String pumpNo) {
        this.pumpNo = pumpNo;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public TxnHistoryModel(String pumpNo, String quantity, String dateTime, String price, String txnType) {
        this.pumpNo = pumpNo;
        this.quantity = quantity;
        this.dateTime = dateTime;
        this.price = price;
        this.txnType = txnType;
    }
}
