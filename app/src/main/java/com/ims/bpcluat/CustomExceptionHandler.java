package com.ims.bpcluat;

import static com.ims.bpcluat.Helper.fileWrite;

import android.content.Context;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CustomExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Context context;

    public CustomExceptionHandler(Context context) {
        this.context = context;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        // Get the current date and time for the filename
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        String timestamp = formatter.format(date);
        String fileName = "Crash_" + timestamp + ".txt";

        // Get the stack trace as a string
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();

        // Write the crash log to a file
        fileWrite(context, fileName, "App Crash", stackTrace);

        // Optionally, rethrow the exception to let the system handle it (e.g., showing crash dialog)
        System.exit(2);
    }

}

