package org.mj.leapremote.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.mj.leapremote.Define;
import org.mj.leapremote.ui.fragments.QuickConnectFragment;
import org.mj.leapremote.R;
import org.mj.leapremote.util.Utils;

import java.util.concurrent.atomic.AtomicBoolean;

import static android.app.Notification.VISIBILITY_SECRET;

public class AutoService extends AccessibilityService {
    private Handler mHandler;
    public static AutoService mService;

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread handlerThread = new HandlerThread("auto-handler");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        mService = this;
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
            builder = new Notification.Builder(this, "fcid");
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
        return builder.build();
    }





    public interface RecordGestureListener {
        void onSuccess(JSONArray gesture);
    }

    JSONArray gesture = new JSONArray();
    long lastStartTime;
    long startTime;
    long indexTime;
    boolean pressing = false;
    JSONArray points = new JSONArray();


    public void initView(RecordGestureListener recordGestureListener) {
        gesture = new JSONArray();
        System.out.println("INIT VIEW!!!");
        // 获取 WindowManager 实例
        WindowManager wm = (WindowManager) getSystemService(AccessibilityService.WINDOW_SERVICE);
        if (wm == null) {
            return;
        }

        // 创建 LayoutParams
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        // 适配显示切口模式
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        // 创建 LayoutParams
        WindowManager.LayoutParams lp2 = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        // 适配显示切口模式
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }




        Button closeButton = new Button(this);
        closeButton.setText("X");
        closeButton.setBackgroundColor(Color.TRANSPARENT); // 叉按钮背景色
        closeButton.setTextColor(Color.RED); // 叉按钮文字颜色

        // 设置按钮的 LayoutParams
        WindowManager.LayoutParams buttonParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );

        // 设置按钮在屏幕右下角
        buttonParams.gravity = Gravity.BOTTOM | Gravity.END;
        buttonParams.x = 5; // 距离右边缘的距离
        buttonParams.y = 5; // 距离底部的距离



        AtomicBoolean executing = new AtomicBoolean(false);

        View rootView = new View(this);
        rootView.setBackgroundColor(Color.argb(50, 0, 255, 0)); // 半透明绿色
        rootView.setOnTouchListener((v, e) -> {
            if(executing.get()) {
                return true;
            }
            if (e.getAction() == 0) {
                pressing = true;
                points = new JSONArray();
                lastStartTime = 0;
                startTime = (indexTime = System.currentTimeMillis());
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("x", e.getX() / Define.displayMetrics.widthPixels);
                jsonObject.put("y", e.getY() / Define.displayMetrics.heightPixels);
                points.add(jsonObject);
                return true;
            }
            long now = System.currentTimeMillis();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("x", e.getX() / Define.displayMetrics.widthPixels);
            jsonObject.put("y", e.getY() / Define.displayMetrics.heightPixels);
            jsonObject.put("duration", now - indexTime);
            points.add(jsonObject);
            indexTime = now;

            if (e.getAction() == 1) {
                pressing = false;
                JSONObject oneGesture = new JSONObject();
                oneGesture.put("time", lastStartTime!=0?(lastStartTime+startTime)/2:startTime);
                oneGesture.put("points", points);
                gesture.add(oneGesture);
                lastStartTime = startTime;

                wm.removeView(rootView);
                wm.removeView(closeButton);
                rootView.setBackgroundColor(Color.argb(50, 255, 0, 0));
                wm.addView(rootView, lp2);
                wm.addView(closeButton, buttonParams);
                executing.set(true);
                performGesture(points);
                new Handler(getMainLooper()).postDelayed(() -> {
                    executing.set(false);
                    wm.removeView(rootView);
                    wm.removeView(closeButton);
                    rootView.setBackgroundColor(Color.argb(50, 0, 255, 0));
                    wm.addView(rootView, lp);
                    wm.addView(closeButton, buttonParams);
                }, now-startTime);
            }
            return true;
        });


        // 设置关闭按钮的点击事件
        closeButton.setOnClickListener(v -> {
            wm.removeView(rootView);
            wm.removeView(closeButton);
            recordGestureListener.onSuccess(new JSONArray(gesture));
        });



        // 添加 View 到窗口
        wm.addView(rootView, lp);
        wm.addView(closeButton, buttonParams);
    }




    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel("fcid", "NAME", NotificationManager.IMPORTANCE_HIGH);
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForeground(1, getNotification(getString(R.string.leap_remote_is_running), "远程控制服务正在运行"));
        }
        return START_STICKY;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        //initView();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        /*if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            //View点击事件
            Toast.makeText(this, nodeInfo.getText()+"被点击了", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "onAccessibilityEvent: " + nodeInfo.getText());
            if ((nodeInfo.getText() + "").equals("模拟点击")) {
                Toast.makeText(this, "这是来自监听Service的响应！", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "onAccessibilityEvent: 这是来自监听Service的响应！");
            }
        }*/
    }


    @Override
    public void onInterrupt() {
    }

    long performId = Long.MIN_VALUE;
    public void performMultipleGestures(JSONArray gestures) {
        Handler handler = new Handler(getMainLooper());
        long startTime = 0;

        performId+=1;
        final long id = performId;
        for(int i=0;i<gestures.size();i++) {
            JSONObject gesture = gestures.getJSONObject(i);
            if(i==0) {
                startTime = gesture.getLongValue("time");
                handler.post(() -> performGesture(gesture.getJSONArray("points")));
            } else {
                handler.postDelayed(() -> {
                    if(id!=performId)
                        return;
                    performGesture(gesture.getJSONArray("points"));
                    }, gesture.getLongValue("time")-startTime);
            }
        }
    }

    public void performGesture(JSONArray points) {
        System.out.println("performGesture:"+points);
        int width = Define.displayMetrics.widthPixels;
        int height = Define.displayMetrics.heightPixels;
        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        if(points.size()==2) {
            Path path = new Path();
            //if(i==points.size()-1) {
            //    path.setLastPoint(point.getFloatValue("x")*width, point.getFloatValue("y")*height);
            //} else {
            path.moveTo(points.getJSONObject(1).getFloatValue("x")*width, points.getJSONObject(1).getFloatValue("y")*height);
            //}
            gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0, points.getJSONObject(1).getLongValue("duration")));
        } else {
            if(points.size()>=GestureDescription.getMaxStrokeCount() || Build.VERSION.SDK_INT < Build.VERSION_CODES.O || 1==1) {//Only this
                Path path = new Path();
                long duration = 0;
                path.moveTo(points.getJSONObject(0).getFloatValue("x")*width, points.getJSONObject(0).getFloatValue("y")*height);
                for(int i=1;i<points.size();i++) {
                    JSONObject point = points.getJSONObject(i);
                    if(i==points.size()-1) {
                        path.setLastPoint(point.getFloatValue("x")*width, point.getFloatValue("y")*height);
                        duration+=point.getLongValue("duration");
                        break;
                    }
                    path.lineTo(point.getFloatValue("x")*width, point.getFloatValue("y")*height);
                    duration+=point.getLongValue("duration");
                }
                gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0, duration));
            } else {
                long startTime = 0;
                for (int i = 1; i < points.size(); i++) {
                    JSONObject lastPoint = points.getJSONObject(i - 1);
                    JSONObject point = points.getJSONObject(i);
                    if (point.getLongValue("duration") <= 0) {
                        continue;
                    }
                    Path path = new Path();
                    path.moveTo(lastPoint.getFloatValue("x") * width, lastPoint.getFloatValue("y") * height);
                    path.lineTo(point.getFloatValue("x") * width, point.getFloatValue("y") * height);
                    gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, startTime, point.getLongValue("duration"), i != points.size() - 1));
                    startTime += point.getLongValue("duration");
                }
            }
        }
        dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                Log.d("Gesture Completed","Gesture Completed");
                super.onCompleted(gestureDescription);
                //mHandler.postDelayed(mRunnable, 10);
                //mHandler.post(mRunnable);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                Log.d("Gesture Cancelled","Gesture Cancelled");
                super.onCancelled(gestureDescription);
            }
        }, new Handler(Looper.getMainLooper()));
        /*duration = d;
        points = JSONObject.parseArray(p, float[].class);
        path = new Path();
        if(points.size()==2) {
            path.moveTo(points.get(0)[0] * width
                    , points.get(0)[1] * height);
        } else {
            path.moveTo(points.get(0)[0] * width
                    , points.get(0)[1] * height);
            for(int i=1;i<points.size()-1;i++) {
                float[] point = points.get(i);
                path.lineTo(point[0] * width
                        , point[1] * height);
            }
            path.setLastPoint(points.get(points.size()-1)[0] * width
                    , points.get(points.size()-1)[1] * height);
        }
        playPath();*/
    }
}