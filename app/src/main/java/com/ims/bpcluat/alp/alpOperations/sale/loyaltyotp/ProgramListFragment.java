package com.ims.bpcluat.alp.alpOperations.sale.loyaltyotp;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.adapter.alp_adapters.    ProgramsAdapter;
import com.ims.bpcluat.alp.alpOperations.sale.TxnFailFragment;
import com.ims.bpcluat.databinding.FragmentProgramListBinding;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProgramOutput;
import com.ims.bpcluat.model.AlpModels.SaleModel.VirtualCardProgramModel;
import com.ims.bpcluat.model.CngModel;
import com.ims.bpcluat.model.NfrModel;
import com.ims.bpcluat.model.OnlineTxnModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class ProgramListFragment extends Fragment{
    FragmentProgramListBinding binding;
    ApiHelper api;
    Context context;
    ProgramsAdapter programsAdapter;
    List<VirtualCardProgramModel> virtualCardProgramModelArrayList = new ArrayList<>();
    List<ProgramOutput> programOutputList = new ArrayList<>();
    private CngModel cngModel;
    String amount = "", mobileNum;
    private OnlineTxnModel onlineTxnModel;
    private NfrModel nfrModel;

    String mobileNumber = "";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProgramListBinding.inflate(inflater, container, false);
        setRetainInstance(true);

        api = new ApiHelper();
        context = getContext();
        hideKeyboard();

        Bundle bundle = getArguments();
        if (bundle != null) {
            VirtualCardProgramModel virtualCardProgramModel = (VirtualCardProgramModel) bundle.getSerializable("virtualCardProgramModel");
            if (virtualCardProgramModel != null) {
                cngModel = getArguments().getParcelable("cngModel");
                onlineTxnModel = getArguments().getParcelable("onlineTxnModel");
                nfrModel = getArguments().getParcelable("nfrModel");
                programOutputList = virtualCardProgramModel.getOutput();
                virtualCardProgramModelArrayList.add(virtualCardProgramModel);
                Log.d("sizeTest", String.valueOf(programOutputList.size()));
            }
        }

        RecyclerView recyclerView = binding.gridView;
        recyclerView.setLayoutManager(new GridLayoutManager(context, 2));
        programsAdapter = new ProgramsAdapter(context, programOutputList, virtualCardProgramModelArrayList, cngModel, nfrModel, onlineTxnModel);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(programsAdapter);

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cngModel != null) {
                    TxnFailFragment fragment = new TxnFailFragment();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("cngModel", cngModel);
                    bundle.putParcelable("nfrModel", nfrModel);
                    bundle.putParcelable("onlineTxnModel", onlineTxnModel);
                    bundle.putString("txnStatus", "Transaction Failed!");
                    ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);

                } else if (onlineTxnModel != null) {
                    TxnFailFragment fragment = new TxnFailFragment();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("cngModel", cngModel);
                    bundle.putParcelable("nfrModel", nfrModel);
                    bundle.putParcelable("onlineTxnModel", onlineTxnModel);
                    bundle.putString("txnStatus", "Transaction Failed!");

                    ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);

                } else if (nfrModel != null) {
                    TxnFailFragment fragment = new TxnFailFragment();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("cngModel", cngModel);
                    bundle.putParcelable("onlineTxnModel", onlineTxnModel);
                    bundle.putParcelable("nfrModel", nfrModel);
                    bundle.putString("txnStatus", "Transaction Failed!");

                    ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);

                }
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
}