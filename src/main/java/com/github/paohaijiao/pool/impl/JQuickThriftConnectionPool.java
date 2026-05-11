package com.github.paohaijiao.pool.impl;

import com.github.paohaijiao.config.JQuickConnectionConfig;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.function.BiFunction;

/**
 * Thrift连接池
 */
public class JQuickThriftConnectionPool<T> {

    private final GenericObjectPool<T> pool;
    private final String address;

    public JQuickThriftConnectionPool(String address, JQuickConnectionConfig config, BiFunction<String, JQuickConnectionConfig, T> creator) {
        this.address = address;
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();

        poolConfig.setMaxTotal(config.getMaxConnections());
        poolConfig.setMaxIdle(config.getMaxIdle());
        poolConfig.setMinIdle(config.getMinIdle());
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTimeMillis(config.getEvictableIdleTimeMillis());
        this.pool = new GenericObjectPool<>(new BasePooledObjectFactory<T>() {
            @Override
            public T create() throws Exception {
                return creator.apply(address, config);
            }

            @Override
            public PooledObject<T> wrap(T obj) {
                return new DefaultPooledObject<>(obj);
            }

            @Override
            public void destroyObject(PooledObject<T> p) throws Exception {
                // 关闭连接逻辑
                T obj = p.getObject();
                if (obj instanceof org.apache.thrift.protocol.TProtocol) {
                    org.apache.thrift.protocol.TProtocol protocol = (org.apache.thrift.protocol.TProtocol) obj;
                    protocol.getTransport().close();
                }
            }

            @Override
            public boolean validateObject(PooledObject<T> p) {
                T obj = p.getObject();
                if (obj instanceof org.apache.thrift.protocol.TProtocol) {
                    org.apache.thrift.protocol.TProtocol protocol = (org.apache.thrift.protocol.TProtocol) obj;
                    return protocol.getTransport().isOpen();
                }
                return false;
            }
        }, poolConfig);
    }

    public T borrowObject() throws Exception {
        return pool.borrowObject();
    }

    public void returnObject(T obj) {
        if (obj != null) {
            pool.returnObject(obj);
        }
    }

    public void close() {
        pool.close();
    }

    public int getActiveCount() {
        return pool.getNumActive();
    }

    public int getIdleCount() {
        return pool.getNumIdle();
    }
}
