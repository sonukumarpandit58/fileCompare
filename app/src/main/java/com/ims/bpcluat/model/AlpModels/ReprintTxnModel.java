package com.ims.bpcluat.model.AlpModels;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class ReprintTxnModel implements Serializable {
    private String alpTransactionId;
    private String originalAlpTransactionId;
    private String ROName;
    private String roMobileNo;
    private String reportID;
    private String originalClientTxnId;
    private String dealerID;
    private String mobileNumber;
    private String txnProduct;
    private String discount;
    private String txnType;
    private String customerCardNumber;
    private String roCity;
    private String fuelAmount;
    private String petroMilesEarned;
    private String noOfRequestedCard;
    private String txnDiscount;
    private String txnStatus;
    private String amountPaid;
    private String clientTxnId;
    private String paymentReferenceNumber;
    private String programName;
    private String txnSource;
    private String cardBalance;
    private String vehicleNumber;
    private boolean voided;
    private String chargeSlipNumber;
    private String batchNumber;
    private String timestamp;
    private String customerDisclaimer;
    private double txnMEShare;
    private String odometerReading;
    private String aposTerminalID;
    private String netAmount;
    private String chargeSlipFooter;
    private String txnMode;
    private String customerAccountNumber;
    private String txnQuantity;
    private String customerName;
    private String txnBayId;
    private String productRate;
    private String currencyCode;
    private String chargeSlipHeader;
    private String merchantDisclaimer;
    private boolean reversed;
    private String tcsAmount;
    private String txnAmount;

    //PAYLOAD DATA
    private String mobNo;
    private String date;
    private String time;
    private String txnId;

    // Constructor
    public ReprintTxnModel(String alpTransactionId, String originalAlpTransactionId, String ROName, String roMobileNo,
                           String reportID, String originalClientTxnId, String dealerID, String mobileNumber,
                           String txnProduct, String discount, String txnType, String customerCardNumber, String roCity,
                           String fuelAmount, String petroMilesEarned, String noOfRequestedCard, String txnDiscount,
                           String txnStatus, String amountPaid, String clientTxnId, String paymentReferenceNumber,
                           String programName, String txnSource, String cardBalance, String vehicleNumber, boolean voided,
                           String chargeSlipNumber, String batchNumber, String timestamp, String customerDisclaimer,
                           double txnMEShare, String odometerReading, String aposTerminalID, String netAmount,
                           String chargeSlipFooter, String txnMode, String customerAccountNumber, String txnQuantity,
                           String customerName, String txnBayId, String productRate, String currencyCode,
                           String chargeSlipHeader, String merchantDisclaimer, boolean reversed, String tcsAmount,
                           String txnAmount, String mobNo, String date, String time, String txnId) {
        this.alpTransactionId = alpTransactionId;
        this.originalAlpTransactionId = originalAlpTransactionId;
        this.ROName = ROName;
        this.roMobileNo = roMobileNo;
        this.reportID = reportID;
        this.originalClientTxnId = originalClientTxnId;
        this.dealerID = dealerID;
        this.mobileNumber = mobileNumber;
        this.txnProduct = txnProduct;
        this.discount = discount;
        this.txnType = txnType;
        this.customerCardNumber = customerCardNumber;
        this.roCity = roCity;
        this.fuelAmount = fuelAmount;
        this.petroMilesEarned = petroMilesEarned;
        this.noOfRequestedCard = noOfRequestedCard;
        this.txnDiscount = txnDiscount;
        this.txnStatus = txnStatus;
        this.amountPaid = amountPaid;
        this.clientTxnId = clientTxnId;
        this.paymentReferenceNumber = paymentReferenceNumber;
        this.programName = programName;
        this.txnSource = txnSource;
        this.cardBalance = cardBalance;
        this.vehicleNumber = vehicleNumber;
        this.voided = voided;
        this.chargeSlipNumber = chargeSlipNumber;
        this.batchNumber = batchNumber;
        this.timestamp = timestamp;
        this.customerDisclaimer = customerDisclaimer;
        this.txnMEShare = txnMEShare;
        this.odometerReading = odometerReading;
        this.aposTerminalID = aposTerminalID;
        this.netAmount = netAmount;
        this.chargeSlipFooter = chargeSlipFooter;
        this.txnMode = txnMode;
        this.customerAccountNumber = customerAccountNumber;
        this.txnQuantity = txnQuantity;
        this.customerName = customerName;
        this.txnBayId = txnBayId;
        this.productRate = productRate;
        this.currencyCode = currencyCode;
        this.chargeSlipHeader = chargeSlipHeader;
        this.merchantDisclaimer = merchantDisclaimer;
        this.reversed = reversed;
        this.tcsAmount = tcsAmount;
        this.txnAmount = txnAmount;
        this.mobNo = mobNo;
        this.date = date;
        this.time = time;
        this.txnId = txnId;
    }


    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("alpTransactionId", alpTransactionId);
            jsonObject.put("originalAlpTransactionId", originalAlpTransactionId);
            jsonObject.put("ROName", ROName);
            jsonObject.put("roMobileNo", roMobileNo);
            jsonObject.put("reportID", reportID);
            jsonObject.put("originalClientTxnId", originalClientTxnId);
            jsonObject.put("dealerID", dealerID);
            jsonObject.put("mobileNumber", mobileNumber);
            jsonObject.put("txnProduct", txnProduct);
            jsonObject.put("discount", discount);
            jsonObject.put("txnType", txnType);
            jsonObject.put("customerCardNumber", customerCardNumber);
            jsonObject.put("roCity", roCity);
            jsonObject.put("fuelAmount", fuelAmount);
            jsonObject.put("petroMilesEarned", petroMilesEarned);
            jsonObject.put("noOfRequestedCard", noOfRequestedCard);
            jsonObject.put("txnDiscount", txnDiscount);
            jsonObject.put("txnStatus", txnStatus);
            jsonObject.put("amountPaid", amountPaid);
            jsonObject.put("clientTxnId", clientTxnId);
            jsonObject.put("paymentReferenceNumber", paymentReferenceNumber);
            jsonObject.put("programName", programName);
            jsonObject.put("txnSource", txnSource);
            jsonObject.put("cardBalance", cardBalance);
            jsonObject.put("vehicleNumber", vehicleNumber);
            jsonObject.put("voided", voided);
            jsonObject.put("chargeSlipNumber", chargeSlipNumber);
            jsonObject.put("batchNumber", batchNumber);
            jsonObject.put("timestamp", timestamp);
            jsonObject.put("customerDisclaimer", customerDisclaimer);
            jsonObject.put("txnMEShare", txnMEShare);
            jsonObject.put("odometerReading", odometerReading);
            jsonObject.put("aposTerminalID", aposTerminalID);
            jsonObject.put("netAmount", netAmount);
            jsonObject.put("chargeSlipFooter", chargeSlipFooter);
            jsonObject.put("txnMode", txnMode);
            jsonObject.put("customerAccountNumber", customerAccountNumber);
            jsonObject.put("txnQuantity", txnQuantity);
            jsonObject.put("customerName", customerName);
            jsonObject.put("txnBayId", txnBayId);
            jsonObject.put("productRate", productRate);
            jsonObject.put("currencyCode", currencyCode);
            jsonObject.put("chargeSlipHeader", chargeSlipHeader);
            jsonObject.put("merchantDisclaimer", merchantDisclaimer);
            jsonObject.put("reversed", reversed);
            jsonObject.put("tcsAmount", tcsAmount);
            jsonObject.put("txnAmount", txnAmount);
            jsonObject.put("mobNo", mobNo);
            jsonObject.put("date", date);
            jsonObject.put("time", time);
            jsonObject.put("txnId", txnId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public String getAlpTransactionId() {
        return alpTransactionId;
    }

    public void setAlpTransactionId(String alpTransactionId) {
        this.alpTransactionId = alpTransactionId;
    }

    public String getOriginalAlpTransactionId() {
        return originalAlpTransactionId;
    }

    public void setOriginalAlpTransactionId(String originalAlpTransactionId) {
        this.originalAlpTransactionId = originalAlpTransactionId;
    }

    public String getROName() {
        return ROName;
    }

    public void setROName(String ROName) {
        this.ROName = ROName;
    }

    public String getRoMobileNo() {
        return roMobileNo;
    }

    public void setRoMobileNo(String roMobileNo) {
        this.roMobileNo = roMobileNo;
    }

    public String getReportID() {
        return reportID;
    }

    public void setReportID(String reportID) {
        this.reportID = reportID;
    }

    public String getOriginalClientTxnId() {
        return originalClientTxnId;
    }

    public void setOriginalClientTxnId(String originalClientTxnId) {
        this.originalClientTxnId = originalClientTxnId;
    }

    public String getDealerID() {
        return dealerID;
    }

    public void setDealerID(String dealerID) {
        this.dealerID = dealerID;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getTxnProduct() {
        return txnProduct;
    }

    public void setTxnProduct(String txnProduct) {
        this.txnProduct = txnProduct;
    }

    public String getDiscount() {
        return discount;
    }

    public void setDiscount(String discount) {
        this.discount = discount;
    }

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public String getCustomerCardNumber() {
        return customerCardNumber;
    }

    public void setCustomerCardNumber(String customerCardNumber) {
        this.customerCardNumber = customerCardNumber;
    }

    public String getRoCity() {
        return roCity;
    }

    public void setRoCity(String roCity) {
        this.roCity = roCity;
    }

    public String getFuelAmount() {
        return fuelAmount;
    }

    public void setFuelAmount(String fuelAmount) {
        this.fuelAmount = fuelAmount;
    }

    public String getPetroMilesEarned() {
        return petroMilesEarned;
    }

    public void setPetroMilesEarned(String petroMilesEarned) {
        this.petroMilesEarned = petroMilesEarned;
    }

    public String getNoOfRequestedCard() {
        return noOfRequestedCard;
    }

    public void setNoOfRequestedCard(String noOfRequestedCard) {
        this.noOfRequestedCard = noOfRequestedCard;
    }

    public String getTxnDiscount() {
        return txnDiscount;
    }

    public void setTxnDiscount(String txnDiscount) {
        this.txnDiscount = txnDiscount;
    }

    public String getTxnStatus() {
        return txnStatus;
    }

    public void setTxnStatus(String txnStatus) {
        this.txnStatus = txnStatus;
    }

    public String getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(String amountPaid) {
        this.amountPaid = amountPaid;
    }

    public String getClientTxnId() {
        return clientTxnId;
    }

    public void setClientTxnId(String clientTxnId) {
        this.clientTxnId = clientTxnId;
    }

    public String getPaymentReferenceNumber() {
        return paymentReferenceNumber;
    }

    public void setPaymentReferenceNumber(String paymentReferenceNumber) {
        this.paymentReferenceNumber = paymentReferenceNumber;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getTxnSource() {
        return txnSource;
    }

    public void setTxnSource(String txnSource) {
        this.txnSource = txnSource;
    }

    public String getCardBalance() {
        return cardBalance;
    }

    public void setCardBalance(String cardBalance) {
        this.cardBalance = cardBalance;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public boolean isVoided() {
        return voided;
    }

    public void setVoided(boolean voided) {
        this.voided = voided;
    }

    public String getChargeSlipNumber() {
        return chargeSlipNumber;
    }

    public void setChargeSlipNumber(String chargeSlipNumber) {
        this.chargeSlipNumber = chargeSlipNumber;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getCustomerDisclaimer() {
        return customerDisclaimer;
    }

    public void setCustomerDisclaimer(String customerDisclaimer) {
        this.customerDisclaimer = customerDisclaimer;
    }

    public double getTxnMEShare() {
        return txnMEShare;
    }

    public void setTxnMEShare(double txnMEShare) {
        this.txnMEShare = txnMEShare;
    }

    public String getOdometerReading() {
        return odometerReading;
    }

    public void setOdometerReading(String odometerReading) {
        this.odometerReading = odometerReading;
    }

    public String getAposTerminalID() {
        return aposTerminalID;
    }

    public void setAposTerminalID(String aposTerminalID) {
        this.aposTerminalID = aposTerminalID;
    }

    public String getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(String netAmount) {
        this.netAmount = netAmount;
    }

    public String getChargeSlipFooter() {
        return chargeSlipFooter;
    }

    public void setChargeSlipFooter(String chargeSlipFooter) {
        this.chargeSlipFooter = chargeSlipFooter;
    }

    public String getTxnMode() {
        return txnMode;
    }

    public void setTxnMode(String txnMode) {
        this.txnMode = txnMode;
    }

    public String getCustomerAccountNumber() {
        return customerAccountNumber;
    }

    public void setCustomerAccountNumber(String customerAccountNumber) {
        this.customerAccountNumber = customerAccountNumber;
    }

    public String getTxnQuantity() {
        return txnQuantity;
    }

    public void setTxnQuantity(String txnQuantity) {
        this.txnQuantity = txnQuantity;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getTxnBayId() {
        return txnBayId;
    }

    public void setTxnBayId(String txnBayId) {
        this.txnBayId = txnBayId;
    }

    public String getProductRate() {
        return productRate;
    }

    public void setProductRate(String productRate) {
        this.productRate = productRate;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getChargeSlipHeader() {
        return chargeSlipHeader;
    }

    public void setChargeSlipHeader(String chargeSlipHeader) {
        this.chargeSlipHeader = chargeSlipHeader;
    }

    public String getMerchantDisclaimer() {
        return merchantDisclaimer;
    }

    public void setMerchantDisclaimer(String merchantDisclaimer) {
        this.merchantDisclaimer = merchantDisclaimer;
    }

    public boolean isReversed() {
        return reversed;
    }

    public void setReversed(boolean reversed) {
        this.reversed = reversed;
    }

    public String getTcsAmount() {
        return tcsAmount;
    }

    public void setTcsAmount(String tcsAmount) {
        this.tcsAmount = tcsAmount;
    }

    public String getTxnAmount() {
        return txnAmount;
    }

    public void setTxnAmount(String txnAmount) {
        this.txnAmount = txnAmount;
    }

    public String getMobNo() {
        return mobNo;
    }

    public void setMobNo(String mobNo) {
        this.mobNo = mobNo;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTxnId() {
        return txnId;
    }

    public void setTxnId(String txnId) {
        this.txnId = txnId;
    }
}
