package com.ims.bpcluat.fragment;

import static com.ims.bpcluat.Helper.cashChargeslipTime;
import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.convert12HrWithoutSeconds;
import static com.ims.bpcluat.Helper.convertTo12HourFormat;
import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.myDeviceInfo;
import static com.ims.bpcluat.Helper.operatorFirstName;
import static com.ims.bpcluat.Helper.operatorLastName;
import static com.ims.bpcluat.Helper.operatorLoginTime;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.Helper.username;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

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

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.databinding.FragmentTxnSummaryBinding;
import com.ims.bpcluat.databinding.FragmentTxnSummaryDetailBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.helper.ChargeslipHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TxnSummaryDetailFragment extends Fragment implements ApiHelper.NetworkingApiCallBack {
    FragmentTxnSummaryDetailBinding binding;
    DatePickerDialog picker;
    ProgressDialog progress;
    ApiHelper api;
    String paymentMode;
    String[][] txnSummaryData;
    String chargeslipStartDate, chargeslipToDate, chargeslipStartTime, chargeslipEndTime, totalAmountPrint, totalCountPrint;

    public TxnSummaryDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTxnSummaryDetailBinding.inflate(inflater, container, false);
        String operatorName = operatorFirstName + " " + operatorLastName;
        binding.mid.setText(mid);
        binding.tid.setText(tid);
        Log.d("page", "TxnSummaryDetailFragement");
        api = new ApiHelper();
        if (myDeviceInfo.equals("A50")) {
            binding.printBtn.setVisibility(View.GONE);
        }

        SharedPreferences shared = getActivity().getSharedPreferences("txnSummaryDetailData", Context.MODE_PRIVATE);
        String fromDate = (shared.getString("fromDate", ""));
        String toDate = (shared.getString("toDate", ""));
        String startTime = (shared.getString("startTime", ""));
        String endTime = (shared.getString("endTime", ""));
        paymentMode = (shared.getString("paymentMode", ""));

        binding.fromDate.setText(fromDate);
        binding.toDate.setText(toDate);
        binding.startTime.setText(startTime);
        binding.endTime.setText(endTime);
        binding.summaryLoginName.setText(paymentMode + " SUMMARY FOR " + operatorName.toUpperCase());

        Log.d("page", "TxnSummaryDetailFragement1");
        getBillerTxnApi();
        Log.d("page", "TxnSummaryDetailFragement2");

        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new TxnHistorySummaryFragment());
            }
        });

        binding.printBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    JSONArray jsonArray = new JSONArray();
                    for (String[] row : txnSummaryData) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("amount", row[0]);
                        jsonObject.put("time", row[1]);
                        jsonObject.put("pumpNo", row[2]);
                        jsonArray.put(jsonObject);
                    }
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("attendentName", paymentMode + " SUMMARY FOR " + operatorName);
                    jsonObject.put("fromDate", chargeslipStartDate);
                    jsonObject.put("toDate", chargeslipToDate);
                    jsonObject.put("startTime", convert12HrWithoutSeconds(chargeslipStartTime));
                    jsonObject.put("endTime", convert12HrWithoutSeconds(chargeslipEndTime));
                    jsonObject.put("totalCount", totalCountPrint);
                    jsonObject.put("totalAmount", totalAmountPrint);
                    jsonObject.put("mid", mid);
                    jsonObject.put("tid", tid);
                    jsonObject.put("txnSummaryData", jsonArray);

                    Log.d("printJsonObject", String.valueOf(jsonObject));
                    ChargeslipHelper chargeslipHelper = new ChargeslipHelper();
                    Bitmap cs = chargeslipHelper.txnSummaryDetailChargeslip(getActivity(), jsonObject);
                    ChargeslipHelper helper = new ChargeslipHelper();
                    helper.printTxnDetailSummary(getActivity(), cs);
                } catch (Exception e) {
                    Log.d("Exception99", e.toString());
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
                } else {
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
                } else {
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
                    new DatePickerDialog(getActivity(), listener, calendar1.get(Calendar.YEAR), calendar1.get(Calendar.MONTH), calendar1.get(Calendar.DAY_OF_MONTH)).show();
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
                        if (hr.length() == 1) {
                            hr = "0" + hr;
                        }
                        if (min.length() == 1) {
                            min = "0" + min;
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
                        if (hr.length() == 1) {
                            hr = "0" + hr;
                        }
                        if (min.length() == 1) {
                            min = "0" + min;
                        }
                        binding.endTime.setText(hr + ":" + min);
                    }
                }, hour, minute, false);
                timePickerDialog.show();
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
                                    getBillerTxnApi();
                                } else {
                                    Toast.makeText(getActivity(), "To Date should be always greater than From Date", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                getBillerTxnApi();
                            }
                        }
                    }
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return binding.getRoot();
    }

    private void getBillerTxnApi() {
        String fromDate = Helper.dateRequestFormat(binding.fromDate.getText().toString().trim());
        String startTime = Helper.timeRequestFormat(binding.startTime.getText().toString().trim());
        chargeslipStartDate = binding.fromDate.getText().toString().trim();
        chargeslipToDate = binding.toDate.getText().toString().trim();
        chargeslipStartTime = binding.startTime.getText().toString().trim();
        chargeslipStartTime = chargeslipStartTime + ":00";
        chargeslipEndTime = binding.endTime.getText().toString().trim();
        chargeslipEndTime = chargeslipEndTime + ":00";
        String url = "getBillerTxn";
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);
        progress.show();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("userName", username);
            jsonObject.put("channel", channelName);
            jsonObject.put("tid", tid);
            jsonObject.put("source", "TERMINAL");
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("mop", paymentMode);
            jsonObject.put("fromDate", fromDate);
            jsonObject.put("fromTime", startTime);
            Log.d("mopWiseRequest = ", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this::apiResult);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void apiResult(String res, String apiName) {
        Log.d("mopWiseResponse", res);
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
                if (respCode.equals("200")) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONArray outputArray = payLoad.getJSONArray("output");
                                List<String[]> dataList = new ArrayList<>();
                                int count = 0;
                                double totalAmount = 0.0;
                                binding.noTxnFound.setVisibility(View.GONE);
                                for (int i = 0; i < outputArray.length(); i++) {
                                    JSONObject transaction = outputArray.getJSONObject(i);
                                    String transAmt = transaction.getString("transAmt");
                                    String txnTime = transaction.getString("txnTime");
                                    String pumpNo = "";

                                    JSONArray paramListMapping = transaction.getJSONArray("paramListMapping");
                                    for (int j = 0; j < paramListMapping.length(); j++) {
                                        JSONObject paramItem = paramListMapping.getJSONObject(j);
                                        Log.d("paramItem", String.valueOf(paramItem));
                                        int inc = j + 1;
                                        String key = "param" + inc;
                                        String value = "param" + inc + "Lit";
                                        String paramValue = paramItem.getString(key);
                                        String paramLit = paramItem.getString(value);
//                                    if (param.has("param3Lit") && param.getString("param3Lit").equals("PUMP_NO")) {
//                                        pumpNo = param.optString("param3", "");
//                                        break;
//                                    }
                                        switch (paramLit) {
                                            case "PUMP_NO":
                                                pumpNo = paramValue;
                                                break;
                                        }
                                    }

                                    dataList.add(new String[]{transAmt, convertTo12HourFormat(txnTime), pumpNo});
                                    count++;
                                    totalAmount += Double.parseDouble(transAmt);
                                }

                                // Converting List to 2D array
                                String[][] data = new String[dataList.size()][3];
                                for (int i = 0; i < dataList.size(); i++) {
                                    data[i] = dataList.get(i);
                                }

                                totalAmountPrint = Helper.txnAmountUpToTwoDecimal(String.valueOf(totalAmount));
                                totalCountPrint = String.valueOf(count);
                                txnSummaryData = data;
                                tableData(data);
                            } catch (JSONException e) {

                            }

                        }
                    });
                } else {
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
        } catch (Exception e) {
            fileWrite(getContext(), todayDate + ".txt", "getshiftsummaryResponse", e.toString());
            getActivity().runOnUiThread(() -> {
                progress.dismiss();
                Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_SHORT).show();
                if (progress.isShowing()) {
                    progress.dismiss();
                }
                MessagesDialog.showDialog(requireContext(), e.toString(), 0,null, null);

            });
           // Log.d("Exception", e.toString());
        }
    }

    private void tableData(String[][] data) {
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
                tableRow.addView(textView);
            }

            tableLayout.addView(tableRow);
            binding.tableLayout.setVisibility(View.VISIBLE);
            if (myDeviceInfo.equals("A50")) {
                binding.printBtn.setVisibility(View.GONE);
            } else {
                binding.printBtn.setVisibility(View.VISIBLE);
            }
            progress.dismiss();
        }
    }
}