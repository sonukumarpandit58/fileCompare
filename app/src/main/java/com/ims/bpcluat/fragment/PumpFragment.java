package com.ims.bpcluat.fragment;

import static android.app.Activity.RESULT_OK;
import static androidx.core.content.ContextCompat.checkSelfPermission;
import static com.ims.bpcluat.Helper.getProductNameById;
import static com.ims.bpcluat.Helper.metaHosPumpUrl;
import static com.ims.bpcluat.Helper.metaHosSecretKey;
import static com.ims.bpcluat.Helper.metaHosTokenUrl;
import static com.ims.bpcluat.Helper.metaHosVendorId;
import static com.ims.bpcluat.Helper.pumpFetch;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.twentySixRequest;
import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.txnArrayList;
import static com.ims.bpcluat.helper.BleDeviceHelper.commandProtocol;
import static com.ims.bpcluat.helper.BleDeviceHelper.retryOneMoreTime;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.ims.bpcluat.conversion.HexToDecimal;
import com.ims.bpcluat.dialog.BluetoothConnectionDialog;
import com.ims.bpcluat.Helper;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.databinding.FragmentPumpBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.BleDeviceHelper;
import com.ims.bpcluat.model.TxnListModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PumpFragment extends Fragment {
    FragmentPumpBinding binding;
    BluetoothAdapter bluetoothAdapter;
    String pumpNo = "";
    String userToken = "";
    private AlertDialog progressDialog;
    private ProgressBar progressBar;
    private TextView progressMessage;
    private TextView progressPercentage;
    ProgressDialog progress;
    private static final int PERMISSION_REQUEST_CODE = 1;
    String pumpTxnFetchRequest = "";
    private static final int REQUEST_ENABLE_BT = 1;
    Context context;
    //    private Handler handler = new Handler();  // Earlier use comment on 24-12-2024
    private final Handler handler = new Handler(Looper.getMainLooper());
    Date date = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
    String todayDate = formatter.format(date);
    private boolean isDialogVisible = false;
    private boolean responseReceived = false;
    int position = 0;
    String netAmountCheck;

    private BleDeviceHelper bleDeviceHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPumpBinding.inflate(inflater, container, false);
        context = getActivity();

        Log.d("skpPump", Helper.serialNumber);
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog

        fileWrite(context, todayDate + ".txt", "Landing Page : ", "Pump Frag");

        bleDeviceHelper = bleDeviceHelper.getInstance(getActivity());
        bleDeviceHelper.setBleCallback(new BleDeviceHelper.BleCallback() {
            @Override
            public void onBleConnected() {
                try {
                    Log.d("PumpFragment", "BLE Connected, ready to send command");
                    String commandHex = twentySixRequest; // Or any dynamic value
                    Log.d("twentySixRequest", twentySixRequest);
                    commandProtocol = "26h";
                    byte[] command = BleDeviceHelper.hexStringToByteArray(commandHex);
                    bleDeviceHelper.writeCharacteristic.setValue(command);
                    bleDeviceHelper.bluetoothGatt.writeCharacteristic(bleDeviceHelper.writeCharacteristic);
                } catch (SecurityException e) {
                    Log.d("PumpFragmentSecurity", e.toString());
                }
            }

            @Override
            public void onBleResponseReceived(String response) {
                Log.d("PumpFragment", "Received BLE Response: " + response);
                //onDataReceived(response);
                new Handler(Looper.getMainLooper()).post(() -> {
                    handler.removeCallbacksAndMessages(null); // Removes any pending tasks
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    if(response.equals("NoTxnFound")){
                        Toast.makeText(context, "No Txn Found", Toast.LENGTH_SHORT).show();
                        bleDeviceHelper.disconnect();
                        retryOneMoreTime = false;
                    }else{
                        setLatestTransaction();
                    }
                });
            }

            @Override
            public void onBleConnectionFailed() {
                new Handler(Looper.getMainLooper()).post(() -> {
                    handler.removeCallbacksAndMessages(null); // Removes any pending tasks
                    Log.d("PumpFragment", "BLE connection failed or no matching device found");
                    if (progress != null && progress.isShowing()) {
                        progress.dismiss();
                        bleDeviceHelper.disconnect();
                        retryOneMoreTime = false;
                        BluetoothConnectionDialog.showDialog(getActivity());
                    }
                });
            }

            @Override
            public void onBluetoothTurnedOff() {
                Log.d("PumpFragment", "Bluetooth is manually off by user.");
            }
        });

        Bundle bundle = getArguments();
        if (bundle != null) {
            String receivedString = bundle.getString("key");
            Log.d("receivedString", receivedString);
            if (receivedString.equals("fetchPump")) {
                initProgressDialog();
                new MetaHosTokenApiTask().execute();
            }
        } else {
            if (pumpFetch.isEmpty()) {
                if (Helper.pumpArray.size() == 0) {
                    initProgressDialog();
                    new MetaHosTokenApiTask().execute();
                } else {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, Helper.pumpArray);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    binding.duSpinner.setAdapter(adapter);
                }
            } else if (pumpFetch.equals("yes")) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, Helper.pumpArray);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                binding.duSpinner.setAdapter(adapter);
            } else {
                binding.manuallyPumpNo.setVisibility(View.VISIBLE);
                binding.duSpinner.setVisibility(View.GONE);
            }
        }

        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileWrite(context, todayDate + ".txt", "Click Event : ", "Pump Fragment = Submit Button");
                isDialogVisible = false;
                if (binding.duSpinner.getVisibility() == View.VISIBLE) {
                    int selectedItemPosition = binding.duSpinner.getSelectedItemPosition();
                    if (selectedItemPosition == AdapterView.INVALID_POSITION) {
                        // No item is selected
                        Log.d("SpinnerCheck", "No item selected");
                        Helper helper = new Helper();
                        helper.showToastMessage(getActivity(), "Please fetch pump first");
                    } else {
                        Log.d("SpinnerCheck", "Item selected: " + binding.duSpinner.getSelectedItem().toString());
                        String selectedValue = binding.duSpinner.getSelectedItem().toString();
                        int index = selectedValue.indexOf('-');
                        pumpNo = selectedValue.substring(index + 1);
                        if (pumpNo.length() == 1) {
                            pumpNo = "0" + pumpNo;
                        }
                        // pumpNo = "08";
                        Helper.txnReleatedPumpNo = pumpNo;
                        twentySixRequest = BleDeviceHelper.createRequestForFetchTxn(pumpNo);
                        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        if (bluetoothAdapter == null) {
                            MessagesDialog.showDialog(requireContext(), "Device does not support Bluetooth.", 0, null, null);

                            //Toast.makeText(getActivity(), "Device does not support Bluetooth.", Toast.LENGTH_SHORT).show();
                        } else {
                            if (bluetoothAdapter.isEnabled()) {
                                // Bluetooth is enabled
                                checkBluetoothPermission();
                            } else {
                                // Bluetooth is disabled
                                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                            }
                        }
                    }
                }

                if (binding.manuallyPumpNo.getVisibility() == View.VISIBLE) {
                    String pumpNo = binding.manuallyPumpNo.getText().toString().trim();
                    if (pumpNo.isEmpty()) {
                        binding.manuallyPumpNo.setError("please enter pump no");
                        binding.manuallyPumpNo.requestFocus();
                    } else {
                        Helper.closeKeyboard(getActivity());
                        if (pumpNo.length() == 1) {
                            pumpNo = "0" + pumpNo;
                        }
                        Helper.txnReleatedPumpNo = pumpNo;
                        twentySixRequest = BleDeviceHelper.createRequestForFetchTxn(pumpNo);
                        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        if (bluetoothAdapter == null) {
                            MessagesDialog.showDialog(requireContext(), "Device does not support Bluetooth.", 0, null, null);

                            //Toast.makeText(getActivity(), "Device does not support Bluetooth.", Toast.LENGTH_SHORT).show();
                        } else {
                            if (bluetoothAdapter.isEnabled()) {
                                // Bluetooth is enabled
                                checkBluetoothPermission();
                            } else {
                                // Bluetooth is disabled
                                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                            }
                        }
                    }
                }
            }
        });
        return binding.getRoot();
    }

    private void checkBluetoothPermission() {
        if (checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, proceed with the scan
            connectBluetooth();
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // If request is cancelled, the result arrays are empty
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with the scan
                connectBluetooth();
            } else {
                // Permission denied, show a message to the user
                MessagesDialog.showDialog(requireContext(), "Permission required to perform Bluetooth scan", 0, null, null);

                // Toast.makeText(getActivity(), "Permission required to perform Bluetooth scan", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initProgressDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.custom_progress_dialog, null);

        progressBar = dialogView.findViewById(R.id.progress_bar);
        progressMessage = dialogView.findViewById(R.id.progress_message);
        progressPercentage = dialogView.findViewById(R.id.progress_percentage);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogView);
        builder.setCancelable(false); // Make it non-cancelable if needed

        progressDialog = builder.create();
    }


    private class MetaHosTokenApiTask extends AsyncTask<Void, Integer, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressMessage.setText("Token API Call");
            progressDialog.show(); // Show the progress dialog before starting the background task
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                OkHttpClient client = new OkHttpClient();
                MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("vendor_id", metaHosVendorId);
                jsonBody.put("secret_key", metaHosSecretKey);
                Log.d("metaHosFirstRequestCred", String.valueOf(jsonBody));
                RequestBody requestBody = RequestBody.create(String.valueOf(jsonBody), JSON);

                // Create the request with header and body
                Request request = new Request.Builder()
                        .url(metaHosTokenUrl)
                        .post(requestBody)
                        .header("Application", "ANALYTICS")
                        .build();

                Log.d("metaHosFirstRequest", String.valueOf(request));
                fileWrite(context, todayDate + ".txt", "metaHosTokenFetchRequest : ", String.valueOf(request));

                try {
                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        String res = response.body().string();
                        Log.d("metaHosFirstResponse", res);
                        fileWrite(context, todayDate + ".txt", "metaHosTokenFetchResponse :", res);
                        JSONObject jsonObjectResponse = new JSONObject(res);
                        String token = jsonObjectResponse.getString("token");
                        if (!token.isEmpty()) {
                            userToken = jsonObjectResponse.getString("token");
                            publishProgress(50); // Update progress to 50%
                            return true;
                        }
                    } else {
                        Log.d("FailedTokenFetch", "Res: " + response);
                        fileWrite(context, todayDate + ".txt", "metaHosTokenFetchResponse :", String.valueOf(response));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    progressDialog.dismiss();
                    pumpFetch = "no";
                    Log.d("apiCall", e.toString());
                }
            } catch (Exception e) {
                progressDialog.dismiss();
                pumpFetch = "no";
                Log.d("exception", e.toString());
            }
            return false;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            int progress = values[0];
            progressBar.setProgress(progress); // Update the progress bar
            //progressMessage.setText("Loading... " + progress + "%"); // Update the message with the current progress
            progressMessage.setText("Token API Data Received"); // Update the message with the current progress
            progressPercentage.setText(progress + "%"); // Update the percentage text
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (success) {
                // Start the second background task if the first API call was successful
                new MetaHosFetchPumpApiTask().execute();
            } else {
                progressDialog.dismiss(); // Dismiss the progress dialog if the task failed
                if (userToken.isEmpty()) {
                    pumpFetch = "no";
                    MessagesDialog.showDialog(requireContext(), "Token Fetch Failed", 0, null, null);

                    // Toast.makeText(getActivity(), "Token Fetch Failed", Toast.LENGTH_SHORT).show();
                    binding.duSpinner.setVisibility(View.GONE);
                    binding.manuallyPumpNo.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            pumpFetch = "no";
            progressDialog.dismiss(); // Dismiss the progress dialog if the task is cancelled
        }
    }

    private class MetaHosFetchPumpApiTask extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressMessage.setText("Pump API Call");
            // No need to show the dialog again as it is already shown
        }

        @Override
        protected Boolean doInBackground(String... urls) {
            String API_URL = metaHosPumpUrl;
            Log.d("userToken", userToken);
            String AUTH_TOKEN = userToken;
            OkHttpClient client = new OkHttpClient();
            // Create a request with Authorization header
            Request request = new Request.Builder()
                    .url(API_URL + "/ros/" + sapCode + "/bays")
                    .header("Application", "ANALYTICS")
                    .addHeader("Authorization", "Bearer " + AUTH_TOKEN)
                    .build();

            Log.d("metaHosSecondRequest = ", String.valueOf(request));
            fileWrite(context, todayDate + ".txt", "metaHosPumpFetchRequest : ", String.valueOf(request));
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String res = response.body().string();
                    Log.d("metaHosSecondResponse = ", res);
                    fileWrite(context, todayDate + ".txt", "metaHosPumpFetchResponse :", res);
                    Helper.metaHosResponse = res;
                    JSONArray jsonArray = new JSONArray(res);
                    ArrayList<Integer> numberList = new ArrayList<>();
                    Helper.pumpArray.clear();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        JSONArray nozzlesArray = jsonObject.getJSONArray("nozzles");
                        String localBayID = jsonObject.getString("localBayID");
                        numberList.add(Integer.valueOf(localBayID));
//                        for (int j = 0; j < nozzlesArray.length(); j++) {
//                            JSONObject nozzleObject = nozzlesArray.getJSONObject(j);
//                            String globalNozzleID = nozzleObject.getString("globalNozzleID");
//                            numberList.add(Integer.valueOf(globalNozzleID));
//                            //Helper.pumpArray.add("Pump No -" + globalNozzleID);
//                        }
                        new Handler(Looper.getMainLooper()).post(() -> publishProgress(100));
                    }

                    Collections.sort(numberList);

                    for (int i = 0; i < numberList.size(); i++) {
                        Helper.pumpArray.add("Pump No -" + numberList.get(i));
                        //Log.d("MainActivity", "Element at index " + i + ": " + numberList.get(i));
                    }

