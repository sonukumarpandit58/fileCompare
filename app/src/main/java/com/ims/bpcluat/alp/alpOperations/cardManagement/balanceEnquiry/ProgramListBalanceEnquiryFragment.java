package com.ims.bpcluat.alp.alpOperations.cardManagement.balanceEnquiry;

import static com.ims.bpcluat.Helper.appVersion;
import static com.ims.bpcluat.Helper.fileWrite;

import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.Helper.username;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.adapter.alp_adapters.BalanceEnquiryProgramAdapter;
import com.ims.bpcluat.alp.alpOperations.AlpOperationsFragment;
import com.ims.bpcluat.databinding.FragmentProgramListBalanceEnquiryBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.interfaces.BalanceEnquiryInterface;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProductModel;
import com.ims.bpcluat.model.AlpModels.SaleModel.Program;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProgramOutput;
import com.ims.bpcluat.model.AlpModels.SaleModel.VirtualCardProgramModel;
import com.ims.bpcluat.model.UfillModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ProgramListBalanceEnquiryFragment extends Fragment implements ApiHelper.NetworkingApiCallBack, BalanceEnquiryInterface {
    FragmentProgramListBalanceEnquiryBinding binding;
    ProgressDialog progress;
    ApiHelper api;
    String txnId = "", dateTime = "";
    Context context;
    BalanceEnquiryProgramAdapter balanceEnquiryprogramAdapter;
    List<VirtualCardProgramModel> virtualCardProgramModelArrayList = new ArrayList<>();
    List<Program> programList = new ArrayList<>();
    String mobilenumber = "";
    String id, mobNo;
    int indexPosition = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProgramListBalanceEnquiryBinding.inflate(inflater, container, false);
        setRetainInstance(true);
        api = new ApiHelper();
        context = getContext();
        hideKeyboard();

        Bundle bundle = getArguments();
        if (bundle != null) {
//            String programsString = bundle.getString("programs");
            programList = (ArrayList<Program>) bundle.getSerializable("programs");
            id = bundle.getString("id");
            mobNo = bundle.getString("mobNo");



            Log.d("TAG", "onCreateViewsdsd: "+programList);
        }


        RecyclerView recyclerView = binding.listviewbalance;
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        balanceEnquiryprogramAdapter = new BalanceEnquiryProgramAdapter(context, programList, (ArrayList<VirtualCardProgramModel>) virtualCardProgramModelArrayList, this, mobilenumber);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(balanceEnquiryprogramAdapter);

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new MobileNumberBalanceFragment());
            }
        });

        return binding.getRoot();
    }

    public void hideKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


    private void virtualCardInitiateOtpApi(int position, List<Program> programList) {
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
            jsonObject.put("reqDate", reqDate);
            jsonObject.put("reqTime", reqTime);
            jsonObject.put("dateTime", dateTime);
            jsonObject.put("txnType", "ACO");
            jsonObject.put("mobNo", mobNo);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("id", id);
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnId", txnId);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);

            JSONArray billerTranList = new JSONArray();
            JSONObject billerTranItem;

            billerTranItem = new JSONObject();
            billerTranItem.put("field13", programList.get(position).getProgramID());
            billerTranItem.put("field14", programList.get(position).getAccountNumber());
            billerTranItem.put("field15", programList.get(position).getCardNumber());

            billerTranList.put(billerTranItem);
            jsonObject.put("billerTranList", billerTranList);

            Log.d("getOtpRequest=", String.valueOf(jsonObject));
            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void apiResult(String res, String apiName) {
        try {
            if (res.equals("Server Time Out")) {
                getActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                    indexPosition = -1;
                    balanceEnquiryprogramAdapter.notifyDataSetChanged();
                });
            } else {
                Log.d("getOtpRequest = ", res);
                JSONObject jsonObject = new JSONObject(res);
                JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");
                JSONArray outputArray = payLoad.getJSONArray("billerTranList");
                String respDesc = payLoad.getString("respDesc");
                String respCode = payLoad.getString("respCode");

                if (respCode.equals("200")) {
                    progress.dismiss();
                    if (outputArray.length() > 0) {
                        Bundle bundle = new Bundle();
                        bundle.putString("id", id);
                        bundle.putString("mobNo", mobNo);

                        bundle.putInt("indexPosition", indexPosition);
                        bundle.putSerializable("programs", (Serializable) programList);

                        EnterOtpFragment enterOtpFragment = new EnterOtpFragment();
                        enterOtpFragment.setArguments(bundle);

                        ((SideBarActivity) requireActivity()).loadFragement(enterOtpFragment);
                    }
                } else {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(getActivity(), respDesc , 0,null, null);
                    });
                }
                }
        } catch (JSONException e) {
           // fileWrite(getContext(), todayDate + ".txt", "getbalanceEnquiryResponse", e.toString());
            getActivity().runOnUiThread(() -> {
                progress.dismiss();
                MessagesDialog.showDialog(getActivity(), e.toString(), 0,null, null);
                indexPosition = -1;
                balanceEnquiryprogramAdapter.notifyDataSetChanged();
            });
        }
    }

    @Override
    public void onClick(int position, List<Program> programs) {
        indexPosition = position;
        virtualCardInitiateOtpApi(position, programList);
    }
}