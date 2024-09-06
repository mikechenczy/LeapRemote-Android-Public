package org.mj.leapremote.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import androidx.core.app.ActivityCompat;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.mj.leapremote.R;
import org.mj.leapremote.service.AutoService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

public class Utils {
    public static boolean stringIsEmpty(String s) {
        return s==null || s.replaceAll(" ", "").replaceAll("\r", "").replaceAll("\n", "").isEmpty();
    }

    public static boolean check(String username, String password) {
        /*return !(username.contains(" ") || password.contains(" ") || username.equals("") || password.equals("") || username.contains(",") || username.contains("&")
                || username.contains("?") || password.contains("&") || password.contains("?") || username.contains("=") || password.contains("=") || username.contains("/")
                || password.contains("/") || username.contains("\\") || password.contains("\\"));*/
        return !(username.contains(" ") || password.contains(" ") || username.equals("") || password.equals("") || username.length()>=15 || password.length()>=20);
    }

    public static boolean check(String password) {
        return !(password.contains(" ") || password.equals("") || password.length()>=20);
    }

    public static boolean checkUsername(String username) {
        return !(username.contains(" ") || username.equals("") || username.length()>=15);
    }
    public static boolean isEmail(String str) {
        boolean isEmail = false;
        String expr = "^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})$";

        if (str.matches(expr)) {
            isEmail = true;
        }
        if(isEmail) {
            return str.endsWith("@126.com") || str.endsWith("@163.com") || str.endsWith("@qq.com") || str.endsWith("@sina.com") ||
                    str.endsWith("@sina.cn") || str.endsWith("@outlook.com") || str.endsWith("@gmail.com") || str.endsWith("@mjczy.top");
        }
        return false;
    }

    public static String string(String s) {
        return s==null?"":s;
    }

    public static void sleep(){
        try {
            Thread.sleep(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void shareText(Context context, String text) {
        if(context == null || text == null)
            return;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.shareTitle)));
    }

