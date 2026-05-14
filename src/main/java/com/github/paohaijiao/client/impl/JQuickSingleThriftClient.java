package com.github.paohaijiao.client.impl;


import com.github.paohaijiao.client.JQuickThriftClient;
import com.github.paohaijiao.config.JQuickClientConfig;
import com.github.paohaijiao.config.JQuickConnectionConfig;
import com.github.paohaijiao.discovery.JQuickServiceDiscovery;
import com.github.paohaijiao.domain.JQuickServiceInstance;
import com.github.paohaijiao.loadBalence.JQuickLoadBalancer;
import com.github.paohaijiao.pool.JQuickConnectionStrategy;
import com.github.paohaijiao.strategy.JQuickSingleConnectionStrategy;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单连接客户端实现（无连接池）
 */
public class JQuickSingleThriftClient implements JQuickThriftClient {

    private final JQuickServiceDiscovery serviceDiscovery;

    private final JQuickLoadBalancer loadBalancer;

    private final JQuickConnectionStrategy<Object> connectionStrategy;

    private final JQuickConnectionConfig connectionConfig;

    private final JQuickClientConfig clientConfig;

    private final ConcurrentHashMap<Class<?>, Object> proxyCache;

    private final AtomicBoolean closed;

    public JQuickSingleThriftClient(JQuickClientConfig clientConfig, JQuickServiceDiscovery serviceDiscovery, JQuickLoadBalancer loadBalancer, JQuickConnectionStrategy<?> connectionStrategy, JQuickConnectionConfig connectionConfig) {
        this.clientConfig = clientConfig;
        this.serviceDiscovery = serviceDiscovery;
        this.loadBalancer = loadBalancer;
        this.connectionStrategy = connectionStrategy != null ?
                (JQuickConnectionStrategy<Object>) connectionStrategy :
                new JQuickSingleConnectionStrategy<>(connectionConfig);
        this.connectionConfig = connectionConfig;
        this.proxyCache = new ConcurrentHashMap<>();
        this.closed = new AtomicBoolean(false);
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
                        new JQuickSingleInvocationHandler<>(serviceInterface, serviceName))
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
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("proxyCacheSize", proxyCache.size());
        stats.put("closed", closed.get());
        return stats;
    }

    @Override
    public String getClientType() {
        return "single";
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Thrift调用处理器
     */
    private class JQuickSingleInvocationHandler<T> implements InvocationHandler {

        private final Class<T> serviceInterface;
        private final String serviceName;

        public JQuickSingleInvocationHandler(Class<T> serviceInterface, String serviceName) {
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

            for (int i = 0; i <= maxRetries; i++) {
                try {
                    return doInvoke(method, args, instance);
                } catch (Exception e) {
                    lastException = e;
                    if (i >= maxRetries) {
                        break;
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

                if (clientConfig.isMultiplexed()) {
                    callProtocol = new TMultiplexedProtocol(originalProtocol, serviceName);
                }

                Object client = clientClass.getConstructor(org.apache.thrift.protocol.TProtocol.class)
                        .newInstance(callProtocol);
                return method.invoke(client, args);

            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load Thrift client class: " + e.getMessage(), e);
            } finally {
                if (connectionStrategy != null) {
                    connectionStrategy.returnConnection(instance, connection);
                }
            }
        }

        private List<JQuickServiceInstance> getInstances() {
            if (serviceDiscovery == null) {
                return null;
            }
            return serviceDiscovery.getInstances(serviceName);
        }
    }
}
