package jmeter.plugins.http2.sampler.gui;

import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.protocol.http.gui.HTTPArgumentsPanel;

import javax.swing.*;

public class HTTP2SamplerPanel extends JPanel {

    private HTTPArgumentsPanel attributePanel;

    private JPanel webRequestPanel = new JPanel();
    private JPanel webServerPanel = new JPanel();
    private JPanel timeoutPanel = new JPanel();

    private JLabel connectionIdLabel = new JLabel();
    private JLabel connectionTimeoutLabel = new JLabel();
    private JLabel contentEncodingLabel = new JLabel();
    private JLabel contextPathLabel = new JLabel();

    private JLabel protocolLabel = new JLabel();
    private JLabel responseTimeoutLabel = new JLabel();
    private JLabel serverAddressLabel = new JLabel();
    private JLabel serverPortLabel = new JLabel();


    private JTextField connectionIdTextField = new JTextField();
    private JTextField connectionTimeoutTextField = new JTextField();
    private JTextField contentEncodingTextField = new JTextField();
    private JTextField contextPathTextField = new JTextField();
    private JCheckBox ignoreSslErrorsCheckBox = new JCheckBox();

    private JTextField protocolTextField = new JTextField();
    private JPanel querystringAttributesPanel = new JPanel();
    private JTextField responseTimeoutTextField = new JTextField();
    private JTextField serverAddressTextField = new JTextField();
    private JTextField serverPortTextField = new JTextField();
    private JCheckBox streamingConnectionCheckBox = new JCheckBox();


    public HTTP2SamplerPanel() {
        initComponents();

        attributePanel = new HTTPArgumentsPanel();
        querystringAttributesPanel.add(attributePanel);
    }

