package com.ims.bpcluat.fragment;

import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.isValidEmail;
import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.Helper.username;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.ims.bpcluat.AdminLoginActivity;
import com.ims.bpcluat.AdminSideBarActivity;
import com.ims.bpcluat.MainActivity;
import com.ims.bpcluat.R;
import com.ims.bpcluat.adapter.ActiveOperatorListAdapter;
import com.ims.bpcluat.databinding.FragmentActiveOperatorBinding;
import com.ims.bpcluat.databinding.FragmentBottomSheetDialogBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.model.OperatorListModel;
import com.ims.bpcluat.validation.MobileNoValidation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class BottomSheetDialogFragment extends com.google.android.material.bottomsheet.BottomSheetDialogFragment implements ApiHelper.NetworkingApiCallBack {

    FragmentBottomSheetDialogBinding binding;
    ProgressDialog progress;
    int[] blankArray;
    ApiHelper api;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBottomSheetDialogBinding.inflate(inflater, container, false);

        //View view = inflater.inflate(R.layout.fragment_bottom_sheet_dialog, container, false);
        api = new ApiHelper();

        binding.btClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        binding.btSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateFields();
            }
        });
        return binding.getRoot();
        // return view;
    }

    private void validateFields() {
        String firstname = binding.etFirstname.getText().toString().trim();
        String lastName = binding.etLastname.getText().toString().trim();
        String emailid = binding.etEmailid.getText().toString().trim();
        String mobilenum = binding.etMobilenum.getText().toString().trim();
        if (TextUtils.isEmpty(firstname) && TextUtils.isEmpty(lastName) && TextUtils.isEmpty(mobilenum)) {
            binding.etFirstname.setError("Please enter first name");
            binding.etMobilenum.setError("Please enter mobile no");
            binding.etFirstname.requestFocus();
            binding.etMobilenum.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(mobilenum)) {
            binding.etMobilenum.setError("Please enter mobile number");
            binding.etMobilenum.requestFocus();
            return;
        }
        if (mobilenum.length() != 10) {
            binding.etMobilenum.setError("Mobile Number must be 10 digits");
            binding.etMobilenum.requestFocus();
            return;
        }
        if (mobilenum.startsWith("0")) {
            binding.etMobilenum.setError("Mobile Number cannot start with zero");
            binding.etMobilenum.requestFocus();
            return;
        }
        if (mobilenum.equals("1234567890")) {
            binding.etMobilenum.setError("Please enter valid mobile number");
            binding.etMobilenum.requestFocus();
            return;
        }
        if (MobileNoValidation.hasSameNumber(mobilenum)) {
            binding.etMobilenum.setError("All digits of mobile number cannot be same.");
            binding.etMobilenum.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(firstname)) {
            binding.etFirstname.setError("Please enter first name");
            binding.etFirstname.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(lastName)) {
            binding.etLastname.setError("Please enter last name");
            binding.etLastname.requestFocus();
            return;
        }
        if (!TextUtils.isEmpty(emailid)) {
            if (!isValidEmail(emailid)) {
                binding.etEmailid.setError("Please enter valid email-Id");
                binding.etEmailid.requestFocus();
                return;
            }
        }
        addoperator(firstname, lastName, mobilenum, emailid);
    }

    private void addoperator(String firstName, String lastName, String mobileNumber, String emailid) {
        progress = new ProgressDialog(requireContext());
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();
        String url = "userCreation";
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("adminId", mid);
            jsonObject.put("source", "Mobile");
            jsonObject.put("channel", channelName);
            jsonObject.put("reqDate", requestDate());
            jsonObject.put("reqTime", requestTime());
            jsonObject.put("userType", "Operator");
            jsonObject.put("firstName", firstName);
            jsonObject.put("lastName", lastName);
            jsonObject.put("mobileNumber", mobileNumber);
            jsonObject.put("emailId", emailid);
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("operatorDetail", blankArray);
            jsonObject.put("result", blankArray);
            jsonObject.put("billerTranList", blankArray);

            Log.d("operatorCreateRequest = ", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void apiResult(String res, String apiName) {
        try {
            if (res.equals("Server Time Out")) {
                progress.dismiss();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);

                      //  Toast.makeText(getActivity(), "Server Time Out", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.d("operatorCreateResponse = ", res);
                JSONObject jsonObject = new JSONObject(res);
                JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                String respDesc = payLoad.getString("respDesc");
                String respCode = payLoad.getString("respCode");
                if (respCode.equals("200")) {
                    progress.dismiss();
                    Intent intent = new Intent(getActivity(), AdminSideBarActivity.class);
                    startActivity(intent);
                } else {
                    progress.dismiss();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDialog.showDialog(requireContext(), respDesc+" - "+respCode,0, null, null);
                            //Toast.makeText(getActivity(), respDesc+" - "+respCode, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        } catch (JSONException e) {
            fileWrite(getContext(), todayDate + ".txt", "operatorCreateResponse", e.toString());
            getActivity().runOnUiThread(() -> {
                progress.dismiss();
                Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_SHORT).show();
                if (progress.isShowing()) {
                    progress.dismiss();
                }
                MessagesDialog.showDialog(requireContext(), e.toString(),0, null, null);

            });
            //throw new RuntimeException(e);
        }
    }
}