package com.ims.bpcluat.model.AlpModels.SaleModel;

import java.io.Serializable;

public class ProgramWallet implements Serializable {
    private String walletId;
    private String walletName;

    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }

    public String getWalletName() { return walletName; }
    public void setWalletName(String walletName) { this.walletName = walletName; }
}
