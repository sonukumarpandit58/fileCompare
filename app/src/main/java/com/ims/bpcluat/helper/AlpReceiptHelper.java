package com.ims.bpcluat.helper;

import static com.ims.bpcluat.Helper.cashChargeslipDate;
import static com.ims.bpcluat.Helper.cashChargeslipTime;
import static com.ims.bpcluat.Helper.city;
import static com.ims.bpcluat.Helper.dealerContactNumber;
import static com.ims.bpcluat.Helper.mobileNumberMasking;
import static com.ims.bpcluat.Helper.roName;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.txnAmountUpToTwoDecimal;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.ImageDecoder;
import com.ims.bpcluat.R;
import com.ims.bpcluat.interfaces.PrintResponseCallBack;
import com.pax.dal.IDAL;
import com.pax.dal.IPrinter;
import com.pax.dal.exceptions.PrinterDevException;
import com.pax.gl.page.IPage;
import com.pax.gl.page.PaxGLPage;
import com.pax.neptunelite.api.NeptuneLiteUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AlpReceiptHelper {

    ProgressDialog progress;

    public static IPrinter printer;
    public static IDAL dal;
    public static IPrinter a920printer;
    private PrintResponseCallBack callback;
    int dstWidth = 120;
    int dstHeight = 140;

    public AlpReceiptHelper() {
    }

    public void setCallback(PrintResponseCallBack callback) {
        this.callback = callback;
    }

    public Bitmap alpVoidSlip(final Activity mActivity, JSONObject jsonObject, String printType, String mobileNum) {
        try {
            PaxGLPage iPaxGLPage;
            final int FONT_BIG = 28;
            final int FONT_22 = 22;
            final int FONT_24 = 24;
            final int FONT_26 = 26;
            final int FONT_NORMAL = 20;
            final int FONT_BIGEST = 40;
            int FONT_SMALL = 18;
            int EXTRA_FONT_SMALL = 15;
            int LINE_SPACE = 10;
            iPaxGLPage = PaxGLPage.getInstance(mActivity);
            IPage page = iPaxGLPage.createPage();
            IPage.ILine.IUnit unit = page.createUnit();
            page.adjustLineSpace(-6);

            JSONArray outputArray = jsonObject.getJSONArray("output");
            JSONObject outputObject = outputArray.getJSONObject(0);

            String attendentName = Helper.operatorFirstName + " " + Helper.operatorLastName;
            String reportType = jsonObject.getString("reportType");
            String ROName = outputObject.getString("ROName");
            String roCity = outputObject.getString("roCity");
            String roMobileNo = outputObject.getString("roMobileNo");
            String timestamp = outputObject.getString("timestamp");
            String aposTerminalID = outputObject.getString("aposTerminalID");
            String txnId = outputObject.getString("alpTransactionId");
            String orgId = outputObject.getString("originalAlpTransactionId");
            String chargeSlipNumber = outputObject.getString("chargeSlipNumber");
            String reportID = outputObject.getString("reportID");
            String txnType = outputObject.getString("txnType");
            String chargeSlipHeader = outputObject.getString("chargeSlipHeader");
            String chargeSlipFooter = outputObject.getString("chargeSlipFooter");
            String customerAccountNumber = outputObject.getString("customerAccountNumber");
            String customerCardNumber = outputObject.getString("customerCardNumber");
            String customerName = outputObject.getString("customerName");
            String noOfRequestedCard = outputObject.getString("noOfRequestedCard");
            String vehicleNumber = outputObject.getString("vehicleNumber");
            String paymentReferenceNumber = outputObject.getString("paymentReferenceNumber");
            String amountPaid = outputObject.getString("amountPaid");
            String txnQuantity = outputObject.getString("txnQuantity");
            String txnMEShare = outputObject.getString("txnMEShare");

            String txnProduct = outputObject.getString("txnProduct");
            String productRate = outputObject.getString("productRate");

            String currencyCode = outputObject.getString("currencyCode");
            String programName = outputObject.getString("programName");
            String txnMode = outputObject.getString("txnMode");
            String txnSource = outputObject.getString("txnSource");
            String txnStatus = outputObject.getString("txnStatus");

            unit.setAlign(IPage.EAlign.CENTER);
            unit.setText("Customer Receipt");

            ImageDecoder decoder = new ImageDecoder(mActivity);
            Bitmap headerBitmap = decoder.decodeImage(R.drawable.bpcl);
            Bitmap headerLogo = Bitmap.createScaledBitmap(headerBitmap, dstWidth, dstHeight, true);
            page.addLine().addUnit(headerLogo, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit(ROName + "(" + sapCode + ")", FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(roCity, FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(roMobileNo, FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit(chargeSlipHeader, FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);


            page.addLine().addUnit("TID : ", FONT_SMALL, (float) 4).addUnit(aposTerminalID, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Txn id : ", FONT_SMALL, (float) 4).addUnit(txnId, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("OrgID : ", FONT_SMALL, (float) 4).addUnit(orgId, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Date & Time", FONT_SMALL, (float) 4).addUnit(timestamp, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("SLIP No : ", FONT_SMALL, (float) 4).addUnit(chargeSlipNumber, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

//            page.addLine().addUnit("Report Type : ", FONT_SMALL, (float) 4).addUnit(reportType, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
//            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Report ID : ", FONT_SMALL, (float) 4).addUnit(reportID, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Txn Type : ", FONT_SMALL, (float) 4).addUnit(txnType, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Txn Mode : ", FONT_SMALL, (float) 4).addUnit(txnSource, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("CUST NAME : ", FONT_SMALL, (float) 4).addUnit(customerName, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Acc. No. : ", FONT_SMALL, (float) 4).addUnit(customerAccountNumber, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("CARD ID. : ", FONT_SMALL, (float) 4).addUnit(customerCardNumber, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Veh/Card : ", FONT_SMALL, (float) 4).addUnit(vehicleNumber, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);


            if (mobileNum != null && !mobileNum.isEmpty()) {
                page.addLine().addUnit("Mobile No : ", FONT_SMALL, (float) 4).addUnit(mobileNum, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }


            page.addLine().addUnit("Program Name : ", FONT_SMALL, (float) 4).addUnit(programName, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Wallet : ", FONT_SMALL, (float) 4).addUnit(txnMode, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Product : ", FONT_SMALL, (float) 4).addUnit(txnProduct, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Rate : ", FONT_SMALL, (float) 4).addUnit("₹ " + txnAmountUpToTwoDecimal(productRate), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

//            page.addLine().addUnit("Curr : ", FONT_SMALL, (float) 4).addUnit(currencyCode, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
//            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Vol in Ltrs : ", FONT_SMALL, (float) 4).addUnit(txnQuantity, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Amount : ", FONT_SMALL, (float) 4).addUnit("₹ " + txnAmountUpToTwoDecimal(amountPaid), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);


            if (!printType.equals("CUSTOMER COPY")) {
                page.addLine().addUnit("ME Share : ", FONT_SMALL, (float) 4).addUnit(txnMEShare, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

//            page.addLine().addUnit("Txn Status : ", FONT_SMALL, (float) 4).addUnit(txnStatus, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);

//            page.addLine().addUnit(chargeSlipFooter, FONT_NORMAL, IPage.EAlign.CENTER);
//            page.addLine().addUnit(" ", LINE_SPACE);
//            page.addLine().addUnit(" ", LINE_SPACE);
//            page.addLine().addUnit(" ", LINE_SPACE);
//            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("I AGREE TO PAY AS PER CARD ISSUER AGREEEMENT", EXTRA_FONT_SMALL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(Helper.footerMessage, EXTRA_FONT_SMALL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit(printType, FONT_24, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Version : " + Helper.version, FONT_SMALL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().adjustTopSpace(10);
            Bitmap footerBitmap = decoder.decodeImage(R.drawable.footer_logo);
            Bitmap footerLogo = Bitmap.createScaledBitmap(footerBitmap, 144, 85, true);
            page.addLine().addUnit(footerLogo, IPage.EAlign.CENTER);

            page.addLine().adjustTopSpace(50);
            int width = 384;

            Bitmap bitmap = iPaxGLPage.pageToBitmap(page, width);
            return bitmap;
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public Bitmap reprintSlip(final Activity mActivity, JSONObject jsonObject, String printType) {
        try {
            PaxGLPage iPaxGLPage;
            final int FONT_BIG = 28;
            final int FONT_22 = 22;
            final int FONT_24 = 24;
            final int FONT_26 = 26;
            final int FONT_NORMAL = 20;
            final int FONT_MIDDLE = 26;
            final int FONT_BIGEST = 40;
            int FONT_SMALL = 18;
            int EXTRA_FONT_SMALL = 15;
            int LINE_SPACE = 10;
            iPaxGLPage = PaxGLPage.getInstance(mActivity);
            IPage page = iPaxGLPage.createPage();
            IPage.ILine.IUnit unit = page.createUnit();
            page.adjustLineSpace(-6);

            String attendentName = Helper.operatorFirstName + " " + Helper.operatorLastName;
            String ROName = jsonObject.getString("ROName");
            String roCity = jsonObject.getString("roCity");
            String roMobileNo = jsonObject.getString("roMobileNo");
            String timestamp = jsonObject.getString("timestamp");

            String chargeSlipNumber = jsonObject.getString("chargeSlipNumber");
            String reportID = jsonObject.getString("reportID");
            String txnType = jsonObject.getString("txnType");
            String txnSource = jsonObject.getString("txnSource");
            String customerName = jsonObject.getString("customerName");
            String customerAccountNumber = jsonObject.getString("customerAccountNumber");
            String customerCardNumber = jsonObject.getString("customerCardNumber");
            String vehicleNumber = jsonObject.getString("vehicleNumber");
            String odometerReading = jsonObject.getString("odometerReading");
            String txnMode = jsonObject.getString("txnMode");
            String txnProduct = jsonObject.getString("txnProduct");
            String productRate = jsonObject.getString("productRate");
            String txnQuantity = jsonObject.getString("txnQuantity");
            String amountPaid = jsonObject.getString("amountPaid");
            String txnMEShare = jsonObject.getString("txnMEShare");

            String alpTransactionId = jsonObject.getString("alpTransactionId");
            String originalAlpTransactionId = jsonObject.getString("originalAlpTransactionId");
            String dealerID = jsonObject.getString("dealerID");
            String mobileNumber = jsonObject.getString("mobileNumber");
            if (!mobileNumber.isEmpty()) {
                mobileNumber = mobileNumberMasking(mobileNumber);
            }
            String discount = jsonObject.getString("discount");
            String fuelAmount = jsonObject.getString("fuelAmount");
            String petroMilesEarned = jsonObject.getString("petroMilesEarned");
            String txnDiscount = jsonObject.getString("txnDiscount");
            String txnStatus = jsonObject.getString("txnStatus");
            String clientTxnId = jsonObject.getString("clientTxnId");
            String programName = jsonObject.getString("programName");
            String cardBalance = jsonObject.getString("cardBalance");
            String voided = jsonObject.getString("voided");

            String chargeSlipHeader = jsonObject.getString("chargeSlipHeader");
            String chargeSlipFooter = jsonObject.getString("chargeSlipFooter");
            String merchantDisclaimer = jsonObject.getString("merchantDisclaimer");
            String customerDisclaimer = jsonObject.getString("customerDisclaimer");

            String aposTerminalID = jsonObject.getString("aposTerminalID");
            String netAmount = jsonObject.getString("netAmount");
            String txnBayId = jsonObject.getString("txnBayId");
            String tcsAmount = jsonObject.getString("tcsAmount");
            String txnAmount = jsonObject.getString("txnAmount");


            String mobNo = jsonObject.getString("mobNo");
            String date = jsonObject.getString("date");
            String time = jsonObject.getString("time");
            String txnId = jsonObject.getString("txnId");
            String attendantName = Helper.operatorFirstName + " " + Helper.operatorLastName;
            if (txnType.equals("SALES")) {
                txnType = "ALP Sale";
            }
//            String amt = jsonObject.getString("amt");
//            String dateTime = jsonObject.getString("dateTime");

            unit.setAlign(IPage.EAlign.CENTER);
            unit.setText("Customer Receipt");

            ImageDecoder decoder = new ImageDecoder(mActivity);
            Bitmap headerBitmap = decoder.decodeImage(R.drawable.bpcl);
            Bitmap headerLogo = Bitmap.createScaledBitmap(headerBitmap, dstWidth, dstHeight, true);
            page.addLine().addUnit(headerLogo, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit(ROName + "(" + sapCode + ")", FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit(roCity, FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit(roMobileNo, FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Duplicate Copy", FONT_NORMAL, IPage.EAlign.CENTER);

            page.addLine().addUnit(" ", LINE_SPACE);


            if (printType.equals("POS SLIP")) {
                page.addLine().addUnit("Date : ", FONT_SMALL, (float) 4).addUnit(date, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit("Time : ", FONT_SMALL, (float) 4).addUnit(time, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);

                if (txnProduct.toLowerCase().contains("lubes")) {
                    page.addLine().addUnit("Product : ", FONT_SMALL, (float) 4).addUnit("NFR", FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                }else{
                    page.addLine().addUnit("Product : ", FONT_SMALL, (float) 4).addUnit(txnProduct, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                }


                page.addLine().addUnit("PayMode : ", FONT_SMALL, (float) 4).addUnit(txnType, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit("Txn Id : ", FONT_SMALL, (float) 4).addUnit(txnId, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit("Attendant : ", FONT_SMALL, (float) 4).addUnit(attendantName, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);

                if (!txnProduct.toLowerCase().contains("lubes")) {
                    page.addLine().addUnit("Rate/Ltr : ", FONT_SMALL, (float) 4).addUnit("₹ " + txnAmountUpToTwoDecimal(productRate), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                    page.addLine().addUnit("Volume(Ltr.) : ", FONT_SMALL, (float) 4).addUnit(txnQuantity, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                }
                page.addLine().addUnit("Amount(Rs.) : ", FONT_SMALL, (float) 4).addUnit("₹ " + txnAmountUpToTwoDecimal(amountPaid), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit("VechNo : ", FONT_SMALL, (float) 4).addUnit(vehicleNumber, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
                if (!mobileNumber.isEmpty()) {
                    page.addLine().addUnit("Mobile No : ", FONT_SMALL, (float) 4).addUnit(mobileNumber, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                }

            } else {

                page.addLine().addUnit("TID : ", FONT_SMALL, (float) 4).addUnit(aposTerminalID, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);

                page.addLine().addUnit("TxnID : ", FONT_SMALL, (float) 4).addUnit(alpTransactionId, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);

                if (txnType.toLowerCase().contains("void")) {
                    if (!originalAlpTransactionId.isEmpty()) {
                        page.addLine().addUnit("OrgID : ", FONT_SMALL, (float) 4).addUnit(originalAlpTransactionId, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                        page.addLine().addUnit(" ", LINE_SPACE);
                    }
                }


                page.addLine().addUnit("Date & Time : ", FONT_SMALL, (float) 4).addUnit(timestamp, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);


                page.addLine().addUnit("SLIP No : ", FONT_SMALL, (float) 7).addUnit(chargeSlipNumber, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit("Report ID : ", FONT_SMALL, (float) 4).addUnit(reportID, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit("Txn Type : ", FONT_SMALL, (float) 4).addUnit(txnType, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);


                page.addLine().addUnit("Txn Mode : ", FONT_SMALL, (float) 4).addUnit(txnSource, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);

//                if (!txnType.equals("SALE")) {
//                    page.addLine().addUnit("DealerID : ", FONT_SMALL, (float) 4).addUnit(dealerID, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
//                    page.addLine().addUnit(" ", LINE_SPACE);
//                    page.addLine().addUnit("Txn Status : ", FONT_SMALL, (float) 4).addUnit(txnStatus, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
//                    page.addLine().addUnit(" ", LINE_SPACE);
//                }
                page.addLine().addUnit("CUST NAME : ", FONT_SMALL, (float) 4).addUnit(customerName, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit("Acc. No. : ", FONT_SMALL, (float) 4).addUnit(customerAccountNumber, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit("CARD ID: ", FONT_SMALL, (float) 4).addUnit(customerCardNumber, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);


                page.addLine().addUnit("Veh/Card : ", FONT_SMALL, (float) 4).addUnit(vehicleNumber, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);

                if (!mobileNumber.isEmpty()) {
                    if (!txnType.equals("SALE")) {
                        page.addLine().addUnit("Mobile No : ", FONT_SMALL, (float) 4).addUnit(mobileNumber, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                        page.addLine().addUnit(" ", LINE_SPACE);
                    }
                }
                if (!txnType.toLowerCase().contains("void")) {
                    if (!odometerReading.isEmpty()) {
                        page.addLine().addUnit("Odometer : ", FONT_SMALL, (float) 4).addUnit(odometerReading, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                        page.addLine().addUnit(" ", LINE_SPACE);
                    }
                }

                if (!txnType.equals("SALE")) {
                    page.addLine().addUnit("Program Name : ", FONT_SMALL, (float) 4).addUnit(programName, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                }

                if (!txnType.toLowerCase().contains("void")) {
                    if (!printType.equals("MERCHANT COPY")) {
                        page.addLine().addUnit("Card Bal : ", FONT_SMALL, (float) 4).addUnit("₹ " + txnAmountUpToTwoDecimal(cardBalance), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                        page.addLine().addUnit(" ", LINE_SPACE);
                    }
                }
                page.addLine().addUnit("Wallet : ", FONT_SMALL, (float) 4).addUnit(txnMode, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);


                page.addLine().addUnit("Product : ", FONT_SMALL, (float) 4).addUnit(txnProduct, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);

                page.addLine().addUnit("Rate : ", FONT_SMALL, (float) 4).addUnit("₹ " + txnAmountUpToTwoDecimal(productRate), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);

                page.addLine().addUnit("Vol in Ltrs: ", FONT_SMALL, (float) 4).addUnit(txnQuantity, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);


                if (!txnType.equals("SALE")) {
                    page.addLine().addUnit("Amount : ", FONT_SMALL, (float) 4).addUnit("₹ " + txnAmountUpToTwoDecimal(amountPaid), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                }

                if (!txnType.toLowerCase().contains("void")) {

                    if (!txnType.equals("SALE")) {
                        page.addLine().addUnit("Discount : ", FONT_SMALL, (float) 4).addUnit(discount, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                        page.addLine().addUnit(" ", LINE_SPACE);
                    }
                    page.addLine().addUnit("Fuel Amount : ", FONT_SMALL, (float) 4).addUnit("₹ " + txnAmountUpToTwoDecimal(fuelAmount), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                    if (!txnType.equals("SALE")) {
                        page.addLine().addUnit("Net Amount : ", FONT_SMALL, (float) 4).addUnit("₹ " + txnAmountUpToTwoDecimal(netAmount), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                        page.addLine().addUnit(" ", LINE_SPACE);
                        page.addLine().addUnit("Txn BayId : ", FONT_SMALL, (float) 4).addUnit(txnBayId, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                        page.addLine().addUnit(" ", LINE_SPACE);
                    }
                    page.addLine().addUnit("Tcs Amount : ", FONT_SMALL, (float) 4).addUnit("₹ " + txnAmountUpToTwoDecimal(tcsAmount), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                    page.addLine().addUnit("Txn Amount : ", FONT_SMALL, (float) 4).addUnit("₹ " + txnAmountUpToTwoDecimal(txnAmount), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);

                    if (!petroMilesEarned.isEmpty()) {
                        page.addLine().addUnit("PMs Earn : ", FONT_SMALL, (float) 4).addUnit(petroMilesEarned, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                        page.addLine().addUnit(" ", LINE_SPACE);
                    }

                }

                if (!printType.equals("CUSTOMER COPY")) {
                    page.addLine().addUnit("ME Share : ", FONT_SMALL, (float) 4).addUnit("₹ " + txnAmountUpToTwoDecimal(txnMEShare), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                }

                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit(" ", LINE_SPACE);

//            page.addLine().addUnit(chargeSlipFooter, FONT_NORMAL, IPage.EAlign.CENTER);
//            page.addLine().addUnit(" ", LINE_SPACE);
//            page.addLine().addUnit(" ", LINE_SPACE);
//            page.addLine().addUnit(" ", LINE_SPACE);
//            page.addLine().addUnit(" ", LINE_SPACE);

                if (printType.equals("MERCHANT COPY")) {
                    page.addLine().addUnit(" ", LINE_SPACE);
                    page.addLine().addUnit(" ", LINE_SPACE);
                    page.addLine().addUnit(" ", LINE_SPACE);
                    page.addLine().addUnit(merchantDisclaimer, FONT_NORMAL, IPage.EAlign.CENTER);
                } else if (printType.equals("CUSTOMER COPY")) {
                    page.addLine().addUnit(customerDisclaimer, FONT_NORMAL, IPage.EAlign.CENTER);
                }
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit(" ", LINE_SPACE);

            }

            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("I AGREE TO PAY AS PER CARD ISSUER AGREEEMENT", EXTRA_FONT_SMALL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(Helper.footerMessage, EXTRA_FONT_SMALL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit(printType, FONT_24, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Version : " + Helper.version, FONT_SMALL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().adjustTopSpace(10);

            page.addLine().adjustTopSpace(5);
            Bitmap footerBitmap = decoder.decodeImage(R.drawable.footer_logo);
            Bitmap footerLogo = Bitmap.createScaledBitmap(footerBitmap, 144, 85, true);
            page.addLine().addUnit(footerLogo, IPage.EAlign.CENTER);

            page.addLine().adjustTopSpace(50);
            int width = 384;

            Bitmap bitmap = iPaxGLPage.pageToBitmap(page, width);
            return bitmap;
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public Bitmap shiftSummarySlip(final Activity mActivity, JSONObject jsonObject, String printType, String mobileNum, String summary) {
        try {
            PaxGLPage iPaxGLPage;
            final int FONT_BIG = 28;
            final int FONT_22 = 22;
            final int FONT_24 = 24;
            final int FONT_26 = 26;
            final int FONT_NORMAL = 20;
            final int FONT_BIGEST = 40;
            int FONT_SMALL = 18;
            int EXTRA_FONT_SMALL = 15;
            int LINE_SPACE = 10;
            iPaxGLPage = PaxGLPage.getInstance(mActivity);
            IPage page = iPaxGLPage.createPage();
            IPage.ILine.IUnit unit = page.createUnit();
            page.adjustLineSpace(-6);

            // Parse JSON data

            String tid = jsonObject.getString("tid");
            String mid = jsonObject.getString("mid");
            String reportType = jsonObject.getString("reportType");

            JSONArray outputArray = jsonObject.getJSONArray("output");
            JSONObject outputArrayJSONObject = outputArray.getJSONObject(0);
            String reportId = outputArrayJSONObject.getString("reportId");
            String roName = outputArrayJSONObject.getString("roName");
            String roAddress = outputArrayJSONObject.getString("roAddress");
            String roMobileNo = outputArrayJSONObject.getString("roMobileNo");
            String dealerID = outputArrayJSONObject.getString("dealerID");
            String shiftStart = outputArrayJSONObject.getString("shiftStart");
            String shiftEnd = outputArrayJSONObject.getString("shiftEnd");
            String terminalID = outputArrayJSONObject.getString("terminalID");

            JSONObject payableSummaryObject = outputArrayJSONObject.getJSONObject("payableSummary");
            String payableByDealer = payableSummaryObject.getString("payableByDealer");
            String receivableByDealer = payableSummaryObject.getString("receivableByDealer");
            String netReceivable = payableSummaryObject.getString("netReceivable");
            String receivableDealerCredit = payableSummaryObject.getString("receivableDealerCredit");
            String meShare = payableSummaryObject.getString("meShare");

            JSONArray productWiseSummaryArray = new JSONArray();
            if (outputArrayJSONObject.has("productWiseSummary")) {
                productWiseSummaryArray = outputArrayJSONObject.getJSONArray("productWiseSummary");
            } else {
            }

            JSONArray transactionsArray = new JSONArray();
            if (outputArrayJSONObject.has("transactions")) {
                transactionsArray = outputArrayJSONObject.getJSONArray("transactions");
            } else {
            }

            JSONArray walletWiseSummaryArray = new JSONArray();
            if (outputArrayJSONObject.has("walletWiseSummary")) {
                walletWiseSummaryArray = outputArrayJSONObject.getJSONArray("walletWiseSummary");
            } else {
            }


            ImageDecoder decoder = new ImageDecoder(mActivity);
            Bitmap headerBitmap = decoder.decodeImage(R.drawable.bpcl);
            Bitmap headerLogo = Bitmap.createScaledBitmap(headerBitmap, dstWidth, dstHeight, true);
            page.addLine().addUnit(headerLogo, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

//            String attendentName = Helper.operatorFirstName + " " + Helper.operatorLastName;
//            if (!attendentName.isEmpty()) {
//                page.addLine().addUnit("Attendant Name: " + attendentName, FONT_SMALL, IPage.EAlign.LEFT);
//                page.addLine().addUnit(" ", LINE_SPACE);
//            }

            page.addLine().addUnit(roName + "(" + sapCode + ")", FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit(roAddress, FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit(roMobileNo, FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);

            if (summary.equals("duplicate")) {
                page.addLine().addUnit("Duplicate Copy", FONT_NORMAL, IPage.EAlign.CENTER);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            page.addLine().addUnit("TID: ", FONT_SMALL, (float) 4).addUnit(terminalID, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("ReportID: ", FONT_SMALL, (float) 4).addUnit(reportId, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Date & Time: ", FONT_SMALL, (float) 4).addUnit(cashChargeslipDate() + " " + cashChargeslipTime(), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("DealerID: ", FONT_SMALL, (float) 4).addUnit(dealerID, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Mobile No: ", FONT_SMALL, (float) 4).addUnit(mobileNum, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Shift Start: ", FONT_SMALL, (float) 4).addUnit(shiftStart, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Shift End: ", FONT_SMALL, (float) 4).addUnit(shiftEnd, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("----------------------------------------------------------", FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit("SHIFT SUMMARY", FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit("----------------------------------------------------------", FONT_NORMAL, IPage.EAlign.CENTER);

            page.addLine().addUnit("Payable by Dealer: ", FONT_SMALL, (float) 4).addUnit(" ₹" + txnAmountUpToTwoDecimal(payableByDealer), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Receivable by Dealer: ", FONT_SMALL, (float) 6).addUnit(" ₹" + txnAmountUpToTwoDecimal(receivableByDealer), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Net Receivable: ", FONT_SMALL, (float) 4).addUnit(" ₹" + txnAmountUpToTwoDecimal(netReceivable), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Receivable Dealer Credit: ", FONT_SMALL, (float) 8).addUnit(" ₹" + txnAmountUpToTwoDecimal(receivableDealerCredit), FONT_SMALL, IPage.EAlign.RIGHT, (float) 4);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Me Share: ", FONT_SMALL, (float) 4).addUnit(" ₹" + txnAmountUpToTwoDecimal(meShare), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("----------------------------------------------------------", FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit("PRODUCT-WISE SUMMARY", FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit("----------------------------------------------------------", FONT_NORMAL, IPage.EAlign.CENTER);

            for (int i = 0; i < productWiseSummaryArray.length(); i++) {
                JSONObject product = productWiseSummaryArray.getJSONObject(i);

                if (product.has("productName") && product.has("productSaleAmount")) {
                    String productName = product.getString("productName");
                    String productSaleAmount = product.getString("productSaleAmount");

                    page.addLine().addUnit(productName + ":", FONT_SMALL, (float) 7).addUnit(" ₹" + txnAmountUpToTwoDecimal(productSaleAmount), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                    page.addLine().addUnit(" ", LINE_SPACE);
                    page.addLine().addUnit(" ", LINE_SPACE);
                }
            }
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("----------------------------------------------------------", FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit("WALLET-WISE SUMMARY", FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit("----------------------------------------------------------", FONT_NORMAL, IPage.EAlign.CENTER);
            for (int i = 0; i < walletWiseSummaryArray.length(); i++) {
                JSONObject wallet = walletWiseSummaryArray.getJSONObject(i);

                String walletValue =  txnAmountUpToTwoDecimal(wallet.getString("amount"));

                page.addLine().addUnit("Txn Type: ", FONT_SMALL, (float) 4).addUnit(wallet.getString("txnType"), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit("Txn Mode: ", FONT_SMALL, (float) 4).addUnit(wallet.getString("txnMode"), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit("Amount: ", FONT_SMALL, (float) 4).addUnit("₹" + txnAmountUpToTwoDecimal(walletValue), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit("Me Share: ", FONT_SMALL, (float) 4).addUnit(" ₹" + txnAmountUpToTwoDecimal(wallet.getString("meShare")), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit("Count: ", FONT_SMALL, (float) 4).addUnit(wallet.getString("count"), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit(" ", LINE_SPACE);
                // Add a separator line
                page.addLine().addUnit("----------------------------------------------------------", FONT_NORMAL, IPage.EAlign.CENTER);
                page.addLine().addUnit(" ", LINE_SPACE);

            }


            page.addLine().addUnit(" ", LINE_SPACE);

            if (!reportType.equals("summary")) {
                page.addLine().addUnit("----------------------------------------------------------", FONT_NORMAL, IPage.EAlign.CENTER);
                page.addLine().addUnit("DETAIL REPORT", FONT_NORMAL, IPage.EAlign.CENTER);
                page.addLine().addUnit("----------------------------------------------------------", FONT_NORMAL, IPage.EAlign.CENTER);

                for (int i = 0; i < transactionsArray.length(); i++) {
                    JSONObject transaction = transactionsArray.getJSONObject(i);
                    String txnAmount =  txnAmountUpToTwoDecimal(transaction.getString("amount"));

                    page.addLine().addUnit("CUST NAME: ", +FONT_SMALL, (float) 4).addUnit(transaction.getString("accountName"), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                    page.addLine().addUnit("Acc No: ", +FONT_SMALL, (float) 4).addUnit(transaction.getString("accountNumber"), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                    page.addLine().addUnit("Me Share: ", +FONT_SMALL, (float) 4).addUnit(" ₹" + txnAmountUpToTwoDecimal(transaction.getString("meShare")), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                    page.addLine().addUnit("SLIP No: ", +FONT_SMALL, (float) 6).addUnit(transaction.getString("chargeSlipNumber"), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                    page.addLine().addUnit("TxnID: ", +FONT_SMALL, (float) 4).addUnit(transaction.getString("alpTransactionId"), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                    page.addLine().addUnit("Amount: ", FONT_SMALL, (float) 4).addUnit(" ₹" + txnAmountUpToTwoDecimal(txnAmount), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);


                    if (transaction.has("batchNumber")) {
                        String batchNumber = transaction.getString("batchNumber");
                        if (!batchNumber.isEmpty()) {
                            page.addLine().addUnit("Batch Number: ", +FONT_SMALL, (float) 4).addUnit(batchNumber, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                            page.addLine().addUnit(" ", LINE_SPACE);
                        }
                    }

                    if (transaction.has("cardNumber")) {
                        String cardNumber = transaction.getString("cardNumber");
                        if (!cardNumber.isEmpty()) {
                            page.addLine().addUnit("Card Number: ", +FONT_SMALL, (float) 4).addUnit(cardNumber, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                            page.addLine().addUnit(" ", LINE_SPACE);
                        }
                    }

                    page.addLine().addUnit("Date: ", +FONT_SMALL, (float) 4).addUnit(transaction.getString("date"), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);

                    if (transaction.has("meShare")) {
                        int meShared = transaction.getInt("meShare");
                        if (meShared > 0) {
                            page.addLine().addUnit("Me Share: ", FONT_SMALL, (float) 4).addUnit(" ₹" + txnAmountUpToTwoDecimal(String.valueOf(meShared)), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                            page.addLine().addUnit(" ", LINE_SPACE);
                        }
                    }

                    if (transaction.has("noOfCards")) {
                        String noOfCards = transaction.getString("noOfCards");
                        if (!noOfCards.isEmpty()) {
                            page.addLine().addUnit("No of Cards: ", +FONT_SMALL, (float) 4).addUnit(noOfCards, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                            page.addLine().addUnit(" ", LINE_SPACE);
                        }
                    }

                    if (transaction.has("posting")) {
                        boolean posting = transaction.getBoolean("posting");
                        page.addLine().addUnit("Posting: ", +FONT_SMALL, (float) 4).addUnit((posting ? "Yes" : "No"), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                        page.addLine().addUnit(" ", LINE_SPACE);
                    }


                    if (transaction.has("product")) {
                        String product = transaction.getString("product");
                        if (!product.isEmpty()) {
                            page.addLine().addUnit("Product: ", +FONT_SMALL, (float) 4).addUnit(product, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                            page.addLine().addUnit(" ", LINE_SPACE);
                        }
                    }

                    page.addLine().addUnit("Transaction Mode: ", +FONT_SMALL, (float) 4).addUnit(transaction.getString("txnMode"), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                    page.addLine().addUnit("Transaction Type: ", +FONT_SMALL, (float) 4).addUnit(transaction.getString("txnType"), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                    // Add vehicle number (Check if it's not empty)
                    String vehicleNumber = transaction.getString("vehicleNumber");
//                if (!vehicleNumber.isEmpty()) {
                    page.addLine().addUnit("Vehicle Number: ", +FONT_SMALL, (float) 4).addUnit(vehicleNumber, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
//                }

                    // Add a separator line
                    page.addLine().addUnit("----------------------------------------------------------", FONT_NORMAL, IPage.EAlign.CENTER);
                    page.addLine().addUnit(" ", LINE_SPACE);
                }
            }


            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("I AGREE TO PAY AS PER CARD ISSUER AGREEEMENT", EXTRA_FONT_SMALL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(Helper.footerMessage, EXTRA_FONT_SMALL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit(printType, FONT_24, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Version : " + Helper.version, FONT_SMALL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().adjustTopSpace(10);

            Bitmap footerBitmap = decoder.decodeImage(R.drawable.footer_logo);
            Bitmap footerLogo = Bitmap.createScaledBitmap(footerBitmap, 144, 85, true);
            page.addLine().addUnit(footerLogo, IPage.EAlign.CENTER);
            page.addLine().adjustTopSpace(50);

            int width = 384;
            Bitmap bitmap = iPaxGLPage.pageToBitmap(page, width);
            return bitmap;
        } catch (JSONException ex) {
            Log.e("JSONException", "JSON Parsing error: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            Log.e("Exception", "Error occurred: " + ex.getMessage(), ex);
        }
        return null;
    }

    public Bitmap balanceInquirySlip(final Activity mActivity, JSONObject jsonObject, String printType) {
        try {
            PaxGLPage iPaxGLPage;
            final int FONT_BIG = 28;
            final int FONT_22 = 22;
            final int FONT_24 = 24;
            final int FONT_26 = 26;
            final int FONT_NORMAL = 20;
            final int FONT_MIDDLE = 20;
            final int FONT_BIGEST = 40;
            int FONT_SMALL = 18;
            int EXTRA_FONT_SMALL = 15;
            int LINE_SPACE = 10;
            iPaxGLPage = PaxGLPage.getInstance(mActivity);
            IPage page = iPaxGLPage.createPage();
            IPage.ILine.IUnit unit = page.createUnit();
            page.adjustLineSpace(-6);


            JSONArray outputArray = jsonObject.getJSONArray("output");
            JSONObject outputObject = outputArray.getJSONObject(0);

            JSONArray balanceDetailsArray = outputObject.getJSONArray("balanceDetails");

            JSONArray billerTranListArray = jsonObject.getJSONArray("billerTranList");
            JSONObject billerTran = billerTranListArray.getJSONObject(0);
            String programId = billerTran.getString("field13");
            String field14 = billerTran.getString("field14");
            String field15 = billerTran.getString("field15");


            String attendentName = Helper.operatorFirstName + " " + Helper.operatorLastName;
            String txnId = jsonObject.getString("txnId");

            String aposTerminalID = "";
            if (outputObject.has("aposTerminalID")) {
                aposTerminalID = outputObject.getString("aposTerminalID");
            }

            String chargeSlipHeader = outputObject.getString("chargeSlipHeader");
            String chargeSlipFooter = outputObject.getString("chargeSlipFooter");
            String customerMobileNumber = outputObject.getString("customerMobileNumber");
            String dealerID = outputObject.getString("dealerID");
            String txnSource = outputObject.getString("txnSource");

            String ROName = outputObject.getString("roName");
            String roCity = outputObject.getString("roCity");
            String roMobileNo = outputObject.getString("roMobileNumber");
            String timestamp = outputObject.getString("timestamp");

//            String chargeSlipNumber = jsonObject.getString("chargeSlipNumber");
//            String reportID = jsonObject.getString("reportID");

            String txnType = outputObject.getString("txnType");
            String txnMode = outputObject.getString("txnSource");

            String customerName = "";
            String customerAccountNumber = "";
            String customerCardNumber = "";
            String walletBalance = "";
            String currencyCode = "";


            JSONObject balanceDetail = balanceDetailsArray.getJSONObject(0);
            String programName = balanceDetail.getString("programName");
            customerName = balanceDetail.getString("customerName");
            customerAccountNumber = balanceDetail.getString("customerAccountNumber");
            customerCardNumber = balanceDetail.getString("customerCardNumber");
            walletBalance = balanceDetail.getString("walletBalance");
            currencyCode = balanceDetail.getString("currencyCode");

            unit.setAlign(IPage.EAlign.CENTER);
            unit.setText("Customer Receipt");

            ImageDecoder decoder = new ImageDecoder(mActivity);
            Bitmap headerBitmap = decoder.decodeImage(R.drawable.bpcl);
            Bitmap headerLogo = Bitmap.createScaledBitmap(headerBitmap, dstWidth, dstHeight, true);
            page.addLine().addUnit(headerLogo, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit(ROName + "(" + sapCode + ")", FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit(roCity, FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit(roMobileNo, FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit(chargeSlipHeader, FONT_NORMAL, IPage.EAlign.CENTER);

            page.addLine().addUnit("TID : ", FONT_SMALL, (float) 4).addUnit(tid, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
//            page.addLine().addUnit("Txn id : ", FONT_SMALL, (float) 4).addUnit(txnId, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
//            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Date & Time", FONT_SMALL, (float) 4).addUnit(timestamp, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Txn Type : ", FONT_SMALL, (float) 4).addUnit(txnType, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Txn Mode : ", FONT_SMALL, (float) 4).addUnit(txnMode, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);


            page.addLine().addUnit("CUST NAME : ", FONT_SMALL, (float) 4).addUnit(customerName, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Acc. No. : ", FONT_SMALL, (float) 4).addUnit(customerAccountNumber, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("CARD ID: ", FONT_SMALL, (float) 4).addUnit(customerCardNumber, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);


            page.addLine().addUnit("Prog Name : ", FONT_SMALL, (float) 4).addUnit(programName, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
//                page.addLine().addUnit("Curr : ", FONT_SMALL, (float) 4).addUnit(currencyCode, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
//                page.addLine().addUnit(" ", LINE_SPACE);


            page.addLine().addUnit("Crd Bal : ", FONT_SMALL, (float) 4).addUnit("₹ " + txnAmountUpToTwoDecimal(walletBalance), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);

            //page.addLine().addUnit(chargeSlipFooter, FONT_NORMAL, IPage.EAlign.CENTER);

            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);

//                if (!dealerID.isEmpty()) {
//                    page.addLine().addUnit("DealerID : ", FONT_SMALL, (float) 4).addUnit(dealerID, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
//                    page.addLine().addUnit(" ", LINE_SPACE);
//                }
//
//                page.addLine().addUnit("Mobile No : ", FONT_SMALL, (float) 4).addUnit(customerMobileNumber, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
//                page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("I AGREE TO PAY AS PER CARD ISSUER AGREEEMENT", EXTRA_FONT_SMALL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(Helper.footerMessage, EXTRA_FONT_SMALL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);

//            page.addLine().addUnit(printType, FONT_24, IPage.EAlign.CENTER);
//            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Version : " + Helper.version, FONT_SMALL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().adjustTopSpace(10);

            page.addLine().adjustTopSpace(5);
            Bitmap footerBitmap = decoder.decodeImage(R.drawable.footer_logo);
            Bitmap footerLogo = Bitmap.createScaledBitmap(footerBitmap, 144, 85, true);
            page.addLine().addUnit(footerLogo, IPage.EAlign.CENTER);

            page.addLine().adjustTopSpace(50);
            int width = 384;

            Bitmap bitmap = iPaxGLPage.pageToBitmap(page, width);
            return bitmap;
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public Bitmap enrollmentSlip(final Activity mActivity, JSONObject jsonObject, String printType, String mobileNumber) {
        try {
            PaxGLPage iPaxGLPage;
            final int FONT_BIG = 28;
            final int FONT_22 = 22;
            final int FONT_24 = 24;
            final int FONT_26 = 26;
            final int FONT_NORMAL = 20;
            final int FONT_MIDDLE = 20;
            final int FONT_BIGEST = 40;
            int FONT_SMALL = 18;
            int EXTRA_FONT_SMALL = 15;
            int LINE_SPACE = 10;
            iPaxGLPage = PaxGLPage.getInstance(mActivity);
            IPage page = iPaxGLPage.createPage();
            IPage.ILine.IUnit unit = page.createUnit();
            page.adjustLineSpace(-6);


            JSONArray outputArray = jsonObject.getJSONArray("output");
            JSONObject outputObject = outputArray.getJSONObject(0);

//            JSONArray balanceDetailsArray = outputObject.getJSONArray("balanceDetails");

//            JSONArray billerTranListArray = jsonObject.getJSONArray("billerTranList");
//            JSONObject billerTran = billerTranListArray.getJSONObject(0);


            String attendentName = Helper.operatorFirstName + " " + Helper.operatorLastName;
//            String mobNo = jsonObject.getString("mobNo");

            String ROName = outputObject.getString("ROName");
            String roCity = outputObject.getString("roCity");
            String roMobileNo = outputObject.getString("roMobileNo");
            String timestamp = outputObject.getString("timestamp");

            String aposTerminalID = "";
            if (outputObject.has("aposTerminalID")) {
                aposTerminalID = outputObject.getString("aposTerminalID");
            }

            String txnId = outputObject.getString("alpTransactionId");
            String chargeSlipNumber = outputObject.getString("chargeSlipNumber");
            String reportID = outputObject.getString("reportID");
            String txnType = outputObject.getString("txnType");
            String chargeSlipHeader = outputObject.getString("chargeSlipHeader");
            String chargeSlipFooter = outputObject.getString("chargeSlipFooter");
            String customerAccountNumber = outputObject.getString("customerAccountNumber");
            String customerName = outputObject.getString("customerName");
            String noOfRequestedCard = outputObject.getString("noOfRequestedCard");
            String paymentReferenceNumber = outputObject.getString("paymentReferenceNumber");
            String amountPaid = outputObject.getString("amountPaid");
            String currencyCode = outputObject.getString("currencyCode");
            String programName = outputObject.getString("programName");
            String txnMode = outputObject.getString("txnMode");
            String txnStatus = outputObject.getString("txnStatus");


            unit.setAlign(IPage.EAlign.CENTER);
            unit.setText("Customer Receipt");

            ImageDecoder decoder = new ImageDecoder(mActivity);
            Bitmap headerBitmap = decoder.decodeImage(R.drawable.bpcl);
            Bitmap headerLogo = Bitmap.createScaledBitmap(headerBitmap, dstWidth, dstHeight, true);
            page.addLine().addUnit(headerLogo, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit(ROName + "(" + sapCode + ")", FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(roCity, FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(roMobileNo, FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit(chargeSlipHeader, FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);


            page.addLine().addUnit("TID : ", FONT_SMALL, (float) 4).addUnit(tid, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Txn id : ", FONT_SMALL, (float) 4).addUnit(txnId, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Date & Time", FONT_SMALL, (float) 4).addUnit(timestamp, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("SLIP No : ", FONT_SMALL, (float) 4).addUnit(chargeSlipNumber, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Report ID : ", FONT_SMALL, (float) 4).addUnit(reportID, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("TYPE : ", FONT_SMALL, (float) 4).addUnit(txnType, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            if (mobileNumber != null && !mobileNumber.isEmpty()) {
                page.addLine().addUnit("Custom Mob : ", FONT_SMALL, (float) 4).addUnit(mobileNumber, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            page.addLine().addUnit("Acc. No. : ", FONT_SMALL, (float) 4).addUnit(customerAccountNumber, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Cust Name : ", FONT_SMALL, (float) 4).addUnit(customerName, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("No of Cards : ", FONT_SMALL, (float) 4).addUnit(noOfRequestedCard, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("MOP : ", FONT_SMALL, (float) 4).addUnit(txnMode, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Program ID : ", FONT_SMALL, (float) 4).addUnit(programName, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Curr : ", FONT_SMALL, (float) 4).addUnit(currencyCode, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Amount Paid : ", FONT_SMALL, (float) 4).addUnit("₹ " + txnAmountUpToTwoDecimal(amountPaid), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Txn Status : ", FONT_SMALL, (float) 4).addUnit(txnStatus, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);

//            page.addLine().addUnit(chargeSlipFooter, FONT_NORMAL, IPage.EAlign.CENTER);
//            page.addLine().addUnit(" ", LINE_SPACE);
//            page.addLine().addUnit(" ", LINE_SPACE);
//            page.addLine().addUnit(" ", LINE_SPACE);


            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("I AGREE TO PAY AS PER CARD ISSUER AGREEEMENT", EXTRA_FONT_SMALL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(Helper.footerMessage, EXTRA_FONT_SMALL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit(printType, FONT_24, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Version : " + Helper.version, FONT_SMALL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().adjustTopSpace(10);

            page.addLine().adjustTopSpace(5);
            Bitmap footerBitmap = decoder.decodeImage(R.drawable.footer_logo);
            Bitmap footerLogo = Bitmap.createScaledBitmap(footerBitmap, 144, 85, true);
            page.addLine().addUnit(footerLogo, IPage.EAlign.CENTER);

            page.addLine().adjustTopSpace(50);
            int width = 384;

            Bitmap bitmap = iPaxGLPage.pageToBitmap(page, width);
            return bitmap;
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void merchantDialog(final Activity mActivity, Bitmap voidSlip) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        LayoutInflater inflater = mActivity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.print_popup, null);
        builder.setView(dialogView);

        AlertDialog alertDialog = builder.create();
        TextView alertMessage = dialogView.findViewById(R.id.alert_title);
        String alert1 = "Print Merchant Receipt";
        alertMessage.setText(alert1);
        // Customize dialog buttons if needed
        Button noBtn = dialogView.findViewById(R.id.alert_cancel_button);
        Button yesBtn = dialogView.findViewById(R.id.alert_ok_button);

        noBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                callback.merchantPrintNo();
//                  customerDialog(mActivity, jsonObject,"Print Customer Receipt");
            }
        });

        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                printReceipt(mActivity, voidSlip, "MERCHANT COPY");
            }
        });
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    public void customerDialog(final Activity mActivity, Bitmap voidSlip) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        LayoutInflater inflater = mActivity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.print_popup, null);
        builder.setView(dialogView);

        AlertDialog alertDialog = builder.create();
        TextView alertMessage = dialogView.findViewById(R.id.alert_title);
        String alert1 = "Print Customer Receipt";
        alertMessage.setText(alert1);
        // Customize dialog buttons if needed
        Button noBtn = dialogView.findViewById(R.id.alert_cancel_button);
        Button yesBtn = dialogView.findViewById(R.id.alert_ok_button);

        noBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                callback.customerPrintNo();
            }
        });

        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();

                printReceipt(mActivity, voidSlip, "CUSTOMER COPY");
            }
        });
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    public void fuelDialog(final Activity mActivity, Bitmap voidSlip, String fuelType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        LayoutInflater inflater = mActivity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.print_popup, null);
        builder.setView(dialogView);

        AlertDialog alertDialog = builder.create();
        TextView alertMessage = dialogView.findViewById(R.id.alert_title);
        String alert1 = "Print " + fuelType + " Receipt";
        alertMessage.setText(alert1);
        // Customize dialog buttons if needed
        Button noBtn = dialogView.findViewById(R.id.alert_cancel_button);
        Button yesBtn = dialogView.findViewById(R.id.alert_ok_button);

        noBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                callback.fuelBillPrintNo();
            }
        });

        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                progress = new ProgressDialog(mActivity);
                progress.setTitle("Loading");
                progress.setMessage("Wait while loading...");
                progress.setCancelable(false);
                progress.show();
                printReceipt(mActivity, voidSlip, fuelType);
            }
        });
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    public void printReceipt(Activity mActivity, Bitmap voidSlip, String printType) {
        try {
            dal = NeptuneLiteUser.getInstance().getDal(mActivity);
            printer = dal.getPrinter();
            a920printer = dal.getPrinter();
            Log.i("init", "true");
            a920printer.init();
            printer.setGray(450);
            a920printer.spaceSet(Byte.parseByte("1"), Byte.parseByte("0"));
            a920printer.leftIndent(Short.parseShort("0"));
            a920printer.setGray(Integer.parseInt("4"));
            a920printer.invert(false);
            a920printer.step(Integer.parseInt("50"));
            // Bitmap bitmap = iPaxGLPage.pageToBitmap(page, width);
            a920printer.step(Integer.parseInt("10"));
            a920printer.print(voidSlip, new IPrinter.IPinterListener() {
                @Override
                public void onSucc() {
                    Log.d("printLog", "succes");
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (printType.equals("MERCHANT COPY")) {
                                callback.merchantPrintYes();
                            } else if (printType.equals("CUSTOMER COPY")) {
                                callback.customerPrintYes();
                            } else {
                                progress.dismiss();
                                callback.fuelBillPrintYes();
                            }
                        }
                    });
                }

                @Override
                public void onError(int i) {
                    Log.d("printLog", "fail");
                    String errorvalue = ErrorValue(String.valueOf(i));
                    Log.d("printStatus2", errorvalue);
                    if (printType.equals("MERCHANT COPY")) {
                        callback.merchantPrintError(errorvalue);
                    } else {
                        progress.dismiss();
                        callback.customerPrintError(errorvalue);
                    }
                }
            });

        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        } catch (PrinterDevException ex) {
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String ErrorValue(String ErrorStatus) {
        String Result = "0";
        if (ErrorStatus.equals("1")) {
            Result = "Printer is busy(1)";
        } else if (ErrorStatus.equals("2")) {
            Result = "Out of paper, please insert paper roll(2) ";
        } else if (ErrorStatus.equals("3")) {
            Result = "The format of print data packet error(3)";
        } else if (ErrorStatus.equals("4")) {
            Result = "Printer malfunctions(-4) ";
        } else if (ErrorStatus.equals("8")) {
            Result = "Printer over heats(-8) ";
        } else if (ErrorStatus.equals("9")) {
            Result = "Printer voltage is too low(-9) ";
        } else if (ErrorStatus.equals("-16")) {
            Result = "Printing is unfinished(-16) ";
        } else if (ErrorStatus.equals("-6")) {
            Result = "Cut jam error(only support:E500,E800)(-6)";
        }
        // -2000
        else if (ErrorStatus.equals("-5")) {
            Result = "Cover open error(only support:E500,E800)(-5) ";
        } else if (ErrorStatus.equals("-4")) {
            Result = "The printer has not installed font library(-4) ";
        } else if (ErrorStatus.equals("-2")) {
            Result = "Data package is too long(-2) ";
        }
        return Result;
    }

}