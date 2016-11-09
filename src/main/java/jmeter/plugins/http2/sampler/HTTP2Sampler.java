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
import io.netty.util.AsciiString;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.http.control.CookieHandler;
import org.apache.jmeter.protocol.http.control.CookieManager;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.protocol.http.util.EncoderCache;
import org.apache.jmeter.protocol.http.util.HTTPArgument;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.ThreadListener;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.reflect.ClassTools;
import org.apache.jorphan.util.JMeterException;
import org.apache.jorphan.util.JOrphanUtils;
import org.apache.log.Logger;

import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HTTP2Sampler extends AbstractSampler implements TestStateListener, ThreadListener {
    private static final long serialVersionUID = 5859387434748163229L;

    private static final Logger log = LoggingManager.getLoggerForClass();

    public static final int DEFAULT_CONNECTION_TIMEOUT = 20000; //20 sec
    public static final int DEFAULT_RESPONSE_TIMEOUT = 20000; //20 sec

    public static final String METHOD = "HTTP2Sampler.method";
    public static final String PROTOCOL_SCHEME = "HTTP2Sampler.scheme";

    private static final String ARG_VAL_SEP = "="; // $NON-NLS-1$
    private static final String QRY_SEP = "&"; // $NON-NLS-1$

    public static final String DEFAULT_METHOD = "GET";

    private static final Map<String, Http2Client> connectionsMap = Collections.synchronizedMap(new HashMap<String, Http2Client>());

    private HeaderManager headerManager;
    private CookieManager cookieManager;
    private CookieHandler cookieHandler;


    public HTTP2Sampler() {
        super();
        setName("HTTP2 Sampler");
    }


    public URI getUri() throws URISyntaxException {
        String path = this.getContextPath();
        // Hack to allow entire URL to be provided in host field
        if (path.startsWith("http://")
                || path.startsWith("https://")) {
            return new URI(path);
        }
        String domain = getServerAddress();
        String scheme = getProtocolScheme();
        // HTTP URLs must be absolute, allow file to be relative
        if (!path.startsWith("/")) { // $NON-NLS-1$
            path = "/" + path; // $NON-NLS-1$
        }

        String queryString = getQueryString(getContentEncoding());
        return new URI(scheme, null, domain, getServerPortAsInt(), path, queryString, null);
    }

    public String getQueryString(String contentEncoding) {
        // Check if the sampler has a specified content encoding
        if (JOrphanUtils.isBlank(contentEncoding)) {
            // We use the encoding which should be used according to the HTTP spec, which is UTF-8
            contentEncoding = EncoderCache.URL_ARGUMENT_ENCODING;
        }
        StringBuilder buf = new StringBuilder();
        PropertyIterator iter = getQueryStringParameters().iterator();
        boolean first = true;
        while (iter.hasNext()) {
            HTTPArgument item;
            Object objectValue = iter.next().getObjectValue();
            try {
                item = (HTTPArgument) objectValue;
            } catch (ClassCastException e) {
                item = new HTTPArgument((Argument) objectValue);
            }
            final String encodedName = item.getEncodedName();
            if (encodedName.length() == 0) {
                continue; // Skip parameters with a blank name (allows use of optional variables in parameter lists)
            }
            if (!first) {
                buf.append(QRY_SEP);
            } else {
                first = false;
            }
            buf.append(encodedName);
            if (item.getMetaData() == null) {
                buf.append(ARG_VAL_SEP);
            } else {
                buf.append(item.getMetaData());
            }

            // Encode the parameter value in the specified content encoding
            try {
                buf.append(item.getEncodedValue(contentEncoding));
            } catch (UnsupportedEncodingException e) {
                log.warn("Unable to encode parameter in encoding " + contentEncoding + ", parameter value not included in query string");
            }
        }
        return buf.toString();
    }


    @Override
    public void setName(String name) {
        if (name != null) {
            setProperty(TestElement.NAME, name);
        }
    }

    @Override
    public String getName() {
        return getPropertyAsString(TestElement.NAME);
    }

    @Override
    public void addTestElement(TestElement el) {
        if (el instanceof HeaderManager) {
            headerManager = (HeaderManager) el;
        } else if (el instanceof CookieManager) {
            cookieManager = (CookieManager) el;
            try {
                cookieHandler = (CookieHandler) ClassTools.construct(cookieManager.getImplementation(), cookieManager.getPolicy());
            } catch (JMeterException e) {
                log.error("Failed to construct cookie handler ", e);
            }
        } else {
            super.addTestElement(el);
        }
    }


    @Override
    public SampleResult sample(Entry entry) {
        return sample(null, getMethod(), true, -1);
    }

    String getConnectionIdForConnectionsMap() {
        return getThreadName() + getConnectionId();
    }

    private Http2Client getHttp2Client() throws Exception {
        boolean addSocketToMap = false;

        String connectionId = getConnectionIdForConnectionsMap();
        Http2Client http2Client;

        if (isStreamingConnection()) {
            http2Client = connectionsMap.get(connectionId);
            if (null != http2Client) {
                http2Client.initialize(this);
                return http2Client;
            }
        }

        http2Client = new NettyHttp2Client(this);
        if (isStreamingConnection()) {
            addSocketToMap = true;
        }


        http2Client.init();
        http2Client.start();

        if (addSocketToMap) {
            connectionsMap.put(connectionId, http2Client);
        }
        return http2Client;
    }

    protected HTTPSampleResult sample(URL url, String method, boolean followRedirects, int depth) {
        // Load test elements
        HeaderManager headerManager = (HeaderManager) getProperty(HTTPSamplerBase.HEADER_MANAGER).getObjectValue();

        HTTPSampleResult sampleResult = new HTTPSampleResult();
        sampleResult.setSampleLabel(getName());

        //This StringBuilder will track all exceptions related to the protocol processing
        StringBuilder errorList = new StringBuilder();
        errorList.append("\n\n[Problems]\n");

        //Could improve precission by moving this closer to the action
        sampleResult.sampleStart();

        boolean isOK = false;

        Http2Client http2Client = null;
        try {
            http2Client = getHttp2Client();
            if (http2Client == null) {
                //Couldn't open a connection, set the status and exit
                sampleResult.setResponseCode("500");
                sampleResult.setSuccessful(false);
                sampleResult.sampleEnd();
                sampleResult.setResponseMessage(errorList.toString());
                errorList.append(" - Connection couldn't be opened\n");
                return sampleResult;
            }

            URL requestUrl = url;
            if (url == null) {
                requestUrl = getUri().toURL();
            }

            sampleResult.setURL(url);


            FullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, new HttpMethod(method), requestUrl.toExternalForm());
            request.headers().add(HttpHeaderNames.HOST, String.format("%s:%s", requestUrl.getHost(), requestUrl.getPort()));
            if (cookieManager != null) {
                for (int i = 0; i < cookieManager.getCookieCount(); i++) {
                    HttpCookie cookie = new HttpCookie(cookieManager.get(i).getName(), cookieManager.get(i).getValue());
                    cookie.setVersion(cookieManager.get(i).getVersion());
                    if ("JSESSIONID".equals(cookie.getName())) {
                        request.headers().add("Cookie", String.format("%s=%s", cookie.getName(), cookie.getValue()));
                    }
                }
            }


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

            try {
                final FullHttpResponse response = http2Client.sendRequest(request);
                final AsciiString responseCode = response.status().codeAsText();
                final String reasonPhrase = response.status().reasonPhrase();
                sampleResult.setResponseCode(new StringBuilder(responseCode.length()).append(responseCode).toString());
                sampleResult.setResponseMessage(new StringBuilder(reasonPhrase.length()).append(reasonPhrase).toString());
                sampleResult.setResponseHeaders(NettyHttp2Client.getResponseHeaders(response));
                if (cookieManager != null && cookieHandler != null) {
                    String setCookieHeader = response.headers().get("set-cookie");
                    if (setCookieHeader != null) {
                        cookieHandler.addCookieFromHeader(cookieManager, true, setCookieHeader, new URL(
                                requestUrl.getProtocol(),
                                requestUrl.getHost(),
                                requestUrl.getPort(),
                                requestUrl.getQuery() != null ? requestUrl.getPath() + "?" + requestUrl.getQuery() : requestUrl.getPath()
                        ));
                    }
                }
            } catch (Exception exception) {
                sampleResult.setSuccessful(false);
                return sampleResult;
            }

            if (isStreamingConnection()) {
                http2Client.leaveConnectionOpen();
            } else {
                http2Client.close();
            }
            isOK = true;

            // End sampling
            sampleResult.sampleEnd();
            sampleResult.setSuccessful(true);

        } catch (Exception e) {
            isOK = false;
            errorList.append(" - Execution interrupted: ").append(e.getMessage()).append("\n").append(StringUtils.join(e.getStackTrace(), "\n")).append("\n");
        } finally {
            if (sampleResult.getEndTime() == 0L) {
                sampleResult.sampleEnd();
            }
            if (sampleResult.getResponseCode().isEmpty()) {
                sampleResult.setResponseCode("400");
            }
            if (http2Client != null) {
                if (!isOK) {
                    try {
                        http2Client.close();
                    } catch (Exception e) {
                        errorList.append(" - Closing client resulted in exception: ").append(e.getMessage()).append("\n").append(StringUtils.join(e.getStackTrace(), "\n")).append("\n");
                        isOK = false;
                    }
                }
                if (!http2Client.isConnected()) {
                    connectionsMap.remove(getConnectionIdForConnectionsMap());
                }
            }
            sampleResult.setSuccessful(isOK);
            String logMessage = (http2Client != null) ? http2Client.getLogMessage() : "";
            sampleResult.setResponseMessage(logMessage + errorList);
            return sampleResult;
        }
    }


    public void setMethod(String value) {
        setProperty(METHOD, value);
    }

    public String getMethod() {
        return getPropertyAsString(METHOD);
    }

    public void setProtocolScheme(String value) {
        setProperty(PROTOCOL_SCHEME, value);
    }

    public String getProtocolScheme() {
        return getPropertyAsString(PROTOCOL_SCHEME);
    }

    public void setServerAddress(String serverAddress) {
        setProperty("serverAddress", serverAddress);
    }

    public String getServerAddress() {
        return getPropertyAsString("serverAddress");
    }

    public String getResponseTimeout() {
        return getPropertyAsString("responseTimeout", "20000");
    }

    public void setResponseTimeout(String responseTimeout) {
        setProperty("responseTimeout", responseTimeout);
    }

    public void setContextPath(String contextPath) {
        setProperty("contextPath", contextPath);
    }

    public String getContextPath() {
        return getPropertyAsString("contextPath");
    }

    public String getServerPort() {
        final String portAsString = getPropertyAsString("serverPort", "0");
        Integer port;

        try {
            port = Integer.valueOf(portAsString);
            return port.toString();
        } catch (NumberFormatException ex) {
            return portAsString;
        }
    }

    public int getServerPortAsInt() {
        String portAsString = getServerPort();
        int port = 0;
        try {
            port = Integer.parseInt(portAsString);
        } catch (NumberFormatException ex) {
            port = ("http".equalsIgnoreCase(getProtocolScheme())) ? 80 : 443;
        }
        return port;
    }

    public void setServerPort(String port) {
        setProperty("serverPort", port);
    }


    public void setQueryStringParameters(Arguments queryStringParameters) {
        setProperty(new TestElementProperty("queryStringParameters", queryStringParameters));
    }

    public Arguments getQueryStringParameters() {
        return (Arguments) getProperty("queryStringParameters").getObjectValue();
    }

    public void setStreamingConnection(Boolean streamingConnection) {
        setProperty("streamingConnection", streamingConnection);
    }

    public Boolean isStreamingConnection() {
        return getPropertyAsBoolean("streamingConnection");
    }

    public void setConnectionId(String connectionId) {
        setProperty("connectionId", connectionId);
    }

    public String getConnectionId() {
        return getPropertyAsString("connectionId");
    }

    public String getConnectionTimeout() {
        return getPropertyAsString("connectionTimeout", "5000");
    }

    public void setConnectionTimeout(String connectionTimeout) {
        setProperty("connectionTimeout", connectionTimeout);
    }

    public void setContentEncoding(String contentEncoding) {
        setProperty("contentEncoding", contentEncoding);
    }

    public String getContentEncoding() {
        return getPropertyAsString("contentEncoding", "UTF-8");
    }

    public void setIgnoreSslErrors(Boolean ignoreSslErrors) {
        setProperty("ignoreSslErrors", ignoreSslErrors);
    }

    public Boolean isIgnoreSslErrors() {
        return getPropertyAsBoolean("ignoreSslErrors");
    }

    private HeaderManager getHeaderManager() {
        return (HeaderManager) getProperty(HTTPSamplerBase.HEADER_MANAGER).getObjectValue();
    }

    @Override
    public void testStarted() {
        testStarted("unknown");
    }

    @Override
    public void testStarted(String host) {
        try {
            setProperty("connectionTimeoutInt", Integer.parseInt(getConnectionTimeout()));
        } catch (NumberFormatException ex) {
            log.warn("Connection timeout is not a number; using the default connection timeout of " + DEFAULT_CONNECTION_TIMEOUT + " ms");
            setProperty("connectionTimeoutInt", DEFAULT_RESPONSE_TIMEOUT);
        }
        try {
            setProperty("responseTimeoutInt", Integer.parseInt(getResponseTimeout()));
        } catch (NumberFormatException ex) {
            log.warn("Response timeout is not a number; using the default response timeout of " + DEFAULT_RESPONSE_TIMEOUT + " ms");
            setProperty("responseTimeoutInt", DEFAULT_RESPONSE_TIMEOUT);
        }
    }

    @Override
    public void testEnded() {
        testEnded("unknown");

    }

    @Override
    public void testEnded(String host) {
        for (Http2Client client : connectionsMap.values()) {
            try {
                client.close();
            } catch (Exception ex) {
                log.warn("Failed to close HTTP/2 client", ex);
            }
        }
        connectionsMap.clear();
    }

    @Override
    public void threadStarted() {

    }

    @Override
    public void threadFinished() {
        Http2Client client = connectionsMap.get(getConnectionIdForConnectionsMap());
        if (client != null) {
            try {
                client.close();
                connectionsMap.remove(client);
            } catch (Exception ex) {
                log.warn("Failed to close client " + getConnectionIdForConnectionsMap(), ex);
            }
        }
    }
}

