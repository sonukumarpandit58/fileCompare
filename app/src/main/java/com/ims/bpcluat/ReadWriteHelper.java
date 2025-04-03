package com.ims.bpcluat;

import static com.ims.bpcluat.Helper.serialNumber;
import static com.ims.bpcluat.helper.ApiHelper.uploadFileUrl;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.ims.bpcluat.helper.ApiHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ReadWriteHelper {
    ProgressDialog progress;

    Context context;

    ApiHelper api = new ApiHelper();

    public static long getCurrentTimestampMiles() {
        return System.currentTimeMillis();  // Returns current time in milliseconds
    }

    public static String getCurrentTimestamp() {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyyHHmmss");
        return sdf.format(now);
    }

    private static String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(new Date());
    }

    public static void saveLastUploadTimestamp(Context context, long timestamp) {
        SharedPreferences prefs = context.getSharedPreferences("BPCL_PREFS", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("last_upload_timestamp", timestamp);
        editor.apply();
    }

    public static long getLastUploadTimestamp(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("BPCL_PREFS", Context.MODE_PRIVATE);
        return prefs.getLong("last_upload_timestamp", 0);
    }

    public static void testUpload(Context context) {
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "BPCL Log");
            if (root.exists()) {
                File[] files = root.listFiles();

                if (files != null) {
                    for (File file : files) {
                        notifyApi(file, context);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //REQUEST FILE
    public static void checkAndUploadStoredFiles(Context context) {
        long currentTimestamp = getCurrentTimestampMiles();
        long lastUploadTimestamp = getLastUploadTimestamp(context);
        long timeDifference = currentTimestamp - lastUploadTimestamp;

        if (timeDifference >= 86400000) {
            try {
                File root = new File(Environment.getExternalStorageDirectory(), "BPCL Log");
                if (root.exists()) {
                    File[] files = root.listFiles();

                    if (files != null) {
                        for (File file : files) {
                            if (!file.getName().startsWith("Log_")) {
                                Log.d("checkkk", "checkAndUploadStoredFiles: ");
                                notifyApi(file, context);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            saveLastUploadTimestamp(context, currentTimestamp);
        } else {
            Log.d("FileManagement", "24 hours haven't passed yet, skipping upload.");
        }
    }

    //REQUEST FILE
    public static void createRequestFile(Context context, String content) {
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "BPCL Log");
            if (!root.exists()) {
                root.mkdirs();
            }

            File[] requestFileCheck = root.listFiles((dir, name) -> name.startsWith(Helper.tid + "_"));
            File requestFile;

            if (requestFileCheck != null && requestFileCheck.length > 0) {
                requestFile = requestFileCheck[0];
            } else {
                String sFileName = Helper.tid + "_" + getCurrentTimestamp() + ".txt";
                requestFile = new File(root, sFileName);
            }

            FileWriter writer2 = new FileWriter(requestFile, true);
            writer2.append(content).append("\n");
            writer2.flush();
            writer2.close();

            checkFileSizeAndNotifyApi(requestFile, context);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //REQUEST FILE
    private static void checkFileSizeAndNotifyApi(File file, Context context) {
        long fileSizeInKB = file.length() / 1024;

        if (fileSizeInKB >= 180 && fileSizeInKB <= 200) {
            notifyApi(file, context);
        }
    }

    public static void createLogFile(Context context, String content) {
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "BPCL Log");
            if (!root.exists()) {
                root.mkdirs();
            }

            File[] logFiles = root.listFiles((dir, name) -> name.startsWith("Log_"));

            if (logFiles != null && logFiles.length > 0) {
                Arrays.sort(logFiles, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));

                if (logFiles.length >= 10) {
                    logFiles[0].delete();
                }
            }

            long currentTime = System.currentTimeMillis();

            File logFile;
            boolean newLogFileNeeded = true;


            if (logFiles != null && logFiles.length > 0) {
                logFile = logFiles[logFiles.length - 1];

                long lastModifiedTime = logFile.lastModified();
                if (currentTime - lastModifiedTime < 24 * 60 * 60 * 1000) {
                    // Less than 24 hours since the last log file was modified, continue appending
                    newLogFileNeeded = false;
                }
            }

            // Create a new log file if 24 hours have passed or no log file exists
            if (newLogFileNeeded) {
                String currentDate = getCurrentDate();
                String logFileName = "Log_" + currentDate + ".txt";
                logFile = new File(root, logFileName);
            } else {
                logFile = logFiles[logFiles.length - 1];  // Continue appending to the most recent file
            }

            FileWriter writer = new FileWriter(logFile, true);
            writer.append(content).append("\n\n");
            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void notifyApi(File file, Context context) {
        if (!file.exists()) {
            Log.e("notifyApi", "File not found: " + file.getAbsolutePath());
            return;
        }
        try {
            OkHttpClient client = new OkHttpClient();

            RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/octet-stream"));

            MultipartBody.Builder multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
            multipartBuilder.addFormDataPart("file", file.getName(), fileBody);
            multipartBuilder.addFormDataPart("appName", "bpcl");
            multipartBuilder.addFormDataPart("type", "fuel");
            multipartBuilder.addFormDataPart("serialNumber", serialNumber);

            RequestBody requestBody = multipartBuilder.build();

            Request request = new Request.Builder().
                    url(uploadFileUrl).post(requestBody).
                    header("Application", "ANALYTICS").build();

            new Thread(() -> {
                try {
                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        String res = response.body().string();
                        Log.d("fileUploadRes", res);
                        if (!file.getName().startsWith("Log_")) {
                            if (file.exists()) {
                                file.delete();
                            }
                        }

                    } else {
                        Log.d("fileUploadRes2", response.toString());
                    }
                } catch (IOException e) {
                    Log.e("Second API Exception", e.getMessage());
                }
            }).start();
        } catch (Exception e) {
            Log.e("notifyApi Exception", e.getMessage());
        }
    }


    public static void logUploadApi(Context context) {
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "BPCL Log");
            if (root.exists() && root.isDirectory()) {
                File[] files = root.listFiles();
                if (files != null && files.length > 0) {
                    boolean logFileExists = false;
                    for (File file : files) {
                        if (file.isFile() && file.getName().startsWith("Log_")) {
                            logFileExists = true;
                            OkHttpClient client = new OkHttpClient();
                            RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/octet-stream"));
                            MultipartBody.Builder multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM)
                                    .addFormDataPart("file", file.getName(), fileBody)
                                    .addFormDataPart("appName", "bpcl")
                                    .addFormDataPart("type", "fuel")
                                    .addFormDataPart("serialNumber", serialNumber);
                            RequestBody requestBody = multipartBuilder.build();
                            Request request = new Request.Builder()
                                    .url(uploadFileUrl)
                                    .post(requestBody)
                                    .header("Application", "ANALYTICS")
                                    .build();
                            new Thread(() -> {
                                try {
                                    Response response = client.newCall(request).execute();
                                    if (response.isSuccessful()) {
                                        String res = response.body().string();
                                        Log.d("fileUploadRes", res);
                                        if (file.exists()) {
                                            file.delete();
                                        }
                                    } else {
                                        Log.d("fileUploadRes2", response.toString());
                                    }
                                } catch (IOException e) {
                                    Log.e("Second API Exception", e.getMessage());
                                }
                            }).start();
                        }
                    }
                    if (!logFileExists) {
                        Log.e("noLogUpload", "No Log_ files found for upload.");
                        Toast.makeText(context, "No Log files found for upload.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("logUploadApi", "BPCL Log directory is empty or no files present.");
                }
            } else {
                Log.e("logUploadApi", "BPCL Log directory not found.");
            }
        } catch (Exception e) {
            Log.e("logUploadApi Exception", e.getMessage());
        }
    }

}
