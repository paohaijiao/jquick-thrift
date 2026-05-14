package com.github.paohaijiao.protocol;

import com.github.paohaijiao.config.JQuickProtocolConfig;
import com.github.paohaijiao.console.JConsole;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;

/**
 * JSON 协议工厂
 */
public class JQuickJsonProtocolFactory implements JQuickProtocolFactory {

    private static final JConsole console = JConsole.getInstance();

    private final JQuickProtocolConfig config;

    private final TJSONProtocol.Factory factory;

    public JQuickJsonProtocolFactory(JQuickProtocolConfig config) {
        this.config = config;
        this.factory = new TJSONProtocol.Factory();
        console.debug("JQuickJsonProtocolFactory initialized");
    }

    @Override
    public TProtocol createProtocol(TTransport transport) {
        return factory.getProtocol(transport);
    }

    @Override
    public String getProtocolType() {
        return "json";
    }

    @Override
    public JQuickProtocolConfig getConfig() {
        return config;
    }
}
