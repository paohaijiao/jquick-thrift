package com.github.paohaijiao.factory.impl;


import com.github.paohaijiao.config.JQuickServerConfig;
import com.github.paohaijiao.enums.JQuickServerTypeEnums;
import com.github.paohaijiao.factory.JQuickServerFactory;
import com.github.paohaijiao.protocol.JQuickProtocolFactory;
import com.github.paohaijiao.server.JQuickThriftServer;
import com.github.paohaijiao.server.impl.JQuickThreadPoolServer;
import com.github.paohaijiao.transport.JQuickTransportFactory;

/**
 * 线程池服务器工厂
 */
public class JQuickThreadPoolServerFactory implements JQuickServerFactory {

    @Override
    public JQuickThriftServer create(JQuickServerConfig config, JQuickProtocolFactory protocolFactory, JQuickTransportFactory transportFactory) {
        return new JQuickThreadPoolServer(config, protocolFactory, transportFactory);
    }

    @Override
    public String getServerType() {
        return JQuickServerTypeEnums.threadpool.getCode();
    }
}