//                    for (int i = 0; i < jsonArray.length(); i++) {
//                        JSONObject jsonObject = jsonArray.getJSONObject(i);
//                        String localBayID = jsonObject.getString("localBayID");
//                        Helper.pumpArray.add("Pump No -" + localBayID);
//                        new Handler(Looper.getMainLooper()).post(() -> publishProgress(100));
//                    }
                } else {
                    Log.d("FailedPumpFetch", "Res: " + response);
                    fileWrite(context, todayDate + ".txt", "metaHosPumpFetchResponse :", String.valueOf(response));
                    new Handler(Looper.getMainLooper()).post(() -> publishProgress(100));
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("apiCall", e.toString());
                pumpFetch = "no";
                progressDialog.dismiss();
            } catch (JSONException e) {
                pumpFetch = "no";
                progressDialog.dismiss();
                //Log.d("pumpFetchException",e.toString());
            }
            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            int progress = values[0];
            progressBar.setProgress(progress); // Update the progress bar
            //progressMessage.setText("Loading... " + progress + "%"); // Update the message with the current progress
            progressMessage.setText("Pump Api Data Received");
            progressPercentage.setText(progress + "%"); // Update the percentage text

            if (Helper.pumpArray.size() > 0) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, Helper.pumpArray);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                binding.duSpinner.setAdapter(adapter);
                pumpFetch = "yes";
                //  checkBluetoothPermission();
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            progressDialog.dismiss(); // Dismiss the progress dialog when the task is complete
            if (Helper.pumpArray.size() == 0) {
                pumpFetch = "no";
                MessagesDialog.showDialog(requireContext(), "Pump Fetch Failed", 0, null, null);

                //Toast.makeText(getActivity(), "Pump Fetch Failed", Toast.LENGTH_SHORT).show();
                binding.duSpinner.setVisibility(View.GONE);
                binding.manuallyPumpNo.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            pumpFetch = "no";
            progressDialog.dismiss(); // Dismiss the progress dialog if the task is cancelled
        }
    }

    public void connectBluetooth() {
        try {
            Log.d("PumpFragment", "connectBluetooth method Called");
            progress.show();
            handler.removeCallbacksAndMessages(null); // Removes any pending tasks
            handler.postDelayed(() -> {
                if (progress.isShowing()) {
                    progress.dismiss();
                    bleDeviceHelper.disconnect();
                    retryOneMoreTime = false;
                    BluetoothConnectionDialog.showDialog(getActivity());
                }
            }, 20000); // 5 seconds timeout
            bleDeviceHelper.initiateBleScan();
        } catch (Exception e) {
            fileWrite(context, todayDate + ".txt", "PumpFragment connectBluetooth Exception", e.toString());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Log.d("skpResult", "Bluetooth on ho gya hai");
                checkBluetoothPermission();
                // Bluetooth was enabled
            } else {
                // Bluetooth was not enabled
                Log.d("skpResult", "Bluetooth off ho gya hai");
                MessagesDialog.showDialog(requireContext(), "Bluetooth is off", 0, null, null);
                // Toast.makeText(getActivity(), "Bluetooth is off", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            // Cancel the handler to avoid executing after leaving the fragment
            //handler.removeCallbacksAndMessages(null);
        } catch (Exception e) {
            // Log or handle the exception to avoid crashing the app
            Log.e("PumpFragment", "Error removing handler callbacks in onPause: " + e.getMessage());
            fileWrite(context, todayDate + ".txt", "PumpFragment onPause Exception", e.toString());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d("PumpFragment", "onDestroyView method called");
        fileWrite(context, todayDate + ".txt", "PumpFragment:method Called = ", "onDestroyView");
        try {
            handler.removeCallbacksAndMessages(null);
        } catch (Exception e) {
            Log.d("BluetoothDisconnect", "PumpFragment 608 Lines");
            fileWrite(context, todayDate + ".txt", "PumpFragment onDestroyView Exception", e.toString());
        }
    }

    @Override
    public void onDestroy() {
        try {
            super.onDestroy();
            fileWrite(context, todayDate + ".txt", "PumpFrag:method Called = ", "onDestroy");
            Log.d("PumpFragment", "onDestroy method called");
        } catch (Exception e) {
            fileWrite(context, todayDate + ".txt", "PumpFragment onDestroy Exception", e.toString());
        }
    }

    public void setLatestTransaction(){
        if (txnArrayList.size() > 0) {
            // Temporary list with datetime and object mapping
            List<Pair<String, Object>> sortedList = new ArrayList<>();
            for (int i = 0; i < txnArrayList.size(); i++) {
                Object myobj = txnArrayList.get(i);
                Gson gson = new Gson();
                String json = gson.toJson(myobj);

                try {
                    JSONObject jsonObject = new JSONObject(json);
                    String hour = HexToDecimal.convert(jsonObject.getString("Hour"));
                    String minute = HexToDecimal.convert(jsonObject.getString("Minute"));
                    String second = HexToDecimal.convert(jsonObject.getString("Second"));

                    if (hour.length() == 1) hour = "0" + hour;
                    if (minute.length() == 1) minute = "0" + minute;
                    if (second.length() == 1) second = "0" + second;

                    String timeString = hour + ":" + minute + ":" + second;

                    // Add to temporary list
                    sortedList.add(new Pair<>(timeString, myobj));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            // Sort list in descending order based on time
            Collections.sort(sortedList, (p1, p2) -> p2.first.compareTo(p1.first));

            // Clear original list and re-add sorted items
            txnArrayList.clear();
            for (Pair<String, Object> pair : sortedList) {
                txnArrayList.add(pair.second);
            }
        }

        fileWrite(context, todayDate + ".txt", "Redirect to : ", "Single Transaction Screen");
        ((SideBarActivity) requireActivity()).loadFragement(new OnlineSingleTransactionFragment());
    }
}