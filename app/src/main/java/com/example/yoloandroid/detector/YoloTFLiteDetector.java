package com.example.yoloandroid.detector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Build;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import com.example.yoloandroid.utils.Recognition;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.nnapi.NnApiDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.metadata.MetadataExtractor;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;


public class YoloTFLiteDetector {

    private Size INPNUT_SIZE = new Size(320, 320);
    private int[] OUTPUT_SIZE = new int[3];
    private final float DETECT_THRESHOLD = 0.05f;//0.25f;
    private final float IOU_THRESHOLD = 0.45f;
    private final float IOU_CLASS_DUPLICATED_THRESHOLD = 0.7f;


    private String MODEL_FILE;
    private Interpreter tflite;
    private List<String> associatedAxisLabels;
    Interpreter.Options options = new Interpreter.Options();

    public String getModelFile() {
        return this.MODEL_FILE;
    }

    public void setModelFile(String modelFile){
        //Se selecciona el modelo a utilizar
        //MODELOS YOLOV8
        String MODEL_YOLOV8M_320 = "yolov8m_320.tflite";
        String MODEL_YOLOV8N_160 = "yolov8n_160.tflite";
        String MODEL_YOLOV8N_320 = "yolov8n_320.tflite";
        String MODEL_YOLOV8S_320 = "yolov8s_320.tflite";
        String MODEL_YOLOV8L_320 = "yolov8l_320.tflite";

        //MODELOS YOLOV8 CON DETECCIÓN DE LINEA
        String MODEL_YOLOV8S_160_LINEA = "YOLOV8s_160_linea.tflite";
        String MODEL_YOLOV8S_320_LINEA = "YOLOV8s_320_linea.tflite";
        String MODEL_YOLOV8M_160_LINEA = "YOLOV8m_160_linea.tflite";
        String MODEL_YOLOV8M_320_LINEA = "YOLOV8m_320_linea.tflite";
        //"yolov8m-seg_float32_320.tflite";//

        switch (modelFile) {
            case "yolov8s-linea-160":
                MODEL_FILE = MODEL_YOLOV8S_160_LINEA;
                INPNUT_SIZE = new Size(160, 160);
                break;

            case "yolov8m-linea-160":
                MODEL_FILE = MODEL_YOLOV8M_160_LINEA;
                INPNUT_SIZE = new Size(160, 160);
                break;

            case "yolov8s-linea-320":
                MODEL_FILE = MODEL_YOLOV8S_320_LINEA;
                INPNUT_SIZE = new Size(320, 320);
                break;

            case "yolov8m-linea-320":
                MODEL_FILE = MODEL_YOLOV8M_320_LINEA;
                INPNUT_SIZE = new Size(320, 320);
                break;

            case "yolov8n-320":
                MODEL_FILE = MODEL_YOLOV8N_320;
                INPNUT_SIZE = new Size(320, 320);
                break;

            case "yolov8s-320":
                MODEL_FILE = MODEL_YOLOV8S_320;
                INPNUT_SIZE = new Size(320, 320);
                break;

            case "yolov8m-320":
                MODEL_FILE = MODEL_YOLOV8M_320;
                INPNUT_SIZE = new Size(320, 320);
                break;

            case "yolov8l-320":
                MODEL_FILE = MODEL_YOLOV8L_320;
                INPNUT_SIZE = new Size(320, 320);
                break;

            case "yolov8n-160":
                MODEL_FILE = MODEL_YOLOV8N_160;
                INPNUT_SIZE = new Size(160, 160);
                break;

            default:
                Log.i("tfliteSupport", "Only yolov8s/n/m/sint8 can be load!");
        }
    }

    public Size getInputSize(){return this.INPNUT_SIZE;}

    public int[] outputShape;

