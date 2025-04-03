package com.ims.bpcluat;

import static com.ims.bpcluat.Helper.metaHosPumpUrl;
import static com.ims.bpcluat.Helper.metaHosSecretKey;
import static com.ims.bpcluat.Helper.metaHosTokenUrl;
import static com.ims.bpcluat.Helper.metaHosVendorId;
import static com.ims.bpcluat.Helper.pumpFetch;
import static com.ims.bpcluat.Helper.roOnlineStatus;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.serialNumber;


import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.firstdata.merchantservicessdk.MSApi;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.ims.bpcluat.adapter.CustomExpandableListAdapter;
import com.ims.bpcluat.alp.AlpFragment;
import com.ims.bpcluat.alp.alpOperations.cardManagement.CardManagementFragment;
import com.ims.bpcluat.alp.alpOperations.sale.SaleFragment;
import com.ims.bpcluat.alp.alpOperations.sale.TxnFailFragment;
import com.ims.bpcluat.alp.alpOperations.utility.UtilityFragment;
import com.ims.bpcluat.cng.CngFragment;
import com.ims.bpcluat.databinding.ActivitySideBarBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.fragment.AppInfoFragment;
import com.ims.bpcluat.fragment.OfflineFragment;
import com.ims.bpcluat.fragment.OnlineSingleTransactionFragment;
import com.ims.bpcluat.fragment.PumpFragment;
import com.ims.bpcluat.fragment.ReprintFragment;
import com.ims.bpcluat.fragment.TxnHistorySummaryFragment;
import com.ims.bpcluat.model.CngModel;
import com.ims.bpcluat.model.NfrModel;
import com.ims.bpcluat.model.OnlineTxnModel;
import com.ims.bpcluat.nfr.NfrFragment;
import com.ims.bpcluat.receiver.ConnectivityReceiver;
import com.ims.bpcluat.ufill.UfillRegistrationFragment;
import com.ims.bpcluat.ufill.ufil1.UfillOneFragment;
import com.ims.bpcluat.ufill.void_transaction.VoidFragment;
import com.ims.bpcluat.ufill.ufil2.UfillTwoFragment;
import com.pax.fdms.opensdk.base24.Base24Constant;
import com.pax.fdms.opensdk.base24.Base24Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SideBarActivity extends AppCompatActivity {

    ActivitySideBarBinding xml;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    ExpandableListView expandableListView;
    CustomExpandableListAdapter expandableListAdapter;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;
    String myDeviceInfo, userToken;
    private ConnectivityReceiver connectivityReceiver = new ConnectivityReceiver();
    ActionBarDrawerToggle toggle;
    private CngModel cngModel;
    private NfrModel nfrModel;
    private OnlineTxnModel onlineTxnModel;
    ProgressDialog progress;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        xml = ActivitySideBarBinding.inflate(getLayoutInflater());
        setContentView(xml.getRoot());

        context = this;

        progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);
        expandableListView = findViewById(R.id.expandableListView);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.OpenDrawer, R.string.CloseDrawer);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        myDeviceInfo = Build.MODEL;
        prepareListData();
        expandableListAdapter = new CustomExpandableListAdapter(this, listDataHeader, listDataChild);
        expandableListView.setAdapter(expandableListAdapter);

        if (ContextCompat.checkSelfPermission(SideBarActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(SideBarActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted, proceed with file writing
            ReadWriteHelper.createRequestFile(SideBarActivity.this,"");
            File bpclFolder = new File(Environment.getExternalStorageDirectory(), "BPCL Log Data");
            if (!bpclFolder.exists()) {
                bpclFolder.mkdirs();
            }
        } else {
            // Request necessary permissions
            askPermissionForStorage();
        }

        expandableListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                String groupName = listDataHeader.get(groupPosition);
                if (groupName.equals("Home")) {
                    loadFragement(new PumpFragment());
                } else if (groupName.equals("Fetch Pump")) {
                    Bundle bundle = new Bundle();
                    bundle.putString("key", "fetchPump");
                    PumpFragment fragment = new PumpFragment();
                    fragment.setArguments(bundle);
                    loadFragement(fragment);
                } else if (groupName.equals("Sale")) {
                    if (Helper.metaHosProduct.isEmpty()) {
                        progress.show();
                        tokenFetch();
                    } else {
                        loadFragementWithDifferentToolbar(new OfflineFragment());
                    }
                } else if (groupName.equals("OTHERS")) {
                    loadFragementWithDifferentToolbar(new NfrFragment());
                } else if (groupName.equals("CNG")) {
                    loadFragement(new CngFragment());
                } else if (groupName.equals("Reprint")) {
                    loadFragement(new ReprintFragment());
                } else if (groupName.equals("Upload Fuel Log")) {
                    ReadWriteHelper.logUploadApi(context);
                } else if (groupName.equals("Settlement")) {
                    settlement();
                } else if (groupName.equals("ALP")) {
                    loadFragementWithDifferentToolbar(new AlpFragment());
                } else if (groupName.equals("Transaction History")) {
                    loadFragementWithDifferentToolbar(new TxnHistorySummaryFragment());
                } else if (groupName.equals("App Info")) {
                    loadFragement(new AppInfoFragment());
                } else if (groupName.equals("Signout")) {
                    pumpFetch = "";
                    new Helper().logoutDialog(SideBarActivity.this);
                } else {
                    return false; // Handle expandable groups separately
                }
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                String selectedItem = listDataChild.get(listDataHeader.get(groupPosition)).get(childPosition);
                if (selectedItem.equals("Ufill Registration")) {
                    if (connectivityReceiver.isConnected(getApplicationContext())) {
                        loadFragement(new UfillRegistrationFragment());
                    } else {
                        MessagesDialog.showDialog(SideBarActivity.this, "No internet connection", 0, null, null);

                        // Toast.makeText(SideBarActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
                    }
                } else if (selectedItem.equals("UFill 1.0")) {
                    loadFragement(new UfillOneFragment());
                } else if (selectedItem.equals("UFill 2.0")) {
                    loadFragement(new UfillTwoFragment());
                }
                // else if (selectedItem.equals("Ufill QR")) {
                //     loadFragement(new UfillQrFragment());
                // }
                else if (selectedItem.equals("Void")) {
                    loadFragement(new VoidFragment());
                }
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        cngModel = getIntent().getParcelableExtra("cngModel");
        nfrModel = getIntent().getParcelableExtra("nfrModel");
        onlineTxnModel = getIntent().getParcelableExtra("onlineTxnModel");
        Intent intent = getIntent();
        // Extract the data passed from the first activity
        if (intent != null && intent.hasExtra("redirect")) {
            // Extract the string value passed from the first activity
            String className = intent.getStringExtra("redirect");

            if (className.equals("NfrFragment")) {
                loadFragement(new NfrFragment());
            } else if (className.equals("CngFragment")) {
                loadFragement(new CngFragment());
            } else if (className.equals("OnlineSingleTransactionFragment")) {
                loadFragement(new OnlineSingleTransactionFragment());
            } else if (className.equals("offlinefragment")) {
                loadFragement(new OfflineFragment());
            } else if (className.equals("VoidFragment")) {
                loadFragement(new VoidFragment());
            } else if (className.equals("UtilityFragment")) {
                loadFragement(new UtilityFragment());
            } else if (className.equals("CardManagementFragment")) {
                loadFragement(new CardManagementFragment());
            } else if (className.equals("AlpFragment")) {
                loadFragement(new AlpFragment());
            } else if (className.equals("LoyalityInsertCard")) {
                Bundle bundle = new Bundle();
                bundle.putParcelable("cngModel", cngModel);
                bundle.putParcelable("nfrModel", nfrModel);
                bundle.putParcelable("onlineTxnModel", onlineTxnModel);
                loadFragmentWithData(bundle, new TxnFailFragment());
            } else if (className.equals("SalesFragment")) {
                Bundle bundle = new Bundle();
                bundle.putParcelable("cngModel", cngModel);
                bundle.putParcelable("nfrModel", nfrModel);
                bundle.putParcelable("onlineTxnModel", onlineTxnModel);
                loadFragmentWithData(bundle, new SaleFragment());
            } else {
                PumpFragment fragment = new PumpFragment();
                if (fragment != null) {
                    loadFragement(fragment);
                }
            }
        } else {
            // Handle the case where the intent is null or does not contain the expected extra
            PumpFragment fragment = new PumpFragment();
            if (fragment != null) {
                loadFragement(fragment);
            }
        }

        Log.d("serialNumbdfer", "onCreate: "+serialNumber);
    }

    private void prepareListData() {
        listDataHeader = new ArrayList<>();
        listDataChild = new HashMap<>();

        // Adding group data

        listDataHeader.add("Home");
        listDataHeader.add("Fetch Pump");
        listDataHeader.add("Ufill");
        listDataHeader.add("ALP");
        if (roOnlineStatus.equals("0")) {
            listDataHeader.add("Sale");
        }
        listDataHeader.add("OTHERS");
        listDataHeader.add("CNG");
        if (!Build.MODEL.equals("A50")) {
            listDataHeader.add("Reprint");
        }
//        listDataHeader.add("ALP");
        listDataHeader.add("Settlement");
        listDataHeader.add("Upload Fuel Log");
        listDataHeader.add("Transaction History");
        listDataHeader.add("App Info");
        listDataHeader.add("Signout");

        // Adding child data for Ufill
        List<String> ufill = new ArrayList<>();
        ufill.add("Ufill Registration");
        ufill.add("UFill 1.0");
        ufill.add("UFill 2.0");
        // ufill.add("Ufill QR");
        ufill.add("Void");

        // Adding children to their respective groups
        listDataChild.put(listDataHeader.get(2), ufill); // Header, Child data
        // Make sure each group has a corresponding entry in listDataChild, even if it's an empty list
        for (String header : listDataHeader) {
            if (!listDataChild.containsKey(header)) {
                listDataChild.put(header, new ArrayList<>()); // Empty list if no children
            }
        }
    }

    public void loadFragement(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.container, fragment);
//        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    public void loadFragementWithDifferentToolbar(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.container, fragment);
//        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        invalidateOptionsMenu(); //
    }

    public void loadFragmentWithData(Bundle bundle, Fragment fragment) {
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.container, fragment);
//        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void settlement() {
        Base24Request request = new Base24Request();
        request.setFunctionCode(Base24Constant.TYPE_SETTLEMENT);
        JSONObject requestParams = new JSONObject();
        try {
            requestParams.put("base24Request", new Gson().toJson(request));
            MSApi.getInstance().doPayment(this, 123, requestParams);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d("onActivityResult248", "method called.");
        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == 123) {
//            if (resultCode == RESULT_OK) {
//                String response = data.getStringExtra("response");
//                try {
//                    JSONObject jsonObject = new JSONObject(response);
//                    String message = jsonObject.getJSONObject("base24Response").getString("responseCode");
//                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
//                } catch (JSONException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void tokenFetch() {
        // First API request
        try {
            OkHttpClient client = new OkHttpClient();
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("vendor_id", metaHosVendorId);
            jsonBody.put("secret_key", metaHosSecretKey);
            Log.d("metaHosFirstRequestCred", String.valueOf(jsonBody));
            RequestBody requestBody = RequestBody.create(String.valueOf(jsonBody), JSON);

            Request request = new Request.Builder()
                    .url(metaHosTokenUrl)
                    .post(requestBody)
                    .header("Application", "ANALYTICS")
                    .build();

            // Execute the second request in a background thread
            new Thread(() -> {
                try {
                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        String res = response.body().string();
                        Log.d("metaHosFirstReponse", res);
                        JSONObject jsonObjectResponse = new JSONObject(res);
                        String token = jsonObjectResponse.getString("token");
                        if (!token.isEmpty()) {
                            userToken = jsonObjectResponse.getString("token");
                            getLatestProductPrices();
                        }
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (progress.isShowing()) {
                                    progress.dismiss();
                                    MessagesDialog.showDialog(SideBarActivity.this, "Error in fetching product.", 0, null, null);
                                    //Toast.makeText(SideBarActivity.this, "Error in fetching product.", Toast.LENGTH_SHORT).show();
                                    Log.e("Second API Error", "Request failed with code: " + response.code());
                                }
                            }
                        });
                    }
                } catch (IOException e) {
                    Log.e("Second API Exception", e.getMessage());
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    SharedPreferences shared = getSharedPreferences("sharedPreferMetaHosProduct", MODE_PRIVATE);
                    String localSavedProduct = (shared.getString("metaHosProduct", ""));

                    if (!localSavedProduct.isEmpty()) {
                        Helper.metaHosProduct = localSavedProduct;
                        loadFragementWithDifferentToolbar(new OfflineFragment());
                    } else {
                        runOnUiThread(() ->
                                MessagesDialog.showDialog(SideBarActivity.this, "Error in fetching product", 0, null, null));
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        } catch (Exception e) {
        }
    }

    // Second API request to get latest product prices
    public void getLatestProductPrices() {
        // Create the request
        String AUTH_TOKEN = userToken;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(metaHosPumpUrl + "/ros/" + sapCode + "/products/prices/latest")
                .header("Application", "ANALYTICS")
                .addHeader("Authorization", "Bearer " + AUTH_TOKEN)
                .build();

        Log.d("Request", String.valueOf(request));

        // Execute the request in a background thread
        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("metaHosPriceRes", responseBody);
                    JSONArray jsonArray = new JSONArray(responseBody);
                    if (jsonArray.length() > 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (progress.isShowing()) {
                                    progress.dismiss();
                                }

                                SharedPreferences previousProductDelete = getSharedPreferences("sharedPreferMetaHosProduct", Context.MODE_PRIVATE);
                                previousProductDelete.edit().clear().commit();

                                SharedPreferences sharedPreferences = getSharedPreferences("sharedPreferMetaHosProduct", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("metaHosProduct", responseBody);
                                editor.commit();

                                Helper.metaHosProduct = responseBody;
                                loadFragementWithDifferentToolbar(new OfflineFragment());
                            }
                        });
                    } else {
                        if (progress.isShowing()) {
                            progress.dismiss();
                        }
                        SharedPreferences shared = getSharedPreferences("sharedPreferMetaHosProduct", MODE_PRIVATE);
                        String localSavedProduct = (shared.getString("metaHosProduct", ""));

                        if (!localSavedProduct.isEmpty()) {
                            Helper.metaHosProduct = localSavedProduct;
                            loadFragementWithDifferentToolbar(new OfflineFragment());
                        } else {
                            runOnUiThread(() ->
                                    MessagesDialog.showDialog(SideBarActivity.this, "Error in fetching product.", 0, null, null));
                        }
                        //Toast.makeText(this, "Error in fetching product.", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    SharedPreferences shared = getSharedPreferences("sharedPreferMetaHosProduct", MODE_PRIVATE);
                    String localSavedProduct = (shared.getString("metaHosProduct", ""));

                    if (!localSavedProduct.isEmpty()) {
                        Helper.metaHosProduct = localSavedProduct;
                        loadFragementWithDifferentToolbar(new OfflineFragment());
                    } else {
                        runOnUiThread(() ->
                                MessagesDialog.showDialog(SideBarActivity.this, "Error in fetching product", 0, null, null));
                        Log.e("Error", "Request failed with code: " + response.code());
                    }
                }
            } catch (IOException e) {
                Log.e("Exception", e.getMessage());
                if (progress.isShowing()) {
                    progress.dismiss();
                }
                SharedPreferences shared = getSharedPreferences("sharedPreferMetaHosProduct", MODE_PRIVATE);
                String localSavedProduct = (shared.getString("metaHosProduct", ""));

                if (!localSavedProduct.isEmpty()) {
                    Helper.metaHosProduct = localSavedProduct;
                    loadFragementWithDifferentToolbar(new OfflineFragment());
                } else {
                    runOnUiThread(() ->
                            MessagesDialog.showDialog(SideBarActivity.this, "Error in fetching product", 0, null, null));
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void askPermissionForStorage() {
        ActivityCompat.requestPermissions((Activity) SideBarActivity.this, new String[]{
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }, 100);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with writing to the file
                ReadWriteHelper.createRequestFile(SideBarActivity.this,"");
                File bpclFolder = new File(Environment.getExternalStorageDirectory(), "BPCL Log Data");
                if (!bpclFolder.exists()) {
                    bpclFolder.mkdirs();
                }
            } else {
                // Permission denied
                Toast.makeText(SideBarActivity.this, "Permission denied. Cannot write to file.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
