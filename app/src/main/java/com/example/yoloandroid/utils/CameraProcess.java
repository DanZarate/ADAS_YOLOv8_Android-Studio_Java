package com.example.yoloandroid.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

public class CameraProcess {

    private CameraControl cameraControl; // Agrega esta variable global



    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"};


    public boolean allPermissionsGranted(Context context) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void requestPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
    }


    public void startCamera(Context context, ImageAnalysis.Analyzer analyzer, PreviewView previewView) {

        cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
//                            .setTargetResolution(new Size(1080, 1920))
                            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                            .setTargetAspectRatioCustom(new Rational(16,9))
//                            .setTargetRotation(Surface.ROTATION_90)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build();
                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer);
                    Preview previewBuilder = new Preview.Builder()
//                            .setTargetResolution(new Size(1080,1440))
                            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                            .setTargetRotation(Surface.ROTATION_90)
                            .build();
//                    Log.i("builder", previewView.getHeight()+"/"+previewView.getWidth());
                    CameraSelector cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
                    previewBuilder.setSurfaceProvider(previewView.createSurfaceProvider());
                    //previewBuilder.setSurfaceProvider(previewView.getSurfaceProvider());


                    //cameraProvider.unbindAll();
                    //cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector, imageAnalysis, previewBuilder);
                    // Inicializa el zoom


                    cameraProvider.unbindAll();
                    Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector, imageAnalysis, previewBuilder);

                    initializeZoom(camera.getCameraControl());
                    setZoom(0.1f);

                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(context));
    }

    public void initializeZoom(CameraControl cameraControl) {
        this.cameraControl = cameraControl;
    }



    /**
     * Configura el zoom de la cámara.
     * @param zoomFactor El factor de zoom (1.0f es sin zoom, >1.0f para hacer zoom in, <1.0f para zoom out).
     */
    public void setZoom(float zoomFactor) {
        if (cameraControl != null) {
            cameraControl.setZoomRatio(zoomFactor);
        }
    }


    public void showCameraSupportSize(Activity activity) {
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics cc = manager.getCameraCharacteristics(id);
                if (cc.get(CameraCharacteristics.LENS_FACING) == 1) {
                    Size[] previewSizes = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                            .getOutputSizes(SurfaceTexture.class);
                    for (Size s : previewSizes){
                        Log.i("camera", s.getHeight()+"/"+s.getWidth());
                    }
                    break;

                }
            }
        } catch (Exception e) {
            Log.e("image", "can not open camera", e);
        }
    }

}

