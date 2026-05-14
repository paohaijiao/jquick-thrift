package com.github.paohaijiao.server.impl;

import com.github.paohaijiao.config.JQuickServerConfig;
import com.github.paohaijiao.console.JConsole;
import com.github.paohaijiao.enums.JQuickServerTypeEnums;
import com.github.paohaijiao.protocol.JQuickProtocolFactory;
import com.github.paohaijiao.server.JQuickThriftServer;
import com.github.paohaijiao.transport.JQuickTransportFactory;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerTransport;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 线程池服务器实现
 */
public class JQuickThreadPoolServer implements JQuickThriftServer {

    private static final JConsole console = JConsole.getInstance();

    private final JQuickServerConfig config;

    private final JQuickProtocolFactory protocolFactory;

    private final JQuickTransportFactory transportFactory;

    private final TMultiplexedProcessor multiplexedProcessor;

    private final Map<String, Object> services;

    private final Map<String, TProcessor> processorCache;

    private TServer server;

    private volatile boolean running;

    public JQuickThreadPoolServer(JQuickServerConfig config, JQuickProtocolFactory protocolFactory, JQuickTransportFactory transportFactory) {
        this.config = config;
        this.protocolFactory = protocolFactory;
        this.transportFactory = transportFactory;
        this.multiplexedProcessor = new TMultiplexedProcessor();
        this.services = new ConcurrentHashMap<>();
        this.processorCache = new ConcurrentHashMap<>();
    }

    @Override
    public void start() throws Exception {
        if (running) {
            console.warn("Server already running on port " + config.getPort());
            return;
        }
        TServerTransport transport = transportFactory.createServerTransport(config.getPort(), transportFactory.getConfig());
        TThreadPoolServer.Args args = new TThreadPoolServer.Args(transport)
                .protocolFactory(t -> protocolFactory.createProtocol(t))
                .processor(multiplexedProcessor)
                .minWorkerThreads(config.getMinWorkerThreads())
                .maxWorkerThreads(config.getMaxWorkerThreads())
                .stopTimeoutVal(config.getStopTimeoutMs())
                .stopTimeoutUnit(TimeUnit.MILLISECONDS);
        this.server = new TThreadPoolServer(args);
        this.running = true;
        console.info("Starting ThreadPool Thrift server on port " + config.getPort() + " with " + services.size() + " service(s): " + services.keySet());
        new Thread(() -> {
            server.serve();
            running = false;
            console.info("Thrift server on port " + config.getPort() + " stopped");
        }, "jquick-thrift-server-" + config.getPort()).start();
    }

    @Override
    public void stop() {
        if (!running || server == null) {
            return;
        }
        console.info("Stopping Thrift server on port " + config.getPort());
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
        processorCache.put(serviceName, processor);
        console.info("Registered service: " + serviceName + " -> " + serviceImpl.getClass().getName());
    }

    @Override
    public void unregisterService(String serviceName) {
        services.remove(serviceName);
        processorCache.remove(serviceName);
        rebuildProcessor();
        console.info("Unregistered service: " + serviceName);
    }

    private void rebuildProcessor() {
        TMultiplexedProcessor newProcessor = new TMultiplexedProcessor();
        for (Map.Entry<String, Object> entry : services.entrySet()) {
            TProcessor processor = processorCache.computeIfAbsent(entry.getKey(), k -> createProcessorForService(entry.getValue()));
            newProcessor.registerProcessor(entry.getKey(), processor);
        }
        console.warn("Service unregistered, server restart required to apply changes");
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
        console.debug("Creating processor for service: " + baseName + ", processor class: " + processorClassName);
        try {
            Class<?> processorClass = Class.forName(processorClassName);
            return (TProcessor) processorClass.getConstructor(iface).newInstance(service);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Processor class not found: " + processorClassName, e);
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
        return JQuickServerTypeEnums.threadpool.getCode();
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
