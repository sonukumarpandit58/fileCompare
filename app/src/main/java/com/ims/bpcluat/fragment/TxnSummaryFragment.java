package com.ims.bpcluat.fragment;

import static com.ims.bpcluat.Helper.*;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.ims.bpcluat.dialog.BluetoothConnectionDialog;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.databinding.FragmentTxnSummaryBinding;
import com.ims.bpcluat.helper.ChargeslipHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.ims.bpcluat.helper.ApiHelper.NetworkingApiCallBack;
import com.ims.bpcluat.receiver.ConnectivityReceiver;


public class TxnSummaryFragment extends Fragment implements NetworkingApiCallBack {
    FragmentTxnSummaryBinding binding;
    DatePickerDialog picker;
    ProgressDialog progress;
    ApiHelper api;
    String[][] txnSummaryData;
    String fromDateForNextPage = "", toDateForNextPage = "", startTimeForNextPage = "", endTimeForNextPage = "";
    private ConnectivityReceiver connectivityReceiver = new ConnectivityReceiver();
    public TxnSummaryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTxnSummaryBinding.inflate(inflater, container, false);
        api = new ApiHelper();
        if (myDeviceInfo.equals("A50")) {
            binding.printBtn.setVisibility(View.GONE);
        }

        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (progress.isShowing()) {
                    progress.dismiss();
                    // Toast.makeText(context, "Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        }, 13000);
        String operatorName = operatorFirstName + " " + operatorLastName;
        binding.summaryLoginName.setText("SUMMARY FOR " +operatorName.toUpperCase());

        binding.mid.setText(mid);
        binding.tid.setText(tid);

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        String defaultDateSet = formatter.format(date);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        String defaultTime = timeFormat.format(date);

        binding.fromDate.setText(defaultDateSet);
        binding.toDate.setText(defaultDateSet);
        binding.startTime.setText(txnSummaryFromDateByDefault(operatorLoginTime));
        binding.endTime.setText(defaultTime);
        if (connectivityReceiver.isConnected(getContext())) {
            fetchTxnSummaryData("","","","","default");
        } else {
            MessagesDialog.showDialog(requireContext(), "No internet connection", 0,null, null);
        }

        binding.printBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    JSONArray jsonArray = new JSONArray();
                    for (String[] row : txnSummaryData) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("mop", row[0]);
                        jsonObject.put("count", row[1]);
                        jsonObject.put("amount", row[2]);
                        jsonArray.put(jsonObject);
                    }
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("attendentName",operatorName);
                    jsonObject.put("fromDate", fromDateForNextPage);
                    jsonObject.put("toDate", toDateForNextPage);
                    jsonObject.put("startTime", convert12HrWithoutSeconds(startTimeForNextPage));
                    jsonObject.put("endTime", convert12HrWithoutSeconds(endTimeForNextPage));
                    jsonObject.put("mid",mid);
                    jsonObject.put("tid",tid);
                    jsonObject.put("txnSummaryData",jsonArray);
                    ChargeslipHelper chargeslipHelper = new ChargeslipHelper();
                    Bitmap cs = chargeslipHelper.txnSummaryChargeslip(getActivity(), jsonObject);
                    ChargeslipHelper helper = new ChargeslipHelper();
                    helper.txnSummaryPrintDialog(getActivity(), getString(R.string.logoutMessage), cs);
                }catch (Exception e){
                    Log.d("Exception99",e.toString());
                }
            }
        });

        binding.homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SideBarActivity.class);
                startActivity(intent);
            }
        });

        binding.txnSummarySubmitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fromDate = binding.fromDate.getText().toString().trim();
                String toDate = binding.toDate.getText().toString().trim();
                String startTime = binding.startTime.getText().toString().trim();
                String endTime = binding.endTime.getText().toString().trim();

                String[] splitFromDate = fromDate.split("-");
                String selectedFromDay = splitFromDate[0];
                String selectedFromMonth = splitFromDate[1];
                String selectedFromYear = splitFromDate[2];
                String selectedFromDate = selectedFromYear+""+selectedFromMonth+selectedFromDay;

                String[] splitToDate = toDate.split("-");
                String selectedToDay = splitToDate[0];
                String selectedToMonth = splitToDate[1];
                String selectedToYear = splitToDate[2];
                String selectedToDate = selectedToYear+""+selectedToMonth+selectedToDay;

                String[] splitStartTime = startTime.split(":");
                String startTimeHr = splitStartTime[0];
                String startTimeMin = splitStartTime[1];
                String selectedStartTime = startTimeHr+""+startTimeMin+"00";

                String[] splitEndTime = endTime.split(":");
                String endTimeHr = splitEndTime[0];
                String endTimeMin = splitEndTime[1];
                String selectedEndTime = endTimeHr+""+endTimeMin+"00";

                // Correct SimpleDateFormat for date only
                SimpleDateFormat sdfDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                // Correct SimpleDateFormat for date and time
                SimpleDateFormat sdfDateTime = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
                try {
                    // Combine date and time correctly for parsing
                    Date fromDateTime = sdfDateTime.parse(fromDate + " " + startTime);
                    Date toDateTime = sdfDateTime.parse(toDate + " " + endTime);

                    if (fromDateTime != null && toDateTime != null) {
                        if (toDateTime.before(fromDateTime)) {
                            Toast.makeText(getActivity(), "End Time should be always greater than Start Time", Toast.LENGTH_SHORT).show();
                        } else {
                            Boolean biggerDateCheck = sdfDate.parse(fromDate).before(sdfDate.parse(toDate));
                            if (!biggerDateCheck) {
                                Boolean equalDateCheck = sdfDate.parse(fromDate).equals(sdfDate.parse(toDate));
                                if (equalDateCheck) {
                                    if (connectivityReceiver.isConnected(getContext())) {
                                        fetchTxnSummaryData(selectedFromDate, selectedToDate, selectedStartTime, selectedEndTime, "search");
                                    } else {
                                        MessagesDialog.showDialog(requireContext(), "No internet connection", 0,null, null);
                                    }
                                } else {
                                    Toast.makeText(getActivity(), "To Date should be always greater than From Date", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                if (connectivityReceiver.isConnected(getContext())) {
                                    fetchTxnSummaryData(selectedFromDate, selectedToDate, selectedStartTime, selectedEndTime, "search");
                                } else {
                                    MessagesDialog.showDialog(requireContext(), "No internet connection", 0,null, null);
                                }
                            }
                        }
                    }
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        binding.fromDate.setInputType(InputType.TYPE_NULL);
        binding.toDate.setInputType(InputType.TYPE_NULL);

        binding.fromDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar calendar = Calendar.getInstance();
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                int month = calendar.get(Calendar.MONTH);
                int year = calendar.get(Calendar.YEAR);

                // Set minimum date to two days before the current date
                Calendar minDate = Calendar.getInstance();
                minDate.add(Calendar.DAY_OF_MONTH, -1);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    picker = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                            month = month + 1;
                            String monthString = String.valueOf(month);
                            if (monthString.length() == 1) {
                                monthString = "0" + monthString;
                            }
                            String dayString = String.valueOf(dayOfMonth);
                            if (dayString.length() == 1) {
                                dayString = "0" + dayString;
                            }
                            binding.fromDate.setText(new StringBuilder().append(dayString).append("-")
                                    .append(monthString).append("-").append(year).append(" "));
                        }
                    }, year, month, day);
                    picker.getDatePicker().setMaxDate(calendar.getTimeInMillis()); // Future all date disable
                    picker.getDatePicker().setMinDate(minDate.getTimeInMillis());  // Only current and last two days selectable
                    picker.show();
                }else{
                    DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                            month = month + 1;
                            String monthString = String.valueOf(month);
                            if (monthString.length() == 1) {
                                monthString = "0" + monthString;
                            }
                            String dayString = String.valueOf(dayOfMonth);
                            if (dayString.length() == 1) {
                                dayString = "0" + dayString;
                            }
                            binding.fromDate.setText(new StringBuilder().append(dayString).append("-").append(monthString).append("-").append(year).append(" "));
                        }
                    };

                    Calendar calendar1 = Calendar.getInstance();
                    DatePickerDialog dialog = new DatePickerDialog(getActivity(), listener, calendar1.get(Calendar.YEAR), calendar1.get(Calendar.MONTH), calendar1.get(Calendar.DAY_OF_MONTH));
                    dialog.getDatePicker().setMaxDate(calendar.getTimeInMillis()); // Future all dates disabled
                    dialog.getDatePicker().setMinDate(minDate.getTimeInMillis());  // Only current and last two days selectable
                    dialog.show();
                }
            }
        });

        binding.toDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar calendar = Calendar.getInstance();
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                int month = calendar.get(Calendar.MONTH);
                int year = calendar.get(Calendar.YEAR);
                // date picker dialog

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    picker = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                            month = month + 1;
                            String monthString = String.valueOf(month);
                            if (monthString.length() == 1) {
                                monthString = "0" + monthString;
                            }
                            String dayString = String.valueOf(dayOfMonth);
                            if (dayString.length() == 1) {
                                dayString = "0" + dayString;
                            }
                            binding.toDate.setText(new StringBuilder().append(dayString).append("-")
                                    .append(monthString).append("-").append(year).append(" "));
                        }
                    }, year, month, day);
                    picker.getDatePicker().setMaxDate(calendar.getTimeInMillis()); // Future all date disable
                    picker.getDatePicker().setMinDate(calendar.getTimeInMillis());
                    picker.show();
                }else{
                    DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                            month = month + 1;
                            String monthString = String.valueOf(month);
                            if (monthString.length() == 1) {
                                monthString = "0" + monthString;
                            }
                            String dayString = String.valueOf(dayOfMonth);
                            if (dayString.length() == 1) {
                                dayString = "0" + dayString;
                            }
                            binding.toDate.setText(new StringBuilder().append(dayString).append("-")
                                    .append(monthString).append("-").append(year).append(" "));
                        }
                    };

                    Calendar calendar1 = Calendar.getInstance();
                    new DatePickerDialog(getActivity(),listener,calendar1.get(Calendar.YEAR),calendar1.get(Calendar.MONTH),calendar1.get(Calendar.DAY_OF_MONTH)).show();
                }
            }
        });

        binding.startTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar c = Calendar.getInstance();
                int hour = c.get(Calendar.HOUR_OF_DAY);
                int minute = c.get(Calendar.MINUTE);
                TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        String hr = String.valueOf(hourOfDay);
                        String min = String.valueOf(minute);
                        if(hr.length() == 1){
                            hr = "0"+hr;
                        }
                        if(min.length() == 1){
                            min = "0"+min;
                        }
                        binding.startTime.setText(hr + ":" + min);
                    }
                }, hour, minute, false);
                timePickerDialog.show();
            }
        });

        binding.endTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar c = Calendar.getInstance();
                int hour = c.get(Calendar.HOUR_OF_DAY);
                int minute = c.get(Calendar.MINUTE);
                TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        String hr = String.valueOf(hourOfDay);
                        String min = String.valueOf(minute);
                        if(hr.length() == 1){
                            hr = "0"+hr;
                        }
                        if(min.length() == 1){
                            min = "0"+min;
                        }
                        binding.endTime.setText(hr + ":" + min);
                    }
                }, hour, minute, false);
                timePickerDialog.show();
            }
        });
        return binding.getRoot();
    }

    public void fetchTxnSummaryData(String selectedFromDate, String selectedToDate, String selectedStartTime, String selectedEndTime,String type) {
        fromDateForNextPage = binding.fromDate.getText().toString().trim();
        toDateForNextPage = binding.toDate.getText().toString().trim();
        startTimeForNextPage = binding.startTime.getText().toString().trim();
        endTimeForNextPage = binding.endTime.getText().toString().trim();

        String url = "getBillerTxnSummary";
        progress.show();
        JSONObject jsonObject = new JSONObject();
        try {
            if(type.equals("default")){
                jsonObject.put("client",manualGetClientId());
                jsonObject.put("instId",manualGetInstId());
                jsonObject.put("userId", username);
                jsonObject.put("channel", channelName);
                jsonObject.put("tid", tid);
                jsonObject.put("source", "TERMINAL");
                jsonObject.put("reqDate", requestDate());
                jsonObject.put("reqTime", requestTime());
                jsonObject.put("fromDate", "");
                jsonObject.put("toDate", "");
                jsonObject.put("fromTime", operatorLoginTime);
                api.networking(jsonObject, url, "20");
            }else{
                jsonObject.put("client",manualGetClientId());
                jsonObject.put("instId",manualGetInstId());
                jsonObject.put("userId", username);
                jsonObject.put("channel", channelName);
                jsonObject.put("tid", tid);
                jsonObject.put("source", "TERMINAL");
                jsonObject.put("reqDate", requestDate());
                jsonObject.put("reqTime", requestTime());
                jsonObject.put("fromDate", selectedFromDate);
                jsonObject.put("toDate", selectedToDate);
                jsonObject.put("fromTime", selectedStartTime);
            }
            Log.d("TxnSummaryRequest = ", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this::apiResult);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean checktimings(String startTime, String endTime) {
        String pattern = "HH:mm";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        try {
            Date date1 = sdf.parse(startTime);
            Date date2 = sdf.parse(endTime);
            if(date1.before(date2)) {
                return true;
            } else {
                if(date1.equals(date2)){
                    return true;
                }else{
                    return false;
                }
            }
        } catch (ParseException e){
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void apiResult(String res, String apiName) {
        Log.d("TxnSummaryResponse",res);
        try {
            if (res.equals("Server Time Out")) {
                progress.dismiss();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), "Server Time Out", Toast.LENGTH_SHORT).show();
                    }
                });
            }else{
                JSONObject jsonObject = new JSONObject(res);
                JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                String respCode = payLoad.getString("respCode");
                JSONArray outputArray = payLoad.getJSONArray("output");
                if(respCode.equals("200")){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binding.noTxnFound.setVisibility(View.GONE);
                            try {
                                List<String[]> paymentEntries = new ArrayList<>();
                                List<String[]> totalEntries = new ArrayList<>();
                                Map<String, Integer> paymentCountMap = new HashMap<>();
                                Map<String, Double> paymentVolumeMap = new HashMap<>();
                                int totalSaleCount = 0;
                                double totalSaleVolume = 0.0;

                                for (int i = 0; i < outputArray.length(); i++) {
                                    JSONObject entryObject = outputArray.getJSONObject(i);
                                    String methodOfPayment = entryObject.optString("methodOfPayment", "");
                                    String summaryType = entryObject.optString("summaryType", "");
                                    int saleCount = entryObject.getInt("totalSaleCount");
                                    double saleVolume = entryObject.getDouble("totalSaleVolume");

                                    if (!methodOfPayment.isEmpty()) {
                                        if (paymentCountMap.containsKey(methodOfPayment)) {
                                            paymentCountMap.put(methodOfPayment, paymentCountMap.get(methodOfPayment) + saleCount);
                                            paymentVolumeMap.put(methodOfPayment, paymentVolumeMap.get(methodOfPayment) + saleVolume);
                                        } else {
                                            paymentCountMap.put(methodOfPayment, saleCount);
                                            paymentVolumeMap.put(methodOfPayment, saleVolume);
                                        }
                                    }

                                    if (summaryType.equals("TOTAL")) {
                                        totalSaleCount += saleCount;
                                        totalSaleVolume += saleVolume;
                                    }
                                }

                                // Add payment entries
                                for (Map.Entry<String, Integer> entry : paymentCountMap.entrySet()) {
                                    String[] paymentEntry = new String[4];
                                    paymentEntry[0] = entry.getKey();
                                    paymentEntry[1] = String.valueOf(entry.getValue());
                                    paymentEntry[2] = String.format("%.2f", paymentVolumeMap.get(entry.getKey()));
                                    paymentEntry[3] = "View More";
                                    paymentEntries.add(paymentEntry);
                                }

                                // Add total entries
                                String[] totalEntry = new String[4];
                                totalEntry[0] = "TOTAL";
                                totalEntry[1] = String.valueOf(totalSaleCount);
                                totalEntry[2] = String.format("%.2f", totalSaleVolume);
                                totalEntry[3] = "";
                                totalEntries.add(totalEntry);

                                // Combine lists with method of payment entries first and TOTAL entries last
                                List<String[]> combinedEntries = new ArrayList<>();
                                combinedEntries.addAll(paymentEntries);
                                combinedEntries.addAll(totalEntries);

                                // Convert combined list to array
                                String[][] data = new String[combinedEntries.size()][4];
                                for (int i = 0; i < combinedEntries.size(); i++) {
                                    data[i] = combinedEntries.get(i);
                                }
                                txnSummaryData = data;
                                tableData(data);
                            } catch (Exception e) {
                                Log.d("Exception", e.toString());
                            }
                        }
                    });
                }else{
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progress.dismiss();
                            binding.noTxnFound.setVisibility(View.VISIBLE);
                            binding.tableLayout.setVisibility(View.GONE);
                            binding.printBtn.setVisibility(View.GONE);
                        }
                    });
                }
            }
        } catch (JSONException e) {
//            Log.d("TxnSummaryFragment",e.toString());
            throw new RuntimeException(e);

        }
    }

    public void tableData(String[][] data) {
        TableLayout tableLayout = binding.tableLayout;
        int childCount = tableLayout.getChildCount();
        if (childCount > 1) {
            tableLayout.removeViews(1, childCount - 1);
        }
        // Calculate a dynamic height based on the device's screen size
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float dpHeight = displayMetrics.heightPixels / displayMetrics.density;
        int minHeight = (int) (dpHeight / 15); // Adjust the divisor to control the height
        for (int rowIndex = 0; rowIndex < data.length; rowIndex++) {
            String[] row = data[rowIndex];
            TableRow tableRow = new TableRow(getActivity());
            tableRow.setLayoutParams(new TableRow.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            tableRow.setBackground(getResources().getDrawable(R.drawable.cell_border));

            for (int colIndex = 0; colIndex < row.length; colIndex++) {
                String cell = row[colIndex];
                TextView textView = new TextView(getActivity());
                textView.setText(cell);
                textView.setPadding(4, 4, 4, 4);
                textView.setTextSize(15);
                textView.setTextColor(getResources().getColor(android.R.color.black));
                textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                textView.setMinHeight(minHeight);
                TableRow.LayoutParams params = new TableRow.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                textView.setLayoutParams(params);

                if (colIndex == 3) { // Action column
                    textView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                    textView.setOnClickListener(v -> {
                        //Toast.makeText(getActivity(), "Clicked on row: " + row[0], Toast.LENGTH_SHORT).show();
                        if(!row[0].equals("Total")){
                            SharedPreferences shared = getActivity().getSharedPreferences("txnSummaryDetailData", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = shared.edit();
                            editor.putString("fromDate",fromDateForNextPage);
                            editor.putString("toDate",toDateForNextPage);
                            editor.putString("startTime",startTimeForNextPage);
                            editor.putString("endTime",endTimeForNextPage);
                            editor.putString("paymentMode",row[0]);
                            editor.commit();
                            if (connectivityReceiver.isConnected(getContext())) {
                                ((SideBarActivity) requireActivity()).loadFragement(new TxnSummaryDetailFragment());
                            } else {
                                MessagesDialog.showDialog(requireContext(), "No internet connection", 0,null, null);
                            }
                        }
                        textView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                    });
                } else {
                    textView.setBackground(getResources().getDrawable(R.drawable.cell_border));
                }
                tableRow.addView(textView);
            }

            tableLayout.addView(tableRow);
            binding.tableLayout.setVisibility(View.VISIBLE);
            if (myDeviceInfo.equals("A50")) {
                binding.printBtn.setVisibility(View.GONE);
            }else{
                binding.printBtn.setVisibility(View.VISIBLE);
            }
            progress.dismiss();
        }
    }
}