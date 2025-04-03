package com.ims.bpcluat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import com.ims.bpcluat.conversion.DecimalToHex;
import com.pax.dal.IDAL;
import com.pax.dal.IPrinter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.util.Base64;
import android.widget.Toast;
import androidx.annotation.NonNull;

public class Helper {
    //Hardware serial no ko remove karna hai
    public static String serialNumber = "";
    private static PermissionCallback permissionCallback;

    public interface PermissionCallback {
        void onPermissionGranted(String serialNumber);
        void onPermissionDenied();
    }

    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    public static String mid = "";
    public static String tid = "";
//    public static String mid = "470000099309183"; // UAT
//    public static String tid = "39287941";  // UAT

//    public static String mid = "470000099156572"; // Production
//    public static String tid = "00116281";  // Production
//    public static String mid = "470000099313126"; // Production
//    public static String tid = "00474884";  // Production
    public static String myDeviceInfo = "";
    public static String pumpFetch = "";
    public static String appVersion = "1.0.20";
    public static String versionDate = "202504031524";
    public static String version = "BPCL" + appVersion;
    public static String loginType = "";
    public static String client = "47000";
    public static String instId = "47";
    public static ArrayList txnArrayList = new ArrayList<>();
    public static List<Map<String, String>> fuelProductList = new ArrayList<>();
    public static IPrinter printer;
    public static IDAL dal;
    public static IPrinter a920printer;
    public static String twentySixRequest = "";
    public static String bleBroadcastingName = "";
    /* Start : Operator related Variables */
    public static String username = "";
    public static String channelName = "BPCL";
    public static String operatorLoginTime = "";
    public static String operatorLoginDate = "";
    public static String sapCode = "";
    public static String operatorFirstName = "";
    public static String operatorLastName = "";
    public static String coverage = "";
    public static String roName = "";
    public static String address1 = "";
    public static String city = "";
    public static String dealerContactNumber = "";
    public static String metaHosSecretKey;
    public static String metaHosVendorId;
    public static String metaHosTokenUrl;
    public static String metaHosPumpUrl;
    public static String metaHosResponse = "";
    public static String roOnlineStatus = "";
    public static String metaHosProduct = "";
    public static String footerMessage = "";
    /* End : Operator related Variables */

    /* Start : Txn related Variables */
    public static List<String> pumpArray = new ArrayList<>();
    public static String txnReleatedPumpNo = "";
    public static JSONArray productsArray;
    public static int txnListPostionSelected;

    public static Date date = new Date();
    public static SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
    public static String todayDate = formatter.format(date);

    public static String bleStatus = "";

    public Helper() {
    }

