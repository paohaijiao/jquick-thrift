package com.github.paohaijiao.client;
import com.github.paohaijiao.config.JQuickConnectionConfig;
import com.github.paohaijiao.domain.JQuickServiceInstance;
import com.github.paohaijiao.loadBalence.JQuickLoadBalancer;
import com.github.paohaijiao.loadBalence.impl.JQuickRoundRobinLoadBalancer;
import com.github.paohaijiao.pool.JQuickConnectionStrategy;
import com.github.paohaijiao.discovery.JQuickServiceDiscovery;
import com.github.paohaijiao.pool.impl.JQuickPooledConnectionStrategy;
import com.github.paohaijiao.spi.ServiceLoader;
import org.apache.thrift.protocol.TProtocol;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Thrift RPC客户端
 * 支持负载均衡、连接池、服务发现等高级特性
 */
public class JQuickThriftClient {

    private final JQuickServiceDiscovery serviceDiscovery;

    private final JQuickLoadBalancer loadBalancer;

    private final JQuickConnectionStrategy<Object> connectionStrategy;

    private final JQuickConnectionConfig connectionConfig;

    private final ConcurrentHashMap<Class<?>, Object> proxyCache;

    private final ConcurrentHashMap<String, List<JQuickServiceInstance>> instanceCache;

    private final AtomicInteger failoverCount;

    public JQuickThriftClient(Builder builder) {
        this.serviceDiscovery = builder.serviceDiscovery;
        this.loadBalancer = builder.loadBalancer;
        this.connectionStrategy = builder.connectionStrategy != null ? (JQuickConnectionStrategy<Object>) builder.connectionStrategy : null;
        this.connectionConfig = builder.connectionConfig;
        this.proxyCache = new ConcurrentHashMap<>();
        this.instanceCache = new ConcurrentHashMap<>();
        this.failoverCount = new AtomicInteger(0);
        if (serviceDiscovery != null) {
            serviceDiscovery.subscribe("*", (serviceName, instances) -> {
                instanceCache.put(serviceName, instances);
            });
        }
    }

