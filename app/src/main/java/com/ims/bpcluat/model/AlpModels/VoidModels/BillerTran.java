package com.ims.bpcluat.model.AlpModels.VoidModels;

import java.util.List;

public class BillerTran {
    private String mid;
    private String tid;
    private String ft_number;
    private List<Object> paramList;

    public String getMid() {
        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getFt_number() {
        return ft_number;
    }

    public void setFt_number(String ft_number) {
        this.ft_number = ft_number;
    }

    public List<Object> getParamList() {
        return paramList;
    }

    public void setParamList(List<Object> paramList) {
        this.paramList = paramList;
    }
}
