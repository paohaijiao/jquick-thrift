package com.github.paohaijiao.config;

import com.github.paohaijiao.enums.JQuickClientTypeEnums;
import lombok.Data;

/**
 * 客户端配置
 */
@Data
public class JQuickClientConfig {

    private String clientType = JQuickClientTypeEnums.pooled.getCode();  // pooled, single, multiplexed

    private int maxRetries = 3;

    private int timeout = 30000;

    private int maxConnections = 10;

    private int maxIdle = 5;

    private int minIdle = 1;

    private long evictableIdleTimeMillis = 60000L;

    private String loadBalancer = "roundRobin";  // roundRobin, random, weighted

    private boolean multiplexed = true;  // 是否使用多路复用

    public static JQuickClientConfig pooled() {
        JQuickClientConfig config = new JQuickClientConfig();
        config.setClientType(JQuickClientTypeEnums.pooled.getCode());
        return config;
    }

    public static JQuickClientConfig single() {
        JQuickClientConfig config = new JQuickClientConfig();
        config.setClientType(JQuickClientTypeEnums.single.getCode());
        config.setMaxConnections(1);
        return config;
    }

    public static JQuickClientConfig multiplexed() {
        JQuickClientConfig config = new JQuickClientConfig();
        config.setClientType(JQuickClientTypeEnums.multiplexed.getCode());
        config.setMultiplexed(true);
        return config;
    }
}
