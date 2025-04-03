package com.ims.bpcluat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ims.bpcluat.R;
import com.ims.bpcluat.interfaces.ViewDetailRecyclerViewInterface;
import com.ims.bpcluat.model.OperatorListModel;

import java.util.ArrayList;

public class InActiveOperatorListAdapter extends RecyclerView.Adapter<InActiveOperatorListAdapter.ViewHolder> {
    private final ViewDetailRecyclerViewInterface recyclerViewInterface;
    Context context;
    ArrayList<OperatorListModel> operatorList;

    public InActiveOperatorListAdapter(Context context, ArrayList<OperatorListModel> operatorList, ViewDetailRecyclerViewInterface recyclerViewInterface) {
        this.context = context;
        this.operatorList = operatorList;
        this.recyclerViewInterface = recyclerViewInterface;
    }

    @NonNull
    @Override

    public InActiveOperatorListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.inactive_operator_row, parent, false);
        //ViewHolder viewHolder = new ViewHolder(view);
        InActiveOperatorListAdapter.ViewHolder viewHolder = new InActiveOperatorListAdapter.ViewHolder(view, recyclerViewInterface);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull InActiveOperatorListAdapter.ViewHolder holder, int position) {
        holder.name.setText(operatorList.get(position).getFirstName() + " " + operatorList.get(position).getLastName());
        holder.mobileNumber.setText(operatorList.get(position).mobileNumber);
    }

    @Override
    public int getItemCount() {
        return operatorList.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, mobileNumber, viewdetails, resttpin;

        public ViewHolder(@NonNull View itemView, ViewDetailRecyclerViewInterface recyclerViewInterface) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_name);
            mobileNumber = itemView.findViewById(R.id.tv_mobnum);
            viewdetails = itemView.findViewById(R.id.tv_viewdetails);

            viewdetails.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // int pos = getAdapterPosition();
                    if (recyclerViewInterface != null) {
                        // Log.d("postiton", String.valueOf(pos));
                        int pos = getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            recyclerViewInterface.onViewDetailClick(pos);
                        }
                    }

                }
            });

        }
    }
}