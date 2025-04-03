package com.ims.bpcluat.fragment;

import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.Helper.username;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.UpdateOperatorDetailsActivity;
import com.ims.bpcluat.adapter.ActiveOperatorListAdapter;
import com.ims.bpcluat.databinding.FragmentActiveOperatorBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.interfaces.ViewDetailRecyclerViewInterface;
import com.ims.bpcluat.model.OperatorListModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ActiveOperatorFragment extends Fragment implements ApiHelper.NetworkingApiCallBack, ViewDetailRecyclerViewInterface {

    FragmentActiveOperatorBinding binding;
    ApiHelper api;
    ArrayList<OperatorListModel> activeoperatorModelArrayList = new ArrayList<>();
    OperatorListModel operatorlistmodel = new OperatorListModel();
    ProgressDialog progress;
    String mob;


    public ActiveOperatorFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentActiveOperatorBinding.inflate(inflater, container, false);
        api = new ApiHelper();
        // Toast.makeText(requireContext(), "active", Toast.LENGTH_SHORT).show();
        getOperatorListApi();

        binding.ivAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BottomSheetDialogFragment bottomSheetDialogFragment = new BottomSheetDialogFragment();
                bottomSheetDialogFragment.show(getChildFragmentManager(), "addOperatorBottomSheet");
            }
        });

        return binding.getRoot();
    }

    private void getOperatorListApi() {
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();
        String url = "getOperatorList";
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("adminId", Helper.mid);
            jsonObject.put("source", "Mobile");
            jsonObject.put("channel", channelName);
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            Log.d("operatorListRequest=", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void resetTpin() {
        progress = new ProgressDialog(requireContext());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();
        String url = "resetOprPin";
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("userName", mob);
            jsonObject.put("source", "Mobile");
            jsonObject.put("channel", channelName);
            jsonObject.put("resetFlag", "1");
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);

            Log.d("resetTpinRequest=", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void showAlertDialog(String mob) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Reset TPIN Warning")
                .setMessage("Please click on Reset Button to reset Your TPIN for username " + mob + " in oder to login")
                .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        resetTpin();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showAlertDialoghome() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Reset TPIN Successfully")
                .setMessage("Your TPIN is Reset Successfully Use Your New TPIN for " + mob + " in order to login")
                .setPositiveButton("Home", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onViewDetailClick(int position) {
        Helper.txnListPostionSelected = position;
        Intent intent = new Intent(getActivity(), UpdateOperatorDetailsActivity.class);
        intent.putExtra("firstName", activeoperatorModelArrayList.get(position).getFirstName());
        intent.putExtra("lastName", activeoperatorModelArrayList.get(position).getLastName());
        intent.putExtra("mobileNumber", activeoperatorModelArrayList.get(position).getMobileNumber());
        intent.putExtra("emailId", activeoperatorModelArrayList.get(position).getEmailId());
        startActivity(intent);
    }

    @Override
    public void onclick(int position) {
        Helper.txnListPostionSelected = position;
        mob = activeoperatorModelArrayList.get(position).getMobileNumber();
        Log.d("mob", mob);
        showAlertDialog(mob);
    }

    @Override
    public void apiResult(String res, String apiName) {
        if (apiName.equals("getOperatorList")) {
            try {
                if (res.equals("Server Time Out")) {
                    progress.dismiss();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                        }
                    });
                } else {
                    Log.d("getOperatorListResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respCode = payLoad.getString("respCode");
                    if (respCode.equals("200")) {
                        progress.dismiss();
                        JSONArray outputArray = payLoad.getJSONArray("operatorDetail");
                        String firstName = "";
                        String lastName = "";
                        String mobileNumber = "";
                        String emailId = "";

                        Log.d("outputArray", String.valueOf(outputArray));
                        for (int i = 0; i < outputArray.length(); i++) {
                            JSONObject outputObj = outputArray.getJSONObject(i);
                            if (outputObj.getString("status").equals("Activate")) {
                                firstName = outputObj.getString("firstName");
                                lastName =  outputObj.getString("lastName");
                                mobileNumber = outputObj.getString("mobileNumber");
                                if(outputObj.has(emailId)){
                                    emailId = outputObj.getString("emailId");
                                }
                                activeoperatorModelArrayList.add(new OperatorListModel(firstName, lastName,mobileNumber, emailId));
                            }
                        }

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                binding.activeoperatorRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                                ActiveOperatorListAdapter adapter = new ActiveOperatorListAdapter(getActivity(), activeoperatorModelArrayList, ActiveOperatorFragment.this);
                                binding.activeoperatorRecyclerView.setAdapter(adapter);
                                progress.dismiss();
                            }
                        });

                    } else {
                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            MessagesDialog.showDialog(requireContext(), respCode, 0,null, null);
                        });
                      /*  getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                //Toast.makeText(getActivity(), respDesc+" - "+respCode, Toast.LENGTH_SHORT).show();
                            }
                        });*/
                    }

                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "activeOperatorResponse", e.toString());
                getActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_SHORT).show();
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    MessagesDialog.showDialog(requireContext(), e.toString(), 0,null, null);

                });
               // throw new RuntimeException(e);
            }
        }
        else if (apiName.equals("resetOprPin")) {
            try {
                if (res.equals("Server Time Out")) {
                    progress.dismiss();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                        }
                    });
                } else {
                    Log.d("resetOprPinResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                    String respCode = payLoad.getString("respCode");
                    if (respCode.equals("200")) {
                        progress.dismiss();
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showAlertDialoghome();
                            }
                        });
                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            String errorMsg = payLoad.getString("respCode");
                            String respDesc = payLoad.getString("respDesc");
                            @Override
                            public void run() {
                                progress.dismiss();
                                MessagesDialog.showDialog(requireContext(), errorMsg + " - " +respDesc, 0,null, null);
                                // Toast.makeText(requireContext(), errorMsg + " - " +respDesc, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "activeOperatorResponse", e.toString());
                getActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_SHORT).show();
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    MessagesDialog.showDialog(requireContext(), e.toString(), 0,null, null);
                });
                //throw new RuntimeException(e);
            }
        }
    }

}