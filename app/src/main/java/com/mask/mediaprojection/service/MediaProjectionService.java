package com.mask.mediaprojection.service;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;

import com.mask.mediaprojection.interfaces.MediaProjectionNotificationEngine;
import com.mask.mediaprojection.interfaces.MediaCodecCallback;
import com.mask.mediaprojection.interfaces.ScreenCaptureCallback;
import com.mask.mediaprojection.utils.MediaProjectionHelper;

import org.mj.leapremote.ui.activities.MainActivity;
import org.mj.leapremote.util.ClientHelper;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 媒体投影 Service
 * Created by lishilin on 2020/03/19
 */
public class MediaProjectionService extends Service {

    private static final int ID_MEDIA_PROJECTION = MediaProjectionHelper.REQUEST_CODE;

    private DisplayMetrics displayMetrics;
    private boolean isScreenCaptureEnable;// 是否可以屏幕截图
    private boolean isMediaCodecEnable;// 是否可以媒体录制

    private MediaProjectionManager mediaProjectionManager;
    public MediaProjection mediaProjection;

    private VirtualDisplay virtualDisplayImageReader;
    private ImageReader imageReader;
    private boolean isImageAvailable;

    private VirtualDisplay virtualDisplayMediaRecorder;
    private MediaCodec mediaCodec;
    private File mediaFile;
    private boolean isMediaRecording;
    private MediaCodecCallback mediaCodecCallback;

    private MediaProjectionNotificationEngine notificationEngine;
    private boolean loopCapture;
    private Thread encodeThread;
    public boolean hevc;
    //private Surface mSurface;

    public class MediaProjectionBinder extends Binder {

        public MediaProjectionService getService() {
            return MediaProjectionService.this;
        }

    }

    /**
     * 绑定Service
     *
     * @param context           context
     * @param serviceConnection serviceConnection
     */
    public static void bindService(Context context, ServiceConnection serviceConnection) {
        Intent intent = new Intent(context, MediaProjectionService.class);
        context.bindService(intent, serviceConnection, Service.BIND_AUTO_CREATE);
    }

    /**
     * 解绑Service
     *
     * @param context           context
     * @param serviceConnection serviceConnection
     */
    public static void unbindService(Context context, ServiceConnection serviceConnection) {
        context.unbindService(serviceConnection);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MediaProjectionBinder();
    }

    @Override
    public void onDestroy() {
        destroy();
        super.onDestroy();
    }

    /**
     * 销毁
     */
    private void destroy() {
        stopImageReader();

        stopMediaRecorder();

        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }

        if (mediaProjectionManager != null) {
            mediaProjectionManager = null;
        }

