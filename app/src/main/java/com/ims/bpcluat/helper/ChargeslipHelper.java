package com.ims.bpcluat.helper;

import static com.ims.bpcluat.Helper.dealerContactNumber;
import static com.ims.bpcluat.Helper.footerMessage;
import static com.ims.bpcluat.Helper.mid;
import static com.ims.bpcluat.Helper.sapCode;
import static com.ims.bpcluat.Helper.tid;
import static com.ims.bpcluat.Helper.txnAmountUpToTwoDecimal;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.ims.bpcluat.Helper;
import com.ims.bpcluat.ImageDecoder;
import com.ims.bpcluat.MainActivity;
import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.interfaces.BluetoothResponseCallback;
import com.ims.bpcluat.interfaces.PrintResponseCallBack;
import com.ims.bpcluat.interfaces.RePrintResponseCallBack;
import com.pax.dal.IDAL;
import com.pax.dal.IPrinter;
import com.pax.dal.exceptions.PrinterDevException;
import com.pax.gl.page.IPage;
import com.pax.gl.page.IPage.EAlign.*;
import com.pax.gl.page.PaxGLPage;
import com.pax.neptunelite.api.NeptuneLiteUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ChargeslipHelper {
    public static IPrinter printer;
    public static IDAL dal;
    public static IPrinter a920printer;
    private PrintResponseCallBack callback;
    private RePrintResponseCallBack rePrintResponseCallBack;
    ProgressDialog progress;
    int dstWidth = 120;
    int dstHeight = 140;

    public ChargeslipHelper() {

    }

    public void setCallback(PrintResponseCallBack callback) {
        this.callback = callback;
    }

    public void setCallbackReprint(RePrintResponseCallBack rePrintResponseCallBack) {
        this.rePrintResponseCallBack = rePrintResponseCallBack;
    }

    public Bitmap chargeslip(final Activity mActivity, JSONObject jsonObject, String printType) {
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

            String agencyName = jsonObject.getString("agencyName");
//            String address = jsonObject.getString("address");
            String city = jsonObject.getString("city");
            String dealerContactNo = jsonObject.getString("dealerContactNo");
            String date = jsonObject.getString("date");
            String time = jsonObject.getString("time");
            String bayNo = jsonObject.getString("bayNo");
            String nozzleNo = jsonObject.getString("nozzleNo");
            String product = jsonObject.getString("product");
            String payMode = jsonObject.getString("payMode");
            String type = "";
            if (jsonObject.has("type")) {
                type = jsonObject.getString("type");
            }

            String txnId = jsonObject.getString("txnId");
            String attendentName = jsonObject.getString("attendentName");
            String txnStart = jsonObject.getString("txnStart");
            String txnEnd = jsonObject.getString("txnEnd");
            String rate = jsonObject.getString("rate");
            String volume = jsonObject.getString("volume");
            String amount = jsonObject.getString("amount");
            String presetType = jsonObject.getString("presetType");
            String presetValue = jsonObject.getString("presetValue");
            String vehicleNo = jsonObject.getString("vehicleNo");
            String mobileNo = jsonObject.getString("mobileNo");

            String batchNo = jsonObject.getString("batchNo");
            String terminalInvoiceNo = jsonObject.getString("terminalInvoiceNo");
            String cardNo = jsonObject.getString("cardNo");
            String authCode = jsonObject.getString("authCode");
            String cardTxnCustomerName = jsonObject.getString("cardTxnCustomerName");

            /* Start NFR Print */
            String nfrTotalAmount = (String) jsonObject.get("nfrTotalAmount");
            String nfrProductName = (String) jsonObject.get("nfrProductName");
            String nfrUnitPrice = (String) jsonObject.get("nfrUnitPrice");
            String nfrVolume = (String) jsonObject.get("nfrVolume");
            String[] nfrProductArray = null;
            String[] nfrUnitPriceArray = null;
            String[] nfrVolumeArray = null;
            if (!nfrTotalAmount.isEmpty()) {
                nfrProductArray = nfrProductName.split(",");
                nfrVolumeArray = nfrVolume.split(",");
                nfrUnitPriceArray = nfrUnitPrice.split(",");
            }
            /* End NFR Print */

            Log.d("TYOPEPEPEE", "chargeslip: " + type);
            if (payMode.equals("ALP")) {
                if (type.equals("PreAuth")) {
                    payMode = "PreAuth";
                } else {
                    payMode = "SALE";
                }
                Log.d("payModeeee", "payMode: " + payMode);
                Log.d("payModeeee1", "payModetype: " + type);

            }

            unit.setAlign(IPage.EAlign.CENTER);
            unit.setText("Customer Receipt");

            ImageDecoder decoder = new ImageDecoder(mActivity);
            Bitmap headerBitmap = decoder.decodeImage(R.drawable.bpcl);
            Bitmap headerLogo = Bitmap.createScaledBitmap(headerBitmap, dstWidth, dstHeight, true);
            page.addLine().addUnit(headerLogo, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            if (!agencyName.isEmpty()) {
                page.addLine().addUnit(agencyName + "(" + sapCode + ")", FONT_SMALL, IPage.EAlign.CENTER);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

//            if (!address.isEmpty()) {
//                page.addLine().addUnit(address, FONT_SMALL, IPage.EAlign.CENTER);
//                page.addLine().addUnit(" ", LINE_SPACE);
//            }

            if (!city.isEmpty()) {
                page.addLine().addUnit(city, FONT_SMALL, IPage.EAlign.CENTER);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!dealerContactNo.isEmpty()) {
                page.addLine().addUnit(dealerContactNo, FONT_SMALL, IPage.EAlign.CENTER);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!payMode.equals("ALP")) {
                page.addLine().addUnit("MID: ", FONT_SMALL, (float) 4).addUnit(mid, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);

                page.addLine().addUnit("TID: ", FONT_SMALL, (float) 4).addUnit(tid, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }
            page.addLine().addUnit("Date: ", FONT_SMALL, (float) 4).addUnit(date, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Time: ", FONT_SMALL, (float) 4).addUnit(time, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            if (!bayNo.isEmpty()) {
                page.addLine().addUnit("BayNo: ", FONT_SMALL, (float) 4).addUnit(bayNo, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!nozzleNo.isEmpty()) {
                page.addLine().addUnit("NozzleNo: ", FONT_SMALL, (float) 4).addUnit(nozzleNo, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!product.isEmpty()) {
                page.addLine().addUnit("Product: ", FONT_SMALL, (float) 4).addUnit(product, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            page.addLine().addUnit("PayMode: ", FONT_SMALL, (float) 4).addUnit(payMode, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            if(payMode.equals("CARD")){
                if (!batchNo.isEmpty()) {
                    page.addLine().addUnit("Batch no: ", FONT_SMALL, (float) 4).addUnit(batchNo, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                }
                if (!terminalInvoiceNo.isEmpty()) {
                    page.addLine().addUnit("Invoice no: ", FONT_SMALL, (float) 4).addUnit(terminalInvoiceNo, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                }
                if (!cardNo.isEmpty()) {
                    page.addLine().addUnit("Card no: ", FONT_SMALL, (float) 4).addUnit(cardNo, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                }
                if (!authCode.isEmpty()) {
                    page.addLine().addUnit("App code:", FONT_SMALL, (float) 4).addUnit(authCode, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                }
//                if (!cardTxnCustomerName.isEmpty()) {
//                    page.addLine().addUnit("Card Holder: ", FONT_SMALL, (float) 4).addUnit(cardTxnCustomerName, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
//                    page.addLine().addUnit(" ", LINE_SPACE);
//                }
            }

            page.addLine().addUnit("Txn Id: ", FONT_SMALL, (float) 4).addUnit(txnId, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            if (!product.isEmpty()) {
                page.addLine().addUnit("Attendant: ", FONT_SMALL, (float) 4).addUnit(attendentName, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!txnStart.isEmpty()) {
                page.addLine().addUnit("TxSt: ", FONT_SMALL, (float) 4).addUnit(txnStart, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!txnEnd.isEmpty()) {
                page.addLine().addUnit("TxEnd: ", FONT_SMALL, (float) 4).addUnit(txnEnd, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!rate.isEmpty()) {
                page.addLine().addUnit("Rate/Ltr.: ", FONT_SMALL, (float) 4).addUnit("₹ " + rate, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!volume.isEmpty()) {
                page.addLine().addUnit("Volume(Ltr.): ", FONT_SMALL, (float) 4).addUnit(volume, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            page.addLine().addUnit("Amount(Rs.): ", FONT_SMALL, (float) 4).addUnit("₹ " + amount, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            if (!presetType.isEmpty()) {
                page.addLine().addUnit("PresetType: ", FONT_SMALL, (float) 4).addUnit(presetType, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!presetValue.isEmpty()) {
                page.addLine().addUnit("Preset Value: ", FONT_SMALL, (float) 4).addUnit(presetValue, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!vehicleNo.isEmpty()) {
                page.addLine().addUnit("VechNo: ", FONT_SMALL, (float) 4).addUnit(vehicleNo, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!mobileNo.isEmpty()) {
                page.addLine().addUnit("MobileNo: ", FONT_SMALL, (float) 4).addUnit(mobileNo, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            /* Start : Only for NFR Print */
            if (!nfrTotalAmount.isEmpty()) {
                page.addLine().addUnit("----------------------------------------------------------------", FONT_NORMAL);
                page.addLine().addUnit("Name", FONT_SMALL, IPage.EAlign.LEFT, (float) 4).addUnit("Quantity", FONT_SMALL, IPage.EAlign.CENTER, (float) 4).addUnit("Amt/Qty", FONT_SMALL, IPage.EAlign.CENTER, (float) 4);
                page.addLine().addUnit("----------------------------------------------------------------", FONT_NORMAL);
                for (int i = 0; i < nfrProductArray.length; i++) {
                    page.addLine().addUnit(nfrProductArray[i], FONT_SMALL, IPage.EAlign.LEFT, (float) 4).addUnit(nfrVolumeArray[i], FONT_SMALL, IPage.EAlign.CENTER, (float) 4).addUnit(nfrUnitPriceArray[i], FONT_SMALL, IPage.EAlign.CENTER, (float) 4);
                    page.addLine().addUnit(" ", LINE_SPACE);
                }
                page.addLine().addUnit("----------------------------------------------------------------", FONT_NORMAL);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit(" ", LINE_SPACE);
            }
            /* End : Only for NFR Print */

            if (payMode.equals("ALP Sale") || payMode.equals("CARD")) {
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit("I AGREE TO PAY AS PER CARD ISSUER AGREEEMENT", EXTRA_FONT_SMALL, IPage.EAlign.CENTER);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(footerMessage, EXTRA_FONT_SMALL, IPage.EAlign.CENTER);
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

    public void txnSummaryPrintDialog(final Activity mActivity, String popUpMsg, Bitmap chargeslip) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        LayoutInflater inflater = mActivity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.print_popup, null);
        builder.setView(dialogView);

        AlertDialog alertDialog = builder.create();
        TextView alertMessage = dialogView.findViewById(R.id.alert_title);
        String alert1 = popUpMsg;
        alertMessage.setText(alert1);
        // Customize dialog buttons if needed
        Button noBtn = dialogView.findViewById(R.id.alert_cancel_button);
        Button yesBtn = dialogView.findViewById(R.id.alert_ok_button);

        noBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                printTxnSummary(mActivity, chargeslip);
            }
        });

        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    public Bitmap txnSummaryChargeslip(final Activity mActivity, JSONObject jsonObject) {
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

            String attendentName = jsonObject.getString("attendentName");
            String fromDate = jsonObject.getString("fromDate");
            String toDate = jsonObject.getString("toDate");
            String startTime = jsonObject.getString("startTime");
            String endTime = jsonObject.getString("endTime");
            String mid = jsonObject.getString("mid");
            String tid = jsonObject.getString("tid");
            String txnSummaryData = jsonObject.getString("txnSummaryData");
            JSONArray txnArray = new JSONArray(txnSummaryData);
            unit.setAlign(IPage.EAlign.CENTER);
            unit.setText("Customer Receipt");

            ImageDecoder decoder = new ImageDecoder(mActivity);
            Bitmap headerBitmap = decoder.decodeImage(R.drawable.bpcl);
            Bitmap headerLogo = Bitmap.createScaledBitmap(headerBitmap, dstWidth, dstHeight, true);
            page.addLine().addUnit(headerLogo, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            if (!attendentName.isEmpty()) {
                page.addLine().addUnit("SUMMARY FOR " + attendentName, FONT_SMALL, IPage.EAlign.CENTER);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            page.addLine().addUnit("MID: " + mid, FONT_SMALL, (float) 8).addUnit("TID: " + tid, FONT_SMALL, IPage.EAlign.RIGHT, (float) 4);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("From Date: " + fromDate, FONT_SMALL, (float) 4).addUnit("To Date: " + toDate, FONT_SMALL, IPage.EAlign.RIGHT, (float) 4);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Start Time: " + startTime, FONT_SMALL, (float) 4).addUnit("End Time: " + endTime, FONT_SMALL, IPage.EAlign.RIGHT, (float) 4);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("----------------------------------------------------------------", FONT_NORMAL);
            page.addLine().addUnit("MOP", FONT_SMALL, IPage.EAlign.LEFT, (float) 2).addUnit("Count(No.)", FONT_SMALL, IPage.EAlign.LEFT, (float) 5).addUnit("Amount(INR)", FONT_SMALL, IPage.EAlign.LEFT, (float) 5);
            page.addLine().addUnit("----------------------------------------------------------------", FONT_NORMAL);

            for (int i = 0; i < txnArray.length(); i++) {
                JSONObject obj = txnArray.getJSONObject(i);
                String mop = obj.getString("mop");
                String count = obj.getString("count");
                String amount = obj.getString("amount");
                page.addLine().addUnit(mop, FONT_SMALL, IPage.EAlign.LEFT, (float) 4).addUnit(count, FONT_SMALL, IPage.EAlign.LEFT, (float) 4).addUnit("₹ " + amount, FONT_SMALL, IPage.EAlign.LEFT, (float) 4);
                page.addLine().addUnit("----------------------------------------------------------------", FONT_NORMAL);
            }
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

    public void printTxnSummary(Activity mActivity, Bitmap chargeslip) {
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
            a920printer.print(chargeslip, new IPrinter.IPinterListener() {
                @Override
                public void onSucc() {
                    Log.d("printLog", "succes");
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(mActivity, MainActivity.class);
                            mActivity.startActivity(intent);
                        }
                    });
                }

                @Override
                public void onError(int i) {
                    Log.d("printLog", "fail");
                    String errorvalue = ErrorValue(String.valueOf(i));
                    Log.d("printStatus2", errorvalue);
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

    public Bitmap txnSummaryDetailChargeslip(final Activity mActivity, JSONObject jsonObject) {
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

            String attendentName = jsonObject.getString("attendentName");
            String fromDate = jsonObject.getString("fromDate");
            String toDate = jsonObject.getString("toDate");
            String startTime = jsonObject.getString("startTime");
            String endTime = jsonObject.getString("endTime");
            String totalCount = jsonObject.getString("totalCount");
            String totalAmount = jsonObject.getString("totalAmount");
            String mid = jsonObject.getString("mid");
            String tid = jsonObject.getString("tid");
            String txnSummaryData = jsonObject.getString("txnSummaryData");
            JSONArray txnArray = new JSONArray(txnSummaryData);
            unit.setAlign(IPage.EAlign.CENTER);
            unit.setText("Customer Receipt");

            ImageDecoder decoder = new ImageDecoder(mActivity);
            Bitmap headerBitmap = decoder.decodeImage(R.drawable.bpcl);
            Bitmap headerLogo = Bitmap.createScaledBitmap(headerBitmap, dstWidth, dstHeight, true);
            page.addLine().addUnit(headerLogo, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            if (!attendentName.isEmpty()) {
                page.addLine().addUnit(attendentName, FONT_SMALL, IPage.EAlign.CENTER);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            page.addLine().addUnit("MID: " + mid, FONT_SMALL, (float) 8).addUnit("TID: " + tid, FONT_SMALL, IPage.EAlign.RIGHT, (float) 4);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("From Date: " + fromDate, FONT_SMALL, (float) 4).addUnit("To Date: " + toDate, FONT_SMALL, IPage.EAlign.RIGHT, (float) 4);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Start Time: " + startTime, FONT_SMALL, (float) 4).addUnit("End Time: " + endTime, FONT_SMALL, IPage.EAlign.RIGHT, (float) 4);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Total Count: " + totalCount, FONT_SMALL, (float) 6).addUnit("Total Amount: " + totalAmount, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("----------------------------------------------------------------", FONT_NORMAL);
            page.addLine().addUnit("Amount", FONT_SMALL, IPage.EAlign.LEFT, (float) 4).addUnit("Time", FONT_SMALL, IPage.EAlign.LEFT, (float) 3).addUnit("Pump No", FONT_SMALL, IPage.EAlign.LEFT, (float) 3);
            page.addLine().addUnit("----------------------------------------------------------------", FONT_NORMAL);

            for (int i = 0; i < txnArray.length(); i++) {
                JSONObject obj = txnArray.getJSONObject(i);
                String amount = obj.getString("amount");
                String time = obj.getString("time");
                String pumpNo = obj.getString("pumpNo");

                page.addLine().addUnit("₹ " + amount, FONT_SMALL, IPage.EAlign.LEFT, (float) 4).addUnit(time, FONT_SMALL, IPage.EAlign.LEFT, (float) 6).addUnit(pumpNo, FONT_SMALL, IPage.EAlign.LEFT, (float) 3);
                page.addLine().addUnit("----------------------------------------------------------------", FONT_NORMAL);
            }
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

    public void printTxnDetailSummary(Activity mActivity, Bitmap chargeslip) {
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
            a920printer.print(chargeslip, new IPrinter.IPinterListener() {
                @Override
                public void onSucc() {
                    Log.d("printLog", "succes");
                }

                @Override
                public void onError(int i) {
                    Log.d("printLog", "fail");
                    String errorvalue = ErrorValue(String.valueOf(i));
                    Log.d("printStatus2", errorvalue);
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

    public void fuelBillDialog(final Activity mActivity, Bitmap chargselip, String fuelType) {
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
                //  customerDialog(mActivity, jsonObject,"Print Customer Receipt");
            }
        });

        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                printReceipt(mActivity, chargselip, fuelType);
            }
        });
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    public void merchantDialog(final Activity mActivity, Bitmap chargselip) {
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
                //  customerDialog(mActivity, jsonObject,"Print Customer Receipt");
            }
        });

        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                printReceipt(mActivity, chargselip, "MERCHANT COPY");
            }
        });
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    public void customerDialog(final Activity mActivity, Bitmap chargselip) {
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
                progress = new ProgressDialog(mActivity);
                progress.setTitle("Loading");
                progress.setMessage("Wait while loading...");
                progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
                progress.show();
                printReceipt(mActivity, chargselip, "CUSTOMER COPY");
            }
        });
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    public void printReceipt(Activity mActivity, Bitmap chargeslip, String printType) {
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
            a920printer.print(chargeslip, new IPrinter.IPinterListener() {
                @Override
                public void onSucc() {
                    Log.d("printLog", "succes");
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (printType.equals("FUEL BILL") || printType.equals("POS SLIP")) {
                                callback.fuelBillPrintYes();
                            } else if (printType.equals("MERCHANT COPY")) {
                                callback.merchantPrintYes();
                            } else {
                                progress.dismiss();
                                callback.customerPrintYes();
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

    public void rePrint(Activity mActivity, Bitmap chargeslip) {
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
            a920printer.print(chargeslip, new IPrinter.IPinterListener() {
                @Override
                public void onSucc() {
                    Log.d("printLog", "succes");
                    if (rePrintResponseCallBack != null) {
                        rePrintResponseCallBack.printSuccess();
                    }
                }

                @Override
                public void onError(int i) {
                    Log.d("printLog", "fail");
                    String errorvalue = ErrorValue(String.valueOf(i));
                    Log.d("printStatus2", errorvalue);
                    if (rePrintResponseCallBack != null) {
                        rePrintResponseCallBack.printFail(errorvalue);
                    }
                }
            });

        } catch (JSONException | PrinterDevException | RuntimeException ex) {
            //  throw new RuntimeException(ex);
        } catch (Exception e) {
            //throw new RuntimeException(e);
        }
    }

    public Bitmap ufillChargeslip(final Activity mActivity, JSONObject jsonObject, String printType) {
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

            String attendentName = jsonObject.getString("attendentName");
            String agencyName = jsonObject.getString("agencyName");
            String address = jsonObject.getString("address");
            String city = jsonObject.getString("city");
            String date = jsonObject.getString("date");
            String time = jsonObject.getString("time");
            String mid = jsonObject.getString("mid");
            String tid = jsonObject.getString("tid");
            String txnType = jsonObject.getString("txnType");
            String product = jsonObject.getString("product");
            String txnId = jsonObject.getString("txnId");
            String unitPrice = jsonObject.getString("unitPrice");
            String quantity = jsonObject.getString("quantity");
            String pumpNo = jsonObject.getString("pumpNo");
            String nozzleNo = jsonObject.getString("nozzleNo");
            String mobileNumber = jsonObject.getString("mobileNumber");
            String vehicleNumber = jsonObject.getString("vehicleNumber");
            String vehicleType = jsonObject.getString("vehicleType");
            String totalSale = jsonObject.getString("totalSale");
            String discount = jsonObject.getString("discount");
            String netAmount = jsonObject.getString("netAmount");

            unit.setAlign(IPage.EAlign.CENTER);
            unit.setText("Customer Receipt");

            ImageDecoder decoder = new ImageDecoder(mActivity);
            Bitmap headerBitmap = decoder.decodeImage(R.drawable.bpcl);
            Bitmap headerLogo = Bitmap.createScaledBitmap(headerBitmap, dstWidth, dstHeight, true);
            page.addLine().addUnit(headerLogo, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            if (!agencyName.isEmpty()) {
                page.addLine().addUnit(agencyName + "(" + sapCode + ")", FONT_SMALL, IPage.EAlign.CENTER);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!address.isEmpty()) {
                page.addLine().addUnit(address, FONT_SMALL, IPage.EAlign.CENTER);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!city.isEmpty()) {
                page.addLine().addUnit(city, FONT_SMALL, IPage.EAlign.CENTER);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!dealerContactNumber.isEmpty()) {
                page.addLine().addUnit(dealerContactNumber, FONT_SMALL, IPage.EAlign.CENTER);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit(" ", LINE_SPACE);
            }


            if (!attendentName.isEmpty()) {
                page.addLine().addUnit("Attendant Name - " + attendentName, FONT_SMALL, IPage.EAlign.LEFT);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            page.addLine().addUnit("DATE: " + date, FONT_SMALL, (float) 4).addUnit("TIME: " + time, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("MID: " + mid, FONT_SMALL, (float) 8).addUnit("TID: " + tid, FONT_SMALL, IPage.EAlign.RIGHT, (float) 4);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Sale", FONT_BIG, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Txn Type: " + txnType, FONT_SMALL, (float) 4);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Product : ", FONT_SMALL, (float) 4).addUnit(product, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Txn id : ", FONT_SMALL, (float) 4).addUnit(txnId, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            if (!unitPrice.isEmpty() && !quantity.isEmpty()) {
                page.addLine().addUnit("Unit Price : ₹ " + unitPrice, FONT_SMALL, (float) 8).addUnit("Quantity : " + quantity + " Ltr", FONT_SMALL, IPage.EAlign.RIGHT, (float) 7);
                page.addLine().addUnit(" ", LINE_SPACE);
            }
            if (!pumpNo.isEmpty() && !nozzleNo.isEmpty()) {
                page.addLine().addUnit("Pump No : " + pumpNo, FONT_SMALL, (float) 8).addUnit("Nozzle No : " + nozzleNo, FONT_SMALL, IPage.EAlign.RIGHT, (float) 7);
                page.addLine().addUnit(" ", LINE_SPACE);
            } else if (!pumpNo.isEmpty()) {
                page.addLine().addUnit("Pump No :" + pumpNo, FONT_SMALL, (float) 4);
                page.addLine().addUnit(" ", LINE_SPACE);
            } else if (!nozzleNo.isEmpty()) {
                page.addLine().addUnit("Nozzle No :" + nozzleNo, FONT_SMALL, (float) 4);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!vehicleNumber.isEmpty() && !mobileNumber.isEmpty()) {
                page.addLine().addUnit("Veh No : " + vehicleNumber, FONT_SMALL, (float) 8).addUnit("Mob No : " + mobileNumber, FONT_SMALL, IPage.EAlign.RIGHT, (float) 8);
                page.addLine().addUnit(" ", LINE_SPACE);
            } else if (!vehicleNumber.isEmpty()) {
                page.addLine().addUnit("Veh No :" + vehicleNumber, FONT_SMALL, (float) 4);
                page.addLine().addUnit(" ", LINE_SPACE);
            } else if (!mobileNumber.isEmpty()) {
                page.addLine().addUnit("Mob No :" + mobileNumber, FONT_SMALL, (float) 4);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!vehicleType.isEmpty()) {
                page.addLine().addUnit("Vehicle Type : ", FONT_SMALL, (float) 4).addUnit(vehicleType, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Total Sale", FONT_SMALL, (float) 4).addUnit("₹ " + totalSale, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("----------------------------------------------------------", FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Net Amount", FONT_24, (float) 8).addUnit("₹ " + netAmount, FONT_24, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("----------------------------------------------------------", FONT_NORMAL, IPage.EAlign.CENTER);

            page.addLine().addUnit(" ", LINE_SPACE);
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

    public Bitmap alpSaleChargeslip(final Activity mActivity, JSONObject jsonObject, String printType) {
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

            String agencyName = jsonObject.getString("agencyName");
//            String address = jsonObject.getString("address");
            String city = jsonObject.getString("city");
            String roMobileNo = jsonObject.getString("roMobileNo");
            String date = jsonObject.getString("date");
            String time = jsonObject.getString("time");

            String tid = jsonObject.getString("tid");
            String txnId = jsonObject.getString("txnId");
            String slipNo = jsonObject.getString("slipNo");
            String reportId = jsonObject.getString("reportId");
            String type = jsonObject.getString("type");
            String txnSource = jsonObject.getString("txnSource");
            String custName = jsonObject.getString("custName");
            String accountNo = jsonObject.getString("accountNo");
            String cardId = jsonObject.getString("cardId");
            String vehCard = jsonObject.getString("vehCard");
            String odometer = jsonObject.getString("odometer");
            String wallet = jsonObject.getString("wallet");
            String product = jsonObject.getString("product");
            String rate = jsonObject.getString("rate");
            String vol = jsonObject.getString("vol");
            String fuelAmount = jsonObject.getString("fuelAmount");
            String tcsAmount = jsonObject.getString("tcsAmount");
            String tcsAmountValue =  txnAmountUpToTwoDecimal(tcsAmount);

            String txnAmount = jsonObject.getString("txnAmount");
            String pmEarn = jsonObject.getString("pmEarn");
            String meShare = jsonObject.getString("meShare");
            String cardBalance = jsonObject.getString("cardBalance");

            if (type.equals("PostAuth")) {
                type = "SALE";
            }

            unit.setAlign(IPage.EAlign.CENTER);
            unit.setText("Customer Receipt");

            ImageDecoder decoder = new ImageDecoder(mActivity);
            Bitmap headerBitmap = decoder.decodeImage(R.drawable.bpcl);
            Bitmap headerLogo = Bitmap.createScaledBitmap(headerBitmap, dstWidth, dstHeight, true);
            page.addLine().addUnit(headerLogo, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            if (!agencyName.isEmpty()) {
                page.addLine().addUnit(agencyName + "(" + sapCode + ")", FONT_SMALL, IPage.EAlign.CENTER);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

//            if (!address.isEmpty()) {
//                page.addLine().addUnit(address, FONT_SMALL, IPage.EAlign.CENTER);
//                page.addLine().addUnit(" ", LINE_SPACE);
//            }

            if (!city.isEmpty()) {
                page.addLine().addUnit(city, FONT_SMALL, IPage.EAlign.CENTER);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!roMobileNo.isEmpty()) {
                page.addLine().addUnit(roMobileNo, FONT_SMALL, IPage.EAlign.CENTER);
                page.addLine().addUnit(" ", LINE_SPACE);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            page.addLine().addUnit("TID: ", FONT_SMALL, (float) 4).addUnit(tid, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("TxnID: ", FONT_SMALL, (float) 4).addUnit(txnId, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit("Date Time:", FONT_SMALL, (float) 4).addUnit(date + " " + time, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            if (!slipNo.isEmpty()) {
                page.addLine().addUnit("SLIP No: ", FONT_SMALL, (float) 4).addUnit(slipNo, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!reportId.isEmpty()) {
                page.addLine().addUnit("Report Id: ", FONT_SMALL, (float) 4).addUnit(reportId, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!type.isEmpty()) {
                page.addLine().addUnit("Txn Type: ", FONT_SMALL, (float) 4).addUnit(type, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!txnSource.isEmpty()) {
                page.addLine().addUnit("Txn Mode: ", FONT_SMALL, (float) 4).addUnit(txnSource, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!custName.isEmpty()) {
                page.addLine().addUnit("CUST NAME: ", FONT_SMALL, (float) 4).addUnit(custName, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!accountNo.isEmpty()) {
                page.addLine().addUnit("Acc. No.: ", FONT_SMALL, (float) 4).addUnit(accountNo, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!cardId.isEmpty()) {
                page.addLine().addUnit("CARD ID: ", FONT_SMALL, (float) 4).addUnit(cardId, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            page.addLine().addUnit("Veh/Card: ", FONT_SMALL, (float) 4).addUnit(vehCard, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);

            if (!odometer.isEmpty()) {
                page.addLine().addUnit("Odometer: ", FONT_SMALL, (float) 4).addUnit(odometer, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (printType.equals("CUSTOMER COPY")) {
                if (!cardBalance.isEmpty()) {
                    page.addLine().addUnit("Card Bal: ", FONT_SMALL, (float) 4).addUnit("₹ " + cardBalance, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                }
            }

            if (!wallet.isEmpty()) {
                page.addLine().addUnit("Wallet: ", FONT_SMALL, (float) 4).addUnit(wallet, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!product.isEmpty()) {
                page.addLine().addUnit("Product: ", FONT_SMALL, (float) 4).addUnit(product, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!rate.isEmpty()) {
                page.addLine().addUnit("Rate: ", FONT_SMALL, (float) 4).addUnit("₹ " + rate, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!vol.isEmpty()) {
                page.addLine().addUnit("Vol in Ltrs: ", FONT_SMALL, (float) 4).addUnit(vol, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!fuelAmount.isEmpty()) {
                page.addLine().addUnit("Fuel Amount: ", FONT_SMALL, (float) 4).addUnit("₹ " + fuelAmount, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!tcsAmount.isEmpty()) {
                page.addLine().addUnit("TCS Amount: ", FONT_SMALL, (float) 4).addUnit("₹ " + tcsAmountValue, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!txnAmount.isEmpty()) {
                page.addLine().addUnit("TXN Amount: ", FONT_SMALL, (float) 4).addUnit("₹ " + txnAmount, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (!pmEarn.isEmpty()) {
                page.addLine().addUnit("PMs Earn: ", FONT_SMALL, (float) 4).addUnit(pmEarn, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                page.addLine().addUnit(" ", LINE_SPACE);
            }

            if (printType.equals("MERCHANT COPY")) {
                if (!meShare.isEmpty()) {
                    page.addLine().addUnit("ME Share: ", FONT_SMALL, (float) 4).addUnit("₹ " + meShare, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
                    page.addLine().addUnit(" ", LINE_SPACE);
                }
            }

            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("I AGREE TO PAY AS PER CARD ISSUER AGREEEMENT", EXTRA_FONT_SMALL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(footerMessage, EXTRA_FONT_SMALL, IPage.EAlign.CENTER);
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
}
