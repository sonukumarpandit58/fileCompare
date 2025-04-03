package com.ims.bpcluat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.ims.bpcluat.dialog.MessagesDialog;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

public class CustomCameraActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private PreviewView previewView;
    private ImageButton btnClose, btnFlash, btnCapture;
    private Button btnRetake, btnCancel, btnProceed;
    private ImageView capturedImageView;
    private LinearLayout buttonLayout;
    private boolean isFlashOn = false;
    private Camera camera;
    private ImageCapture imageCapture;
    private Bitmap capturedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_camera);

        previewView = findViewById(R.id.previewView);
        btnClose = findViewById(R.id.btn_close);
        btnFlash = findViewById(R.id.btn_flash);
        btnCapture = findViewById(R.id.btn_capture);
        capturedImageView = findViewById(R.id.capturedImageView);
        buttonLayout = findViewById(R.id.buttonLayout);
        btnRetake = findViewById(R.id.btn_retake);
        btnCancel = findViewById(R.id.btn_cancel);
        btnProceed = findViewById(R.id.btn_proceed);

        btnClose.setOnClickListener(v -> finish());
        btnFlash.setOnClickListener(v -> toggleFlash());
        btnCapture.setOnClickListener(v -> captureImage());

        btnRetake.setOnClickListener(v -> retakeImage());
        btnCancel.setOnClickListener(v -> cancelImage());
        btnProceed.setOnClickListener(v -> proceedWithImage());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        imageCapture = new ImageCapture.Builder().build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
    }

    private void toggleFlash() {
        if (camera == null) {
            MessagesDialog.showDialog(CustomCameraActivity.this, "Camera is not ready", 0,null, null);

           // Toast.makeText(this, "Camera is not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        CameraControl cameraControl = camera.getCameraControl();
        isFlashOn = !isFlashOn;
        if (isFlashOn) {
            cameraControl.enableTorch(true);
            btnFlash.setImageResource(R.drawable.ic_flash_on);
           // Toast.makeText(this, "Flash On", Toast.LENGTH_SHORT).show();
        } else {
            cameraControl.enableTorch(false);
            btnFlash.setImageResource(R.drawable.ic_flash_off);
          //  Toast.makeText(this, "Flash Off", Toast.LENGTH_SHORT).show();
        }
    }

    private void captureImage() {
        if (imageCapture == null) {
            MessagesDialog.showDialog(CustomCameraActivity.this, "ImageCapture is not ready", 0,null, null);

           // Toast.makeText(this, "ImageCapture is not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                processImage(image);
                image.close();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                MessagesDialog.showDialog(CustomCameraActivity.this, "Image capture failed: " + exception.getMessage(), 0,null, null);

                //Toast.makeText(CustomCameraActivity.this, "Image capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processImage(ImageProxy imageProxy) {
        @SuppressLint("UnsafeOptInUsageError")
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            Bitmap bitmap = toBitmap(mediaImage);
            capturedBitmap = cropBitmapToPreview(bitmap, previewView.getWidth(), previewView.getHeight(), imageProxy.getImageInfo().getRotationDegrees());
            runOnUiThread(() -> {
                capturedImageView.setImageBitmap(capturedBitmap);
                showCapturedImage();
            });
        }
    }

    private Bitmap toBitmap(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
    }

    private Bitmap cropBitmapToPreview(Bitmap bitmap, int previewWidth, int previewHeight, int rotationDegrees) {
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        // Calculate the aspect ratio of the preview and the bitmap
        float previewRatio = (float) previewWidth / previewHeight;
        float bitmapRatio = (float) bitmapWidth / bitmapHeight;

        int cropWidth, cropHeight;
        if (previewRatio > bitmapRatio) {
            // Preview is wider than the bitmap, crop the height
            cropWidth = bitmapWidth;
            cropHeight = (int) (bitmapWidth / previewRatio);
        } else {
            // Preview is taller than the bitmap, crop the width
            cropHeight = bitmapHeight;
            cropWidth = (int) (bitmapHeight * previewRatio);
        }

        int cropX = (bitmapWidth - cropWidth) / 2;
        int cropY = (bitmapHeight - cropHeight) / 2;

        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);

        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight, matrix, true);

        return croppedBitmap;
    }

    private void showCapturedImage() {
        previewView.setVisibility(View.GONE);
        btnCapture.setVisibility(View.GONE);
        capturedImageView.setVisibility(View.VISIBLE);
        buttonLayout.setVisibility(View.VISIBLE);
    }

    private void hideCapturedImage() {
        previewView.setVisibility(View.VISIBLE);
        btnCapture.setVisibility(View.VISIBLE);
        capturedImageView.setVisibility(View.GONE);
        buttonLayout.setVisibility(View.GONE);
    }

    private void retakeImage() {
        hideCapturedImage();
    }

    private void cancelImage() {
        hideCapturedImage();
    }

    private void proceedWithImage() {
        if (capturedBitmap != null) {
            recognizeText(capturedBitmap);
        }
    }

    private void recognizeText(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(result -> {
                    String resultText = result.getText();
                    Log.d("ocrResult",resultText);
                    Intent intent = new Intent();
                    intent.putExtra("ocrResult", resultText);
                    setResult(RESULT_OK, intent);
                    finish();
                  //  Toast.makeText(CustomCameraActivity.this, resultText, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    MessagesDialog.showDialog(CustomCameraActivity.this, "Text recognition failed: " + e.getMessage(), 0,null, null);

                   // Toast.makeText(CustomCameraActivity.this, "Text recognition failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
