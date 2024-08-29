package org.mj.leapremote.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaCodec;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSONObject;
import com.mask.mediaprojection.interfaces.MediaCodecCallback;
import com.mask.mediaprojection.interfaces.ScreenCaptureCallback;
import com.mask.mediaprojection.utils.MediaProjectionHelper;
import org.mj.leapremote.Define;
import org.mj.leapremote.cs.direct.server.ServerHandler;
import org.mj.leapremote.ui.fragments.QuickConnectFragment;
import org.mj.leapremote.R;
import org.mj.leapremote.cs.direct.server.Handlers;
import org.mj.leapremote.cs.direct.server.Server;
import org.mj.leapremote.util.ImageUtils;
import org.mj.leapremote.util.image.ZipCompress;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import io.netty.channel.ChannelHandlerContext;

import static android.app.Notification.VISIBILITY_SECRET;

public class ServerService extends Service {
    public static ServerService mService;
    public Server server;

    public class MyBinder extends Binder {
        public ServerService getService(){
            return ServerService.this;
        }
    }
    private MyBinder binder = new MyBinder();
    public boolean stopped;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent==null)
            return START_NOT_STICKY;
        String method = intent.getStringExtra("method");
        switch (method) {
            case "start":
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForeground(1, getNotification(getString(R.string.leap_remote_is_running), "远程控制服务正在运行"));
                }
                mService = this;
                server = new Server(Define.defaultPort);
                server.start();
                //loopSendImage();
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
            case "startRecord":
                startRecord();
                break;
            case "stopRecord":
                stopRecord();
                break;
        }
        return START_STICKY;
    }

    private void sizeChange(boolean portrait) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "sizeChange");
        jsonObject.put("portrait", portrait);
        for(ChannelHandlerContext ctx : Handlers.handlers) {
            ServerHandler.send(ctx, jsonObject.toJSONString());
        }
    }

    private void resolution(int resolution) {
        stopRecord();
        startRecord(resolution);
    }

    private void restartMedia() {
        stopRecord();
        startRecord();
    }

    public void stop() {
        mService = null;
        stopped = true;
        server.interrupt();
        stopSelf();
    }

    private Notification getNotification(String title, String message) {
        createNotificationChannel();
        //创建一个跳转到活动页面的意图
        Intent clickIntent = new Intent(this, QuickConnectFragment.class);
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
                .setSmallIcon(R.mipmap.ic_launcher)//设置状态栏里的小图标
                .setTicker(title)//设置状态栏里面的提示文本
                .setWhen(System.currentTimeMillis())//设置推送时间，格式为"小时：分钟"
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))//设置通知栏里面的大图标
                .setContentTitle(title)//设置通知栏里面的标题文本
                .setContentText(message);//设置通知栏里面的内容文本
        //根据消息构造器创建一个通知对象
        Notification notify = builder.build();
        return notify;
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel("f_channel_id", "CHANNEL_NAME", NotificationManager.IMPORTANCE_HIGH);
        //是否绕过请勿打扰模式
        channel.canBypassDnd();
        //闪光灯
        channel.enableLights(true);
        //锁屏显示通知
        channel.setLockscreenVisibility(VISIBILITY_SECRET);
        //闪关灯的灯光颜色
        channel.setLightColor(Color.RED);
        //桌面launcher的消息角标
        channel.canShowBadge();
        //是否允许震动
        channel.enableVibration(true);
        //获取系统通知响铃声音的配置
        channel.getAudioAttributes();
        //获取通知取到组
        channel.getGroup();
        //设置可绕过  请勿打扰模式
        channel.setBypassDnd(true);
        //设置震动模式
        channel.setVibrationPattern(new long[]{100, 100, 200});
        //是否会有灯光
        channel.shouldShowLights();
        getManager().createNotificationChannel(channel);
    }

    private NotificationManager mManager;

    private NotificationManager getManager() {
        if (mManager == null) {
            mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return mManager;
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
        Define.isControlled = true;
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
            if (sendingData != 0) {
                System.out.println("SENDINGDATA:" + sendingData);
            }
            if (sendingData >= 10) {
                return;
            }
        }
        if(!MediaProjectionHelper.getInstance().isHevc()) {
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
        for(ChannelHandlerContext ctx : Handlers.handlers) {
            ServerHandler.send(ctx, jsonObject.toJSONString());
        }
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
        Define.isControlled = false;
        MediaProjectionHelper.getInstance().stopMediaCodec();
    }

    public void loopSendImage() {
        new Thread(() -> {
            while (!stopped) {
                doScreenCapture();
                try {
                    Thread.sleep(Define.wait);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    private void doScreenCapture() {
        MediaProjectionHelper.getInstance().capture(new ScreenCaptureCallback() {
            @Override
            public void onSuccess(Bitmap bitmap) {
                super.onSuccess(bitmap);
                if(Handlers.handlers.size()!=0) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("type", "image");
                    jsonObject.put("image", ZipCompress.compress(
                            ImageUtils.bitmapToByteArray(
                                    ImageUtils.compressBitmap(bitmap, (float)1/Define.scale), Define.quality)));
                    int orientation = getResources().getConfiguration().orientation;
                    jsonObject.put("rotate", (orientation== Configuration.ORIENTATION_LANDSCAPE
                            &&Define.displayMetrics.heightPixels>=Define.displayMetrics.widthPixels)
                            || (orientation==Configuration.ORIENTATION_PORTRAIT
                            &&Define.displayMetrics.heightPixels<Define.displayMetrics.widthPixels));
                    long start = System.currentTimeMillis();
                    for(ChannelHandlerContext ctx : Handlers.handlers) {
                        ServerHandler.send(ctx, jsonObject.toJSONString());
                    }
                    long timeNeed = System.currentTimeMillis() - start;
                        /*if(Define.speedLimited) {
                            long size = jsonObject.getBytes("image").length;
                            long wait = Math.round(1000 / (Define.maxSpeed / size)) - timeNeed;
                            if(wait>0) {
                                Define.wait = wait;
                            }
                        }*/
                }
            }

            @Override
            public void onFail(int errCode) {
                super.onFail(errCode);
            }
        });
    }
}
