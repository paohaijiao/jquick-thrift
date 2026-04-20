package com.github.paohaijiao.client;

import org.apache.thrift.transport.TTransport;

import static com.github.paohaijiao.util.JQuickThriftUtil.closeQuietly;

public class JQuickThriftClient<T> implements AutoCloseable {

    private final T client;

    private final TTransport transport;

    public JQuickThriftClient(T client, TTransport transport) {
        this.client = client;
        this.transport = transport;
    }

    public T getClient() {
        return client;
    }

    public TTransport getTransport() {
        return transport;
    }

    @Override
    public void close() {
        closeQuietly(transport);
    }
}
