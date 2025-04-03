package com.ims.bpcluat.conversion;

public class StringToHexadecimal {
    public static String create(String text) {
        StringBuilder hexString = new StringBuilder();
        for (char ch : text.toCharArray()) {
            hexString.append(Integer.toHexString(ch));
        }
        return hexString.toString();
    }
}
