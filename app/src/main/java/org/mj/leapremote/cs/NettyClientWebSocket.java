package org.mj.leapremote.cs;

import android.app.Activity;

import org.mj.leapremote.Define;
import org.mj.leapremote.util.Utils;

import java.net.URI;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker13;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.util.concurrent.GenericFutureListener;

public class NettyClientWebSocket extends Thread {

    public Activity activity;
    // 通信管道
    //private MyWebSocketClient client;
    public OnConnectSuccessCallback connectSuccessCallback;
    public ClientHandler clientHandler;

    public void sendMessage(String msg) {
        clientHandler.sendMessage(msg);
    }

    public interface OnConnectSuccessCallback {
        void success();
        void failed(boolean fatal, String err, int times);
    }
    public NettyClientWebSocket(Activity activity, OnConnectSuccessCallback connectSuccessCallback) {
        this.activity = activity;
        this.connectSuccessCallback = connectSuccessCallback;
        this.clientHandler = new ClientHandler(activity);
    }

    @Override
    public void run() {
        System.out.println("WebSocket Client Start");
        doConnect();
    }

    boolean interrupt;

    @Override
    public void interrupt() {
        interrupt = true;
        System.out.println("interrupted");
        super.interrupt();
    }

    public boolean connected;

    public boolean connect(String url){
        EventLoopGroup client = new NioEventLoopGroup();
        try{
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(client);
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            bootstrap.option(ChannelOption.TCP_NODELAY, true);
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Define.connectTimeout);
            bootstrap.channel(NioSocketChannel.class);
            final WebSocketClientHandler[] handler = {new WebSocketClientHandler(NettyClientWebSocket.this)};
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel channel) {
                    ChannelPipeline pipeline = channel.pipeline();
                    pipeline.addLast(new HttpClientCodec(), new HttpObjectAggregator(2155380*10));
                    //pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(2155380*10, 0, 4, 0, 4));
                    pipeline.addLast("handler", handler[0]);
                }
            });
            URI uri = new URI(url);
            ChannelFuture cf = bootstrap.connect(uri.getHost(), uri.getPort()).sync();
            cf.addListener((GenericFutureListener<ChannelFuture>) channelFuture -> {
                String log = String.format("连接websocket服务器: %s isSuccess=%s", url, channelFuture.isSuccess());
                System.out.println(log);
                connectSuccessCallback.success();
                System.out.println(channelFuture.isSuccess());
                if(channelFuture.isSuccess()){
                    //进行握手
                    Channel channel = channelFuture.channel();
                    handler[0] = (WebSocketClientHandler)channel.pipeline().get("handler");
                    WebSocketClientHandshaker handshaker =
                            new MyHandshaker(uri, new DefaultHttpHeaders(), 2155380*10, "192.168.2.16:2086");
                    handler[0].setHandshaker(handshaker);
                    handshaker.handshake(channel);
                    // 阻塞等待是否握手成功?
                    //handler[0].handshakeFuture().sync();
                    handler[0].handshakeFuture();
                }
            });
            cf.channel().closeFuture().sync();
            return !handler[0].exceptionCaught;
        } catch (Exception ex){
            ex.printStackTrace();
        } finally {
            client.shutdownGracefully();
        }
        return false;
    }

    public void doConnect() {
        int i=0;
        boolean reconnect = false;
        while (!connected && !interrupt) {
            long start = System.currentTimeMillis();
            System.out.println(Define.url);
            connected = connect(Define.url+Define.deviceId);
            System.out.println(getName()+connected+interrupt);
            if(!connected) {
                long time = System.currentTimeMillis() - start;
                if(time<Define.connectTimeout) {
                    try {
                        Thread.sleep(Define.connectTimeout-time);
                    } catch (InterruptedException e) {
                        connectSuccessCallback.failed(true, e.toString(), ++i);
                        return;
                    }
                }
                connectSuccessCallback.failed(false, "Failed to connect", ++i);
            } else {
                reconnect = true;
            }
        }
        System.out.println("NEED CONTINUE:"+ interrupt);
        if(!interrupt&&reconnect) {
            connected = false;
            new Thread(this::doConnect).start();
        }
        /*int i=0;
        while (!connected) {
            try {
                client = new MyWebSocketClient(new URI("ws://home.mjczy.life:2086/websocket/"+Define.deviceId), this);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                connectSuccessCallback.failed(true, e.toString(), ++i);
                return;
            }
            long start = System.currentTimeMillis();
            try {
                client.connectBlocking(Define.connectTimeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                connectSuccessCallback.failed(true, e.toString(), ++i);
                return;
            }
            long time = System.currentTimeMillis() - start;
            if(time<Define.connectTimeout) {
                try {
                    Thread.sleep(Define.connectTimeout-time);
                } catch (InterruptedException e) {
                    connectSuccessCallback.failed(true, e.toString(), ++i);
                    return;
                }
            }
            if(!client.isOpen()) {
                System.out.println(client.getURI());
                client.closeConnection(2, "reconnect stop");
                connectSuccessCallback.failed(false, "Failed to connect", ++i);
                continue;
            }
            sendBasicData();
            connected = true;
            connectSuccessCallback.success();
        }*/
    }
}