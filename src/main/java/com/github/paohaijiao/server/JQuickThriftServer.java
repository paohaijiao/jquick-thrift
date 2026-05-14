package com.github.paohaijiao.server;

import org.apache.thrift.server.TServer;

/**
 * Thrift 服务器接口
 */
public interface JQuickThriftServer {

    /**
     * 启动服务
     */
    void start() throws Exception;

    /**
     * 停止服务
     */
    void stop();

    /**
     * 注册服务
     */
    void registerService(String serviceName, Object serviceImpl);

    /**
     * 批量注册服务
     */
    default void registerServices(java.util.Map<String, Object> services) {
        services.forEach(this::registerService);
    }

    /**
     * 注销服务
     */
    void unregisterService(String serviceName);

    /**
     * 获取服务器实例
     */
    TServer getServer();

    /**
     * 是否运行中
     */
    boolean isRunning();

    /**
     * 获取服务器类型
     */
    String getServerType();

    /**
     * 获取已注册的服务
     */
    java.util.Map<String, Object> getRegisteredServices();

    /**
     * 获取端口
     */
    int getPort();
}
