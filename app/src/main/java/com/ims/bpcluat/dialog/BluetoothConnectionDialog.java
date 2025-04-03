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
import com.ims.bpcluat.ufill.UfillPresetActivity;

public class BluetoothConnectionDialog {

    // Define an interface for the callback
    public interface OnDialogDismissListener {
        void onDismiss();
    }

    public static void showDialog(Context context) {
        showDialog(context, 0, null);
    }

    public static void showDialog(Context context, int counter, OnDialogDismissListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_bluetooth_connection, null);
        builder.setView(dialogView);

        AlertDialog alertDialog = builder.create();

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView textView = dialogView.findViewById(R.id.text_message);
        Button okButton = dialogView.findViewById(R.id.button_ok);

        // Check if the calling context is UfillPresetActivity and update message & button text
        if (context instanceof UfillPresetActivity) {
            if (counter == 4) {
                textView.setText("Unable to complete the transaction.");
                okButton.setText("Home");
            } else {
                textView.setText("Connection unsuccessful!\nMake sure Bluetooth is turned on and in range.");
                okButton.setText("Retry");
            }
        } else {
            textView.setText("Connection unsuccessful!\nMake sure Bluetooth is turned on and in range.");
            okButton.setText("OK");
        }

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();

                // Trigger callback only for UfillPresetActivity
                if (context instanceof UfillPresetActivity && listener != null) {
                    listener.onDismiss();
                }
            }
        });

        alertDialog.setCancelable(false);
        alertDialog.show();
    }
}
