package com.ims.bpcluat;

import static com.ims.bpcluat.Helper.fileWrite;

import android.os.Build;
import android.util.Log;

import com.ims.bpcluat.conversion.HexToDecimal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class TransactionParser {

    private static int index = 0;
    private static String hexData;

    private static String getBytes(int length) {
        // Check if there are enough bytes left in the hexData to read
        if (index + length * 2 > hexData.length()) {
            Log.e("TransactionParser", "Attempted to read beyond the end of hexData at index: " + index);
            throw new StringIndexOutOfBoundsException("Attempted to read beyond the end of hexData");
        }
        String bytesValue = hexData.substring(index, index + length * 2);
        index += length * 2;
        return bytesValue;
    }

    private static Map<String, String> parseTransaction() {
        Map<String, String> transaction = new HashMap<>();
        try{
            transaction.put("PumpNumber", getBytes(1));
            transaction.put("NozzleNumber", getBytes(1));
            transaction.put("Year", getBytes(1));
            transaction.put("Month", getBytes(1));
            transaction.put("Day", getBytes(1));
            transaction.put("UniqueID", getBytes(2));
            transaction.put("Hour", getBytes(1));
            transaction.put("Minute", getBytes(1));
            transaction.put("Second", getBytes(1));
            transaction.put("Volume", getBytes(4));
            transaction.put("Amount", getBytes(4));
            transaction.put("ProductPrice", getBytes(4));
            transaction.put("ProductID", getBytes(1));
            transaction.put("AuthID", getBytes(2));
            transaction.put("TxnStartYear", getBytes(1));
            transaction.put("TxnStartMonth", getBytes(1));
            transaction.put("TxnStartDay", getBytes(1));
            transaction.put("TxnStartHour", getBytes(1));
            transaction.put("TxnStartMinute", getBytes(1));
            transaction.put("TxnStartSecond", getBytes(1));
            transaction.put("TxnPresetType", getBytes(1));
            transaction.put("TxnPresetValue", getBytes(4));
            transaction.put("AttendantID", getBytes(1));
            transaction.put("EnableOptionByte", getBytes(1));

            String enableOptionByte = transaction.get("EnableOptionByte");
            Log.d("enableOptionByte",enableOptionByte);
            if(enableOptionByte.equals("00")){
                Log.d("enableOptionByte1","inside if block");
               // return null;
            }
            Log.d("sonuTest","Process upto here");
            int enableOptionByteInt = Integer.parseInt(enableOptionByte, 16);
            String enableOptionBinary = Integer.toBinaryString(enableOptionByteInt);
            enableOptionBinary = String.format("%8s", enableOptionBinary).replace(' ', '0');
            Log.d("enableOptionBinary1",enableOptionByte + " - " + enableOptionBinary);
            enableOptionBinary = new StringBuilder(enableOptionBinary).reverse().toString();
            Log.d("enableOptionBinary2",enableOptionByte + " - " + enableOptionBinary);

            if (enableOptionBinary.charAt(0) == '1') { // Bit 0: Discount
                transaction.put("Discount", getBytes(4));
            }

            transaction.put("NetAmount", getBytes(4));
            transaction.put("MOP", getBytes(1));

            if (enableOptionBinary.charAt(1) == '1') { // Bit 1: Payment Mode
                transaction.put("PaymentMode", getBytes(1));
            }
            if (enableOptionBinary.charAt(2) == '1') { // Bit 2: Mobile Number
                transaction.put("MobileNumber", getBytes(13));
            }
            if (enableOptionBinary.charAt(3) == '1') { // Bit 3: Vehicle Number, Vehicle Type
                transaction.put("VehicleNumber", getBytes(10));
                transaction.put("VehicleType", getBytes(1));
            }
            if (enableOptionBinary.charAt(4) == '1') { // Bit 4: Is Transaction Printed
                transaction.put("IsTransactionPrinted", getBytes(1));
            }
            if (enableOptionBinary.charAt(5) == '1') { // Bit 5: Voucher ID or Order ID or Account Number
                transaction.put("VoucherIDOrOrderIDOrAccountNumber", getBytes(20));
            }
            if (enableOptionBinary.charAt(6) == '1') { // Bit 6: Extra or Cash Memo Number
                transaction.put("ExtraOrCashMemoNumber", getBytes(10));
            }
            if (enableOptionBinary.charAt(7) == '1') { // Bit 7: Extra 1 or Transaction Reference Number
                transaction.put("Extra1OrTransactionReferenceNumber", getBytes(40));
            }

            if (transaction.get("PaymentMode") == null || !transaction.get("PaymentMode").equals("00")) {
               // return null;
            }
            Log.d("160 lines","160 lines");

            String netAmtCheck = HexToDecimal.convertAmount(transaction.get("NetAmount"));
            Log.d("netAmtCheck",netAmtCheck);
            if(Helper.isZero(netAmtCheck)){
               // return null;
            }

            Log.d("transaction", String.valueOf(transaction));
        }catch (Exception e){
            Log.d("ParsingException",e.toString());
        }
        return transaction;
    }

    private static Map<String, Object> parseHexData(String hex) {
        hexData = hex;
        index = 0;

        Map<String, Object> result = new HashMap<>();

        // Parse header
        Map<String, String> header = new HashMap<>();
        header.put("Length", getBytes(2));
        header.put("Command", getBytes(1));
        header.put("ACK", getBytes(1));
        header.put("PumpNumber", getBytes(1));
        header.put("TransactionCount", getBytes(1));
        result.put("Header", header);

        // Parse transactions
        List<Map<String, String>> transactions = new ArrayList<>();
        int transactionCount = Integer.parseInt(header.get("TransactionCount"), 16);
        for (int i = 0; i < transactionCount; i++) {
          //  transactions.add(parseTransaction());
            Map<String, String> transaction = parseTransaction();
            if (transaction != null) {
                transactions.add(transaction);
            }
        }
        result.put("Transactions", transactions);

        // Parse CRC
        String crc = getBytes(2);
        result.put("CRC", crc);

        return result;
    }

    public static Map<String, Object> txnParser(String hexData) {
        Map<String, Object> parsedData = parseHexData(hexData);
        System.out.println("Header: " + parsedData.get("Header"));
        System.out.println("Transactions: " + parsedData.get("Transactions"));
        System.out.println("CRC: " + parsedData.get("CRC"));
        return parsedData;
    }

}
