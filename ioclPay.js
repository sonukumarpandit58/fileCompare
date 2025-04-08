function fetchVoucherApi() {
  var userName = localStorage.getItem("userName");
  var mid = JSON.parse(localStorage.getItem("tidSetting")).fdMID;
  var tid = JSON.parse(localStorage.getItem("tidSetting")).fdTID;
  var ioclPayVoucher = localStorage.getItem("ioclPayVoucher");
  var txnId = localStorage.getItem("txnupdateid");
  var amt = parseFloat(localStorage.getItem("amt")).toFixed(2);
  var tranDate = ("0" + new Date().getDate()).slice(-2) + ("0" + (new Date().getMonth() + 1)).slice(-2) + new Date().getFullYear();
  var tranTime = returnReqTime(new Date());
  var fetchVoucherUrl = baseUrl + "fetchVoucher";

  localStorage.setItem("ioclPayTranDate", tranDate);
  localStorage.setItem("ioclPayTranTime", tranTime);

  var fetchVoucherRequest = {
    source: "TERMINAL",
    channel: "IOCL",
    reqDate: returnReqDate(new Date()), //current date
    reqTime: returnReqTime(new Date()), //current time
    userName: userName,
    mid: mid,
    tid: tid,
    billerTranList: [
      {
        mid: mid,
        tid: tid,
        rrn: ioclPayVoucher,
        ft_number: txnId,
        tran_amt: amt,
        tran_date: tranDate,
        tran_time: tranTime,
      },
    ],
  };
  console.log("fetch Voucher Url = " + fetchVoucherUrl);
  console.log("fetch Voucher Request = ", JSON.stringify(fetchVoucherRequest));

  try {
    window.plugins.a920.jwt(fetchVoucherUrl,fetchVoucherRequest,"",20,
      function (data) {
        console.log("data = " + data);
        if (data == "403") {
          localStorage.setItem("ioclPaymentFailedMessage", "Server Timeout");
          window.location = "../../payment-fail.html";
          return;
        } else if (data == "404") {
          localStorage.setItem("ioclPaymentFailedMessage","Server Unavailable");
          window.location = "../../payment-fail.html";
          return;
        }
        else {
          var jsondata = data;
          jsondata = jsondata.replace('"{', "{");
          jsondata = jsondata.replace('}"', "}");
          console.log("fetch Voucher Response =" + jsondata);
          jsondata = JSON.parse(jsondata);
          if (jsondata.nameValuePairs.PAYLOAD.respCode == "200") {
            var voucherAmount = jsondata.nameValuePairs.PAYLOAD.output[0].VoucherAmount;
            localStorage.setItem("voucherAmount",voucherAmount);

            var rewardBalance = localStorage.getItem("rewardBalance");
            if(rewardBalance){
              voucherRedemption();
            }else{
              xtraRewardProfileFetch(ioclPayMobile, function () {
              });
            }
            
          } else {
            localStorage.setItem("ioclPaymentFailedMessage",jsondata.nameValuePairs.PAYLOAD.respDesc);
            window.location = "../../payment-fail.html";
            return;
          }
        }
      },
      function (err) {
        console.log("error = " + err);
        window.location = "../../payment-fail.html";
        return;
      }
    );
  } catch (err) {
    console.log("catch block error = " + err);
    window.location = "../../payment-fail.html";
    return;
  }
}

function xtraRewardProfileFetch(ioclPayMobile, callback) {
  var salutation = "";
  var firstname = "";
  var lastname = "";
  var mobNo = ioclPayMobile;
  var vehNo = "";
  var duNo = localStorage.getItem("selectedPumpNo").split("- ")[1];
  var nozzleNo = localStorage.getItem("selectedNozzleNo").split("- ")[1];
  var userName = localStorage.getItem("userName");
  var sapCode = localStorage.getItem("sapCode");
  var isOffline = localStorage.getItem("isOffline");
  if (isOffline == "YES") {
    nozzleNo = localStorage.getItem("ioclNozzleNumber");
  }
  var profileUrl = baseUrl + "itpsFetchConsumerProfileV2";
  var profileReq = {
    source: "Mobile",
    channel: "IOCL",
    reqDate: giveDateInyyyymmdd(new Date()),
    reqTime: giveTimeInhhmmss(new Date()),
    userName: userName,
    mobNo: mobNo,
    vehNo: vehNo,
    tagType: "",
    tagId: "",
    roCode: sapCode,
    nozNo: nozzleNo,
    pumpNo: duNo,
    screenName: "",
  };
  console.log("XtraRewards Request = ", JSON.stringify(profileReq));
  window.plugins.a920.jwt(
    profileUrl,
    profileReq,
    "",
    timeout,
    function (data) {
      console.log("XR Response = " + data);
      //  callback(); //wait for notification response
    },
    function (err) {}
  );
  callback(); //not wait for notification response
  voucherRedemption();
}

