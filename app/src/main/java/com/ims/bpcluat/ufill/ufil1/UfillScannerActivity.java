package com.ims.bpcluat.ufill.ufil1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.ResultPoint;
import com.ims.bpcluat.R;
import com.ims.bpcluat.databinding.ActivityUfillScannerBinding;
import com.ims.bpcluat.helper.ApiHelper;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.List;

public class UfillScannerActivity extends AppCompatActivity {

    private ActivityUfillScannerBinding binding;
    private boolean isResultShown = false;
    private Context context;
    private static final int REQUEST_CAMERA_CODE = 100;
    private ApiHelper api;
    private DecoratedBarcodeView barcodeScannerView;
    private boolean isFlashOn = false;
    private Handler timeoutHandler;
    private Runnable timeoutRunnable;
    private boolean isTimeoutCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        binding = ActivityUfillScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        barcodeScannerView = binding.barcodeScanner;
        api = new ApiHelper();
        context = this;

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            askCameraPermission();
        } else {
            cameraOpen();
        }

        binding.btnFlash.setOnClickListener(v -> toggleFlash());
        binding.btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();
            }
        });

        timeoutHandler = new Handler();
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isResultShown) {
                    Log.d("TAG", "Timeout reached without scanning.!!");
                    isTimeoutCompleted = true;

                    Intent intent = new Intent();
                    intent.putExtra("timeoutCompleted", true); // Add this flag to indicate timeout completion
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }
            }
        };

        timeoutHandler.postDelayed(timeoutRunnable, 10000);

    }

    public void cameraOpen() {
        Log.d("TAG", "Opening camera");
        barcodeScannerView.resume();
        barcodeScannerView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (!isResultShown) {
                    Log.d("TAG", "Barcode scanned: " + result.getText());
                    String scannedText = result.getText();
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    Intent intent = new Intent();
                    intent.putExtra("qrResult", scannedText);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                    isResultShown = true;
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {
                    Log.d("TAG", "Possible result point##: " + resultPoints.toString());
            }
        });
    }


    public void askCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_CODE);
    }

    private void toggleFlash() {
        if (isFlashOn) {
            barcodeScannerView.setTorchOn();
            binding.btnFlash.setImageResource(R.drawable.ic_flash_on);
        } else {
            barcodeScannerView.setTorchOff();
            binding.btnFlash.setImageResource(R.drawable.ic_flash_off);
        }
        isFlashOn = !isFlashOn;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("TAG", "Camera permission granted");
                cameraOpen();
            } else {
                Log.d("TAG", "Camera permission denied");
                // Handle the case where permission is not granted
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            barcodeScannerView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeScannerView.pause();
    }
}