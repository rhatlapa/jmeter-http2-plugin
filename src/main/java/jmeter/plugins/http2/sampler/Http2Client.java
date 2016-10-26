package jmeter.plugins.http2.sampler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.ssl.SslContext;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

public class Http2Client implements AutoCloseable {

    private static final Logger log = LoggingManager.getLoggerForClass();

    private final SslContext sslCtx;
    private EventLoopGroup workerGroup;
    private final Bootstrap bootstrap = new Bootstrap();
    private final String host;
    private final int port;
    private final Http2ClientInitializer http2ClientInitializer;
    private HttpResponseHandler responseHandler;
    private Channel channel = null;

    public Http2Client(SslContext sslCtx, String host, int port) {
        log.debug("Created new HTTP2 client");
        this.sslCtx = sslCtx;
        this.host = host;
        this.port = port;
        this.workerGroup = new NioEventLoopGroup();
        this.http2ClientInitializer = new Http2ClientInitializer(sslCtx, Integer.MAX_VALUE);
    }

    public void init() {
        bootstrap.group(workerGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.remoteAddress(host, port);
        bootstrap.handler(http2ClientInitializer);
    }

    public void start() throws Exception {
        // Start the client.
        if (channel != null) {
            throw new IllegalStateException("Client already started");
        } else {
            log.debug("Started new HTTP2 client");
            channel = bootstrap.connect().syncUninterruptibly().channel();
            initSettings();
            initResponseHandler();
        }
    }

    private void initResponseHandler() {
        responseHandler = http2ClientInitializer.responseHandler();
    }

    public FullHttpResponse sendRequest(FullHttpRequest request, int streamId) {
        responseHandler.put(streamId, channel.newPromise());
        channel.writeAndFlush(request);
        final SortedMap<Integer, FullHttpResponse> responseMap = responseHandler.awaitResponses(10, TimeUnit.SECONDS);
        return responseMap.get(streamId);
    }

    public void stop() {
        if (channel == null) {
            throw new IllegalStateException("Client is not running");
        } else {
            log.debug("Stopping HTTP2 client");
            channel.close().syncUninterruptibly();
            channel = null;
        }
    }

    private void initSettings() throws Exception {
        // Wait for the HTTP/2 upgrade to occur.
        Http2SettingsHandler http2SettingsHandler = http2ClientInitializer.settingsHandler();
        http2SettingsHandler.awaitSettings(5, TimeUnit.SECONDS);
    }


    @Override
    public void close() throws Exception {
        if (workerGroup != null) {
            log.debug("Closing HTTP2 client");
            workerGroup.shutdownGracefully();
            if (!workerGroup.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Worker group wasn't fully terminated after specified timeout");
            }
            workerGroup = null;
        }
    }
}
