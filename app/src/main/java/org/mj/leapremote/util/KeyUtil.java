package org.mj.leapremote.util;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.widget.Toast;

import org.mj.leapremote.R;
import org.mj.leapremote.service.AutoService;
import org.mj.leapremote.ui.activities.MainActivity;

public class KeyUtil {
    public static void unlock(Context context) {
        KeyguardManager km= (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
//得到键盘锁管理器对象
        KeyguardManager.KeyguardLock kl = km.newKeyguardLock("unLock");
//参数是LogCat里用的Tag
        kl.disableKeyguard(); //解锁
        PowerManager pm=(PowerManager) context.getSystemService(Context.POWER_SERVICE);//获取电源管理器对象
        @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "bright");
//获取PowerManager.WakeLock对象，后面的参数|表示同时传入两个值，最后的是LogCat里用的Tag
        wl.acquire();//点亮屏幕
        wl.release();//释放
    }

    public static void lock(Context context) {
        if(AutoService.mService!=null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                AutoService.mService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN);
                return;
            }
            Toast.makeText(context, context.getString(R.string.the_function_needs_android) + "9", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(context, R.string.accessibility_not_enabled, Toast.LENGTH_SHORT).show();
        //MainActivity.INSTANCE.actsFragment.lock();
    }

    public static void lockOrUnlock(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if(pm.isScreenOn()) {
            lock(context);
            return;
        }
        unlock(context);
    }
}
