package com.ims.bpcluat.adapter;

import android.content.Context;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ims.bpcluat.R;
import com.ims.bpcluat.TxnListRecyclerViewInterface;
import com.ims.bpcluat.interfaces.NfrCartInterface;
import com.ims.bpcluat.model.NfrCartModel;
import com.ims.bpcluat.model.TxnListModel;
import com.ims.bpcluat.validation.DecimalDigitsInputFilter;

import java.util.ArrayList;

public class NfrCartRecyclerViewAdapter extends RecyclerView.Adapter<NfrCartRecyclerViewAdapter.ViewHolder> {
    private final NfrCartInterface nfrCartInterface;
    Context context;
    ArrayList<NfrCartModel> nfrCartModels;
    public NfrCartRecyclerViewAdapter(Context context, ArrayList<NfrCartModel> nfrCartModels,NfrCartInterface nfrCartInterface){
        this.context = context;
        this.nfrCartModels = nfrCartModels;
        this.nfrCartInterface = nfrCartInterface;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.nfr_cart_row,parent,false);
        ViewHolder viewHolder = new NfrCartRecyclerViewAdapter.ViewHolder(view,nfrCartInterface);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.productName.setText(nfrCartModels.get(position).getProductName());
        holder.price.setText(nfrCartModels.get(position).getPrice());
        holder.qty.setText(nfrCartModels.get(position).getQty());
    }

    @Override
    public int getItemCount() {
        return nfrCartModels.size();
    }

    public void updateQty(int position, String qty) {
        nfrCartModels.get(position).setQty(qty);
        notifyItemChanged(position);
    }

    public void clearData() {
        nfrCartModels.clear();
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        TextView productName, qty;
        EditText price;
        Button btnDecrease,btnIncrease;
        ImageButton btnDelete;
        public ViewHolder(@NonNull View itemView,NfrCartInterface nfrCartInterface) {
            super(itemView);
            productName = itemView.findViewById(R.id.tvProductName);
            price = itemView.findViewById(R.id.etPrice);
            qty = itemView.findViewById(R.id.tvQuantity);
            btnDecrease = itemView.findViewById(R.id.btnDecrease);
            btnIncrease = itemView.findViewById(R.id.btnIncrease);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            price.setFilters(new InputFilter[] { new DecimalDigitsInputFilter(2,100000) });

            price.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(nfrCartInterface != null){
                        int pos = getAdapterPosition();
                        if(pos != RecyclerView.NO_POSITION){
                            nfrCartInterface.onPriceClick(pos,price);
                        }
                    }
                }
            });

            btnDecrease.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(nfrCartInterface != null){
                        int pos = getAdapterPosition();
                        if(pos != RecyclerView.NO_POSITION){
                            nfrCartInterface.onMinusClick(pos);
                        }
                    }
                }
            });

            btnIncrease.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(nfrCartInterface != null){
                        int pos = getAdapterPosition();
                        if(pos != RecyclerView.NO_POSITION){
                            nfrCartInterface.onPlusClick(pos);
                        }
                    }
                }
            });

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(nfrCartInterface != null){
                        int pos = getAdapterPosition();
                        if(pos != RecyclerView.NO_POSITION){
                            nfrCartInterface.onDeleteClick(pos);
                        }
                    }
                }
            });
        }
    }

}