package org.mj.leapremote.util.image;

import android.graphics.Bitmap;

import org.mj.leapremote.util.BitmapUtil;

import java.util.HashMap;
import java.util.Map;

public class Depress {
    public static Depress instance;
    public int width;
    public int height;
    public byte[][] previous;
    public Depress(int width, int height) {
        instance = this;
        this.width = width;
        this.height = height;
    }

    public Depress() {
        instance = this;
    }

    public void changeWidthAndHeight(int width, int height) {
        this.width = width;
        this.height = height;
        previous = null;
    }

    public Bitmap depress(boolean compressed, byte[] bytes) {
        if(compressed) {
            return BitmapUtil.rgb2Bitmap(asResult(asSource(asSourceCompressed(bytes))), width, height);
        }
        return BitmapUtil.rgb2Bitmap(bytes, width, height);
    }

    private Map<Integer, byte[]> asSourceCompressed(byte[] bytes) {
        Map<Integer, byte[]> result = new HashMap<>();
        for(int i=0;i<bytes.length/7;i++) {
            byte[] integer = new byte[4];
            for(int j=0;j<4;j++) {
                integer[j] = bytes[i*7+j];
            }
            byte[] rgb = new byte[3];
            for(int j=0;j<3;j++) {
                rgb[j] = bytes[i*7+4+j];
            }
            result.put(bytesToInt(integer), rgb);
        }
        return result;
    }

    public byte[][] asSource(Map<Integer, byte[]> bytes) {
        for(int i:bytes.keySet()) {
            previous[i] = bytes.get(i);
        }
        return previous;
    }

    /*private byte[][] asSource(byte[] bytes) {
        previous = new byte[3][bytes.length/3];
        for(int i=0;i<previous.length;i++) {
            for (int j=0;j<3;j++) {
                previous[i][j] = bytes[i*3+j];
            }
        }
        return previous;
    }*/


    public int getIndexFromXY(int x, int y) {
        return width*y+x;
    }

    public boolean checkTheSame(byte[] rgb0, byte[] rgb1) {
        return rgb0[0]==rgb1[0] &&
                rgb0[1] == rgb1[1] &&
                rgb0[2] == rgb1[2];
    }

    public byte[] asResult(byte[][] bytes) {
        byte[] result = new byte[bytes.length*3];
        for(int i=0;i<bytes.length;i++) {
            System.arraycopy(bytes[i], 0, result, i * 3, 3);
        }
        return result;
    }

    public static int bytesToInt(byte[] src) {
        return bytesToInt(src, 0);
    }

    /**
     * byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序，和和intToBytes（）配套使用
     *
     * @param src
     *            byte数组
     * @param offset
     *            从数组的第offset位开始
     * @return int数值
     */
    public static int bytesToInt(byte[] src, int offset) {
        int value;
        value = (int) ((src[offset] & 0xFF)
                | ((src[offset+1] & 0xFF)<<8)
                | ((src[offset+2] & 0xFF)<<16)
                | ((src[offset+3] & 0xFF)<<24));
        return value;
    }
}
