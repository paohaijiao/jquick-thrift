package com.github.paohaijiao.builder;

import com.github.paohaijiao.client.JQuickThriftClient;
import com.github.paohaijiao.config.*;
import com.github.paohaijiao.console.JConsole;
import com.github.paohaijiao.discovery.JQuickServiceDiscovery;
import com.github.paohaijiao.loadBalence.JQuickLoadBalancer;
import com.github.paohaijiao.loadBalence.impl.JQuickRoundRobinLoadBalancer;
import com.github.paohaijiao.manager.JQuickDynamicFactory;
import com.github.paohaijiao.server.JQuickThriftServer;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 动态构建器
 * 支持从配置文件构建组件
 */
public class JQuickDynamicBuilder {

    private static final JConsole console = JConsole.getInstance();

    private final JQuickDynamicFactory factory;

    public JQuickDynamicBuilder() {
        this.factory = new JQuickDynamicFactory();
    }

    public JQuickDynamicBuilder(JQuickDynamicFactory factory) {
        this.factory = factory;
    }

    /**
     * 从 Properties 配置文件构建服务端
     */
    public JQuickThriftServer buildServerFromProperties(Properties props) throws Exception {
        JQuickServerConfig serverConfig = parseServerConfig(props);
        JQuickProtocolConfig protocolConfig = parseProtocolConfig(props);
        JQuickTransportConfig transportConfig = parseTransportConfig(props);
        factory.switchProtocol(protocolConfig.getType());
        factory.switchTransport(transportConfig.getTransportType());
        JQuickThriftServer server = factory.createServer(serverConfig);
        String serviceClasses = props.getProperty("server.services");
        if (serviceClasses != null && !serviceClasses.isEmpty()) {
            for (String serviceClass : serviceClasses.split(",")) {
                String trimmed = serviceClass.trim();
                if (!trimmed.isEmpty()) {
                    Object serviceImpl = Class.forName(trimmed).newInstance();
                    String serviceName = extractServiceName(serviceImpl);
                    server.registerService(serviceName, serviceImpl);
                    console.info("Registered service: " + serviceName + " -> " + trimmed);
                }
            }
        }
        return server;
    }

    /**
     * 从 Properties 配置文件构建客户端
     */
    public JQuickThriftClient buildClientFromProperties(Properties props, JQuickServiceDiscovery discovery) throws Exception {
        JQuickClientConfig clientConfig = parseClientConfig(props);
        JQuickConnectionConfig connectionConfig = parseConnectionConfig(props);
        String loadBalancerType = props.getProperty("client.loadBalancer", "roundRobin");
        JQuickLoadBalancer loadBalancer = createLoadBalancer(loadBalancerType);
        return factory.createClient(clientConfig, discovery, loadBalancer, null, connectionConfig);
    }

    /**
     * 从 Map 配置构建服务端
     */
    public JQuickThriftServer buildServerFromMap(Map<String, Object> configMap) throws Exception {
        JQuickServerConfig serverConfig = parseServerConfigFromMap(configMap);
        JQuickProtocolConfig protocolConfig = parseProtocolConfigFromMap(configMap);
        JQuickTransportConfig transportConfig = parseTransportConfigFromMap(configMap);
        factory.switchProtocol(protocolConfig.getType());
        factory.switchTransport(transportConfig.getTransportType());
        JQuickThriftServer server = factory.createServer(serverConfig);
        @SuppressWarnings("unchecked")
        List<Map<String, String>> services = (List<Map<String, String>>) configMap.get("services");
        if (services != null) {
            for (Map<String, String> serviceConfig : services) {
                String serviceName = serviceConfig.get("name");
                String serviceClass = serviceConfig.get("impl");
                if (serviceName != null && serviceClass != null) {
                    Object serviceImpl = Class.forName(serviceClass).newInstance();
                    server.registerService(serviceName, serviceImpl);
                    console.info("Registered service: " + serviceName + " -> " + serviceClass);
                }
            }
        }

        return server;
    }

