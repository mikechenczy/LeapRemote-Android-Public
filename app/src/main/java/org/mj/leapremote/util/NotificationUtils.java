package org.mj.leapremote.util;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import org.mj.leapremote.ui.fragments.QuickConnectFragment;
import org.mj.leapremote.R;

import static android.app.Notification.PRIORITY_DEFAULT;
import static android.app.Notification.VISIBILITY_SECRET;

public class NotificationUtils extends ContextWrapper {

    public String CHANNEL_ID = "default";
    private static final String CHANNEL_NAME = "Default Channel";
    private static final String CHANNEL_DESCRIPTION = "this is default channel!";
    private NotificationManager mManager;

    public NotificationUtils(Context base) {
        super(base);
    }

    public NotificationUtils(Context base, String channelId) {
        super(base);
        CHANNEL_ID = channelId;
    }

    @TargetApi(Build.VERSION_CODES.O)
    public void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
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

    private NotificationManager getManager() {
        if (mManager == null) {
            mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return mManager;
    }

    /**
     * 发送通知
     */
    public void sendNotification(String title, String content) {
        NotificationCompat.Builder builder = getNotification(title, content);
        getManager().notify(1, builder.build());
    }

    private NotificationCompat.Builder getNotification(String title, String content) {
        NotificationCompat.Builder builder = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(getApplicationContext());
            builder.setPriority(PRIORITY_DEFAULT);
        }
        //标题
        builder.setContentTitle(title);
        //文本内容
        //builder.setContentText(content);
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(content));
        //小图标
        builder.setSmallIcon(R.mipmap.ic_launcher);
        //设置点击信息后自动清除通知
        builder.setAutoCancel(true);
        Intent intent = new Intent(this, QuickConnectFragment.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, -1,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        return builder;
    }

    /**
     * 发送通知
     */
    public void sendNotification(int notifyId, String title, String content) {
        NotificationCompat.Builder builder = getNotification(title, content);
        getManager().notify(notifyId, builder.build());
    }

    /**
     * 发送带有进度的通知
     */
    public void sendNotificationProgress(String title, String content, int progress, PendingIntent intent) {
        NotificationCompat.Builder builder = getNotificationProgress(title, content, progress, intent);
        getManager().notify(0, builder.build());
    }

    /**
     * 获取带有进度的Notification
     */
    private NotificationCompat.Builder getNotificationProgress(String title, String content,
                                                               int progress, PendingIntent intent) {
        NotificationCompat.Builder builder = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(getApplicationContext());
            builder.setPriority(PRIORITY_DEFAULT);
        }
        //标题
        builder.setContentTitle(title);
        //文本内容
        builder.setContentText(content);
        //小图标
        builder.setSmallIcon(R.mipmap.ic_launcher);
        //设置大图标，未设置时使用小图标代替，拉下通知栏显示的那个图标
        //设置大图片 BitmpFactory.decodeResource(Resource res,int id) 根据给定的资源Id解析成位图
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        if (progress > 0 && progress < 100) {
            //一种是有进度刻度的（false）,一种是循环流动的（true）
            //设置为false，表示刻度，设置为true，表示流动
            builder.setProgress(100, progress, false);
        } else {
            //0,0,false,可以将进度条隐藏
            builder.setProgress(0, 0, false);
            builder.setContentText("下载完成");
        }
        //设置点击信息后自动清除通知
        builder.setAutoCancel(true);
        //通知的时间
        builder.setWhen(System.currentTimeMillis());
        //设置点击信息后的跳转（意图）
        builder.setContentIntent(intent);
        return builder;
    }
}