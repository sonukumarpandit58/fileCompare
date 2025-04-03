package com.ims.bpcluat;

import java.io.Serializable;

public class ApiReqRes implements Serializable{

    private static final long serialVersionUID = 1L;
    private String statusCode;
    private String payload;
    private String sequenceNo;
    private double amount;
    private double quantity;
    private double RSP;
    private String product;
    private int mobileNo;
    private int pumpNo;
    private int nozzelNo;

    public String getStatusCode() {
        return statusCode;
    }
    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }
    public String getPayload() {
        return payload;
    }
    public void setPayload(String payload) {
        this.payload = payload;
    }
    public String getSequenceNo() {
        return sequenceNo;
    }
    public void setSequenceNo(String sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

/*
    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getAmount() {
        return amount;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setRSP(double rsp) {
        this.RSP = rsp;
    }

    public double getRSP() {
        return RSP;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getProduct() {
        return product;
    }

    public void setNozzelNo(int nozzelNo) {
        this.nozzelNo = nozzelNo;
    }

    public int getNozzelNo() {
        return nozzelNo;
    }

    public void setPumpNo(int pumpNo) {
        this.pumpNo = pumpNo;
    }

    public int getPumpNo() {
        return pumpNo;
    }

    public void setMobileNo(int mobileNo) {
        this.mobileNo = mobileNo;
    }

    public int getMobileNo() {
        return mobileNo;
    }

 */
}
