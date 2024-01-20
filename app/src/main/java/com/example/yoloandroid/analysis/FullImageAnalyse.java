package com.example.yoloandroid.analysis;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;

import com.example.yoloandroid.detector.YoloTFLiteDetector;
import com.example.yoloandroid.utils.ImageProcess;
import com.example.yoloandroid.utils.Recognition;

//import org.opencv.core.Rect;

import java.util.ArrayList;
import java.util.Objects;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class FullImageAnalyse implements ImageAnalysis.Analyzer {

    public static class Result{

        public Result(long costTime, Bitmap bitmap) {
            this.costTime = costTime;
            this.bitmap = bitmap;
        }
        long costTime;
        Bitmap bitmap;
    }

    ImageView boxLabelCanvas;
    PreviewView previewView;
    int rotation;
    boolean zona;
    int kp;
    int pLinea;
    int pBox;
    float rightx1,righty1;
    float rightx2,righty2;
    float leftx1,lefty1;
    float leftx2,lefty2;
    float rightx1_aux;
    float righty1_aux;
    float rightx2_aux;
    float righty2_aux;

    float leftx1_aux;
    float lefty1_aux;
    float leftx2_aux;
    float lefty2_aux;
    private TextView inferenceTimeTextView;
    private TextView frameSizeTextView;
    ImageProcess imageProcess;
    private YoloTFLiteDetector yoloTFLiteDetector;



    public FullImageAnalyse(Context context,
                            PreviewView previewView,
                            ImageView boxLabelCanvas,
                            int rotation,
                            TextView inferenceTimeTextView,
                            TextView frameSizeTextView,
                            YoloTFLiteDetector yoloTFLiteDetector, boolean zona, int kp, int pLinea, int pBox) {
        this.previewView = previewView;
        this.boxLabelCanvas = boxLabelCanvas;
        this.rotation = rotation;
        this.inferenceTimeTextView = inferenceTimeTextView;
        this.frameSizeTextView = frameSizeTextView;
        this.imageProcess = new ImageProcess();
        this.yoloTFLiteDetector = yoloTFLiteDetector;
        this.zona = zona;
        this.kp = kp;
        this.pLinea = pLinea;
        this.pBox = pBox;
    }

    @SuppressLint("CheckResult")
    @Override
    public void analyze(@NonNull ImageProxy image) {
        int previewHeight = previewView.getHeight();
        int previewWidth = previewView.getWidth();




        Observable.create( (ObservableEmitter<Result> emitter) -> {
                    long start = System.currentTimeMillis();

                    byte[][] yuvBytes = new byte[3][];
                    ImageProxy.PlaneProxy[] planes = image.getPlanes();
                    int imageHeight = image.getHeight();
                    int imagewWidth = image.getWidth();

                    imageProcess.fillBytes(planes, yuvBytes);
                    int yRowStride = planes[0].getRowStride();
                    final int uvRowStride = planes[1].getRowStride();
                    final int uvPixelStride = planes[1].getPixelStride();



                    int[] rgbBytes = new int[imageHeight * imagewWidth];
                    imageProcess.YUV420ToARGB8888(
                            yuvBytes[0],
                            yuvBytes[1],
                            yuvBytes[2],
                            imagewWidth,
                            imageHeight,
                            yRowStride,
                            uvRowStride,
                            uvPixelStride,
                            rgbBytes);

                    // 原图bitmap
                    Bitmap imageBitmap = Bitmap.createBitmap(imagewWidth, imageHeight, Bitmap.Config.ARGB_8888);
                    imageBitmap.setPixels(rgbBytes, 0, imagewWidth, 0, 0, imagewWidth, imageHeight);

                    // 图片适应屏幕fill_start格式的bitmap
                    double scale = Math.max(
                            previewHeight / (double) (rotation % 180 == 0 ? imagewWidth : imageHeight),
                            previewWidth / (double) (rotation % 180 == 0 ? imageHeight : imagewWidth)
                    );
                    Matrix fullScreenTransform = imageProcess.getTransformationMatrix(
                            imagewWidth, imageHeight,
                            (int) (scale * imageHeight), (int) (scale * imagewWidth),
                            rotation % 180 == 0 ? 90 : 0, false
                    );

                    // 适应preview的全尺寸bitmap
                    Bitmap fullImageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imagewWidth, imageHeight, fullScreenTransform, false);
                    // 裁剪出跟preview在屏幕上一样大小的bitmap
                    Bitmap cropImageBitmap = Bitmap.createBitmap(fullImageBitmap, 0, 0, previewWidth, previewHeight);

                    // 模型输入的bitmap
                    Matrix previewToModelTransform =
                            imageProcess.getTransformationMatrix(
                                    cropImageBitmap.getWidth(), cropImageBitmap.getHeight(),
                                    yoloTFLiteDetector.getInputSize().getWidth(),
                                    yoloTFLiteDetector.getInputSize().getHeight(),
                                    0, false);
                    Bitmap modelInputBitmap = Bitmap.createBitmap(cropImageBitmap, 0, 0,
                            cropImageBitmap.getWidth(), cropImageBitmap.getHeight(),
                            previewToModelTransform, false);

                    Matrix modelToPreviewTransform = new Matrix();
                    previewToModelTransform.invert(modelToPreviewTransform);

                    ArrayList<Recognition> recognitions = yoloTFLiteDetector.detect(modelInputBitmap);
//            ArrayList<Recognition> recognitions = yoloTFLiteDetector.detect(imageBitmap);

                    Bitmap emptyCropSizeBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
                    Canvas cropCanvas = new Canvas(emptyCropSizeBitmap);
//            Paint white = new Paint();
//            white.setColor(Color.WHITE);
//            white.setStyle(Paint.Style.FILL);
//            cropCanvas.drawRect(new RectF(0,0,previewWidth, previewHeight), white);
                    // 边框画笔
                    Paint boxPaint = new Paint();
                    boxPaint.setStrokeWidth(5);
                    boxPaint.setStyle(Paint.Style.STROKE);
                    boxPaint.setColor(Color.GREEN);
                    // Texto probabilidad
                    Paint textPain = new Paint();
                    textPain.setTextSize(50);
                    textPain.setColor(Color.BLACK);
                    textPain.setStyle(Paint.Style.FILL);
                    // Texto probabilidad
                    Paint textPain2 = new Paint();
                    textPain2.setTextSize(80);
                    textPain2.setColor(Color.RED);
                    textPain2.setStyle(Paint.Style.FILL);
                    // Dibujar el polígono
                    Paint linePaint = new Paint();
                    linePaint.setStrokeWidth(5);
                    linePaint.setColor(Color.YELLOW);

                    Paint linePaint2 = new Paint();
                    linePaint2.setStrokeWidth(5);
                    linePaint2.setColor(Color.GREEN);

                    Paint paint = new Paint();
                    paint.setColor(Color.BLUE);
                    paint.setStrokeWidth(5);

                    //Crea un objeto Path y agrega los puntos al polígono
                    Path path = new Path();


                    for (Recognition res : recognitions) {
                        RectF location = res.getLocation();
                        String label = res.getLabelName();
                        float confidence = res.getConfidence();
                        modelToPreviewTransform.mapRect(location);

                        float pBox_aux = ((pBox+10)/100.0f);
                        if(pBox_aux>1.0f) {
                            pBox_aux = 1.0f;
                        }

                        if (Objects.equals(label, "aeroplane")) {
                            if (confidence >= (pLinea/100.0f)) {

                                float left = location.left;
                                float top = location.top;
                                float right = location.right;
                                float bottom = location.bottom;
                    /*
                    (left, top): Esquina superior izquierda.
                    (right, top): Esquina superior derecha.
                    (left, bottom): Esquina inferior izquierda.
                    (right, bottom): Esquina inferior derecha.
                     */
                                // Calcular el centro del rectángulo
                                float centerX = (location.left + location.right) / 2.0f;
                                float centerY = (location.top + location.bottom) / 2.0f;


                                //Linea Izquierda
                                if((previewWidth/2) > centerX){
                                    //cropCanvas.drawLine(right, top, left, bottom, linePaint);
                                    rightx1_aux = right;
                                    righty1_aux = top;
                                    rightx2_aux = left;
                                    righty2_aux = bottom;

                                    if (rightx1_aux>leftx1_aux)
                                    {
                                        float aux1 = leftx1_aux;
                                        float aux2 = lefty1_aux;

                                        leftx1_aux = rightx1_aux;
                                        lefty1_aux = righty1_aux;

                                        rightx1_aux = aux1;
                                        righty1_aux = aux2;
                                    }
                                    if (rightx2_aux>leftx2_aux)
                                    {
                                        float aux1 = leftx2_aux;
                                        float aux2 = lefty2_aux;

                                        leftx2_aux = rightx2_aux;
                                        lefty2_aux = righty2_aux;

                                        rightx2_aux = aux1;
                                        righty2_aux = aux2;
                                    }

                                    rightx1 = rightx1 + (rightx1_aux-rightx1)*confidence*(kp/100.0f);//rightx1_aux;//
                                    rightx2 = rightx2 + (rightx2_aux-rightx2)*confidence*(kp/100.0f);//rightx2_aux;//
                                    righty1 = righty1 + (righty1_aux-righty1)*confidence*(kp/100.0f);//righty1_aux;//
                                    righty2 = righty2 + (righty2_aux-righty2)*confidence*(kp/100.0f);//righty2_aux;//
                                    cropCanvas.drawLine(rightx1, righty1, rightx2, righty2, linePaint);
                                }
                                //Linea Derecha
                                else {
                                    //cropCanvas.drawLine(left, top, right, bottom, linePaint);
                                    leftx1_aux = left;
                                    lefty1_aux = top;
                                    leftx2_aux = right;
                                    lefty2_aux = bottom;

                                    if (rightx1_aux>leftx1_aux)
                                    {
                                        float aux1 = leftx1_aux;
                                        float aux2 = lefty1_aux;

                                        leftx1_aux = rightx1_aux;
                                        lefty1_aux = righty1_aux;

                                        rightx1_aux = aux1;
                                        righty1_aux = aux2;
                                    }
                                    if (rightx2_aux>leftx2_aux)
                                    {
                                        float aux1 = leftx2_aux;
                                        float aux2 = lefty2_aux;

                                        leftx2_aux = rightx2_aux;
                                        lefty2_aux = righty2_aux;

                                        rightx2_aux = aux1;
                                        righty2_aux = aux2;
                                    }

                                    leftx1 = leftx1 + (leftx1_aux-leftx1)*confidence*(kp/100.0f);//leftx1_aux;//
                                    leftx2 = leftx2 + (leftx2_aux-leftx2)*confidence*(kp/100.0f);//leftx2_aux;//
                                    lefty1 = lefty1 + (lefty1_aux-lefty1)*confidence*(kp/100.0f);//lefty1_aux;//
                                    lefty2 = lefty2 + (lefty2_aux-lefty2)*confidence*(kp/100.0f);//lefty2_aux;//
                                    cropCanvas.drawLine(leftx1, lefty1, leftx2, lefty2, linePaint2);
                                }

                                if (rightx1>leftx1)
                                {
                                    float aux1 = leftx1;
                                    float aux2 = lefty1;

                                    leftx1 = rightx1;
                                    lefty1 = righty1;

                                    rightx1 = aux1;
                                    righty1 = aux2;
                                }
                                if (rightx2>leftx2)
                                {
                                    float aux1 = leftx2;
                                    float aux2 = lefty2;

                                    leftx2 = rightx2;
                                    lefty2 = righty2;

                                    rightx2 = aux1;
                                    righty2 = aux2;
                                }
                            }
                        }
                        else {
                            if (confidence > (pBox/100.0f)) {

                                boolean b = Objects.equals(label, "truck") || Objects.equals(label, "bus");
                                if (b) {
                                    if (confidence >= (pBox_aux)) {
                                        cropCanvas.drawRect(location, boxPaint);
                                        cropCanvas.drawText(label + ":" + String.format("%.2f", confidence), location.left, location.top, textPain);
                                    }
                                }
                                else if (Objects.equals(label, "persona")) {
                                    cropCanvas.drawRect(location, boxPaint);
                                    cropCanvas.drawText(label + ":" + String.format("%.2f", confidence), location.left, location.top, textPain);

                                }
                                else{
                                    if (confidence > (pBox/100.0f)) {
                                        cropCanvas.drawRect(location, boxPaint);
                                        cropCanvas.drawText(label + ":" + String.format("%.2f", confidence), location.left, location.top, textPain);
                                    }
                                }



                                // Crear la región para el rectángulo
                                Region regionRect = new Region((int) location.left, (int) location.top, (int) location.right, (int) location.bottom);

                                // Crear la región para el polígono
                                Region regionPoly = new Region();
                                regionPoly.setPath(path, new Region((int) location.left, (int) location.top, (int) location.right, (int) location.bottom));


                                if (!regionPoly.isEmpty()) {


                                    // Calcular la intersección entre las dos regiones
                                    regionPoly.op(regionRect, Region.Op.INTERSECT);

                                    // Calcular el área de la intersección
                            /*Rect intersection = new Rect();
                            intersection.x = regionPoly.getBounds().left;
                            intersection.y = regionPoly.getBounds().top;
                            intersection.width = regionPoly.getBounds().width();
                            intersection.height = regionPoly.getBounds().height();
                            // Calcular el área de la intersección
                            double intersectionArea = intersection.area();*/

                                    // Calcular el área de la intersección
                                    Rect intersection = regionPoly.getBounds();
                                    double intersectionArea = Math.max(0, Math.min(location.right, intersection.right) - Math.max(location.left, intersection.left))
                                            * Math.max(0, Math.min(location.bottom, intersection.bottom) - Math.max(location.top, intersection.top));


                                    // Calcular el área total del rectángulo
                                    double totalArea = (location.right - location.left) * (location.bottom - location.top);

                                    // Calcular el porcentaje de intersección
                                    double intersectionPercentage = (intersectionArea / totalArea) * 100;

                                    if (Objects.equals(label, "persona"))
                                    {
                                        cropCanvas.drawText("Intersección Alerta " + String.format("%.2f", intersectionPercentage), 1, previewHeight, textPain2);

                                    }
                                    else {
                                        if (b)
                                        {
                                            if (intersectionPercentage > 50 && confidence > pBox_aux) {
                                                // Mostrar el porcentaje de intersección
                                                //Log.d("Intersección", "Porcentaje de intersección: " + intersectionPercentage + "%");
                                                cropCanvas.drawText("Intersección Alerta " + String.format("%.2f", intersectionPercentage), 1, previewHeight, textPain2);
                                                //cropCanvas.drawText("Intersección Alerta ", 0, previewHeight, textPain2);
                                            }
                                        }
                                        else {
                                            if (intersectionPercentage > 50 && confidence > ((pBox)/100.0f)) {
                                                // Mostrar el porcentaje de intersección
                                                //Log.d("Intersección", "Porcentaje de intersección: " + intersectionPercentage + "%");
                                                cropCanvas.drawText("Intersección Alerta " + String.format("%.2f", intersectionPercentage), 1, previewHeight, textPain2);
                                                //cropCanvas.drawText("Intersección Alerta ", 0, previewHeight, textPain2);
                                            }
                                        }


                                    }



                                } else {
                                    // No hay intersección
                                    // Tu código para manejar la falta de intersección aquí
                                }

                            }
                        }

                        // Define los puntos del polígono
                        float[] points = {rightx1, righty1, rightx2, righty2, leftx2, lefty2, leftx1, lefty1,rightx1, righty1}; // El último punto es el mismo que el primero para cerrar el polígono

                        // Dibuja el polígono en el Canvas
                        path.moveTo(points[0], points[1]);
                        for (int i = 2; i < points.length; i += 2) {
                            path.lineTo(points[i], points[i + 1]);
                        }

                        // Cierra el polígono
                        path.close();

                    }

            /*
            // Define los puntos del polígono
            float[] points = {rightx1, righty1, rightx2, righty2, leftx2, lefty2, leftx1, lefty1,rightx1, righty1}; // El último punto es el mismo que el primero para cerrar el polígono

            // Dibuja el polígono en el Canvas
            path.moveTo(points[0], points[1]);
            for (int i = 2; i < points.length; i += 2) {
                path.lineTo(points[i], points[i + 1]);
            }

            // Cierra el polígono
            path.close();*/
                    if(zona)
                    {
                        cropCanvas.drawPath(path, paint);
                    }

                    //cropCanvas.drawLine(rightx1, righty1, rightx2, righty2, linePaint);
                    //cropCanvas.drawLine(leftx1, lefty1, leftx2, lefty2, linePaint2);
                    //cropCanvas.drawLine(rightx1, righty1, leftx1, lefty1, linePaint);
                    //cropCanvas.drawLine(rightx2, righty2, leftx2, lefty2, linePaint);


                    long end = System.currentTimeMillis();
                    long costTime = (end - start);
                    image.close();
                    emitter.onNext(new Result(costTime, emptyCropSizeBitmap));
//            emitter.onNext(new Result(costTime, imageBitmap));


                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((Result result) -> {
                    boxLabelCanvas.setImageBitmap(result.bitmap);
                    frameSizeTextView.setText(previewHeight + "x" + previewWidth);
                    inferenceTimeTextView.setText(Long.toString(result.costTime) + "ms");
                });

    }
}
