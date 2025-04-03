package com.ims.bpcluat.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.ims.bpcluat.R;
import com.ims.bpcluat.interfaces.VoidTxnRecyclerViewInterface;
import com.ims.bpcluat.model.VoidReason;
import com.ims.bpcluat.model.VoidTransactionModel;

import java.util.List;

public class VoidTransactionAdapter extends RecyclerView.Adapter<VoidTransactionAdapter.ViewHolder> {

    private VoidTxnRecyclerViewInterface voidTxnRecyclerViewInterface;
    private List<VoidTransactionModel> voidTransactionModels;
    private Context context;
    private int selectedPosition = -1;
    public VoidTransactionAdapter(Context context, List<VoidTransactionModel> voidTransactionModels, VoidTxnRecyclerViewInterface voidTxnRecyclerViewInterface) {
        this.context = context;
        this.voidTransactionModels = voidTransactionModels;
        this.voidTxnRecyclerViewInterface = voidTxnRecyclerViewInterface;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.void_transaction_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        VoidTransactionModel model = voidTransactionModels.get(position);

        holder.mobileNoTextView.setText(model.getCustomerMobileNoMasked());
        holder.voucherCodeTextView.setText(model.getVoucherCode());
        holder.voucherAmtTextView.setText(model.getVoucherAmt());

        holder.txnId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                List<VoidReason> voidReasons = model.getVoidReasons();

                if (selectedPosition != holder.getAdapterPosition()) {
                    selectedPosition = holder.getAdapterPosition();
                    Log.d("postiton", String.valueOf(selectedPosition));

                    notifyDataSetChanged();
                    if (voidTxnRecyclerViewInterface != null) {
                        Log.d("postiton#", String.valueOf(selectedPosition));
                        voidTxnRecyclerViewInterface.onClick(selectedPosition, voidReasons, voidTransactionModels);
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return voidTransactionModels.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView mobileNoTextView, voucherCodeTextView, voucherAmtTextView;
        CardView txnId;

        public ViewHolder(View itemView) {
            super(itemView);
            mobileNoTextView = itemView.findViewById(R.id.mobileNoTextView);
            voucherCodeTextView = itemView.findViewById(R.id.voucherCodeTextView);
            voucherAmtTextView = itemView.findViewById(R.id.voucherAmtTextView);
            txnId = itemView.findViewById(R.id.txnId);
        }
    }



}
