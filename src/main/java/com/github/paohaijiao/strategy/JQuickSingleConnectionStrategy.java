package com.github.paohaijiao.strategy;

import com.github.paohaijiao.config.JQuickConnectionConfig;
import com.github.paohaijiao.domain.JQuickServiceInstance;
import com.github.paohaijiao.pool.JQuickConnectionStrategy;
import com.github.paohaijiao.pool.impl.JQuickThriftConnectionPool;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.layered.TFramedTransport;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单连接策略（无连接池）
 */
public class JQuickSingleConnectionStrategy<T> implements JQuickConnectionStrategy<T> {

    private final Map<String, T> connections = new ConcurrentHashMap<>();

    private final JQuickConnectionConfig defaultConfig;

    public JQuickSingleConnectionStrategy(JQuickConnectionConfig config) {
        this.defaultConfig = config;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getConnection(JQuickServiceInstance instance, JQuickConnectionConfig config) throws Exception {
        String key = instance.getAddress();
        return connections.computeIfAbsent(key, k -> {
            try {
                return (T) createConnection(instance, config != null ? config : defaultConfig);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create connection: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public void returnConnection(JQuickServiceInstance instance, T connection) {
        // 单连接策略不归还，保持连接
    }

    @Override
    public void close() {
        for (T conn : connections.values()) {
            if (conn instanceof TProtocol) {
                ((TProtocol) conn).getTransport().close();
            }
        }
        connections.clear();
    }

    @Override
    public JQuickThriftConnectionPool<T> getConnectionPool() {
        return null;
    }

    private TProtocol createConnection(JQuickServiceInstance instance, JQuickConnectionConfig config) throws Exception {
        String[] parts = instance.getAddress().split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);
        TTransport transport;
        if (config.isFramed()) {
            transport = new TFramedTransport(new TSocket(host, port, config.getTimeout()));
        } else {
            transport = new TSocket(host, port, config.getTimeout());
        }
        transport.open();
        switch (config.getProtocolType()) {
            case "binary":
                return new TBinaryProtocol(transport);
            case "compact":
                return new org.apache.thrift.protocol.TCompactProtocol(transport);
            case "json":
                return new org.apache.thrift.protocol.TJSONProtocol(transport);
            default:
                return new TBinaryProtocol(transport);
        }
    }
}
