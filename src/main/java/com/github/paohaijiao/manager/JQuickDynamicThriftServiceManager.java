package com.github.paohaijiao.manager;

import com.github.paohaijiao.config.JQuickThriftServiceConfig;
import com.github.paohaijiao.config.JQuickThriftServiceInfo;
import com.github.paohaijiao.console.JConsole;
import com.github.paohaijiao.constants.JQuickThriftPriorityConstants;
import com.github.paohaijiao.enums.JLogLevel;
import com.github.paohaijiao.pool.JQuickThriftConnectionPool;
import com.github.paohaijiao.provider.JQuickThriftServiceProvider;
import com.github.paohaijiao.spi.ServiceLoader;
import com.github.paohaijiao.spi.anno.Priority;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.TServiceClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.paohaijiao.enums.JQuickProtocolType.BINARY;
import static com.github.paohaijiao.enums.JQuickTransportType.FRAMED;

/**
 * 动态 Thrift 服务管理器
 * 利用 SPI 自动发现和管理 Thrift 服务
 */
@Priority(JQuickThriftPriorityConstants.CORE_SERVICE)
public class JQuickDynamicThriftServiceManager {

    private static final JQuickDynamicThriftServiceManager INSTANCE = new JQuickDynamicThriftServiceManager();

    private final Map<String, JQuickThriftServiceProvider> serviceProviders = new ConcurrentHashMap<>();
    private final Map<String, Object> processors = new ConcurrentHashMap<>();
    private final Map<String, Class<? extends TServiceClient>> clientClasses = new ConcurrentHashMap<>();
    private final Map<String, JQuickThriftConnectionPool<?>> connectionPools = new ConcurrentHashMap<>();

    private final JConsole console = new JConsole();

    private JQuickDynamicThriftServiceManager() {
        initialize();
    }

    public static JQuickDynamicThriftServiceManager getInstance() {
        return INSTANCE;
    }

    /**
     * 初始化：加载所有 Thrift 服务提供者
     */
    @SuppressWarnings("unchecked")
    private void initialize() {
        console.log(JLogLevel.INFO, "=== 初始化动态 Thrift 服务管理器 ===");

        // 通过 SPI 加载所有服务提供者
        List<JQuickThriftServiceProvider> providers = ServiceLoader.loadServicesByPriority(JQuickThriftServiceProvider.class);

        for (JQuickThriftServiceProvider provider : providers) {
            if (!provider.isEnabled()) {
                console.log(JLogLevel.INFO, "跳过禁用的服务: " + provider.getServiceName());
                continue;
            }

            String serviceName = provider.getServiceName();
            serviceProviders.put(serviceName, provider);
            processors.put(serviceName, provider.getProcessor());
            clientClasses.put(serviceName, provider.getClientClass());
            JQuickThriftServiceConfig config = provider.getConfig();
            if (config.isUsePool()) {
                createConnectionPool(serviceName, provider.getClientClass(), config);
            }

            console.log(JLogLevel.INFO, String.format(
                    "✓ 加载服务: %s (版本: %s, 优先级: %s, 端口: %d)",
                    serviceName, provider.getVersion(),
                    getPriorityLabel(provider.getClass()),
                    config.getPort()
            ));
        }

        console.log(JLogLevel.INFO, "=== 服务加载完成，共 " + serviceProviders.size() + " 个服务 ===");
    }

    /**
     * 创建连接池
     */
    private <T extends TServiceClient> void createConnectionPool(
            String serviceName,
            Class<T> clientClass,
            JQuickThriftServiceConfig config) {

        JQuickThriftConnectionPool<T> pool = new JQuickThriftConnectionPool<>(
                clientClass,
                config.getHost(),
                config.getPort(),
                BINARY,
                FRAMED,
                createPoolConfig(config)
        );

        connectionPools.put(serviceName, pool);
    }

