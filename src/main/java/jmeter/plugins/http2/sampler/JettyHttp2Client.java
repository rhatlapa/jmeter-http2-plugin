package jmeter.plugins.http2.sampler;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http2.api.Session;
import org.eclipse.jetty.http2.api.Stream;
import org.eclipse.jetty.http2.api.server.ServerSessionListener;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.http2.frames.PushPromiseFrame;
import org.eclipse.jetty.util.FuturePromise;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class JettyHttp2Client {

    private final HTTP2Client client = new HTTP2Client();
    private final SslContextFactory sslContextFactory = new SslContextFactory(true);
    private final String host;
    private final int port;
    private static final Logger log = LoggingManager.getLoggerForClass();
    public static final String HTTP_SCHEME = "http";

    public JettyHttp2Client(HTTP2Sampler sampler) {
        this.host = sampler.getServerAddress();
        this.port = sampler.getServerPortAsInt();
    }

    public void init() {
        client.addBean(sslContextFactory);
    }

    public void start() throws Exception {
        if (client.isRunning()) {
            log.debug("Client is already running!");
        } else {
            log.debug("Started new HTTP2 client");
            client.start();
        }
    }

    public boolean isConnected() {
        return client.isRunning();
    }

    public CustomResponse sendRequest(MetaData.Request request) throws Exception {
        FuturePromise<Session> sessionPromise = new FuturePromise<>();
        if(request.getURI().getScheme().equals(HTTP_SCHEME)) {
            client.connect(new InetSocketAddress(host, port), new ServerSessionListener.Adapter(), sessionPromise);
        }else {
            client.connect(sslContextFactory, new InetSocketAddress(host, port), new ServerSessionListener.Adapter(), sessionPromise);
        }
        Session session = sessionPromise.get(5, TimeUnit.SECONDS);

        HeadersFrame headersFrame = new HeadersFrame(request, null, true);
        CountDownLatch clientLatch = new CountDownLatch(1);
        OpenStreamListener adapter = new OpenStreamListener(clientLatch);

        log.debug("Sending request " + headersFrame.getMetaData().toString());
        session.newStream(headersFrame, new FuturePromise<>(), adapter);
        clientLatch.await(5, TimeUnit.SECONDS);
        return adapter.getResponseWithResources();
    }

    public void stop() throws Exception {
        log.debug("Stopping the client.");
        client.stop();
    }

    public String getResponseHeaders(HttpFields fields) {
        StringBuilder headerBuf = new StringBuilder();
        fields.iterator();
        Iterator<HttpField> iterator = fields.iterator();
        while (iterator.hasNext()) {
            HttpField entry = iterator.next();
            headerBuf.append(entry.getName());
            headerBuf.append(": ");
            headerBuf.append(entry.getValue());
            headerBuf.append("\n");
        }

        return headerBuf.toString();
    }

    private class OpenStreamListener extends Stream.Listener.Adapter {

        private CustomResponse responseWithResources = null;
        private final CountDownLatch latch;
        private final Set<URL> pushedResources = new HashSet<>();

        public OpenStreamListener(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onHeaders(Stream stream, HeadersFrame frame) {
            if (frame.getMetaData().isResponse()) {
                MetaData.Response response = (MetaData.Response) frame.getMetaData();
                responseWithResources = new CustomResponse(response, pushedResources);
                log.debug("Received response " + response);
                latch.countDown();
            }
        }

        @Override
        public Stream.Listener onPush(Stream stream, PushPromiseFrame frame) {
            if (frame.getMetaData().isRequest()) {
                MetaData.Request pushRequest = ((MetaData.Request) frame.getMetaData());
                log.debug("Received server push request " + pushRequest);
                HttpURI uri = pushRequest.getURI();
                try {
                    URL resourceURL = new URL(uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort() + uri.getPath());
                    pushedResources.add(resourceURL);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
            return this;
        }

        public CustomResponse getResponseWithResources() {
            return responseWithResources;
        }

    }
}
