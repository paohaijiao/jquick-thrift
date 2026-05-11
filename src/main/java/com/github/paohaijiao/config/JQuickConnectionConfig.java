package com.github.paohaijiao.config;


import lombok.Data;

/**
 * 连接配置
 */
@Data
public class JQuickConnectionConfig {

    private int timeout = 30000;  // 超时时间(ms)

    private int maxConnections = 10;  // 最大连接数

    private int maxIdle = 5;  // 最大空闲连接数

    private int minIdle = 1;  // 最小空闲连接数

    private int maxRetries = 3;  // 最大重试次数

    private long evictableIdleTimeMillis = 60000L;  // 空闲连接回收时间

    private boolean framed = false;  // 是否使用帧传输

    private String protocolType = "binary";  // 协议类型: binary, compact, json

    public static JQuickConnectionConfig defaultConfig() {
        return new JQuickConnectionConfig();
    }

    public void setEvictableIdleTimeMillis(long evictableIdleTimeMillis) {
        this.evictableIdleTimeMillis = evictableIdleTimeMillis;
    }

}