package com.ims.bpcluat.validation;

public class MobileNoValidation {
    public static boolean hasSameNumber(String input) {
        if (input == null || input.isEmpty()) {
            return false;  // Handle null or empty string case
        }

        char firstChar = input.charAt(0);  // Get the first character

        // Check each character starting from index 1
        for (int i = 1; i < input.length(); i++) {
            if (input.charAt(i) != firstChar) {
                return false;  // Found a character different from the first one
            }
        }

        return true;  // All characters are the same
    }

    public static boolean startsWithZeroNumber(String input) {
        if (input == null || input.isEmpty()) {
            return false;  // Handle null or empty string case
        }
        char firstChar = input.charAt(0);  // Get the first character
        // Check if the first character is '0'
        return firstChar == '0';
    }

    public static boolean checkMobileNumberIsValid(String mob) {
        if (mob.length() == 10) {
            if (MobileNoValidation.hasSameNumber(mob)) {
                return false;
            } else if (mob.equals("1234567890")) {
                return false;
            } else if (MobileNoValidation.startsWithZeroNumber(mob)) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }
}
