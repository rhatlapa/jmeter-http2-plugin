package jmeter.plugins.http2.sampler;

import java.net.URL;
import java.util.Set;

import org.eclipse.jetty.http.MetaData;

public class CustomResponse extends MetaData.Response {

    private final Set<URL> pushedResources;

    public CustomResponse(Response response,Set<URL> pushedResources) {
        super(response.getHttpVersion(), response.getStatus(), response.getFields());
        this.pushedResources = pushedResources;
    }

    public Set<URL> getPushedResources() {
        return pushedResources;
    }

}
