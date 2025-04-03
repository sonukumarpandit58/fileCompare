package com.ims.bpcluat.adapter.alp_adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.ims.bpcluat.R;
import com.ims.bpcluat.interfaces.WalletListInterface;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProgramWallet;
import com.ims.bpcluat.model.AlpModels.SaleModel.VirtualCardProgramModel;

import java.util.List;

public class WalletProgramsAdapter extends RecyclerView.Adapter<WalletProgramsAdapter.ViewHolder> {
    private List<ProgramWallet> walletList;
    private List<VirtualCardProgramModel> virtualCardProgramModels;
    private Context context;
    private int selectedPosition = -1;
    private WalletListInterface walletListInterface;


    public WalletProgramsAdapter(Context context, List<ProgramWallet> walletList, List<VirtualCardProgramModel> virtualCardProgramModelList, WalletListInterface walletListInterface) {
        this.walletList = walletList;
        this.context = context;
        this.virtualCardProgramModels = virtualCardProgramModelList;
        this.walletListInterface = walletListInterface;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.wallet_program_gridview_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProgramWallet wallet = walletList.get(position);
        holder.walletName.setText(wallet.getWalletName());

        holder.walletId.setOnClickListener(v -> {
            if (selectedPosition != holder.getAdapterPosition()) {
                selectedPosition = holder.getAdapterPosition();
                Log.d("postiton", String.valueOf(selectedPosition));

                notifyDataSetChanged();
                if (walletListInterface != null) {
                    Log.d("postiton#", String.valueOf(selectedPosition));
                    walletListInterface.walletClick(selectedPosition, virtualCardProgramModels);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return walletList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView walletId;
        TextView walletName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            walletId = itemView.findViewById(R.id.walletId);
            walletName = itemView.findViewById(R.id.walletName);
        }
    }
}
