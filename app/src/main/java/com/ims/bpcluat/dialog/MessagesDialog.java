package com.ims.bpcluat.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.ims.bpcluat.R;
import com.ims.bpcluat.SideBarActivity;

public class MessagesDialog {

    private static AlertDialog currentDialog;

    public static void showDialog(Context context, String msg, int drawableResId, Intent nextScreenIntent, Fragment nextFragment) {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }

        // Create an AlertDialog.Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        // Inflate the custom layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_bluetooth_connection, null);
        builder.setView(dialogView);

        // Create the AlertDialog and store its reference
        currentDialog = builder.create();

        // Set background to be transparent to allow custom rounded corners
        if (currentDialog.getWindow() != null) {
            currentDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Get the TextView and Button from the custom layout
        TextView textView = dialogView.findViewById(R.id.text_message);
        Button okButton = dialogView.findViewById(R.id.button_ok);
        ImageView imageView = dialogView.findViewById(R.id.image_warning);

        // Set the message
        textView.setText(msg);

        if(drawableResId == 0){
            imageView.setImageResource(R.drawable.error);
        }else {
            imageView.setImageResource(drawableResId);
        }

        // Set the Button click listener
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDialog.dismiss();

                // Handle Activity navigation if nextScreenIntent is provided
                if (nextScreenIntent != null) {
                    context.startActivity(nextScreenIntent);
                }

                // Handle Fragment navigation using SideBarActivity's loadFragement if nextFragment is provided
                if (nextFragment != null && context instanceof SideBarActivity) {
                    ((SideBarActivity) context).loadFragement(nextFragment);
                }
            }
        });

        // Show the AlertDialog
        currentDialog.setCancelable(false);
        currentDialog.show();
    }

    // Method to dismiss the currently active dialog
    public static void dismissDialog() {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }
    }
}

