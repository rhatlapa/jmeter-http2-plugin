package jmeter.plugins.http2.sampler;

import java.util.Map;

import io.netty.handler.codec.http.FullHttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

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
}
