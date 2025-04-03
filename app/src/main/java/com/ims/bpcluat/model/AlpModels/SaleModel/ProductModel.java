package com.ims.bpcluat.model.AlpModels.SaleModel;

public class ProductModel {
    private String effectiveFrom;
    private String productCategory;
    private String productID;
    private String productName;
    private String productType;
    private String sequenceID;


    public ProductModel(String effectiveFrom, String productCategory, String productID, String productName, String productType, String sequenceID) {
        this.effectiveFrom = effectiveFrom;
        this.productCategory = productCategory;
        this.productID = productID;
        this.productName = productName;
        this.productType = productType;
        this.sequenceID = sequenceID;
    }

    public ProductModel() {
        // Required empty public constructor
    }

    public String getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(String effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public String getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(String productCategory) {
        this.productCategory = productCategory;
    }

    public String getProductID() {
        return productID;
    }

    public void setProductID(String productID) {
        this.productID = productID;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getSequenceID() {
        return sequenceID;
    }

    public void setSequenceID(String sequenceID) {
        this.sequenceID = sequenceID;
    }
}

