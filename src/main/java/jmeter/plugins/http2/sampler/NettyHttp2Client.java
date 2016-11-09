/*
 *  Copyright 2015 Ryo Okubo
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package jmeter.plugins.http2.sampler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLException;

public class NettyHttp2Client implements Http2Client {

    private static final Logger log = LoggingManager.getLoggerForClass();

    private EventLoopGroup workerGroup;
    private final Bootstrap bootstrap = new Bootstrap();
    private final String host;
    private final int port;
    private final Http2ClientInitializer http2ClientInitializer;
    private HttpResponseHandler responseHandler;
    private Channel channel = null;
    private HTTP2Sampler http2Sampler;
    private CountDownLatch closeLatch = new CountDownLatch(1);

    private final AtomicInteger streamId = new AtomicInteger(3);

    private StringBuilder logMessage = new StringBuilder();

    public NettyHttp2Client(HTTP2Sampler sampler) {
        this.http2Sampler = sampler;
        logMessage.append("\n\n[Execution Flow]\n");
        logMessage.append(" - Opening new connection\n");
        this.host = sampler.getServerAddress();
        this.port = sampler.getServerPortAsInt();
        SslContext sslCtx = null;
        if ("https".equals(sampler.getProtocolScheme())) {
            sslCtx = NettyHttp2Client.getSslContext();
        }
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

    public void initialize(HTTP2Sampler sampler) {
        this.http2Sampler = sampler;
        this.closeLatch = new CountDownLatch(1);
        this.logMessage = new StringBuilder();
        this.logMessage.append("\n\n[Execution Flow]\n");
        this.logMessage.append(" - Reusing existing connection: ").append(http2Sampler.getConnectionId()).append('\n');

    }

    public void start() throws Exception {
        // Start the client.
        if (isConnected()) {
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

    public FullHttpResponse sendRequest(FullHttpRequest request) {
        int streamIdAsInt = streamId.getAndAdd(2);
        log.debug("Sending request and expecting response on stream " + streamIdAsInt);
        responseHandler.put(streamIdAsInt, channel.newPromise());
        channel.writeAndFlush(request);
        final SortedMap<Integer, FullHttpResponse> responseMap = responseHandler.awaitResponses(10, TimeUnit.SECONDS);
        return responseMap.get(streamIdAsInt);
    }

    public void stop() {
        if (!isConnected()) {
            throw new IllegalStateException("Client is not running");
        } else {
            log.debug("Stopping HTTP2 client");
            channel.close().syncUninterruptibly();
            channel = null;
            closeLatch.countDown();
        }
    }

    private void initSettings() throws Exception {
        // Wait for the HTTP/2 upgrade to occur.
        Http2SettingsHandler http2SettingsHandler = http2ClientInitializer.settingsHandler();
        http2SettingsHandler.awaitSettings(5, TimeUnit.SECONDS);
    }

    /**
     * @return true if client is connected (channel is opened), false otherwise;
     */
    public boolean isConnected() {
        return (channel != null) && channel.isOpen();
    }

    public void leaveConnectionOpen() {
        if (isConnected()) {
            logMessage.append(" - Leaving streaming connection open").append('\n');
        }
    }

    @Override
    public void close() throws Exception {
        if (workerGroup != null) {
            log.debug("Closing the client and its connection");
            logMessage.append("Closing the client and its connection");
            try {
                if (isConnected()) {
                    stop();
                }
            } finally {
                log.debug("Shutting down worker group used by the HTTP2 client");
                workerGroup.shutdownGracefully();
                if (!workerGroup.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("Worker group wasn't fully terminated after specified timeout");
                }
                workerGroup = null;
            }
        }
    }

    public String getLogMessage() {
        return logMessage.toString();
    }


    public static SslContext getSslContext() {
        SslContext sslCtx = null;

        final SslProvider provider = OpenSsl.isAlpnSupported() ? SslProvider.OPENSSL : SslProvider.JDK;

        try {
            sslCtx = SslContextBuilder.forClient()
                    .sslProvider(provider)
                    .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .applicationProtocolConfig(new ApplicationProtocolConfig(
                            Protocol.ALPN,
                            SelectorFailureBehavior.NO_ADVERTISE,
                            SelectedListenerFailureBehavior.ACCEPT,
                            ApplicationProtocolNames.HTTP_2))
                    .build();
        } catch (SSLException exception) {
            return null;
        }

        return sslCtx;
    }

    /**
     * Convert Response headers set by Netty stack to one String instance
     */
    public static String getResponseHeaders(FullHttpResponse response) {
        StringBuilder headerBuf = new StringBuilder();

        Iterator<Entry<String, String>> iterator = response.headers().iteratorAsString();
        while (iterator.hasNext()) {
            Entry<String, String> entry = iterator.next();
            headerBuf.append(entry.getKey());
            headerBuf.append(": ");
            headerBuf.append(entry.getValue());
            headerBuf.append("\n");
        }

        return headerBuf.toString();
    }
}
