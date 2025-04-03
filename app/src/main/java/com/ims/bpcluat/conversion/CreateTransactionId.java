package com.ims.bpcluat.conversion;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class CreateTransactionId {

    public static String formatUniqueId(String uniqueId) {
        // Ensure unique_id is a string and is 4 digits long
        uniqueId = String.format("%04d", Integer.parseInt(uniqueId));
        // Format the unique_id based on the given rules
        String uniquelengthCode = String.valueOf(uniqueId.length());
        if (uniquelengthCode.equals("4")) {
            uniqueId = '0' + uniqueId;
        }
        // If there's a zero at the end, keep the unique ID as it is.

        return uniqueId;
    }

    public static List<Integer> createTxnString(String year, String month, String day, String uniqueId) {
        // Step 1: Pick the 2nd digit of the year
        Log.d("myLogYear",year);
        Log.d("mymonth",month);
        Log.d("myLogday",day);
        Log.d("myLoguniq",uniqueId);

        String x = year.substring(3, 4);
        Log.d("myLogx",x);
        // Step 2: Get the 2-digit month
        String y = String.format("%02d", Integer.parseInt(month));
        Log.d("myLogy",y);
        // Step 3: Get the 2-digit day
        String z = String.format("%02d", Integer.parseInt(day));
        Log.d("myLogz",z);
        // Step 4: Format the unique ID
        String k = formatUniqueId(uniqueId);
        Log.d("myLogk",k);
        String finalString = x + y + z + k;
        System.out.println("Final String: " + finalString); // Debugging line
        List<Integer> byteArray = new ArrayList<>();
        for (int i = 0; i < finalString.length(); i += 2) {
            String pair = finalString.substring(i, Math.min(i + 2, finalString.length()));
            byteArray.add(Integer.parseInt(pair));
        }

        return byteArray;
    }

    public static String byteArrayToHex(List<Integer> byteArray) {
        // Convert each byte to its hexadecimal representation and make it uppercase
        StringBuilder hexString = new StringBuilder();
        for (int byteValue : byteArray) {
            hexString.append(String.format("%02X", byteValue));
        }
        return hexString.toString();
    }

    public static String create(String year,String month, String day, String uniqueId) {
        List<Integer> txnString = createTxnString(year, month, day, uniqueId);
        System.out.println("Transaction String: " + txnString);  // Expected output: [40, 62, 00, 49, 16]

        String hexString = byteArrayToHex(txnString);
        System.out.println("Hex String: " + hexString);  // Expected output: "4062004916"
        return hexString;
    }

    public static String chargeslipTxnId(String year,String month, String day, String uniqueId) {
        String x = year.substring(3, 4);
        Log.d("myLogx",x);
        // Step 2: Get the 2-digit month
        String y = String.format("%02d", Integer.parseInt(month));
        Log.d("myLogy",y);
        // Step 3: Get the 2-digit day
        String z = String.format("%02d", Integer.parseInt(day));
        Log.d("myLogz",z);
        // Step 4: Format the unique ID
        String k = formatUniqueId(uniqueId);
        Log.d("myLogk",k);
        String finalString = x + y + z + k;
        return finalString;
    }
}