    /**
     * 从配置文件加载（支持热加载）
     */
    public JQuickThriftServer buildServerFromFile(String configPath) throws Exception {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(configPath)) {
            if (is == null) {
                throw new IllegalArgumentException("Config file not found: " + configPath);
            }
            props.load(is);
        }
        return buildServerFromProperties(props);
    }

    private JQuickServerConfig parseServerConfig(Properties props) {
        JQuickServerConfig config = new JQuickServerConfig();
        config.setPort(Integer.parseInt(props.getProperty("server.port", "9090")));
        config.setServerType(props.getProperty("server.type", "threadpool"));
        config.setMinWorkerThreads(Integer.parseInt(props.getProperty("server.threads.min", "5")));
        config.setMaxWorkerThreads(Integer.parseInt(props.getProperty("server.threads.max", "200")));
        config.setSelectorThreads(Integer.parseInt(props.getProperty("server.selector.threads", "2")));
        return config;
    }

    private JQuickProtocolConfig parseProtocolConfig(Properties props) {
        JQuickProtocolConfig config = new JQuickProtocolConfig();
        config.setType(props.getProperty("protocol.type", "binary"));
        config.setStrictRead(Boolean.parseBoolean(props.getProperty("protocol.strictRead", "true")));
        config.setStrictWrite(Boolean.parseBoolean(props.getProperty("protocol.strictWrite", "true")));
        return config;
    }

    private JQuickTransportConfig parseTransportConfig(Properties props) {
        JQuickTransportConfig config = new JQuickTransportConfig();
        config.setFramed(Boolean.parseBoolean(props.getProperty("transport.framed", "false")));
        config.setTimeout(Integer.parseInt(props.getProperty("transport.timeout", "30000")));
        config.setKeepAlive(Boolean.parseBoolean(props.getProperty("transport.keepAlive", "true")));
        return config;
    }

    private JQuickClientConfig parseClientConfig(Properties props) {
        JQuickClientConfig config = new JQuickClientConfig();
        config.setClientType(props.getProperty("client.type", "pooled"));
        config.setMaxRetries(Integer.parseInt(props.getProperty("client.maxRetries", "3")));
        config.setMultiplexed(Boolean.parseBoolean(props.getProperty("client.multiplexed", "true")));
        config.setLoadBalancer(props.getProperty("client.loadBalancer", "roundRobin"));
        return config;
    }

    private JQuickConnectionConfig parseConnectionConfig(Properties props) {
        JQuickConnectionConfig config = JQuickConnectionConfig.defaultConfig();
        config.setTimeout(Integer.parseInt(props.getProperty("connection.timeout", "30000")));
        config.setMaxConnections(Integer.parseInt(props.getProperty("connection.maxConnections", "10")));
        config.setMaxIdle(Integer.parseInt(props.getProperty("connection.maxIdle", "5")));
        config.setMaxRetries(Integer.parseInt(props.getProperty("connection.maxRetries", "3")));
        config.setFramed(Boolean.parseBoolean(props.getProperty("connection.framed", "false")));
        config.setProtocolType(props.getProperty("connection.protocolType", "binary"));
        return config;
    }

    private JQuickServerConfig parseServerConfigFromMap(Map<String, Object> map) {
        JQuickServerConfig config = new JQuickServerConfig();
        config.setPort((int) map.getOrDefault("port", 9090));
        config.setServerType((String) map.getOrDefault("serverType", "threadpool"));
        config.setMaxWorkerThreads((int) map.getOrDefault("maxWorkerThreads", 200));
        return config;
    }

    private JQuickProtocolConfig parseProtocolConfigFromMap(Map<String, Object> map) {
        JQuickProtocolConfig config = new JQuickProtocolConfig();
        config.setType((String) map.getOrDefault("protocolType", "binary"));
        return config;
    }

    private JQuickTransportConfig parseTransportConfigFromMap(Map<String, Object> map) {
        JQuickTransportConfig config = new JQuickTransportConfig();
        config.setFramed((boolean) map.getOrDefault("framed", false));
        config.setTimeout((int) map.getOrDefault("timeout", 30000));
        return config;
    }

    private JQuickLoadBalancer createLoadBalancer(String type) {
        switch (type) {
            case "roundRobin":
                return new JQuickRoundRobinLoadBalancer();
            case "random":
                return new com.github.paohaijiao.loadBalence.impl.JQuickRandomLoadBalancer();
            case "weighted":
                return new com.github.paohaijiao.loadBalence.impl.JQuickWeightedLoadBalancer();
            default:
                return new JQuickRoundRobinLoadBalancer();
        }
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

    public JQuickDynamicFactory getFactory() {
        return factory;
    }
}
