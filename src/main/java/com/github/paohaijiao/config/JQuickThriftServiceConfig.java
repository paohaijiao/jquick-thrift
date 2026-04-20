package com.github.paohaijiao.config;

import lombok.Data;

@Data
public class JQuickThriftServiceConfig {

    private String host = "localhost";

    private int port = 9090;

    private int timeout = 5000;

    private int maxRetries = 3;

    private boolean usePool = true;

    private int maxPoolSize = 50;
}
