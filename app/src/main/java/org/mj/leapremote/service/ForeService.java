package org.mj.leapremote.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;

import org.mj.leapremote.ui.fragments.QuickConnectFragment;
import org.mj.leapremote.R;
import org.mj.leapremote.receiver.MessageReceiver;

import static android.app.Notification.VISIBILITY_SECRET;

/***
 * @Description: 前台服务
 * channelId必须要一致，否则会报 android.app.RemoteServiceException: Bad notification for startForeground 错误
 * 8.0之上一定要使用 NotificationChannel 适配下才行
 * 步骤
 * 1.通过 “通知服务” 创建 NotificationChannel
 * 2.通过 Notification.Builder 构造器 创建 Notification
 * 3.通过 startForeground 开启服务
 * 4.高于9.0的版本 manifest需要增加  <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
 *
 * 在onCreate中创建一个广播接收器，试试能不能接收到 开单或者预约结束后的通知
 */
public class ForeService extends Service {

    private MessageReceiver mMsgRecv;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, getNotification(getString(R.string.leap_remote_is_running), "远程控制服务正在运行"));

        //注册广播
        mMsgRecv = new MessageReceiver();
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(MessageReceiver.MESSAGE_ACTION);
        registerReceiver(mMsgRecv, mFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //取消监听广播
        unregisterReceiver(mMsgRecv);
        //停止的时候销毁前台服务
        stopForeground(true);
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
                .setTicker("提示消息来啦")//设置状态栏里面的提示文本
                .setWhen(System.currentTimeMillis())//设置推送时间，格式为"小时：分钟"
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))//设置通知栏里面的大图标
                .setContentTitle(title)//设置通知栏里面的标题文本
                .setContentText(message);//设置通知栏里面的内容文本
        //根据消息构造器创建一个通知对象
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            Notification notify = builder.build();
            return notify;
        }
        return null;
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
}