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
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.util.JOrphanUtils;
import org.apache.log.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HTTP2Sampler extends AbstractSampler implements TestStateListener {
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
            HeaderManager value = (HeaderManager) el;
            HeaderManager currentHeaderManager = getHeaderManager();
            if (currentHeaderManager != null) {
                value = currentHeaderManager.merge(value, true);
            }
            setProperty(new TestElementProperty(HTTPSamplerBase.HEADER_MANAGER, value));
        } else {
            super.addTestElement(el);
        }
    }


    @Override
    public SampleResult sample(Entry entry) {
        return sample(null, getMethod(), true, -1);
    }

    protected HTTPSampleResult sample(URL url, String method, boolean followRedirects, int depth) {
        // Load test elements
        HeaderManager headerManager = (HeaderManager)getProperty(HTTPSamplerBase.HEADER_MANAGER).getObjectValue();

        // Send H2 request
        NettyHttp2Client client = new NettyHttp2Client(method, getServerAddress(), getServerPortAsInt(), getContextPath(), headerManager, getProtocolScheme());
        HTTPSampleResult res = client.request();
        res.setSampleLabel(getName());

        return res;
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
        return (HeaderManager)getProperty(HTTPSamplerBase.HEADER_MANAGER).getObjectValue();
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
}

