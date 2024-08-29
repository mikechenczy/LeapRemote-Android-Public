package org.mj.leapremote.cs;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketScheme;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.util.CharsetUtil;
import io.netty.util.NetUtil;
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class MyHandshaker extends WebSocketClientHandshaker {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MyHandshaker.class);
    public static final String MAGIC_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private String expectedChallengeResponseString;
    private final boolean allowExtensions;
    private final boolean performMasking;
    private final boolean allowMaskMismatch;
    private final String host;

    public MyHandshaker(URI webSocketURL, HttpHeaders customHeaders, int maxFramePayloadLength, String host) {
        this(webSocketURL, WebSocketVersion.V13, null, true, customHeaders, maxFramePayloadLength, host);
    }

    public MyHandshaker(URI webSocketURL, WebSocketVersion version, String subprotocol, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength, String host) {
        this(webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength, true, false, host);
    }

    public MyHandshaker(URI webSocketURL, WebSocketVersion version, String subprotocol, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength, boolean performMasking, boolean allowMaskMismatch, String host) {
        super(webSocketURL, version, subprotocol, customHeaders, maxFramePayloadLength);
        this.allowExtensions = allowExtensions;
        this.performMasking = performMasking;
        this.allowMaskMismatch = allowMaskMismatch;
        this.host = host;
    }

    static String rawPath(URI wsURL) {
        String path = wsURL.getRawPath();
        String query = wsURL.getRawQuery();
        if (query != null && !query.isEmpty()) {
            path = path + '?' + query;
        }

        return path != null && !path.isEmpty() ? path : "/";
    }

    static int websocketPort(URI wsURL) {
        int wsPort = wsURL.getPort();
        if (wsPort == -1) {
            return WebSocketScheme.WSS.name().contentEquals(wsURL.getScheme()) ? WebSocketScheme.WSS.port() : WebSocketScheme.WS.port();
        } else {
            return wsPort;
        }
    }

    static CharSequence websocketHostValue(URI wsURL) {
        int port = wsURL.getPort();
        if (port == -1) {
            return wsURL.getHost();
        } else {
            String host = wsURL.getHost();
            if (port == HttpScheme.HTTP.port()) {
                return !HttpScheme.HTTP.name().contentEquals(wsURL.getScheme()) && !WebSocketScheme.WS.name().contentEquals(wsURL.getScheme()) ? NetUtil.toSocketAddressString(host, port) : host;
            } else if (port != HttpScheme.HTTPS.port()) {
                return NetUtil.toSocketAddressString(host, port);
            } else {
                return !HttpScheme.HTTPS.name().contentEquals(wsURL.getScheme()) && !WebSocketScheme.WSS.name().contentEquals(wsURL.getScheme()) ? NetUtil.toSocketAddressString(host, port) : host;
            }
        }
    }

    static CharSequence websocketOriginValue(String host, int wsPort) {
        String originValue = (wsPort == HttpScheme.HTTPS.port() ? HttpScheme.HTTPS.name() : HttpScheme.HTTP.name()) + "://" + host;
        return wsPort != HttpScheme.HTTP.port() && wsPort != HttpScheme.HTTPS.port() ? NetUtil.toSocketAddressString(originValue, wsPort) : originValue;
    }

    protected FullHttpRequest newHandshakeRequest() {
        URI wsURL = this.uri();
        String path = rawPath(wsURL);
        byte[] nonce = WebSocketUtil.randomBytes(16);
        String key = WebSocketUtil.base64(nonce);
        String acceptSeed = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        byte[] sha1 = WebSocketUtil.sha1(acceptSeed.getBytes(CharsetUtil.US_ASCII));
        this.expectedChallengeResponseString = WebSocketUtil.base64(sha1);
        if (logger.isDebugEnabled()) {
            logger.debug("WebSocket version 13 client handshake key: {}, expected response: {}", key, this.expectedChallengeResponseString);
        }

        int wsPort = websocketPort(wsURL);
        String host = this.host==null?wsURL.getHost():this.host;
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path);
        HttpHeaders headers = request.headers();
        headers.add(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET).add(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE).add(HttpHeaderNames.SEC_WEBSOCKET_KEY, key).add(HttpHeaderNames.HOST, host).add(HttpHeaderNames.SEC_WEBSOCKET_ORIGIN, websocketOriginValue(host, wsPort));
        String expectedSubprotocol = this.expectedSubprotocol();
        if (expectedSubprotocol != null && !expectedSubprotocol.isEmpty()) {
            headers.add(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL, expectedSubprotocol);
        }

        headers.add(HttpHeaderNames.SEC_WEBSOCKET_VERSION, "13");
        if (this.customHeaders != null) {
            headers.add(this.customHeaders);
        }

        return request;
    }

    protected void verify(FullHttpResponse response) {
        HttpResponseStatus status = HttpResponseStatus.SWITCHING_PROTOCOLS;
        HttpHeaders headers = response.headers();
        if (!response.status().equals(status)) {
            throw new WebSocketHandshakeException("Invalid handshake response getStatus: " + response.status());
        } else {
            CharSequence upgrade = headers.get(HttpHeaderNames.UPGRADE);
            if (!HttpHeaderValues.WEBSOCKET.contentEqualsIgnoreCase(upgrade)) {
                throw new WebSocketHandshakeException("Invalid handshake response upgrade: " + upgrade);
            } else if (!headers.containsValue(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE, true)) {
                throw new WebSocketHandshakeException("Invalid handshake response connection: " + headers.get(HttpHeaderNames.CONNECTION));
            } else {
                CharSequence accept = headers.get(HttpHeaderNames.SEC_WEBSOCKET_ACCEPT);
                if (accept == null || !accept.equals(this.expectedChallengeResponseString)) {
                    throw new WebSocketHandshakeException(String.format("Invalid challenge. Actual: %s. Expected: %s", accept, this.expectedChallengeResponseString));
                }
            }
        }
    }

    protected WebSocketFrameDecoder newWebsocketDecoder() {
        return new WebSocket13FrameDecoder(false, this.allowExtensions, this.maxFramePayloadLength(), this.allowMaskMismatch);
    }

    protected WebSocketFrameEncoder newWebSocketEncoder() {
        return new WebSocket13FrameEncoder(this.performMasking);
    }
}

