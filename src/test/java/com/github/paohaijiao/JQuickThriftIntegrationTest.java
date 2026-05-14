package com.github.paohaijiao;

import com.example.thrift.demo.*;
import com.github.paohaijiao.client.JQuickThriftClient;
import com.github.paohaijiao.config.*;
import com.github.paohaijiao.discovery.impl.JQuickInMemoryServiceDiscovery;
import com.github.paohaijiao.domain.JQuickServiceInstance;
import com.github.paohaijiao.discovery.JQuickServiceDiscovery;
import com.github.paohaijiao.loadBalence.JQuickLoadBalancer;
import com.github.paohaijiao.loadBalence.impl.JQuickRoundRobinLoadBalancer;
import com.github.paohaijiao.manager.JQuickDynamicFactory;
import com.github.paohaijiao.protocol.JQuickBinaryProtocolFactory;
import com.github.paohaijiao.server.JQuickThriftServer;
import com.github.paohaijiao.server.impl.JQuickThreadPoolServer;
import com.github.paohaijiao.transport.JQuickStandardTransportFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Thrift客户端-服务端集成测试
 */
public class JQuickThriftIntegrationTest {

    private JQuickThriftServer server;

    private JQuickThriftClient client;

    private static final int TEST_PORT = 9093;
    private JQuickInMemoryServiceDiscovery serviceDiscovery;

    private String extractServiceName(Object serviceImpl) {
        Class<?>[] interfaces = serviceImpl.getClass().getInterfaces();
        for (Class<?> iface : interfaces) {
            String ifaceName = iface.getSimpleName();
            if (ifaceName.endsWith("$Iface")) {
                return ifaceName.substring(0, ifaceName.length() - 6);
            }
            if (ifaceName.endsWith("Iface")) {
                return ifaceName.substring(0, ifaceName.length() - 5);
            }
        }
        return serviceImpl.getClass().getSimpleName().replace("Impl", "");
    }
    @Before
    public void setUp() throws Exception {
        System.out.println("\n========================================");
        System.out.println("端到端集成测试开始");
        System.out.println("========================================\n");
        serviceDiscovery = new JQuickInMemoryServiceDiscovery();
        JQuickServerConfig serverConfig = new JQuickServerConfig();
        serverConfig.setPort(TEST_PORT);
        serverConfig.setServerType("threadpool");
        serverConfig.setMaxWorkerThreads(200);
        serverConfig.setMinWorkerThreads(5);
        JQuickProtocolConfig protocolConfig = new JQuickProtocolConfig();
        protocolConfig.setType("binary");
        JQuickTransportConfig transportConfig = new JQuickTransportConfig();
        transportConfig.setFramed(false);
        server = new JQuickThreadPoolServer(serverConfig, new JQuickBinaryProtocolFactory(protocolConfig), new JQuickStandardTransportFactory(transportConfig));
        String serviceName = extractServiceName(new UserServiceImpl());
        server.registerService(serviceName, new UserServiceImpl());
        server.start();
        Thread.sleep(1000);
        System.out.println("✓ 服务端已启动，端口: " + TEST_PORT);

        String SERVICE_NAME="service";
        serviceDiscovery.registerInstance(SERVICE_NAME, "localhost", TEST_PORT);
        System.out.println("✓ 服务实例已注册: localhost:" + TEST_PORT);

        JQuickConnectionConfig connectionConfig = JQuickConnectionConfig.defaultConfig();
        connectionConfig.setTimeout(5000);
        connectionConfig.setMaxRetries(3);
        connectionConfig.setProtocolType("binary");

        JQuickClientConfig clientConfig = new JQuickClientConfig();
        clientConfig.setClientType("pooled");
        clientConfig.setMultiplexed(true);
        clientConfig.setMaxRetries(3);
        JQuickLoadBalancer loadBalancer = new JQuickRoundRobinLoadBalancer();

        JQuickDynamicFactory factory = new JQuickDynamicFactory();
        client = factory.createClient(clientConfig, serviceDiscovery, loadBalancer, null, connectionConfig);

        System.out.println("✓ 客户端已创建\n");
    }

    @After
    public void tearDown() {
        if (client != null) {
            client.close();
        }
        if (server != null && server.isRunning()) {
            server.stop();
        }
        System.out.println("========== 集成测试结束 ==========\n");
    }


    @Test
    public void testIntegrationFramework() {
        System.out.println("集成测试框架已准备就绪");
        System.out.println("服务端端口: " + TEST_PORT);
        System.out.println("服务端状态: " + (server.isRunning() ? "运行中" : "已停止"));
        assertTrue("服务端应运行中", server.isRunning());
    }
}
