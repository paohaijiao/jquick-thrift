package com.github.paohaijiao.loadBalance;

import com.github.paohaijiao.domain.JQuickServiceInstance;
import com.github.paohaijiao.spi.anno.Priority;

import java.util.List;

/**
 * 负载均衡器SPI接口
 * 支持通过SPI机制扩展不同的负载均衡策略
 */
@Priority(100)  // 应用级别优先级
public interface JQuickLoadBalancer {

    /**
     * 从服务实例列表中选择一个实例
     *
     * @param instances 可用的服务实例列表
     * @return 选中的服务实例
     */
    JQuickServiceInstance select(List<JQuickServiceInstance> instances);

    /**
     * 获取负载均衡器名称
     */
    default String getName() {
        return getClass().getSimpleName();
    }
}