final class WebSocketUtil {
    private static final FastThreadLocal<MessageDigest> MD5 = new FastThreadLocal<MessageDigest>() {
        protected MessageDigest initialValue() throws Exception {
            try {
                return MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException var2) {
                throw new InternalError("MD5 not supported on this platform - Outdated?");
            }
        }
    };
    private static final FastThreadLocal<MessageDigest> SHA1 = new FastThreadLocal<MessageDigest>() {
        protected MessageDigest initialValue() throws Exception {
            try {
                return MessageDigest.getInstance("SHA1");
            } catch (NoSuchAlgorithmException var2) {
                throw new InternalError("SHA-1 not supported on this platform - Outdated?");
            }
        }
    };

    static byte[] md5(byte[] data) {
        return digest(MD5, data);
    }

    static byte[] sha1(byte[] data) {
        return digest(SHA1, data);
    }

    private static byte[] digest(FastThreadLocal<MessageDigest> digestFastThreadLocal, byte[] data) {
        MessageDigest digest = (MessageDigest)digestFastThreadLocal.get();
        digest.reset();
        return digest.digest(data);
    }

    static String base64(byte[] data) {
        ByteBuf encodedData = Unpooled.wrappedBuffer(data);
        ByteBuf encoded = Base64.encode(encodedData);
        String encodedString = encoded.toString(CharsetUtil.UTF_8);
        encoded.release();
        return encodedString;
    }

    static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];

        for(int index = 0; index < size; ++index) {
            bytes[index] = (byte)randomNumber(0, 255);
        }

        return bytes;
    }

    static int randomNumber(int minimum, int maximum) {
        return (int)(Math.random() * (double)maximum + (double)minimum);
    }

    private WebSocketUtil() {
    }
}
