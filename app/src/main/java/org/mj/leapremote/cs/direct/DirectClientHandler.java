package org.mj.leapremote.cs.direct;

import android.graphics.Bitmap;
import android.view.Surface;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.mj.leapremote.Define;
import org.mj.leapremote.R;
import org.mj.leapremote.coder.ScreenDecoder;
import org.mj.leapremote.ui.activities.ControlActivity;
import org.mj.leapremote.ui.activities.MainActivity;
import org.mj.leapremote.util.ImageUtils;

import org.mj.leapremote.util.image.ZipCompress;
import org.msgpack.type.RawValue;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 处理客户端的channel
 *
 * @author lucher
 */
public class DirectClientHandler extends SimpleChannelInboundHandler<Object> {
    public NettyClientDirect nettyClientDirect;
    public DirectClientHandler(NettyClientDirect nettyClientDirect) {
        this.nettyClientDirect = nettyClientDirect;
    }

    @Override
    public void channelRead0(ChannelHandlerContext arg0, Object m) {
        /*if(!(m instanceof FullHttpResponse)) {
            return;
        }
        byte[] bytes = new byte[((FullHttpResponse)m).content().readableBytes()];
        ((FullHttpResponse)m).content().readBytes(bytes);
        m = new String(bytes, StandardCharsets.UTF_8);*/
        m = ((RawValue)m).getString();
        if(m.equals("{\"type\": \"isOnline\"}")) {
            return;
        }
        JSONObject msg = JSON.parseObject((String)m);
        switch (msg.getString("type")) {
            case "image":
                if(MainActivity.INSTANCE!=null) {
                    if(ControlActivity.INSTANCE==null)
                        return;
                    ControlActivity.INSTANCE.runOnUiThread(() -> {
                        Bitmap bitmap = ImageUtils.byteArrayToBitmap(ZipCompress.decompress(msg.getBytes("image")));
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
                System.out.println("RECEIVED");
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
                if(MainActivity.INSTANCE!=null) {
                    MainActivity.INSTANCE.runOnUiThread(() -> {
                        ScreenDecoder.decodeData(msg.getBytes("record"), msg.getInteger("index"));
                    });
                }
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
            case "sizeChange":
                System.out.println("SIZECHANGED");
                if(ControlActivity.INSTANCE!=null && ControlActivity.INSTANCE.record!=null && ControlActivity.INSTANCE.record.getSurfaceTexture()!=null) {
                    ScreenDecoder.stopDecode();
                    ScreenDecoder.portrait = msg.getBoolean("portrait");
                    ScreenDecoder.updateResolution(ScreenDecoder.resolution);
                    ControlActivity.INSTANCE.sendCommandHelper.doSendRestartMedia();
                    ScreenDecoder.startDecode(new Surface(ControlActivity.INSTANCE.record.getSurfaceTexture()));
                }
                break;
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
        ctx.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        nettyClientDirect.interrupt();
        if(ControlActivity.INSTANCE!=null && !ControlActivity.INSTANCE.destroying) {
            ControlActivity.INSTANCE.runOnUiThread(() -> Toast.makeText(ControlActivity.INSTANCE, R.string.disconnected, Toast.LENGTH_SHORT).show());
            ControlActivity.INSTANCE.finish();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
    }
}