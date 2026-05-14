package com.github.paohaijiao.domain;

import lombok.Data;

import java.util.Map;
import java.util.Objects;

/**
 * 服务实例信息
 */
@Data
public class JQuickServiceInstance {

    private String serviceName;

    private String host;

    private int port;

    private int weight;  // 权重，用于加权负载均衡

    private boolean healthy;

    private double cpuUsage;

    private double memoryUsage;

    private int activeRequests;

    private long queueSize;

    private long lastReportTime;

    private Map<String, String> metadata;

    public JQuickServiceInstance(String serviceName, String host, int port) {
        this.serviceName = serviceName;
        this.host = host;
        this.port = port;
        this.weight = 1;
        this.healthy = true;
    }

    public JQuickServiceInstance(String serviceName, String host, int port, int weight) {
        this.serviceName = serviceName;
        this.host = host;
        this.port = port;
        this.weight = weight;
        this.healthy = true;
    }

    public String getAddress() {
        return host + ":" + port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JQuickServiceInstance that = (JQuickServiceInstance) o;
        return port == that.port && Objects.equals(serviceName, that.serviceName) && Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, host, port);
    }

    @Override
    public String toString() {
        return String.format("ServiceInstance{name='%s', address=%s:%d, weight=%d, healthy=%s}", serviceName, host, port, weight, healthy);
    }
}
