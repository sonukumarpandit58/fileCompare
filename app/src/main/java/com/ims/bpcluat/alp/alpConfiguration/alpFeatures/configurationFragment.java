package com.ims.bpcluat.alp.alpConfiguration.alpFeatures;

import static com.ims.bpcluat.Helper.appVersion;
import static com.ims.bpcluat.Helper.fileWrite;
import static com.ims.bpcluat.Helper.fuelProductList;
import static com.ims.bpcluat.Helper.getProductId;
import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
//import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.sapCode;
//import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.todayDate;
import static com.ims.bpcluat.Helper.username;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.alp.alpConfiguration.ConfigurationsFragment;
import com.ims.bpcluat.alp.alpOperations.utility.UtilityFragment;
import com.ims.bpcluat.databinding.FragmentConfigurationBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProductModel;
import com.ims.bpcluat.nfr.NfrPaymentActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class configurationFragment extends Fragment{

    FragmentConfigurationBinding binding;
    ProgressDialog progress;
    ApiHelper api;
    Context context;
    String txnId = "", dateTime = "";

    public configurationFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentConfigurationBinding.inflate(inflater, container, false);

        api = new ApiHelper();
        context = getContext();

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SideBarActivity) requireActivity()).loadFragement(new ConfigurationsFragment());
            }
        });

        argData();

        return binding.getRoot();
    }

    private void argData() {
        if (getArguments() != null) {
            String payLoadString = getArguments().getString("payLoad");

            try {
                JSONObject payLoad = new JSONObject(payLoadString);

                JSONArray outputArray = payLoad.getJSONArray("output");

                if (outputArray.length() != 0) {
                    JSONObject outputObject = outputArray.getJSONObject(0);
                    JSONObject terminalParameter = outputObject.getJSONObject("terminalParameter");

                    boolean rechargeAllowed = terminalParameter.getBoolean("rechargeAllowed");
                    String voidTimeLimit = terminalParameter.getString("voidTimeLimit");
                    String petromilesAllowed = terminalParameter.getString("petromilesAllowed");
                    String nonSapAllowed = terminalParameter.getString("nonSapAllowed");
                    boolean preAuthAllowed = terminalParameter.getBoolean("preAuthAllowed");
                    boolean voidAllowed = terminalParameter.getBoolean("voidAllowed");
                    boolean smartpayAllowed = terminalParameter.getBoolean("smartpayAllowed");
                    String reprintTimeLimit = terminalParameter.getString("reprintTimeLimit");
                    boolean salesAllowed = terminalParameter.getBoolean("salesAllowed");
                    boolean enrollmentAllowed = terminalParameter.getBoolean("enrollmentAllowed");
                    String configurationVersion = terminalParameter.getString("configurationVersion");

                    String rechargeAllowedText = String.valueOf(rechargeAllowed);
                    String preAuthAllowedText = String.valueOf(preAuthAllowed);
                    String voidAllowedText = String.valueOf(voidAllowed);
                    String smartpayAllowedText = String.valueOf(smartpayAllowed);
                    String salesAllowedText = String.valueOf(salesAllowed);
                    String enrollmentAllowedText = String.valueOf(enrollmentAllowed);

//                    getActivity().runOnUiThread(() -> {
                        binding.rechargeAllowed.setText(rechargeAllowedText);
                        binding.petromilesId.setText(petromilesAllowed);
                        binding.nonSapId.setText(nonSapAllowed);
                        binding.preAuthId.setText(preAuthAllowedText);
                        binding.voidAllowedId.setText(voidAllowedText);
                        binding.smartpayId.setText(smartpayAllowedText);
                        binding.reprintTimeLimitId.setText(reprintTimeLimit);
                        binding.voidTimeLimitId.setText(voidTimeLimit);
                        binding.saleAllowedId.setText(salesAllowedText);
                        binding.enrollmentAllowedId.setText(enrollmentAllowedText);

                        binding.configurationVersionId.setText(configurationVersion);
//                    });

                } else {
                    MessagesDialog.showDialog(requireContext(), "No terminal parameters available.", 0,null, null);

                    //Toast.makeText(requireContext(), "No terminal parameters available.", Toast.LENGTH_SHORT).show();
                }
            }catch (Exception e){
                Log.d("exception arg", "argData: "+e);
            }




        } else {
            Log.d("getArguments@#", "No arguments passed");
        }

    }
}