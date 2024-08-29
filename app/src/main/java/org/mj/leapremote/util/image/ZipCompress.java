package org.mj.leapremote.util.image;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class ZipCompress {
    /**
     * 解压数据
     *
     * @param bytes 源数据
     * @return 压缩之后的数据
     */
    public static byte[] decompress(byte[] bytes) {
        Inflater inflater = new Inflater();
        //设置待解压数据
        inflater.setInput(bytes);
        byte[] buf = new byte[1024];
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            //是否已解压完成
            while (!inflater.finished()) {
                //解压
                int len = inflater.inflate(buf);
                output.write(buf, 0, len);
            }
            //关闭资源
            inflater.end();
            return output.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    /**
     * 压缩数据
     *
     * @param bytes 源数据
     * @return 压缩之后的数据
     */
    public static byte[] compress(byte[] bytes) {
        System.out.println("ORIGIN:"+bytes.length);
        Deflater deflater = new Deflater();
        //设置待压缩数据
        deflater.setInput(bytes);
        //表示压缩以当前输入内容结束，暂时不知道具体原理
        deflater.finish();
        byte[] buf = new byte[1024];
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            //是否已压缩完成
            while (!deflater.finished()) {
                //压缩
                int len = deflater.deflate(buf);
                output.write(buf, 0, len);
            }
            //关闭资源
            deflater.end();
            System.out.println("OUT:"+output.size());
            return output.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

}
