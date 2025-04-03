package com.ims.bpcluat.conversion;

import android.util.Log;

public class HexToDecimal {
    public static String convert(String hexString) {
        int decimalValue = Integer.parseInt(hexString, 16);
        String decimalValueStr = String.valueOf(decimalValue);
        return decimalValueStr;
    }

    public static String convertAmount(String hexString) {
        int decimalValue = Integer.parseInt(hexString, 16);
        double result = decimalValue / 100.0;
        String amount = String.valueOf(result);
        return amount;
    }

    public static String convertLitre(String hexString) {
        int decimalValue = Integer.parseInt(hexString, 16);
        double result = decimalValue / 100.0;
        String litre = String.valueOf(result);
        return litre;
    }

    public static String convertProduct(String hexString) {
        int decimalValue = Integer.parseInt(hexString, 16);
        String decimalValueStr = String.valueOf(decimalValue);
        return decimalValueStr;
    }
}
