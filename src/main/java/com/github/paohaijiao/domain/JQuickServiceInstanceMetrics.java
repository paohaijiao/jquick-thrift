package com.github.paohaijiao.domain;

import lombok.Data;

@Data
public class JQuickServiceInstanceMetrics {

    private double cpuUsage;

    private double memoryUsage;

    private int activeRequests;

    private long queueSize;

    private long lastReportTime;
}
