package com.ims.bpcluat.fragment;

import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.todayDate;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.UpdateOperatorDetailsActivity;
import com.ims.bpcluat.adapter.InActiveOperatorListAdapter;
import com.ims.bpcluat.databinding.FragmentInActiveOperatorBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.interfaces.ViewDetailRecyclerViewInterface;
import com.ims.bpcluat.model.OperatorListModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class InActiveOperatorFragment extends Fragment implements ApiHelper.NetworkingApiCallBack, ViewDetailRecyclerViewInterface {
    FragmentInActiveOperatorBinding binding;
    ApiHelper api;
    ArrayList<OperatorListModel> inActiveoperatorModelArrayList = new ArrayList<>();
    OperatorListModel operatorlistmodel = new OperatorListModel();
    ProgressDialog progress;

    public InActiveOperatorFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentInActiveOperatorBinding.inflate(inflater, container, false);
       // Toast.makeText(requireContext(), "Inactive", Toast.LENGTH_SHORT).show();
        api = new ApiHelper();
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
            api.setApiCallBack((ApiHelper.NetworkingApiCallBack) this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
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
                            if (outputObj.getString("status").equals("Deactivate")) {
                                firstName = outputObj.getString("firstName");
                                lastName =  outputObj.getString("lastName");
                                mobileNumber = outputObj.getString("mobileNumber");
                                if(outputObj.has(emailId)){
                                    emailId = outputObj.getString("emailId");
                                }
                                inActiveoperatorModelArrayList.add(new OperatorListModel(firstName, lastName,mobileNumber, emailId));
                            }
                        }

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                binding.inactiveoperatorRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                                InActiveOperatorListAdapter adapter = new InActiveOperatorListAdapter((Context) getActivity(), inActiveoperatorModelArrayList, InActiveOperatorFragment.this);
                                binding.inactiveoperatorRecyclerView.setAdapter(adapter);
                                progress.dismiss();
                            }
                        });

                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                MessagesDialog.showDialog(requireContext(), respCode, 0,null, null);
                                //Toast.makeText(getActivity(), respDesc+" - "+respCode, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "getOperatorListResponse", e.toString());
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

    @Override
    public void onViewDetailClick(int position) {
        Helper.txnListPostionSelected = position;
        Intent intent = new Intent(getActivity(), UpdateOperatorDetailsActivity.class);
        intent.putExtra("inactive", "inactive");
        intent.putExtra("firstName", inActiveoperatorModelArrayList.get(position).getFirstName());
        intent.putExtra("lastName", inActiveoperatorModelArrayList.get(position).getLastName());
        intent.putExtra("mobileNumber", inActiveoperatorModelArrayList.get(position).getMobileNumber());
        intent.putExtra("emailId", inActiveoperatorModelArrayList.get(position).getEmailId());
        startActivity(intent);
    }

    @Override
    public void onclick(int position) {

    }
}