package com.ims.bpcluat.model.AlpModels.VoidModels;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.List;

public class AlpTxnModel implements Parcelable {
    private String channel;
    private String reqDate;
    private String reqTime;
    private String response;
    private String respCode;
    private String resDate;
    private String resTime;
    private String userName;
    private String mid;
    private String tid;
    private String client;
    private String respDesc;
    private String id;
    private String mobNo;
    private String txnId;
    private String roCode;
    private String dateTime;
    private String txnType;
    private String password;
    private List<Object> operatorDetail;
    private List<Object> result;
    private List<BillerTran> billerTranList;
    private String instId;
    private List<Output> output;

    public AlpTxnModel() {
    }

    public String getMobNo() {
        return mobNo;
    }

    public void setMobNo(String mobNo) {
        this.mobNo = mobNo;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getReqDate() {
        return reqDate;
    }

    public void setReqDate(String reqDate) {
        this.reqDate = reqDate;
    }

    public String getReqTime() {
        return reqTime;
    }

    public void setReqTime(String reqTime) {
        this.reqTime = reqTime;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getRespCode() {
        return respCode;
    }

    public void setRespCode(String respCode) {
        this.respCode = respCode;
    }

    public String getResDate() {
        return resDate;
    }

    public void setResDate(String resDate) {
        this.resDate = resDate;
    }

    public String getResTime() {
        return resTime;
    }

    public void setResTime(String resTime) {
        this.resTime = resTime;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

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

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getRespDesc() {
        return respDesc;
    }

    public void setRespDesc(String respDesc) {
        this.respDesc = respDesc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTxnId() {
        return txnId;
    }

    public void setTxnId(String txnId) {
        this.txnId = txnId;
    }

    public String getRoCode() {
        return roCode;
    }

    public void setRoCode(String roCode) {
        this.roCode = roCode;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<Object> getOperatorDetail() {
        return operatorDetail;
    }

    public void setOperatorDetail(List<Object> operatorDetail) {
        this.operatorDetail = operatorDetail;
    }

    public List<Object> getResult() {
        return result;
    }

    public void setResult(List<Object> result) {
        this.result = result;
    }

    public List<BillerTran> getBillerTranList() {
        return billerTranList;
    }

    public void setBillerTranList(List<BillerTran> billerTranList) {
        this.billerTranList = billerTranList;
    }

    public String getInstId() {
        return instId;
    }

    public void setInstId(String instId) {
        this.instId = instId;
    }

    public List<Output> getOutput() {
        return output;
    }

    public void setOutput(List<Output> output) {
        this.output = output;
    }


    //NEW PARsABLE data

    protected AlpTxnModel(Parcel in) {
        channel = in.readString();
        reqDate = in.readString();
        reqTime = in.readString();
        response = in.readString();
        respCode = in.readString();
        resDate = in.readString();
        resTime = in.readString();
        userName = in.readString();
        mid = in.readString();
        tid = in.readString();
        mobNo = in.readString();
        client = in.readString();
        respDesc = in.readString();
        id = in.readString();
        txnId = in.readString();
        roCode = in.readString();
        dateTime = in.readString();
        txnType = in.readString();
        password = in.readString();


        operatorDetail = in.readArrayList(Object.class.getClassLoader());
        result = in.readArrayList(Object.class.getClassLoader());
//        billerTranList = in.createTypedArrayList(BillerTran.CREATOR);
        instId = in.readString();
        output = in.createTypedArrayList(Output.CREATOR);
    }


    public static final Creator<AlpTxnModel> CREATOR = new Creator<AlpTxnModel>() {
        @Override
        public AlpTxnModel createFromParcel(Parcel in) {
            return new AlpTxnModel(in);
        }

        @Override
        public AlpTxnModel[] newArray(int size) {
            return new AlpTxnModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int i) {
        dest.writeString(channel);
        dest.writeString(reqDate);
        dest.writeString(reqTime);
        dest.writeString(response);
        dest.writeString(respCode);
        dest.writeString(resDate);
        dest.writeString(resTime);
        dest.writeString(userName);
        dest.writeString(mid);
        dest.writeString(tid);
        dest.writeString(mobNo);
        dest.writeString(client);
        dest.writeString(respDesc);
        dest.writeString(id);
        dest.writeString(txnId);
        dest.writeString(roCode);
        dest.writeString(dateTime);
        dest.writeString(txnType);
        dest.writeString(password);
        // Write lists to Parcel if needed (this requires the lists to be Parcelable or Serializable)
        dest.writeList(operatorDetail);
        dest.writeList(result);
//        dest.writeTypedList(billerTranList);
        dest.writeString(instId);
        dest.writeTypedList(output);
    }
}

