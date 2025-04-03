package com.ims.bpcluat.interfaces;

import com.ims.bpcluat.model.AlpModels.VoidModels.AlpTxnModel;

import java.util.List;

public interface AlpVoidTransactionsInterface {
    void onClick(int position, List<AlpTxnModel> alpTxnModelList);
}
