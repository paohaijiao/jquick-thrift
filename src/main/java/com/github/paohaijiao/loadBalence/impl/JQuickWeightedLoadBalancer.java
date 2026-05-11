package com.github.paohaijiao.loadBalence.impl;

import com.github.paohaijiao.domain.JQuickServiceInstance;
import com.github.paohaijiao.loadBalence.JQuickLoadBalancer;
import com.github.paohaijiao.spi.anno.Priority;

import java.util.List;
import java.util.Random;

/**
 * 加权负载均衡器
 * 根据服务实例的权重进行选择
 */
@Priority(101)
public class JQuickWeightedLoadBalancer implements JQuickLoadBalancer {

    private final Random random = new Random();

    @Override
    public JQuickServiceInstance select(List<JQuickServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        int totalWeight = instances.stream().mapToInt(JQuickServiceInstance::getWeight).sum();
        if (totalWeight <= 0) {
            return instances.get(0);
        }
        int randomWeight = random.nextInt(totalWeight);
        int currentWeight = 0;
        for (JQuickServiceInstance instance : instances) {
            currentWeight += instance.getWeight();
            if (randomWeight < currentWeight) {
                return instance;
            }
        }
        return instances.get(0);
    }

    @Override
    public String getName() {
        return "Weighted";
    }
}