package com.github.paohaijiao.config;

import lombok.Data;

/**
 * 传输配置
 */
@Data
public class JQuickTransportConfig {

    private int timeout = 30000;

    private boolean framed = false;

    private int maxFrameSize = 1024 * 1024;  // 1MB

    private boolean reuseAddress = true;

    private int backlog = 1024;

    private boolean keepAlive = true;

    private boolean tcpNoDelay = true;

    private String transportType = null;

    public static JQuickTransportConfig standard() {
        return new JQuickTransportConfig();
    }

    public static JQuickTransportConfig framed() {
        JQuickTransportConfig config = new JQuickTransportConfig();
        config.setFramed(true);
        return config;
    }

    public static JQuickTransportConfig withTimeout(int timeout) {
        JQuickTransportConfig config = new JQuickTransportConfig();
        config.setTimeout(timeout);
        return config;
    }

    public String getTransportType() {
        return transportType;
    }
}
