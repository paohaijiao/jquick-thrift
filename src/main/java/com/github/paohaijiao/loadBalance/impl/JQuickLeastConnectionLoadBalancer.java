package com.github.paohaijiao.loadBalance.impl;

import com.github.paohaijiao.domain.JQuickServiceInstance;
import com.github.paohaijiao.domain.JQuickServiceInstanceMetrics;
import com.github.paohaijiao.exception.JAssert;
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
        JAssert.notNull(instance.getMetrics(), "the Least Load Balancer  require metric not null");
        JQuickServiceInstanceMetrics metrics=instance.getMetrics();
        double load = metrics.getActiveRequests() * 0.4;
        load += metrics.getCpuUsage() * 0.3;
        load += (metrics.getQueueSize() / 100.0) * 0.3;
        return load;
    }
}