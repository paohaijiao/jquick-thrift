package com.github.paohaijiao.factory.impl;

import com.github.paohaijiao.config.JQuickServerConfig;
import com.github.paohaijiao.factory.JQuickServerFactory;
import com.github.paohaijiao.protocol.JQuickProtocolFactory;
import com.github.paohaijiao.server.JQuickThriftServer;
import com.github.paohaijiao.server.impl.JQuickHsHaServer;
import com.github.paohaijiao.transport.JQuickTransportFactory;

/**
 * 半同步半异步服务器工厂
 */
public class JQuickHsHaServerFactory implements JQuickServerFactory {

    @Override
    public JQuickThriftServer create(JQuickServerConfig config, JQuickProtocolFactory protocolFactory, JQuickTransportFactory transportFactory) {
        return new JQuickHsHaServer(config, protocolFactory, transportFactory);
    }

    @Override
    public String getServerType() {
        return "hsha";
    }
}
