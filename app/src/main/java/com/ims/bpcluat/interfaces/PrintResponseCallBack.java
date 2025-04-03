package com.ims.bpcluat.interfaces;

public interface PrintResponseCallBack {
    void merchantPrintNo();
    void merchantPrintYes();
    void customerPrintNo();
    void customerPrintYes();
    void fuelBillPrintNo();
    void fuelBillPrintYes();
    void merchantPrintError(String errorResponse);
    void customerPrintError(String errorResponse);
}