    /**
     * 获取服务代理
     */
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceInterface, String serviceName) {
        return (T) proxyCache.computeIfAbsent(serviceInterface, key ->
                Proxy.newProxyInstance(serviceInterface.getClassLoader(), new Class[]{serviceInterface}, new ThriftInvocationHandler<>(serviceInterface, serviceName))
        );
    }

    /**
     * 关闭客户端
     */
    public void close() {
        if (serviceDiscovery != null) {
            serviceDiscovery.close();
        }
        if (connectionStrategy != null) {
            connectionStrategy.close();
        }
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("failoverCount", failoverCount.get());
        stats.put("proxyCacheSize", proxyCache.size());
        stats.put("instanceCacheSize", instanceCache.size());
        if (connectionStrategy != null && connectionStrategy.getConnectionPool() != null) {
            stats.put("activeConnections", connectionStrategy.getConnectionPool().getActiveCount());
            stats.put("idleConnections", connectionStrategy.getConnectionPool().getIdleCount());
        }
        return stats;
    }

    /**
     * 构建器
     */
    public static class Builder {

        private JQuickServiceDiscovery serviceDiscovery;

        private JQuickLoadBalancer loadBalancer;

        private JQuickConnectionStrategy<?> connectionStrategy;

        private JQuickConnectionConfig connectionConfig = JQuickConnectionConfig.defaultConfig();

        public Builder serviceDiscovery(JQuickServiceDiscovery serviceDiscovery) {
            this.serviceDiscovery = serviceDiscovery;
            return this;
        }

        public Builder loadBalancer(JQuickLoadBalancer loadBalancer) {
            this.loadBalancer = loadBalancer;
            return this;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public Builder connectionStrategy(JQuickConnectionStrategy<?> connectionStrategy) {
            this.connectionStrategy = connectionStrategy;
            return this;
        }

        public Builder connectionConfig(JQuickConnectionConfig connectionConfig) {
            this.connectionConfig = connectionConfig;
            return this;
        }

        @SuppressWarnings("unchecked")
        public JQuickThriftClient build() {
            if (loadBalancer == null) {
                loadBalancer = ServiceLoader.getHighestPriorityService(JQuickLoadBalancer.class)
                        .orElse(new JQuickRoundRobinLoadBalancer());
            }

            if (connectionStrategy == null) {
                connectionStrategy = new JQuickPooledConnectionStrategy<>(connectionConfig);
            }
            return new JQuickThriftClient(this);
        }
    }

    /**
     * Thrift调用处理器
     */
    private class ThriftInvocationHandler<T> implements InvocationHandler {

        private final Class<T> serviceInterface;
        private final String serviceName;

        public ThriftInvocationHandler(Class<T> serviceInterface, String serviceName) {
            this.serviceInterface = serviceInterface;
            this.serviceName = serviceName;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("toString")) {
                return "ThriftProxy[" + serviceName + "]";
            }
            if (method.getName().equals("hashCode")) {
                return hashCode();
            }
            if (method.getName().equals("equals")) {
                return proxy == args[0];
            }
            List<JQuickServiceInstance> instances = getInstances();  // 获取服务实例
            if (instances == null || instances.isEmpty()) {
                throw new RuntimeException("No available service instance for: " + serviceName);
            }
            JQuickServiceInstance instance = loadBalancer.select(instances);// 负载均衡选择实例
            if (instance == null) {
                throw new RuntimeException("Failed to select service instance for: " + serviceName);
            }
            return invokeWithRetry(method, args, instance);// 执行调用（带重试）
        }

        private Object invokeWithRetry(Method method, Object[] args, JQuickServiceInstance instance) throws Throwable {
            int maxRetries = connectionConfig.getMaxRetries();
            Throwable lastException = null;
            JQuickServiceInstance currentInstance = instance;
            for (int i = 0; i <= maxRetries; i++) {
                try {
                    return doInvoke(method, args, currentInstance);
                } catch (Exception e) {
                    lastException = e;
                    if (i < maxRetries) {
                        currentInstance.setHealthy(false);
                        List<JQuickServiceInstance> instances = getInstances();
                        if (instances != null && !instances.isEmpty()) {
                            currentInstance = loadBalancer.select(instances);
                            failoverCount.incrementAndGet();
                        } else {
                            break;
                        }
                    }
                }
            }

            throw lastException;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private Object doInvoke(Method method, Object[] args, JQuickServiceInstance instance) throws Exception {
            // 获取连接（TProtocol）
            Object connection = connectionStrategy.getConnection(instance, connectionConfig);
            try {
                String serviceInterfaceName = serviceInterface.getName();
                String baseName = serviceInterfaceName;
                if (serviceInterfaceName.endsWith("$Iface")) {
                    baseName = serviceInterfaceName.substring(0, serviceInterfaceName.length() - 6);
                }
                String clientClassName = baseName + "$Client";
                System.out.println("Loading client class: " + clientClassName);
                Class<?> clientClass = Class.forName(clientClassName);
                Object client = clientClass.getConstructor(TProtocol.class).newInstance(connection);
                return method.invoke(client, args);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load Thrift client class. " + "Please ensure the Thrift service interface is correctly generated. " + "Expected class: " + e.getMessage(), e);
            } finally {
                if (connectionStrategy != null) {
                    connectionStrategy.returnConnection(instance, connection);
                }
            }
        }

        private List<JQuickServiceInstance> getInstances() {
            List<JQuickServiceInstance> instances = instanceCache.get(serviceName);
            if (instances == null && serviceDiscovery != null) {
                instances = serviceDiscovery.getInstances(serviceName);
                if (instances != null && !instances.isEmpty()) {
                    instanceCache.put(serviceName, new java.util.ArrayList<>(instances));
                }
            }
            if (instances != null && !instances.isEmpty()) {
                instances = instances.stream().filter(JQuickServiceInstance::isHealthy).collect(Collectors.toList());
            }

            return instances;
        }
    }
}