package org.mj.leapremote.ui.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ashokvarma.bottomnavigation.BottomNavigationBar;
import com.ashokvarma.bottomnavigation.BottomNavigationItem;
import com.ashokvarma.bottomnavigation.TextBadgeItem;
import com.mask.mediaprojection.utils.MediaProjectionHelper;
import org.mj.leapremote.Define;
import org.mj.leapremote.LogUtil;
import org.mj.leapremote.NotificationHelper;
import org.mj.leapremote.R;
import org.mj.leapremote.receiver.IntentReceiver;
import org.mj.leapremote.service.AutoService;
import org.mj.leapremote.service.ServerService;
import org.mj.leapremote.ui.fragments.MainFragment;
import org.mj.leapremote.ui.fragments.MineFragment;
import org.mj.leapremote.ui.fragments.QuickConnectFragment;
import org.mj.leapremote.util.ClientHelper;
import org.mj.leapremote.util.DataUtil;

public class MainActivity extends AppCompatActivity {
    public static MainActivity INSTANCE;
    private BroadcastReceiver receivers = new IntentReceiver();

    public BottomNavigationBar bnb;
    public BottomNavigationItem home;
    public BottomNavigationItem acts;
    public BottomNavigationItem me;
    public TextBadgeItem badge1;
    public TextBadgeItem badge2;
    public TextBadgeItem badge3;

    public MainFragment mainFragment;
    public QuickConnectFragment actsFragment;
    public MineFragment mineFragment;

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        INSTANCE = this;

        ClientHelper.connectServer(getApplication());

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(receivers,filter);

        setContentView(R.layout.activity_main);
        bnb = findViewById(R.id.bnb);
        initData();

        mainFragment = new MainFragment();
        actsFragment = new QuickConnectFragment();
        mineFragment = new MineFragment();
        badge1 = new TextBadgeItem()
                .setTextColorResource(R.color.red)
                .setBackgroundColorResource(R.color.transparent)
                .setHideOnSelect(false);
        badge2 = new TextBadgeItem()
                .setTextColorResource(R.color.red)
                .setBackgroundColorResource(R.color.transparent)
                .setHideOnSelect(false);
        badge3 = new TextBadgeItem()
                .setTextColorResource(R.color.red)
                .setBackgroundColorResource(R.color.transparent)
                .setHideOnSelect(false);
        bnb = findViewById(R.id.bnb);
        home = new BottomNavigationItem(R.drawable.home_oc,"").setInactiveIconResource(R.drawable.home).setBadgeItem(badge1);
        acts = new BottomNavigationItem(R.drawable.acts_oc,"").setInactiveIconResource(R.drawable.acts).setBadgeItem(badge2);
        me = new BottomNavigationItem(R.drawable.me_oc,"").setInactiveIconResource(R.drawable.me).setBadgeItem(badge3);
        bnb
                .addItem(home)
                .addItem(acts)
                .addItem(me)
                .setMode(BottomNavigationBar.MODE_FIXED)
                .setBarBackgroundColor(R.color.white)
                .initialise();
        getFragmentManager().beginTransaction()
                .add(R.id.main_layout, mainFragment)
                .add(R.id.main_layout, actsFragment)
                .add(R.id.main_layout, mineFragment)
                .hide(actsFragment)
                .hide(mineFragment)
                .commit();
        bnb.setTabSelectedListener(new BottomNavigationBar.OnTabSelectedListener() {
            @Override
            public void onTabSelected(int position) {
                switch(position) {
                    case 0:
                        getFragmentManager().beginTransaction()
                                .show(mainFragment)
                                .hide(actsFragment)
                                .hide(mineFragment).commit();
                        badge1.hide();
                        break;
                    case 1:
                        getFragmentManager().beginTransaction()
                                .show(actsFragment)
                                .hide(mainFragment)
                                .hide(mineFragment)
                                .commit();
                        badge2.hide();
                        break;
                    case 2:
                        getFragmentManager().beginTransaction()
                                .show(mineFragment)
                                .hide(mainFragment)
                                .hide(actsFragment)
                                .commit();
                        badge3.hide();
                        break;
                }
            }

            @Override
            public void onTabUnselected(int position) {

            }

            @Override
            public void onTabReselected(int position) {

            }
        });

