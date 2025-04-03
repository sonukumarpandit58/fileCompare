package com.ims.bpcluat.conversion;

public class TextToHex {
    public static String convert(String str,int length) {
        StringBuilder hex = new StringBuilder();

        for (char ch : str.toCharArray()) {
            hex.append(String.format("%02X", (int) ch));
        }

        // Append spaces (0x20) to make it a total of 13 bytes if needed
        while (hex.length() < length) {
            hex.append("20");
        }

        return hex.toString();
    }
}
