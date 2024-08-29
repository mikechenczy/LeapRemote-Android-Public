package org.mj.leapremote.coder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.view.Surface;
import android.widget.LinearLayout;

import org.mj.leapremote.ui.activities.ControlActivity;
import org.mj.leapremote.util.ExtractMpegFramesTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ScreenDecoder {

    public static int VIDEO_WIDTH = 2160;
    public static int VIDEO_HEIGHT = 3840;
    public static int WIDTH = 2160;
    public static int HEIGHT = 3840;
    public static long DECODE_TIME_OUT = 10000;
    public static int ROTATION;
    public static MediaCodec mMediaCodec;
    public static OnDecodeListener onDecode;
    public static boolean hevc;
    public static Integer index = 0;
    private static Thread decodeThread;
    public static Boolean isDecoding = false;

    public static boolean portrait;

    public static int resolution = 50;
    private static Thread decodeInputThread;

    public static void updateResolution(int progress) {
        System.out.println("PORTRAIT:"+portrait+VIDEO_WIDTH +" "+VIDEO_HEIGHT);
        boolean fakePortrait = portrait;
        if(VIDEO_WIDTH > VIDEO_HEIGHT) {
            fakePortrait = !portrait;
        }
        resolution = progress;
        float sizeResolution = (float)Math.pow(resolution,5)*1/7680000-(float)Math.pow(resolution,4)*109/3840000+(float)Math.pow(resolution,3)*13/6000-(float)Math.pow(resolution,2)*599/9600+(float)resolution*51/80+30;
        WIDTH = Math.round((float)((fakePortrait?VIDEO_WIDTH:VIDEO_HEIGHT) * sizeResolution / 100));
        HEIGHT = Math.round((float)((fakePortrait?VIDEO_HEIGHT:VIDEO_WIDTH) * sizeResolution / 100));
        if(WIDTH % 2 == 1) {
            WIDTH -= 1;
        }
        if(HEIGHT % 2 == 1) {
            HEIGHT -= 1;
        }
        System.out.println(WIDTH);
        System.out.println(HEIGHT);
    }

    public static abstract class OnDecodeListener {
        public abstract void onDecode(Bitmap bitmap);
    }

    public static void startDecode(Surface surface) {
        System.out.println("HEVC"+hevc);
        if(mMediaCodec!=null) {
            stopDecode();
        }
        //MainActivity.INSTANCE.runOnUiThread(() -> {
        //outputSurface = new ExtractMpegFramesTest.CodecOutputSurface(VIDEO_WIDTH, VIDEO_HEIGHT);
        //new Thread(() -> {
        synchronized (isDecoding) {
            try {
                mMediaCodec = MediaCodec.createDecoderByType(hevc ? MediaFormat.MIMETYPE_VIDEO_HEVC : MediaFormat.MIMETYPE_VIDEO_AVC);
                synchronized (mMediaCodec) {
                    // 配置MediaCodec
                    MediaFormat mediaFormat =
                            MediaFormat.createVideoFormat(hevc ? MediaFormat.MIMETYPE_VIDEO_HEVC : MediaFormat.MIMETYPE_VIDEO_AVC, WIDTH, HEIGHT);
                    mediaFormat.setInteger(MediaFormat.KEY_ROTATION, ROTATION);
                    //mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 500);//VIDEO_WIDTH * VIDEO_HEIGHT);
                    //mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, SCREEN_FRAME_RATE);
                    //mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, SCREEN_FRAME_INTERVAL);


                    mediaFormat.setInteger(
                            MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                    //mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    //        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
                    float frameRateResolution = (float)Math.pow(resolution,3)*211/1200000-(float)Math.pow(resolution,2)*951/40000+(float)resolution*1187/1200+8;
                    float bitRateResolution = resolution;
                    System.out.println(frameRateResolution);
                    System.out.println(bitRateResolution);
                    // 帧率
                    mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, Math.round(frameRateResolution));
                    // 比特率（比特/秒）
                    mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, Math.round(VIDEO_WIDTH*VIDEO_HEIGHT*mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE)*bitRateResolution/100));//Math.round((float)recordWidth * recordHeight * resolution / 100));
                    // I帧的频率
                    mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);




                    mMediaCodec.configure(mediaFormat, surface, null, 0);
                    mMediaCodec.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            isDecoding = true;
        }
        decodeInputThread = new Thread() {
            @Override
            public void run() {
                while (isDecoding && !interrupted()) {
                    synchronized (dataQueue) {
                        if(dataQueue.size()>0) {
                            System.out.println(dataQueue.size());
                            try {
                                int inputBufferId = mMediaCodec.dequeueInputBuffer(DECODE_TIME_OUT);
                                if (inputBufferId >= 0) {
                                    ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferId);
                                    inputBuffer.clear();
                                    inputBuffer.put(dataQueue.get(0), 0, dataQueue.get(0).length);
                                    mMediaCodec.queueInputBuffer(inputBufferId, 0, dataQueue.get(0).length, System.currentTimeMillis(), 0);
                                }
                                dataQueue.remove(0);
                            } catch (Exception e) {
                                //e.printStackTrace();
                            }
                        }
                    }
                }
            }};
        decodeInputThread.start();
        decodeThread = new Thread() {
            @Override
            public void run() {
                System.out.println("isDecoding && !interrupted(): "+isDecoding+" "+!interrupted());
                while (isDecoding && !interrupted()) {
                    if(mMediaCodec==null)
                        return;
                    try {
                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                        int outputBufferId = mMediaCodec.dequeueOutputBuffer(bufferInfo, DECODE_TIME_OUT);
                        if (outputBufferId >= 0) {
                            /*if(onDecode!=null) {
                                long start = System.currentTimeMillis();
                                Image image = mMediaCodec.getOutputImage(outputBufferId);
                                byte[] data = getBytesFromImageAsType(image);
                                System.out.println("Time used: "+(System.currentTimeMillis() - start));
                                new Thread(() -> {
                                    System.out.println("OUT" + outputBufferId);
                                    Bitmap bitmap = getBitmapFromYUV(data, VIDEO_WIDTH, VIDEO_HEIGHT, 100, 0);
                                    onDecode.onDecode(bitmap);
                                }).start();
                            }*/
                            if(ControlActivity.INSTANCE!=null) {
                                ControlActivity.INSTANCE.calculateSize(WIDTH, HEIGHT);
                            }
                            mMediaCodec.releaseOutputBuffer(outputBufferId, true);
                        } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            // Subsequent data will conform to new format.
                            // Can ignore if using getOutputFormat(outputBufferId)
                            //outputFormat = codec.getOutputFormat(); // option B
                            System.out.println(mMediaCodec.getInputFormat());
                            System.out.println("AAAA");
                            System.out.println();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        decodeThread.start();
        System.out.println("DECODE THREAD STARTED, Surface:"+surface);
    }

    public static List<byte[]> dataQueue = new ArrayList<>();

    public static Map<Integer, byte[]> laidData = new HashMap<>();

    public static void decodeData(byte[] data, int index) {
        if(index==0) {
            dataQueue.clear();
            laidData.clear();
        }
        if(ScreenDecoder.index >= index) {
            ScreenDecoder.index = index;
            forceDecodeData(data);
            checkLaidData();
            return;
        }
        if(ScreenDecoder.index+1 == index) {
            ScreenDecoder.index += 1;
            forceDecodeData(data);
            checkLaidData();
            return;
        }
        laidData.put(index, data);
    }

    private static void checkLaidData() {
        if(laidData.containsKey(index+1)) {
            index += 1;
            forceDecodeData(laidData.get(index));
            laidData.remove(index);
            checkLaidData();
        }
    }

    public static void forceDecodeData(byte[] data) {
        synchronized (dataQueue) {
            dataQueue.add(data);
        }
    }

    public static void stopDecode() {
        System.out.println("STOPPED");
        isDecoding = false;
        if(decodeThread!=null)
            decodeThread.interrupt();
        decodeThread = null;
        if(decodeInputThread!=null)
            decodeInputThread.interrupt();
        decodeInputThread = null;
        synchronized (mMediaCodec) {
            if (mMediaCodec != null) {
                try {
                    //mMediaCodec.signalEndOfInputStream();
                    mMediaCodec.stop();
                    mMediaCodec.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mMediaCodec = null;
        }
    }
}