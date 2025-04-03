package com.ims.bpcluat;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class TxnListRecyclerAdapter extends RecyclerView.Adapter<TxnListRecyclerAdapter.ViewHolder> {
    private final TxnListRecyclerViewInterface recyclerViewInterface;
    Context context;
    ArrayList<TxnListModal> contactList;

    public TxnListRecyclerAdapter(Context context, ArrayList<TxnListModal> contactList, TxnListRecyclerViewInterface recyclerViewInterface) {
        this.context = context;
        this.contactList = contactList;
        this.recyclerViewInterface = recyclerViewInterface;
    }

    @NonNull
    @Override
    public TxnListRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.txn_list_row,parent,false);
        TxnListRecyclerAdapter.ViewHolder viewHolder = new TxnListRecyclerAdapter.ViewHolder(view,recyclerViewInterface);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull TxnListRecyclerAdapter.ViewHolder holder, int position) {
        holder.qty.setText(contactList.get(position).qty);
        holder.product.setText(contactList.get(position).product);
        holder.amt.setText(contactList.get(position).amt);
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView qty,product,amt;
        Button payBtn;
        public ViewHolder(@NonNull View itemView, TxnListRecyclerViewInterface recyclerViewInterface) {
            super(itemView);
            qty = itemView.findViewById(R.id.qty);
            product = itemView.findViewById(R.id.product);
            amt = itemView.findViewById(R.id.amt);
            payBtn = itemView.findViewById(R.id.payBtn);

            payBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(recyclerViewInterface != null){
                        int pos = getAdapterPosition();
                        if(pos != RecyclerView.NO_POSITION){
                            recyclerViewInterface.onPayBtnClick(pos);
                        }
                    }
                }
            });
        }
    }
}

