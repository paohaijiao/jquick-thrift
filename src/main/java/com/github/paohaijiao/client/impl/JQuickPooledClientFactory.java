package com.github.paohaijiao.client.impl;

import com.github.paohaijiao.client.JQuickClientFactory;
import com.github.paohaijiao.client.JQuickThriftClient;
import com.github.paohaijiao.config.JQuickClientConfig;
import com.github.paohaijiao.config.JQuickConnectionConfig;
import com.github.paohaijiao.discovery.JQuickServiceDiscovery;
import com.github.paohaijiao.loadBalence.JQuickLoadBalancer;
import com.github.paohaijiao.pool.JQuickConnectionStrategy;

/**
 * 连接池客户端工厂
 */
public class JQuickPooledClientFactory implements JQuickClientFactory {

    @Override
    public JQuickThriftClient create(JQuickClientConfig config, JQuickServiceDiscovery discovery, JQuickLoadBalancer loadBalancer, JQuickConnectionStrategy<?> connectionStrategy, JQuickConnectionConfig connectionConfig) {
        return new JQuickPooledThriftClient(config, discovery, loadBalancer, connectionStrategy, connectionConfig);
    }

    @Override
    public String getClientType() {
        return "pooled";
    }
}
