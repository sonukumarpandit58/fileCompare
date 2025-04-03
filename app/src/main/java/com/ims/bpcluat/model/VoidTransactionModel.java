package com.ims.bpcluat.model;


import java.util.ArrayList;
import java.util.List;

public class VoidTransactionModel {
    private String channel;
    private String reqDate;
    private String reqTime;
    private String userName;
    private String mid;
    private String tid;
    private String roCode;
    private String txnType;
    private String vmsTxnID;
    private String txnId;
    private String client;
    private String instId;
    private String dateTime;
    private String voucherCode;
    private String customerMobileNoMasked;
    private String voucherAmt;
    private List<VoidReason> voidReasons;


    public VoidTransactionModel(String channel, String instId, String client, String txnId, String vmsTxnID, String txnType, String tid, String roCode, String mid, String userName, String reqDate, String reqTime, String dateTime, String voucherCode, String customerMobileNoMasked, String voucherAmt, List<VoidReason> voidReasons) {
        this.channel = channel;
        this.instId = instId;
        this.client = client;
        this.txnId = txnId;
        this.vmsTxnID = vmsTxnID;
        this.txnType = txnType;
        this.tid = tid;
        this.roCode = roCode;
        this.mid = mid;
        this.userName = userName;
        this.reqDate = reqDate;
        this.reqTime = reqTime;
        this.dateTime = dateTime;
        this.voucherCode = voucherCode;
        this.customerMobileNoMasked = customerMobileNoMasked;
        this.voucherAmt = voucherAmt;
        this.voidReasons = voidReasons != null ? voidReasons : new ArrayList<>();
    }

//    public VoidTransactionModel(String voucherCode, String customerMobileNoMasked, String voucherAmt, List<VoidReason> voidReasons) {
//        this.voucherCode = voucherCode;
//        this.customerMobileNoMasked = customerMobileNoMasked;
//        this.voucherAmt = voucherAmt;
//        this.voidReasons = voidReasons != null ? voidReasons : new ArrayList<>();
//    }

    public String getVmsTxnID() {
        return vmsTxnID;
    }

    public void setVmsTxnID(String vmsTxnID) {
        this.vmsTxnID = vmsTxnID;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getReqDate() {
        return reqDate;
    }

    public void setReqDate(String reqDate) {
        this.reqDate = reqDate;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getReqTime() {
        return reqTime;
    }

    public void setReqTime(String reqTime) {
        this.reqTime = reqTime;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getMid() {
        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getRoCode() {
        return roCode;
    }

    public void setRoCode(String roCode) {
        this.roCode = roCode;
    }

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public String getInstId() {
        return instId;
    }

    public void setInstId(String instId) {
        this.instId = instId;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getTxnId() {
        return txnId;
    }

    public void setTxnId(String txnId) {
        this.txnId = txnId;
    }


    public String getVoucherCode() {
        return voucherCode;
    }

    public String getCustomerMobileNoMasked() {
        return customerMobileNoMasked;
    }

    public String getVoucherAmt() {
        return voucherAmt;
    }

    public List<VoidReason> getVoidReasons() {
        return voidReasons;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public void setCustomerMobileNoMasked(String customerMobileNoMasked) {
        this.customerMobileNoMasked = customerMobileNoMasked;
    }

    public void setVoucherAmt(String voucherAmt) {
        this.voucherAmt = voucherAmt;
    }

    public void setVoidReasons(List<VoidReason> voidReasons) {
        this.voidReasons = voidReasons;
    }
}
