package com.github.paohaijiao;

import com.example.thrift.demo.UserService;
import com.example.thrift.demo.UserServiceImpl;
import com.github.paohaijiao.client.JQuickThriftClient;
import com.github.paohaijiao.config.*;
import com.github.paohaijiao.console.JConsole;
import com.github.paohaijiao.discovery.impl.JQuickInMemoryServiceDiscovery;
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

import java.util.Map;

import static org.junit.Assert.assertNotNull;

/**
 * 动态切换测试
 * 测试运行时切换协议、服务器类型等功能
 */
public class JQuickDynamicSwitchTest {

    private static final JConsole console = JConsole.getInstance();

    private static final int TEST_PORT = 9095;

    private JQuickDynamicFactory factory;

    private JQuickThriftServer server;

    private JQuickThriftClient client;

    private JQuickInMemoryServiceDiscovery discovery;

    @Before
    public void setUp() throws Exception {
        console.info("初始化测试环境");
        discovery = new JQuickInMemoryServiceDiscovery();
        discovery.registerInstance("UserService", "localhost", 9090);
        JQuickServerConfig serverConfig = new JQuickServerConfig();
        serverConfig.setPort(9090);
        serverConfig.setServerType("threadpool");
        JQuickProtocolConfig protocolConfig = new JQuickProtocolConfig();
        protocolConfig.setType("binary");
        JQuickTransportConfig transportConfig = new JQuickTransportConfig();
        transportConfig.setTransportType("standard");
        JQuickBinaryProtocolFactory protocolFactory = new JQuickBinaryProtocolFactory(protocolConfig);
        JQuickStandardTransportFactory transportFactory = new JQuickStandardTransportFactory(transportConfig);
        server = new JQuickThreadPoolServer(serverConfig, protocolFactory, transportFactory);
        server.registerService("UserService", new UserServiceImpl());
        server.start();
        Thread.sleep(1000); // 等待服务启动
        JQuickClientConfig clientConfig = JQuickClientConfig.pooled();
        clientConfig.setMultiplexed(true);
        JQuickConnectionConfig connectionConfig = JQuickConnectionConfig.defaultConfig();
        connectionConfig.setProtocolType("binary");
        connectionConfig.setTimeout(30000);
        JQuickLoadBalancer loadBalancer = new JQuickRoundRobinLoadBalancer();
        factory = new JQuickDynamicFactory();
        transportConfig.setTransportType("standard");  // 或 "fr
        client = factory.createClient(clientConfig, discovery, loadBalancer, null, connectionConfig);
        assertNotNull("客户端初始化失败", client);
        console.info("测试环境初始化完成");
    }

    @After
    public void tearDown() {
        if (client != null) {
            client.close();
        }
        if (server != null && server.isRunning()) {
            server.stop();
        }
        console.info("========== 动态切换测试结束 ==========");
    }

    @Test
    public void testSwitchProtocol() throws Exception {
        console.info("【测试1】动态切换协议");
        UserService.Iface proxy = client.getService(UserService.Iface.class, "UserService");
        // 测试当前协议
        String result1 = proxy.sayHello("Test Binary");
        assertNotNull(result1);
        console.info("Binary 协议调用成功: " + result1);
        // 切换到 Compact 协议
        console.info("切换到 Compact 协议...");
        factory.switchProtocol("compact");
        // 等待切换完成
        Thread.sleep(2000);
        // 测试新协议
        String result2 = proxy.sayHello("Test Compact");
        assertNotNull(result2);
        console.info("Compact 协议调用成功: " + result2);
        // 切换到 JSON 协议
        console.info("切换到 JSON 协议...");
        factory.getActiveTransportConfig().setTransportType("standard");
        factory.switchProtocol("json");
        Thread.sleep(2000);
        String result3 = proxy.sayHello("Test JSON");
        assertNotNull(result3);
        console.info("JSON 协议调用成功: " + result3);
        console.info("协议动态切换测试通过");
    }

    @Test
    public void testSwitchTransport() throws Exception {
        console.info("【测试2】动态切换传输层");
        UserService.Iface proxy = client.getService(UserService.Iface.class, "UserService");
        // 测试标准传输
        String result1 = proxy.sayHello("Standard Transport");
        assertNotNull(result1);
        console.info("标准传输调用成功: " + result1);
        // 切换到帧传输
        console.info("切换到帧传输...");
        factory.switchTransport("framed");
        Thread.sleep(2000);
        String result2 = proxy.sayHello("Framed Transport");
        assertNotNull(result2);
        console.info("帧传输调用成功: " + result2);
        // 切换回标准传输
        console.info("切换回标准传输...");
        factory.switchTransport("standard");
        Thread.sleep(2000);
        String result3 = proxy.sayHello("Back to Standard");
        assertNotNull(result3);
        console.info("标准传输调用成功: " + result3);
        console.info("传输层动态切换测试通过");
    }

