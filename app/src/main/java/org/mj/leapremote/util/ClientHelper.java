package org.mj.leapremote.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.mj.leapremote.service.ClientService;
import org.mj.leapremote.ui.activities.MainActivity;

public class ClientHelper {
    public static void startService(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void connectServer(Context context) {
        Intent intent = new Intent(context, ClientService.class);
        intent.putExtra("method", "connectServer");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void sendMessage(Context context, String msg) {
        Intent intent = new Intent(context, ClientService.class);
        intent.putExtra("method", "sendMessage");
        intent.putExtra("msg", msg);
        startService(context, intent);
    }

    public static void enableRemote(Context context) {
        Intent intent = new Intent(context, ClientService.class);
        intent.putExtra("method", "enableRemote");
        startService(context, intent);
    }

    public static void disableRemote(Context context) {
        Intent intent = new Intent(context, ClientService.class);
        intent.putExtra("method", "disableRemote");
        startService(context, intent);
    }

    public static void enableSend(Context context) {
        Intent intent = new Intent(context, ClientService.class);
        intent.putExtra("method", "enableSend");
        startService(context, intent);
    }

    public static void disableSend(Context context) {
        Intent intent = new Intent(context, ClientService.class);
        intent.putExtra("method", "disableSend");
        startService(context, intent);
    }

    public static void enabled(Context context) {
        Intent intent = new Intent(context, ClientService.class);
        intent.putExtra("method", "enabled");
        startService(context, intent);
    }

    public static void reconnect(Context context) {
        Intent intent = new Intent(context, ClientService.class);
        intent.putExtra("method", "reconnect");
        startService(context, intent);
    }

    public static void restartMedia(Context context) {
        Intent intent = new Intent(context, ClientService.class);
        intent.putExtra("method", "restartMedia");
        startService(context, intent);
    }

    public static void resolution(Context context, int resolution) {
        Intent intent = new Intent(context, ClientService.class);
        intent.putExtra("method", "resolution");
        intent.putExtra("resolution", resolution);
        startService(context, intent);
    }

    public static void sizeChange(Context context, boolean portrait) {
        Intent intent = new Intent(context, ClientService.class);
        intent.putExtra("method", "sizeChange");
        intent.putExtra("portrait", portrait);
        startService(context, intent);
    }

    public static void firstReceived(Context context) {
        Intent intent = new Intent(context, ClientService.class);
        intent.putExtra("method", "firstReceived");
        startService(context, intent);
    }
}
