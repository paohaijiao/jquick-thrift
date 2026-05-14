package com.github.paohaijiao.protocol;

import com.github.paohaijiao.config.JQuickProtocolConfig;
import com.github.paohaijiao.console.JConsole;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;

/**
 * Compact 协议工厂
 */
public class JQuickCompactProtocolFactory implements JQuickProtocolFactory {

    private static final JConsole console = JConsole.getInstance();

    private final JQuickProtocolConfig config;

    private final TCompactProtocol.Factory factory;

    public JQuickCompactProtocolFactory(JQuickProtocolConfig config) {
        this.config = config;
        if (config.getStringLengthLimit() > 0 || config.getContainerLengthLimit() > 0) {
            this.factory = new TCompactProtocol.Factory(config.getStringLengthLimit(), config.getContainerLengthLimit());
        } else {
            this.factory = new TCompactProtocol.Factory();
        }
        console.debug("JQuickCompactProtocolFactory initialized");
    }

    @Override
    public TProtocol createProtocol(TTransport transport) {
        return factory.getProtocol(transport);
    }

    @Override
    public String getProtocolType() {
        return "compact";
    }

    @Override
    public JQuickProtocolConfig getConfig() {
        return config;
    }
}
