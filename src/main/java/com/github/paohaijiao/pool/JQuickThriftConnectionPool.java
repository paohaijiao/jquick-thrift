package com.github.paohaijiao.pool;

import com.github.paohaijiao.config.JQuickGenericObjectPoolConfig;
import com.github.paohaijiao.enums.JQuickProtocolType;
import com.github.paohaijiao.enums.JQuickTransportType;
import com.github.paohaijiao.factory.JQuickThriftClientPoolFactory;
import com.github.paohaijiao.util.JQuickThriftUtil;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.concurrent.ConcurrentHashMap;

public class JQuickThriftConnectionPool<T> implements AutoCloseable {

    private final GenericObjectPool<JQuickThriftUtil.ThriftClient<T>> pool;

    private final ConcurrentHashMap<String, GenericObjectPool<JQuickThriftUtil.ThriftClient<T>>> poolMap = new ConcurrentHashMap<>();

    public JQuickThriftConnectionPool(Class<T> clientClass, String host, int port, JQuickProtocolType protocolType, JQuickTransportType transportType, GenericObjectPoolConfig config) {
        JQuickThriftClientPoolFactory<T> factory = new JQuickThriftClientPoolFactory<>(clientClass, host, port, protocolType, transportType);
        this.pool = new GenericObjectPool<>(factory, config);
    }

    /**
     * 获取连接
     */
    public JQuickThriftUtil.ThriftClient<T> borrowClient() throws Exception {
        return pool.borrowObject();
    }

    /**
     * 归还连接
     */
    public void returnClient(JQuickThriftUtil.ThriftClient<T> client) {
        if (client != null) {
            pool.returnObject(client);
        }
    }

    /**
     * 执行操作并自动归还连接
     */
    public <R> R execute(JQuickThriftUtil.ThriftCallable<T, R> callable) throws Exception {
        JQuickThriftUtil.ThriftClient<T> client = null;
        try {
            client = borrowClient();
            return callable.call(client.getClient());
        } finally {
            if (client != null) {
                returnClient(client);
            }
        }
    }

    /**
     * 获取连接池状态
     */
    public String getPoolStats() {
        return String.format("Active: %d, Idle: %d, Total: %d", pool.getNumActive(), pool.getNumIdle(), pool.getNumIdle() + pool.getNumActive());
    }

    @Override
    public void close() {
        if (pool != null) {
            pool.close();
        }
    }

    /**
     * 创建默认配置
     */
    public static JQuickGenericObjectPoolConfig createDefaultConfig() {
        JQuickGenericObjectPoolConfig config = new JQuickGenericObjectPoolConfig();
        config.setMaxTotal(50);           // 最大连接数
        config.setMaxIdle(20);            // 最大空闲连接
        config.setMinIdle(5);             // 最小空闲连接
        config.setTestOnBorrow(true);     // 借用时测试
        config.setTestOnReturn(true);     // 归还时测试
        config.setTestWhileIdle(true);    // 空闲时测试
        config.setMinEvictableIdleTimeMillis(60000);  // 空闲连接回收时间
        config.setTimeBetweenEvictionRunsMillis(30000); // 检查间隔
        return config;
    }
}
