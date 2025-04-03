package com.ims.bpcluat.alp.alpOperations.sale.loyaltyotp;

import static com.ims.bpcluat.Helper.appVersion;
import static com.ims.bpcluat.Helper.manualGetClientId;
import static com.ims.bpcluat.Helper.manualGetInstId;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.username;
import static com.ims.bpcluat.utils.Navigation.BackWithData;

import android.app.ProgressDialog;
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
import android.widget.Toast;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.adapter.alp_adapters.ProgramsAdapter;
import com.ims.bpcluat.adapter.alp_adapters.WalletProgramsAdapter;
import com.ims.bpcluat.alp.alpOperations.sale.TxnFailFragment;
import com.ims.bpcluat.alp.alpOperations.utility.alpVoid.AlpVoidTransactions;
import com.ims.bpcluat.cng.CngFragment;
import com.ims.bpcluat.databinding.FragmentProgramWalletListBinding;
import com.ims.bpcluat.dialog.MessagesDialog;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.interfaces.WalletListInterface;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProductModel;
import com.ims.bpcluat.model.AlpModels.SaleModel.Program;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProgramOutput;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProgramWallet;
import com.ims.bpcluat.model.AlpModels.SaleModel.VirtualCardProgramModel;
import com.ims.bpcluat.model.AlpModels.VoidModels.AlpTxnModel;
import com.ims.bpcluat.model.CngModel;
import com.ims.bpcluat.model.NfrModel;
import com.ims.bpcluat.model.OnlineTxnModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProgramWalletListFragment extends Fragment implements WalletListInterface {
    FragmentProgramWalletListBinding binding;
    Context context;
    ApiHelper api;
    RecyclerView recyclerView;
    WalletProgramsAdapter walletProgramsAdapter;
    ArrayList<VirtualCardProgramModel> virtualCardProgramModelArrayList = new ArrayList<>();
    String txnId = "", dateTime = "";
    int selectedIndex = -1;
    String drive= "";

    private CngModel cngModel;
    private OnlineTxnModel onlineTxnModel;
    private NfrModel nfrModel;
    public ProgramWalletListFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProgramWalletListBinding.inflate(inflater, container, false);

        api = new ApiHelper();
        context = getContext();
        recyclerView = binding.walletGridView;
        hideKeyboard();

        Bundle bundle = getArguments();
        if (bundle != null) {
            drive = bundle.getString("Drive");
             selectedIndex = bundle.getInt("index", -1);
            cngModel = getArguments().getParcelable("cngModel");
            onlineTxnModel = getArguments().getParcelable("onlineTxnModel");
            nfrModel = getArguments().getParcelable("nfrModel");
            virtualCardProgramModelArrayList = (ArrayList<VirtualCardProgramModel>) bundle.getSerializable("virtualProgramList");
            if (selectedIndex != -1) {
            }
        }

        recyclerView.setLayoutManager(new GridLayoutManager(context, 2));

        List<ProgramWallet> programWalletList = new ArrayList<>();

        if (virtualCardProgramModelArrayList != null) {
            for (VirtualCardProgramModel virtualCardProgramModel : virtualCardProgramModelArrayList) {
                if (virtualCardProgramModel.getOutput() != null) {
                    for (ProgramOutput output : virtualCardProgramModel.getOutput()) {
                        for (Program program : output.getPrograms()) {
                            if (program.getProgramWallet() != null) {
                                programWalletList.addAll(program.getProgramWallet());
                            }
                        }
                    }
                }
            }
        }


        if (!programWalletList.isEmpty()) {
            walletProgramsAdapter = new WalletProgramsAdapter(context, programWalletList, virtualCardProgramModelArrayList, this);
            recyclerView.setHasFixedSize(true);
            recyclerView.setAdapter(walletProgramsAdapter);
        }

        //Bundle finalBundle = new Bundle();

/*        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finalBundle.putParcelable("cngModel", cngModel);
                finalBundle.putParcelable("nfrModel", nfrModel);
                finalBundle.putParcelable("onlineTxnModel", onlineTxnModel);
                VirtualCardProgramModel virtualCardProgramModel = virtualCardProgramModelArrayList.get(0);
                finalBundle.putSerializable("virtualCardProgramModel", virtualCardProgramModel);
                ((SideBarActivity) requireActivity()).loadFragmentWithData(finalBundle, new ProgramListFragment());
            }
        });*/

        binding.toolbar.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cngModel != null) {
                    TxnFailFragment fragment = new TxnFailFragment();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("cngModel", cngModel);
                    bundle.putString("txnStatus", "Transaction Failed!");

                    ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);

                } else if (onlineTxnModel != null) {
                    TxnFailFragment fragment = new TxnFailFragment();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("onlineTxnModel", onlineTxnModel);
                    bundle.putString("txnStatus", "Transaction Failed!");

                    ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);

                } else if (nfrModel != null) {
                    TxnFailFragment fragment = new TxnFailFragment();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("nfrModel", nfrModel);
                    bundle.putString("txnStatus", "Transaction Failed!");

                    ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, fragment);

                }
            }
        });

        return binding.getRoot();
    }

    @Override
    public void walletClick(int position, List<VirtualCardProgramModel> virtualCardProgramModelList) {
        if (virtualCardProgramModelArrayList != null && !virtualCardProgramModelArrayList.isEmpty()) {
            Bundle bundle = new Bundle();
            bundle.putParcelable("cngModel", cngModel);
            bundle.putParcelable("nfrModel", nfrModel);
            bundle.putParcelable("onlineTxnModel", onlineTxnModel);
            bundle.putInt("index", selectedIndex);
            bundle.putInt("wallet_index", position);
            bundle.putString("Drive", drive);
            bundle.putSerializable("virtualProgramList", (ArrayList<VirtualCardProgramModel>) virtualCardProgramModelList);

            ((SideBarActivity) requireActivity()).loadFragmentWithData(bundle, new EnterDetailsFragment());
        } else {
            MessagesDialog.showDialog(requireContext(), "No Wallet available", 0,null, null);

           // Toast.makeText(context, "No Wallet available", Toast.LENGTH_SHORT).show();
        }
    }

    public void hideKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

}