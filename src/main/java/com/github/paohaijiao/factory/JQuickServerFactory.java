package com.github.paohaijiao.factory;

import com.github.paohaijiao.config.JQuickServerConfig;
import com.github.paohaijiao.protocol.JQuickProtocolFactory;
import com.github.paohaijiao.server.JQuickThriftServer;
import com.github.paohaijiao.transport.JQuickTransportFactory;

/**
 * 服务器工厂接口
 */
public interface JQuickServerFactory {

    /**
     * 创建服务器实例
     */
    JQuickThriftServer create(JQuickServerConfig config, JQuickProtocolFactory protocolFactory, JQuickTransportFactory transportFactory);

    /**
     * 获取服务器类型
     */
    String getServerType();
}
