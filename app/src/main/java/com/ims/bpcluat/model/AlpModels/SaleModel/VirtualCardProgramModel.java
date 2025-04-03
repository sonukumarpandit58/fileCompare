package com.ims.bpcluat.model.AlpModels.SaleModel;
import java.io.Serializable;
import java.util.List;
public class VirtualCardProgramModel implements Serializable {
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
    private String txnId;
    private String mobNo;
    private String roCode;
    private String dateTime;
    private String txnType;
    private String appVersion;
    private List<ProgramOutput> output;
    private String instId;
    private String latitude;
    private String longitude;
    private String geotagRange;
    private String hwSrNo;

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getReqDate() { return reqDate; }
    public void setReqDate(String reqDate) { this.reqDate = reqDate; }

    public String getReqTime() { return reqTime; }
    public void setReqTime(String reqTime) { this.reqTime = reqTime; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public String getRespCode() { return respCode; }
    public void setRespCode(String respCode) { this.respCode = respCode; }

    public String getResDate() { return resDate; }
    public void setResDate(String resDate) { this.resDate = resDate; }

    public String getResTime() { return resTime; }
    public void setResTime(String resTime) { this.resTime = resTime; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getMid() { return mid; }
    public void setMid(String mid) { this.mid = mid; }

    public String getTid() { return tid; }
    public void setTid(String tid) { this.tid = tid; }

    public String getClient() { return client; }
    public void setClient(String client) { this.client = client; }

    public String getRespDesc() { return respDesc; }
    public void setRespDesc(String respDesc) { this.respDesc = respDesc; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTxnId() { return txnId; }
    public void setTxnId(String txnId) { this.txnId = txnId; }

    public String getMobNo() { return mobNo; }
    public void setMobNo(String mobNo) { this.mobNo = mobNo; }

    public String getRoCode() { return roCode; }
    public void setRoCode(String roCode) { this.roCode = roCode; }

    public String getDateTime() { return dateTime; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }

    public String getTxnType() { return txnType; }
    public void setTxnType(String txnType) { this.txnType = txnType; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public List<ProgramOutput> getOutput() { return output; }
    public void setOutput(List<ProgramOutput> output) { this.output = output; }

    public String getInstId() { return instId; }
    public void setInstId(String instId) { this.instId = instId; }

    public String getLatitude() { return latitude; }
    public void setLatitude(String latitude) { this.latitude = latitude; }

    public String getLongitude() { return longitude; }
    public void setLongitude(String longitude) { this.longitude = longitude; }

    public String getGeotagRange() { return geotagRange; }
    public void setGeotagRange(String geotagRange) { this.geotagRange = geotagRange; }

    public String getHwSrNo() { return hwSrNo; }
    public void setHwSrNo(String hwSrNo) { this.hwSrNo = hwSrNo; }
}
