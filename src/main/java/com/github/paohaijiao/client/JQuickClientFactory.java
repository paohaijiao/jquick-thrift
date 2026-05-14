package com.github.paohaijiao.client;

import com.github.paohaijiao.config.JQuickClientConfig;
import com.github.paohaijiao.config.JQuickConnectionConfig;
import com.github.paohaijiao.discovery.JQuickServiceDiscovery;
import com.github.paohaijiao.loadBalence.JQuickLoadBalancer;
import com.github.paohaijiao.pool.JQuickConnectionStrategy;

/**
 * 客户端工厂接口
 */
public interface JQuickClientFactory {

    /**
     * 创建客户端实例
     */
    JQuickThriftClient create(JQuickClientConfig config, JQuickServiceDiscovery discovery, JQuickLoadBalancer loadBalancer, JQuickConnectionStrategy<?> connectionStrategy, JQuickConnectionConfig connectionConfig);

    /**
     * 获取客户端类型
     */
    String getClientType();
}
