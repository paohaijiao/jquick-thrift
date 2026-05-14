package com.github.paohaijiao.client;


import java.util.Map;

/**
 * Thrift 客户端接口
 */
public interface JQuickThriftClient {

    /**
     * 获取服务代理
     */
    <T> T getService(Class<T> serviceInterface, String serviceName);

    /**
     * 关闭客户端
     */
    void close();

    /**
     * 获取客户端统计信息
     */
    Map<String, Object> getStats();

    /**
     * 获取客户端类型
     */
    String getClientType();

    /**
     * 是否已关闭
     */
    boolean isClosed();
}
