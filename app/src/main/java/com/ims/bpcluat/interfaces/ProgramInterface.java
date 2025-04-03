package com.ims.bpcluat.interfaces;

import com.ims.bpcluat.model.AlpModels.SaleModel.VirtualCardProgramModel;

import java.util.List;

public interface ProgramInterface {
    void onClickProgram(int position, List<VirtualCardProgramModel> virtualCardProgramModelList);
}
