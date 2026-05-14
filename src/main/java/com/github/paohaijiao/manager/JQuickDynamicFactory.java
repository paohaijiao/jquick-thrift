package com.github.paohaijiao.manager;

import com.github.paohaijiao.client.JQuickClientFactory;
import com.github.paohaijiao.client.JQuickThriftClient;
import com.github.paohaijiao.client.impl.JQuickPooledClientFactory;
import com.github.paohaijiao.client.impl.JQuickSingleClientFactory;
import com.github.paohaijiao.config.*;
import com.github.paohaijiao.console.JConsole;
import com.github.paohaijiao.discovery.JQuickServiceDiscovery;
import com.github.paohaijiao.factory.JQuickServerFactory;
import com.github.paohaijiao.factory.impl.JQuickHsHaServerFactory;
import com.github.paohaijiao.factory.impl.JQuickNonBlockingServerFactory;
import com.github.paohaijiao.factory.impl.JQuickThreadPoolServerFactory;
import com.github.paohaijiao.factory.impl.JQuickThreadedSelectorServerFactory;
import com.github.paohaijiao.loadBalence.JQuickLoadBalancer;
import com.github.paohaijiao.loadBalence.impl.JQuickRandomLoadBalancer;
import com.github.paohaijiao.loadBalence.impl.JQuickRoundRobinLoadBalancer;
import com.github.paohaijiao.loadBalence.impl.JQuickWeightedLoadBalancer;
import com.github.paohaijiao.pool.JQuickConnectionStrategy;
import com.github.paohaijiao.protocol.JQuickBinaryProtocolFactory;
import com.github.paohaijiao.protocol.JQuickCompactProtocolFactory;
import com.github.paohaijiao.protocol.JQuickJsonProtocolFactory;
import com.github.paohaijiao.protocol.JQuickProtocolFactory;
import com.github.paohaijiao.server.JQuickThriftServer;
import com.github.paohaijiao.transport.JQuickFramedTransportFactory;
import com.github.paohaijiao.transport.JQuickStandardTransportFactory;
import com.github.paohaijiao.transport.JQuickTransportFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thrift 动态工厂管理器
 * 支持运行时动态切换协议、传输层、服务器类型
 */
public class JQuickDynamicFactory {

    private static final JConsole console = JConsole.getInstance();

    private final Map<String, JQuickProtocolFactory> protocolFactories = new ConcurrentHashMap<>();

    private final Map<String, JQuickTransportFactory> transportFactories = new ConcurrentHashMap<>();

    private final Map<String, JQuickServerFactory> serverFactories = new ConcurrentHashMap<>();

    private final Map<String, JQuickClientFactory> clientFactories = new ConcurrentHashMap<>();

    private volatile JQuickProtocolConfig activeProtocolConfig;

    private volatile JQuickTransportConfig activeTransportConfig;

    private volatile JQuickServerConfig activeServerConfig;

    private volatile JQuickClientConfig activeClientConfig;

    private volatile JQuickThriftServer activeServer;

    private volatile JQuickThriftClient activeClient;

    public JQuickDynamicFactory() {
        initDefaultFactories();
    }

    private void initDefaultFactories() {
        protocolFactories.put("binary", new JQuickBinaryProtocolFactory(JQuickProtocolConfig.binary()));
        protocolFactories.put("compact", new JQuickCompactProtocolFactory(JQuickProtocolConfig.compact()));
        protocolFactories.put("json", new JQuickJsonProtocolFactory(JQuickProtocolConfig.json()));
        transportFactories.put("standard", new JQuickStandardTransportFactory(JQuickTransportConfig.standard()));
        transportFactories.put("framed", new JQuickFramedTransportFactory(JQuickTransportConfig.framed()));
        serverFactories.put("threadpool", new JQuickThreadPoolServerFactory());
        serverFactories.put("nonblocking", new JQuickNonBlockingServerFactory());
        serverFactories.put("hsha", new JQuickHsHaServerFactory());
        serverFactories.put("selector", new JQuickThreadedSelectorServerFactory());
        clientFactories.put("pooled", new JQuickPooledClientFactory());
        clientFactories.put("single", new JQuickSingleClientFactory());
        this.activeProtocolConfig = JQuickProtocolConfig.binary();
        this.activeTransportConfig = JQuickTransportConfig.standard();
        this.activeServerConfig = JQuickServerConfig.threadPool(9090);
        this.activeClientConfig = JQuickClientConfig.pooled();
        console.info("JQuickDynamicFactory initialized with " + protocolFactories.size() + " protocols, " + transportFactories.size() + " transports, " + serverFactories.size() + " servers, " + clientFactories.size() + " clients");
    }

