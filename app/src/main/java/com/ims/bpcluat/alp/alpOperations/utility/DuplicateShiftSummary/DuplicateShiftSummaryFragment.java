package com.ims.bpcluat.alp.alpOperations.utility.DuplicateShiftSummary;

import static com.ims.bpcluat.Helper.appVersion;
import static com.ims.bpcluat.Helper.fileWrite;

import static com.ims.bpcluat.Helper.logLongMessage;
import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.Helper.username;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.alp.alpOperations.utility.ShiftSummary.ShiftSummaryReciept;
import com.ims.bpcluat.alp.alpOperations.utility.UtilityFragment;
import com.ims.bpcluat.databinding.FragmentDuplicateShiftSummaryBinding;
import com.ims.bpcluat.databinding.FragmentShiftSummaryBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

public class DuplicateShiftSummaryFragment extends Fragment implements ApiHelper.NetworkingApiCallBack{
    FragmentDuplicateShiftSummaryBinding binding;
    ProgressDialog progress;
    ApiHelper api;
    Context context;
    String txnId = "", dateTime = "";
    String shiftsummaryApiCall = "";
    String reportType = "";

    private String[] options = {"detail", "summary"};
    private int selectedOptionIndex = -1;
    private ArrayList<String> selectedOptionsList;

    public DuplicateShiftSummaryFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDuplicateShiftSummaryBinding.inflate(inflater, container, false);
        context = getContext();
        api = new ApiHelper();
        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new UtilityFragment());

            }
        });

        selectedOptionsList = new ArrayList<>(Collections.singletonList("Select an option"));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, selectedOptionsList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.mySpinner.setAdapter(adapter);

        binding.mySpinner.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    showOptionsDialog();
                }
                return true;
            }
        });

        binding.selectbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showConfirmationDialog();
            }
        });

        return binding.getRoot();
    }


    private void duplicateShiftSummaryApi() {
        shiftsummaryApiCall = "duplicateShiftSummaryApiCall";

        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false);
        progress.show();

        String url = "alpreq";
        String reqDate = requestDate();
        String reqTime = requestTime();
        txnId = Helper.createTxnIdForOfflineTxn();
        dateTime = reqDate + reqTime;

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("userName", username);
            jsonObject.put("channel", "BPCL");
            jsonObject.put("tid", tid);
            jsonObject.put("mid", mid);
            jsonObject.put("source", "Mobile");
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("dateTime", dateTime);
            jsonObject.put("txnType", "ASU");
            jsonObject.put("reportType", reportType);
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnId", txnId);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);

            Log.d("shiftsummaryRequest", String.valueOf(jsonObject));

            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);

        } catch (JSONException e) {
            Log.d("respCode", "respCode");
            progress.dismiss();
        }
    }

    public void apiResult(String res, String apiName) {
        if (shiftsummaryApiCall.equals("duplicateShiftSummaryApiCall")) {

            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                    });
                } else {
                   // Log.d("duplicateShiftRes = ", res);
                    logLongMessage("duplicateShiftRes = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");
                    String reportType = payLoad.getString("reportType");

                    if (respCode.equals("200")) {
                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            Log.d("SelectedOption", respCode);
                            Intent intent = new Intent(context, ShiftSummaryReciept.class);
                            intent.putExtra("payload", payLoad.toString());
                            intent.putExtra("summary", "duplicate");
                            context.startActivity(intent);
                        });

                    } else {
                        getActivity().runOnUiThread(() -> {
                            Log.d("SelectedOption", respCode);

                            progress.dismiss();
                            MessagesDialog.showDialog(context, respDesc, 0,null, null);
                        });
                    }
                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "getshiftsummaryResponse", e.toString());
                getActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    MessagesDialog.showDialog(context, e.toString(), 0,null, null);
                });
            }
        }
    }

    private void showOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Purpose");
        builder.setSingleChoiceItems(options, selectedOptionIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedOptionIndex = which;
                if (selectedOptionIndex != -1) {
                    String selectedOption = options[selectedOptionIndex];
                    Log.d("SelectedOption", selectedOption);
                    updateSpinnerSelection(selectedOption);
                    dialog.dismiss();
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateSpinnerSelection(String selectedOption) {
        selectedOptionsList.clear();
        selectedOptionsList.add(selectedOption);
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) binding.mySpinner.getAdapter();
        reportType = binding.mySpinner.getSelectedItem().toString();
        adapter.notifyDataSetChanged();
    }

    private void showConfirmationDialog() {
        if (selectedOptionIndex == -1) {
            Toast.makeText(getContext(), "Please select an option", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage("Are you sure you want to proceed \n with Shift End?");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                duplicateShiftSummaryApi();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Toast.makeText(getContext(), "Action Cancelled", Toast.LENGTH_SHORT).show();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}