function voucherRedemption() {
  var id = localStorage.getItem("id");
  var mid = JSON.parse(localStorage.getItem("tidSetting")).fdMID;
  var tid = JSON.parse(localStorage.getItem("tidSetting")).fdTID;
  var isOffline = localStorage.getItem("isOffline");
  var modeOfRequest = ""; // online or offline
  var userName = localStorage.getItem("userName");
  var tranDate =
    ("0" + new Date().getDate()).slice(-2) +
    ("0" + (new Date().getMonth() + 1)).slice(-2) +
    new Date().getFullYear();
  var tranTime = returnReqTime(new Date());
  localStorage.setItem("ioclPayTranDate", tranDate);
  localStorage.setItem("ioclPayTranTime", tranTime);
  var amt = parseFloat(localStorage.getItem("amt")).toFixed(2);
  var voucherAmount = localStorage.getItem("voucherAmount");

  if(parseFloat(voucherAmount) >= parseFloat(amt)){
    amt = amt;
  }else{
    amt = voucherAmount;
  }

  var ioclPayVoucher = localStorage.getItem("ioclPayVoucher");
  var sapCode = localStorage.getItem("sapCode");
  var ioclPayMobile = localStorage.getItem("ioclPayMobile");
  var pumpNo = localStorage.getItem("selectedPumpNo").split("- ")[1];
  var nozzleNo = "";
  var voucherRedemptionUrl = baseUrl + "voucherRedemptionTxn";
  var txnId = localStorage.getItem("txnupdateid");
  var fccDatetime = localStorage.getItem("fccDatetime");
  var productName = returnProductCode(localStorage.getItem("ProductName"));
  var qty = localStorage.getItem("qty");
  if (isOffline == "YES") {
    id = "";
    nozzleNo = localStorage.getItem("ioclNozzleNumber");
    fccDatetime = "";
    qty = "";
    modeOfRequest = "Offline";
  } else {
    modeOfRequest = "Online";
    nozzleNo = localStorage.getItem("selectedNozzleNo").split("- ")[1];
  }
  var voucherRedemptionRequest = {
    txnId: "", // Send it as blank
    id: id, // id coming for the online txn, blank for offline txn
    channel: "IOCL", //pass IOCL
    reqDate: returnReqDate(new Date()), //current date
    reqTime: returnReqTime(new Date()), //current time
    userName: userName, //operator username
    txnType: "VCU", //VCU for voucher redemption, VCR for reversal
    mid: mid, //MID of Terminal
    tid: tid, //TID of terminal
    billerTranList: [
      {
        mid: mid, //MID of Terminal
        tid: tid, //TID of Terminal
        trans_type: "PURCHASE", //PURCHASE
        trans_status: "PENDING", //PENDING
        tran_amt: amt, //txn amount
        tran_date: tranDate, //txn date for online, current date for offline
        tran_time: tranTime, //txn date for online, current date for offline
        rrn: ioclPayVoucher, //Send Voucher Code
        ft_number: txnId, // txn id
        session_id: txnId, //txn id
        cust_id: userName, //operator username
        pay_method: "IOCLPay", // Voucher
        field1: modeOfRequest, //ONLINE for online txn, OFFLINE for offline txn
        field2: "", //blank
        field3: "", //blank
        field4: "", //blank
        field5: "", //blank
        field6: "", //blank
        field7: "", //blank
        field8: "", //blank
        field9: "", //blank
        field10: "", //blank
        field11: "", //blank
        field12: "", //blank
        field13: "", //blank
        field14: "", //blank
        field15: "", //blank
        paramList: [
          {
            param: sapCode, //SAPCODE
            param_lit: "SAP CODE",
          },
          {
            param: ioclPayMobile, //Mobile number of customer, given in IOCL Pay screen
            param_lit: "Customer Mobile",
          },
          {
            param: pumpNo, //Pump number
            param_lit: "PUMP_NO",
          },
          {
            param: nozzleNo, //Nozzle Number
            param_lit: "NOZZLE",
          },
          {
            param: productName, //Product short code like HS, MS, etc.
            param_lit: "PROD_NAME",
          },
          {
            param: qty, // QUANTITY as per ONLINE TXN, blank for Offline txn
            param_lit: "QUANTITY",
          },
          {
            param: fccDatetime, //Online txn time, blank for offline
            param_lit: "FCC TIMESTAMP",
          },
        ],
      },
    ],
  };
  console.log("voucherRedemptionUrl = " + voucherRedemptionUrl);
  console.log(
    "voucherRedemption Request = ",
    JSON.stringify(voucherRedemptionRequest)
  );
  try {
    window.plugins.a920.jwt(
      voucherRedemptionUrl,
      voucherRedemptionRequest,
      "",
      5,
      function (data) {
        console.log("data = " + data);
        if (data == "403") {
          localStorage.setItem("ioclPaymentFailedMessage", "Server Timeout");
          voucherRedemptionReversal("", function () {});
          window.location = "../../payment-fail.html";
          return;
        } else if (data == "404") {
          localStorage.setItem("ioclPaymentFailedMessage","Server Unavailable");
          //   errormessage(404, "Server Unavailable");
          window.location = "../../payment-fail.html";
          return;
        }
        var jsondata = data;
        jsondata = jsondata.replace('"{', "{");
        jsondata = jsondata.replace('}"', "}");
        console.log("voucherRedemption Response =" + jsondata);
        jsondata = JSON.parse(jsondata);
        if (jsondata.nameValuePairs.PAYLOAD.respCode == "408") {
          voucherRedemptionReversal("", function () {});
          window.location = "../../payment-fail.html";
        }
        else if (jsondata.nameValuePairs.PAYLOAD.respCode !== "200") {
          localStorage.setItem("ioclPaymentFailedMessage",jsondata.nameValuePairs.PAYLOAD.respDesc);
          window.location = "../../payment-fail.html";
        } else {
          var ioclPayMobile = localStorage.getItem("ioclPayMobile");
          localStorage.setItem("custMob",ioclPayMobile);
          var splitTxnCalled = "";

          if(parseFloat(localStorage.getItem("voucherAmount")) >= parseFloat(localStorage.getItem("amt"))){
            splitTxnCalled = "no";
            localStorage.setItem("splitTxnCalled",splitTxnCalled);
          }else{
            splitTxnCalled = "yes";
            localStorage.setItem("splitFirstPayment","IOCLPay");
            localStorage.setItem("splitTxnCalled",splitTxnCalled);
            localStorage.setItem("splitFirstTxnAmt",voucherAmount);
          }
 
          if(splitTxnCalled == "yes"){
            window.location.href = "../../splitPayment.html";
          }else{
            window.location.href = "ioclPayPaymentSuccess.html";
          }
          
        }
      },
      function (err) {
        console.log("error = " + err);
        voucherRedemptionReversal("", function () {});
        window.location='../../payment-fail.html';
      }
    );
  } catch (err) {
    console.log("catch block error = " + err);
    voucherRedemptionReversal("", function () {});
    window.location='../../payment-fail.html';
  }
}

