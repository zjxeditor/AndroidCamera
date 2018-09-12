package com.zjxdev.tracker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private FrameLayout frameView;
    private AutoFitTextureView cameraView;
    private CameraBridge cameraBridge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        frameView = findViewById(R.id.frameView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            recreateCameraView();
            if(cameraBridge == null) {
                cameraBridge = new CameraBridge(cameraView, this, false, 60);
            } else {
                cameraBridge.setTextureView(cameraView);
            }
            cameraBridge.onResume();
        }
    }

    @Override
    protected void onPause() {
        if(cameraBridge != null)
            cameraBridge.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if(cameraBridge != null)
            cameraBridge.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Camera permission granted.");
                } else {
                    Log.e(TAG, "Camera permission denied.");
                }
            }
        }
    }

    private void recreateCameraView() {
        if(cameraView != null)
            frameView.removeView(cameraView);
        cameraView = new AutoFitTextureView(this);
        cameraView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        frameView.addView(cameraView, 0);
    }
}