    public static void getHardwareSerialNumber(final Activity mActivity, PermissionCallback callback) {
        permissionCallback = callback;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ActivityCompat.checkSelfPermission(mActivity, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                // Call Build.getSerial() only on API 26 and above
                serialNumber = Build.getSerial();
                if (serialNumber == null || serialNumber.equals(Build.UNKNOWN)) {
                    serialNumber = "Serial number not available";
                }
                Log.d("SerialNo1", serialNumber);
                permissionCallback.onPermissionGranted(serialNumber);
            } else {
                ActivityCompat.requestPermissions(mActivity, new String[]{android.Manifest.permission.READ_PHONE_STATE}, 1);
            }
        } else {
            // Use Build.SERIAL for API levels below 26
            serialNumber = Build.SERIAL;  // Deprecated, but works on lower API levels
            if (serialNumber == null || serialNumber.equals(Build.UNKNOWN)) {
                serialNumber = "Serial number not available";
            }
            Log.d("SerialNo2", serialNumber);
            permissionCallback.onPermissionGranted(serialNumber);
        }
    }

    public static void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Call Build.getSerial() only on API 26 and above
                    serialNumber = Build.getSerial();
                } else {
                    // Use Build.SERIAL for API levels below 26
                    serialNumber = Build.SERIAL;
                }

                if (serialNumber == null || serialNumber.equals(Build.UNKNOWN)) {
                    serialNumber = "Serial number not available";
                }

                Log.d("SerialNo3", serialNumber);
                permissionCallback.onPermissionGranted(serialNumber);
            } else {
                Log.d("Permission Denied", "Cannot retrieve serial number without permission.");
                permissionCallback.onPermissionDenied();
            }
        }
    }

    public static String reportDate() {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String repDate = dateFormat.format(date);
        return repDate;
    }

    public static String requestDate() {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String reqDate = dateFormat.format(date);
        return reqDate;
    }

    public static String requestTime() {
        Date date = new Date();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss");
        String reqTime = timeFormat.format(date);
        return reqTime;
    }

    public static String cashNotificationDate() {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");
        String reqDate = dateFormat.format(date);
        return reqDate;
    }

    public static String cashNotificationTime() {
        Date date = new Date();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss");
        String reqTime = timeFormat.format(date);
        return reqTime;
    }

    public static String cashChargeslipDate() {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        String reqDate = dateFormat.format(date);
        return reqDate;
    }

    public static String cashChargeslipTime() {
        Date date = new Date();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        String reqTime = timeFormat.format(date);
        return reqTime;
    }

    public static String cardNotificationDate(String dateTime) {
        String resYear = dateTime.substring(0, 4);
        String resMonth = dateTime.substring(4, 6);
        String resDate = dateTime.substring(6, 8);
        return resDate + resMonth + resYear; // DDMMYYYY
    }

    public static String cardNotificationTime(String dateTime) {
        return dateTime.substring(8, 14); // HHMMSS
    }

    public static String cardChargeslipDate(String dateTime) {
        String resYear = dateTime.substring(0, 4);
        String resMonth = dateTime.substring(4, 6);
        String resDate = dateTime.substring(6, 8);
        return resDate + "-" + resMonth + "-" + resYear; // DD-MM-YYYY
    }

    public static String cardChargeslipTime(String dateTime) {
        String time = dateTime.substring(8, 14); // HHMMSS
        String hour = dateTime.substring(8, 10);
        String min = dateTime.substring(10, 12);
        String sec = dateTime.substring(12, 14);
        return hour + ":" + min + ":" + sec;
    }

    public static String dateRequestFormat(String date) {
        String[] splitFromDate = date.split("-");
        String day = splitFromDate[0];
        String month = splitFromDate[1];
        String year = splitFromDate[2];
        return year + "" + month + day;
    }

    public static String timeRequestFormat(String time) {
        String[] splitStartTime = time.split(":");
        String hr = splitStartTime[0];
        String min = splitStartTime[1];
        return hr + "" + min + "00";
    }

    public static String BitMapToString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        String temp = Base64.encodeToString(b, Base64.DEFAULT);
        Log.d("BitmapString", temp);
        return temp;
    }

    public void showToastMessage(final Activity mActivity, String message) {
        LayoutInflater inflater = mActivity.getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_layout, null);
        //View layout = inflater.inflate(R.layout.toast_layout, findViewById(R.id.toast_layout_root));

        TextView text = layout.findViewById(R.id.toast_text);
        text.setText(message);

        Toast toast = new Toast(mActivity);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);

        // Position the toast message at the top
        toast.setGravity(Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 0);
        toast.show();
    }

    public static String padWithZeroes(int number, int length) {
        String myString = String.valueOf(number);
        if (myString.length() <= length) {
            while (myString.length() < length) {
                myString = "0" + myString;
            }
        }
        if (myString.length() > length) {
            return "";
        }
        return myString;
    }

    public static String createTxnIdForOfflineTxn() {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss");
        String reqDate = dateFormat.format(date);
        String reqTime = timeFormat.format(date);
        return reqDate + tid + reqTime;
    }

    public static String txnAmountUpToTwoDecimal(String amt) {
        float amountInDecimal = Float.parseFloat(amt);
        String formattedAmount = String.format("%.2f", amountInDecimal);
        return formattedAmount;
    }

    public static String createCashRrn(String date, String time, String amount) {
        double doubleValue = Double.parseDouble(amount);
        int amountIntegerValue = (int) Math.round(doubleValue);
        return date + time + amountIntegerValue;
    }

    public static String manualGetClientId() {
        String midFirstTwoCharacter = mid.substring(0, 2);
        String manualClientId = "";
        if (midFirstTwoCharacter.equals("47")) {
            manualClientId = "47000";
        } else if (midFirstTwoCharacter.equals("62")) {
            manualClientId = "62001";
        }
        return manualClientId;
    }

    public static JSONObject createJsonObject(String param, String paramLit) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("param", param);
            jsonObject.put("param_lit", paramLit);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public static String reportDateFormatInUtr(String date) {
        // Input date format
        SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MM-yyyy");
        // Output date format
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = null;
        try {
            // Parse the input date string to a Date object
            Date parsedDate = inputFormat.parse(date);
            // Format the Date object to the desired output format
            formattedDate = outputFormat.format(parsedDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return formattedDate;
    }

    public static String manualGetInstId() {
        String midFirstTwoCharacter = mid.substring(0, 2);
        String manualInstId = "";
        if (midFirstTwoCharacter.equals("47")) {
            manualInstId = "47";
        } else if (midFirstTwoCharacter.equals("62")) {
            manualInstId = "62";
        }
        return manualInstId;
    }

    public static String getProductNameById(String id) {
        try {
            for (int i = 0; i < productsArray.length(); i++) {
                JSONObject product = productsArray.getJSONObject(i);
                if (product.getString("id").equals(id)) {
                    return product.getString("productName");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null; // or throw an exception, or return a default value
    }

    public static String bcd2Str(byte[] bArr) {
        if (bArr == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(bArr.length * 2);
        for (byte b : bArr) {
            byte b2 = (byte) ((b & 240) >>> 4);
            byte b3 = (byte) (b & 15);
            byte b4 = (byte) (b2 >= 10 ? ((byte) (b2 - 10)) + 65 : b2 + 48);
            int i = b3 >= 10 ? ((byte) (b3 - 10)) + 65 : b3 + 48;
            sb.append(String.format("%c", new Object[]{Byte.valueOf(b4)}));
            sb.append(String.format("%c", new Object[]{Byte.valueOf((byte) i)}));
        }
        return sb.toString();
    }

    public static String pumpNameExtractFromDeviceName(String deviceName) {
        String ddata = deviceName.substring(deviceName.length() - 4).replaceAll(" ", "");
        StringBuilder specialCharValues = new StringBuilder();
        for (char c : ddata.toCharArray()) {
            specialCharValues.append(String.format("%02X ", (int) c)).append("");
        }

        // Print the integer values of the special characters
        System.out.println("Special Character Values Nware: " + specialCharValues.toString().trim());
        return specialCharValues.toString().replaceAll(" ", "").trim();
    }

    public static String bcd2Str(byte[] bArr, int i) {
        if (bArr == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(i * 2);
        int min = Math.min(bArr.length, i);
        for (int i2 = 0; i2 < min; i2++) {
            char[] cArr = HEX_DIGITS;
            sb.append(cArr[(bArr[i2] & 240) >>> 4]);
            sb.append(cArr[bArr[i2] & 15]);
        }
        return sb.toString();
    }

    public static void homePage(final Activity mActivity) {
        Intent intent = new Intent(mActivity, SideBarActivity.class);
        // intent.putExtra("redirect", "OnlineSingleTransactionFragment");
        txnArrayList.clear();
        mActivity.startActivity(intent);
        mActivity.finish();
    }

    public static void cngHomePage(final Activity mActivity) {
        Intent intent = new Intent(mActivity, SideBarActivity.class);
        intent.putExtra("redirect", "CngFragment");
        mActivity.startActivity(intent);
        mActivity.finish();
    }

    public static void nfrHomePage(final Activity mActivity) {
        SharedPreferences nfrData = mActivity.getSharedPreferences("nfrSharedPreferencesData", Context.MODE_PRIVATE);
        nfrData.edit().clear().commit();
        Intent intent = new Intent(mActivity, SideBarActivity.class);
        intent.putExtra("redirect", "NfrFragment");
        mActivity.startActivity(intent);
        mActivity.finish();
    }

    public static String convertTo12HourFormat(String time24) {
        try {
            // Create a SimpleDateFormat object for parsing the input time
            SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm:ss");
            // Parse the input time string to a Date object
            Date date = inputFormat.parse(time24);

            // Create another SimpleDateFormat object for formatting the Date object to a 12-hour format
            SimpleDateFormat outputFormat = new SimpleDateFormat("hh:mm:ss a");
            // Format the Date object to a 12-hour format time string
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String convert12HrWithoutSeconds(String time24) {
        try {
            // Define the input and output date formats
            SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

            // Parse the input time string into a Date object
            Date date = inputFormat.parse(time24);

            // Format the Date object into the desired output string
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null; // Return null in case of an error
        }
    }

    public static String replaceCommaWithHyphen(String input) {
        if (input.contains(",")) {
            return input.replace(",", "-");
        }
        return input;
    }

    public static String getCardFirst(String maskedCardNo) {
        maskedCardNo = maskedCardNo.toLowerCase().replaceAll("\\s", "");
        return maskedCardNo.substring(0, 6);
    }

    public static String getCardLast(String maskedCardNo) {
        maskedCardNo = maskedCardNo.toLowerCase().replaceAll("\\s", "");
        return maskedCardNo.substring(maskedCardNo.length() - 4);
    }

    public static boolean isValidEmail(CharSequence email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static String reprintDate(String date) {
        String[] txnDate = date.split("/");
        String d = txnDate[0];
        String m = txnDate[1];
        String y = txnDate[2];
        return d + "-" + m + "-" + y;
    }

    public static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // You can change the format and quality of the compression
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    public static Bitmap base64ToBitmap(String base64String) {
        byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }

    public static String upiPrintTime(String time) {
        String h = time.substring(0, 2);
        String m = time.substring(2, 4);
        String s = time.substring(4, 6);
        return h + ":" + m + ":" + s;
    }

    public static String upiPrintDate(String dateTime) {
        String resYear = dateTime.substring(0, 4);
        String resMonth = dateTime.substring(4, 6);
        String resDate = dateTime.substring(6, 8);
        return resDate + "-" + resMonth + "-" + resYear; // DD-MM-YYYY
    }

    public static String upiNotificationDate(String dateTime) {
        String resYear = dateTime.substring(0, 4);
        String resMonth = dateTime.substring(4, 6);
        String resDate = dateTime.substring(6, 8);
        return resDate + resMonth + resYear; // DDMMYYYY
    }

    public static String mobileNumberMasking(String mobileNumber) {
        int mobileNumberLength = mobileNumber.length();
        String mobileLast = mobileNumber.substring(mobileNumberLength - 4);
        String mobileFirst = mobileNumber.substring(0, 2);
        int restCardLength = mobileNumberLength - 6;
        StringBuilder maskedNumber = new StringBuilder(mobileFirst);
        for (int i = 0; i < restCardLength; i++) {
            maskedNumber.append("X");
        }
        maskedNumber.append(mobileLast);
        return maskedNumber.toString();
    }

    public static void closeKeyboard(Activity mActivity) {
        mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mActivity.getWindow().getDecorView().getWindowToken(), 0);
    }

    public static void fileWrite(Context context, String sFileName, String key, String value) {
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "BPCL Log");
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, sFileName);
            FileWriter writer = new FileWriter(gpxfile, true);
            writer.append(synchingTxnDetailForFileWrite(key, value));
            writer.flush();
            writer.close();
            // Toast.makeText(context, "Txn Saved in Folder", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Write content to file
    private static void writeToFile(File file, String content) {
        try (FileOutputStream fos = new FileOutputStream(file, true)) {
            fos.write((content + "\n").getBytes());
        } catch (IOException e) {
            Log.e("firstFileError", "Error writing to file: " + e.getMessage());
        }
    }

    public static String synchingTxnDetailForFileWrite(String key, String value) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(cashChargeslipDate() + " " + cashChargeslipTime());
        sb.append("\n");
        sb.append(key + " " + value);
        return String.valueOf(sb);
    }

    public static String synchingFirstFileWrite(String key, String value) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(key + " " + value);
        return String.valueOf(sb);
    }

    public void logoutDialog(final Activity mActivity) {
        pumpFetch = "";
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        LayoutInflater inflater = mActivity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.print_popup, null);
        builder.setView(dialogView);

        AlertDialog alertDialog = builder.create();
        TextView alertMessage = dialogView.findViewById(R.id.alert_title);
        String alert1 = "Please take print out from Txn Summary before you Logout";
        alertMessage.setText(alert1);
        // Customize dialog buttons if needed
        Button noBtn = dialogView.findViewById(R.id.alert_cancel_button);
        Button yesBtn = dialogView.findViewById(R.id.alert_ok_button);

        noBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                Intent intent = new Intent(mActivity, MainActivity.class);
                mActivity.startActivity(intent);
            }
        });

        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    public static String txnSummaryFromDateByDefault(String time) {
        // Ensure the input string is at least 4 characters long to extract hours and minutes
        if (time.length() >= 4) {
            String hours = time.substring(0, 2);
            String minutes = time.substring(2, 4);
            return hours + ":" + minutes;
        } else {
            throw new IllegalArgumentException("Invalid time format");
        }
    }

    public static boolean isZero(String amt) {
        try {
            // Trim leading zeros
            amt = amt.replaceFirst("^0+(?!$)", "");
            // Parse the amount to a double and check if it is zero
            double amount = Double.parseDouble(amt);
            return amount == 0;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String presetValueForUfillTxn(String amt) {
        // Convert rupees to paise by multiplying by 100
        double paiseAmount = Double.parseDouble(amt) * 100;
        // Convert the paise amount to a string
        String paiseString = String.valueOf((int) paiseAmount); // Convert to integer for hexadecimal conversion
        Log.d("paiseString", paiseString);
        String paiseToHex = DecimalToHex.create(Integer.parseInt(paiseString));
        Log.d("paiseToHex", paiseToHex);
        while (paiseToHex.length() < 8) {
            paiseToHex = "0" + paiseToHex;
        }
        return paiseToHex;
    }

    public static String authCodeForUfillTxn(String str) {
        // Remove the last 4 characters (e.g., 'b266' in this case)
        String withoutLastFour = str.substring(0, str.length() - 4);
        // Get the last 4 characters from the remaining string
        String result = withoutLastFour.substring(withoutLastFour.length() - 4);
        System.out.println("Final result: " + result);
        return result;
    }

    public static String errorCodeForUfillTxn(String str) {
        String successCode = str.substring(14, 16);
        System.out.println("Success Code: " + successCode); // 01 means success
        return successCode;
    }

    public static String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        Date now = new Date();
        return sdf.format(now);
    }

    public static void addProduct(List<Map<String, String>> fuelProductList, String id, String productName, String productAlias) {
        Map<String, String> product = new HashMap<>();
        product.put("id", id);
        product.put("productName", productName);
        product.put("productAlias", productAlias);
        fuelProductList.add(product);
    }

    public static String getProductId(String productAlias, List<Map<String, String>> fuelProductList) {
        productAlias = productAlias.replaceAll("\\s+", "");
        for (Map<String, String> product : fuelProductList) {
            if (product.get("productAlias").equalsIgnoreCase(productAlias)) {
                return product.get("id");
            }
        }
        return null;
    }

    public static String getPresetType(String key) {
        String val = "";
        if (key.equals("0")) {
            val = "No preset";
        } else if (key.equals("1")) {
            val = "Remote volume";
        } else if (key.equals("2")) {
            val = "Remote amount";
        } else if (key.equals("3")) {
            val = "Local volume";
        } else if (key.equals("4")) {
            val = "Local amount";
        } else {
            val = "Top-up";
        }
        return val;
    }

    public static String getPriceByLocalProductID(String metaHosResponse, String localProductID) {
        try {
            JSONArray jsonArray = new JSONArray(metaHosResponse);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject product = jsonArray.getJSONObject(i);
                if (product.getString("localProductID").equals(localProductID)) {
                    return product.getString("price");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String calculateFuelQuantity(String fuelPrice, String customerFuelAmt) {
        try {
            double pricePerLitre = Double.parseDouble(fuelPrice);
            double fuelAmount = Double.parseDouble(customerFuelAmt);
            double quantity = fuelAmount / pricePerLitre;
            return String.format("%.2f", quantity); // Format to 2 decimal places
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return "Invalid input";
        }
    }

    public static String getClientTxnId() {
        String clientTxnId = tid + getCurrentDateTime();
        return clientTxnId;
    }

    public static void logLongMessage(String tag, String message) {
        // Set the maximum chunk size (Logcat has a limit, usually around 4000 characters per log entry)
        int maxLogSize = 4000;
        // Split the message into smaller chunks
        for (int i = 0; i <= message.length() / maxLogSize; i++) {
            int start = i * maxLogSize;
            int end = Math.min((i + 1) * maxLogSize, message.length());
            // Log each chunk
            Log.d(tag, message.substring(start, end));
        }
    }

    public static boolean isAlpCodeExistsForStatusApiCall(String code) {
        if (code.equals("200")) {
            return true;
        } else if (code.equals("4011")) {
            return true;
        } else if (code.equals("4012")) {
            return true;
        } else if (code.equals("4013")) {
            return true;
        } else if (code.equals("4014")) {
            return true;
        } else if (code.equals("3011")) {
            return true;
        } else if (code.equals("3012")) {
            return true;
        } else if (code.equals("3015")) {
            return true;
        } else if (code.equals("3016")) {
            return true;
        } else if (code.equals("3017")) {
            return true;
        } else if (code.equals("3018")) {
            return true;
        }
        return false;
    }

    public static Bitmap resizeBitmap(Bitmap bitmap) {
        int maxHeight = 4096;
        int maxWidth = 4096;

        // Check if resizing is necessary
        if (bitmap.getHeight() > maxHeight || bitmap.getWidth() > maxWidth) {
            // Calculate scale to fit within the max texture size
            float scale = Math.min(((float) maxHeight / bitmap.getHeight()), ((float) maxWidth / bitmap.getWidth()));

            int scaledWidth = Math.round(bitmap.getWidth() * scale);
            int scaledHeight = Math.round(bitmap.getHeight() * scale);

            return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
        }

        // Return the original bitmap if it's within limits
        return bitmap;
    }

    public static String getDeviceUuid(Context context) {
        // Use the ANDROID_ID as a unique identifier for the device
        String uuid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        // Log the UUID for debugging
        if (uuid != null) {
            Log.d("DeviceUtils", "Device UUID: " + uuid);
        } else {
            Log.e("DeviceUtils", "Failed to retrieve Device UUID");
        }
        return uuid != null ? uuid : "unknown";
    }
}
