package com.github.paohaijiao.pool;

import com.github.paohaijiao.domain.JQuickServiceInstance;
import com.github.paohaijiao.spi.anno.Priority;

import java.util.List;

/**
 * 服务发现SPI接口
 * 支持不同的服务注册中心
 */
@Priority(100)
public interface JQuickServiceDiscovery {

    /**
     * 根据服务名获取服务实例列表
     */
    List<JQuickServiceInstance> getInstances(String serviceName);

    /**
     * 订阅服务变更
     */
    void subscribe(String serviceName, ServiceChangeListener listener);

    /**
     * 取消订阅
     */
    void unsubscribe(String serviceName, ServiceChangeListener listener);

    /**
     * 关闭服务发现
     */
    void close();

    /**
     * 服务变更监听器
     */
    interface ServiceChangeListener {
        void onChange(String serviceName, List<JQuickServiceInstance> instances);
    }
}
