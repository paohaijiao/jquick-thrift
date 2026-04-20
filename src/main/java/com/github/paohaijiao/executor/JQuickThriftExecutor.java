package com.github.paohaijiao.executor;

import org.apache.thrift.TServiceClient;

/**
 * Thrift 执行器接口 - 接收实际的客户端类型
 */
@FunctionalInterface
public interface JQuickThriftExecutor<T extends TServiceClient, R> {

    R execute(T client) throws Exception;

}
