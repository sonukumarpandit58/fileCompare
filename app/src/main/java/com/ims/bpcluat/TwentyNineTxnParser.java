package com.ims.bpcluat;

import static com.ims.bpcluat.Helper.getProductNameById;

import android.util.Log;

import com.ims.bpcluat.conversion.CreateTransactionId;
import com.ims.bpcluat.conversion.HexToDecimal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TwentyNineTxnParser {

    private int index = 0;
    private String hexData;

    public TwentyNineTxnParser(String hexData) {
        this.hexData = hexData;
    }

    private String getBytes(int length) {
        String bytesValue = hexData.substring(index, index + length * 2);
        index += length * 2;
        return bytesValue;
    }

    private Map<String, String> parseTransaction() {
        Map<String, String> transaction = new HashMap<>();
        transaction.put("Pump Number", getBytes(1));
        transaction.put("Nozzle Number", getBytes(1));
        transaction.put("Year", getBytes(1));
        transaction.put("Month", getBytes(1));
        transaction.put("Day", getBytes(1));
        transaction.put("Unique ID", getBytes(2));
        transaction.put("Hour", getBytes(1));
        transaction.put("Minute", getBytes(1));
        transaction.put("Second", getBytes(1));
        transaction.put("Volume", getBytes(4));
        transaction.put("Amount", getBytes(4));
        transaction.put("Product Price", getBytes(4));
        transaction.put("Product ID", getBytes(1));
        transaction.put("Auth-ID", getBytes(2));
        transaction.put("Trx Start Year", getBytes(1));
        transaction.put("Trx Start Month", getBytes(1));
        transaction.put("Trx Start Day", getBytes(1));
        transaction.put("Trx Start Hour", getBytes(1));
        transaction.put("Trx Start Minute", getBytes(1));
        transaction.put("Trx Start Second", getBytes(1));
        transaction.put("Trx Preset Type", getBytes(1));
        transaction.put("Trx Preset Value", getBytes(4));
        transaction.put("Attendant ID", getBytes(1));
        transaction.put("Enable Option Byte", getBytes(1));

        int enableOptionByte = Integer.parseInt(transaction.get("Enable Option Byte"), 16);
        String enableOptionBinary = String.format("%8s", Integer.toBinaryString(enableOptionByte)).replace(' ', '0');
        enableOptionBinary = new StringBuilder(enableOptionBinary).reverse().toString();

        if (enableOptionBinary.charAt(0) == '1') { // Bit 0: Discount
            transaction.put("Discount", getBytes(4));
        }

        transaction.put("Net Amount", getBytes(4));
        transaction.put("MOP", getBytes(1));

        if (enableOptionBinary.charAt(1) == '1') { // Bit 1: Payment Mode
            transaction.put("Payment Mode", getBytes(1));
        }
        if (enableOptionBinary.charAt(2) == '1') { // Bit 2: Mobile Number
            transaction.put("Mobile Number", getBytes(13));
        }
        if (enableOptionBinary.charAt(3) == '1') { // Bit 3: Vehicle Number, Vehicle Type
            transaction.put("Vehicle Number", getBytes(10));
            transaction.put("Vehicle Type", getBytes(1));
        }
        if (enableOptionBinary.charAt(4) == '1') { // Bit 4: Is Transaction Printed
            transaction.put("Is Transaction Printed", getBytes(1));
        }
        if (enableOptionBinary.charAt(5) == '1') { // Bit 5: Voucher ID or Order ID or Account Number
            transaction.put("Voucher ID or Order ID or Account Number", getBytes(20));
        }
        if (enableOptionBinary.charAt(6) == '1') { // Bit 6: Extra or Cash Memo Number
            transaction.put("Extra or Cash Memo Number", getBytes(10));
        }
        if (enableOptionBinary.charAt(7) == '1') { // Bit 7: Extra 1 or Transaction Reference Number
            transaction.put("Extra 1 or Transaction Reference Number", getBytes(40));
        }

        return transaction;
    }

    public Map<String, Object> parseHexData() {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> header = new HashMap<>();

        // Parse header
        header.put("Length", getBytes(2));
        header.put("Command", getBytes(1));
        header.put("ACK", getBytes(1));
        header.put("Pump Number", getBytes(1));
        header.put("Transaction Count", "01"); // Assuming 01 as a placeholder

        List<Map<String, String>> transactions = new ArrayList<>();
        int transactionCount = Integer.parseInt(header.get("Transaction Count"), 16);

        for (int i = 0; i < transactionCount; i++) {
            transactions.add(parseTransaction());
        }

        String crc = getBytes(2);

        result.put("Header", header);
        result.put("Transactions", transactions);
        result.put("CRC", crc);

        return result;
    }

    public static String AuthId(String hexData) {
        TwentyNineTxnParser parser = new TwentyNineTxnParser(hexData);
        Map<String, Object> parsedData = parser.parseHexData();

        // Extract the Auth-ID from the first transaction
        List<Map<String, String>> transactions = (List<Map<String, String>>) parsedData.get("Transactions");
        if (transactions != null && !transactions.isEmpty()) {
            return transactions.get(0).get("Auth-ID");
        } else {
            return null; // Return null if no transactions found
        }
    }

    public static String getYearMonthDayUniqueId(String hexData) {
        TwentyNineTxnParser parser = new TwentyNineTxnParser(hexData);
        Map<String, Object> parsedData = parser.parseHexData();
        List<Map<String, String>> transactions = (List<Map<String, String>>) parsedData.get("Transactions");
        if (transactions != null && !transactions.isEmpty()) {
            String year =  HexToDecimal.convert(transactions.get(0).get("Year"));
            String month =  HexToDecimal.convert(transactions.get(0).get("Month"));
            String day =  HexToDecimal.convert(transactions.get(0).get("Day"));
            String uniqueId =  HexToDecimal.convert(transactions.get(0).get("Unique ID"));

            if (day.length() == 1) {
                day = "0" + day;
            }
            if (month.length() == 1) {
                month = "0" + month;
            }

            if (year.length() == 2) {
                year = "20" + year;
            }
            return CreateTransactionId.chargeslipTxnId(year, month, day, uniqueId);
        } else {
            return null; // Return null if no transactions found
        }
    }

    public static String getBleTxnId(String hexData) {
        TwentyNineTxnParser parser = new TwentyNineTxnParser(hexData);
        Map<String, Object> parsedData = parser.parseHexData();
        List<Map<String, String>> transactions = (List<Map<String, String>>) parsedData.get("Transactions");
        if (transactions != null && !transactions.isEmpty()) {
            String year =  HexToDecimal.convert(transactions.get(0).get("Year"));
            String month =  HexToDecimal.convert(transactions.get(0).get("Month"));
            String day =  HexToDecimal.convert(transactions.get(0).get("Day"));
            String uniqueId =  HexToDecimal.convert(transactions.get(0).get("Unique ID"));

            if (day.length() == 1) {
                day = "0" + day;
            }
            if (month.length() == 1) {
                month = "0" + month;
            }

            if (year.length() == 2) {
                year = "20" + year;
            }
            return CreateTransactionId.create(year, month, day, uniqueId);
        } else {
            return null; // Return null if no transactions found
        }
    }

    public static String getProductName(String hexData) {
        TwentyNineTxnParser parser = new TwentyNineTxnParser(hexData);
        Map<String, Object> parsedData = parser.parseHexData();
        List<Map<String, String>> transactions = (List<Map<String, String>>) parsedData.get("Transactions");
        if (transactions != null && !transactions.isEmpty()) {
            String productId =  HexToDecimal.convert(transactions.get(0).get("Product ID"));
            return getProductNameById(productId);
        } else {
            return null; // Return null if no transactions found
        }
    }

    public static String getProductId(String hexData) {
        TwentyNineTxnParser parser = new TwentyNineTxnParser(hexData);
        Map<String, Object> parsedData = parser.parseHexData();
        List<Map<String, String>> transactions = (List<Map<String, String>>) parsedData.get("Transactions");
        if (transactions != null && !transactions.isEmpty()) {
            String productId =  HexToDecimal.convert(transactions.get(0).get("Product ID"));
            return productId;
        } else {
            return null; // Return null if no transactions found
        }
    }


    public static String getAmount(String hexData) {
        TwentyNineTxnParser parser = new TwentyNineTxnParser(hexData);
        Map<String, Object> parsedData = parser.parseHexData();
        List<Map<String, String>> transactions = (List<Map<String, String>>) parsedData.get("Transactions");
        if (transactions != null && !transactions.isEmpty()) {
            return HexToDecimal.convertAmount(transactions.get(0).get("Amount"));
        } else {
            return null; // Return null if no transactions found
        }
    }

    public static String getVolume(String hexData) {
        TwentyNineTxnParser parser = new TwentyNineTxnParser(hexData);
        Map<String, Object> parsedData = parser.parseHexData();
        List<Map<String, String>> transactions = (List<Map<String, String>>) parsedData.get("Transactions");
        if (transactions != null && !transactions.isEmpty()) {
            return HexToDecimal.convertLitre(transactions.get(0).get("Volume"));
        } else {
            return null; // Return null if no transactions found
        }
    }

    public static String fccTimeStamp(String hexData) {
        TwentyNineTxnParser parser = new TwentyNineTxnParser(hexData);
        Map<String, Object> parsedData = parser.parseHexData();
        List<Map<String, String>> transactions = (List<Map<String, String>>) parsedData.get("Transactions");
        if (transactions != null && !transactions.isEmpty()) {
            String year =  HexToDecimal.convert(transactions.get(0).get("Year"));
            String month =  HexToDecimal.convert(transactions.get(0).get("Month"));
            String day =  HexToDecimal.convert(transactions.get(0).get("Day"));
            String hours = HexToDecimal.convert(transactions.get(0).get("Hour"));
            String minute = HexToDecimal.convert(transactions.get(0).get("Minute"));
            String seconds = HexToDecimal.convert(transactions.get(0).get("Second"));

            if (day.length() == 1) {
                day = "0" + day;
            }
            if (month.length() == 1) {
                month = "0" + month;
            }
            if (year.length() == 2) {
                year = "20" + year;
            }
            if (hours.length() == 1) {
                hours = "0" + hours;
            }
            if (minute.length() == 1) {
                minute = "0" + minute;
            }
            if (seconds.length() == 1) {
                seconds = "0" + seconds;
            }

            return year + month + day + hours + minute +seconds;
        } else {
            return null; // Return null if no transactions found
        }
    }

    public static String getTxnStartTime(String hexData) {
        TwentyNineTxnParser parser = new TwentyNineTxnParser(hexData);
        Map<String, Object> parsedData = parser.parseHexData();
        List<Map<String, String>> transactions = (List<Map<String, String>>) parsedData.get("Transactions");
        if (transactions != null && !transactions.isEmpty()) {
            String hours = HexToDecimal.convert(transactions.get(0).get("Trx Start Hour"));
            String minute = HexToDecimal.convert(transactions.get(0).get("Trx Start Minute"));
            String seconds = HexToDecimal.convert(transactions.get(0).get("Trx Start Second"));

            if (hours.length() == 1) {
                hours = "0" + hours;
            }
            if (minute.length() == 1) {
                minute = "0" + minute;
            }
            if (seconds.length() == 1) {
                seconds = "0" + seconds;
            }

            return hours + minute +seconds;
        } else {
            return null; // Return null if no transactions found
        }
    }

    public static String getProductPrice(String hexData) {
        TwentyNineTxnParser parser = new TwentyNineTxnParser(hexData);
        Map<String, Object> parsedData = parser.parseHexData();
        List<Map<String, String>> transactions = (List<Map<String, String>>) parsedData.get("Transactions");
        if (transactions != null && !transactions.isEmpty()) {
            return HexToDecimal.convertLitre(transactions.get(0).get("Product Price"));
        } else {
            return null; // Return null if no transactions found
        }
    }

    public static String getPresetType(String hexData) {
        TwentyNineTxnParser parser = new TwentyNineTxnParser(hexData);
        Map<String, Object> parsedData = parser.parseHexData();
        List<Map<String, String>> transactions = (List<Map<String, String>>) parsedData.get("Transactions");
        if (transactions != null && !transactions.isEmpty()) {
            return HexToDecimal.convert(transactions.get(0).get("Trx Preset Type"));
        } else {
            return null; // Return null if no transactions found
        }
    }
}
