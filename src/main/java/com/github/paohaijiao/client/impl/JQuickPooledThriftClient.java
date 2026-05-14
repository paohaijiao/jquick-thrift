package com.github.paohaijiao.client.impl;

import com.github.paohaijiao.client.JQuickThriftClient;
import com.github.paohaijiao.config.JQuickClientConfig;
import com.github.paohaijiao.config.JQuickConnectionConfig;
import com.github.paohaijiao.discovery.JQuickServiceDiscovery;
import com.github.paohaijiao.domain.JQuickServiceInstance;
import com.github.paohaijiao.enums.JQuickClientTypeEnums;
import com.github.paohaijiao.loadBalence.JQuickLoadBalancer;
import com.github.paohaijiao.loadBalence.impl.JQuickRoundRobinLoadBalancer;
import com.github.paohaijiao.pool.JQuickConnectionStrategy;
import com.github.paohaijiao.pool.impl.JQuickPooledConnectionStrategy;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 连接池客户端实现
 */
public class JQuickPooledThriftClient implements JQuickThriftClient {

    private final JQuickServiceDiscovery serviceDiscovery;

    private final JQuickLoadBalancer loadBalancer;

    private final JQuickConnectionStrategy<Object> connectionStrategy;

    private final JQuickConnectionConfig connectionConfig;

    private final JQuickClientConfig clientConfig;

    private final ConcurrentHashMap<Class<?>, Object> proxyCache;

    private final ConcurrentHashMap<String, List<JQuickServiceInstance>> instanceCache;

    private final AtomicInteger failoverCount;

    private final AtomicBoolean closed;

    public JQuickPooledThriftClient(JQuickClientConfig clientConfig, JQuickServiceDiscovery serviceDiscovery, JQuickLoadBalancer loadBalancer, JQuickConnectionStrategy<?> connectionStrategy, JQuickConnectionConfig connectionConfig) {
        this.clientConfig = clientConfig;
        this.serviceDiscovery = serviceDiscovery;
        this.loadBalancer = loadBalancer != null ? loadBalancer : new JQuickRoundRobinLoadBalancer();
        this.connectionStrategy = connectionStrategy != null ? (JQuickConnectionStrategy<Object>) connectionStrategy : new JQuickPooledConnectionStrategy<>(connectionConfig);
        this.connectionConfig = connectionConfig;
        this.proxyCache = new ConcurrentHashMap<>();
        this.instanceCache = new ConcurrentHashMap<>();
        this.failoverCount = new AtomicInteger(0);
        this.closed = new AtomicBoolean(false);
        if (serviceDiscovery != null) {
            serviceDiscovery.subscribe("*", (serviceName, instances) -> {
                instanceCache.put(serviceName, instances);
            });
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceInterface, String serviceName) {
        if (closed.get()) {
            throw new IllegalStateException("Client already closed");
        }
        return (T) proxyCache.computeIfAbsent(serviceInterface, key ->
                Proxy.newProxyInstance(serviceInterface.getClassLoader(),
                        new Class[]{serviceInterface},
                        new JQuickThriftInvocationHandler<>(serviceInterface, serviceName))
        );
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (serviceDiscovery != null) {
            serviceDiscovery.close();
        }
        if (connectionStrategy != null) {
            connectionStrategy.close();
        }
        proxyCache.clear();
        instanceCache.clear();
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("failoverCount", failoverCount.get());
        stats.put("proxyCacheSize", proxyCache.size());
        stats.put("instanceCacheSize", instanceCache.size());
        stats.put("closed", closed.get());
        if (connectionStrategy != null && connectionStrategy.getConnectionPool() != null) {
            stats.put("activeConnections", connectionStrategy.getConnectionPool().getActiveCount());
            stats.put("idleConnections", connectionStrategy.getConnectionPool().getIdleCount());
        }
        return stats;
    }

    @Override
    public String getClientType() {
        return JQuickClientTypeEnums.pooled.getCode();
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Thrift调用处理器
     */
    private class JQuickThriftInvocationHandler<T> implements InvocationHandler {

        private final Class<T> serviceInterface;

        private final String serviceName;

        public JQuickThriftInvocationHandler(Class<T> serviceInterface, String serviceName) {
            this.serviceInterface = serviceInterface;
            this.serviceName = serviceName;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("toString")) {
                return "JQuickThriftProxy[" + serviceName + "]";
            }
            if (method.getName().equals("hashCode")) {
                return hashCode();
            }
            if (method.getName().equals("equals")) {
                return proxy == args[0];
            }

            List<JQuickServiceInstance> instances = getInstances();
            if (instances == null || instances.isEmpty()) {
                throw new RuntimeException("No available service instance for: " + serviceName);
            }

            JQuickServiceInstance instance = loadBalancer.select(instances);
            if (instance == null) {
                throw new RuntimeException("Failed to select service instance for: " + serviceName);
            }

            return invokeWithRetry(method, args, instance);
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
            Object connection = connectionStrategy.getConnection(instance, connectionConfig);
            try {
                String serviceInterfaceName = serviceInterface.getName();
                String baseName = serviceInterfaceName;
                if (serviceInterfaceName.endsWith("$Iface")) {
                    baseName = serviceInterfaceName.substring(0, serviceInterfaceName.length() - 6);
                }
                String clientClassName = baseName + "$Client";
                Class<?> clientClass = Class.forName(clientClassName);
                TProtocol originalProtocol = (TProtocol) connection;
                TProtocol callProtocol = originalProtocol;
                // 如果启用多路复用，包装协议
                if (clientConfig.isMultiplexed()) {
                    callProtocol = new TMultiplexedProtocol(originalProtocol, serviceName);
                }
                Object client = clientClass.getConstructor(org.apache.thrift.protocol.TProtocol.class).newInstance(callProtocol);
                return method.invoke(client, args);

            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load Thrift client class. " +
                        "Expected class: " + e.getMessage(), e);
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
                instances = instances.stream()
                        .filter(JQuickServiceInstance::isHealthy)
                        .collect(Collectors.toList());
            }
            return instances;
        }
    }
}
