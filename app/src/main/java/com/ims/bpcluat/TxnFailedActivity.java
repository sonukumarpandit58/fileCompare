package com.ims.bpcluat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.ims.bpcluat.databinding.ActivitySuccessBinding;
import com.ims.bpcluat.databinding.ActivityTxnFailedBinding;

public class TxnFailedActivity extends AppCompatActivity {

    ActivityTxnFailedBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTxnFailedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TxnFailedActivity.this, SideBarActivity.class);
                startActivity(intent);
                finish();
            }
        });

        binding.retryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}