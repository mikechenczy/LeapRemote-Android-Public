package org.mj.leapremote.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import org.mj.leapremote.Define;
import org.mj.leapremote.ui.activities.MainActivity;

import static org.mj.leapremote.util.ServerUtil.refreshWebSocketIpv4AndIpv6;

public class IntentReceiver extends BroadcastReceiver {
    boolean first = true;
    int time = 0;
    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        //Toast.makeText(context," "+ (++time),Toast.LENGTH_SHORT).show();
        if (networkInfo !=null && networkInfo.isAvailable()) {
            if(!first) {
                if(MainActivity.INSTANCE!=null) {
                    new Thread(() -> {
                        try {
                            Thread.sleep(Define.networkAvailableWaitTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //System.out.println("interrupt");
                        refreshWebSocketIpv4AndIpv6(MainActivity.INSTANCE);
                        if (MainActivity.INSTANCE.actsFragment != null) {
                            MainActivity.INSTANCE.actsFragment.refreshHosts();
                        }
                    }).start();
                }
            }
        } else {
            //Toast.makeText(context,"网络连接失败",Toast.LENGTH_SHORT).show();
        }
        first = false;
    }
}