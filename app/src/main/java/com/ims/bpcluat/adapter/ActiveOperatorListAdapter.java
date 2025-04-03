package com.ims.bpcluat.adapter;

import static com.ims.bpcluat.Helper.channelName;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.requestDate;
import static com.ims.bpcluat.Helper.requestTime;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.username;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.R;
import com.ims.bpcluat.fragment.ActiveOperatorFragment;
import com.ims.bpcluat.helper.ApiHelper;
import com.ims.bpcluat.interfaces.ViewDetailRecyclerViewInterface;
import com.ims.bpcluat.model.OperatorListModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ActiveOperatorListAdapter extends RecyclerView.Adapter<ActiveOperatorListAdapter.ViewHolder> {
    private final ViewDetailRecyclerViewInterface recyclerViewInterface;
    Context context;
    ProgressDialog progress;
    ApiHelper api;
    ArrayList<OperatorListModel> operatorList;

    public ActiveOperatorListAdapter(Context context, ArrayList<OperatorListModel> operatorList, ViewDetailRecyclerViewInterface recyclerViewInterface) {
        this.context = context;
        this.operatorList = operatorList;
        this.recyclerViewInterface = recyclerViewInterface;
    }

    @NonNull
    @Override

    public ActiveOperatorListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.active_operator_row, parent, false);
        //ViewHolder viewHolder = new ViewHolder(view);
        ActiveOperatorListAdapter.ViewHolder viewHolder = new ActiveOperatorListAdapter.ViewHolder(view, recyclerViewInterface);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ActiveOperatorListAdapter.ViewHolder holder, int position) {
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
            resttpin = itemView.findViewById(R.id.resttpin);

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
            resttpin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION){
                        recyclerViewInterface.onclick(pos);
                    }
                }
            });


        }
    }
}
