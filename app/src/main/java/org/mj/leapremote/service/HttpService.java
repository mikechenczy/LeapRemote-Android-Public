package org.mj.leapremote.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.mj.leapremote.Define;
import org.mj.leapremote.model.Device;
import org.mj.leapremote.model.User;
import org.mj.leapremote.ui.activities.LogoActivity;
import org.mj.leapremote.ui.activities.MainActivity;
import org.mj.leapremote.util.DeviceUtil;
import org.mj.leapremote.util.HttpUtils;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;
import org.mj.leapremote.util.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mj.leapremote.util.ServerUtil.refreshIpv4AndIpv6;

public class HttpService {
    public static HttpClient httpClient;

    static {
        BasicHttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, Define.connectTimeout);
        HttpConnectionParams.setSoTimeout(httpParams, Define.socketTimeout);
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", new PlainSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        ClientConnectionManager connManager = new ThreadSafeClientConnManager(httpParams, schemeRegistry);
        httpClient = new DefaultHttpClient(connManager, httpParams);
    }

    public static String httpGet(String url) throws IOException {
        System.out.println(url);
        try {
            HttpGet httpGet = new HttpGet(url);
            if(!Define.ipv6Support && !Utils.stringIsNull(Define.host))
                httpGet.setHeader("Host", Define.host);
            HttpResponse res = httpClient.execute(httpGet);
            return EntityUtils.toString(res.getEntity(), "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Http Get Failed: "+url);
        }
    }

    public static JSONObject httpGetJSON(String url) throws IOException {
        String content = httpGet(url);
        try {
            return JSONObject.parseObject(content);
        } catch (Exception e){
            throw new IOException("JSON Parsing Error: "+content);
        }
    }

    public static boolean ipGotWait() {
        AtomicBoolean stop = new AtomicBoolean(false);
        new Thread(() -> {
            try {
                Thread.sleep(Define.connectTimeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!Define.ipGot)
                stop.set(true);
        }).start();
        while (!Define.ipGot && !stop.get()) {
            try {
                Thread.sleep(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return Define.ipGot;
    }

    public static User login(String usernameOrEmail, String password) {
        if(!ipGotWait())
            return null;
        Map<String, String> map = new HashMap<>();
        map.put("username", usernameOrEmail);
        map.put("password", password);
        map.put("device", "Android");
        map.put("ipAddress", getPublicIp());
        map.put("version", Define.version);
        DeviceUtil deviceUtil = new DeviceUtil(LogoActivity.INSTANCE!=null?LogoActivity.INSTANCE: MainActivity.INSTANCE);
        map.put("os", deviceUtil.getModel());
        map.put("androidVersion", deviceUtil.getSDKVersionName());
        map.put("deviceId", deviceUtil.getUniqueDeviceId());
        map.put("package", (LogoActivity.INSTANCE!=null?LogoActivity.INSTANCE: MainActivity.INSTANCE).getPackageName());
        String url = Define.server + "user/login";
        url = HttpUtils.setParamToUrl(url, map);
        try {
            JSONObject json = httpGetJSON(url);
            int errno = json.getInteger("errno");
            if (errno == 0) {
                if(json.containsKey("isExamining") && json.getBoolean("isExamining"))
                    Define.isExamining = true;
                return JSON.parseObject(json.getString("user"), new TypeReference<User>(){});
            }
            User user = new User();
            user.setUserId(errno);
            return user;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static User register(Map<String, String> map) {
        if(!ipGotWait())
            return null;
        String url = Define.server + "user/register";
        url = HttpUtils.setParamToUrl(url, map);
        try {
            JSONObject json = httpGetJSON(url);
            int errno = json.getInteger("errno");
            if (errno == 0) {
                return JSON.parseObject(json.getString("user"), new TypeReference<User>(){});
            }
            User user = new User();
            user.setUserId(errno);
            return user;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject needUpdate() {
        if(!ipGotWait())
            return null;
        Map<String, String> map = new HashMap<>();
        map.put("version", Define.version);
        String url = Define.server + "core/checkVersionAndroidNew";
        url = HttpUtils.setParamToUrl(url, map);
        try {
            return httpGetJSON(url);
        } catch (IOException ioException) {
            return null;
        }
    }

    public static boolean logout() {
        refreshIpv4AndIpv6();
        String url = Define.server + "user/logout";
        try {
            return httpGet(url).equals("true");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Device getDevice(String connectId, String connectPin) {
        refreshIpv4AndIpv6();
        Map<String, String> map = new HashMap<>();
        map.put("connectId", connectId);
        map.put("connectPin", connectPin);
        String url = Define.server + "core/getDeviceByConnectId";
        url = HttpUtils.setParamToUrl(url, map);
        try {
            JSONObject json = httpGetJSON(url);
            Device device = new Device();
            device.setConnectId(connectId);
            device.setConnectPin(connectPin);
            device.setMode(Device.MODE_PLAIN);
            device.setName(json.getString("name"));
            device.setStatus(json.getInteger("status"));
            device.setDeviceId(json.getString("deviceId"));
            return device;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getMessageContent() {
        refreshIpv4AndIpv6();
        String url = Define.server + "core/getMessageContent?version="+Define.version+"&device=android";
        try {
            return httpGet(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONArray helpPingIp(JSONArray hosts) {
        refreshIpv4AndIpv6();
        String url = Define.server + "core/helpPingIp?deviceId="+Define.deviceId+"&hosts="+ URLEncoder.encode(hosts.toString());
        try {
            return httpGetJSON(url).getJSONArray("result");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject publicIp(JSONArray hosts) {
        refreshIpv4AndIpv6();
        String url = Define.server + "core/publicIp?deviceId="+Define.deviceId+"&hosts="+ URLEncoder.encode(hosts.toString());
        try {
            return httpGetJSON(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int forgetPasswordEmail(String username) {
        if (!ipGotWait())
            return -1;
        String url = Define.server + "user/forgetPasswordEmail";
        Map<String, String> map = new HashMap<>();
        map.put("username", username);
        url = HttpUtils.setParamToUrl(url, map);
        try {
            return httpGetJSON(url).getInteger("errno");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int forgetPassword(Map<String, String> map) {
        if (!ipGotWait())
            return -1;
        String url = Define.server + "user/forgetPassword";
        url = HttpUtils.setParamToUrl(url, map);
        try {
            return httpGetJSON(url).getInteger("errno");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static String getPublicIp() {
        try {
            String path = "http://www.net.cn/static/customercare/yourip.asp";// 要获得html页面内容的地址
            URL url = new URL(path);// 创建url对象
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();// 打开连接
            conn.setRequestProperty("contentType", "GBK"); // 设置url中文参数编码
            conn.setConnectTimeout(5 * 1000);// 请求的时间
            conn.setRequestMethod("GET");// 请求方式
            InputStream inStream = conn.getInputStream();
            // readLesoSysXML(inStream);
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    inStream, "GBK"));
            StringBuilder builder = new StringBuilder();
            String line;
            // 读取获取到内容的最后一行,写入
            while ((line = in.readLine()) != null) {
                builder.append(line);
            }
            List<String> ips = new ArrayList<>();
            //用正则表达式提取String字符串中的IP地址
            String regEx="((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)";
            String str = builder.toString();
            Pattern p = Pattern.compile(regEx);
            Matcher m = p.matcher(str);
            while (m.find()) {
                String result = m.group();
                ips.add(result);
            }
            // 返回公网IP值
            return ips.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("获取公网IP连接超时");
            return "";
        }
    }
}