        stopForeground(true);
    }

    /**
     * 结束 屏幕截图
     */
    private void stopImageReader() {
        isImageAvailable = false;

        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }

        if (virtualDisplayImageReader != null) {
            virtualDisplayImageReader.release();
            virtualDisplayImageReader = null;
        }
    }

    /**
     * 结束 媒体录制
     */
    private void stopMediaRecorder() {
        stopRecording();

        if (virtualDisplayMediaRecorder != null) {
            virtualDisplayMediaRecorder.release();
            virtualDisplayMediaRecorder = null;
        }
    }

    /**
     * 显示通知栏
     */
    private void showNotification() {
        if (notificationEngine == null) {
            return;
        }

        Notification notification = notificationEngine.getNotification();

        startForeground(ID_MEDIA_PROJECTION, notification);
    }

    public boolean isHevc() {
        return hevc;
    }

    /**
     * 创建 屏幕截图
     */
    private void createImageReader() {
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        int densityDpi = displayMetrics.densityDpi;

        imageReader = ImageReader.newInstance(Math.max(width, height), Math.max(width, height), PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(null, null);
        /*imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                capture(reader, new ScreenCaptureCallback() {
                    @Override
                    public void onSuccess(Bitmap bitmap) {
                        System.err.println("Handle Bitmap");
                        if(bitmapToVideoEncoder==null)
                            return;
                        new Thread(() -> {
                            bitmapToVideoEncoder.queueFrame(bitmap);
                        }).start();
                    }
                });
            }
        }, null);*/
        /*imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                System.out.println("DDDD"+isImageAvailable);
                isImageAvailable = true;
            }
        }, null);*/

        virtualDisplayImageReader = mediaProjection.createVirtualDisplay("ScreenCapture",
                Math.max(width, height), Math.max(width, height), densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);
    }

    public boolean checkHevcAvailable() {
        try {
            int width = displayMetrics.widthPixels;
            int height = displayMetrics.heightPixels;
            MediaFormat mediaFormat =
                    MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height);
            mediaFormat.setInteger(
                    MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            //mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
            //        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
            // 比特率（比特/秒）
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 500);
            // 帧率
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 10);
            // I帧的频率
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            MediaCodec mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
            mediaCodec.stop();
            mediaCodec.release();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }



    /**
     * 创建 媒体录制
     */
    private void createMediaRecorder() {
        hevc = checkHevcAvailable();
        hevc = false;
        System.out.println("HEVC:"+hevc);
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        int densityDpi = displayMetrics.densityDpi;
        portrait = getResources().getConfiguration().orientation==Configuration.ORIENTATION_PORTRAIT;

        // 创建保存路径
        //final File dirFile = FileUtils.getCacheMovieDir(this);
        //boolean mkdirs = dirFile.mkdirs();
        // 创建保存文件
        // mediaFile = new File(dirFile, FileUtils.getDateName("MediaRecorder") + ".mp4");
        int recordWidth = width;
        int recordHeight = height;
        if(recordWidth>recordHeight) {
            recordWidth = height;
            recordHeight = width;
        }
        float sizeResolution = (float)Math.pow(resolution,5)*1/7680000-(float)Math.pow(resolution,4)*109/3840000+(float)Math.pow(resolution,3)*13/6000-(float)Math.pow(resolution,2)*599/9600+(float)resolution*51/80+30;
        recordWidth = Math.round((float)(recordWidth * sizeResolution / 100));
        recordHeight = Math.round((float)(recordHeight * sizeResolution / 100));
        if(recordWidth % 2 == 1) {
            recordWidth -= 1;
        }
        if(recordHeight % 2 == 1) {
            recordHeight -= 1;
        }
        MediaFormat mediaFormat =
                MediaFormat.createVideoFormat(hevc?MediaFormat.MIMETYPE_VIDEO_HEVC:MediaFormat.MIMETYPE_VIDEO_AVC,
                        portrait ?recordWidth:recordHeight, portrait ?recordHeight:recordWidth);
        mediaFormat.setInteger(
                MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        //mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
        //        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
        float frameRateResolution = (float)Math.pow(resolution,3)*211/1200000-(float)Math.pow(resolution,2)*951/40000+(float)resolution*1187/1200+8;
        float bitRateResolution = resolution;
        System.out.println(sizeResolution);
        System.out.println(frameRateResolution);
        System.out.println(bitRateResolution);
        // 帧率
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, Math.round(frameRateResolution));
        // 比特率（比特/秒）
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, Math.round(recordWidth*recordHeight*mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE)*bitRateResolution/100));//Math.round((float)recordWidth * recordHeight * resolution / 100));
        // I帧的频率
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        /*try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            //mSurface = mediaCodec.createInputSurface();
            mediaCodec.start();
            System.out.println("STARTED");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }*/
        if (virtualDisplayMediaRecorder == null) {
            try {
                mediaCodec = MediaCodec.createEncoderByType(hevc?MediaFormat.MIMETYPE_VIDEO_HEVC:MediaFormat.MIMETYPE_VIDEO_AVC);
                mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                virtualDisplayMediaRecorder = mediaProjection.createVirtualDisplay(
                        "screen",
                        portrait ?recordWidth:recordHeight,
                        portrait ?recordHeight:recordWidth,
                        densityDpi,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        mediaCodec.createInputSurface(),
                        null,
                        null);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } else {
            try {
                mediaCodec = MediaCodec.createEncoderByType(hevc?MediaFormat.MIMETYPE_VIDEO_HEVC:MediaFormat.MIMETYPE_VIDEO_AVC);
                mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                virtualDisplayMediaRecorder.setSurface(mediaCodec.createInputSurface());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
        // 调用顺序不能乱
        /*mediaCodec = new MediaCodec();
        mediaCodec.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaCodec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaCodec.setOutputFile(mediaFile.getAbsolutePath());
        mediaCodec.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaCodec.setVideoSize(width, height);
        mediaCodec.setVideoFrameRate(30);
        mediaCodec.setVideoEncodingBitRate(5 * width * height);

        mediaCodec.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                if (mediaCodecCallback != null) {
                    mediaCodecCallback.onFail();
                }
            }
        });

        try {
            mediaCodec.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (virtualDisplayMediaRecorder == null) {
            virtualDisplayMediaRecorder = mediaProjection.createVirtualDisplay("MediaRecorder",
                    width, height, densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mediaCodec.getSurface(), null, null);
        } else {
            virtualDisplayMediaRecorder.setSurface(mediaCodec.getSurface());
        }*/
    }

    /**
     * 设置 通知引擎
     *
     * @param notificationEngine notificationEngine
     */
    public void setNotificationEngine(MediaProjectionNotificationEngine notificationEngine) {
        this.notificationEngine = notificationEngine;
    }

    /**
     * 创建VirtualDisplay
     *
     * @param resultCode            resultCode
     * @param data                  data
     * @param displayMetrics        displayMetrics
     * @param isScreenCaptureEnable 是否可以屏幕截图
     * @param isMediaRecorderEnable 是否可以媒体录制
     */
    public void createVirtualDisplay(int resultCode, Intent data, DisplayMetrics displayMetrics, boolean isScreenCaptureEnable, boolean isMediaRecorderEnable) {
        this.displayMetrics = displayMetrics;
        this.isScreenCaptureEnable = isScreenCaptureEnable;
        this.isMediaCodecEnable = isMediaRecorderEnable;

        if (data == null) {
            stopSelf();
            return;
        }

        showNotification();

        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mediaProjectionManager == null) {
            stopSelf();
            return;
        }

        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            stopSelf();
            return;
        }

        if (isScreenCaptureEnable) {
            createImageReader();
        }
    }

    public void capture(ScreenCaptureCallback callback) {
        if (!isScreenCaptureEnable) {
            callback.onFail(1);
            return;
        }
        if (imageReader == null) {
            callback.onFail(2);
            return;
        }
        //if (!isImageAvailable) {
        //    callback.onFail(3);
        //    return;
        //}
        Image image = imageReader.acquireLatestImage();
        if (image == null) {
            callback.onFail(4);
            return;
        }

        // 获取数据
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane plane = image.getPlanes()[0];
        final ByteBuffer buffer = plane.getBuffer();

        // 重新计算Bitmap宽度，防止Bitmap显示错位
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        int bitmapWidth = width + rowPadding / pixelStride;

        // 创建Bitmap
        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        // 释放资源
        image.close();

        // 裁剪Bitmap，因为重新计算宽度原因，会导致Bitmap宽度偏大
        //Bitmap result = Bitmap.createBitmap(bitmap, 0, 0, width, height);
        //result.setConfig(Bitmap.Config.RGB_565);
        //bitmap.recycle();

        callback.onSuccess(result(bitmap, displayMetrics.widthPixels, displayMetrics.heightPixels, getResources().getConfiguration().orientation));
    }

    public void capture(ImageReader imageReader, ScreenCaptureCallback callback) {
        if (!isScreenCaptureEnable) {
            callback.onFail(1);
            return;
        }
        if (imageReader == null) {
            callback.onFail(2);
            return;
        }
        //if (!isImageAvailable) {
        //    callback.onFail(3);
        //    return;
        //}
        System.out.println("ACQUIRING");
        Image image = imageReader.acquireLatestImage();
        System.out.println("ACQUIRED");
        if (image == null) {
            callback.onFail(4);
            return;
        }

        // 获取数据
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane plane = image.getPlanes()[0];
        final ByteBuffer buffer = plane.getBuffer();

        // 重新计算Bitmap宽度，防止Bitmap显示错位
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        int bitmapWidth = width + rowPadding / pixelStride;

        // 创建Bitmap
        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        // 释放资源
        image.close();

        // 裁剪Bitmap，因为重新计算宽度原因，会导致Bitmap宽度偏大
        //Bitmap result = Bitmap.createBitmap(bitmap, 0, 0, width, height);
        //result.setConfig(Bitmap.Config.RGB_565);
        //bitmap.recycle();

        callback.onSuccess(result(bitmap, displayMetrics.widthPixels, displayMetrics.heightPixels, getResources().getConfiguration().orientation));
    }

    public Bitmap result(Bitmap bmpOriginal, int width, int height, int orientation) {
        //int width, height;
        //height = bmpOriginal.getHeight();
        //width = bmpOriginal.getWidth();
        int square = Math.max(width, height);
        boolean phone = height>=width;
        boolean changed = (orientation==Configuration.ORIENTATION_LANDSCAPE&&height>=width) || (orientation==Configuration.ORIENTATION_PORTRAIT&&width>height);
        //Bitmap cropAndRotate = Bitmap.createBitmap(bmpOriginal, changed?(square-height)/2:(square-width)/2, changed?(square-width)/2:(square-height)/2, width, height, matrix, false);
        //bmpOriginal.recycle();
        Bitmap rgb565 = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(rgb565);
        Paint paint = new Paint();
        //ColorMatrix cm = new ColorMatrix();
        //cm.setSaturation(0);
        //ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        //paint.setColorFilter(f);
        Matrix matrix = new Matrix();
        matrix.postRotate(changed?90:0);
        if(phone) {
            matrix.postTranslate(changed?(square-width)/2+width:-(square-width)/2, 0);
        } else {
            matrix.postTranslate(changed?square:0, -(square-height)/2);
        }
        c.drawBitmap(bmpOriginal, matrix, paint);
        bmpOriginal.recycle();
        return rgb565;
    }

    private boolean portrait;


    public int resolution = 50;

    /**
     * 开始 媒体录制
     *
     * @param callback callback
     */
    public void startRecording(MediaCodecCallback callback, int resolution) {
        if(resolution!=-1) {
            this.resolution = resolution;
        }
        this.mediaCodecCallback = callback;
        if (!isMediaCodecEnable) {
            if (mediaCodecCallback != null) {
                mediaCodecCallback.onFail();
            }
            return;
        }
        if (isMediaRecording) {
            if (mediaCodecCallback != null) {
                mediaCodecCallback.onFail();
            }
            return;
        }

        //isMediaRecording = true;

        createMediaRecorder();

        isMediaRecording = true;

        /*new Thread(() -> {
            while (isMediaRecording) {
                        capture(new ScreenCaptureCallback() {
                            @Override
                            public void onSuccess(Bitmap bitmap) {
                                System.out.println("SUCCESS");
                                int inputIndex = mediaCodec.dequeueInputBuffer(10000);
                                if (inputIndex >= 0) {
                                    System.out.println("DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD" + inputIndex);
                                    long pts = System.nanoTime() / 1000;
                                    //Surface surface = mSurface;//mediaCodec.createInputSurface();
                                    //Canvas canvas = surface.lockCanvas(null);
                                    //canvas.drawBitmap(bitmap, new Matrix(), null);
                                    //surface.unlockCanvasAndPost(canvas);
                                    mediaCodec.queueInputBuffer(inputIndex, 0, 0, pts, 0);
                                }
                                //bitmap.recycle();
                            }

                            @Override
                            public void onFail(int errCode) {
                                //long pts = System.nanoTime() / 1000;
                                //mediaCodec.queueInputBuffer(inputIndex, 0, 0, pts, 0);
                            }
                        });

            }
        }).start();*/

        //if(!portrait) {
        //    ClientHelper.sizeChange(getApplicationContext(), portrait);
        //}

        encodeThread = new Thread() {
            @Override
            public void run() {
                try {
                    mediaCodec.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                while (isMediaRecording && !interrupted()) {
                    try {
                        int outPutBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
                        if (outPutBufferId >= 0) {
                            System.out.println(outPutBufferId);
                            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT != portrait) {
                                ClientHelper.sizeChange(getApplicationContext(), getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
                                stopRecording();
                                startRecording(callback, resolution);
                                return;
                            }
                            ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outPutBufferId);
                            mediaCodecCallback.onSuccess(byteBuffer, bufferInfo);
                            mediaCodec.releaseOutputBuffer(outPutBufferId, false);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        encodeThread.start();
    }

    /**
     * 停止 媒体录制
     */
    public void stopRecording() {
        if (!isMediaCodecEnable) {
            if (mediaCodecCallback != null) {
                mediaCodecCallback.onFail();
            }
        }

        if (mediaCodec == null) {
            if (mediaCodecCallback != null) {
                mediaCodecCallback.onFail();
            }
            return;
        }
        if (!isMediaRecording) {
            if (mediaCodecCallback != null) {
                mediaCodecCallback.onFail();
            }
            return;
        }

        isMediaRecording = false;
        System.out.println("STOP");
        if(encodeThread!=null) {
            encodeThread.interrupt();
        }
        encodeThread = null;
        try {
            mediaCodec.stop();
            mediaCodec.reset();
            mediaCodec.release();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mediaCodec = null;

        mediaFile = null;

        mediaCodecCallback = null;
    }

    public void destroyVirtualDisplay() {
        if (virtualDisplayMediaRecorder != null) {
            virtualDisplayMediaRecorder.release();
            virtualDisplayMediaRecorder = null;
        }
    }

}
