package org.mj.leapremote.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;

import java.io.ByteArrayOutputStream;

public class ImageUtils {

    public static String byteArrayToString(byte[] bytes){
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] stringToByteArray(String str){
        return Base64.getDecoder().decode(str);
    }

    public static Bitmap byteArrayToBitmap(byte[] b) {
        return BitmapFactory.decodeByteArray(b, 0, b.length);
    }

    public static byte[] bitmapToByteArray(Bitmap b) {
        return bitmapToByteArray(b, 100);
    }

    public static byte[] bitmapToByteArray(Bitmap b, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        return baos.toByteArray();
    }

    static public Bitmap compressBitmap(Bitmap beforeBitmap, double maxWidth, double maxHeight) {

        // 图片原有的宽度和高度
        float beforeWidth = beforeBitmap.getWidth();
        float beforeHeight = beforeBitmap.getHeight();
        /*if (beforeWidth <= maxWidth && beforeHeight <= maxHeight) {
            return beforeBitmap;
        }*/

        // 计算宽高缩放率，等比例缩放
        float scaleWidth =  ((float) maxWidth) / beforeWidth;
        float scaleHeight = ((float)maxHeight) / beforeHeight;
        float scale = scaleWidth;
        if (scaleWidth > scaleHeight) {
            scale = scaleHeight;
        }
        //Log.d("BitmapUtils", "before[" + beforeWidth + ", " + beforeHeight + "] max[" + maxWidth + ", " + maxHeight + "] scale:" + scale);

        // 矩阵对象
        Matrix matrix = new Matrix();
        // 缩放图片动作 缩放比例
        matrix.postScale(scale, scale);
        // 创建一个新的Bitmap 从原始图像剪切图像
        return Bitmap.createBitmap(beforeBitmap, 0, 0,
                (int) beforeWidth, (int) beforeHeight, matrix, true);
    }

    static public Bitmap compressBitmap(Bitmap beforeBitmap, float scale) {
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        return Bitmap.createBitmap(beforeBitmap, 0, 0,
                beforeBitmap.getWidth(), beforeBitmap.getHeight(), matrix, true);
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int state) {
        int degree;
        switch (state) {
            case 1:
                degree=90;
                break;
            case 2:
                degree=180;
                break;
            case 3:
                degree=270;
                break;
            default:
                return bitmap;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap result = Bitmap.createBitmap(state==2?width:height, state==2?height:width, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(result);
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        switch (state) {
            case 1:
                matrix.postTranslate(height, 0);
                break;
            case 2:
                matrix.postTranslate(width, height);
                break;
            case 3:
                matrix.postTranslate(0, width);
                break;
        }
        c.drawBitmap(bitmap, matrix, new Paint());
        bitmap.recycle();
        return result;
    }
}