    public void initialModel(Context activity) {
        // Se inicializa el modelo
        try {

            ByteBuffer tfliteModel = FileUtil.loadMappedFile(activity, MODEL_FILE);
            tflite = new Interpreter(tfliteModel, options);
            Log.i("tfliteSupport", "Modelo de lectura de éxito: " + MODEL_FILE);

            // Obtiene el número de tensores de entrada en el modelo
            int inputTensorCount = tflite.getInputTensorCount();//1//1
            int outputTensorCount = tflite.getOutputTensorCount();//1//2


            if (inputTensorCount > 0) {
                // Obtén el primer tensor de entrada (índice 0)
                Tensor inputTensor = tflite.getInputTensor(0);
                int[] inputShape = inputTensor.shape(); // Obtiene la forma del tensor de entrada
                for (int dimension : inputShape) {
                    System.out.println("Dimensión de entrada: " + dimension);
                    //Log.i("tfliteSupport", "Dimensión de entrada:" + dimension);
                }
                //Tensor outputTensor = tflite.getOutputTensor(0);
            }
            outputShape = tflite.getOutputTensor(0).shape();
            OUTPUT_SIZE = outputShape;

            if(OUTPUT_SIZE[2]>OUTPUT_SIZE[1])
            {
                OUTPUT_SIZE = new int[]{OUTPUT_SIZE[0], OUTPUT_SIZE[2], OUTPUT_SIZE[1]};
            }
            // Obtiene la forma del tensor de salida en el índice 0 (si existe al menos un tensor de salida)
            if (outputTensorCount > 0) {
                Tensor outputTensor = tflite.getOutputTensor(0);
                outputShape = outputTensor.shape(); // Obtiene la forma del tensor de salida
                for (int dimension : outputShape) {
                    System.out.println("Dimensión de salida: " + dimension);
                    //Log.i("tfliteSupport", "Dimensión de salida:" + dimension);
                }
            }

            String LABEL_FILE = "coco_label.txt";
            associatedAxisLabels = FileUtil.loadLabels(activity, LABEL_FILE);
            Log.i("tfliteSupport", "Etiqueta de lectura de éxito: " + LABEL_FILE);

        } catch (IOException e) {
            Log.e("tfliteSupport", "Error al leer el modelo o la etiqueta: ", e);
            Toast.makeText(activity, "load model error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public ArrayList<Recognition> detect(Bitmap bitmap) {

        // Asegurarse de que el bitmap tenga el tamaño correcto (320x320) (160x160)
        int targetWidth = INPNUT_SIZE.getWidth();
        int targetHeight = INPNUT_SIZE.getHeight();
        bitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false);

        // Normalizar los valores de píxeles a flotantes en el rango [0, 1]
        float[] floatValues = new float[targetWidth * targetHeight * 3]; // 3 para canales RGB
        for (int i = 0; i < targetWidth; i++) {
            for (int j = 0; j < targetHeight; j++) {
                int pixelValue = bitmap.getPixel(i, j);
                floatValues[(j * targetWidth + i) * 3] = Color.red(pixelValue) / 255.0f;
                floatValues[(j * targetWidth + i) * 3 + 1] = Color.green(pixelValue) / 255.0f;
                floatValues[(j * targetWidth + i) * 3 + 2] = Color.blue(pixelValue) / 255.0f;
            }
        }


        // Configurar la forma del tensor de entrada y salida según los detalles del modelo
        int batchSize = 1;
        int inputChannels = 3;
        int inputHeight = INPNUT_SIZE.getHeight();
        int inputWidth = INPNUT_SIZE.getWidth();

        // Crear los buffers de entrada y salida
        float[][][][] inputBuffer = new float[batchSize][inputHeight][inputWidth][inputChannels];


        // Cargar la imagen en el buffer de entrada
        for (int i = 0; i < inputHeight; i++) {
            for (int j = 0; j < inputWidth; j++) {
                System.arraycopy(floatValues, (i * inputWidth + j) * 3, inputBuffer[0][i][j], 0, inputChannels);
            }
        }

        float[][][] output = new float[OUTPUT_SIZE[0]][OUTPUT_SIZE[2]][OUTPUT_SIZE[1]];  // Output tensor shape is [3, 2].

        Object[] inputArray = {inputBuffer};
        // Crea un mapa para almacenar el arreglo de salida
        Map<Integer, Object> map = new HashMap<>(2);


        // Computación inferencial
        if (null != tflite) {
            try {
                map.put (0, output);
                tflite.runForMultipleInputsOutputs(inputArray,map);

            } catch (Exception e) {
                e.printStackTrace();
                // Maneja las excepciones apropiadamente
                Log.e("MI APP", "Se produjo una excepción: ", e);
            }
        }

        // Extraer la capa interna
        float[][] matrizBidimensional = ((float[][][]) Objects.requireNonNull(map.get(0)))[0];


        // Obtener las dimensiones originales
        int filasOriginales = matrizBidimensional.length;
        int columnasOriginales = matrizBidimensional[0].length;

        // Crear una nueva matriz para almacenar la transposición
        float[][] matrizTranspuesta = new float[columnasOriginales][filasOriginales];

        // Realizar la transposición manualmente
        for (int i = 0; i < filasOriginales; i++) {
            for (int j = 0; j < columnasOriginales; j++) {
                matrizTranspuesta[j][i] = matrizBidimensional[i][j];
            }
        }


        // Obtener el número de filas y columnas originales
        int rows = matrizTranspuesta.length;

        //int columnasOriginales2 = matrizTranspuesta[0].length;//116

        // Crear las matrices boxes y masks
        float[][] boxes2 = new float[rows][84];//[8400,84]//[525,84]

        // Llenar las matrices boxes[8400,84] y masks[8400,32]
        for (int i = 0; i < rows; i++) {
            //System.arraycopy(matrizTranspuesta[i], 0, boxes2[i], 0, 84);
            boxes2[i] = Arrays.copyOf(matrizTranspuesta[i], 84);
        }

        ArrayList<Recognition> allRecognitions = new ArrayList<>();


        for (int i=0; i < rows; i++) {

            float x = boxes2[i][0];
            float y = boxes2[i][1];
            float w = boxes2[i][2];
            float h = boxes2[i][3];


            // Dado que el desarrollador de yolo dividió la salida por el tamaño de la imagen al exportar tflite, debe multiplicarse aquí
            x = x * INPNUT_SIZE.getWidth();
            y = y * INPNUT_SIZE.getHeight();
            w = w * INPNUT_SIZE.getWidth();
            h = h * INPNUT_SIZE.getHeight();

            int xmin = (int) Math.max(0, x - w / 2.);
            int ymin = (int) Math.max(0, y - h / 2.);

            int xmax = (int) Math.min(INPNUT_SIZE.getWidth(), x + w / 2.);
            int ymax = (int) Math.min(INPNUT_SIZE.getHeight(), y + h / 2.);

            float confidence = 0.0F;


            float[] classScores = Arrays.copyOfRange(boxes2[i], 4, (84));
//            if(i % 1000 == 0){
//                Log.i("tfliteSupport","x,y,w,h,conf:"+x+","+y+","+w+","+h+","+confidence);
//            }
            int labelId = 0;
            float maxLabelScores = 0.f;
            for (int j = 0; j < classScores.length; j++) {
                if (classScores[j] > maxLabelScores) {
                    maxLabelScores = classScores[j];
                    labelId = j;
                }
            }


            confidence = maxLabelScores;
            // confidence = outputs[i][4];

            Recognition r = new Recognition(
                    labelId,
                    "",
                    maxLabelScores,
                    confidence,
                    new RectF(xmin, ymin, xmax, ymax));//,
            //polygon);
            allRecognitions.add(
                    r);
        }




//        Log.i("tfliteSupport", "recognize data size: "+allRecognitions.size());

        // salida inhibitoria no máxima
        ArrayList<Recognition> nmsRecognitions = nms(allRecognitions);
        //ArrayList<Recognition> nmsRecognitions = (allRecognitions);
        // La segunda supresión no máxima, filtrando aquellos objetos que reconocen más de dos bordes de objetivo de diferentes categorías para el mismo objetivo
        ArrayList<Recognition> nmsFilterBoxDuplicationRecognitions = nmsAllClass(nmsRecognitions);
        //ArrayList<Recognition> nmsFilterBoxDuplicationRecognitions = (nmsRecognitions);


        // Actualizar información de la etiqueta
        for(Recognition recognition : nmsFilterBoxDuplicationRecognitions){
            int labelId = recognition.getLabelId();
            String labelName = associatedAxisLabels.get(labelId);
            recognition.setLabelName(labelName);
        }

        return nmsFilterBoxDuplicationRecognitions;

    }


    protected ArrayList<Recognition> nms(ArrayList<Recognition> allRecognitions) {
        ArrayList<Recognition> nmsRecognitions = new ArrayList<Recognition>();

        // Atraviesa cada categoría, haz nms debajo de cada categoría
        for (int i = 0; i < OUTPUT_SIZE[2]-4; i++) {
            // Aquí hay una cola para cada categoría, y coloque la puntuación de la etiqueta alta al frente//6300
            PriorityQueue<Recognition> pq =
                    new PriorityQueue<Recognition>(
                            2100,
                            new Comparator<Recognition>() {
                                @Override
                                public int compare(final Recognition l, final Recognition r) {
                                    // Intentionally reversed to put high confidence at the head of the queue.
                                    return Float.compare(r.getConfidence(), l.getConfidence());
                                }
                            });

            // La misma categoría se filtra y obj es mayor que el umbral establecido
            for (int j = 0; j < allRecognitions.size(); ++j) {
//                if (allRecognitions.get(j).getLabelId() == i) {
                if (allRecognitions.get(j).getLabelId() == i && allRecognitions.get(j).getConfidence() > DETECT_THRESHOLD) {
                    pq.add(allRecognitions.get(j));
//                    Log.i("tfliteSupport", allRecognitions.get(j).toString());
                }
            }

            // bucle nms a través
            while (pq.size() > 0) {
                // Saque primero el que tenga la probabilidad más alta
                Recognition[] a = new Recognition[pq.size()];
                Recognition[] detections = pq.toArray(a);
                Recognition max = detections[0];
                nmsRecognitions.add(max);
                pq.clear();

                for (int k = 1; k < detections.length; k++) {
                    Recognition detection = detections[k];
                    if (boxIou(max.getLocation(), detection.getLocation()) < IOU_THRESHOLD) {
                        pq.add(detection);
                    }
                }
            }
        }
        return nmsRecognitions;
    }

    protected ArrayList<Recognition> nmsAllClass(ArrayList<Recognition> allRecognitions) {
        //Realice una supresión no máxima para todos los datos sin distinguir categorías
        ArrayList<Recognition> nmsRecognitions = new ArrayList<Recognition>();

        PriorityQueue<Recognition> pq =
                new PriorityQueue<Recognition>(
                        100,
                        new Comparator<Recognition>() {
                            @Override
                            public int compare(final Recognition l, final Recognition r) {
                                // Intentionally reversed to put high confidence at the head of the queue.
                                return Float.compare(r.getConfidence(), l.getConfidence());
                            }
                        });

        // La misma categoría se filtra y obj es mayor que el umbral establecido
        for (int j = 0; j < allRecognitions.size(); ++j) {
            if (allRecognitions.get(j).getConfidence() > DETECT_THRESHOLD) {
                pq.add(allRecognitions.get(j));
            }
        }

        while (pq.size() > 0) {
            // Saque primero el que tenga la probabilidad más alta
            Recognition[] a = new Recognition[pq.size()];
            Recognition[] detections = pq.toArray(a);
            Recognition max = detections[0];
            nmsRecognitions.add(max);
            pq.clear();

            for (int k = 1; k < detections.length; k++) {
                Recognition detection = detections[k];
                if (boxIou(max.getLocation(), detection.getLocation()) < IOU_CLASS_DUPLICATED_THRESHOLD) {
                    pq.add(detection);
                }
            }
        }
        return nmsRecognitions;
    }

    protected float boxIou(RectF a, RectF b) {
        float intersection = boxIntersection(a, b);
        float union = boxUnion(a, b);
        if (union <= 0) return 1;
        return intersection / union;
    }

    protected float boxIntersection(RectF a, RectF b) {
        float maxLeft = Math.max(a.left, b.left);
        float maxTop = Math.max(a.top, b.top);
        float minRight = Math.min(a.right, b.right);
        float minBottom = Math.min(a.bottom, b.bottom);
        float w = minRight -  maxLeft;
        float h = minBottom - maxTop;

        if (w < 0 || h < 0) return 0;
        return w * h;
    }

    protected float boxUnion(RectF a, RectF b) {
        float i = boxIntersection(a, b);
        return (a.right - a.left) * (a.bottom - a.top) + (b.right - b.left) * (b.bottom - b.top) - i;
    }

    public void addNNApiDelegate() {
        NnApiDelegate nnApiDelegate = null;
        // Inicialice el intérprete con el delegado NNAPI para Android Pie o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            NnApiDelegate.Options nnApiOptions = new NnApiDelegate.Options();
//            nnApiOptions.setAllowFp16(true);
//            nnApiOptions.setUseNnapiCpu(true);
//            nnApiOptions.setExecutionPreference(NnApiDelegate.Options.EXECUTION_PREFERENCE_SUSTAINED_SPEED);
//            nnApiDelegate = new NnApiDelegate(nnApiOptions);
            nnApiDelegate = new NnApiDelegate();
            options.addDelegate(nnApiDelegate);
            Log.i("tfliteSupport", "using nnapi delegate.");
        }
    }

    public void addGPUDelegate() {
        CompatibilityList compatibilityList = new CompatibilityList();
        if(compatibilityList.isDelegateSupportedOnThisDevice()){
            GpuDelegate.Options delegateOptions = compatibilityList.getBestOptionsForThisDevice();
            GpuDelegate gpuDelegate = new GpuDelegate(delegateOptions);
            options.addDelegate(gpuDelegate);
            Log.i("tfliteSupport", "using gpu delegate.");
        } else {
            addThread(4);
        }
    }

    public void addThread(int thread) {
        options.setNumThreads(thread);
    }

}
