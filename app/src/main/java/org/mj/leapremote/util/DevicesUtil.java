package org.mj.leapremote.util;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.mj.leapremote.Define;
import org.mj.leapremote.model.Device;

import java.util.ArrayList;
import java.util.List;

public class DevicesUtil {
    public static void updateDevices(Context context) {
        if(context==null)
            return;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "devices");
        ClientHelper.sendMessage(context, jsonObject.toJSONString());
    }

    public static void updateDevicesWithLocal(Context context) {
        if(context==null)
            return;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "devices");
        jsonObject.put("devices", getPlainDevicesAsJSONArray());
        ClientHelper.sendMessage(context, jsonObject.toJSONString());
    }

    public static List<String> getDeviceIdsFromDevices() {
        return getDeviceIdsFromDevices(Define.plainDevices);
    }

    public static List<String> getDeviceIdsFromDevices(List<Device> devices) {
        List<String> result = new ArrayList<>();
        if(devices==null)
            return result;
        for(Device device : devices) {
            if(device.getDeviceId()!=null)
                result.add(device.getDeviceId());
        }
        return result;
    }

    public static Device getDeviceFromDevices(String deviceId) {
        return getDeviceFromDevices(deviceId, Define.plainDevices);
    }


    public static Device getDeviceFromDevices(String deviceId, List<Device> devices) {
        if(devices==null)
            return null;
        for(Device device : devices) {
            if(device.getDeviceId()!=null && device.getDeviceId().equals(deviceId))
                return device;
        }
        return null;
    }

    public static int getIndexFromDevices(String deviceId) {
        return getIndexFromDevices(deviceId, Define.plainDevices);
    }


    public static int getIndexFromDevices(String deviceId, List<Device> devices) {
        if(devices==null)
            return -1;
        for(int i=0;i<devices.size();i++) {
            if(devices.get(i).getDeviceId()!=null && devices.get(i).getDeviceId().equals(deviceId))
                return i;
        }
        return -1;
    }

    public static JSONArray getPlainDevicesAsJSONArray() {
        JSONArray result = new JSONArray();
        for(Device device : Define.plainDevices) {
            if(device.getMode()==Device.MODE_PLAIN) {
                JSONObject object = new JSONObject();
                object.put("deviceId", device.getDeviceId());
                object.put("connectId", device.getConnectId());
                object.put("connectPin", device.getConnectPin());
                result.add(object);
            }
        }
        return result;
    }

    /*public static synchronized void update(Device device) {
        if(device==null || device.getDeviceId()==null)
            return;
        int index = getIndexFromDevices(device.getDeviceId());
        if(index==-1)
            return;
        Define.manualDevices.remove(index);
        Define.manualDevices.add(device);
    }*/

    public static synchronized void insertOrUpdate(Device device) {
        if(device==null || device.getDeviceId()==null)
            return;
        int index = getIndexFromDevices(device.getDeviceId());
        if(index==-1) {
            Define.plainDevices.add(device);
            return;
        }
        Define.plainDevices.remove(index);
        Define.plainDevices.add(device);
    }

    /*public static synchronized void update(JSONObject json) {
        if(json==null)
            return;
        String deviceId = json.getString("deviceId");
        if(Utils.stringIsEmpty(deviceId))
            return;
        Device device = getDeviceFromDevices(deviceId);
        if(device==null)
            return;
        device.setName(json.getString("name"));
        device.setStatus(json.getInteger("status"));
        update(device);
    }*/

    public static synchronized void insertOrUpdate(JSONObject json) {
        if(json==null)
            return;
        String deviceId = json.getString("deviceId");
        if(Utils.stringIsEmpty(deviceId))
            return;
        Device device = getDeviceFromDevices(deviceId);
        if(device==null) {
            device = new Device();
            device.setDeviceId(deviceId);
        }
        device.setConnectId(json.getString("connectId"));
        device.setConnectPin(json.getString("connectPin"));
        device.setName(json.getString("name"));
        device.setStatus(json.getInteger("status"));
        insertOrUpdate(device);
    }

    public static String getDirectDevicesJSONString() {
        JSONArray jsonArray = new JSONArray();
        for(Device device : Define.directDevices) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", device.getName());
            jsonObject.put("host", device.getIp());
            jsonObject.put("port", device.getPort());
            jsonArray.add(jsonObject);
        }
        return jsonArray.toJSONString();
    }

    public static void setDirectDevicesJSONString(String s) {
        if(Utils.stringIsEmpty(s))
            return;
        try {
            JSONArray jsonArray = JSON.parseArray(s);
            for(int i=0;i<jsonArray.size();i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Device device = new Device();
                device.setMode(Device.MODE_DIRECT);
                device.setIp(jsonObject.getString("host"));
                device.setPort(jsonObject.getInteger("port"));
                device.setName(jsonObject.getString("name"));
                device.setStatus(Device.STATUS_NOT_SUPPORTED);
                Define.directDevices.add(device);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