    private void initComponents() {

        serverAddressLabel.setText("Server Name or IP:");
        serverPortLabel.setText("Port Number:");

        webServerPanel.setBorder(BorderFactory.createTitledBorder("Web Server"));

        GroupLayout webServerPanelLayout = new javax.swing.GroupLayout(webServerPanel);
        webServerPanel.setLayout(webServerPanelLayout);
        webServerPanelLayout.setHorizontalGroup(
                webServerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(webServerPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(serverAddressLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(serverAddressTextField)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(serverPortLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(serverPortTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );
        webServerPanelLayout.setVerticalGroup(
                webServerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(webServerPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(webServerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(serverAddressLabel)
                                        .addComponent(serverAddressTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(serverPortLabel)
                                        .addComponent(serverPortTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        timeoutPanel.setBorder(BorderFactory.createTitledBorder("Timeout (miliseconds)"));
        responseTimeoutLabel.setText("Response:");
        connectionTimeoutLabel.setText("Connection:");


        javax.swing.GroupLayout timeoutPanelLayout = new javax.swing.GroupLayout(timeoutPanel);
        timeoutPanel.setLayout(timeoutPanelLayout);
        timeoutPanelLayout.setHorizontalGroup(
                timeoutPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(timeoutPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(connectionTimeoutLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(connectionTimeoutTextField)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(responseTimeoutLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(responseTimeoutTextField)
                                .addContainerGap())
        );
        timeoutPanelLayout.setVerticalGroup(
                timeoutPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(timeoutPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(timeoutPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(connectionTimeoutLabel)
                                        .addComponent(connectionTimeoutTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(responseTimeoutLabel)
                                        .addComponent(responseTimeoutTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        connectionIdLabel.setText("Connection Id:");

        contentEncodingLabel.setText("Content encoding:");
        contextPathLabel.setText("Path:");

        protocolLabel.setText("Protocol [http/https]:");

        webRequestPanel.setBorder(BorderFactory.createTitledBorder("Web Request"));


        protocolTextField.setToolTipText("");

        querystringAttributesPanel.setLayout(new BoxLayout(querystringAttributesPanel, BoxLayout.LINE_AXIS));

        ignoreSslErrorsCheckBox.setText("Ignore SSL certificate errors");

        streamingConnectionCheckBox.setText("Streaming connection");

        javax.swing.GroupLayout webRequestPanelLayout = new javax.swing.GroupLayout(webRequestPanel);
        webRequestPanel.setLayout(webRequestPanelLayout);
        webRequestPanelLayout.setHorizontalGroup(
                webRequestPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(webRequestPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(webRequestPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(querystringAttributesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(webRequestPanelLayout.createSequentialGroup()
                                                .addComponent(protocolLabel)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(protocolTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(contentEncodingLabel)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(contentEncodingTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(connectionIdLabel)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(connectionIdTextField))
                                        .addGroup(webRequestPanelLayout.createSequentialGroup()
                                                .addGroup(webRequestPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(webRequestPanelLayout.createSequentialGroup()
                                                                .addComponent(ignoreSslErrorsCheckBox)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(streamingConnectionCheckBox)))
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addGroup(webRequestPanelLayout.createSequentialGroup()
                                                .addComponent(contextPathLabel)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(contextPathTextField)))
                                .addContainerGap())
        );
        webRequestPanelLayout.setVerticalGroup(
                webRequestPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(webRequestPanelLayout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addGroup(webRequestPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(protocolLabel)
                                        .addComponent(protocolTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(contentEncodingLabel)
                                        .addComponent(contentEncodingTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(connectionIdLabel)
                                        .addComponent(connectionIdTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(webRequestPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(contextPathLabel)
                                        .addComponent(contextPathTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(webRequestPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(ignoreSslErrorsCheckBox)
                                        .addComponent(streamingConnectionCheckBox))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(querystringAttributesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 102, Short.MAX_VALUE)
                                .addContainerGap())
        );


        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(webRequestPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(webServerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(timeoutPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                )
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(timeoutPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(webServerPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(webRequestPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );
    }


    public void setConnectionId(String connectionId) {
        connectionIdTextField.setText(connectionId);
    }

    public String getConnectionId() {
        return connectionIdTextField.getText();
    }

    public void setContentEncoding(String contentEncoding) {
        contentEncodingTextField.setText(contentEncoding);
    }

    public String getContentEncoding() {
        return contentEncodingTextField.getText();
    }

    public void setContextPath(String contextPath) {
        contextPathTextField.setText(contextPath);
    }

    public String getContextPath() {
        return contextPathTextField.getText();
    }

    public void setProtocol(String protocol) {
        protocolTextField.setText(protocol);
    }

    public String getProtocol() {
        return protocolTextField.getText();
    }

    public void setResponseTimeout(String responseTimeout) {
        responseTimeoutTextField.setText(responseTimeout);
    }

    public String getResponseTimeout() {
        return responseTimeoutTextField.getText();
    }

    public void setConnectionTimeout(String connectionTimeout) {
        connectionTimeoutTextField.setText(connectionTimeout);
    }

    public String getConnectionTimeout() {
        return connectionTimeoutTextField.getText();
    }

    public void setServerAddress(String serverAddress) {
        serverAddressTextField.setText(serverAddress);
    }

    public String getServerAddress() {
        return serverAddressTextField.getText();
    }

    public void setServerPort(String serverPort) {
        serverPortTextField.setText(serverPort);
    }

    public String getServerPort() {
        return serverPortTextField.getText();
    }

    public void setStreamingConnection(Boolean streamingConnection) {
        streamingConnectionCheckBox.setSelected(streamingConnection);
    }

    public Boolean isStreamingConnection() {
        return streamingConnectionCheckBox.isSelected();
    }

    public void setIgnoreSslErrors(Boolean ignoreSslErrors) {
        ignoreSslErrorsCheckBox.setSelected(ignoreSslErrors);
    }

    public Boolean isIgnoreSslErrors() {
        return ignoreSslErrorsCheckBox.isSelected();
    }


    /**
     * @return the attributePanel
     */
    public ArgumentsPanel getAttributePanel() {
        return attributePanel;
    }

}
