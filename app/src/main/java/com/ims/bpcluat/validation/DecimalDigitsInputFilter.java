package com.ims.bpcluat.validation;

import android.text.InputFilter;
import android.text.Spanned;

public class DecimalDigitsInputFilter implements InputFilter {
    private final int decimalDigits;
    private final double maxValue;

    public DecimalDigitsInputFilter(int decimalDigits, double maxValue) {
        this.decimalDigits = decimalDigits;
        this.maxValue = maxValue;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        StringBuilder builder = new StringBuilder(dest);
        builder.replace(dstart, dend, source.subSequence(start, end).toString());
        String input = builder.toString();

        // Check if the input matches the decimal format with the specified digits
        if (!input.matches("^\\d*\\.?\\d{0," + decimalDigits + "}$")) {
            return ""; // Reject the input
        }

        // Check if the input is less than the maximum allowed value
        try {
            double inputValue = Double.parseDouble(input);
            if (inputValue >= maxValue) {
                return ""; // Reject the input
            }
        } catch (NumberFormatException e) {
            // If parsing fails, reject the input
            return "";
        }

        return null; // Accept the input
    }
}
