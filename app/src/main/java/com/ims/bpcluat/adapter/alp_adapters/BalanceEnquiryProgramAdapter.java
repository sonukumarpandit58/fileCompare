package com.ims.bpcluat.adapter.alp_adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.ims.bpcluat.R;
import com.ims.bpcluat.interfaces.BalanceEnquiryInterface;
import com.ims.bpcluat.model.AlpModels.SaleModel.Program;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProgramOutput;
import com.ims.bpcluat.model.AlpModels.SaleModel.VirtualCardProgramModel;

import java.util.ArrayList;
import java.util.List;

public class BalanceEnquiryProgramAdapter extends RecyclerView.Adapter<BalanceEnquiryProgramAdapter.ViewHolder> {
    private BalanceEnquiryInterface balanceEnquiryInterface;
    Context context;
    private List<VirtualCardProgramModel> virtualCardProgramModelList;
    private List<Program> programList;
    private int selectedPosition = -1;
    String mobilenumber = "";

    public BalanceEnquiryProgramAdapter(Context context, List<Program> programList, ArrayList<VirtualCardProgramModel> virtualCardProgramModels, BalanceEnquiryInterface balanceEnquiryInterface, String mobilenumber) {
        this.context = context;
        this.programList = programList;
        this.virtualCardProgramModelList = virtualCardProgramModels;
        this.balanceEnquiryInterface = balanceEnquiryInterface;
        this.mobilenumber = mobilenumber;

//        for (ProgramOutput programOutput : programOutputList) {
//            if (programOutput.getPrograms() != null && !programOutput.getPrograms().isEmpty()) {
//                this.programList.addAll(programOutput.getPrograms());
//            }
//        }
    }

    @NonNull
    @Override
    public BalanceEnquiryProgramAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.balance_enquiry_program_list_row, parent, false);
        BalanceEnquiryProgramAdapter.ViewHolder viewHolder = new BalanceEnquiryProgramAdapter.ViewHolder(view, balanceEnquiryInterface);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Program program = programList.get(position);
        holder.productName.setText(program.getProgramID());

    /*    holder.productClick.setOnClickListener(view -> {
            if (selectedPosition != holder.getAdapterPosition()) {
                selectedPosition = holder.getAdapterPosition();

                Log.d("position", String.valueOf(selectedPosition));
                notifyDataSetChanged();


                if(balanceEnquiryInterface != null){
                    balanceEnquiryInterface.onClick(selectedPosition, programList);
                }


            }

        });*/
        holder.productClick.setOnClickListener(view -> {
            if (selectedPosition != holder.getAdapterPosition()) {
                selectedPosition = holder.getAdapterPosition();

                Log.d("position", String.valueOf(selectedPosition));
                notifyDataSetChanged();  // Refresh the adapter to update UI

                if (balanceEnquiryInterface != null) {
                    balanceEnquiryInterface.onClick(selectedPosition, programList);
                }
            } else {
                // Allow re-clicks even if the item is already selected, especially after an error
                if (balanceEnquiryInterface != null) {
                    balanceEnquiryInterface.onClick(selectedPosition, programList);
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

        public ViewHolder(@NonNull View itemView, BalanceEnquiryInterface balanceEnquiryInterface) {
            super(itemView);
            productClick = itemView.findViewById(R.id.balanceTxnId);
            productName = itemView.findViewById(R.id.balanceproduct);


        }

    }
}
