package org.mj.leapremote.service;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.IBinder;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSONObject;
import com.mask.mediaprojection.interfaces.MediaCodecCallback;
import com.mask.mediaprojection.interfaces.ScreenCaptureCallback;
import com.mask.mediaprojection.utils.MediaProjectionHelper;
import org.mj.leapremote.Define;
import org.mj.leapremote.coder.ScreenDecoder;
import org.mj.leapremote.cs.NettyClientWebSocket;
import org.mj.leapremote.ui.activities.MainActivity;
import org.mj.leapremote.R;
import org.mj.leapremote.util.ImageUtils;
import org.mj.leapremote.util.KeyUtil;
import org.mj.leapremote.util.NotificationUtils;
import org.mj.leapremote.util.Utils;
import org.mj.leapremote.util.image.Compress;
import org.mj.leapremote.util.image.ZipCompress;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ClientService extends Service {
    //Data
    public boolean send;

    //Private
    private NettyClientWebSocket client;

    public void sendMessage(String msg) {
        if(client==null) {
            Toast.makeText(this, R.string.cannot_connect_to_server, Toast.LENGTH_SHORT).show();
            return;
        }
        client.sendMessage(msg);
    }

    public class MyBinder extends Binder {

        public ClientService getService(){
            return ClientService.this;
        }

    }
    private MyBinder binder = new MyBinder();
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForeground(1, getNotification(getString(R.string.leap_remote_is_running), "远程控制服务正在运行"));
        }
    }

    private void enabled() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "remote");
        jsonObject.put("enabled", Define.remotePlainEnabled);
        jsonObject.put("directEnabled", Define.remoteDirectEnabled);
        sendMessage(jsonObject.toJSONString());
    }

    public void enableRemote() {
        Define.remotePlainEnabled = true;
        //loopSendImage();
        enabled();
    }

    public void disableRemote() {
        send = false;
        Define.remotePlainEnabled = false;
        enabled();
    }

    public void enableSend() {
        send = true;
        startRecord();
        Define.isControlled = true;
        KeyUtil.unlock(MainActivity.INSTANCE);
    }

    public void disableSend() {
        send = false;
        Define.isControlled = false;
        stopRecord();
    }

    private void reconnect() {
        if(client==null) {
            connectServer();
            return;
        }
        client.interrupt();
        client = null;
        connectServer();
    }

    private byte[] vps_pps_sps;
    private boolean first = true;
    private boolean firstReceived;
    private List<byte[]> queueData = new ArrayList<>();
    private Integer index = 0;
    private Integer sendingData = 0;
    private void startRecord() {
        startRecord(-1);
    }
    private void startRecord(int resolution) {
        sendingData = 0;
        first = true;
        firstReceived = false;
        queueData.clear();
        MediaProjectionHelper.getInstance().startMediaCodec(new MediaCodecCallback() {
            @Override
            public void onSuccess(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
                final byte[] bytes = new byte[bufferInfo.size];
                byteBuffer.get(bytes);
                if(!first && !firstReceived) {
                    queueData.add(bytes);
                    return;
                }
                if(!first) {
                    synchronized (queueData) {
                        if (queueData.size() != 0) {
                            for(int i=0;i<queueData.size();i++) {
                                handleBuffer(queueData.get(i));
                            }
                            queueData.clear();
                        }
                    }
                }
                handleBuffer(bytes);
            }
        }, resolution);
    }

    private void handleBuffer(byte[] bytes) {
        synchronized (sendingData) {
            if (sendingData >= 10) {
                System.out.println("SENDINGDATA > 10!!!!" + sendingData);
                return;
            }
        }
        if(1==1 || !MediaProjectionHelper.getInstance().isHevc()) {
            new Thread(() -> sendData(bytes)).start();
            return;
        }
        int offSet = 4;
        if (bytes[2] == 0x01) {
            offSet = 3;
        }
        int type = (bytes[offSet] & 0x7E) >> 1;
        if (type == 32) {
            vps_pps_sps = bytes;
        } else if (type == 19) {
            byte[] newBytes = new byte[vps_pps_sps.length + bytes.length];
            System.arraycopy(vps_pps_sps, 0, newBytes, 0, vps_pps_sps.length);
            System.arraycopy(bytes, 0, newBytes, vps_pps_sps.length, bytes.length);
            new Thread(() -> sendData(newBytes)).start();
        } else {
            new Thread(() -> sendData(bytes)).start();
        }
    }

    private void sendData(byte[] data) {
        synchronized (sendingData) {
            sendingData +=1;
        }
        //new Thread(() -> {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "record");
        jsonObject.put("record", data);
        if(first) {
            first = false;
            index = 0;
            jsonObject.put("first", true);
            jsonObject.put("portrait", getResources().getConfiguration().orientation==Configuration.ORIENTATION_PORTRAIT);
            jsonObject.put("hevc", MediaProjectionHelper.getInstance().isHevc());
            jsonObject.put("width", Define.displayMetrics.widthPixels);
            jsonObject.put("height", Define.displayMetrics.heightPixels);
        }
        synchronized (index) {
            jsonObject.put("index", index);
            index += 1;
        }
        System.out.println((float)((float)data.length/1024));
                /*jsonObject.put("image",compress.compress(ImageUtils.byteArrayToBitmap(
                        ImageUtils.bitmapToByteArray(
                                ImageUtils.compressBitmap(bitmap, (float)bitmap.getWidth()/Define.scale,
                                        (float)bitmap.getHeight()/ Define.scale), Define.quality))));
                jsonObject.put("compressed", compress.compressed);
                if(compress.changedSize) {
                    jsonObject.put("width", compress.width);
                    jsonObject.put("height", compress.height);
                }*/
        int orientation = getResources().getConfiguration().orientation;
        jsonObject.put("rotate", (orientation==Configuration.ORIENTATION_LANDSCAPE
                &&Define.displayMetrics.heightPixels>=Define.displayMetrics.widthPixels)
                || (orientation==Configuration.ORIENTATION_PORTRAIT
                &&Define.displayMetrics.heightPixels<Define.displayMetrics.widthPixels));
        long start = System.currentTimeMillis();
        JSONObject send = new JSONObject();
        send.put("type", "send");
        send.put("controlId", Define.controlId);
        send.put("controlled", true);
        send.put("data", jsonObject);
        client.sendMessage(send.toJSONString());
        long timeNeed = System.currentTimeMillis() - start;
                /*if(Define.speedLimited) {
                    long size = jsonObject.getBytes("image").length;
                    long wait = Math.round(1000 / (Define.maxSpeed / size)) - timeNeed;
                    if(wait>0) {
                        Define.wait = wait;
                    }
                }*/
        //}).start();
        synchronized (sendingData) {
            sendingData -=1;
        }
    }

    private void stopRecord() {
        MediaProjectionHelper.getInstance().stopMediaCodec();
    }

    public Compress compress;

    private void loopSendImage() {
        compress = new Compress();
        new Thread(() -> {
            while(Define.remotePlainEnabled) {
                try {
                    Thread.sleep(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while (send) {
                    doScreenCapture();
                    try {
                        Thread.sleep(Define.wait);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public static Bitmap takeScreenShot(Activity act) {
        if (act == null || act.isFinishing()) {
            //Log.d(TAG, "act参数为空.");
            return null;
        }
// 获取当前视图的view
        View scrView = act.getWindow().getDecorView();
        scrView.setDrawingCacheEnabled(true);
        scrView.buildDrawingCache(true);
// 获取状态栏⾼度
        Rect statuBarRect = new Rect();
        scrView.getWindowVisibleDisplayFrame(statuBarRect);
        int statusBarHeight = statuBarRect.top;
        int width = act.getWindowManager().getDefaultDisplay().getWidth();
        int height = act.getWindowManager().getDefaultDisplay().getHeight();
        Bitmap scrBmp = null;
        try {
// 去掉标题栏的截图
            scrBmp = Bitmap.createBitmap( scrView.getDrawingCache(), 0, statusBarHeight,
                    width, height - statusBarHeight);
        } catch (IllegalArgumentException e) {
            //Log.d("", "#### 旋转屏幕导致去掉状态栏失败");
        }
        scrView.setDrawingCacheEnabled(false);
        scrView.destroyDrawingCache();
        return scrBmp;
    }

    private void doScreenCapture() {
        MediaProjectionHelper.getInstance().capture(new ScreenCaptureCallback() {
            @Override
            public void onSuccess(Bitmap bitmap) {
                super.onSuccess(bitmap);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", "image");
                byte[] bytes = ZipCompress.compress(
                        ImageUtils.bitmapToByteArray(
                                ImageUtils.compressBitmap(bitmap, (float)1/Define.scale), Define.quality));
                jsonObject.put("image", bytes);
                System.out.println((float)((float)bytes.length/1024));
                /*jsonObject.put("image",compress.compress(ImageUtils.byteArrayToBitmap(
                        ImageUtils.bitmapToByteArray(
                                ImageUtils.compressBitmap(bitmap, (float)bitmap.getWidth()/Define.scale,
                                        (float)bitmap.getHeight()/ Define.scale), Define.quality))));
                jsonObject.put("compressed", compress.compressed);
                if(compress.changedSize) {
                    jsonObject.put("width", compress.width);
                    jsonObject.put("height", compress.height);
                }*/
                int orientation = getResources().getConfiguration().orientation;
                jsonObject.put("rotate", (orientation==Configuration.ORIENTATION_LANDSCAPE
                        &&Define.displayMetrics.heightPixels>=Define.displayMetrics.widthPixels)
                        || (orientation==Configuration.ORIENTATION_PORTRAIT
                        &&Define.displayMetrics.heightPixels<Define.displayMetrics.widthPixels));
                long start = System.currentTimeMillis();
                JSONObject send = new JSONObject();
                send.put("type", "send");
                send.put("controlId", Define.controlId);
                send.put("controlled", true);
                send.put("data", jsonObject);
                client.sendMessage(send.toJSONString());
                long timeNeed = System.currentTimeMillis() - start;
                if(Define.speedLimited) {
                    long size = jsonObject.getBytes("image").length;
                    long wait = Math.round(1000 / (Define.maxSpeed / size)) - timeNeed;
                    if(wait>0) {
                        Define.wait = wait;
                    }
                }
            }
            //LogUtil.i("ScreenCapture onSuccess");

//                int[] position = new int[2];
//                layout_space.getLocationOnScreen(position);
//                int width = layout_space.getWidth();
//                int height = layout_space.getHeight();
//                bitmap = Bitmap.createBitmap(bitmap, position[0], position[1], width, height);

            //saveBitmapToFile(bitmap, "ScreenCapture");

            @Override
            public void onFail(int errCode) {
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent==null)
            return START_NOT_STICKY;
        String method = intent.getStringExtra("method");
        switch (method) {
            case "connectServer":
                connectServer();
                break;
            case "enableRemote":
                enableRemote();
                break;
            case "disableRemote":
                disableRemote();
                break;
            case "enabled":
                enabled();
                break;
            case "enableSend":
                enableSend();
                break;
            case "disableSend":
                disableSend();
                break;
            case "sendMessage":
                sendMessage(intent.getStringExtra("msg"));
                break;
            case "reconnect":
                reconnect();
                break;
            case "restartMedia":
                restartMedia();
                break;
            case "resolution":
                resolution(intent.getIntExtra("resolution", 50));
                break;
            case "sizeChange":
                sizeChange(intent.getBooleanExtra("portrait", true));
                break;
            case "firstReceived":
                firstReceived = true;
                break;
        }
        return START_STICKY;
    }

    private void sizeChange(boolean portrait) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "sizeChange");
        jsonObject.put("portrait", portrait);
        JSONObject send = new JSONObject();
        send.put("type", "send");
        send.put("controlId", Define.controlId);
        send.put("controlled", true);
        send.put("data", jsonObject);
        client.sendMessage(send.toJSONString());
    }

    private void resolution(int resolution) {
        stopRecord();
        startRecord(resolution);
    }

    private void restartMedia() {
        stopRecord();
        startRecord();
    }

    private void connectServer() {
        if(client==null) {
            client = new NettyClientWebSocket(MainActivity.INSTANCE
                    , new NettyClientWebSocket.OnConnectSuccessCallback() {
                @Override
                public void success() {
                    //System.out.println("NETTY SUCCESS");
                }
                @Override
                public void failed(boolean fatal, String err, int times) {
                    System.out.println(err);
                    if(fatal) {
                        System.err.println("WebSocket connect failed fatal: " + err);
                    }
                }
            });
            client.start();
        }
    }

    private Notification getNotification(String title, String message) {
        new NotificationUtils(this, "f_channel_id").createNotificationChannel();
        //创建一个跳转到活动页面的意图
        Intent clickIntent = new Intent(this, MainActivity.class);
        //clickIntent.putExtra("flag", count);//这里可以传值
        //创建一个用于页面跳转的延迟意图
        PendingIntent contentIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            contentIntent = PendingIntent.getActivity(this, 1012, clickIntent
                    , PendingIntent.FLAG_IMMUTABLE);
        } else {
            contentIntent = PendingIntent.getActivity(this, 1012, clickIntent
                    , PendingIntent.FLAG_ONE_SHOT);
        }
        //创建一个通知消息的构造器
        Notification.Builder builder = new Notification.Builder(this);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //Android8.0开始必须给每个通知分配对应的渠道
            builder = new Notification.Builder(this, "f_channel_id");
        }
        builder.setContentIntent(contentIntent)//设置内容的点击意图
                .setAutoCancel(true)//设置是否允许自动清除
                .setSmallIcon(R.drawable.ic_launcher)//设置状态栏里的小图标
                .setWhen(System.currentTimeMillis())//设置推送时间，格式为"小时：分钟"
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.ic_launcher))//设置通知栏里面的大图标
                .setContentTitle(title)//设置通知栏里面的标题文本
                .setContentText(message);//设置通知栏里面的内容文本
        //根据消息构造器创建一个通知对象
        return builder.build();
    }

    private NotificationManager mManager;

    private NotificationManager getManager() {
        if (mManager == null) {
            mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return mManager;
    }
}
