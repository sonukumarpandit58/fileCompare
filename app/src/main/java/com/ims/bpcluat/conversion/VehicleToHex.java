package com.ims.bpcluat.conversion;

public class VehicleToHex {
    public static String convert(String str) {
        StringBuilder hex = new StringBuilder();

        for (char ch : str.toCharArray()) {
            hex.append(String.format("%02X", (int) ch));
        }

        return hex.toString();
    }
}
