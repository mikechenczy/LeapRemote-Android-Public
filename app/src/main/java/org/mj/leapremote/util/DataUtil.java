package org.mj.leapremote.util;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.mj.leapremote.BaseApplication;
import org.mj.leapremote.Define;

import static android.content.Context.MODE_PRIVATE;

public class DataUtil {
    public static SharedPreferences sharedPreferences = BaseApplication.getInstance().getSharedPreferences("yan",MODE_PRIVATE);
    public static void save() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("agree", Define.agree);
        if(Define.user!=null) {
            editor.putString("username", Define.user.getUsername());
            if(Define.autoLogin) {
                editor.putBoolean("autoLogin", true);
                editor.putString("password", Define.user.getPassword());
            } else {
                editor.putBoolean("autoLogin", false);
            }
        }

        if(Define.neverShowMessageDialog!=null)
            editor.putString("neverShowMessageDialog", Define.neverShowMessageDialog);
        if(Define.neverShowVersionDialog !=null)
            editor.putString("neverShowVersionDialog", Define.neverShowVersionDialog);
        editor.putString("directDevices", DevicesUtil.getDirectDevicesJSONString());
        editor.putString("savedGestures", Define.savedGestures);
        editor.putBoolean("isSoftwareActivated", Define.isSoftwareActivated);
        editor.putString("deviceId", Define.deviceId);
        editor.apply();
    }

    public static void load() {
        Define.autoLoginUsername = sharedPreferences.getString("username", "");
        Define.autoLoginPassword = sharedPreferences.getString("password", "");
        Define.autoLogin = sharedPreferences.getBoolean("autoLogin", false);
        Define.agree = sharedPreferences.getBoolean("agree", false);
        Define.neverShowMessageDialog = sharedPreferences.getString("neverShowMessageDialog", null);
        Define.neverShowVersionDialog = sharedPreferences.getString("neverShowVersionDialog", null);
        Define.savedGestures = sharedPreferences.getString("savedGestures", "[]");
        Define.isSoftwareActivated = sharedPreferences.getBoolean("isSoftwareActivated", false);
        Define.deviceId = sharedPreferences.getString("deviceId", Utils.getUniquePsuedoID());
        DevicesUtil.setDirectDevicesJSONString(sharedPreferences.getString("directDevices", null));
    }
}
