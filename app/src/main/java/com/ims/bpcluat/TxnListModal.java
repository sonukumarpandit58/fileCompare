package com.ims.bpcluat;

public class TxnListModal {
    String qty, product,amt;

    public TxnListModal(String qty, String product, String amt) {
        this.qty = qty;
        this.product = product;
        this.amt = amt;
    }

    public String getQty() {
        return qty;
    }

    public void setQty(String qty) {
        this.qty = qty;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getAmt() {
        return amt;
    }

    public void setAmt(String amt) {
        this.amt = amt;
    }
}
