package com.ims.bpcluat.conversion;

public class DecimalToHex {
    public static String create(int decimal) {
        return Integer.toHexString(decimal).toUpperCase(); // Convert to uppercase for standard hex format
    }
}
