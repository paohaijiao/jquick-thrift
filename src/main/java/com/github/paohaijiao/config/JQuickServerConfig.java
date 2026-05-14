package com.github.paohaijiao.config;

import com.github.paohaijiao.enums.JQuickServerTypeEnums;
import lombok.Data;

/**
 * 服务器配置
 */
@Data
public class JQuickServerConfig {

    private int port = 9090;

    private String serverType = JQuickServerTypeEnums.threadpool.getCode();  // threadpool, threaded, nonblocking, hsha, selector

    private int workerThreads = 100;

    private int minWorkerThreads = 5;

    private int maxWorkerThreads = 200;

    private int acceptQueueSize = 1024;

    private int selectorThreads = 2;

    private int acceptQueueSizePerThread = 1024;

    private int stopTimeoutMs = 30000;

    public static JQuickServerConfig threadPool(int port) {
        JQuickServerConfig config = new JQuickServerConfig();
        config.setPort(port);
        config.setServerType(JQuickServerTypeEnums.threadpool.getCode());
        return config;
    }

    public static JQuickServerConfig nonBlocking(int port) {
        JQuickServerConfig config = new JQuickServerConfig();
        config.setPort(port);
        config.setServerType(JQuickServerTypeEnums.nonblocking.getCode());
        return config;
    }

    public static JQuickServerConfig hsHa(int port) {
        JQuickServerConfig config = new JQuickServerConfig();
        config.setPort(port);
        config.setServerType(JQuickServerTypeEnums.hsha.getCode());
        return config;
    }

    public static JQuickServerConfig selector(int port) {
        JQuickServerConfig config = new JQuickServerConfig();
        config.setPort(port);
        config.setServerType(JQuickServerTypeEnums.selector.getCode());
        return config;
    }
}
