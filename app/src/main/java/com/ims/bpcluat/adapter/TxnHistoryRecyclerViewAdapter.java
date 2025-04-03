package com.ims.bpcluat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ims.bpcluat.R;
import com.ims.bpcluat.model.TxnHistoryModel;

import java.util.ArrayList;

public class TxnHistoryRecyclerViewAdapter extends RecyclerView.Adapter<TxnHistoryRecyclerViewAdapter.ViewHolder> {

    Context context;
    ArrayList<TxnHistoryModel> historyList;

    public TxnHistoryRecyclerViewAdapter(Context context, ArrayList<TxnHistoryModel> historyList) {
        this.context = context;
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.txn_history_row, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.pumpNo.setText(historyList.get(position).pumpNo);
        holder.quantity.setText(historyList.get(position).quantity);
        holder.dateTime.setText(historyList.get(position).dateTime);
        holder.price.setText(historyList.get(position).price);
        holder.txnType.setText(historyList.get(position).txnType);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView pumpNo, quantity, dateTime, price, txnType;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            pumpNo = itemView.findViewById(R.id.pumpNo);
            quantity = itemView.findViewById(R.id.quantity);
            dateTime = itemView.findViewById(R.id.dateTime);
            price = itemView.findViewById(R.id.price);
            txnType = itemView.findViewById(R.id.txnType);
        }
    }

}