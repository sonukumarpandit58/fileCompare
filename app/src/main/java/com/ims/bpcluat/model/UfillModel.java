package com.ims.bpcluat.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class UfillModel implements Parcelable {
    String mobileNumber;
    String vehicleNumber;
    String vehicleType;
    String txnType;
    String txnNotificationDate;
    String txnNotificationTime;
    String txnChargselipDate;
    String txnChargeslipTime;
    String pumpNo;
    String nozzleNo;
    String voucherNo;
    String voucherStatus;
    String voucherAmt;
    String fuelledAmt;
    String refundAmt;
    String txnId;
    String product;
    String txnEndTime;
    String dateTime;
    String utrNo;
    String ufillTxnId;
    String localBayID;
    String qty;
    String qrCodeUrl;
    String fccTimeStamp;
    String productId;
    String volume;
    String productPrice;
    String localMpdId;
    String presetType;
    String txnStartTime;
    String prebookTxn;
    String prebookTxnTime;
    String id;
    String authCode;
    String rrn;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getRrn() {
        return rrn;
    }

    public void setRrn(String rrn) {
        this.rrn = rrn;
    }

    public UfillModel() {
        // Required empty public constructor
    }

    public String getPrebookTxn() {
        return prebookTxn;
    }

    public void setPrebookTxn(String prebookTxn) {
        this.prebookTxn = prebookTxn;
    }

    public String getPrebookTxnTime() {
        return prebookTxnTime;
    }

    public void setPrebookTxnTime(String prebookTxnTime) {
        this.prebookTxnTime = prebookTxnTime;
    }

    protected UfillModel(Parcel in) {
        mobileNumber = in.readString();
        vehicleNumber = in.readString();
        vehicleType = in.readString();
        txnType = in.readString();
        txnNotificationDate = in.readString();
        txnNotificationTime = in.readString();
        txnChargselipDate = in.readString();
        txnChargeslipTime = in.readString();
        pumpNo = in.readString();
        nozzleNo = in.readString();
        voucherNo = in.readString();
        voucherStatus = in.readString();
        voucherAmt = in.readString();
        fuelledAmt = in.readString();
        refundAmt = in.readString();
        txnId = in.readString();
        product = in.readString();
        txnEndTime = in.readString();
        dateTime = in.readString();
        utrNo = in.readString();
        ufillTxnId = in.readString();
        localBayID = in.readString();
        qty = in.readString();
        qrCodeUrl = in.readString();
        fccTimeStamp = in.readString();
        productId = in.readString();
        volume = in.readString();
        productPrice = in.readString();
        localMpdId = in.readString();
        presetType = in.readString();
        txnStartTime = in.readString();
        prebookTxn = in.readString();
        prebookTxnTime = in.readString();
        id = in.readString();
        authCode = in.readString();
        rrn = in.readString();
    }

    public static final Creator<UfillModel> CREATOR = new Creator<UfillModel>() {
        @Override
        public UfillModel createFromParcel(Parcel in) {
            return new UfillModel(in);
        }

        @Override
        public UfillModel[] newArray(int size) {
            return new UfillModel[size];
        }
    };

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

    public String getPumpNo() {
        return pumpNo;
    }

    public void setPumpNo(String pumpNo) {
        this.pumpNo = pumpNo;
    }

    public String getNozzleNo() {
        return nozzleNo;
    }

    public void setNozzleNo(String nozzleNo) {
        this.nozzleNo = nozzleNo;
    }

    public String getVoucherNo() {
        return voucherNo;
    }

    public void setVoucherNo(String voucherNo) {
        this.voucherNo = voucherNo;
    }

    public String getVoucherStatus() {
        return voucherStatus;
    }

    public void setVoucherStatus(String voucherStatus) {
        this.voucherStatus = voucherStatus;
    }

    public String getVoucherAmt() {
        return voucherAmt;
    }

    public void setVoucherAmt(String voucherAmt) {
        this.voucherAmt = voucherAmt;
    }

    public String getFuelledAmt() {
        return fuelledAmt;
    }

    public void setFuelledAmt(String fuelledAmt) {
        this.fuelledAmt = fuelledAmt;
    }

    public String getRefundAmt() {
        return refundAmt;
    }

    public void setRefundAmt(String refundAmt) {
        this.refundAmt = refundAmt;
    }

    public String getTxnId() {
        return txnId;
    }

    public void setTxnId(String txnId) {
        this.txnId = txnId;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getTxnEndTime() {
        return txnEndTime;
    }

    public void setTxnEndTime(String txnEndTime) {
        this.txnEndTime = txnEndTime;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getUtrNo() {
        return utrNo;
    }

    public void setUtrNo(String utrNo) {
        this.utrNo = utrNo;
    }

    public String getUfillTxnId() {
        return ufillTxnId;
    }

    public void setUfillTxnId(String ufillTxnId) {
        this.ufillTxnId = ufillTxnId;
    }

    public String getLocalBayID() {
        return localBayID;
    }

    public void setLocalBayID(String localBayID) {
        this.localBayID = localBayID;
    }

    public String getQty() {
        return qty;
    }

    public void setQty(String qty) {
        this.qty = qty;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public String getFccTimeStamp() {
        return fccTimeStamp;
    }

    public void setFccTimeStamp(String fccTimeStamp) {
        this.fccTimeStamp = fccTimeStamp;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(String productPrice) {
        this.productPrice = productPrice;
    }

    public String getLocalMpdId() {
        return localMpdId;
    }

    public void setLocalMpdId(String localMpdId) {
        this.localMpdId = localMpdId;
    }

    public String getPresetType() {
        return presetType;
    }

    public void setPresetType(String presetType) {
        this.presetType = presetType;
    }

    public String getTxnStartTime() {
        return txnStartTime;
    }

    public void setTxnStartTime(String txnStartTime) {
        this.txnStartTime = txnStartTime;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mobileNumber);
        dest.writeString(vehicleNumber);
        dest.writeString(vehicleType);
        dest.writeString(txnType);
        dest.writeString(txnNotificationDate);
        dest.writeString(txnNotificationTime);
        dest.writeString(txnChargselipDate);
        dest.writeString(txnChargeslipTime);
        dest.writeString(pumpNo);
        dest.writeString(nozzleNo);
        dest.writeString(voucherNo);
        dest.writeString(voucherStatus);
        dest.writeString(voucherAmt);
        dest.writeString(fuelledAmt);
        dest.writeString(refundAmt);
        dest.writeString(txnId);
        dest.writeString(product);
        dest.writeString(txnEndTime);
        dest.writeString(dateTime);
        dest.writeString(utrNo);
        dest.writeString(ufillTxnId);
        dest.writeString(localBayID);
        dest.writeString(qty);
        dest.writeString(qrCodeUrl);
        dest.writeString(fccTimeStamp);
        dest.writeString(productId);
        dest.writeString(volume);
        dest.writeString(productPrice);
        dest.writeString(localMpdId);
        dest.writeString(presetType);
        dest.writeString(txnStartTime);
        dest.writeString(prebookTxn);
        dest.writeString(prebookTxnTime);
        dest.writeString(id);
        dest.writeString(authCode);
        dest.writeString(rrn);
    }
}
