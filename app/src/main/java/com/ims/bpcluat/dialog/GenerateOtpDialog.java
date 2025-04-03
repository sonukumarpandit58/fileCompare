package com.ims.bpcluat.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;
import com.ims.bpcluat.alp.alpOperations.sale.SaleFragment;
import com.ims.bpcluat.alp.alpOperations.sale.loyaltyotp.MobileNumberFragment;

public class GenerateOtpDialog {

    public static void showDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        // Inflate the custom layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_generate_otp, null);
        builder.setView(dialogView);

        // Create the AlertDialog
        AlertDialog alertDialog = builder.create();

        // Set background to be transparent to allow custom rounded corners
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Get the TextView and Button from the custom layout
        TextView textView = dialogView.findViewById(R.id.text_messageotp);
        Button yesButton = dialogView.findViewById(R.id.button_yes);
        Button otpbutton_no = dialogView.findViewById(R.id.otpbutton_no);

        // Set the message
        textView.setText("Do you want to generate \n OTP?");

        // Set the Button click listener
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        otpbutton_no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                ((SideBarActivity) context).loadFragement(new SaleFragment());

            }
        });

        // Show the AlertDialog
        alertDialog.setCancelable(false);
        alertDialog.show();
    }
}