    public static String decodeString(String s) {
        try {
            return URLDecoder.decode(s, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return s;
        }
    }

    public static boolean checkFileName(String fileName) {
        return fileName != null && (!(fileName.contains("\\") || fileName.contains("/") || fileName.contains(":") || fileName.contains("*") || fileName.contains("?")
                || fileName.contains("\"") || fileName.contains("<") || fileName.contains(">") || fileName.contains("|") || fileName.equals("")));
    }

    public static String getHumanFileSize(File file){
        if(file==null)
            return null;
        long blockSize=0;
        try {
            if(file.isDirectory()){
                blockSize = getFileSizes(file);
            }else{
                blockSize = getFileSize(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return FormatFileSize(blockSize);
    }

    private static long getFileSize(File file)
    {
        return file.length();
    }

    private static long getFileSizes(File f)
    {
        long size = 0;
        File files[] = f.listFiles();
        for (int i = 0; i < files.length; i++)
            size = size + (files[i].isDirectory()?getFileSizes(files[i]):getFileSize(files[i]));
        return size;
    }

    public static String FormatFileSize(long fileS)
    {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize="0B";
        if(fileS==0){
            return wrongSize;
        }
        if (fileS < 1024){
            fileSizeString = df.format((double) fileS) + "B";
        }
        else if (fileS < 1048576){
            fileSizeString = df.format((double) fileS / 1024) + "KB";
        }
        else if (fileS < 1073741824){
            fileSizeString = df.format((double) fileS / 1048576) + "MB";
        }
        else{
            fileSizeString = df.format((double) fileS / 1073741824) + "GB";
        }
        return fileSizeString;
    }

    public static String byteArrayToString(byte[] bytes) {
        if(bytes==null)
            return null;
        return new BASE64Encoder().encode(bytes);
    }

    public static byte[] stringToByteArray(String str) {
        if(str==null)
            return null;
        try {
            return new BASE64Decoder().decodeBuffer(str);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Serializable byteArrayToObject(byte[] bytes) {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        try {
            Serializable result;
            ObjectInputStream oos = new ObjectInputStream(bais);
            result = (Serializable) oos.readObject();
            oos.close();
            bais.close();
            return result;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] objectToByteArray(Serializable obj) {
        byte[] result;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.flush();
            result = baos.toByteArray();
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Serializable stringToObject(String str) {
        return byteArrayToObject(stringToByteArray(str));
    }

    public static String objectToString(Serializable obj) {
        return byteArrayToString(objectToByteArray(obj));
    }

    public static String getHumanTime(long ms) {
        if(ms<=0)
            return "已到期";
        long day = ms / 1000 / 60 / 60 / 24;
        long hour = ms / 1000 / 60 / 60 % 24;
        long min = ms / 1000 / 60 % 60;
        long sec = ms / 1000 % 60;
        String result;
        if(day>0)
            result = day+"天"+hour+"时"+min+"分";
        else if(hour>0)
            result = hour+"时"+min+"分";
        else if(min>0)
            result = min+"分"+sec+"秒";
        else
            result = sec+"秒";
        return result;
    }

    public static int[] sortIntArrayFromBiggest(int[] arr){
        for (int i = 1; i < arr.length; i++) {
            for (int j=i;j>0;j--){
                if (arr[j]<=arr[j-1]){
                    break;
                }else{
                    int temp = arr[j];
                    arr[j] = arr[j-1];
                    arr[j-1] = temp;
                }
            }
        }
        return arr;
    }

    public static int[] integerArrayToIntArray(Integer[] array) {
        if(array==null)
            return null;
        if(array.length==0)
            return new int[]{};
        int[] result = new int[array.length];
        for(int i=0;i<array.length;i++)
            result[i] = array[i];
        return result;
    }


    public static String getVersion(Context context) {
        PackageInfo pkg;
        String versionName = "";
        try {
            pkg = context.getPackageManager().getPackageInfo(context.getApplicationContext().getPackageName(), 0);
            versionName = pkg.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionName;
    }

    public static void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, new String[]{
                        "android.permission.READ_EXTERNAL_STORAGE",
                        "android.permission.WRITE_EXTERNAL_STORAGE" },1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isAccessibilitySettingsOn(Context mContext) {
        int accessibilityEnabled = 0;
        final String service = mContext.getPackageName() + "/" + AutoService.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
// Log.v(TAG, "accessibilityEnabled = " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
// Log.e(TAG, "Error finding setting, default accessibility to not found: "
// + e.getMessage());
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
// Log.v(TAG, "***ACCESSIBILITY IS ENABLED*** -----------------");
            String settingValue = Settings.Secure.getString(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();

// Log.v(TAG, "-------------- > accessibilityService :: " + accessibilityService + " " + service);
                    if (accessibilityService.equalsIgnoreCase(service)) {
// Log.v(TAG, "We've found the correct setting - accessibility is switched on!");
                        return true;
                    }
                }
            }
        } else {
// Log.v(TAG, "***ACCESSIBILITY IS DISABLED***");
        }

        return false;
    }

    public static DisplayMetrics getMetricsFull(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowMgr = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        windowMgr.getDefaultDisplay().getRealMetrics(metrics);
        return metrics;
    }

    public static String getUniquePsuedoID() {
        String serial;
        String m_szDevIDShort = Build.BOARD+Build.BRAND+Build.CPU_ABI+Build.DEVICE+Build.DISPLAY+Build.HOST+Build.ID+Build.MANUFACTURER+Build.MODEL+Build.PRODUCT+Build.TAGS+Build.TYPE+Build.USER + new Random().nextInt(90000)+10000;
        //13 位
        try { serial = android.os.Build.class.getField("SERIAL").get(null).toString();
            // API>=9 使用serial号
            return Md5(new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString());
        } catch (Exception exception) {
            // rial需要一个初始化
            serial = "serial";
            // 随便一个初始化
        }
        return Md5(new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString());
    }

    public static String Md5(String sourceStr) {
        String result = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(sourceStr.getBytes());
            byte[] b = md.digest();
            int i;
            StringBuilder buf = new StringBuilder();
            for (byte value : b) {
                i = value;
                if (i < 0) i += 256;
                if (i < 16) buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            result = buf.substring(8, 24);
            System.out.println("result: " + result);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean checkPort(String port) {
        try {
            int p = Integer.parseInt(port);
            return p > 0 && p <= 65536;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isIFrame(byte[] data) {
        if( data == null || data.length < 5) {
            return false;
        }
        Log.i("IFrame", "data0:"+toHex(data[0])+"--data[1]:"+toHex(data[1])+"--data[2]:"+toHex(data[2])+
                "--data3:"+toHex(data[3])+"--data4:"+toHex(data[4]));
        if (data[0] == 0x0
                && data[1] == 0x0
                && data[2] == 0x0
                && data[3] == 0x1
                && data[4] == 0x67) {
            Log.d("IFrame", "check I frame data: " + Arrays.toString(Arrays.copyOf(data, 5)));
            return true;
        }
        byte nalu = data[4];
        return ((nalu & 0x1F) == 5) ? true : false;
    }

    public static String toHex(byte b) {
        String result = Integer.toHexString(b & 0xFF);
        if (result.length() == 1) {
            result = '0' + result;
        }
        return result;
    }

    public static JSONArray simplifyPoints(JSONArray points) {
        if(points.size()<3)
            return points;
        JSONArray result = new JSONArray();
        result.add(points.getJSONObject(0));
        long duration = 0;
        int basePointIndex = 0;
        for(int i=2;i<points.size();i++) {
            JSONObject basePoint = points.getJSONObject(basePointIndex);
            JSONObject lastPoint = points.getJSONObject(i-1);
            JSONObject point = points.getJSONObject(i);
            if(Math.abs(
                    Math.atan(
                            (point.getFloatValue("y")-basePoint.getFloatValue("y"))
                            /
                            (point.getFloatValue("x")-basePoint.getFloatValue("x")))
                    -
                    Math.atan(
                            (lastPoint.getFloatValue("y")-basePoint.getFloatValue("y"))
                            /
                            (lastPoint.getFloatValue("x")-basePoint.getFloatValue("x"))))<0.03490658503988659) {
                duration+=lastPoint.getLongValue("duration");
            } else {
                basePointIndex = i-1;
                lastPoint.replace("duration", lastPoint.getLongValue("duration")+duration);
                duration=0;
                result.add(lastPoint);
            }
            if(i==points.size()-1) {
                point.replace("duration", point.getLongValue("duration")+duration);
                result.add(point);
            }
        }
        return result;
    }
}
