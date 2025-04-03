package com.ims.bpcluat.adapter.alp_adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.ims.bpcluat.R;
import com.ims.bpcluat.interfaces.AlpVoidTransactionsInterface;
import com.ims.bpcluat.model.AlpModels.VoidModels.AlpTxnModel;
import com.ims.bpcluat.model.AlpModels.VoidModels.Output;
import com.ims.bpcluat.model.AlpModels.VoidModels.TxnList;

import java.util.ArrayList;
import java.util.List;

public class AlpTransactionAdapter extends RecyclerView.Adapter<AlpTransactionAdapter.ViewHolder> {

    private AlpVoidTransactionsInterface alpVoidTransactionsInterface;
    private List<AlpTxnModel> alpTxnModelList;
    private List<TxnList> txnListItems;
    private Context context;
    private int selectedPosition = -1;

    public AlpTransactionAdapter(Context context, List<AlpTxnModel> alpTxnModels, AlpVoidTransactionsInterface alpVoidTransactionsInterface) {
        this.context = context;
        this.alpVoidTransactionsInterface = alpVoidTransactionsInterface;
        this.alpTxnModelList = alpTxnModels;
        this.txnListItems = new ArrayList<>();

        for (AlpTxnModel alpTxnModel : alpTxnModels) {
            if (alpTxnModel.getOutput() != null && !alpTxnModel.getOutput().isEmpty()) {
                for (Output output : alpTxnModel.getOutput()) {
                    if (output.getTxnList() != null && !output.getTxnList().isEmpty()) {
                        this.txnListItems.addAll(output.getTxnList());
                    }
                }
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.alp_void_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TxnList txnListItem = txnListItems.get(position);
        holder.aplTransactionID.setText(txnListItem.getAlpTransactionId());
        holder.voucherAmtTextView.setText(txnListItem.getTxnAmount());

        holder.txnId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    selectedPosition = holder.getAdapterPosition();
                    Log.d("postitonfffff", String.valueOf(selectedPosition));

                    if (alpVoidTransactionsInterface != null) {
                        Log.d("postiton#", String.valueOf(selectedPosition));
                        alpVoidTransactionsInterface.onClick(selectedPosition, alpTxnModelList);
                    }
            }
        });
    }

    @Override
    public int getItemCount() {
        return txnListItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView aplTransactionID, voucherAmtTextView;
        CardView txnId;

        public ViewHolder(View itemView) {
            super(itemView);
            aplTransactionID = itemView.findViewById(R.id.alpTransactionId);
            voucherAmtTextView = itemView.findViewById(R.id.alpVoucherAmt);
            txnId = itemView.findViewById(R.id.alpTxnId);
        }
    }

}
