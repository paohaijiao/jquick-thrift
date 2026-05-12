package com.github.paohaijiao.discovery.impl;

import com.github.paohaijiao.discovery.JQuickServiceDiscovery;
import com.github.paohaijiao.domain.JQuickServiceInstance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public  class JQuickSimpleServiceDiscovery implements JQuickServiceDiscovery {

    private final String serviceName;

    private final String host;

    private final int port;

    public JQuickSimpleServiceDiscovery(String serviceName, String host, int port) {
        this.serviceName = serviceName;
        this.host = host;
        this.port = port;
    }

    @Override
    public List<JQuickServiceInstance> getInstances(String serviceName) {
        if (this.serviceName.equals(serviceName)) {
            return Arrays.asList(new JQuickServiceInstance(serviceName, host, port));
        }
        return new ArrayList<>();
    }

    @Override
    public void subscribe(String serviceName, ServiceChangeListener listener) {
    }

    @Override
    public void unsubscribe(String serviceName, ServiceChangeListener listener) {
    }

    @Override
    public void close() {
    }
}
