package com.github.paohaijiao.pool;

import com.github.paohaijiao.config.JQuickConnectionConfig;
import com.github.paohaijiao.domain.JQuickServiceInstance;
import com.github.paohaijiao.pool.impl.JQuickThriftConnectionPool;
import com.github.paohaijiao.spi.anno.Priority;

/**
 * 连接策略SPI接口
 * 支持不同的连接管理策略
 */
@Priority(200)
public interface JQuickConnectionStrategy<T> {

    /**
     * 获取或创建连接
     */
    T getConnection(JQuickServiceInstance instance, JQuickConnectionConfig config) throws Exception;

    /**
     * 归还连接
     */
    void returnConnection(JQuickServiceInstance instance, T connection);

    /**
     * 关闭所有连接
     */
    void close();

    /**
     * 获取连接池（如果有）
     */
    default JQuickThriftConnectionPool<T> getConnectionPool() {
        return null;
    }
}
