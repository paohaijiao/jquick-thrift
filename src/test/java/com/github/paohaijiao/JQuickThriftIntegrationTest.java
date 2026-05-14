package com.github.paohaijiao;

import com.example.thrift.demo.*;
import com.github.paohaijiao.client.JQuickThriftClient;
import com.github.paohaijiao.domain.JQuickServiceInstance;
import com.github.paohaijiao.discovery.JQuickServiceDiscovery;
import com.github.paohaijiao.server.JQuickThriftServer;
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



    @Before
    public void setUp() throws Exception {
        System.out.println("\n========== 集成测试开始 ==========");
        server = new JQuickThriftServer.Builder()
                .port(TEST_PORT)
                .registerService(new UserServiceImpl())
                .build();
        server.start();
        Thread.sleep(1000);
        System.out.println("服务端已启动，端口: " + TEST_PORT);
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
