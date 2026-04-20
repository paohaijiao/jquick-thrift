package com.github.paohaijiao.builder;

import com.github.paohaijiao.util.JQuickThriftUtil;
import org.apache.thrift.server.*;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;

/**
 * Thrift 服务端构建器
 */
public class JQuickThriftServerBuilder<T> {

    private final T processor;
    private int port = 9090;
    private JQuickThriftUtil.ServerType serverType = JQuickThriftUtil.ServerType.THREAD_POOL;
    private int minWorkerThreads = 5;
    private int maxWorkerThreads = 100;
    private int selectorThreads = 2;

    private JQuickThriftServerBuilder(T processor) {
        this.processor = processor;
    }

    public static <T> JQuickThriftServerBuilder<T> builder(T processor) {
        return new JQuickThriftServerBuilder<>(processor);
    }

    public JQuickThriftServerBuilder<T> port(int port) {
        this.port = port;
        return this;
    }

    public JQuickThriftServerBuilder<T> serverType(JQuickThriftUtil.ServerType serverType) {
        this.serverType = serverType;
        return this;
    }

    public JQuickThriftServerBuilder<T> threadPool(int min, int max) {
        this.minWorkerThreads = min;
        this.maxWorkerThreads = max;
        return this;
    }

    public JQuickThriftServerBuilder<T> selectorThreads(int threads) {
        this.selectorThreads = threads;
        return this;
    }

    @SuppressWarnings("unchecked")
    public TServer build() throws TTransportException {
        TServerTransport serverTransport = new TServerSocket(port);
        TServer server;

        switch (serverType) {
            case SIMPLE:
                TServer.Args simpleArgs = new TServer.Args(serverTransport);
                simpleArgs.processor(processor);
                server = new TSimpleServer(simpleArgs);
                break;

            case THREAD_POOL:
                TThreadPoolServer.Args threadPoolArgs = new TThreadPoolServer.Args(serverTransport);
                threadPoolArgs.processor(processor);
                threadPoolArgs.minWorkerThreads(minWorkerThreads);
                threadPoolArgs.maxWorkerThreads(maxWorkerThreads);
                server = new TThreadPoolServer(threadPoolArgs);
                break;

            case NONBLOCKING:
                TNonblockingServerSocket nonblockingSocket = new TNonblockingServerSocket(port);
                TNonblockingServer.Args nonblockingArgs = new TNonblockingServer.Args(nonblockingSocket);
                nonblockingArgs.processor(processor);
                server = new TNonblockingServer(nonblockingArgs);
                break;

            case HS_HA:
                TNonblockingServerSocket hsHaSocket = new TNonblockingServerSocket(port);
                THsHaServer.Args hsHaArgs = new THsHaServer.Args(hsHaSocket);
                hsHaArgs.processor(processor);
                hsHaArgs.selectorThreads(selectorThreads);
                hsHaArgs.workerThreads(maxWorkerThreads);
                server = new THsHaServer(hsHaArgs);
                break;

            default:
                TServer.Args defaultArgs = new TServer.Args(serverTransport);
                defaultArgs.processor(processor);
                server = new TSimpleServer(defaultArgs);
        }

        return server;
    }
}
