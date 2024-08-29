package org.mj.leapremote.cs;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;

public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
    private WebSocketClientHandshaker handshaker = null;
    private ChannelPromise handshakeFuture = null;
    public NettyClientWebSocket webSocket;
    public boolean exceptionCaught;

    public WebSocketClientHandler(NettyClientWebSocket webSocket) {
        this.webSocket = webSocket;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        webSocket.clientHandler.setCtx(ctx);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.handshakeFuture = ctx.newPromise();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        //System.out.println("WebSocketClientHandler::channelRead0: ");
        // 握手协议返回，设置结束握手
        if (!this.handshaker.isHandshakeComplete()){
            FullHttpResponse response = (FullHttpResponse)msg;
            this.handshaker.finishHandshake(ctx.channel(), response);
            this.handshakeFuture.setSuccess();
            //System.out.println("WebSocketClientHandler::channelRead0 HandshakeComplete...");
            webSocket.clientHandler.sendBasicData();
            webSocket.clientHandler.startHeartbeat();
            return;
        }

        if (msg instanceof TextWebSocketFrame) {
            //System.out.println(msg);
            TextWebSocketFrame textFrame = (TextWebSocketFrame)msg;
            //System.out.println("WebSocketClientHandler::channelRead0 textFrame: " + textFrame.text());
            webSocket.clientHandler.handleMessage(textFrame.text());
        }

        if(msg instanceof BinaryWebSocketFrame) {
            //System.out.println((((BinaryWebSocketFrame) msg).content()).array().length);
            webSocket.clientHandler.handleMessage(new String(((BinaryWebSocketFrame) msg).content().array()));
        }

        if (msg instanceof CloseWebSocketFrame){
            //System.out.println("WebSocketClientHandler::channelRead0 CloseWebSocketFrame");
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Inactive");
        webSocket.clientHandler.isClosed = true;
        webSocket.clientHandler.stopHeartbeat();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        exceptionCaught = true;
        System.out.println("WebSocketClientHandler::exceptionCaught");
        cause.printStackTrace();
        ctx.channel().close();
    }

    public void setHandshaker(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }

    public ChannelFuture handshakeFuture() {
        return this.handshakeFuture;
    }

    public ChannelPromise getHandshakeFuture() {
        return handshakeFuture;
    }

    public void setHandshakeFuture(ChannelPromise handshakeFuture) {
        this.handshakeFuture = handshakeFuture;
    }

}