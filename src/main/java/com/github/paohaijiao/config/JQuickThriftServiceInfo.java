package com.github.paohaijiao.config;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Thrift 服务信息
 * 用于存储和传递服务的元数据信息
 */
public class JQuickThriftServiceInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    // 基础信息
    private String serviceName;
    private String version;
    private String description;
    private boolean enabled;
    private long registeredTime;

    // 配置信息
    private JQuickThriftServiceConfig config;

    // 统计信息
    private int totalCalls;
    private int successCalls;
    private int failedCalls;
    private double avgResponseTime;
    private long lastCallTime;

    // 扩展属性
    private Map<String, Object> attributes = new HashMap<>();

    public JQuickThriftServiceInfo() {
        this.registeredTime = System.currentTimeMillis();
        this.totalCalls = 0;
        this.successCalls = 0;
        this.failedCalls = 0;
        this.avgResponseTime = 0;
    }

    // Getters and Setters

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getRegisteredTime() {
        return registeredTime;
    }

    public void setRegisteredTime(long registeredTime) {
        this.registeredTime = registeredTime;
    }

    public JQuickThriftServiceConfig getConfig() {
        return config;
    }

    public void setConfig(JQuickThriftServiceConfig config) {
        this.config = config;
    }

    public int getTotalCalls() {
        return totalCalls;
    }

    public void setTotalCalls(int totalCalls) {
        this.totalCalls = totalCalls;
    }

    public int getSuccessCalls() {
        return successCalls;
    }

    public void setSuccessCalls(int successCalls) {
        this.successCalls = successCalls;
    }

    public int getFailedCalls() {
        return failedCalls;
    }

    public void setFailedCalls(int failedCalls) {
        this.failedCalls = failedCalls;
    }

    public double getAvgResponseTime() {
        return avgResponseTime;
    }

    public void setAvgResponseTime(double avgResponseTime) {
        this.avgResponseTime = avgResponseTime;
    }

    public long getLastCallTime() {
        return lastCallTime;
    }

    public void setLastCallTime(long lastCallTime) {
        this.lastCallTime = lastCallTime;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    /**
     * 添加扩展属性
     */
    public void addAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    /**
     * 获取扩展属性
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    /**
     * 记录调用统计
     */
    public synchronized void recordCall(boolean success, long responseTime) {
        this.totalCalls++;
        if (success) {
            this.successCalls++;
        } else {
            this.failedCalls++;
        }

        // 更新平均响应时间（移动平均）
        if (this.totalCalls == 1) {
            this.avgResponseTime = responseTime;
        } else {
            this.avgResponseTime = (this.avgResponseTime * (this.totalCalls - 1) + responseTime) / this.totalCalls;
        }

        this.lastCallTime = System.currentTimeMillis();
    }

    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        if (totalCalls == 0) {
            return 100.0;
        }
        return (successCalls * 100.0) / totalCalls;
    }

    /**
     * 获取服务运行时长（毫秒）
     */
    public long getUptime() {
        return System.currentTimeMillis() - registeredTime;
    }

    /**
     * 获取服务端点地址
     */
    public String getEndpoint() {
        if (config != null) {
            return config.getHost() + ":" + config.getPort();
        }
        return "unknown";
    }

    /**
     * 重置统计信息
     */
    public synchronized void resetStats() {
        this.totalCalls = 0;
        this.successCalls = 0;
        this.failedCalls = 0;
        this.avgResponseTime = 0;
        this.lastCallTime = 0;
    }

    /**
     * 检查服务是否健康
     */
    public boolean isHealthy() {
        // 如果有失败记录，检查成功率是否低于阈值
        if (totalCalls > 10) {
            return getSuccessRate() > 50.0; // 成功率低于50%视为不健康
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format(
                "ThriftServiceInfo{name='%s', version='%s', endpoint='%s', enabled=%s, " +
                        "calls=%d, successRate=%.2f%%, avgResponseTime=%.2fms, healthy=%s}",
                serviceName, version, getEndpoint(), enabled,
                totalCalls, getSuccessRate(), avgResponseTime, isHealthy()
        );
    }

    /**
     * 构建器模式
     */
    public static class Builder {

        private final JQuickThriftServiceInfo info = new JQuickThriftServiceInfo();

        public Builder serviceName(String serviceName) {
            info.serviceName = serviceName;
            return this;
        }

        public Builder version(String version) {
            info.version = version;
            return this;
        }

        public Builder description(String description) {
            info.description = description;
            return this;
        }

        public Builder enabled(boolean enabled) {
            info.enabled = enabled;
            return this;
        }

        public Builder config(JQuickThriftServiceConfig config) {
            info.config = config;
            return this;
        }

        public Builder attribute(String key, Object value) {
            info.addAttribute(key, value);
            return this;
        }

        public JQuickThriftServiceInfo build() {
            return info;
        }
    }
}
