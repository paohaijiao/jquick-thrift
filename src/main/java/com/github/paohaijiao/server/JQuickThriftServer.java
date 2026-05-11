package com.github.paohaijiao.server;

import com.github.paohaijiao.ano.JQuickThriftService;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thrift RPC服务端
 * 支持通过注解自动注册服务
 */
public class JQuickThriftServer {

    private static final Logger logger = LoggerFactory.getLogger(JQuickThriftServer.class);

    private final int port;
    private final TProtocolFactory protocolFactory;
    private final Map<String, Object> services;
    private TServer server;
    private volatile boolean running;

    private JQuickThriftServer(Builder builder) {
        this.port = builder.port;
        this.protocolFactory = builder.protocolFactory;
        this.services = builder.services;
    }

    /**
     * 启动服务
     */
    public void start() throws TTransportException {
        if (running) {
            logger.warn("Server already running on port {}", port);
            return;
        }
        TServerTransport transport = new TServerSocket(port);
        // 创建处理器
        TServer server = new TThreadPoolServer(
                new TThreadPoolServer.Args(transport)
                        .protocolFactory(protocolFactory)
                        .processor(createProcessor())
        );
        this.server = server;
        this.running = true;
        logger.info("Starting Thrift server on port {}", port);
        // 在新线程中启动服务
        new Thread(() -> {
            server.serve();
        }, "thrift-server-" + port).start();
    }

    /**
     * 停止服务
     */
    public void stop() {
        if (!running) {
            return;
        }
        logger.info("Stopping Thrift server on port {}", port);
        if (server != null && server.isServing()) {
            server.stop();
        }
        running = false;
    }

    /**
     * 创建多服务处理器
     */
    private TProcessor createProcessor() {
        if (services.isEmpty()) {
            throw new IllegalStateException("No service registered");
        }
        Object firstService = services.values().iterator().next();
        return createProcessorForService(firstService);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private TProcessor createProcessorForService(Object service) {
        try {
            Class<?> serviceClass = service.getClass();
            Class<?> iface = serviceClass.getInterfaces()[0];
            Class<?> processorClass = Class.forName(iface.getName() + "$Processor");

            return (TProcessor) processorClass.getConstructor(iface).newInstance(service);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create processor", e);
        }
    }

    /**
     * 获取服务端口
     */
    public int getPort() {
        return port;
    }

    /**
     * 是否运行中
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * 构建器
     */
    public static class Builder {

        private final Map<String, Object> services = new ConcurrentHashMap<>();
        private int port;
        private TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder protocolFactory(TProtocolFactory protocolFactory) {
            this.protocolFactory = protocolFactory;
            return this;
        }

        public Builder registerService(Object service) {
            JQuickThriftService annotation = service.getClass().getAnnotation(JQuickThriftService.class);
            String serviceName = annotation != null ? annotation.name() : service.getClass().getSimpleName();
            services.put(serviceName, service);
            return this;
        }

        public Builder registerService(String name, Object service) {
            services.put(name, service);
            return this;
        }

        public JQuickThriftServer build() {
            if (port <= 0) {
                throw new IllegalArgumentException("Invalid port: " + port);
            }
            if (services.isEmpty()) {
                throw new IllegalStateException("No service registered");
            }
            return new JQuickThriftServer(this);
        }
    }
}