function voucherRedemptionReversal(vcr) {
  var id = localStorage.getItem("id");
  var mid = JSON.parse(localStorage.getItem("tidSetting")).fdMID;
  var tid = JSON.parse(localStorage.getItem("tidSetting")).fdTID;
  var isOffline = localStorage.getItem("isOffline");
  var modeOfRequest = ""; // online or offline
  var userName = localStorage.getItem("userName");
  var tranDate = localStorage.getItem("ioclPayTranDate");
  var tranTime = localStorage.getItem("ioclPayTranTime");
  var amt = parseFloat(localStorage.getItem("amt")).toFixed(2);
  var voucherAmount = localStorage.getItem("voucherAmount");

  if(parseFloat(voucherAmount) >= parseFloat(amt)){
    amt = amt;
  }else{
    amt = voucherAmount;
  }
  var ioclPayVoucher = localStorage.getItem("ioclPayVoucher");
  var sapCode = localStorage.getItem("sapCode");
  var ioclPayMobile = localStorage.getItem("ioclPayMobile");
  var pumpNo = localStorage.getItem("selectedPumpNo").split("- ")[1];
  var nozzleNo = localStorage.getItem("selectedNozzleNo").split("- ")[1];
  var voucherRedemptionUrl = baseUrl + "voucherRedemptionTxn";
  var txnId = localStorage.getItem("txnupdateid");
  var fccDatetime = localStorage.getItem("fccDatetime");
  var productName = returnProductCode(localStorage.getItem("ProductName"));
  var qty = localStorage.getItem("qty");
  if (isOffline == "YES") {
    id = "";
    nozzleNo = localStorage.getItem("ioclNozzleNumber");
    fccDatetime = "";
    qty = "";
    modeOfRequest = "Offline";
  } else {
    modeOfRequest = "Online";
  }
  var voucherRedemptionRequest = {
    txnId: "", // Send it as blank
    id: id, // id coming for the online txn, blank for offline txn
    channel: "IOCL", //pass IOCL
    reqDate: returnReqDate(new Date()), //current date
    reqTime: returnReqTime(new Date()), //current time
    userName: userName, //operator username
    txnType: "VCR", //VCU for voucher redemption, VCR for reversal
    mid: mid, //MID of Terminal
    tid: tid, //TID of terminal
    billerTranList: [
      {
        mid: mid, //MID of Terminal
        tid: tid, //TID of Terminal
        trans_type: "PURCHASE", //PURCHASE
        trans_status: "PENDING", //PENDING
        tran_amt: amt, //txn amount
        tran_date: tranDate, //txn date for online, current date for offline
        tran_time: tranTime, //txn date for online, current date for offline
        rrn: ioclPayVoucher, //Send Voucher Code
        ft_number: txnId, // txn id
        session_id: txnId, //txn id
        cust_id: userName, //operator username
        pay_method: "IOCLPay", // Voucher
        field1: modeOfRequest, //ONLINE for online txn, OFFLINE for offline txn
        field2: "", //blank
        field3: "", //blank
        field4: "", //blank
        field5: "", //blank
        field6: "", //blank
        field7: "", //blank
        field8: "", //blank
        field9: "", //blank
        field10: "", //blank
        field11: "", //blank
        field12: "", //blank
        field13: "", //blank
        field14: "", //blank
        field15: "", //blank
        paramList: [
          {
            param: sapCode, //SAPCODE
            param_lit: "SAP CODE",
          },
          {
            param: ioclPayMobile, //Mobile number of customer, given in IOCL Pay screen
            param_lit: "Customer Mobile",
          },
          {
            param: pumpNo, //Pump number
            param_lit: "PUMP_NO",
          },
          {
            param: nozzleNo, //Nozzle Number
            param_lit: "NOZZLE",
          },
          {
            param: productName, //Product short code like HS, MS, etc.
            param_lit: "PROD_NAME",
          },
          {
            param: qty, // QUANTITY as per ONLINE TXN, blank for Offline txn
            param_lit: "QUANTITY",
          },
          {
            param: fccDatetime, //Online txn time, blank for offline
            param_lit: "FCC TIMESTAMP",
          },
        ],
      },
    ],
  };
  console.log("voucherRedemptionUrl = " + voucherRedemptionUrl);
  console.log(
    "voucherRedemptionReversal Request = ",
    JSON.stringify(voucherRedemptionRequest)
  );
  try {
    window.plugins.a920.jwt(
      voucherRedemptionUrl,
      voucherRedemptionRequest,
      "",
      20,
      function (data) {
        console.log("Reversal Response = " + data);
      },
      function (err) {
        window.location = "../../payment-fail.html";
      }
    );
  } catch (err) {
    console.log("catch block error = " + err);
    window.location = "../../payment-fail.html";
  }
}
