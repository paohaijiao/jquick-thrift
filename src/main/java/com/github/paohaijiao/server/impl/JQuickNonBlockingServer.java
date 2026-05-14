package com.github.paohaijiao.server.impl;

import com.github.paohaijiao.config.JQuickServerConfig;
import com.github.paohaijiao.console.JConsole;
import com.github.paohaijiao.enums.JQuickServerTypeEnums;
import com.github.paohaijiao.protocol.JQuickProtocolFactory;
import com.github.paohaijiao.server.JQuickThriftServer;
import com.github.paohaijiao.transport.JQuickTransportFactory;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 非阻塞服务器实现（单线程）
 */
public class JQuickNonBlockingServer implements JQuickThriftServer {

    private static final JConsole console = JConsole.getInstance();

    private final JQuickServerConfig config;

    private final JQuickProtocolFactory protocolFactory;

    private final JQuickTransportFactory transportFactory;

    private final TMultiplexedProcessor multiplexedProcessor;

    private final Map<String, Object> services;

    private TServer server;
    private volatile boolean running;

    public JQuickNonBlockingServer(JQuickServerConfig config, JQuickProtocolFactory protocolFactory, JQuickTransportFactory transportFactory) {
        this.config = config;
        this.protocolFactory = protocolFactory;
        this.transportFactory = transportFactory;
        this.multiplexedProcessor = new TMultiplexedProcessor();
        this.services = new ConcurrentHashMap<>();
        console.debug("JQuickNonBlockingServer created on port " + config.getPort());
    }

    @Override
    public void start() throws Exception {
        if (running) {
            console.warn("Server already running on port " + config.getPort());
            return;
        }
        TNonblockingServerTransport transport = new TNonblockingServerSocket(
                config.getPort(),
                config.getAcceptQueueSize()
        );
        TNonblockingServer.Args args = new TNonblockingServer.Args(transport)
                .protocolFactory(t -> protocolFactory.createProtocol(t))
                .processor(multiplexedProcessor);
        this.server = new TNonblockingServer(args);
        this.running = true;
        console.info("Starting NonBlocking Thrift server on port " + config.getPort() +
                " with " + services.size() + " service(s)");
        new Thread(() -> {
            server.serve();
            running = false;
            console.info("NonBlocking Thrift server on port " + config.getPort() + " stopped");
        }, "jquick-nonblocking-server-" + config.getPort()).start();
    }

    @Override
    public void stop() {
        if (!running || server == null) return;
        console.info("Stopping NonBlocking Thrift server on port " + config.getPort());
        if (server.isServing()) {
            server.stop();
        }
        running = false;
    }

    @Override
    public void registerService(String serviceName, Object serviceImpl) {
        TProcessor processor = createProcessorForService(serviceImpl);
        multiplexedProcessor.registerProcessor(serviceName, processor);
        services.put(serviceName, serviceImpl);
        console.info("Registered service: " + serviceName + " -> " + serviceImpl.getClass().getName());
    }

    @Override
    public void unregisterService(String serviceName) {
        services.remove(serviceName);
        console.info("Unregistered service: " + serviceName);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private TProcessor createProcessorForService(Object service) {
        Class<?> serviceClass = service.getClass();
        Class<?>[] interfaces = serviceClass.getInterfaces();
        if (interfaces.length == 0) {
            throw new RuntimeException("Service class " + serviceClass.getName() +
                    " does not implement any interface");
        }
        Class<?> iface = findIfaceInterface(interfaces);
        String baseName = iface.getName();
        if (baseName.endsWith("$Iface")) {
            baseName = baseName.substring(0, baseName.length() - 6);
        }
        String processorClassName = baseName + "$Processor";
        try {
            Class<?> processorClass = Class.forName(processorClassName);
            return (TProcessor) processorClass.getConstructor(iface).newInstance(service);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create processor: " + e.getMessage(), e);
        }
    }

    private Class<?> findIfaceInterface(Class<?>[] interfaces) {
        for (Class<?> anInterface : interfaces) {
            if (anInterface.getName().endsWith("$Iface") ||
                    anInterface.getSimpleName().equals("Iface")) {
                return anInterface;
            }
        }
        return interfaces[0];
    }

    @Override
    public TServer getServer() {
        return server;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public String getServerType() {
        return JQuickServerTypeEnums.nonblocking.getCode();
    }

    @Override
    public Map<String, Object> getRegisteredServices() {
        return new ConcurrentHashMap<>(services);
    }

    @Override
    public int getPort() {
        return config.getPort();
    }
}