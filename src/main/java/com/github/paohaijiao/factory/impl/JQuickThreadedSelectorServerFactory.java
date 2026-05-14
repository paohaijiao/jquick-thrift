package com.github.paohaijiao.factory.impl;

import com.github.paohaijiao.config.JQuickServerConfig;
import com.github.paohaijiao.factory.JQuickServerFactory;
import com.github.paohaijiao.protocol.JQuickProtocolFactory;
import com.github.paohaijiao.server.JQuickThriftServer;
import com.github.paohaijiao.server.impl.JQuickThreadedSelectorServer;
import com.github.paohaijiao.transport.JQuickTransportFactory;

/**
 * 主从 Reactor 服务器工厂
 */
public class JQuickThreadedSelectorServerFactory implements JQuickServerFactory {

    @Override
    public JQuickThriftServer create(JQuickServerConfig config, JQuickProtocolFactory protocolFactory, JQuickTransportFactory transportFactory) {
        return new JQuickThreadedSelectorServer(config, protocolFactory, transportFactory);
    }

    @Override
    public String getServerType() {
        return "selector";
    }
}