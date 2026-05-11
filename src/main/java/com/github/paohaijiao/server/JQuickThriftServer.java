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
        TProcessor processor = createProcessor();
        TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(transport)
                        .protocolFactory(protocolFactory)
                        .processor(processor)
        );
        this.server = server;
        this.running = true;
        logger.info("Starting Thrift server on port {}", port);
        // 在新线程中启动服务
        new Thread(() -> {server.serve();}, "thrift-server-" + port).start();
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
     * 创建处理器
     * 注意：由于 libthrift 0.16.0 不支持 TMultiplexedProcessor，目前只支持单个服务
     */
    private TProcessor createProcessor() {
        if (services.isEmpty()) {
            throw new IllegalStateException("No service registered");
        }

        if (services.size() > 1) {
            logger.warn("Multiple services registered ({}), but current version only supports single service. " +
                    "Only the first service will be used.", services.size());
            logger.warn("Registered services: {}", services.keySet());
        }

        // 取第一个服务
        Object service = services.values().iterator().next();
        return createProcessorForService(service);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private TProcessor createProcessorForService(Object service) {
        try {
            Class<?> serviceClass = service.getClass();
            Class<?>[] interfaces = serviceClass.getInterfaces();

            if (interfaces.length == 0) {
                throw new RuntimeException("Service class " + serviceClass.getName() +
                        " does not implement any interface. Thrift service must implement the generated Iface interface.");
            }

            // 找到 Thrift 服务接口（通常是 Iface 接口或直接是服务接口）
            Class<?> iface = null;
            for (Class<?> anInterface : interfaces) {
                if (anInterface.getName().endsWith("$Iface") ||
                        anInterface.getSimpleName().equals("Iface")) {
                    iface = anInterface;
                    break;
                }
            }

            if (iface == null) {
                // 如果没有找到 Iface 接口，使用第一个接口
                iface = interfaces[0];
                logger.debug("Using interface: {}", iface.getName());
            }

            String ifaceName = iface.getName();
            // 移除末尾的 $Iface 后缀（如果存在）
            String baseName = ifaceName;
            if (ifaceName.endsWith("$Iface")) {
                baseName = ifaceName.substring(0, ifaceName.length() - 6);
            }

            // 正确的 Processor 类名格式: ServiceName$Processor
            String processorClassName = baseName + "$Processor";
            logger.debug("Looking for processor class: {}", processorClassName);

            Class<?> processorClass = Class.forName(processorClassName);

            // Processor 构造函数签名: (Iface iface)
            return (TProcessor) processorClass.getConstructor(iface).newInstance(service);

        } catch (ClassNotFoundException e) {
            logger.error("Processor class not found. Please ensure Thrift code is generated correctly.", e);
            throw new RuntimeException("Failed to create processor: " + e.getMessage() +
                    ". Make sure the service class implements the correct Thrift Iface interface.", e);
        } catch (Exception e) {
            logger.error("Failed to create processor for service", e);
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
            String serviceName = annotation != null && !annotation.name().isEmpty()
                    ? annotation.name()
                    : extractServiceName(service);
            services.put(serviceName, service);
            logger.debug("Registered service: {} -> {}", serviceName, service.getClass().getName());
            return this;
        }

        public Builder registerService(String name, Object service) {
            services.put(name, service);
            logger.debug("Registered service: {} -> {}", name, service.getClass().getName());
            return this;
        }

        private String extractServiceName(Object service) {
            Class<?>[] interfaces = service.getClass().getInterfaces();
            for (Class<?> iface : interfaces) {
                String ifaceName = iface.getSimpleName();
                if (ifaceName.endsWith("$Iface")) {
                    return ifaceName.substring(0, ifaceName.length() - 6);
                }
                if (ifaceName.endsWith("Iface")) {
                    return ifaceName.substring(0, ifaceName.length() - 5);
                }
            }
            return service.getClass().getSimpleName().replace("Impl", "");
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