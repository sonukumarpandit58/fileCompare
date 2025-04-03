package com.ims.bpcluat.interfaces;

import com.ims.bpcluat.model.AlpModels.SaleModel.Program;
import com.ims.bpcluat.model.AlpModels.SaleModel.VirtualCardProgramModel;

import java.util.List;

public interface BalanceEnquiryInterface {
   void onClick(int position, List<Program> programs);
}
