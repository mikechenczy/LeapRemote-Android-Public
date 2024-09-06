package org.mj.leapremote.util;

import android.app.Activity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.apache.commons.lang.StringEscapeUtils;
import org.mj.leapremote.Define;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ServerUtil {

    public static JSONObject getAddress() throws Exception {
        return JSON.parseObject(StringEscapeUtils.unescapeJava(GetTextRecord.getATxt("socket-new."+Define.baseDomain)));
    }

    public static JSONObject getAddressOld() throws Exception {
        AtomicReference<JSONObject> result = new AtomicReference<>(null);
        AtomicBoolean ipv4Failed = new AtomicBoolean(false);
        AtomicBoolean ipv6Failed = new AtomicBoolean(false);
        new Thread(() -> {
            try {
                JSONObject address = getAddressIpv4();
                if(result.get()==null)
                    result.set(address);
            } catch (Exception e) {
                e.printStackTrace();
                ipv4Failed.set(true);
            }
        }).start();
        new Thread(() -> {
            try {
                JSONObject address = getAddressIpv6();
                if(result.get()==null)
                    result.set(address);
            } catch (Exception e) {
                e.printStackTrace();
                ipv6Failed.set(true);
            }
        }).start();
        while (!(result.get()!=null || (ipv4Failed.get()&&ipv6Failed.get()))) {
            Thread.sleep(0);
        }
        if(result.get()!=null)
            return result.get();
        throw new Exception();
    }

    public static JSONObject getAddressIpv4() throws IOException, ParseException {
        return getAddressFromUrl("http://"+"socket."+Define.baseDomain+"/socket");
    }

    public static JSONObject getAddressIpv6() throws IOException, ParseException {
        return getAddressFromUrl("http://"+"socketipv6."+Define.baseDomain+"/socket");
    }

    public static JSONObject getAddressFromUrl(String url) throws IOException, ParseException {
        //long start = System.currentTimeMillis();
        HttpResponse response = new DefaultHttpClient().execute(new HttpGet(url));
        String content = EntityUtils.toString(response.getEntity(), "utf-8");
        JSONObject jsonObject = JSONObject.parseObject(content);
        if(jsonObject==null)
            throw new ParseException();
        //System.out.println("用时："+(System.currentTimeMillis()-start)+"ms");
        return jsonObject;
    }

    public static boolean ipv6Test() {
        try {
            URL url = new URL(Define.serverInfo.getString("ipv6-new"));
            return isReachable(url.getHost(), url.getPort()==-1? url.getDefaultPort() : url.getPort());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isReachable(String addr) {
        return isReachable(addr, 80);
    }

    public static boolean isReachable(String addr, int port) {
        return isReachable(addr, port, Define.connectTimeout);
    }

    public static boolean isReachable(String addr, int port, int timeout) {
        // Any Open port on other machine
        // openPort =  22 - ssh, 80 or 443 - webserver, 25 - mailserver etc.
        try {
            new Socket().connect(new InetSocketAddress(addr, port), timeout);
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean checkNetwork() {
        return isReachable("www.baidu.com");
    }


    public static void refreshIpv4AndIpv6() {
        new Thread(() -> {
            //if (ServerUtil.checkNetwork()) {
                boolean ipv6Support = ServerUtil.ipv6Test();
                if(Define.ipv6Support==ipv6Support)
                    return;
                Define.ipv6Support = ipv6Support;
                Define.server = Define.serverInfo.getString(Define.ipv6Support ? "ipv6-new" : "ipv4-new");
                Define.url = Define.serverInfo.getString(Define.ipv6Support ? "ipv6Url-new" : "ipv4Url-new");
            //}
        }).start();
    }
    public static void refreshWebSocketIpv4AndIpv6(Activity activity) {
        new Thread(() -> {
            //if (ServerUtil.checkNetwork()) {
                boolean ipv6Support = ServerUtil.ipv6Test();
                if(Define.ipv6Support==ipv6Support)
                    return;
                Define.ipv6Support = ipv6Support;
                Define.server = Define.serverInfo.getString(Define.ipv6Support ? "ipv6-new" : "ipv4-new");
                Define.url = Define.serverInfo.getString(Define.ipv6Support ? "ipv6Url-new" : "ipv4Url-new");
                activity.runOnUiThread(() -> ClientHelper.reconnect(activity));
            //}
        }).start();
    }



    /*public  static  boolean ping(String ipAddress) {
        try {
            ipAddress = InetAddress.getByName(ipAddress).getHostAddress();
            int  timeOut =  3000 ;   // 超时应该在3钞以上
            boolean status = false;      //  当返回值是true时，说明host是可用的，false则不可。
            status = InetAddress.getByName(ipAddress).isReachable(timeOut);
            System.out.println("12312312312 "+ipAddress+status);
            return status;
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return false;
    }*/

    /*public static boolean ping(String ipAddress) {
        try {
            AtomicInteger responseCode = new AtomicInteger();
            AtomicBoolean finish = new AtomicBoolean(false);
            new Thread(() -> {
                try {
                    URL url = new URL("http://"+ipAddress);
                    System.out.println(url);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(Define.connectTimeout);
                    conn.connect();
                    responseCode.set(conn.getResponseCode());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finish.set(true);
            }).start();
            while (!finish.get()) {
                Thread.sleep(0);
            }
            System.out.println(ipAddress + responseCode);
            System.out.println(!(String.valueOf(responseCode.get()).startsWith("4") || String.valueOf(responseCode.get()).startsWith("5")));
            return !(String.valueOf(responseCode.get()).startsWith("4") || String.valueOf(responseCode.get()).startsWith("5"));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }*/
}
