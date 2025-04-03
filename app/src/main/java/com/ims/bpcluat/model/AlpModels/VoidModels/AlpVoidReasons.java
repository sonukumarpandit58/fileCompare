package com.ims.bpcluat.model.AlpModels.VoidModels;

public class AlpVoidReasons {
    private String reason;
    private String reasonID;

    public AlpVoidReasons(String reason, String reasonID) {
        this.reason = reason;
        this.reasonID = reasonID;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReasonID() {
        return reasonID;
    }

    public void setReasonID(String reasonID) {
        this.reasonID = reasonID;
    }
}
