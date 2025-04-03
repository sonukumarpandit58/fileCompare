package com.ims.bpcluat.model;

public class VoucherModel {
    public String utrNo, dateTime, amount, amtAuthorizedRs, ufillTxnId, pumpNo, localBayID;
    private boolean isSelected;

    public VoucherModel(String utrNo, String dateTime, String amount, String amtAuthorizedRs, String ufillTxnId, String pumpNo,String localBayID) {
        this.utrNo = utrNo;
        this.dateTime = dateTime;
        this.amount = amount;
        this.amtAuthorizedRs = amtAuthorizedRs;
        this.ufillTxnId = ufillTxnId;
        this.pumpNo = pumpNo;
        this.localBayID = localBayID;
        this.isSelected = false;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public String getUtrNo() {
        return utrNo;
    }

    public void setUtrNo(String utrNo) {
        this.utrNo = utrNo;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getAmtAuthorizedRs() {
        return amtAuthorizedRs;
    }

    public void setAmtAuthorizedRs(String amtAuthorizedRs) {
        this.amtAuthorizedRs = amtAuthorizedRs;
    }

    public String getUfillTxnId() {
        return ufillTxnId;
    }

    public void setUfillTxnId(String ufillTxnId) {
        this.ufillTxnId = ufillTxnId;
    }

    public String getPumpNo() {
        return pumpNo;
    }

    public void setPumpNo(String pumpNo) {
        this.pumpNo = pumpNo;
    }

    public String getLocalBayID() {
        return localBayID;
    }

    public void setLocalBayID(String localBayID) {
        this.localBayID = localBayID;
    }
}
