package com.ims.bpcluat.adapter.alp_adapters;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.alp.alpOperations.sale.AmountOtpFragment;
import com.ims.bpcluat.alp.alpOperations.sale.ProductListFragment;
import com.ims.bpcluat.model.AlpModels.SaleModel.ProductModel;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
   private List<ProductModel> productModelList;
    private int selectedPosition = -1;
    private Context context;

    public ProductAdapter(List<ProductModel> productModelList, Context context) {
        this.productModelList = productModelList;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder( ViewGroup parent, int viewType) {
         View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.smart_pay_qr_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ProductModel productModel = productModelList.get(position);

        holder.productName.setText(productModel.getProductName());

        holder.productId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedPosition != holder.getAdapterPosition()) {
                    selectedPosition = holder.getAdapterPosition();
                    Log.d("postiton", String.valueOf(selectedPosition));

                    AmountOtpFragment fragment = new AmountOtpFragment();
                    Bundle bundle = new Bundle();
                    bundle.putInt("index", selectedPosition);
                    bundle.putSerializable("productList", (ArrayList<ProductModel>) productModelList);
                    fragment.setArguments(bundle);
                    ((SideBarActivity) context).loadFragement(fragment);
                }
            }
        });
    }


    @Override
    public int getItemCount() {
        return productModelList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        LinearLayout productId;
        TextView productName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            productId = itemView.findViewById(R.id.productId);
            productName = itemView.findViewById(R.id.productName);
        }
    }
}
