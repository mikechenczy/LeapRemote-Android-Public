package org.mj.leapremote.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.UUID;

public class DeviceUtil {
    private Context context;
    public DeviceUtil(Context context) {
        this.context = context;
    }


    public int getVersionCode() {
        int versionCode = 0;
        try {
            //获取软件版本号，对应AndroidManifest.xml下android:versionCode
            versionCode = context.getPackageManager().
                    getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    /**
     * 获取版本号名称
     *
     * @param
     * @return
     */
    public String getVerName() {
        String verName = "";
        try {
            verName = context.getPackageManager().
                    getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return verName;
    }


    /**
     * Return the version name of device's system.
     *
     * @return the version name of device's system
     */
    public String getSDKVersionName() {
        return Build.VERSION.RELEASE;
    }

    /**
     * Return version code of device's system.
     *
     * @return version code of device's system
     */
    public int getSDKVersionCode() {
        return Build.VERSION.SDK_INT;
    }

    /**
     * Return the android id of device.
     *
     * @return the android id of device
     */
    @SuppressLint("HardwareIds")
    public String getAndroidID() {
        String id = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
        if ("9774d56d682e549c".equals(id)) return "";
        return id == null ? "" : id;
    }

    /**
     * Return the manufacturer of the product/hardware.
     * <p>e.g. Xiaomi</p>
     *
     * @return the manufacturer of the product/hardware
     */
    public String getManufacturer() {
        return Build.MANUFACTURER;
    }

    /**
     * Return the model of device.
     * <p>e.g. MI2SC</p>
     *
     * @return the model of device
     */
    public String getModel() {
        return Build.MODEL;
    }

    /**
     * Return an ordered list of ABIs supported by this device. The most preferred ABI is the first
     * element in the list.
     *
     * @return an ordered list of ABIs supported by this device
     */
    public String[] getABIs() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return Build.SUPPORTED_ABIS;
        } else {
            if (!TextUtils.isEmpty(Build.CPU_ABI2)) {
                return new String[]{Build.CPU_ABI, Build.CPU_ABI2};
            }
            return new String[]{Build.CPU_ABI};
        }
    }


    private final String KEY_UDID = "KEY_UDID";
    private volatile static String udid;

    /**
     * Return the unique device id.
     * <pre>{1}{UUID(macAddress)}</pre>
     * <pre>{2}{UUID(androidId )}</pre>
     * <pre>{9}{UUID(random    )}</pre>
     *
     * @return the unique device id
     */
    public String getUniqueDeviceId() {
        return getUniqueDeviceId("", true);
    }

    /**
     * Return the unique device id.
     * <pre>android 10 deprecated {prefix}{1}{UUID(macAddress)}</pre>
     * <pre>{prefix}{2}{UUID(androidId )}</pre>
     * <pre>{prefix}{9}{UUID(random    )}</pre>
     *
     * @param prefix The prefix of the unique device id.
     * @return the unique device id
     */
    public String getUniqueDeviceId(String prefix) {
        return getUniqueDeviceId(prefix, true);
    }

    /**
     * Return the unique device id.
     * <pre>{1}{UUID(macAddress)}</pre>
     * <pre>{2}{UUID(androidId )}</pre>
     * <pre>{9}{UUID(random    )}</pre>
     *
     * @param useCache True to use cache, false otherwise.
     * @return the unique device id
     */
    public String getUniqueDeviceId(boolean useCache) {
        return getUniqueDeviceId("", useCache);
    }

    /**
     * Return the unique device id.
     * <pre>android 10 deprecated {prefix}{1}{UUID(macAddress)}</pre>
     * <pre>{prefix}{2}{UUID(androidId )}</pre>
     * <pre>{prefix}{9}{UUID(random    )}</pre>
     *
     * @param prefix   The prefix of the unique device id.
     * @param useCache True to use cache, false otherwise.
     * @return the unique device id
     */
    public String getUniqueDeviceId(String prefix, boolean useCache) {
        if (!useCache) {
            return getUniqueDeviceIdReal(prefix);
        }
        if (udid == null) {
            synchronized (DeviceUtil.class) {
                if (udid == null) {
                    UtilsBridge utilsBridge = new UtilsBridge(context);
                    final String id = utilsBridge.getSpUtils4Utils().getString(KEY_UDID, null);
                    if (id != null) {
                        udid = id;
                        return udid;
                    }
                    return getUniqueDeviceIdReal(prefix);
                }
            }
        }
        return udid;
    }

    private String getUniqueDeviceIdReal(String prefix) {
        try {
            final String androidId = getAndroidID();
            if (!TextUtils.isEmpty(androidId)) {
                return saveUdid(prefix + 2, androidId);
            }

        } catch (Exception ignore) {/**/}
        return saveUdid(prefix + 9, "");
    }



    private String saveUdid(String prefix, String id) {
        udid = getUdid(prefix, id);
        UtilsBridge utilsBridge=new UtilsBridge(context);
        utilsBridge.getSpUtils4Utils().put(KEY_UDID, udid);
        return udid;
    }

    private String getUdid(String prefix, String id) {
        if (id.equals("")) {
            return prefix + UUID.randomUUID().toString().replace("-", "");
        }
        return prefix + UUID.nameUUIDFromBytes(id.getBytes()).toString().replace("-", "");
    }
}
