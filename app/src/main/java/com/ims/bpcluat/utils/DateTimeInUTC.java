package com.ims.bpcluat.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class DateTimeInUTC {
    public static String getCurrentUTCDateTime() {
        // Create a calendar instance for the current date and time
        Calendar calendar = Calendar.getInstance();
        // Create a SimpleDateFormat instance with the desired format including milliseconds
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        // Set the time zone to UTC
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        // Return the current date and time in UTC
        return sdf.format(calendar.getTime());
    }

    public static String convertToUTCFormat(String dateStr) {
        // Define the input format (yyyyMMddHHmmss)
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        // Define the output format (yyyy-MM-dd'T'HH:mm:ss.SSS'Z')
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        outputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            // Parse the input date string into a Date object
            return outputFormat.format(inputFormat.parse(dateStr));
        } catch (ParseException e) {
            e.printStackTrace();
            return null; // Return null if parsing fails
        }
    }

    public static String convertCurrentDateWithTime(String timeStr) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String currentDate = dateFormat.format(calendar.getTime());

        // Combine the current date with the passed time (yyyyMMdd + HHmmss)
        String dateTimeStr = currentDate + timeStr;

        // Define the input format (yyyyMMddHHmmss)
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        // Define the output format (yyyy-MM-dd'T'HH:mm:ss.SSS'Z')
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        outputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            // Parse the combined date and time string into a Date object
            return outputFormat.format(inputFormat.parse(dateTimeStr));
        } catch (ParseException e) {
            e.printStackTrace();
            return null; // Return null if parsing fails
        }
    }
}
