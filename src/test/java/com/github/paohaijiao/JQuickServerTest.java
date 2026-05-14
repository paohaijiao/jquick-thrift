package com.github.paohaijiao;

import com.example.thrift.demo.UserServiceImpl;
import com.github.paohaijiao.config.JQuickServerConfig;
import com.github.paohaijiao.console.JConsole;
import com.github.paohaijiao.manager.JQuickDynamicFactory;
import com.github.paohaijiao.server.JQuickThriftServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * 服务端测试
 */
public class JQuickServerTest {

    private static final JConsole console = JConsole.getInstance();
    private static final int TEST_PORT = 9091;

    private JQuickThriftServer server;
    private JQuickDynamicFactory factory;

    @Before
    public void setUp() {
        console.info("========== 服务端测试开始 ==========");
        factory = new JQuickDynamicFactory();
    }

    @After
    public void tearDown() {
        if (server != null && server.isRunning()) {
            server.stop();
            console.info("服务已停止");
        }
        console.info("========== 服务端测试结束 ==========");
    }

    @Test
    public void testThreadPoolServerCreation() throws Exception {
        console.info("测试线程池服务器创建");
        JQuickServerConfig config = JQuickServerConfig.threadPool(TEST_PORT);
        factory.getActiveTransportConfig().setTransportType("standard");
        JQuickThriftServer server = factory.createServer(config);
        assertNotNull("服务器不应为 null", server);
        assertEquals("服务器类型应为 threadpool", "threadpool", server.getServerType());
        assertEquals("端口应为 " + TEST_PORT, TEST_PORT, server.getPort());
        console.info("线程池服务器创建成功");
    }

    @Test
    public void testNonBlockingServerCreation() throws Exception {
        console.info("测试非阻塞服务器创建");
        JQuickServerConfig config = JQuickServerConfig.nonBlocking(TEST_PORT + 1);
        factory.getActiveTransportConfig().setTransportType("standard");
        JQuickThriftServer server = factory.createServer(config);
        assertNotNull("服务器不应为 null", server);
        assertEquals("服务器类型应为 nonblocking", "nonblocking", server.getServerType());
        console.info("非阻塞服务器创建成功");
    }

    @Test
    public void testHsHaServerCreation() throws Exception {
        console.info("测试 HsHa 服务器创建");
        JQuickServerConfig config = JQuickServerConfig.hsHa(TEST_PORT + 2);
        factory.getActiveTransportConfig().setTransportType("standard");
        JQuickThriftServer server = factory.createServer(config);
        assertNotNull("服务器不应为 null", server);
        assertEquals("服务器类型应为 hsha", "hsha", server.getServerType());
        console.info("HsHa 服务器创建成功");
    }

    @Test
    public void testThreadedSelectorServerCreation() throws Exception {
        console.info("测试 ThreadedSelector 服务器创建");
        JQuickServerConfig config = JQuickServerConfig.selector(TEST_PORT + 3);
        factory.getActiveTransportConfig().setTransportType("standard");
        JQuickThriftServer server = factory.createServer(config);
        assertNotNull("服务器不应为 null", server);
        assertEquals("服务器类型应为 selector", "selector", server.getServerType());
        console.info("ThreadedSelector 服务器创建成功");
    }

    @Test
    public void testServerStartAndStop() throws Exception {
        console.info("测试服务器启动和停止");
        JQuickServerConfig config = JQuickServerConfig.threadPool(TEST_PORT);
        factory.getActiveTransportConfig().setTransportType("standard");
        server = factory.createServer(config);
        server.registerService("UserService", new UserServiceImpl());
        server.start();
        Thread.sleep(1000);
        assertTrue("服务器应正在运行", server.isRunning());
        console.info("服务器启动成功，端口: " + server.getPort());
        server.stop();
        Thread.sleep(500);
        assertFalse("服务器应已停止", server.isRunning());
        console.info("服务器停止成功");
    }

    @Test
    public void testServiceRegistration() throws Exception {
        console.info("测试服务注册");
        JQuickServerConfig config = JQuickServerConfig.threadPool(TEST_PORT);
        factory.getActiveTransportConfig().setTransportType("standard");
        server = factory.createServer(config);
        server.registerService("UserService", new UserServiceImpl());
        Map<String, Object> services = server.getRegisteredServices();
        assertEquals("应注册 1 个服务", 1, services.size());
        assertTrue("应包含 UserService", services.containsKey("UserService"));
        console.info("服务注册成功: " + services.keySet());
    }

    @Test
    public void testMultipleServiceRegistration() throws Exception {
        console.info("测试多服务注册");
        JQuickServerConfig config = JQuickServerConfig.threadPool(TEST_PORT);
        factory.getActiveTransportConfig().setTransportType("standard");
        server = factory.createServer(config);
        server.registerService("UserService", new UserServiceImpl());
        server.registerService("AdminService", new UserServiceImpl());
        Map<String, Object> services = server.getRegisteredServices();
        assertEquals("应注册 2 个服务", 2, services.size());
        assertTrue("应包含 UserService", services.containsKey("UserService"));
        assertTrue("应包含 AdminService", services.containsKey("AdminService"));
        console.info("多服务注册成功: " + services.keySet());
    }

    @Test
    public void testServerConfig() {
        console.info("测试服务器配置");
        JQuickServerConfig config = JQuickServerConfig.threadPool(9090);
        config.setMinWorkerThreads(10);
        config.setMaxWorkerThreads(500);
        config.setSelectorThreads(4);
        config.setAcceptQueueSize(2048);
        assertEquals("端口应为 9090", 9090, config.getPort());
        assertEquals("minWorkerThreads 应为 10", 10, config.getMinWorkerThreads());
        assertEquals("maxWorkerThreads 应为 500", 500, config.getMaxWorkerThreads());
        assertEquals("selectorThreads 应为 4", 4, config.getSelectorThreads());
        console.info("服务器配置测试通过");
    }
}
