package com.example.yoloandroid.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;
import android.util.Size;

import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;

import org.tensorflow.lite.DataType;

import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.nio.ByteBuffer;


public class ImageProcess {


    public void fillBytes(final ImageProxy.PlaneProxy[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    public int YUV2RGB(int y, int u, int v) {
        // Adjust and check YUV values
        y = Math.max((y - 16), 0);
        u -= 128;
        v -= 128;

        // This is the floating point equivalent. We do the conversion in integer
        // because some Android devices do not have floating point in hardware.
        // nR = (int)(1.164 * nY + 2.018 * nU);
        // nG = (int)(1.164 * nY - 0.813 * nV - 0.391 * nU);
        // nB = (int)(1.164 * nY + 1.596 * nV);
        int y1192 = 1192 * y;
        int r = (y1192 + 1634 * v);
        int g = (y1192 - 833 * v - 400 * u);
        int b = (y1192 + 2066 * u);

        // Ajustar los valores RGB para que estén dentro de los límites [0, kMaxChannelValue]
        int kMaxChannelValue = 262143;
        r = r > kMaxChannelValue ? kMaxChannelValue : (Math.max(r, 0));
        g = g > kMaxChannelValue ? kMaxChannelValue : (Math.max(g, 0));
        b = b > kMaxChannelValue ? kMaxChannelValue : (Math.max(b, 0));

        return 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
    }

    public void YUV420ToARGB8888(
            byte[] yData,
            byte[] uData,
            byte[] vData,
            int width,
            int height,
            int yRowStride,
            int uvRowStride,
            int uvPixelStride,
            int[] out) {
        int yp = 0;
        for (int j = 0; j < height; j++) {
            int pY = yRowStride * j;
            int pUV = uvRowStride * (j >> 1);

            for (int i = 0; i < width; i++) {
                int uv_offset = pUV + (i >> 1) * uvPixelStride;

                out[yp++] = YUV2RGB(0xff & yData[pY + i], 0xff & uData[uv_offset], 0xff & vData[uv_offset]);
            }
        }
    }

    public Matrix getTransformationMatrix(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation,
            final boolean maintainAspectRatio) {
        final Matrix matrix = new Matrix();

        if (applyRotation != 0) {
            if (applyRotation % 90 != 0) {
                Log.e("Rotation", "Rotation != 90°, got: " + Integer.toString(applyRotation));
            }

            // Traduce de modo que el centro de la imagen esté en el origen.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

            // Girar alrededor del origen.
            matrix.postRotate(applyRotation);
        }

        // Toma en cuenta la rotación ya aplicada, si la hay, y luego determina
        // cuanta escala se necesita para cada eje.
        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;

        final int inWidth = transpose ? srcHeight : srcWidth;
        final int inHeight = transpose ? srcWidth : srcHeight;

        // aplica escala si es necesario.
        if (inWidth != dstWidth || inHeight != dstHeight) {
            final float scaleFactorX = dstWidth / (float) inWidth;
            final float scaleFactorY = dstHeight / (float) inHeight;

            if (maintainAspectRatio) {
                //Escale por factor mínimo para que dst se llene por completo manteniendo
                // la relación de aspecto. Es posible que alguna imagen se caiga del borde.
                final float scaleFactor = Math.max(scaleFactorX, scaleFactorY);
                matrix.postScale(scaleFactor, scaleFactor);
            } else {
                // Escale exactamente para llenar dst desde src.
                matrix.postScale(scaleFactorX, scaleFactorY);
            }
        }

        if (applyRotation != 0) {
            // Traducir desde la referencia centrada en el origen al marco de destino.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;
    }

    //
    public static TensorImage resizeImage(Bitmap bitmap, Size size) {
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeOp(size.getHeight(), size.getWidth(), ResizeOp.ResizeMethod.BILINEAR))
                        .build();

        TensorImage tImage = new TensorImage(DataType.UINT8);


        tImage.load(bitmap);
        tImage = imageProcessor.process(tImage);

        TensorBuffer probabilityBuffer =
                TensorBuffer.createFixedSize(new int[]{1, 1001}, DataType.FLOAT32);

        return tImage;
    }
}
