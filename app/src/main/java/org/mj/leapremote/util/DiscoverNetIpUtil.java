package org.mj.leapremote.util;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.mj.leapremote.R;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DiscoverNetIpUtil{
    /**
     * 获取ip地址
     *
     * @return
     */
    public static String getHostIP() {
        return getIpAddress("eth0");
    }

    /**
     * Get Ip address 自动获取IP地址
     * 可以传入eth1，eth0,wlan0,等
     *
     */
    public static String getIpAddress(String ipType) {
        String hostIp = null;
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                System.out.println(ni.getName());
                if (ni.getName().equals(ipType)) {
                    Enumeration<InetAddress> ias = ni.getInetAddresses();
                    while (ias.hasMoreElements()) {

                        ia = ias.nextElement();
                        String ip = ia.getHostAddress();
                        if (ia instanceof Inet6Address) {
                            if(ip != null && ip.startsWith("2")) {
                                return ia.getHostAddress();
                            }
                            continue;// skip ipv6
                        }

                        // 过滤掉127段的ip地址
                        if (!"127.0.0.1".equals(ip)) {
                            hostIp = ia.getHostAddress();
                            break;
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return hostIp;
    }

    public static List<String> getAllInet6Hosts() {
        List<String> result = new ArrayList<>();
        for(Inet6Address inet6Address : getAllInet6Addresses()) {
            result.add(inet6Address.getHostAddress());
        }
        return result;
    }

    public static List<String> getAllPublicInet6Hosts() {
        List<String> result = new ArrayList<>();
        for(String ip : getAllInet6Hosts()) {
            if(ip!=null && !ip.contains("%") && (ip.startsWith("2") || ip.startsWith("3")))
                result.add(ip);
        }
        return result;
    }

    public static JSONArray getAllInet6HostsAndNames() {
        JSONArray result = new JSONArray();
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("ip", ia.getHostAddress());
                        jsonObject.put("niName", ni.getName());
                        result.add(jsonObject);
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static JSONArray getAllPublicInet6HostsAndNames(Context context) {
        JSONArray result = new JSONArray();
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                if (ni.isLoopback() || !ni.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                JSONObject jsonObject = new JSONObject();
                boolean rmnet = false;
                boolean rmnetAvailable = false;
                boolean gotIp = false;
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        if (!ia.isLinkLocalAddress() &&
                                !ia.isSiteLocalAddress() &&
                                !ia.isMulticastAddress() &&
                                !ia.isAnyLocalAddress() &&
                                ia.getHostAddress() !=null &&
                                (ia.getHostAddress().startsWith("2") || ia.getHostAddress().startsWith("3")) &&
                                !gotIp) {// &&
                            //!ni.getName().contains("rmnet")) {
                            rmnet = ni.getName().contains("rmnet");
                            jsonObject.put("ip", ia.getHostAddress());
                            jsonObject.put("niName", rmnet?context.getText(R.string.rmnet):ni.getName());
                            gotIp = true;
                        }
                        /*String ip = ia.getHostAddress();
                        if(ip!=null && !ip.contains("%") && !ni.getName().contains("rmnet") && (ip.startsWith("2") || ip.startsWith("3"))) {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("ip", ip);
                            jsonObject.put("niName", ni.getName());
                            System.out.println(jsonObject);
                            System.out.println(ia);
                            System.out.println(ni);
                            result.add(jsonObject);
                        }*/
                    } else {
                        rmnetAvailable = true;
                    }
                }
                System.out.println(jsonObject);
                System.out.println(ni);
                if(gotIp) {
                    if(rmnet) {
                        if(rmnetAvailable) {
                            result.add(jsonObject);
                        }
                    } else {
                        result.add(jsonObject);
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static List<Inet6Address> getAllInet6Addresses() {
        List<Inet6Address> result = new ArrayList<>();
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                System.out.println(ni.getName());
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        result.add((Inet6Address) ia);
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 创建一个线程向本地所有ip发送一个数据
     */
    public static void sendDataToLocal() {
        //局域网内存在的ip集合
        final List<String> ipList = new ArrayList<>();
        final Map<String, String> map = new HashMap<>();
        //获取本机所在的局域网地址
        String hostIP = getHostIP();
        int lastIndexOf = hostIP.lastIndexOf(".");
        final String substring = hostIP.substring(0, lastIndexOf + 1);
        new Thread(() -> {
            DatagramPacket dp = new DatagramPacket(new byte[0], 0, 0);
            DatagramSocket socket;
            try {
                socket = new DatagramSocket();
                int position = 2;
                while (position < 255) {
                    Log.e("Scanner ", "run: udp-" + substring + position);
                    dp.setAddress(InetAddress.getByName(substring + String.valueOf(position)));
                    socket.send(dp);
                    position++;
                    if (position == 125) {
                        //分两段掉包，一次性发的话，达到236左右，会耗时3秒左右再往下发
                        socket.close();
                        socket = new DatagramSocket();
                    }
                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 读取/proc/net/arp并且解析出来ip，mac,flag
     * flag 为0x00说明目前不在局域网内，曾经在过.0x02代表在局域网内
     */
    public static void readArp() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line = "";
            String ip = "";
            //flag 为0x00说明目前不在局域网内，曾经在过.0x02代表在局域网内
            String flag = "";
            String mac = "";
            if (br.readLine() == null) {
                Log.e("scanner", "readArp: null");
            }
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() < 63)
                    continue;
                if (line.toUpperCase(Locale.US).contains("IP"))
                    continue;
                ip = line.substring(0, 17).trim();
                flag = line.substring(29, 32).trim();
                mac = line.substring(41, 63).trim();
                if (mac.contains("00:00:00:00:00:00"))
                    continue;
                Log.e("scanner", "readArp: mac= " + mac + " ; ip= " + ip + " ;flag= " + flag);
            }
            br.close();
        } catch (Exception ignored) {
        }
    }
}

