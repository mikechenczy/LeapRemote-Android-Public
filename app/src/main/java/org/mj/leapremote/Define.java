package org.mj.leapremote;

import android.util.DisplayMetrics;

import com.alibaba.fastjson.JSONObject;
import org.mj.leapremote.model.Device;
import org.mj.leapremote.model.User;
import org.mj.leapremote.util.DataUtil;
import org.mj.leapremote.util.ServerUtil;
import org.mj.leapremote.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Define {
    public static final boolean isDebug = false;
    public static List<String> baseDomains = Arrays.asList("mjczy.top", "mjczy.life", "mjczy.xyz", "mjczy.club", "mjczy.info", "mjczy.org", "mjczy.net");
    public static String baseDomain = baseDomains.get(0);
    public static JSONObject serverInfo;
    public static String server;
    public static boolean ipGot;
    public static boolean ipv6Support;
    public static final int socketTimeout = 6000;
    public static final int connectTimeout = 6000;
    public static String version;
    public static List<Device> plainDevices = new ArrayList<>();
    public static List<Device> directDevices = new ArrayList<>();
    public static boolean agree;
    public static String neverShowMessageDialog;
    public static String neverShowVersionDialog;
    public static float scale = 2f;
    public static int quality = 5;
    public static long wait;
    public static float maxSpeed = 1024*1024;
    public static boolean speedLimited = false;
    public static int userId;
    public static String connectId;
    public static String connectPin;
    public static boolean controlled;
    public static int controlId;
    public static boolean direct;
    public static boolean remotePlainEnabled;
    public static boolean remoteDirectEnabled;
    public static boolean serverDirect;
    public static long waitTimeout = 7000;
    public static String url;
    public static User user;
    public static boolean autoLogin;
    public static String autoLoginUsername;
    public static String autoLoginPassword;
    public static boolean isExamining;
    public static String deviceId = Utils.getUniquePsuedoID();
    public static int defaultPort = 11451;
    public static String temporaryId;
    public static String temporaryPin;
    public static long networkAvailableWaitTime = 2000;
    public static DisplayMetrics displayMetrics;
    public static String host;
    public static String savedGestures;
    public static String controlSavedGestures;
    public static boolean isControlled;

    public static boolean isSoftwareActivated;

    static {
        System.setProperty("dns.server", "223.5.5.5,1.1.1.1,8.8.8.8");
        serverInfo = new JSONObject();
        serverInfo.put("ipv4-new", "http://192.168.1.99:2086/");
        serverInfo.put("ipv6-new", "http://ipv6.mjczy.top:2086/");
        serverInfo.put("ipv4Url-new", "ws://192.168.1.99:2086/websocket/");
        serverInfo.put("ipv6Url-new", "ws://ipv6.mjczy.top:2086/websocket/");
        serverInfo.put("host", "");
        server = serverInfo.getString("ipv4");
        url = serverInfo.getString("ipv4Url");
        host = serverInfo.getString("host");
        getIp();
        DataUtil.load();
    }

    public static void getIp() {
        if(isDebug) {
            serverInfo = new JSONObject();
            serverInfo.put("ipv4-new", "http://192.168.1.99:2086/");
            serverInfo.put("ipv6-new", "http://desktop.mjczy.top:2086/");
            serverInfo.put("ipv4Url-new", "ws://192.168.1.99:2086/websocket/");
            serverInfo.put("ipv6Url-new", "ws://desktop.mjczy.top:2086/websocket/");
            serverInfo.put("host", "");
            server = serverInfo.getString("ipv4-new");
            url = serverInfo.getString("ipv4Url-new");
            host = serverInfo.getString("host");
            System.out.println(!Define.ipv6Support && Utils.stringIsNull(host));
            System.out.println("HOST: "+host);
            ipGot = true;
            return;
        }
        new Thread(() -> {
            ipGot = false;
            ipv6Support = ServerUtil.ipv6Test();
            try {
                serverInfo = ServerUtil.getAddress().getJSONObject("leapremote");
                System.out.println(serverInfo);
                server = ipv6Support ? serverInfo.getString("ipv6-new") : serverInfo.getString("ipv4-new");
                url = ipv6Support ? serverInfo.getString("ipv6Url-new") : serverInfo.getString("ipv4Url-new");
                host = serverInfo.getString("host");
                System.out.println(url);
                ipGot = true;
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
            getDomainAndIp();
        }).start();
    }

    public static void getDomainAndIp() {
        new Thread(() -> {
            ipGot = false;
            AtomicBoolean domainGot = new AtomicBoolean(false);
            for (String domain : baseDomains) {
                new Thread(() -> {
                    while (!domainGot.get()) {
                        boolean success = ServerUtil.isReachable(domain);
                        if(success) {
                            domainGot.set(true);
                            baseDomain = domain;
                        }
                    }
                }).start();
            }
            while (!domainGot.get()) {
                try {
                    Thread.sleep(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            while (true) {
                try {
                    Thread.sleep(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ipv6Support = ServerUtil.ipv6Test();
                try {
                    serverInfo = ServerUtil.getAddress().getJSONObject("leapremote");
                    server = ipv6Support ? serverInfo.getString("ipv6-new") : serverInfo.getString("ipv4-new");
                    url = ipv6Support ? serverInfo.getString("ipv6Url-new") : serverInfo.getString("ipv4Url-new");
                    host = serverInfo.getString("host");
                    ipGot = true;
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
