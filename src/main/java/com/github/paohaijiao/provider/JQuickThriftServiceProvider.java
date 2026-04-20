package com.github.paohaijiao.provider;

import com.github.paohaijiao.config.JQuickThriftServiceConfig;
import com.github.paohaijiao.config.JQuickThriftServiceInfo;
import org.apache.thrift.TBase;
import org.apache.thrift.TServiceClient;

/**
 * Thrift 服务提供者接口
 * 所有 Thrift 服务实现都需要实现此接口
 */
public interface JQuickThriftServiceProvider {

    /**
     * 获取服务名称
     */
    String getServiceName();

    /**
     * 获取服务版本
     */
    default String getVersion() {
        return "1.0.0";
    }

    /**
     * 获取服务描述
     */
    default String getDescription() {
        return "";
    }

    /**
     * 获取服务端处理器
     */
    Object getProcessor();

    /**
     * 获取客户端实现类
     */
    Class<? extends TServiceClient> getClientClass();

    /**
     * 获取请求/响应类型
     */
    Class<? extends TBase<?, ?>> getRequestType();

    /**
     * 获取响应类型
     */
    Class<? extends TBase<?, ?>> getResponseType();

    /**
     * 是否启用
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * 服务配置
     */
    default JQuickThriftServiceConfig getConfig() {
        return new JQuickThriftServiceConfig();
    }
    /**
     * 获取服务信息（用于监控和管理）
     */
    default JQuickThriftServiceInfo getServiceInfo() {
        JQuickThriftServiceInfo info = new JQuickThriftServiceInfo.Builder()
                .serviceName(getServiceName())
                .version(getVersion())
                .description(getDescription())
                .enabled(isEnabled())
                .config(getConfig())
                .build();
        return info;
    }

    /**
     * 健康检查
     */
    default boolean healthCheck() {
        return true;
    }

    /**
     * 初始化回调
     */
    default void onInit() {
    }

    /**
     * 销毁回调
     */
    default void onDestroy() {
    }
}