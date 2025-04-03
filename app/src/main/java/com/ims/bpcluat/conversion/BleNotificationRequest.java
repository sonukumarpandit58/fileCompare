package com.ims.bpcluat.conversion;

public class BleNotificationRequest {
    public static String createRequest(String pumpNumber, String transactionId, String isMopChange, String isTrxPrinted,
                                       String isDiscountApply, String discount, String netAmount, String terminalId,
                                       String mop, String paymentMode, String mobileNumber, String vehicleNumber,
                                       String vehicleType, String voucherIdOrOrderId, String extraOrCashMemoNo,
                                       String extraOrTransactionReferenceNo) {

        String commandNo = "27";
        String data = padHex(pumpNumber, 1) +
                padHex(transactionId, 5) +
                padHex(isMopChange, 1) +
                padHex(isTrxPrinted, 1) +
                padHex(isDiscountApply, 1) +
                padHex(discount, 4) +
                padHex(netAmount, 4) +
                padHex(terminalId, 10) +
                padHex(mop, 1) +
                padHex(paymentMode, 1) +
                padHex(mobileNumber, 13) +
                padHex(vehicleNumber, 10) +
                padHex(vehicleType, 1) +
                padHex(voucherIdOrOrderId, 20) +
                padHex(extraOrCashMemoNo, 10) +
                padHex(extraOrTransactionReferenceNo, 40);
        return ("007D" + commandNo + "01" + data).toUpperCase();  // 27h
    }

    private static String padHex(String hexString, int length) {
        StringBuilder paddedHex = new StringBuilder(hexString);
        while (paddedHex.length() < length * 2) {
            paddedHex.append('0');
        }
        return paddedHex.toString();
    }

    public static String twentyEightRequest(String pumpNumber, String nozzleNumber, String presetType, String presetValue,
                                       String mop, String paymentType, String mobileNumber, String vehicleNumber,String vehicleType,
                                       String orderId, String extraOrCashMemoNo, String terminalId,
                                       String extraOrTransactionReferenceNo) {

        String commandNo = "28";
        String data = padHex(pumpNumber, 1) +
                padHex(nozzleNumber, 1) +
                padHex(presetType, 1) +
                padHex(presetValue, 4) +
                padHex(mop, 1) +
                padHex(paymentType, 1) +
                padHex(mobileNumber, 13) +
                padHex(vehicleNumber, 10) +
                padHex(vehicleType, 1) +
                padHex(orderId, 20) +
                padHex(extraOrCashMemoNo, 10) +
                padHex(terminalId, 10) +
                padHex(extraOrTransactionReferenceNo, 40);

        return ("0073" + commandNo + "01" + data).toUpperCase();
    }
}
