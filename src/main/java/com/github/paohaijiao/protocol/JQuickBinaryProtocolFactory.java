package com.github.paohaijiao.protocol;

import com.github.paohaijiao.config.JQuickProtocolConfig;
import com.github.paohaijiao.console.JConsole;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;

/**
 * Binary 协议工厂
 */
public class JQuickBinaryProtocolFactory implements JQuickProtocolFactory {

    private static final JConsole console = JConsole.getInstance();

    private final JQuickProtocolConfig config;

    private final TBinaryProtocol.Factory factory;

    public JQuickBinaryProtocolFactory(JQuickProtocolConfig config) {
        this.config = config;
        if (config.getStringLengthLimit() > 0 || config.getContainerLengthLimit() > 0) {
            this.factory = new TBinaryProtocol.Factory(config.isStrictRead(), config.isStrictWrite(), config.getStringLengthLimit(), config.getContainerLengthLimit());
        } else {
            this.factory = new TBinaryProtocol.Factory(config.isStrictRead(), config.isStrictWrite());
        }
        console.debug("JQuickBinaryProtocolFactory initialized with strictRead=" + config.isStrictRead() + ", strictWrite=" + config.isStrictWrite());
    }

    @Override
    public TProtocol createProtocol(TTransport transport) {
        return factory.getProtocol(transport);
    }

    @Override
    public String getProtocolType() {
        return "binary";
    }

    @Override
    public JQuickProtocolConfig getConfig() {
        return config;
    }
}
