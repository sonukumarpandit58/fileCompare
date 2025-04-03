package com.ims.bpcluat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.TxnListRecyclerViewInterface;
import com.ims.bpcluat.fragment.OnlineSingleTransactionFragment;
import com.ims.bpcluat.model.TxnListModel;

import java.util.ArrayList;

public class TxnListRecyclerAdapter extends RecyclerView.Adapter<TxnListRecyclerAdapter.ViewHolder> {
    private final TxnListRecyclerViewInterface recyclerViewInterface;
    Context context;
    ArrayList<TxnListModel> contactList;

    public TxnListRecyclerAdapter(Context context, ArrayList<TxnListModel> contactList, TxnListRecyclerViewInterface recyclerViewInterface) {
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

