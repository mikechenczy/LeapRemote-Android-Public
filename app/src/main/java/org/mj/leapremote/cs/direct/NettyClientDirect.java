package org.mj.leapremote.cs.direct;

import android.app.Activity;

import org.mj.leapremote.Define;
import org.mj.leapremote.coder.MessagePackDecoder;
import org.mj.leapremote.coder.MessagePackEncoder;

import org.json.JSONObject;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

public class NettyClientDirect extends Thread {
    public static NettyClientDirect INSTANCE;

    public Activity activity;
    private final String host;
    private final int port;
    private final OnConnectSuccessCallback connectSuccessCallback;

    private Channel channel;
    private Bootstrap bootstrap;


    public interface OnConnectSuccessCallback {
        void success();
        void failed(String err);
    }


    public NettyClientDirect(Activity activity, String host, int port, OnConnectSuccessCallback connectSuccessCallback) {
        this.activity = activity;
        this.host = host;
        this.port = port;
        this.connectSuccessCallback = connectSuccessCallback;
        INSTANCE = this;
    }

    @Override
    public void run() {
        init();
        doConnect();
    }

    /**
     * 发送消息
     *
     * @param event
     */
    public void sendMessage(String event) {
        if (channel != null && channel.isActive()) {//已建立连接状态
            channel.writeAndFlush(event);
            /*try {
                URI uri = new URI("/");
                DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
                        uri.toASCIIString(), Unpooled.wrappedBuffer(event.getBytes(StandardCharsets.UTF_8)));
                request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());
                channel.writeAndFlush(request);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }*/
        } else {
            //EventBus.getDefault().post("还未建立连接，不能发送消息");
        }
    }

    public void init() {
        EventLoopGroup group = new NioEventLoopGroup();
        //NIO客户端启动辅助类，降低客户端开发复杂度
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                // 当执行channelfactory的newChannel方法时,会创建NioSocketChannel实例
                .channel(NioSocketChannel.class)
                // ChannelOption.TCP_NODELAY参数对应于套接字选项中的TCP_NODELAY,该参数的使用与Nagle算法有关
                // Nagle算法是将小的数据包组装为更大的帧然后进行发送，而不是输入一次发送一次,因此在数据包不足的时候会等待其他数据的到了，组装成大的数据包进行发送，虽然该方式有效提高网络的有效
                // 负载，但是却造成了延时，而该参数的作用就是禁止使用Nagle算法，使用于小数据即时传输，于TCP_NODELAY相对应的是TCP_CORK，该选项是需要等到发送的数据量最大的时候，一次性发送
                // 数据，适用于文件传输。
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
                // handler()和childHandler()的主要区别是，handler()是发生在初始化的时候，childHandler()是发生在客户端连接之后
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        // Tcp粘包处理，添加一个LengthFieldBasedFrameDecoder解码器，它会在解码时按照消息头的长度来进行解码。
                        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(2155380*10, 0, 4, 0, 4));
                        // MessagePack解码器，消息进来后先由frameDecoder处理，再给msgPackDecoder处理
                        pipeline.addLast("msgPackDecoder", new MessagePackDecoder());
                        // Tcp粘包处理，添加一个
                        // LengthFieldPrepender编码器，它会在ByteBuf之前增加4个字节的字段，用于记录消息长度。
                        pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
                        // MessagePack编码器，消息发出之前先由frameEncoder处理，再给msgPackEncoder处理
                        pipeline.addLast("msgPackEncoder", new MessagePackEncoder());
                        // 消息处理handler
                        pipeline.addLast("handler", new DirectClientHandler(NettyClientDirect.this));
                        // 客户端接收到的是httpResponse响应，所以要使用HttpResponseDecoder进行解码
                        //ch.pipeline().addLast(new HttpResponseDecoder());
                        // 客户端发送的是httprequest，所以要使用HttpRequestEncoder进行编码
                        //ch.pipeline().addLast(new HttpRequestEncoder());
                        //ch.pipeline().addLast("httpAggregator", new HttpObjectAggregator(2155380*10));
                        //ch.pipeline().addLast(direct?new DirectClientHandler(NettyClient.this):new ClientHandler(NettyClient.this));
                    }
                });
    }


    public boolean connected;

    public void doConnect() {
        try {
            channel = bootstrap.connect(host, port).sync().channel();
            JSONObject object = new JSONObject();
            object.put("type", "deviceId");
            object.put("deviceId", Define.deviceId);
            sendMessage(object.toString());
            connected = true;
            connectSuccessCallback.success();
        } catch (Exception ce) {
            ce.printStackTrace();
            channel = null;
            connectSuccessCallback.failed(ce.toString());
            interrupt();
        }
    }

    @Override
    public void interrupt() {
        if(channel!=null) {
            channel.close();
        }
        connected = false;
        INSTANCE = null;
        super.interrupt();
    }
}