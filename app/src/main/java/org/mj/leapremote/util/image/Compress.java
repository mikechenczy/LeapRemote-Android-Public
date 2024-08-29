package org.mj.leapremote.util.image;

import android.graphics.Bitmap;

import org.mj.leapremote.util.BitmapUtil;

import java.util.HashMap;
import java.util.Map;

public class Compress {
    public int width;
    public int height;
    public byte[][] previous;
    public Map<Integer, byte[]> send;
    public boolean compressed;
    public boolean changedSize;

    public Compress(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Compress() {
    }

    public byte[] compress(Bitmap bitmap) {
        if(bitmap.getWidth()!=width || bitmap.getHeight()!=height) {
            changedSize = true;
            width = bitmap.getWidth();
            height = bitmap.getHeight();
            previous = null;
            compressed = false;
            return asResult(BitmapUtil.getRGB(bitmap));
        }
        changedSize = false;
        return compress(BitmapUtil.getRGB(bitmap));
    }

    public byte[] compress(byte[][] bytes) {
        if(previous==null) {
            previous = bytes;
            compressed = false;
            return asResult(bytes);
        }
        send = new HashMap<>();
        for(int i=0;i<height;i++) {
            for(int j=0;j<width;j++) {
                int index = getIndexFromXY(j, i);
                if(checkTheSame(previous[index], bytes[index])) {
                    continue;
                }
                send.put(index, bytes[index]);
            }
        }
        previous = bytes;
        if(send.size()*7>bytes.length*3) {
            compressed = false;
            return asResult(bytes);
        }
        compressed = true;
        return asResult(send);
    }

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

    public byte[] asResult(Map<Integer, byte[]> bytes) {
        byte[] result = new byte[bytes.size()*7];
        for(int i : bytes.keySet()) {
            System.arraycopy(intToBytes(i), 0, result, i * 7, 4);
            System.arraycopy(bytes.get(i), 0, result, i * 7 + 1, 3);
        }
        return result;
    }

    /**
     * 将int数值转换为占四个字节的byte数组，本方法适用于(低位在前，高位在后)的顺序。 和bytesToInt（）配套使用
     * @param value
     *            要转换的int值
     * @return byte数组
     */
    public static byte[] intToBytes(int value) {
        byte[] src = new byte[4];
        src[3] =  (byte) ((value>>24) & 0xFF);
        src[2] =  (byte) ((value>>16) & 0xFF);
        src[1] =  (byte) ((value>>8) & 0xFF);
        src[0] =  (byte) (value & 0xFF);
        return src;
    }
}