    @Test
    public void testSwitchServerType() throws Exception {
        console.info("【测试3】动态切换服务器类型");
        UserService.Iface proxy = client.getService(UserService.Iface.class, "UserService");
        // 测试当前 ThreadPool 服务器
        String result1 = proxy.sayHello("ThreadPool Server");
        assertNotNull(result1);
        console.info("ThreadPool 服务器调用成功: " + result1);
        // 切换到 NonBlocking 服务器
        console.info("切换到 NonBlocking 服务器...");
        JQuickServerConfig config = JQuickServerConfig.nonBlocking(TEST_PORT);
        config.setMaxWorkerThreads(50);
        factory.getActiveTransportConfig().setTransportType("standard");
        factory.switchServer("nonblocking", config);
        Thread.sleep(3000);
        String result2 = proxy.sayHello("NonBlocking Server");
        assertNotNull(result2);
        console.info("NonBlocking 服务器调用成功: " + result2);
        console.info("服务器类型动态切换测试通过");
    }

    @Test
    public void testSwitchLoadBalancer() throws Exception {
        console.info("【测试4】动态切换负载均衡器");
        // 注册多个实例用于负载均衡测试
        discovery.registerInstance("UserService", "localhost", TEST_PORT);
        discovery.registerInstance("UserService", "localhost", TEST_PORT + 1);
        // 切换到轮询负载均衡
        console.info("切换到轮询负载均衡...");
        factory.switchLoadBalancer("roundRobin");
        UserService.Iface proxy = client.getService(UserService.Iface.class, "UserService");
        for (int i = 0; i < 5; i++) {
            String result = proxy.sayHello("RoundRobin " + i);
            assertNotNull(result);
        }
        console.info("轮询负载均衡测试完成");
        // 切换到随机负载均衡
        console.info("切换到随机负载均衡...");
        factory.switchLoadBalancer("random");
        for (int i = 0; i < 5; i++) {
            String result = proxy.sayHello("Random " + i);
            assertNotNull(result);
        }
        console.info("随机负载均衡测试完成");
        console.info("负载均衡器动态切换测试通过");
    }

    @Test
    public void testDynamicSwitchStats() throws Exception {
        console.info("【测试5】动态切换统计信息");
        // 执行一些操作
        UserService.Iface proxy = client.getService(UserService.Iface.class, "UserService");
        proxy.sayHello("Test");
        // 获取切换前统计
        console.info("切换前统计:");
        printStats();
        // 切换协议
        console.info("切换到 Compact 协议...");
        factory.switchProtocol("compact");
        Thread.sleep(2000);
        proxy.sayHello("After Switch");
        // 获取切换后统计
        console.info("切换后统计:");
        printStats();
        assertNotNull("统计信息不应为 null", factory.getCurrentStats());
        console.info("动态切换统计测试通过");
    }

    @Test
    public void testContinuousOperationsDuringSwitch() throws Exception {
        console.info("【测试6】切换过程中的连续操作");
        UserService.Iface proxy = client.getService(UserService.Iface.class, "UserService");
        // 在切换过程中持续调用
        Thread switchThread = new Thread(() -> {
            try {
                Thread.sleep(500);
                console.info("开始切换协议...");
                factory.switchProtocol("compact");
                Thread.sleep(500);
                console.info("开始切换传输层...");
                factory.switchTransport("framed");
                Thread.sleep(500);
                console.info("开始切换服务器...");
                JQuickServerConfig config = JQuickServerConfig.nonBlocking(TEST_PORT);
                factory.switchServer("nonblocking", config);
            } catch (Exception e) {
                console.error("切换失败: " + e.getMessage());
            }
        });
        switchThread.start();
        // 持续调用
        for (int i = 0; i < 20; i++) {
            try {
                String result = proxy.sayHello("Operation " + i);
                assertNotNull(result);
                console.info("操作 " + i + " 成功: " + result);
            } catch (Exception e) {
                console.info("操作 " + i + " 失败（切换期间可接受）: " + e.getMessage());
            }
            Thread.sleep(100);
        }
        switchThread.join();
        // 验证最终可用性
        String finalResult = proxy.sayHello("Final Test");
        assertNotNull("最终调用应成功", finalResult);
        console.info("最终调用成功: " + finalResult);
        console.info("切换过程连续操作测试通过");
    }

    private void printStats() {
        Map stats = factory.getCurrentStats();
        console.info("  activeProtocol: " + stats.get("activeProtocol"));
        console.info("  activeTransport: " + stats.get("activeTransport"));
        console.info("  activeServerType: " + stats.get("activeServerType"));
        console.info("  serverRunning: " + stats.get("serverRunning"));
    }
}