    /**
     * 动态切换服务器类型
     */
    public void switchServer(String serverType, JQuickServerConfig config) throws Exception {
        JQuickServerFactory factory = serverFactories.get(serverType);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown server type: " + serverType + ". Available: " + serverFactories.keySet());
        }
        console.info("Switching server from " + (activeServer != null ? activeServer.getServerType() : "null") + " to " + serverType);
        if (activeServer != null && activeServer.isRunning()) {
            activeServer.stop();
        }
        this.activeServerConfig = config;
        Map<String, Object> registeredServices = null;
        if (activeServer != null) {
            registeredServices = activeServer.getRegisteredServices();
        }
        this.activeServer = factory.create(config, getProtocolFactory(activeProtocolConfig.getType()), getTransportFactory(activeTransportConfig.getTransportType()));
        if (registeredServices != null && !registeredServices.isEmpty()) {
            for (Map.Entry<String, Object> entry : registeredServices.entrySet()) {
                activeServer.registerService(entry.getKey(), entry.getValue());
            }
        }
        if (activeServer != null) {
            activeServer.start();
            console.info("Server switched to " + serverType + " on port " + config.getPort());
        }
    }

    /**
     * 动态切换协议
     */
    public void switchProtocol(String protocolType) throws Exception {
        JQuickProtocolFactory factory = protocolFactories.get(protocolType);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown protocol type: " + protocolType + ". Available: " + protocolFactories.keySet());
        }
        console.info("Switching protocol from " + activeProtocolConfig.getType() + " to " + protocolType);
        this.activeProtocolConfig = factory.getConfig();
        if (activeServer != null && activeServer.isRunning()) {
            boolean wasRunning = activeServer.isRunning();
            String serverType = activeServer.getServerType();
            Map<String, Object> services = activeServer.getRegisteredServices();
            int port = activeServer.getPort();
            activeServer.stop();
            JQuickServerFactory serverFactory = serverFactories.get(serverType);
            if (serverFactory != null) {
                JQuickServerConfig newConfig = new JQuickServerConfig();
                newConfig.setPort(port);
                newConfig.setServerType(serverType);
                this.activeServer = serverFactory.create(newConfig, factory, getTransportFactory(activeTransportConfig.getTransportType()));
                for (Map.Entry<String, Object> entry : services.entrySet()) {
                    activeServer.registerService(entry.getKey(), entry.getValue());
                }
                if (wasRunning) {
                    activeServer.start();
                }
            }
        }
        console.info("Protocol switched to " + protocolType);
    }

    /**
     * 动态切换传输层
     */
    public void switchTransport(String transportType) throws Exception {
        JQuickTransportFactory factory = transportFactories.get(transportType);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown transport type: " + transportType + ". Available: " + transportFactories.keySet());
        }
        console.info("Switching transport from " + activeTransportConfig.getTransportType() + " to " + transportType);
        this.activeTransportConfig = factory.getConfig();
        if (activeServer != null && activeServer.isRunning()) {
            boolean wasRunning = activeServer.isRunning();
            String serverType = activeServer.getServerType();
            Map<String, Object> services = activeServer.getRegisteredServices();
            int port = activeServer.getPort();
            activeServer.stop();
            JQuickServerFactory serverFactory = serverFactories.get(serverType);
            if (serverFactory != null) {
                JQuickServerConfig newConfig = new JQuickServerConfig();
                newConfig.setPort(port);
                newConfig.setServerType(serverType);
                this.activeServer = serverFactory.create(newConfig, getProtocolFactory(activeProtocolConfig.getType()), factory);
                for (Map.Entry<String, Object> entry : services.entrySet()) {
                    activeServer.registerService(entry.getKey(), entry.getValue());
                }
                if (wasRunning) {
                    activeServer.start();
                }
            }
        }
        console.info("Transport switched to " + transportType);
    }

    /**
     * 动态切换负载均衡策略
     */
    public JQuickLoadBalancer switchLoadBalancer(String loadBalancerType) {
        console.info("Switching load balancer to " + loadBalancerType);
        switch (loadBalancerType) {
            case "roundRobin":
                return new JQuickRoundRobinLoadBalancer();
            case "random":
                return new JQuickRandomLoadBalancer();
            case "weighted":
                return new JQuickWeightedLoadBalancer();
            default:
                throw new IllegalArgumentException("Unknown load balancer: " + loadBalancerType);
        }
    }

    /**
     * 创建服务端
     */
    public JQuickThriftServer createServer(JQuickServerConfig config) {
        JQuickServerFactory factory = serverFactories.get(config.getServerType());
        if (factory == null) {
            throw new IllegalArgumentException("Unknown server type: " + config.getServerType());
        }
        this.activeServerConfig = config;
        this.activeServer = factory.create(config, getProtocolFactory(activeProtocolConfig.getType()), getTransportFactory(activeTransportConfig.getTransportType()));
        return activeServer;
    }

    /**
     * 创建客户端
     */
    public JQuickThriftClient createClient(JQuickClientConfig config, JQuickServiceDiscovery discovery, JQuickLoadBalancer loadBalancer, JQuickConnectionStrategy<?> connectionStrategy, JQuickConnectionConfig connectionConfig) {
        JQuickClientFactory factory = clientFactories.get(config.getClientType());
        if (factory == null) {
            throw new IllegalArgumentException("Unknown client type: " + config.getClientType());
        }
        this.activeClientConfig = config;
        this.activeClient = factory.create(config, discovery, loadBalancer, connectionStrategy, connectionConfig);
        return activeClient;
    }

    /**
     * 获取当前统计信息
     */
    public Map<String, Object> getCurrentStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("activeProtocol", activeProtocolConfig.getType());
        stats.put("activeTransport", activeTransportConfig.getTransportType());
        stats.put("activeServerType", activeServer != null ? activeServer.getServerType() : "none");
        stats.put("activeClientType", activeClient != null ? activeClient.getClientType() : "none");
        stats.put("serverRunning", activeServer != null && activeServer.isRunning());
        if (activeServer != null) {
            stats.put("serverPort", activeServer.getPort());
            stats.put("registeredServices", activeServer.getRegisteredServices().keySet());
        }
        if (activeClient != null && !activeClient.isClosed()) {
            stats.put("clientStats", activeClient.getStats());
        }
        return stats;
    }

    public JQuickProtocolFactory getProtocolFactory(String type) {
        JQuickProtocolFactory factory = protocolFactories.get(type);
        if (factory == null) {
            console.warn("Protocol factory not found for type: " + type + ", using binary");
            return protocolFactories.get("binary");
        }
        return factory;
    }

    public JQuickTransportFactory getTransportFactory(String type) {
        JQuickTransportFactory factory = transportFactories.get(type);
        if (factory == null) {
            console.warn("Transport factory not found for type: " + type + ", using standard");
            return transportFactories.get("standard");
        }
        return factory;
    }

    public JQuickThriftServer getActiveServer() {
        return activeServer;
    }

    public JQuickThriftClient getActiveClient() {
        return activeClient;
    }

    public JQuickProtocolConfig getActiveProtocolConfig() {
        return activeProtocolConfig;
    }

    public JQuickTransportConfig getActiveTransportConfig() {
        return activeTransportConfig;
    }

    public void registerProtocolFactory(String type, JQuickProtocolFactory factory) {
        protocolFactories.put(type, factory);
        console.info("Registered protocol factory: " + type);
    }

    public void registerTransportFactory(String type, JQuickTransportFactory factory) {
        transportFactories.put(type, factory);
        console.info("Registered transport factory: " + type);
    }

    public void registerServerFactory(String type, JQuickServerFactory factory) {
        serverFactories.put(type, factory);
        console.info("Registered server factory: " + type);
    }

    public void registerClientFactory(String type, JQuickClientFactory factory) {
        clientFactories.put(type, factory);
        console.info("Registered client factory: " + type);
    }
}