    /**
     * 获取服务客户端（从连接池）
     */
    @SuppressWarnings("unchecked")
    public <T extends TServiceClient> T getClient(String serviceName) {
        JQuickThriftConnectionPool<T> pool = (JQuickThriftConnectionPool<T>) connectionPools.get(serviceName);
        if (pool == null) {
            throw new RuntimeException("未找到服务: " + serviceName);
        }

        try {
            return pool.borrowClient().getClient();
        } catch (Exception e) {
            throw new RuntimeException("获取客户端失败: " + serviceName, e);
        }
    }

    /**
     * 执行服务调用
     */
    public <T extends TServiceClient, R> R execute(String serviceName,
                                                   ThriftExecutor<T, R> executor) {
        JQuickThriftConnectionPool<T> pool = (JQuickThriftConnectionPool<T>) connectionPools.get(serviceName);
        if (pool == null) {
            throw new RuntimeException("未找到服务: " + serviceName);
        }

        try {
            return pool.execute(executor);
        } catch (Exception e) {
            throw new RuntimeException("服务调用失败: " + serviceName, e);
        }
    }

    /**
     * 获取所有可用的服务
     */
    public List<String> getAvailableServices() {
        return new ArrayList<>(serviceProviders.keySet());
    }

    /**
     * 获取服务信息
     */
    public JQuickThriftServiceInfo getServiceInfo(String serviceName) {
        JQuickThriftServiceProvider provider = serviceProviders.get(serviceName);
        if (provider == null) return null;

        JQuickThriftServiceInfo info = new JQuickThriftServiceInfo();
        info.setServiceName(serviceName);
        info.setVersion(provider.getVersion());
        info.setEnabled(provider.isEnabled());
        info.setConfig(provider.getConfig());
        return info;
    }

    /**
     * 打印所有服务信息
     */
    public void printAllServices() {
        console.log(JLogLevel.INFO, "=== 已注册的 Thrift 服务 ===");
        List<JQuickThriftServiceProvider> providers = ServiceLoader.loadServicesByPriority(JQuickThriftServiceProvider.class);

        for (int i = 0; i < providers.size(); i++) {
            JQuickThriftServiceProvider provider = providers.get(i);
            JQuickThriftServiceConfig config = provider.getConfig();
            String info = String.format(
                    "%d. %s (v%s) -> %s:%d [%s]",
                    i + 1,
                    provider.getServiceName(),
                    provider.getVersion(),
                    config.getHost(),
                    config.getPort(),
                    provider.isEnabled() ? "启用" : "禁用"
            );
            console.log(JLogLevel.INFO, info);
        }
    }

    /**
     * 动态重新加载服务
     */
    public void reload() {
        console.log(JLogLevel.INFO, "重新加载 Thrift 服务...");

        // 关闭所有连接池
        connectionPools.values().forEach(pool -> {
            try {
                pool.close();
            } catch (Exception e) {
                console.log(JLogLevel.ERROR, "关闭连接池失败: " + e.getMessage());
            }
        });

        // 清空缓存
        serviceProviders.clear();
        processors.clear();
        clientClasses.clear();
        connectionPools.clear();

        // 重新初始化
        initialize();
    }

    private String getPriorityLabel(Class<?> clazz) {
        com.github.paohaijiao.spi.anno.Priority priority =
                clazz.getAnnotation(com.github.paohaijiao.spi.anno.Priority.class);
        if (priority != null) {
            return String.valueOf(priority.value());
        }
        return "默认";
    }

    private GenericObjectPoolConfig createPoolConfig(JQuickThriftServiceConfig config) {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(config.getMaxPoolSize());
        poolConfig.setMaxIdle(config.getMaxPoolSize() / 2);
        poolConfig.setMinIdle(5);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        return poolConfig;
    }

    /**
     * Thrift 执行器接口
     */
    @FunctionalInterface
    public interface ThriftExecutor<T extends TServiceClient, R> {
        R execute(T client) throws Exception;
    }
}
