package com.github.paohaijiao.client.impl;

import com.github.paohaijiao.client.JQuickClientFactory;
import com.github.paohaijiao.client.JQuickThriftClient;
import com.github.paohaijiao.config.JQuickClientConfig;
import com.github.paohaijiao.config.JQuickConnectionConfig;
import com.github.paohaijiao.discovery.JQuickServiceDiscovery;
import com.github.paohaijiao.loadBalance.JQuickLoadBalancer;
import com.github.paohaijiao.pool.JQuickConnectionStrategy;

/**
 * 单连接客户端工厂
 */
public class JQuickSingleClientFactory implements JQuickClientFactory {

    @Override
    public JQuickThriftClient create(JQuickClientConfig config, JQuickServiceDiscovery discovery, JQuickLoadBalancer loadBalancer, JQuickConnectionStrategy<?> connectionStrategy, JQuickConnectionConfig connectionConfig) {
        return new JQuickSingleThriftClient(config, discovery, loadBalancer, connectionStrategy, connectionConfig);
    }

    @Override
    public String getClientType() {
        return "single";
    }
}
