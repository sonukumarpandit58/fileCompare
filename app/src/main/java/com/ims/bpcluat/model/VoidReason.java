package com.ims.bpcluat.model;

public class VoidReason {
    private String voidCode;
    private String voidReasonText;

    // Constructor
    public VoidReason(String voidCode, String voidReasonText) {
        this.voidCode = voidCode;
        this.voidReasonText = voidReasonText;
    }

    public String getVoidCode() {
        return voidCode;
    }

    public void setVoidCode(String voidCode) {
        this.voidCode = voidCode;
    }

    public String getVoidReasonText() {
        return voidReasonText;
    }

    public void setVoidReasonText(String voidReasonText) {
        this.voidReasonText = voidReasonText;
    }
}

