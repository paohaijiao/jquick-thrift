package com.github.paohaijiao.loadBalance.impl;

import com.github.paohaijiao.domain.JQuickServiceInstance;
import com.github.paohaijiao.loadBalance.JQuickLoadBalancer;
import com.github.paohaijiao.spi.anno.Priority;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询负载均衡器
 */
@Priority(100)  // 高优先级，默认使用
public class JQuickRoundRobinLoadBalancer implements JQuickLoadBalancer {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public JQuickServiceInstance select(List<JQuickServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        int index = Math.abs(counter.getAndIncrement() % instances.size());
        return instances.get(index);
    }

    @Override
    public String getName() {
        return "RoundRobin";
    }
}
