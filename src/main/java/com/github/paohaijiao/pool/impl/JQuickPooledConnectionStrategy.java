package com.github.paohaijiao.pool.impl;

import com.github.paohaijiao.config.JQuickConnectionConfig;
import com.github.paohaijiao.domain.JQuickServiceInstance;
import com.github.paohaijiao.pool.JQuickConnectionStrategy;
import com.github.paohaijiao.spi.anno.Priority;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.layered.TFramedTransport;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 连接池策略
 */
@Priority(100)
public class JQuickPooledConnectionStrategy<T> implements JQuickConnectionStrategy<T> {

    private final Map<String, JQuickThriftConnectionPool<T>> pools = new ConcurrentHashMap<>();

    private final JQuickConnectionConfig defaultConfig;

    public JQuickPooledConnectionStrategy(JQuickConnectionConfig config) {
        this.defaultConfig = config;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getConnection(JQuickServiceInstance instance, JQuickConnectionConfig config) throws Exception {
        String key = instance.getAddress();
        JQuickThriftConnectionPool<T> pool = pools.computeIfAbsent(key, k -> {
            JQuickConnectionConfig poolConfig = config != null ? config : defaultConfig;
            return new JQuickThriftConnectionPool<>(k, poolConfig, this::createConnection);
        });
        return pool.borrowObject();
    }

    @Override
    public void returnConnection(JQuickServiceInstance instance, T connection) {
        String key = instance.getAddress();
        JQuickThriftConnectionPool<T> pool = pools.get(key);
        if (pool != null) {
            pool.returnObject(connection);
        }
    }

    @Override
    public void close() {
        for (JQuickThriftConnectionPool<T> pool : pools.values()) {
            pool.close();
        }
        pools.clear();
    }

    @Override
    public JQuickThriftConnectionPool<T> getConnectionPool() {
        // 返回第一个连接池或null
        return pools.isEmpty() ? null : pools.values().iterator().next();
    }

    /**
     * 创建新的Thrift连接
     */
    @SuppressWarnings("unchecked")
    private T createConnection(String address, JQuickConnectionConfig config) {
        String[] parts = address.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);
        try {

            TTransport transport;
            if (config.isFramed()) {
                transport = new TFramedTransport(new TSocket(host, port, config.getTimeout()));
            } else {
                transport = new TSocket(host, port, config.getTimeout());
            }
            transport.open();
            TProtocol protocol;
            switch (config.getProtocolType()) {
                case "binary":
                    protocol = new TBinaryProtocol(transport);
                    break;
                case "compact":
                    protocol = new org.apache.thrift.protocol.TCompactProtocol(transport);
                    break;
                case "json":
                    protocol = new org.apache.thrift.protocol.TJSONProtocol(transport);
                    break;
                default:
                    protocol = new TBinaryProtocol(transport);
            }

            // 返回协议对象作为连接
            return (T) protocol;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
