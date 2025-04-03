package com.ims.bpcluat.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class NfrModel implements Parcelable {
    private String name;
    private String qty;
    private String amt;

    String mobileNumber;
    String vehicleNumber;
    String vehicleType;
    String txnType;
    String txnNotificationDate;
    String txnNotificationTime;
    String txnChargselipDate;
    String txnChargeslipTime;
    String rrn;
    String cardType;
    String posEntryMode;
    String terminalInvoiceNo;
    String batchNo;
    String aid;
    String tsi;
    String tvr;
    String atc;
    String transactionCertificate;
    String cardPaymentVersionNo;
    String authTid;
    String authBank;
    String authCode;
    String cardNo;
    String cardFirst;
    String cardLast;
    String txnId;
    String field3;
    String field6;
    String field7;
    String field9;
    String field13;
    String alpTxnId;
    String alpTid;
    String alpSlipNo;
    String alpReportId;
    String alpType;
    String alpTxnSource;
    String alpCustName;
    String alpAccNo;
    String alpCardId;
    String alpVechCard;
    String alpOdometer;
    String alpWallet;
    String alpProduct;
    String alpRate;
    String alpVol;
    String alpFuelAmount;
    String alpTcsAmount;
    String alpTxnAmount;
    String alpPmEarn;
    String alpMeShare;
    String alpCardBalance;
    String ROName;
    String roCity;
    String roMobileNo;
    String cardTxnCustomerName;

    public NfrModel() {

    }

    protected NfrModel(Parcel in) {
        name = in.readString();
        qty = in.readString();
        amt = in.readString();
        mobileNumber = in.readString();
        vehicleNumber = in.readString();
        vehicleType = in.readString();
        txnType = in.readString();
        txnNotificationDate = in.readString();
        txnNotificationTime = in.readString();
        txnChargselipDate = in.readString();
        txnChargeslipTime = in.readString();
        rrn = in.readString();
        cardType = in.readString();
        posEntryMode = in.readString();
        terminalInvoiceNo = in.readString();
        batchNo = in.readString();
        aid = in.readString();
        tsi = in.readString();
        tvr = in.readString();
        atc = in.readString();
        transactionCertificate = in.readString();
        cardPaymentVersionNo = in.readString();
        authTid = in.readString();
        authBank = in.readString();
        authCode = in.readString();
        cardNo = in.readString();
        cardFirst = in.readString();
        cardLast = in.readString();
        txnId = in.readString();
        field3 = in.readString();
        field6 = in.readString();
        field7 = in.readString();
        field9 = in.readString();
        field13 = in.readString();
        alpTxnId = in.readString();
        alpTid = in.readString();
        alpSlipNo = in.readString();
        alpReportId = in.readString();
        alpType = in.readString();
        alpTxnSource = in.readString();
        alpCustName = in.readString();
        alpAccNo = in.readString();
        alpCardId = in.readString();
        alpVechCard = in.readString();
        alpOdometer = in.readString();
        alpWallet = in.readString();
        alpProduct = in.readString();
        alpRate = in.readString();
        alpVol = in.readString();
        alpFuelAmount = in.readString();
        alpTcsAmount = in.readString();
        alpTxnAmount = in.readString();
        alpPmEarn = in.readString();
        alpMeShare = in.readString();
        alpCardBalance = in.readString();
        ROName = in.readString();
        roCity = in.readString();
        roMobileNo = in.readString();
        cardTxnCustomerName = in.readString();
    }

    public static final Creator<NfrModel> CREATOR = new Creator<NfrModel>() {
        @Override
        public NfrModel createFromParcel(Parcel in) {
            return new NfrModel(in);
        }

        @Override
        public NfrModel[] newArray(int size) {
            return new NfrModel[size];
        }
    };

    public String getRoCity() {
        return roCity;
    }

    public void setRoCity(String roCity) {
        this.roCity = roCity;
    }

    public String getRoMobileNo() {
        return roMobileNo;
    }

    public void setRoMobileNo(String roMobileNo) {
        this.roMobileNo = roMobileNo;
    }

    public String getROName() {
        return ROName;
    }

    public void setROName(String ROName) {
        this.ROName = ROName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQty() {
        return qty;
    }

    public void setQty(String qty) {
        this.qty = qty;
    }

    public String getAmt() {
        return amt;
    }

    public void setAmt(String amt) {
        this.amt = amt;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public String getTxnNotificationDate() {
        return txnNotificationDate;
    }

    public void setTxnNotificationDate(String txnNotificationDate) {
        this.txnNotificationDate = txnNotificationDate;
    }

    public String getTxnNotificationTime() {
        return txnNotificationTime;
    }

    public void setTxnNotificationTime(String txnNotificationTime) {
        this.txnNotificationTime = txnNotificationTime;
    }

    public String getTxnChargselipDate() {
        return txnChargselipDate;
    }

    public void setTxnChargselipDate(String txnChargselipDate) {
        this.txnChargselipDate = txnChargselipDate;
    }

    public String getTxnChargeslipTime() {
        return txnChargeslipTime;
    }

    public void setTxnChargeslipTime(String txnChargeslipTime) {
        this.txnChargeslipTime = txnChargeslipTime;
    }

    public String getRrn() {
        return rrn;
    }

    public void setRrn(String rrn) {
        this.rrn = rrn;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getPosEntryMode() {
        return posEntryMode;
    }

    public void setPosEntryMode(String posEntryMode) {
        this.posEntryMode = posEntryMode;
    }

    public String getTerminalInvoiceNo() {
        return terminalInvoiceNo;
    }

    public void setTerminalInvoiceNo(String terminalInvoiceNo) {
        this.terminalInvoiceNo = terminalInvoiceNo;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getAid() {
        return aid;
    }

    public void setAid(String aid) {
        this.aid = aid;
    }

    public String getTsi() {
        return tsi;
    }

    public void setTsi(String tsi) {
        this.tsi = tsi;
    }

    public String getTvr() {
        return tvr;
    }

    public void setTvr(String tvr) {
        this.tvr = tvr;
    }

    public String getAtc() {
        return atc;
    }

    public void setAtc(String atc) {
        this.atc = atc;
    }

    public String getTransactionCertificate() {
        return transactionCertificate;
    }

    public void setTransactionCertificate(String transactionCertificate) {
        this.transactionCertificate = transactionCertificate;
    }

    public String getCardPaymentVersionNo() {
        return cardPaymentVersionNo;
    }

    public void setCardPaymentVersionNo(String cardPaymentVersionNo) {
        this.cardPaymentVersionNo = cardPaymentVersionNo;
    }

    public String getAuthTid() {
        return authTid;
    }

    public void setAuthTid(String authTid) {
        this.authTid = authTid;
    }

    public String getAuthBank() {
        return authBank;
    }

    public void setAuthBank(String authBank) {
        this.authBank = authBank;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public String getCardFirst() {
        return cardFirst;
    }

    public void setCardFirst(String cardFirst) {
        this.cardFirst = cardFirst;
    }

    public String getCardLast() {
        return cardLast;
    }

    public void setCardLast(String cardLast) {
        this.cardLast = cardLast;
    }

    public String getTxnId() {
        return txnId;
    }

    public void setTxnId(String txnId) {
        this.txnId = txnId;
    }

    public String getField3() {
        return field3;
    }

    public void setField3(String field3) {
        this.field3 = field3;
    }

    public String getField6() {
        return field6;
    }

    public void setField6(String field6) {
        this.field6 = field6;
    }

    public String getField7() {
        return field7;
    }

    public void setField7(String field7) {
        this.field7 = field7;
    }

    public String getField9() {
        return field9;
    }

    public void setField9(String field9) {
        this.field9 = field9;
    }

    public String getField13() {
        return field13;
    }

    public void setField13(String field13) {
        this.field13 = field13;
    }

    public String getAlpTxnId() {
        return alpTxnId;
    }

    public void setAlpTxnId(String alpTxnId) {
        this.alpTxnId = alpTxnId;
    }

    public String getAlpTid() {
        return alpTid;
    }

    public void setAlpTid(String alpTid) {
        this.alpTid = alpTid;
    }

    public String getAlpSlipNo() {
        return alpSlipNo;
    }

    public void setAlpSlipNo(String alpSlipNo) {
        this.alpSlipNo = alpSlipNo;
    }

    public String getAlpReportId() {
        return alpReportId;
    }

    public void setAlpReportId(String alpReportId) {
        this.alpReportId = alpReportId;
    }

    public String getAlpType() {
        return alpType;
    }

    public void setAlpType(String alpType) {
        this.alpType = alpType;
    }

    public String getAlpTxnSource() {
        return alpTxnSource;
    }

    public void setAlpTxnSource(String alpTxnSource) {
        this.alpTxnSource = alpTxnSource;
    }

    public String getAlpCustName() {
        return alpCustName;
    }

    public void setAlpCustName(String alpCustName) {
        this.alpCustName = alpCustName;
    }

    public String getAlpAccNo() {
        return alpAccNo;
    }

    public void setAlpAccNo(String alpAccNo) {
        this.alpAccNo = alpAccNo;
    }

    public String getAlpCardId() {
        return alpCardId;
    }

    public void setAlpCardId(String alpCardId) {
        this.alpCardId = alpCardId;
    }

    public String getAlpVechCard() {
        return alpVechCard;
    }

    public void setAlpVechCard(String alpVechCard) {
        this.alpVechCard = alpVechCard;
    }

    public String getAlpOdometer() {
        return alpOdometer;
    }

    public void setAlpOdometer(String alpOdometer) {
        this.alpOdometer = alpOdometer;
    }

    public String getAlpWallet() {
        return alpWallet;
    }

    public void setAlpWallet(String alpWallet) {
        this.alpWallet = alpWallet;
    }

    public String getAlpProduct() {
        return alpProduct;
    }

    public void setAlpProduct(String alpProduct) {
        this.alpProduct = alpProduct;
    }

    public String getAlpRate() {
        return alpRate;
    }

    public void setAlpRate(String alpRate) {
        this.alpRate = alpRate;
    }

    public String getAlpVol() {
        return alpVol;
    }

    public void setAlpVol(String alpVol) {
        this.alpVol = alpVol;
    }

    public String getAlpFuelAmount() {
        return alpFuelAmount;
    }

    public void setAlpFuelAmount(String alpFuelAmount) {
        this.alpFuelAmount = alpFuelAmount;
    }

    public String getAlpTcsAmount() {
        return alpTcsAmount;
    }

    public void setAlpTcsAmount(String alpTcsAmount) {
        this.alpTcsAmount = alpTcsAmount;
    }

    public String getAlpTxnAmount() {
        return alpTxnAmount;
    }

    public void setAlpTxnAmount(String alpTxnAmount) {
        this.alpTxnAmount = alpTxnAmount;
    }

    public String getAlpPmEarn() {
        return alpPmEarn;
    }

    public void setAlpPmEarn(String alpPmEarn) {
        this.alpPmEarn = alpPmEarn;
    }

    public String getAlpMeShare() {
        return alpMeShare;
    }

    public void setAlpMeShare(String alpMeShare) {
        this.alpMeShare = alpMeShare;
    }

    public String getAlpCardBalance() {
        return alpCardBalance;
    }

    public void setAlpCardBalance(String alpCardBalance) {
        this.alpCardBalance = alpCardBalance;
    }

    public String getCardTxnCustomerName() {
        return cardTxnCustomerName;
    }

    public void setCardTxnCustomerName(String cardTxnCustomerName) {
        this.cardTxnCustomerName = cardTxnCustomerName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(qty);
        dest.writeString(amt);
        dest.writeString(mobileNumber);
        dest.writeString(vehicleNumber);
        dest.writeString(vehicleType);
        dest.writeString(txnType);
        dest.writeString(txnNotificationDate);
        dest.writeString(txnNotificationTime);
        dest.writeString(txnChargselipDate);
        dest.writeString(txnChargeslipTime);
        dest.writeString(rrn);
        dest.writeString(cardType);
        dest.writeString(posEntryMode);
        dest.writeString(terminalInvoiceNo);
        dest.writeString(batchNo);
        dest.writeString(aid);
        dest.writeString(tsi);
        dest.writeString(tvr);
        dest.writeString(atc);
        dest.writeString(transactionCertificate);
        dest.writeString(cardPaymentVersionNo);
        dest.writeString(authTid);
        dest.writeString(authBank);
        dest.writeString(authCode);
        dest.writeString(cardNo);
        dest.writeString(cardFirst);
        dest.writeString(cardLast);
        dest.writeString(txnId);
        dest.writeString(field3);
        dest.writeString(field6);
        dest.writeString(field7);
        dest.writeString(field9);
        dest.writeString(field13);
        dest.writeString(alpTxnId);
        dest.writeString(alpTid);
        dest.writeString(alpSlipNo);
        dest.writeString(alpReportId);
        dest.writeString(alpType);
        dest.writeString(alpTxnSource);
        dest.writeString(alpCustName);
        dest.writeString(alpAccNo);
        dest.writeString(alpCardId);
        dest.writeString(alpVechCard);
        dest.writeString(alpOdometer);
        dest.writeString(alpWallet);
        dest.writeString(alpProduct);
        dest.writeString(alpRate);
        dest.writeString(alpVol);
        dest.writeString(alpFuelAmount);
        dest.writeString(alpTcsAmount);
        dest.writeString(alpTxnAmount);
        dest.writeString(alpPmEarn);
        dest.writeString(alpMeShare);
        dest.writeString(alpCardBalance);
        dest.writeString(ROName);
        dest.writeString(roCity);
        dest.writeString(roMobileNo);
        dest.writeString(cardTxnCustomerName);
    }
}