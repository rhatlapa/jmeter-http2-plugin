package jmeter.plugins.http2.sampler;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

public interface Http2Client extends AutoCloseable {
    void init();

    void initialize(HTTP2Sampler http2Sampler);

    void start() throws Exception;


    FullHttpResponse sendRequest(FullHttpRequest request);

    void stop();

    /**
     * @return true if client is connected (channel is opened), false otherwise;
     */
    boolean isConnected();

    void leaveConnectionOpen();

    String getLogMessage();
}
