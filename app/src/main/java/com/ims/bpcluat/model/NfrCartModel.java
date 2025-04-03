package com.ims.bpcluat.model;

public class NfrCartModel {
    String productName, price, qty;

    public NfrCartModel(String productName, String price, String qty) {
        this.productName = productName;
        this.price = price;
        this.qty = qty;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getQty() {
        return qty;
    }

    public void setQty(String qty) {
        this.qty = qty;
    }
}
