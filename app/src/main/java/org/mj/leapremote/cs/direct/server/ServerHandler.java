package org.mj.leapremote.cs.direct.server;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.mj.leapremote.Define;
import org.mj.leapremote.service.AutoService;
import org.mj.leapremote.service.ServerService;
import org.mj.leapremote.ui.activities.MainActivity;
import org.mj.leapremote.util.ClientHelper;
import org.mj.leapremote.util.KeyUtil;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;

import org.msgpack.type.RawValue;

import java.io.UnsupportedEncodingException;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class ServerHandler extends SimpleChannelInboundHandler<Object> {

    public static void send(ChannelHandlerContext ctx, String content) {
        ctx.writeAndFlush(content);
    }

    private static FullHttpResponse httpResponseHandle(String responseMessage) {
        FullHttpResponse response = null;
        try {
            response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(responseMessage.getBytes("UTF-8")));
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return response;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Channel Active "+ctx);
        /*new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ctx.writeAndFlush("{\"type\": \"isOnline\"}");
            }
        }).start();*/
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        //byte[] bytes = new byte[msg.content().readableBytes()];
        //msg.content().readBytes(bytes);
        //do something msg
        System.out.println(((RawValue)msg).getString());
        //JSONObject jsonObject = JSON.parseObject(new String(bytes, StandardCharsets.UTF_8));
        JSONObject jsonObject = JSON.parseObject(((RawValue)msg).getString());
        //ctx.writeAndFlush(jsonObject.toString());
        switch (jsonObject.getString("type")) {
            case "deviceId":
                if(Handlers.handlers.size()>0) {
                    ctx.close();
                    return;
                }
                Handlers.handlers.add(ctx);
                if(Handlers.handlers.size()==1) {
                    KeyUtil.unlock(MainActivity.INSTANCE);
                    startRecord();
                    JSONObject savedGestures = new JSONObject();
                    savedGestures.put("type", "savedGestures");
                    savedGestures.put("savedGestures", Define.savedGestures);
                    send(ctx, savedGestures.toString());
                }
                break;
            case "firstReceived":
                MainActivity.INSTANCE.runOnUiThread(() -> {
                    Intent intent = new Intent(MainActivity.INSTANCE, ServerService.class);
                    intent.putExtra("method", "firstReceived");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        MainActivity.INSTANCE.startForegroundService(intent);
                    } else {
                        MainActivity.INSTANCE.startService(intent);
                    }
                });
                break;
            case "restartMedia":
                System.out.println("RESTARTMEDIA");
                MainActivity.INSTANCE.runOnUiThread(() -> {
                    Intent intent = new Intent(MainActivity.INSTANCE, ServerService.class);
                    intent.putExtra("method", "restartMedia");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        MainActivity.INSTANCE.startForegroundService(intent);
                    } else {
                        MainActivity.INSTANCE.startService(intent);
                    }
                });
                break;
            case "resolution":
                MainActivity.INSTANCE.runOnUiThread(() -> {
                    Intent intent = new Intent(MainActivity.INSTANCE, ServerService.class);
                    intent.putExtra("method", "resolution");
                    intent.putExtra("resolution", jsonObject.getInteger("resolution"));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        MainActivity.INSTANCE.startForegroundService(intent);
                    } else {
                        MainActivity.INSTANCE.startService(intent);
                    }
                });
                break;
            case "action":
                MainActivity.INSTANCE.runOnUiThread(() -> AutoService.mService.performGesture(jsonObject.getJSONArray("points")));
                break;
            case "gestures":
                MainActivity.INSTANCE.runOnUiThread(() -> AutoService.mService.performMultipleGestures(jsonObject.getJSONArray("gestures")));
                break;
            case "button":
                MainActivity.INSTANCE.runOnUiThread(() -> AutoService.mService.performGlobalAction(jsonObject.getInteger("button")));
                break;
            case "key":
                switch (jsonObject.getString("key")) {
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
                Define.scale = (float)jsonObject.getInteger("scale")/10f;
                Define.quality = jsonObject.getInteger("quality");
                break;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        //System.out.println(ctx);
        //System.out.println(Handler.removeHandler(ctx));
        ctx.close();
        Handlers.handlers.remove(ctx);
        if(Handlers.handlers.size()==0) {
            stopRecord();
        }
    }
    private void startRecord() {
        MainActivity.INSTANCE.runOnUiThread(() -> {
            Intent intent = new Intent(MainActivity.INSTANCE, ServerService.class);
            intent.putExtra("method", "startRecord");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                MainActivity.INSTANCE.startForegroundService(intent);
            } else {
                MainActivity.INSTANCE.startService(intent);
            }
        });
    }

    private void stopRecord() {
        MainActivity.INSTANCE.runOnUiThread(() -> {
            Intent intent = new Intent(MainActivity.INSTANCE, ServerService.class);
            intent.putExtra("method", "stopRecord");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                MainActivity.INSTANCE.startForegroundService(intent);
            } else {
                MainActivity.INSTANCE.startService(intent);
            }
        });
    }
}