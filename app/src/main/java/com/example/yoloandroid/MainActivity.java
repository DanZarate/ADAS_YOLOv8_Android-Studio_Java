package com.example.yoloandroid;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.camera.lifecycle.ProcessCameraProvider;

import com.example.yoloandroid.analysis.FullImageAnalyse;
import com.example.yoloandroid.analysis.FullScreenAnalyse;
import com.example.yoloandroid.detector.YoloTFLiteDetector;
import com.example.yoloandroid.utils.CameraProcess;
import com.google.common.util.concurrent.ListenableFuture;

//import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";
    /*static{
        if(OpenCVLoader.initDebug()){
            Log.d(TAG,"OpenCV instalado");
        }
        else {
            Log.d(TAG,"OpenCV no instalado");
        }
    }*/


    private boolean IS_FULL_SCREEN = false;

    private PreviewView cameraPreviewMatch;
    private PreviewView cameraPreviewWrap;
    private ImageView boxLabelCanvas;
    private Spinner modelSpinner;
    private Switch immersive;
    private Switch verZona;
    private SeekBar seekBarKp;
    private SeekBar seekBarLane;
    private SeekBar seekBarBox;
    private boolean zona;
    private int kp;
    private int pLinea;
    private int pBox;
    private TextView inferenceTimeTextView;
    private TextView frameSizeTextView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private YoloTFLiteDetector yolov8TFLiteDetector;

    private CameraProcess cameraProcess = new CameraProcess();

    private TextView miTextView;



    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    private void initModel(String modelName) {
        // modelo de carga
        try {
            this.yolov8TFLiteDetector = new YoloTFLiteDetector();
            this.yolov8TFLiteDetector.setModelFile(modelName);
//            this.yolov8TFLiteDetector.addNNApiDelegate();
            this.yolov8TFLiteDetector.addGPUDelegate();
            this.yolov8TFLiteDetector.initialModel(this);
            Log.i("model", "Success loading model" + this.yolov8TFLiteDetector.getModelFile());
        } catch (Exception e) {
            Log.e("image", "load model error: " + e.getMessage() + e.toString());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        miTextView = findViewById(R.id.parametros);

        // Ocultar la barra de estado superior al abrir la aplicación
//        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        // pantalla completa
        cameraPreviewMatch = findViewById(R.id.camera_preview_match);
        cameraPreviewMatch.setScaleType(PreviewView.ScaleType.FILL_START);

        // pantalla completa
        cameraPreviewWrap = findViewById(R.id.camera_preview_wrap);
//        cameraPreviewWrap.setScaleType(PreviewView.ScaleType.FILL_START);

        // box/label -- pantalla de caja/etiqueta
        boxLabelCanvas = findViewById(R.id.box_label_canvas);

        // botón desplegable
        modelSpinner = findViewById(R.id.model);

        // Botón de experiencia inmersiva
        immersive = findViewById(R.id.immersive);

        // Botón de Visualización de zona
        verZona = findViewById(R.id.verZona);

        // Constante Kp
        seekBarKp = findViewById(R.id.seekBar);
        // Precisión linea
        seekBarLane = findViewById(R.id.seekBar2);

        seekBarBox = findViewById(R.id.seekBar3);

        // Algunas vistas actualizadas en tiempo real
        inferenceTimeTextView = findViewById(R.id.inference_time);
        frameSizeTextView = findViewById(R.id.frame_size);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        // Solicitar permiso de cámara
        if (!cameraProcess.allPermissionsGranted(this)) {
            cameraProcess.requestPermissions(this);
        }

        // Obtenga los parámetros de rotación de la cámara de la cámara del teléfono móvil
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Log.i("image", "rotation: " + rotation);

        cameraProcess.showCameraSupportSize(MainActivity.this);

        // Inicializar y cargar yolov8n-320
        initModel("yolov8n-320");//

        // Botón de alternancia del modelo de escucha
        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String model = (String) adapterView.getItemAtPosition(i);
                Toast.makeText(MainActivity.this, "loading model: " + model, Toast.LENGTH_LONG).show();
                initModel(model);
                if(IS_FULL_SCREEN){
                    cameraPreviewWrap.removeAllViews();
                    FullScreenAnalyse fullScreenAnalyse = new FullScreenAnalyse(MainActivity.this,
                            cameraPreviewMatch,
                            boxLabelCanvas,
                            rotation,
                            inferenceTimeTextView,
                            frameSizeTextView,
                            yolov8TFLiteDetector);
                    cameraProcess.startCamera(MainActivity.this, fullScreenAnalyse, cameraPreviewMatch);
                }else{
                    cameraPreviewMatch.removeAllViews();
                    FullImageAnalyse fullImageAnalyse = new FullImageAnalyse(
                            MainActivity.this,
                            cameraPreviewWrap,
                            boxLabelCanvas,
                            rotation,
                            inferenceTimeTextView,
                            frameSizeTextView,
                            yolov8TFLiteDetector,zona, kp, pLinea, pBox);
                    cameraProcess.startCamera(MainActivity.this, fullImageAnalyse, cameraPreviewWrap);
                }


            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        seekBarBox.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                pBox = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                FullImageAnalyse fullImageAnalyse = new FullImageAnalyse(
                        MainActivity.this,
                        cameraPreviewWrap,
                        boxLabelCanvas,
                        rotation,
                        inferenceTimeTextView,
                        frameSizeTextView,
                        yolov8TFLiteDetector, zona, kp, pLinea, pBox);
                cameraProcess.startCamera(MainActivity.this, fullImageAnalyse, cameraPreviewWrap);
            }
        });

        seekBarLane.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                pLinea = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                FullImageAnalyse fullImageAnalyse = new FullImageAnalyse(
                        MainActivity.this,
                        cameraPreviewWrap,
                        boxLabelCanvas,
                        rotation,
                        inferenceTimeTextView,
                        frameSizeTextView,
                        yolov8TFLiteDetector, zona, kp, pLinea, pBox);
                cameraProcess.startCamera(MainActivity.this, fullImageAnalyse, cameraPreviewWrap);
            }
        });

        seekBarKp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                kp = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                FullImageAnalyse fullImageAnalyse = new FullImageAnalyse(
                        MainActivity.this,
                        cameraPreviewWrap,
                        boxLabelCanvas,
                        rotation,
                        inferenceTimeTextView,
                        frameSizeTextView,
                        yolov8TFLiteDetector, zona, kp,pLinea, pBox);
                cameraProcess.startCamera(MainActivity.this, fullImageAnalyse, cameraPreviewWrap);

            }
        });

        verZona.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                zona = isChecked;
                FullImageAnalyse fullImageAnalyse = new FullImageAnalyse(
                        MainActivity.this,
                        cameraPreviewWrap,
                        boxLabelCanvas,
                        rotation,
                        inferenceTimeTextView,
                        frameSizeTextView,
                        yolov8TFLiteDetector, zona, kp,pLinea, pBox);
                cameraProcess.startCamera(MainActivity.this, fullImageAnalyse, cameraPreviewWrap);
            }
        });

        // Escuche los botones de cambio de vista
        immersive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                IS_FULL_SCREEN = b;
                if (b) {
                    cameraPreviewWrap.removeAllViews();
                    FullScreenAnalyse fullScreenAnalyse = new FullScreenAnalyse(MainActivity.this,
                            cameraPreviewMatch,
                            boxLabelCanvas,
                            rotation,
                            inferenceTimeTextView,
                            frameSizeTextView,
                            yolov8TFLiteDetector);
                    cameraProcess.startCamera(MainActivity.this, fullScreenAnalyse, cameraPreviewMatch);

                } else {
                    cameraPreviewMatch.removeAllViews();
                    FullImageAnalyse fullImageAnalyse = new FullImageAnalyse(
                            MainActivity.this,
                            cameraPreviewWrap,
                            boxLabelCanvas,
                            rotation,
                            inferenceTimeTextView,
                            frameSizeTextView,
                            yolov8TFLiteDetector, zona,kp,pLinea, pBox);
                    cameraProcess.startCamera(MainActivity.this, fullImageAnalyse, cameraPreviewWrap);


                }
            }
        });


    }
}