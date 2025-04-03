package com.ims.bpcluat.adapter.alp_adapters;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.alp.alpOperations.utility.reprint.Receipt;
import com.ims.bpcluat.model.AlpModels.ReprintTxnModel;
import com.ims.bpcluat.ufill.void_transaction.VoidTransaction;

import java.io.Serializable;
import java.util.List;

public class ReprintTxnAdapter extends RecyclerView.Adapter<ReprintTxnAdapter.ViewHolder> {
    private List<ReprintTxnModel> reprintTxnModelList;
    private Context context;
    private int selectedPosition = -1;


    public ReprintTxnAdapter(Context context, List<ReprintTxnModel> reprintTxnModelList) {
        this.context = context;
        this.reprintTxnModelList = reprintTxnModelList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.reprint_txn_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ReprintTxnModel reprintTxnModel = reprintTxnModelList.get(position);
        holder.amount.setText(reprintTxnModel.getAmountPaid());
        holder.dateTime.setText(reprintTxnModel.getTimestamp());

        holder.txnId.setOnClickListener(view -> {

            Intent intent = new Intent(context, Receipt.class);
            intent.putExtra("reprintTxnModel", (Serializable) reprintTxnModel);
            context.startActivity(intent);

        });
    }


    @Override
    public int getItemCount() {
        return reprintTxnModelList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CardView txnId;
        TextView amount;
        TextView dateTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txnId = itemView.findViewById(R.id.txnId);
            amount = itemView.findViewById(R.id.price);
            dateTime = itemView.findViewById(R.id.dateTime);
        }
    }

}
