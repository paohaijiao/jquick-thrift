package com.github.paohaijiao.callable;

/**
 * 客户端调用接口 - 修正：接收 T 类型而非 ThriftClient<T>
 */
@FunctionalInterface
public interface JQuickThriftCallable<T, R> {
    R call(T client) throws Exception;
}