        if (!Define.agree&&!Define.isExamining)
            showPrivacy();
    }

    AlertDialog privacyDialog;

    public void showPrivacy() {
        final View inflate = LayoutInflater.from(this).inflate(R.layout.dialog_privacy_show, null);
        TextView tv_title = inflate.findViewById(R.id.tv_title);
        tv_title.setText(R.string.privacy_title);
        TextView tv_content = inflate.findViewById(R.id.tv_content);
        tv_content.setText(R.string.privacy);
        privacyDialog = new AlertDialog
                .Builder(this)
                .setView(inflate)
                .setOnDismissListener(dialog -> finish())
                .show();
        // 通过WindowManager获取
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        final WindowManager.LayoutParams params = privacyDialog.getWindow().getAttributes();
        params.width = dm.widthPixels*4/5;
        params.height = dm.heightPixels*3/5;
        privacyDialog.getWindow().setAttributes(params);
        privacyDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    public void onClickDisagree(View v) {
        finish();
    }

    public void onClickAgree(View v) {
        privacyDialog.setOnDismissListener(null);
        privacyDialog.dismiss();
        Define.agree = true;
        new Thread(DataUtil::save).start();
    }

    private void initData() {
        MediaProjectionHelper.getInstance().setNotificationEngine(() -> {
            String title = getString(R.string.service_start);
            return NotificationHelper.getInstance().createSystem()
                    .setOngoing(true)// 常驻通知栏
                    .setTicker(title)
                    .setContentText(title)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .build();
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            LogUtil.i("Environment.isExternalStorageLegacy: " + Environment.isExternalStorageLegacy());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receivers);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1 :
                System.out.println("REC");
                if (resultCode == Activity.RESULT_OK) {
                } else {
                    //TODO add on
                    System.out.println(requestCode+": "+resultCode);
                }
                return;
            default:
                if(!Define.serverDirect) {
                    if (resultCode == Activity.RESULT_OK) {
                        MediaProjectionHelper.getInstance().createVirtualDisplay(requestCode, resultCode, data, true, true);
                        Intent intent = new Intent(getApplicationContext(), AutoService.class);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            getApplication().startForegroundService(intent);
                        } else {
                            getApplication().startService(intent);
                        }
                        ClientHelper.enableRemote(getApplicationContext());
                        if (mineFragment != null && mineFragment.enablePlainRemoteSwift != null)
                            mineFragment.clickOn(mineFragment.enablePlainRemoteSwift);
                    } else {
                        MediaProjectionHelper.getInstance().stopService(this);
                    }
                } else {
                    if (resultCode == Activity.RESULT_OK) {
                        MediaProjectionHelper.getInstance().createVirtualDisplay(requestCode, resultCode, data, true, true);
                        Intent intent = new Intent(getApplicationContext(), AutoService.class);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            getApplication().startForegroundService(intent);
                        } else {
                            getApplication().startService(intent);
                        }
                        intent = new Intent(getApplicationContext(), ServerService.class);
                        intent.putExtra("method", "start");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            getApplication().startForegroundService(intent);
                        } else {
                            getApplication().startService(intent);
                        }
                        Define.remoteDirectEnabled = true;
                        if (mineFragment != null && mineFragment.enableDirectRemoteSwift != null)
                            mineFragment.clickOn(mineFragment.enableDirectRemoteSwift);
                        ClientHelper.enabled(this);
                    } else {
                        MediaProjectionHelper.getInstance().stopService(this);
                    }
                }
        }
    }
}
