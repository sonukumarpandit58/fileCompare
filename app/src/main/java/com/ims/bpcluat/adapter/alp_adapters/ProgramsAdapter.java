package com.ims.bpcluat.adapter.alp_adapters;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.alp.alpOperations.sale.ProductListFragment;
import com.ims.bpcluat.alp.alpOperations.sale.loyaltyotp.EnterDetailsFragment;
import com.ims.bpcluat.alp.alpOperations.sale.loyaltyotp.ProgramWalletListFragment;
import com.ims.bpcluat.model.AlpModels.SaleModel.Program;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProgramOutput;
import com.ims.bpcluat.model.AlpModels.SaleModel.VirtualCardProgramModel;
import com.ims.bpcluat.model.CngModel;
import com.ims.bpcluat.model.NfrModel;
import com.ims.bpcluat.model.OnlineTxnModel;

import java.util.ArrayList;
import java.util.List;

public class ProgramsAdapter extends RecyclerView.Adapter<ProgramsAdapter.ViewHolder> {
    private List<VirtualCardProgramModel> virtualCardProgramModelList;
    private List<Program> programList;
    private Context context;
    private int selectedPosition = -1;
    private CngModel cngModel;
    private OnlineTxnModel onlineTxnModel;
    private NfrModel nfrModel;


    public ProgramsAdapter(Context context, List<ProgramOutput> programOutputList, List<VirtualCardProgramModel> virtualCardProgramModels, CngModel cngModel, NfrModel nfrModel, OnlineTxnModel onlineTxnModel) {
        this.context = context;
        this.programList = new ArrayList<>();
        this.virtualCardProgramModelList = virtualCardProgramModels;

        this.onlineTxnModel = onlineTxnModel;
        this.nfrModel = nfrModel;
        this.cngModel = cngModel;

        for (ProgramOutput programOutput : programOutputList) {
            if (programOutput.getPrograms() != null && !programOutput.getPrograms().isEmpty()) {
                this.programList.addAll(programOutput.getPrograms());
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.program_grid_view_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Program program = programList.get(position);
        holder.productName.setText(program.getProgramID());

            holder.productClick.setOnClickListener(view -> {
                if (selectedPosition != holder.getAdapterPosition()) {
                    selectedPosition = holder.getAdapterPosition();
                    Log.d("position", String.valueOf(selectedPosition));
                    notifyDataSetChanged();

                    Bundle bundle = new Bundle();

                    if(program.getProgramID().equals("SmartFleet")){
                        ProgramWalletListFragment fragment = new ProgramWalletListFragment();
                        bundle.putInt("index", selectedPosition);
                        bundle.putString("Drive", "SmartFleet");
                        bundle.putParcelable("cngModel", cngModel);
                        bundle.putParcelable("nfrModel", nfrModel);
                        bundle.putParcelable("onlineTxnModel", onlineTxnModel);
                        bundle.putSerializable("virtualProgramList", (ArrayList<VirtualCardProgramModel>) virtualCardProgramModelList);
                        fragment.setArguments(bundle);
                        ((SideBarActivity) context).loadFragement(fragment);
                    } else if (program.getProgramID().equals("SmartDrive")){
                        EnterDetailsFragment fragment = new EnterDetailsFragment();
                        bundle.putInt("index", selectedPosition);
                        bundle.putString("Drive", "SmartDrive");
                        bundle.putParcelable("cngModel", cngModel);
                        bundle.putParcelable("nfrModel", nfrModel);
                        bundle.putParcelable("onlineTxnModel", onlineTxnModel);
                        bundle.putSerializable("virtualProgramList", (ArrayList<VirtualCardProgramModel>) virtualCardProgramModelList);
                        fragment.setArguments(bundle);
                        ((SideBarActivity) context).loadFragement(fragment);
                    } else {
                        EnterDetailsFragment fragment = new EnterDetailsFragment();
                        bundle.putInt("index", selectedPosition);
                        bundle.putString("Drive", "PetroCorp");
                        bundle.putParcelable("cngModel", cngModel);
                        bundle.putParcelable("nfrModel", nfrModel);
                        bundle.putParcelable("onlineTxnModel", onlineTxnModel);
                        bundle.putSerializable("virtualProgramList", (ArrayList<VirtualCardProgramModel>) virtualCardProgramModelList);
                        fragment.setArguments(bundle);
                        ((SideBarActivity) context).loadFragement(fragment);
                    }

                }
            });

    }

    @Override
    public int getItemCount() {
        return programList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CardView productClick;
        TextView productName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productClick = itemView.findViewById(R.id.programId);
            productName = itemView.findViewById(R.id.programName);
        }
    }

}
