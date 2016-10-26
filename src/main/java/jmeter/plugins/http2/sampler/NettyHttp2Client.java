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

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
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
import io.netty.util.AsciiString;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Iterator;
import java.util.Map.Entry;
import javax.net.ssl.SSLException;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class NettyHttp2Client {
    private static final Logger log = LoggingManager.getLoggerForClass();

    private final String method;
    private final String scheme;
    private final String host;
    private final int port;
    private final String path;
    private final HeaderManager headerManager;

    private final SslContext sslCtx;

    public NettyHttp2Client(String method, String host, int port, String path, HeaderManager headerManager, String scheme) {
        this.method = method;
        this.host = host;
        this.port = port;
        this.path = path;
        this.headerManager = headerManager;
        this.scheme = scheme;
        if ("https".equals(scheme)) {
            sslCtx = getSslContext();
            if (sslCtx == null) {
                throw new RuntimeException("Failed to create SSL context for https");
            }
        } else {
            sslCtx = null;
        }
    }

    public HTTPSampleResult request() {
        HTTPSampleResult sampleResult = new HTTPSampleResult();

        // Configure the client.
        try (Http2Client http2Client = new Http2Client(sslCtx, host, port)) {
            http2Client.init();

            // Start sampling
            sampleResult.sampleStart();

            // Start the client.
            http2Client.start();

            final URI hostName = URI.create(scheme + "://" + host + ':' + port);

            // Set attributes to SampleResult
            try {
                sampleResult.setURL(hostName.toURL());
            } catch (MalformedURLException exception) {
                sampleResult.setSuccessful(false);
                return sampleResult;
            }

            String requestPath = (path.startsWith("/")) ? hostName.toString() + path : path;

            FullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, new HttpMethod(method), requestPath);
            request.headers().add(HttpHeaderNames.HOST, hostName);

            // Add request headers set by HeaderManager
            if (headerManager != null) {
                CollectionProperty headers = headerManager.getHeaders();
                if (headers != null) {
                    PropertyIterator i = headers.iterator();
                    while (i.hasNext()) {
                        org.apache.jmeter.protocol.http.control.Header header
                                = (org.apache.jmeter.protocol.http.control.Header) i.next().getObjectValue();
                        request.headers().add(header.getName(), header.getValue());
                    }
                }
            }

            int streamId = 3;
            try {
                final FullHttpResponse response = http2Client.sendRequest(request, streamId);
                final AsciiString responseCode = response.status().codeAsText();
                final String reasonPhrase = response.status().reasonPhrase();
                sampleResult.setResponseCode(new StringBuilder(responseCode.length()).append(responseCode).toString());
                sampleResult.setResponseMessage(new StringBuilder(reasonPhrase.length()).append(reasonPhrase).toString());
                sampleResult.setResponseHeaders(getResponseHeaders(response));
            } catch (Exception exception) {
                sampleResult.setSuccessful(false);
                return sampleResult;
            }

            http2Client.stop();

            // End sampling
            sampleResult.sampleEnd();
            sampleResult.setSuccessful(true);
        } catch (Exception ex) {
            sampleResult.setSuccessful(false);
        }

        return sampleResult;
    }

    private SslContext getSslContext() {
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
    private String getResponseHeaders(FullHttpResponse response) {
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
