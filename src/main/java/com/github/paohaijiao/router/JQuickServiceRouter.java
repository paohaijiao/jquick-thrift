package com.github.paohaijiao.router;

import com.github.paohaijiao.constants.JQuickThriftPriorityConstants;
import com.github.paohaijiao.provider.JQuickThriftServiceProvider;
import com.github.paohaijiao.spi.ServiceLoader;
import com.github.paohaijiao.spi.anno.Priority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.paohaijiao.enums.JQuickLoadBalanceStrategy.*;

/**
 * 服务路由器 - 支持多种负载均衡策略
 */
@Priority(JQuickThriftPriorityConstants.INFRASTRUCTURE)
public class JQuickServiceRouter {

    private final Map<String, List<JQuickThriftServiceProvider>> serviceInstances = new HashMap<>();

    private final Map<String, AtomicInteger> roundRobinCounters = new HashMap<>();

    public enum LoadBalanceStrategy {

    }

    public JQuickServiceRouter() {
        loadServices();
    }

    private void loadServices() {
        List<JQuickThriftServiceProvider> providers = ServiceLoader.loadServicesByPriority(JQuickThriftServiceProvider.class);
        for (JQuickThriftServiceProvider provider : providers) {
            String serviceName = provider.getServiceName();
            serviceInstances.computeIfAbsent(serviceName, k -> new ArrayList<>()).add(provider);
            roundRobinCounters.putIfAbsent(serviceName, new AtomicInteger(0));
        }
    }

    /**
     * 获取服务实例
     */
    public JQuickThriftServiceProvider getServiceInstance(String serviceName, LoadBalanceStrategy strategy) {
        List<JQuickThriftServiceProvider> instances = serviceInstances.get(serviceName);
        if (instances == null || instances.isEmpty()) {
            throw new RuntimeException("没有可用的服务实例: " + serviceName);
        }

        if (instances.size() == 1) {
            return instances.get(0);
        }

        switch (strategy) {
            case ROUND_ROBIN:
                return roundRobin(instances);
            case RANDOM:
                return random(instances);
            case WEIGHTED:
                return weighted(instances);
            default:
                return instances.get(0);
        }
    }

    private JQuickThriftServiceProvider roundRobin(List<JQuickThriftServiceProvider> instances) {
        String key = instances.get(0).getServiceName();
        int index = roundRobinCounters.get(key).getAndIncrement() % instances.size();
        return instances.get(index);
    }

    private JQuickThriftServiceProvider random(List<JQuickThriftServiceProvider> instances) {
        int index = ThreadLocalRandom.current().nextInt(instances.size());
        return instances.get(index);
    }

    private JQuickThriftServiceProvider weighted(List<JQuickThriftServiceProvider> instances) {
        // 基于优先级的加权选择
        int totalWeight = instances.stream()
                .mapToInt(p -> getPriorityWeight(p.getClass()))
                .sum();

        int randomWeight = ThreadLocalRandom.current().nextInt(totalWeight);
        int currentWeight = 0;

        for (JQuickThriftServiceProvider instance : instances) {
            currentWeight += getPriorityWeight(instance.getClass());
            if (randomWeight < currentWeight) {
                return instance;
            }
        }

        return instances.get(0);
    }

    private int getPriorityWeight(Class<?> clazz) {
        Priority priority = clazz.getAnnotation(Priority.class);
        if (priority != null) {
            // 优先级数值越小，权重越大
            return Math.max(1, 1000 / priority.value());
        }
        return 10;
    }
}
