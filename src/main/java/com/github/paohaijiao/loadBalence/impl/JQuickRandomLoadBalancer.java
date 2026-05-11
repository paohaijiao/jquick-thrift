package com.github.paohaijiao.loadBalence.impl;

import com.github.paohaijiao.domain.JQuickServiceInstance;
import com.github.paohaijiao.loadBalence.JQuickLoadBalancer;
import com.github.paohaijiao.spi.anno.Priority;

import java.util.List;
import java.util.Random;

/**
 * 随机负载均衡器
 */
@Priority(200)  // 中优先级
public class JQuickRandomLoadBalancer implements JQuickLoadBalancer {

    private final Random random = new Random();

    @Override
    public JQuickServiceInstance select(List<JQuickServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        if (instances.size() == 1) {
            return instances.get(0);
        }
        return instances.get(random.nextInt(instances.size()));
    }

    @Override
    public String getName() {
        return "Random";
    }
}
