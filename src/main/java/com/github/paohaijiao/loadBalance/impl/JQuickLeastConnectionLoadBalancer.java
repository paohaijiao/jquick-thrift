package com.github.paohaijiao.loadBalance.impl;

import com.github.paohaijiao.domain.JQuickServiceInstance;
import com.github.paohaijiao.loadBalance.JQuickLoadBalancer;

import java.util.Comparator;
import java.util.List;

public class JQuickLeastConnectionLoadBalancer implements JQuickLoadBalancer {

    @Override
    public JQuickServiceInstance select(List<JQuickServiceInstance> instances) {
        return instances.stream()
                .min(Comparator.comparingDouble(this::calculateLoadScore))
                .orElse(null);
    }

    private double calculateLoadScore(JQuickServiceInstance instance) {
        double load = instance.getActiveRequests() * 0.4;
        load += instance.getCpuUsage() * 0.3;
        load += (instance.getQueueSize() / 100.0) * 0.3;
        return load;
    }
}