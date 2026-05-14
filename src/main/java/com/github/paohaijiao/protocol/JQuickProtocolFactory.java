package com.github.paohaijiao.protocol;

import com.github.paohaijiao.config.JQuickProtocolConfig;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;

public interface JQuickProtocolFactory {
    /**
     * 创建协议实例
     */
    TProtocol createProtocol(TTransport transport);

    /**
     * 获取协议类型名称
     */
    String getProtocolType();

    /**
     * 获取协议配置
     */
    JQuickProtocolConfig getConfig();
}
