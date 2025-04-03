package com.ims.bpcluat.interfaces;

import com.ims.bpcluat.model.AlpModels.SaleModel.VirtualCardProgramModel;

import java.util.List;

public interface WalletListInterface {
      void walletClick(int position, List<VirtualCardProgramModel> virtualCardProgramModelList);
}
