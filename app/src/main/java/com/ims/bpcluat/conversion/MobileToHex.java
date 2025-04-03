package com.ims.bpcluat.conversion;

public class MobileToHex {
    public static String create(String str) {
        StringBuilder hex = new StringBuilder();

        for (char ch : str.toCharArray()) {
            hex.append(String.format("%02X", (int) ch));
        }

        // Append spaces (0x20) to make it a total of 13 bytes if needed
        while (hex.length() < 26) {
            hex.append("20");
        }

        return hex.toString();
    }
}
