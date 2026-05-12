package com.github.paohaijiao.discovery.impl;

import com.github.paohaijiao.discovery.JQuickServiceDiscovery;
import com.github.paohaijiao.domain.JQuickServiceInstance;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 内存服务发现实现（用于测试）
 */
public  class JQuickInMemoryServiceDiscovery implements JQuickServiceDiscovery {

    private final Map<String, List<JQuickServiceInstance>> serviceMap = new ConcurrentHashMap<>();

    private final List<ServiceChangeListener> listeners = new CopyOnWriteArrayList<>();

    public void registerInstance(String serviceName, String host, int port) {
        JQuickServiceInstance instance = new JQuickServiceInstance(serviceName, host, port);
        serviceMap.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>()).add(instance);
        notifyListeners(serviceName);
    }

    public void registerInstance(String serviceName, String host, int port, int weight) {
        JQuickServiceInstance instance = new JQuickServiceInstance(serviceName, host, port, weight);
        serviceMap.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>()).add(instance);
        notifyListeners(serviceName);
    }

    private void notifyListeners(String serviceName) {
        List<JQuickServiceInstance> instances = getInstances(serviceName);
        for (ServiceChangeListener listener : listeners) {
            listener.onChange(serviceName, instances);
        }
    }

    @Override
    public List<JQuickServiceInstance> getInstances(String serviceName) {
        return serviceMap.getOrDefault(serviceName, new CopyOnWriteArrayList<>());
    }

    @Override
    public void subscribe(String serviceName, ServiceChangeListener listener) {
        listeners.add(listener);
        // 立即通知当前状态
        listener.onChange(serviceName, getInstances(serviceName));
    }

    @Override
    public void unsubscribe(String serviceName, ServiceChangeListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void close() {
        serviceMap.clear();
        listeners.clear();
    }
}