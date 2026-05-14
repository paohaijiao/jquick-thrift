package com.github.paohaijiao.transport;

import com.github.paohaijiao.config.JQuickTransportConfig;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransport;

/**
 * Thrift 传输工厂接口
 */
public interface JQuickTransportFactory {

    /**
     * 创建客户端传输层
     */
    TTransport createClientTransport(String host, int port, JQuickTransportConfig config) throws Exception;

    /**
     * 创建服务端传输层
     */
    TServerTransport createServerTransport(int port, JQuickTransportConfig config) throws Exception;

    /**
     * 获取传输类型
     */
    String getTransportType();

    /**
     * 获取传输配置
     */
    JQuickTransportConfig getConfig();
}