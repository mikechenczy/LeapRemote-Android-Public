package org.mj.leapremote.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import org.mj.leapremote.util.NotificationUtils;

public class MessageReceiver extends BroadcastReceiver {

    public static final String MESSAGE_ACTION = "MESSAGE_ACTION";
    public static final String MESSAGE_ID = "MESSAGE_ID";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (MESSAGE_ACTION == intent.getAction()) {
            String messageid = intent.getStringExtra(MESSAGE_ID);
            if (!TextUtils.isEmpty(messageid)) {
                new NotificationUtils(context).sendNotification("提示", messageid);
            }
        }
    }
}