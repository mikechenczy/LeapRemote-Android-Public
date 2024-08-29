package org.mj.leapremote.cs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Build;
import android.view.Surface;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lxj.xpopup.XPopup;
import com.mask.mediaprojection.service.MediaProjectionService;

import org.mj.leapremote.Define;
import org.mj.leapremote.R;
import org.mj.leapremote.coder.ScreenDecoder;
import org.mj.leapremote.cs.direct.NettyClientDirect;
import org.mj.leapremote.model.Device;
import org.mj.leapremote.service.AutoService;
import org.mj.leapremote.service.HttpService;
import org.mj.leapremote.ui.activities.ControlActivity;
import org.mj.leapremote.ui.activities.MainActivity;
import org.mj.leapremote.util.ClientHelper;
import org.mj.leapremote.util.DevicesUtil;
import org.mj.leapremote.util.ImageUtils;
import org.mj.leapremote.util.KeyUtil;
import org.mj.leapremote.util.image.Depress;
import org.mj.leapremote.util.image.ZipCompress;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class ClientHandler {
    public Activity activity;
    private ChannelHandlerContext ctx;

    public ClientHandler(Activity activity) {
        this.activity = activity;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public void sendMessage(String content) {
        /*if(client!=null && client.isOpen()) {
            new Thread(() -> {
                try {
                    client.send(content);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            return;
        }*/
        if(ctx!=null) {
            new Thread(() -> {
                /*try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/
                //System.out.println("WRITE AND FLUSH");
                ctx.channel().writeAndFlush(new TextWebSocketFrame(content));
                //ctx.channel().writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)));
            }).start();
            return;
        }
        if(JSON.parseObject(content).getString("type").equals("send"))
            ClientHelper.disableSend(activity);
        activity.runOnUiThread(() -> Toast.makeText(activity, R.string.cannot_connect_to_server, Toast.LENGTH_SHORT).show());
    }

    public void sendBasicData() {
        JSONObject object = new JSONObject();
        object.put("type", "basicData");
        object.put("version", Define.version);
        object.put("ip", HttpService.getPublicIp());
        JSONObject deviceInfo = new JSONObject();
        deviceInfo.put("device", Build.DEVICE);
        deviceInfo.put("operateSystem", Device.OS_ANDROID);
        deviceInfo.put("abi", JSONArray.toJSONString(Build.SUPPORTED_ABIS));
        deviceInfo.put("model", Build.MODEL);
        deviceInfo.put("brand", Build.BRAND);
        deviceInfo.put("width", Define.displayMetrics.widthPixels);
        deviceInfo.put("height", Define.displayMetrics.heightPixels);
        deviceInfo.put("dpi", Define.displayMetrics.densityDpi);
        object.put("deviceInfo", deviceInfo);
        sendMessage(object.toJSONString());
        object = new JSONObject();
        object.put("type", "remote");
        object.put("enabled", Define.remotePlainEnabled);
        object.put("directEnabled", Define.remoteDirectEnabled);
        sendMessage(object.toJSONString());
        object = new JSONObject();
        object.put("type", "devices");
        sendMessage(object.toJSONString());
    }

    public void handleMessage(String message) {
        if(message.equals("{\"type\": \"isOnline\"}")) {
            return;
        }
        JSONObject msg = JSON.parseObject(message);
        switch (msg.getString("type")) {
            case "devices":
                Define.plainDevices.clear();
                JSONArray devices = msg.getJSONArray("devices");
                for(int i=0;i<devices.size();i++) {
                    DevicesUtil.insertOrUpdate(devices.getJSONObject(i));
                }
                if(MainActivity.INSTANCE!=null && MainActivity.INSTANCE.mainFragment!=null)
                    MainActivity.INSTANCE.mainFragment.refreshDevices();
                break;
            case "connectIdAndPin":
                Define.connectId = msg.getString("connectId");
                Define.connectPin = msg.getString("connectPin");
                if(MainActivity.INSTANCE!=null && MainActivity.INSTANCE.actsFragment!=null) {
                    MainActivity.INSTANCE.runOnUiThread(() -> MainActivity.INSTANCE.actsFragment.updateConnectIdAndPin());
                }
                break;
            case "control":
                Define.controlled = msg.getBoolean("controlled");
                Define.controlId = msg.getInteger("controlId");
                //System.out.println("123123123123123123123"+msg.getBoolean("controlled"));
                System.out.println("PRE SHOULD OPEN ACTIVITY!!!"+msg);
                if(msg.getBoolean("controlled")) {
                    ClientHelper.enableSend(activity.getApplicationContext());
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("type", "savedGestures");
                    jsonObject.put("savedGestures", Define.savedGestures);
                    JSONObject send = new JSONObject();
                    send.put("type", "send");
                    send.put("controlId", Define.controlId);
                    send.put("controlled", true);
                    send.put("data", jsonObject);
                    ClientHelper.sendMessage(activity.getApplicationContext(), send.toString());
                    /*ScreenDecoder.VIDEO_WIDTH = Define.displayMetrics.widthPixels;
                    ScreenDecoder.VIDEO_HEIGHT = Define.displayMetrics.heightPixels;
                    ScreenDecoder.updateResolution(80);
                    ScreenDecoder.onDecode = new ScreenDecoder.OnDecodeListener() {
                        @Override
                        public void onDecode(Bitmap bitmap) {
                        }
                    };
                    ScreenDecoder.startDecode(null);
                    System.out.println("SHOULD OPEN ACTIVITY!!!");
                    activity.runOnUiThread(() -> {
                        activity.startActivity(new Intent(activity, ControlActivity.class));
                    });*/
                    return;
                }
                new Depress();
                ScreenDecoder.VIDEO_WIDTH = msg.getInteger("width");
                ScreenDecoder.VIDEO_HEIGHT = msg.getInteger("height");
                ScreenDecoder.updateResolution(50);
                ScreenDecoder.onDecode = new ScreenDecoder.OnDecodeListener() {
                    @Override
                    public void onDecode(Bitmap bitmap) {
                        if(ControlActivity.INSTANCE!=null) {
                            ControlActivity.INSTANCE.runOnUiThread(() -> {
                        /*if(msg.containsKey("width")) {
                            Depress.instance.changeWidthAndHeight(msg.getInteger("width"), msg.getInteger("height"));
                        }
                        Bitmap bitmap = Depress.instance.depress(msg.getBoolean("compressed"), msg.getBytes("image"));*/
                                ControlActivity.INSTANCE.updateImage(bitmap);
                                //ControlActivity.INSTANCE.updateRotate(msg.getBooleanValue("rotate"));
                            });
                        }
                    }
                };
                ScreenDecoder.startDecode(null);
                activity.runOnUiThread(() -> {
                    activity.startActivity(new Intent(activity, ControlActivity.class));
                });
                break;
            case "image":
                if(MainActivity.INSTANCE!=null) {
                    if(ControlActivity.INSTANCE==null)
                        return;
                    ControlActivity.INSTANCE.runOnUiThread(() -> {
                        Bitmap bitmap = ImageUtils.byteArrayToBitmap(ZipCompress.decompress(msg.getBytes("image")));
                        /*if(msg.containsKey("width")) {
                            Depress.instance.changeWidthAndHeight(msg.getInteger("width"), msg.getInteger("height"));
                        }
                        Bitmap bitmap = Depress.instance.depress(msg.getBoolean("compressed"), msg.getBytes("image"));*/
                        ControlActivity.INSTANCE.updateImage(bitmap);
                        ControlActivity.INSTANCE.updateRotate(msg.getBooleanValue("rotate"));
                    });
                }
                break;
            case "savedGestures":
                Define.controlSavedGestures = msg.getString("savedGestures");
                if(ControlActivity.INSTANCE!=null) {
                    ControlActivity.INSTANCE.setGestures(Define.controlSavedGestures);
                }
                break;
            case "record":
                if(msg.containsKey("first")) {
                    System.out.println("FIRST");
                    ControlActivity.INSTANCE.sendCommandHelper.doSendFirstReceived();
                    synchronized (ScreenDecoder.isDecoding) {
                        if (ControlActivity.INSTANCE != null) {
                            ScreenDecoder.ROTATION = ControlActivity.INSTANCE.rotateState * 90;
                        } else {
                            ScreenDecoder.ROTATION = 0;
                        }
                        ScreenDecoder.index = 0;
                        ScreenDecoder.laidData.clear();
                        ScreenDecoder.dataQueue.clear();
                        ScreenDecoder.portrait = msg.getBoolean("portrait");
                        ScreenDecoder.hevc = msg.getBoolean("hevc");
                        ScreenDecoder.VIDEO_WIDTH = msg.getInteger("width");
                        ScreenDecoder.VIDEO_HEIGHT = msg.getInteger("height");
                        ScreenDecoder.updateResolution(ScreenDecoder.resolution);
                        if (ScreenDecoder.isDecoding) {
                            if (ControlActivity.INSTANCE != null && ControlActivity.INSTANCE.record != null && ControlActivity.INSTANCE.record.getSurfaceTexture() != null) {
                                ScreenDecoder.stopDecode();
                                ScreenDecoder.startDecode(new Surface(ControlActivity.INSTANCE.record.getSurfaceTexture()));
                            }
                        }
                    }
                }
                ScreenDecoder.decodeData(msg.getBytes("record"), msg.getInteger("index"));
                /*if(ControlActivity.INSTANCE!=null) {
                    //if(ControlActivity.INSTANCE==null)
                        //return;
                    //ControlActivity.INSTANCE.runOnUiThread(() -> {
                    new Thread(() -> {
                        ControlActivity.INSTANCE.runOnUiThread(() -> {

                        });
                    }).start();
                        //Bitmap bitmap = ImageUtils.byteArrayToBitmap(ZipCompress.decompress(msg.getBytes("image")));
                        /*if(msg.containsKey("width")) {
                            Depress.instance.changeWidthAndHeight(msg.getInteger("width"), msg.getInteger("height"));
                        }
                        Bitmap bitmap = Depress.instance.depress(msg.getBoolean("compressed"), msg.getBytes("image"));*/
                        //ControlActivity.INSTANCE.updateImage(bitmap);
                       // ControlActivity.INSTANCE.updateRotate(msg.getBooleanValue("rotate"));
                    //});
                //}
                break;
            case "firstReceived":
                MainActivity.INSTANCE.runOnUiThread(() -> {
                    ClientHelper.firstReceived(MainActivity.INSTANCE);
                });
                break;
            case "sizeChange":
                if(ControlActivity.INSTANCE!=null && ControlActivity.INSTANCE.record!=null && ControlActivity.INSTANCE.record.getSurfaceTexture()!=null) {
                    ScreenDecoder.stopDecode();
                    ScreenDecoder.portrait = msg.getBoolean("portrait");
                    ScreenDecoder.updateResolution(ScreenDecoder.resolution);
                    ControlActivity.INSTANCE.sendCommandHelper.doSendRestartMedia();
                    ScreenDecoder.startDecode(new Surface(ControlActivity.INSTANCE.record.getSurfaceTexture()));
                }
                break;
                //Below is for controlled
            case "restartMedia":
                System.out.println("RESTARTMEDIA");
                MainActivity.INSTANCE.runOnUiThread(() -> {
                    ClientHelper.restartMedia(MainActivity.INSTANCE);
                });
                break;
            case "resolution":
                MainActivity.INSTANCE.runOnUiThread(() -> {
                    ClientHelper.resolution(MainActivity.INSTANCE, msg.getInteger("resolution"));
                });
                break;
            case "action":
                AutoService.mService.performGesture(msg.getJSONArray("points"));
                break;
            case "gestures":
                AutoService.mService.performMultipleGestures(msg.getJSONArray("gestures"));
                break;
            case "button":
                AutoService.mService.performGlobalAction(msg.getInteger("button"));
                break;
            case "key":
                switch (msg.getString("key")) {
                    case "unlock":
                        MainActivity.INSTANCE.runOnUiThread(() -> KeyUtil.unlock(MainActivity.INSTANCE));
                        break;
                    case "lock":
                        MainActivity.INSTANCE.runOnUiThread(() -> KeyUtil.lock(MainActivity.INSTANCE));
                        break;
                    case "lockOrUnlock":
                        MainActivity.INSTANCE.runOnUiThread(() -> KeyUtil.lockOrUnlock(MainActivity.INSTANCE));
                        break;
                    case "volumeUp":
                        MainActivity.INSTANCE.runOnUiThread(() -> {
                            AudioManager audioManager = (AudioManager) MainActivity.INSTANCE.getSystemService(Context.AUDIO_SERVICE);
                            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
                        });
                        break;
                    case "volumeDown":
                        MainActivity.INSTANCE.runOnUiThread(() -> {
                            AudioManager audioManager = (AudioManager) MainActivity.INSTANCE.getSystemService(Context.AUDIO_SERVICE);
                            audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
                        });
                        break;
                    case "volumeMute":
                        MainActivity.INSTANCE.runOnUiThread(() -> {
                            AudioManager audioManager = (AudioManager) MainActivity.INSTANCE.getSystemService(Context.AUDIO_SERVICE);
                            audioManager.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_PLAY_SOUND);
                        });
                        break;
                }
                break;
            case "quality":
                Define.scale = (float)msg.getInteger("scale")/10f;
                Define.quality = msg.getInteger("quality");
                break;
            case "controlFailed":
                System.out.println("failed");
                activity.runOnUiThread(() -> Toast.makeText(activity, msg.getString("reason"), Toast.LENGTH_SHORT).show());
                break;
            case "controlRemoved":
                System.out.println("RECEIVED SUCCESS");
                Define.controlled = false;
                Define.controlId = 0;
                if(msg.getBoolean("controlled")) {
                    ClientHelper.disableSend(activity.getApplicationContext());
                    return;
                }
                if(ControlActivity.INSTANCE!=null) {
                    ControlActivity.INSTANCE.runOnUiThread(() -> ControlActivity.INSTANCE.finish());
                }
                break;
            case "controlForward":
                if(MainActivity.INSTANCE!=null) {
                    if (!Define.ipv6Support) {
                        JSONObject request = new JSONObject();
                        request.put("type", "connect");
                        request.put("connectId", Define.temporaryId);
                        request.put("connectPin", Define.temporaryPin);
                        request.put("forward", true);
                        ClientHelper.sendMessage(MainActivity.INSTANCE.getApplicationContext(), request.toJSONString());
                        return;
                    }
                    new XPopup.Builder(MainActivity.INSTANCE).asConfirm("提示", msg.getString("reason"), () -> {
                        JSONArray jsonArray = msg.getJSONArray("availableHosts");
                        List<String> data = new ArrayList<>();
                        for(int i=0;i<jsonArray.size();i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            String string = "";
                            if(jsonObject.getString("niName").contains("wlan")) {
                                string+="Wifi:";
                            } else {
                                string+="移动数据:";
                            }
                            string += jsonObject.getString("ip");
                            data.add(string);
                        }
                        new XPopup.Builder(MainActivity.INSTANCE).asCenterList("选择ip进行连接", data.toArray(new String[]{}), (position, text) -> {
                            if(NettyClientDirect.INSTANCE !=null) {
                                NettyClientDirect.INSTANCE.interrupt();
                            }
                            new NettyClientDirect(MainActivity.INSTANCE, jsonArray.getJSONObject(position).getString("ip"), Define.defaultPort
                                    , new NettyClientDirect.OnConnectSuccessCallback() {
                                @Override
                                public void success() {
                                    Define.direct = true;
                                    MainActivity.INSTANCE.startActivity(new Intent(MainActivity.INSTANCE, ControlActivity.class));
                                }
                                @Override
                                public void failed(String err) {
                                    System.err.println(err);
                                    MainActivity.INSTANCE.runOnUiThread(() -> Toast.makeText(MainActivity.INSTANCE, R.string.failed_to_connect, Toast.LENGTH_SHORT).show());
                                }
                            }).start();
                        }).show();
                    }, () -> {
                        JSONObject request = new JSONObject();
                        request.put("type", "connect");
                        request.put("connectId", Define.temporaryId);
                        request.put("connectPin", Define.temporaryPin);
                        request.put("forward", true);
                        ClientHelper.sendMessage(MainActivity.INSTANCE.getApplicationContext(), request.toJSONString());
                    }).show();
                }
                break;
            case "speedLimit":
                Define.speedLimited = msg.getBoolean("speedLimited");
                Define.maxSpeed = msg.getFloatValue("maxSpeed");
                System.out.println("SpeedLimit: "+Define.speedLimited+", MaxSpeed: "+Define.maxSpeed);
                break;
        }
    }

    public Timer heartbeatTimer;
    public boolean isClosed;

    public void startHeartbeat() {
        // 启动心跳定时器，每隔30秒发送一条心跳消息
        this.heartbeatTimer = new Timer();
        this.heartbeatTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!isClosed) {
                    // 发送心跳消息
                    if(ctx!=null)
                        sendMessage("heartbeat");
                }
            }
        }, 30000, 30000);
    }

    public void stopHeartbeat() {
        // 停止心跳定时器
        if (this.heartbeatTimer != null) {
            this.heartbeatTimer.cancel();
            this.heartbeatTimer = null;
        }
    }
}
