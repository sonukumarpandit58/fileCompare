package com.ims.bpcluat.interfaces;

import com.ims.bpcluat.model.VoidReason;
import com.ims.bpcluat.model.VoidTransactionModel;

import java.util.List;

public interface VoidTxnRecyclerViewInterface {
    void onClick(int position, List<VoidReason> voidReasons, List<VoidTransactionModel> voidTransactionModelList);
}
