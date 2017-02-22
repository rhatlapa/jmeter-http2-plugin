package jmeter.plugins.http2.sampler;

import java.util.Map;

import io.netty.handler.codec.http.FullHttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.MetaData;

public class HttpResponseAdapter {

    /**
     *
     * @return synthetic {@link org.apache.http.message.BasicHttpResponse} from given {@link io.netty.handler.codec.http.FullHttpResponse}
     */
    public static BasicHttpResponse wrapAsApacheHttpReponse(FullHttpResponse fullHttpResponse) {
        String reasonPhrase = fullHttpResponse.status().reasonPhrase();
        int code = fullHttpResponse.status().code();
        ProtocolVersion version = new ProtocolVersion("HTTP", 2, 0);
        BasicStatusLine syntheticLine = new BasicStatusLine(version, code, reasonPhrase);
        BasicHttpResponse response = new BasicHttpResponse(syntheticLine);
        for (Map.Entry<String, String> header : fullHttpResponse.headers()) {
            response.addHeader(header.getKey(), header.getValue());
        }
        response.addHeader("Cache-Control","max-age=2592000");
        return response;
    }

    public static BasicHttpResponse wrapAsApacheHttpReponse(MetaData.Response response) {
        String reasonPhrase = response.getReason();
        int code = response.getStatus();
        ProtocolVersion version = new ProtocolVersion("HTTP", 2, 0);
        BasicStatusLine syntheticLine = new BasicStatusLine(version, code, reasonPhrase);
        BasicHttpResponse basicHttpResponse = new BasicHttpResponse(syntheticLine);
        for (HttpField field : response.getFields()) {
            basicHttpResponse.addHeader(field.getName(), field.getValue());
        }
        basicHttpResponse.addHeader("Cache-Control","max-age=2592000");
        return basicHttpResponse;
    }
}
