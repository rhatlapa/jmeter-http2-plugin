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
package jmeter.plugins.http2.sampler.gui;

import jmeter.plugins.http2.sampler.HTTP2Sampler;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import java.awt.*;

public class HTTP2SamplerGui extends AbstractSamplerGui {

    private static final Logger log = LoggingManager.getLoggerForClass();

    private HTTP2SamplerPanel http2SamplerPanel;

    public HTTP2SamplerGui(){
        super();
        init();

        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());

        this.add(makeTitlePanel(), BorderLayout.NORTH);
        this.add(http2SamplerPanel, BorderLayout.CENTER);
    }

    @Override
    public String getStaticLabel() {
        return "HTTP2 Sampler";
    }

    public String getLabelResource() {
        return "HTTP2 Sampler";
    }

    public TestElement createTestElement() {
        HTTP2Sampler sampler = new HTTP2Sampler();

        modifyTestElement(sampler);

        return sampler;
    }

    @Override
    public void configure(TestElement element) {
        super.configure(element);

        if (element instanceof HTTP2Sampler) {
            HTTP2Sampler sampler = (HTTP2Sampler) element;
            /* method.setText(sampler.getMethod()); */
            http2SamplerPanel.setProtocol(sampler.getProtocolScheme());
            http2SamplerPanel.setServerAddress(sampler.getServerAddress());
            http2SamplerPanel.setServerPort(sampler.getServerPort());
            http2SamplerPanel.setContextPath(sampler.getContextPath());

            http2SamplerPanel.setProtocol(sampler.getProtocolScheme());
            http2SamplerPanel.setContentEncoding(sampler.getContentEncoding());
            http2SamplerPanel.setResponseTimeout(sampler.getResponseTimeout());
            http2SamplerPanel.setConnectionTimeout(sampler.getConnectionTimeout());
            http2SamplerPanel.setIgnoreSslErrors(sampler.isIgnoreSslErrors());
            http2SamplerPanel.setStreamingConnection(sampler.isStreamingConnection());
            http2SamplerPanel.setConnectionId(sampler.getConnectionId());

            Arguments queryStringParameters = sampler.getQueryStringParameters();
            if (queryStringParameters != null) {
                http2SamplerPanel.getAttributePanel().configure(queryStringParameters);
            }
        }
    }

    public void modifyTestElement(TestElement element) {
        configureTestElement(element);
        if (element instanceof HTTP2Sampler) {
            HTTP2Sampler http2Sampler = (HTTP2Sampler) element;
            http2Sampler.setServerAddress(http2SamplerPanel.getServerAddress());
            http2Sampler.setServerPort(http2SamplerPanel.getServerPort());
            http2Sampler.setProtocolScheme(http2SamplerPanel.getProtocol());
            http2Sampler.setContextPath(http2SamplerPanel.getContextPath());
            http2Sampler.setContentEncoding(http2SamplerPanel.getContentEncoding());
            http2Sampler.setConnectionTimeout(http2SamplerPanel.getConnectionTimeout());
            http2Sampler.setResponseTimeout(http2SamplerPanel.getResponseTimeout());
            http2Sampler.setIgnoreSslErrors(http2SamplerPanel.isIgnoreSslErrors());
            http2Sampler.setStreamingConnection(http2SamplerPanel.isStreamingConnection());
            http2Sampler.setConnectionId(http2SamplerPanel.getConnectionId());


            ArgumentsPanel queryStringParameters = http2SamplerPanel.getAttributePanel();
            if (queryStringParameters != null) {
                http2Sampler.setQueryStringParameters((Arguments)queryStringParameters.createTestElement());
            }
        }
        
      
    }

    @Override
    public void clearGui() {
        super.clearGui();
    }

    private void init() {
        http2SamplerPanel = new HTTP2SamplerPanel();
    }
}
