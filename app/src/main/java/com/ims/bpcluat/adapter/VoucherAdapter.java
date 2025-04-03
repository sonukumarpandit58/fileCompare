package com.ims.bpcluat.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ims.bpcluat.R;
import com.ims.bpcluat.interfaces.ViewDetailRecyclerViewInterface;
import com.ims.bpcluat.interfaces.VoucherRecycerViewInterface;
import com.ims.bpcluat.model.TxnHistoryModel;
import com.ims.bpcluat.model.VoucherModel;

import java.util.ArrayList;

public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.ViewHolder> {
    private final VoucherRecycerViewInterface recyclerViewInterface;
    private Context context;
    private ArrayList<VoucherModel> voucherList;
    private int selectedPosition = -1; // No selection by default

    public VoucherAdapter(Context context, ArrayList<VoucherModel> voucherList, VoucherRecycerViewInterface recyclerViewInterface) {
        this.context = context;
        this.voucherList = voucherList;
        this.recyclerViewInterface = recyclerViewInterface;
    }

    @NonNull
    @Override
    public VoucherAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.vouchers_listrow, parent, false);
        return new ViewHolder(view, recyclerViewInterface);
    }

    @Override
    public void onBindViewHolder(@NonNull VoucherAdapter.ViewHolder holder, int position) {
        VoucherModel voucherModel = voucherList.get(position);
        holder.utrno.setText(voucherList.get(position).utrNo);
        holder.datetime.setText(voucherList.get(position).dateTime);
        holder.payamount.setText(voucherList.get(position).amount);
        holder.voucheramount.setText(voucherList.get(position).amtAuthorizedRs);
        holder.radiobutton.setChecked(position == selectedPosition);

        // Set an onClickListener for the RadioButton
        holder.radiobutton.setOnClickListener(v -> {
            // Uncheck all other RadioButtons and set this one as selected
            selectedPosition = holder.getAdapterPosition(); // Update selectedPosition
            for (VoucherModel model : voucherList) {
                model.setSelected(false);
            }
            voucherModel.setSelected(true);
            notifyDataSetChanged(); // Notify the adapter to refresh the UI
            if (recyclerViewInterface != null) {
                recyclerViewInterface.onclick(selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return voucherList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView utrno, datetime, payamount, voucheramount;
        RadioButton radiobutton;

        public ViewHolder(View itemView, VoucherRecycerViewInterface recyclerViewInterface) {
            super(itemView);
            utrno = itemView.findViewById(R.id.utrno);
            datetime = itemView.findViewById(R.id.datetime);
            payamount = itemView.findViewById(R.id.payamount);
            voucheramount = itemView.findViewById(R.id.voucheramount);
            radiobutton = itemView.findViewById(R.id.rbtn);
        }
    }
}
