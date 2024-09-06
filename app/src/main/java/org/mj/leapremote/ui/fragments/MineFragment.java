package org.mj.leapremote.ui.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.SwitchCompat;

import com.alibaba.fastjson.JSONObject;
import com.leon.lib.settingview.LSettingItem;
import com.mask.mediaprojection.utils.MediaProjectionHelper;
import org.mj.leapremote.Define;
import org.mj.leapremote.R;

import org.mj.leapremote.service.AutoService;
import org.mj.leapremote.service.HttpService;
import org.mj.leapremote.service.ServerService;
import org.mj.leapremote.ui.activities.GesturesActivity;
import org.mj.leapremote.ui.activities.MainActivity;
import org.mj.leapremote.util.ClientHelper;
import org.mj.leapremote.util.UpdateManager;
import org.mj.leapremote.util.Utils;

import java.lang.reflect.Field;

public class MineFragment extends Fragment {
    public View view;
    public LSettingItem enablePlainRemoteSwift;
    public LSettingItem enableDirectRemoteSwift;
    public LSettingItem gestures;
    public LSettingItem login;
    public LSettingItem share;
    public LSettingItem checkVersion;

    public ProgressDialog progressDialog;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_mine, container, false);
        ((TextView) view.findViewById(R.id.versionText)).setText(getString(R.string.versionText) + Define.version);
        enablePlainRemoteSwift = view.findViewById(R.id.enable_plain_remote_swift);
        enableDirectRemoteSwift = view.findViewById(R.id.enable_direct_remote_swift);
        gestures = view.findViewById(R.id.record_gesture);
        login = view.findViewById(R.id.login);
        share = view.findViewById(R.id.share);
        checkVersion = view.findViewById(R.id.check_version);
        initData();
        initListeners();
        return view;
    }

    private void initData() {
        if (Define.remotePlainEnabled)
            enablePlainRemoteSwift.clickOn();
        if (Define.remoteDirectEnabled)
            enableDirectRemoteSwift.clickOn();
    }

    private void initListeners() {
        try {
            Field switchItem = enablePlainRemoteSwift.getClass().getDeclaredField("mRightIcon_switch");
            switchItem.setAccessible(true);
            SwitchCompat switchCompat = (SwitchCompat) switchItem.get(enablePlainRemoteSwift);
            switchCompat.setOnClickListener(v -> onEnablePlainRemoteClick());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        enablePlainRemoteSwift.setmOnLSettingItemClick(this::onEnablePlainRemoteClick);
        try {
            Field switchItem = enableDirectRemoteSwift.getClass().getDeclaredField("mRightIcon_switch");
            switchItem.setAccessible(true);
            SwitchCompat switchCompat = (SwitchCompat) switchItem.get(enableDirectRemoteSwift);
            switchCompat.setOnClickListener(v -> onEnableDirectRemoteClick());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        enableDirectRemoteSwift.setmOnLSettingItemClick(this::onEnableDirectRemoteClick);
        gestures.setmOnLSettingItemClick(() -> {
            if (!Utils.isAccessibilitySettingsOn(getActivity())) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                Toast.makeText(getActivity(), getString(R.string.enable_the_function), Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(getActivity(), GesturesActivity.class));
        });
        login.setmOnLSettingItemClick(() -> Toast.makeText(getActivity(), R.string.function_not_available, Toast.LENGTH_SHORT).show());
        share.setmOnLSettingItemClick(() -> Utils.shareText(MainActivity.INSTANCE, getString(R.string.shareContent) +
                getString(R.string.website_url)));
        checkVersion.setmOnLSettingItemClick(() -> new Thread(() -> {
            JSONObject s = HttpService.needUpdate();
            if(s==null)
                MainActivity.INSTANCE.runOnUiThread(() -> Toast.makeText(MainActivity.INSTANCE, getString(R.string.cannot_connect_to_server), Toast.LENGTH_SHORT).show());
            else {
                MainActivity.INSTANCE.runOnUiThread(() -> {
                    if (s.get("force")==null) {
                        new AlertDialog.Builder(MainActivity.INSTANCE).setTitle(R.string.upToDate).setMessage(R.string.upToDate).setPositiveButton(R.string.admit, null).show();
                    } else {
                        new AlertDialog.Builder(MainActivity.INSTANCE).setTitle(R.string.newVersion).setMessage(getString(R.string.version)+":" + s.getString("version") + "\n"+getString(R.string.content)+":" + s.getString("description") + "\n"+(s.getBoolean("force")?getString(R.string.forceUpdate):"")+getString(R.string.toUpdate))
                                .setPositiveButton(R.string.yes, (dialog, which) -> {
                                    progressDialog = new ProgressDialog(MainActivity.INSTANCE);
                                    progressDialog.setTitle(R.string.downloadingUpdate);
                                    //设置水平进度条
                                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                    //设置进度条最大值为100
                                    progressDialog.setMax(100);
                                    //设置进度条当前值为0
                                    progressDialog.setProgress(0);
                                    progressDialog.setCancelable(!s.getBoolean("force"));// 设置是否可以通过点击Back键取消
                                    progressDialog.setCanceledOnTouchOutside(false);
                                    progressDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.btn_cancel), (dialog1, which1) -> {
                                        if (s.getBoolean("force")) {
                                            MainActivity.INSTANCE.finish();
                                            System.exit(0);
                                        }
                                    });
                                    UpdateManager updateManager = new UpdateManager(this);
                                    updateManager.update();
                                    progressDialog.setOnDismissListener((dialog1 -> updateManager.downloadApkThread.interrupt()));
                                    progressDialog.show();
                                })
                                .setCancelable(!s.getBoolean("force"))
                                .setNegativeButton(R.string.no, (dialog, which) -> {
                                    if(s.getBoolean("force")) {
                                        MainActivity.INSTANCE.finish();
                                        System.exit(0);
                                    }
                                }).show();
                    }
                });
            }
        }).start());
    }

    private void onEnablePlainRemoteClick() {
        if(isClickedOn(enablePlainRemoteSwift)) {
            clickOn(enablePlainRemoteSwift);
            if (!Define.remotePlainEnabled) {
                if (!Utils.isAccessibilitySettingsOn(getActivity())) {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    Toast.makeText(getActivity(), getString(R.string.enable_the_function), Toast.LENGTH_SHORT).show();
                    return;
                }
                Define.serverDirect = false;
                if(!Define.remoteDirectEnabled) {
                    MediaProjectionHelper.getInstance().startService(getActivity());
                    return;
                }
                Define.remotePlainEnabled = true;
                clickOn(enablePlainRemoteSwift);
                ClientHelper.enableRemote(getActivity().getApplicationContext());
            }
        } else {
            if(!Define.remoteDirectEnabled) {
                doNormalStop();
            }
            Define.remotePlainEnabled = false;
            ClientHelper.disableRemote(getActivity());
        }
    }

    private void onEnableDirectRemoteClick() {
        if(isClickedOn(enableDirectRemoteSwift)) {
            clickOn(enableDirectRemoteSwift);
            if (!Define.remoteDirectEnabled) {
                if (!Utils.isAccessibilitySettingsOn(getActivity())) {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    Toast.makeText(getActivity(), getString(R.string.enable_the_function), Toast.LENGTH_SHORT).show();
                    return;
                }
                Define.serverDirect = true;
                if(!Define.remotePlainEnabled) {
                    MediaProjectionHelper.getInstance().startService(getActivity());
                    return;
                }
                Intent intent = new Intent(getActivity().getApplicationContext(), ServerService.class);
                intent.putExtra("method", "start");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    getActivity().getApplication().startForegroundService(intent);
                } else {
                    getActivity().getApplication().startService(intent);
                }
                Define.remoteDirectEnabled = true;
                clickOn(enableDirectRemoteSwift);
                ClientHelper.enabled(getActivity());
            }
        } else {
            if(!Define.remotePlainEnabled) {
                doNormalStop();
            }
            Define.remoteDirectEnabled = false;
            ServerService.mService.stop();
            ClientHelper.enabled(getActivity());
        }
    }

    private void doNormalStop() {
        if(AutoService.mService!=null)
            AutoService.mService.stopSelf();
        MediaProjectionHelper.getInstance().stopService(getActivity());
    }

    public void clickOn(LSettingItem settingItem) {
        try {
            Field switchItem = LSettingItem.class.getDeclaredField("mRightStyle");
            switchItem.setAccessible(true);
            int mRightStyle = (int) switchItem.get(settingItem);
            switchItem = LSettingItem.class.getDeclaredField("mRightIcon_check");
            switchItem.setAccessible(true);
            AppCompatCheckBox mRightIcon_check = (AppCompatCheckBox) switchItem.get(settingItem);
            switchItem = LSettingItem.class.getDeclaredField("mRightIcon_switch");
            switchItem.setAccessible(true);
            SwitchCompat mRightIcon_switch = (SwitchCompat) switchItem.get(settingItem);
            switch (mRightStyle) {
                case 2:
                    //选择框切换选中状态
                    mRightIcon_check.setChecked(!mRightIcon_check.isChecked());
                    break;
                case 3:
                    //开关切换状态
                    mRightIcon_switch.setChecked(!mRightIcon_switch.isChecked());
                    break;
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public boolean isClickedOn(LSettingItem settingItem) {
        try {
            Field switchItem = LSettingItem.class.getDeclaredField("mRightStyle");
            switchItem.setAccessible(true);
            int mRightStyle = (int) switchItem.get(settingItem);
            switchItem = LSettingItem.class.getDeclaredField("mRightIcon_check");
            switchItem.setAccessible(true);
            AppCompatCheckBox mRightIcon_check = (AppCompatCheckBox) switchItem.get(settingItem);
            switchItem = LSettingItem.class.getDeclaredField("mRightIcon_switch");
            switchItem.setAccessible(true);
            SwitchCompat mRightIcon_switch = (SwitchCompat) switchItem.get(settingItem);
            switch (mRightStyle) {
                case 2:
                    //选择框切换选中状态
                    return mRightIcon_check.isChecked();
                case 3:
                    //开关切换状态
                    return mRightIcon_switch.isChecked();
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
        return false;
    }
}
