package com.ims.bpcluat.alp.alpOperations.sale.loyaltyotp;

import static com.ims.bpcluat.Helper.appVersion;
import static com.ims.bpcluat.Helper.fileWrite;

import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
////import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.sapCode;
////import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.Helper.username;
import static com.ims.bpcluat.utils.Navigation.BackWithData;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.PaymentActivity;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.alp.alpOperations.sale.SaleFragment;
import com.ims.bpcluat.alp.alpOperations.sale.TxnFailFragment;
import com.ims.bpcluat.cng.CngPaymentActivity;
import com.ims.bpcluat.cng.CngSuccessActivity;
import com.ims.bpcluat.databinding.FragmentMobileNumberBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.model.AlpModels.SaleModel.Program;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProgramOutput;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProgramWallet;
import com.ims.bpcluat.model.AlpModels.SaleModel.VirtualCardProgramModel;
import com.ims.bpcluat.model.CngModel;
import com.ims.bpcluat.model.NfrModel;
import com.ims.bpcluat.model.OnlineTxnModel;
import com.ims.bpcluat.nfr.NfrPaymentActivity;
import com.ims.bpcluat.validation.MobileNoValidation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class MobileNumberFragment extends Fragment implements ApiHelper.NetworkingApiCallBack {
    FragmentMobileNumberBinding binding;
    ProgressDialog progress;
    ApiHelper api;
    String txnId = "", dateTime = "", isTxnOnline = "";
    String programlist = "";

    private CngModel cngModel;
    String amount = "", mobileNumber = "";
    private OnlineTxnModel onlineTxnModel;
    private NfrModel nfrModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMobileNumberBinding.inflate(inflater, container, false);
        api = new ApiHelper();


        if (getArguments() != null) {
            cngModel = getArguments().getParcelable("cngModel");
            onlineTxnModel = getArguments().getParcelable("onlineTxnModel");
            nfrModel = getArguments().getParcelable("nfrModel");

            if (cngModel != null) {
                mobileNumber = cngModel.getMobileNumber();
            } else if (onlineTxnModel != null) {
                mobileNumber = onlineTxnModel.getMobileNumber();
                isTxnOnline = onlineTxnModel.getIsTxnOnline();
            } else if(nfrModel != null){
                mobileNumber = nfrModel.getMobileNumber();
            }
            binding.entermobilenum.setText(mobileNumber);
        }

   /*     binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SaleFragment fragment = new SaleFragment();
                Bundle bundle = new Bundle();

                if (cngModel != null) {
                    cngModel.setMobileNumber(mobileNumber);
                } else if (onlineTxnModel != null) {
                    onlineTxnModel.setMobileNumber(mobileNumber);
                } else if(nfrModel != null){
                    nfrModel.setMobileNumber(mobileNumber);
                }

                bundle.putParcelable("cngModel", cngModel);
                bundle.putParcelable("nfrModel", nfrModel);
                bundle.putParcelable("onlineTxnModel", onlineTxnModel);
                ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);
            }
        });*/

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cngModel != null) {
                    SaleFragment fragment = new SaleFragment();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("cngModel", cngModel);
                    ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);

                } else if (onlineTxnModel != null) {
                    SaleFragment fragment = new SaleFragment();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("onlineTxnModel", onlineTxnModel);
                    ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);

                } else if (nfrModel != null) {
                    SaleFragment fragment = new SaleFragment();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("nfrModel", nfrModel);
                    ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);

                }
            }
        });

        binding.submitMobBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateFields();
            }
        });

        return binding.getRoot();
    }

    private void validateFields() {
        mobileNumber = binding.entermobilenum.getText().toString().trim();
        if (TextUtils.isEmpty(mobileNumber)) {
            binding.entermobilenum.setError("Please enter mobile number");
            binding.entermobilenum.requestFocus();

            return;
        }
        if (TextUtils.isEmpty(mobileNumber)) {
            binding.entermobilenum.setError("Please enter mobile number");
            binding.entermobilenum.requestFocus();
            return;
        }
        if (mobileNumber.length() != 10) {
            binding.entermobilenum.setError("Mobile Number must be 10 digits");
            binding.entermobilenum.requestFocus();
            return;
        }
        if (mobileNumber.startsWith("0")) {
            binding.entermobilenum.setError("Mobile Number cannot start with zero");
            binding.entermobilenum.requestFocus();
            return;
        }
        if (mobileNumber.equals("1234567890")) {
            binding.entermobilenum.setError("Please enter valid mobile number");
            binding.entermobilenum.requestFocus();
            return;
        }
        if (MobileNoValidation.hasSameNumber(mobileNumber)) {
            binding.entermobilenum.setError("All digits of mobile number cannot be same.");
            binding.entermobilenum.requestFocus();
            return;
        }
        fetchVirtualCardProgramApi(mobileNumber);
    }

    private void fetchVirtualCardProgramApi(String mobileNum) {
        programlist = "fetchVirtualCardProgramApi";
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
            jsonObject.put("reqDate", reqDate);
            jsonObject.put("reqTime", reqTime);
            jsonObject.put("mid", mid);
            jsonObject.put("tid", tid);
            jsonObject.put("roCode", sapCode);
            jsonObject.put("txnType", "AVC");
            jsonObject.put("tranChannel", "VC");
            jsonObject.put("source", "Mobile");
            jsonObject.put("dateTime", dateTime);
            jsonObject.put("mobNo", mobileNum);
            jsonObject.put("txnId", txnId);
            jsonObject.put("hwSrNo", Helper.serialNumber);
            jsonObject.put("latitude", "0");
            jsonObject.put("longitude", "0");
            jsonObject.put("geotagRange", "10");
            jsonObject.put("client", manualGetClientId());
            jsonObject.put("instId", manualGetInstId());
            jsonObject.put("appVersion", "BPCL" + appVersion);

            Log.d("ApiName","AVC");
            Log.d("FileName","MobileNumberFragment");
            Log.d("ApiRequest", String.valueOf(jsonObject));

            api.networking(jsonObject, url, "20");
            api.setApiCallBack(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void apiResult(String res, String apiName) {
        if (programlist.equals("fetchVirtualCardProgramApi")) {
            try {
                if (res.equals("Server Time Out")) {
                    getActivity().runOnUiThread(() -> {
                        progress.dismiss();
                        MessagesDialog.showDialog(requireContext(), "Server Time Out", 0,null, null);
                    });
                } else {
                    Log.d("programListResponse = ", res);
                    JSONObject jsonObject = new JSONObject(res);
                    JSONObject payLoad = jsonObject.getJSONObject("nameValuePairs").getJSONObject("PAYLOAD");

                    VirtualCardProgramModel virtualCardProgramModel = new VirtualCardProgramModel();
                    virtualCardProgramModel.setRespDesc(payLoad.getString("respDesc"));
                    virtualCardProgramModel.setRespCode(payLoad.getString("respCode"));

                    String respDesc = payLoad.getString("respDesc");
                    String respCode = payLoad.getString("respCode");

                    if (respCode.equals("200")) {
                        Log.d("fetchResponseCode2", respCode);

                        progress.dismiss();
                        JSONArray outputArray = payLoad.getJSONArray("output");
                        List<ProgramOutput> outputList = new ArrayList<>();

                        for (int i = 0; i < outputArray.length(); i++) {
                            JSONObject outputObject = outputArray.getJSONObject(i);

                            ProgramOutput programOutput = new ProgramOutput();
                            programOutput.setRedirect(outputObject.getString("redirect"));
                            programOutput.setStatus(outputObject.getString("status"));

                            if (outputObject.has("statusCode")) {
                                programOutput.setStatusCode(outputObject.getString("statusCode"));
                            }

                            if (outputObject.has("statusCode")) {
                                programOutput.setStatusCode(outputObject.getString("statusCode"));

                            }

                            JSONArray programsArray = outputObject.getJSONArray("programs");
                            List<Program> programsList = new ArrayList<>();

                            for (int j = 0; j < programsArray.length(); j++) {
                                JSONObject programObject = programsArray.getJSONObject(j);

                                Program program = new Program("", "", "");
                                program.setProgram(programObject.getString("program"));
                                program.setAccountNumber(programObject.getString("accountNumber"));
                                program.setCardNumber(programObject.getString("cardNumber"));
                                program.setProgramID(programObject.getString("programID"));

                                if (programObject.has("programWallet")) {
                                    JSONArray programWalletArray = programObject.getJSONArray("programWallet");
                                    List<ProgramWallet> programWalletList = new ArrayList<>();

                                    for (int k = 0; k < programWalletArray.length(); k++) {
                                        JSONObject walletObject = programWalletArray.getJSONObject(k);

                                        ProgramWallet programWallet = new ProgramWallet();
                                        programWallet.setWalletId(walletObject.getString("walletId"));
                                        programWallet.setWalletName(walletObject.getString("walletName"));

                                        programWalletList.add(programWallet);
                                    }
                                    program.setProgramWallet(programWalletList);

                                    if (!programWalletList.isEmpty()) {
                                        program.setProgramWallet(programWalletList);
                                    } else {
                                        Log.d("EmptyWalletList", "Program wallet list is empty for program: " + program.getProgram());
                                        continue;
                                    }
                                }

                                programsList.add(program);
                            }

                            programOutput.setPrograms(programsList);
                            outputList.add(programOutput);
                        }

                        virtualCardProgramModel.setOutput(outputList);

                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            Log.d("programResponse = ", res);

                            ProgramListFragment fragment = new ProgramListFragment();
                            Bundle bundle = new Bundle();
                            bundle.putParcelable("cngModel", cngModel);
                            bundle.putParcelable("nfrModel", nfrModel);
                            bundle.putParcelable("onlineTxnModel", onlineTxnModel);
                            //cngModel.setMobileNumber(mobileNumber);
                            if (cngModel != null) {
                                cngModel.setMobileNumber(mobileNumber);
                            } else if (onlineTxnModel != null) {
                                onlineTxnModel.setMobileNumber(mobileNumber);
                            } else if(nfrModel != null){
                                nfrModel.setMobileNumber(mobileNumber);
                            }

                            bundle.putSerializable("virtualCardProgramModel", virtualCardProgramModel);
                            ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);
                        });

                    } else {
                        getActivity().runOnUiThread(() -> {
                            progress.dismiss();
                            Log.d("programResponseException = ", res);
                            Intent intent = new Intent();
                            Log.d("TAG", "apiResult: hhhhhheyy");
                            if (cngModel != null) {
                                intent = new Intent(getActivity(), CngPaymentActivity.class);
                                intent.putExtra("cngModel", cngModel);
                                intent.putExtra("Insertcard", "Insertcard");
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                Log.d("TAG", "cngModel: hhhhhheyy");

                            } else if (onlineTxnModel != null) {
                                if(isTxnOnline.equals("no")){
                                    intent = new Intent(getActivity(), PaymentActivity.class);
                                    intent.putExtra("onlineTxnModel", onlineTxnModel);
                                    intent.putExtra("isTxnOnline", "isTxnOnline");
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                }else{
                                    intent = new Intent(getActivity(), PaymentActivity.class);
                                    intent.putExtra("onlineTxnModel", onlineTxnModel);
                                    intent.putExtra("Insertcard", "Insertcard");
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                }
                            } else if (nfrModel != null) {
                                intent = new Intent(getActivity(), NfrPaymentActivity.class);
                                intent.putExtra("nfrModel", nfrModel);
                                intent.putExtra("Insertcard", "Insertcard");
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                Log.d("TAG", "nfrModel: hhhhhheyy");
                            }
                            MessagesDialog.showDialog(getContext(), respDesc, 0,intent, null);
                        });
                    }
                }
            } catch (JSONException e) {
                fileWrite(getContext(), todayDate + ".txt", "fetchVirtualCardProgramApi", e.toString());
                getActivity().runOnUiThread(() -> {
                    if(progress.isShowing()){
                        progress.dismiss();
                    }

                    Intent intent = new Intent();
                    Log.d("TAG", "apiResult: hhhhhheyy");
                    if (cngModel != null) {
                        intent = new Intent(getActivity(), CngPaymentActivity.class);
                        intent.putExtra("cngModel", cngModel);
                        intent.putExtra("Insertcard", "Insertcard");
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        Log.d("TAG", "cngModel: hhhhhheyy");

                    } else if (onlineTxnModel != null) {
                        if(isTxnOnline.equals("no")){
                            intent = new Intent(getActivity(), PaymentActivity.class);
                            intent.putExtra("onlineTxnModel", onlineTxnModel);
                            intent.putExtra("isTxnOnline", "isTxnOnline");
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        }else{
                            intent = new Intent(getActivity(), PaymentActivity.class);
                            intent.putExtra("onlineTxnModel", onlineTxnModel);
                            intent.putExtra("Insertcard", "Insertcard");
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        }
                    } else if (nfrModel != null) {
                        intent = new Intent(getActivity(), NfrPaymentActivity.class);
                        intent.putExtra("nfrModel", nfrModel);
                        intent.putExtra("Insertcard", "Insertcard");
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        Log.d("TAG", "nfrModel: hhhhhheyy");
                    }
                    MessagesDialog.showDialog(getContext(), e.toString(),0, intent, null);

                    e.printStackTrace();
                });
            }
        }
    }


}