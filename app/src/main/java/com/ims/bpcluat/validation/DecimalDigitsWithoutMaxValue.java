package com.ims.bpcluat.validation;

import android.text.InputFilter;
import android.text.Spanned;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DecimalDigitsWithoutMaxValue implements InputFilter {

    private static final Pattern mPattern = Pattern.compile("^[0-9]*+((\\.[0-9]{0,2})?)$");
    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        String input = dest.subSequence(0, dstart) + source.toString() + dest.subSequence(dend, dest.length());
        // Automatically prepend 0 if the first character is a decimal
        if (input.equals(".")) {
            return "0.";
        }
        // Regular expression check
        Matcher matcher = mPattern.matcher(input);
        if (!matcher.matches()) {
            return "";
        }
        return null;
    }
}
