package com.ims.bpcluat.ufill.void_transaction;

import static com.ims.bpcluat.Helper.cashChargeslipDate;
import static com.ims.bpcluat.Helper.cashChargeslipTime;

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
import com.ims.bpcluat.model.VoidTransactionModel;
import com.pax.dal.IDAL;
import com.pax.dal.IPrinter;
import com.pax.dal.exceptions.PrinterDevException;
import com.pax.gl.page.IPage;
import com.pax.gl.page.PaxGLPage;
import com.pax.neptunelite.api.NeptuneLiteUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class VoidReceiptHelper {

    ProgressDialog progress;

    public static IPrinter printer;
    public static IDAL dal;
    public static IPrinter a920printer;
    private PrintResponseCallBack callback;
    int dstWidth = 120;
    int dstHeight = 140;

    List<VoidTransactionModel> voidTransactionModelList;
    public VoidReceiptHelper() {
    }


    public void setCallback(PrintResponseCallBack callback) {
        this.callback = callback;
    }

    public Bitmap voidSlip(final Activity mActivity, JSONObject jsonObject, String printType, String mobileNum) {
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

            String attendentName = Helper.operatorFirstName + " " + Helper.operatorLastName;
//            String agencyName = jsonObject.getString("agencyName");
//            String address = jsonObject.getString("address");
//            String city = jsonObject.getString("city");
//            String date = jsonObject.getString("date");
//            String time = jsonObject.getString("time");
            String mid = jsonObject.getString("mid");
            String tid = jsonObject.getString("tid");
            String txnType = jsonObject.getString("txnType");
            String reportType = jsonObject.getString("reportType");
            String txnId = jsonObject.getString("txnId");
            String amt = jsonObject.getString("amt");
            String dateTime = jsonObject.getString("dateTime");


//            String mobileNumber = jsonObject.getString("mobileNumber");


            /* Start NFR Print */
//            String nfrTotalAmount = (String) jsonObject.get("nfrTotalAmount");
//            String nfrProductName = (String) jsonObject.get("nfrProductName");
//            String nfrUnitPrice = (String) jsonObject.get("nfrUnitPrice");
//            String nfrVolume = (String) jsonObject.get("nfrVolume");
            String[] nfrProductArray = null;
            String[] nfrUnitPriceArray = null;
            String[] nfrVolumeArray = null;
//            if (!nfrTotalAmount.isEmpty()) {
//                nfrProductArray = nfrProductName.split(",");
//                nfrVolumeArray = nfrVolume.split(",");
//                nfrUnitPriceArray = nfrUnitPrice.split(",");
//            }
            /* End NFR Print */

            unit.setAlign(IPage.EAlign.CENTER);
            unit.setText("Customer Receipt");

            ImageDecoder decoder = new ImageDecoder(mActivity);
            Bitmap headerBitmap = decoder.decodeImage(R.drawable.bpcl);
            Bitmap headerLogo = Bitmap.createScaledBitmap(headerBitmap, dstWidth, dstHeight, true);
            page.addLine().addUnit(headerLogo, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);


            if (!attendentName.isEmpty()) {
                page.addLine().addUnit("Attendant Name - " + attendentName, FONT_SMALL, IPage.EAlign.LEFT);
                page.addLine().addUnit(" ", LINE_SPACE);
            }
            page.addLine().addUnit("DATE: " + cashChargeslipDate(), FONT_SMALL, (float) 4).addUnit("TIME: " + cashChargeslipTime(), FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("MID: " + mid, FONT_SMALL, (float) 8).addUnit("TID: " + tid, FONT_SMALL, IPage.EAlign.RIGHT, (float) 4);
            page.addLine().addUnit(" ", LINE_SPACE);

//            if (!batchNo.isEmpty() && !invoiceNo.isEmpty()) {
//                page.addLine().addUnit("BATCH No: " + batchNo, FONT_SMALL, (float) 5).addUnit("INVOICE No: " + invoiceNo, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
//                page.addLine().addUnit(" ", LINE_SPACE);
//            } else if (!batchNo.isEmpty()) {
//                page.addLine().addUnit("BATCH No: " + batchNo, FONT_SMALL, (float) 5);
//            } else if (!invoiceNo.isEmpty()) {
//                page.addLine().addUnit("INVOICE No: " + invoiceNo, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
//            }


            page.addLine().addUnit("Sale", FONT_BIG, IPage.EAlign.CENTER);

            page.addLine().addUnit("Sale", FONT_BIG, IPage.EAlign.CENTER);
            page.addLine().addUnit("Txn Type: " + "VOID", FONT_SMALL, (float) 4);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Report Type : ", FONT_SMALL, (float) 4).addUnit(reportType, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Txn id : ", FONT_SMALL, (float) 4).addUnit(txnId, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Mobile No : ", FONT_SMALL, (float) 4).addUnit(mobileNum, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Amount : ", FONT_SMALL, (float) 4).addUnit("₹ " + amt, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit("Total Sale", FONT_SMALL, (float) 4).addUnit("₹ " + amt, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
//            if (!unitPrice.isEmpty() && !quantity.isEmpty()) {
//                page.addLine().addUnit("Unit Price : ₹ " + unitPrice, FONT_SMALL, (float) 8).addUnit("Quantity : " + quantity + " Ltr", FONT_SMALL, IPage.EAlign.RIGHT, (float) 7);
//                page.addLine().addUnit(" ", LINE_SPACE);
//            }
//            if (!pumpNo.isEmpty() && !nozzleNo.isEmpty()) {
//                page.addLine().addUnit("Pump No : " + pumpNo, FONT_SMALL, (float) 8).addUnit("Nozzle No : " + nozzleNo, FONT_SMALL, IPage.EAlign.RIGHT, (float) 7);
//                page.addLine().addUnit(" ", LINE_SPACE);
//            } else if (!pumpNo.isEmpty()) {
//                page.addLine().addUnit("Pump No :" + pumpNo, FONT_SMALL, (float) 4);
//                page.addLine().addUnit(" ", LINE_SPACE);
//            } else if (!nozzleNo.isEmpty()) {
//                page.addLine().addUnit("Nozzle No :" + nozzleNo, FONT_SMALL, (float) 4);
//                page.addLine().addUnit(" ", LINE_SPACE);
//            }

//            if (!vehicleNumber.isEmpty() && !mobileNumber.isEmpty()) {
//                page.addLine().addUnit("Veh No : " + vehicleNumber, FONT_SMALL, (float) 8).addUnit("Mob No : " + mobileNumber, FONT_SMALL, IPage.EAlign.RIGHT, (float) 8);
//                page.addLine().addUnit(" ", LINE_SPACE);
//            } else if (!vehicleNumber.isEmpty()) {
//                page.addLine().addUnit("Veh No :" + vehicleNumber, FONT_SMALL, (float) 4);
//                page.addLine().addUnit(" ", LINE_SPACE);
//            } else if (!mobileNumber.isEmpty()) {
//                page.addLine().addUnit("Mob No :" + mobileNumber, FONT_SMALL, (float) 4);
//                page.addLine().addUnit(" ", LINE_SPACE);
//            }

//            if(!vehicleType.isEmpty()){
//                page.addLine().addUnit("Vehicle Type : ", FONT_SMALL, (float) 4).addUnit(vehicleType, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
//                page.addLine().addUnit(" ", LINE_SPACE);
//            }

            page.addLine().addUnit("----------------------------------------------------------", FONT_NORMAL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);
//            page.addLine().addUnit("Total Sale", FONT_SMALL, (float) 4).addUnit("₹ " + totalSale, FONT_SMALL, IPage.EAlign.RIGHT, (float) 6);
//            page.addLine().addUnit(" ", LINE_SPACE);
//            page.addLine().addUnit("----------------------------------------------------------", FONT_NORMAL, IPage.EAlign.CENTER);
//            page.addLine().addUnit(" ", LINE_SPACE);
//            page.addLine().addUnit("Net Amount", FONT_24, (float) 8).addUnit("₹ " + netAmount, FONT_24, IPage.EAlign.RIGHT, (float) 6);
//            page.addLine().addUnit(" ", LINE_SPACE);
//            page.addLine().addUnit("----------------------------------------------------------", FONT_NORMAL, IPage.EAlign.CENTER);

            page.addLine().addUnit("Net Amount", FONT_24, (float) 8).addUnit("₹ " + amt, FONT_24, IPage.EAlign.RIGHT, (float) 6);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("----------------------------------------------------------", FONT_NORMAL, IPage.EAlign.CENTER);

            page.addLine().addUnit(" ", LINE_SPACE);

            /* Start : Only for NFR Print */
//            if (!nfrTotalAmount.isEmpty()) {
//                page.addLine().addUnit("----------------------------------------------------------------", FONT_NORMAL);
//                page.addLine().addUnit("Name", FONT_SMALL, IPage.EAlign.LEFT, (float) 4).addUnit("Quantity", FONT_SMALL, IPage.EAlign.CENTER, (float) 4).addUnit("Amt/Qty", FONT_SMALL, IPage.EAlign.CENTER, (float) 4);
//                page.addLine().addUnit("----------------------------------------------------------------", FONT_NORMAL);
//                for (int i = 0; i < nfrProductArray.length; i++) {
//                    page.addLine().addUnit(nfrProductArray[i], FONT_SMALL, IPage.EAlign.LEFT, (float) 4).addUnit(nfrVolumeArray[i], FONT_SMALL, IPage.EAlign.CENTER, (float) 4).addUnit(nfrUnitPriceArray[i], FONT_SMALL, IPage.EAlign.CENTER, (float) 4);
//                    page.addLine().addUnit(" ", LINE_SPACE);
//                }
//                page.addLine().addUnit("----------------------------------------------------------------", FONT_NORMAL);
//                page.addLine().addUnit(" ", LINE_SPACE);
//                page.addLine().addUnit(" ", LINE_SPACE);
//            }
            /* End : Only for NFR Print */


            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit(printType, FONT_24, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);
            page.addLine().addUnit("Version : " + Helper.version, FONT_SMALL, IPage.EAlign.CENTER);
            page.addLine().addUnit(" ", LINE_SPACE);

            page.addLine().adjustTopSpace(10);

//            page.addLine().addUnit(appVersionNo, FONT_SMALL, IPage.EAlign.CENTER);
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

    public void merchantDialog(final Activity mActivity,Bitmap voidSlip) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        LayoutInflater inflater = mActivity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.print_popup, null);
        builder.setView(dialogView);

        AlertDialog alertDialog = builder.create();
        TextView alertMessage = dialogView.findViewById(R.id.alert_title);
        String alert1 = "Print Void Receipt";
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
                printReceipt(mActivity,voidSlip,"MERCHANT COPY");
            }
        });
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    public void printReceipt(Activity mActivity, Bitmap voidSlip,String printType) {
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
                                callback.merchantPrintNo();
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

