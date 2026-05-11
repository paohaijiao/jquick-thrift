package com.github.paohaijiao;

import com.example.thrift.demo.UserService;
import com.example.thrift.demo.UserServiceImpl;
import com.github.paohaijiao.ano.JQuickThriftService;
import com.github.paohaijiao.server.JQuickThriftServer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TJSONProtocol;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Thrift服务端测试用例（修复版）
 */
public class JQuickThriftServerTest {

    private JQuickThriftServer server;
    private static final int TEST_PORT = 9091;

    @JQuickThriftService(name = "UserService", version = 1)
    private static class AnnotatedUserServiceImpl extends UserServiceImpl implements UserService.Iface {
        // 带注解的服务实现
    }

    @Before
    public void setUp() {
        System.out.println("\n========== 服务端测试开始 ==========");
    }

    @After
    public void tearDown() {
        if (server != null && server.isRunning()) {
            server.stop();
            System.out.println("服务已停止");
        }
        System.out.println("========== 服务端测试结束 ==========\n");
    }

    @Test
    public void testServerStart() throws Exception {
        System.out.println("测试服务端启动");
        server = new JQuickThriftServer.Builder()
                .port(TEST_PORT)
                .registerService(new UserServiceImpl())
                .build();
        server.start();
        Thread.sleep(1000);
        assertTrue("服务应正在运行", server.isRunning());
        assertEquals("端口应为" + TEST_PORT, TEST_PORT, server.getPort());
        System.out.println("服务端启动成功，端口: " + server.getPort());
    }

    @Test
    public void testServerWithAnnotatedService() throws Exception {
        System.out.println("测试带注解的服务注册");
        server = new JQuickThriftServer.Builder()
                .port(TEST_PORT)
                .registerService(new AnnotatedUserServiceImpl())
                .build();
        server.start();
        Thread.sleep(1000);
        assertTrue("服务应正在运行", server.isRunning());
        System.out.println("带注解的服务注册成功");
    }

    @Test
    public void testServerWithNamedService() throws Exception {
        System.out.println("测试带名称的服务注册");
        server = new JQuickThriftServer.Builder()
                .port(TEST_PORT)
                .registerService("CustomUserService", new UserServiceImpl())
                .build();
        server.start();
        Thread.sleep(1000);
        assertTrue("服务应正在运行", server.isRunning());
        System.out.println("带名称的服务注册成功");
    }

    @Test
    public void testServerWithDifferentProtocols() throws Exception {
        System.out.println("测试多种协议类型的服务端");
        server = new JQuickThriftServer.Builder()
                .port(TEST_PORT)
                .protocolFactory(new TBinaryProtocol.Factory())
                .registerService(new UserServiceImpl())
                .build();
        server.start();
        Thread.sleep(500);
        assertTrue("Binary协议服务应运行中", server.isRunning());
        server.stop();
        Thread.sleep(500);
        server = new JQuickThriftServer.Builder()
                .port(TEST_PORT + 1)
                .protocolFactory(new TCompactProtocol.Factory())
                .registerService(new UserServiceImpl())
                .build();
        server.start();
        Thread.sleep(500);
        assertTrue("Compact协议服务应运行中", server.isRunning());
        server.stop();
        Thread.sleep(500);

        System.out.println("多种协议类型测试通过");
    }

    // 5. 测试服务停止
    @Test
    public void testServerStop() throws Exception {
        System.out.println("测试服务停止");
        server = new JQuickThriftServer.Builder()
                .port(TEST_PORT)
                .registerService(new UserServiceImpl())
                .build();
        server.start();
        Thread.sleep(1000);
        assertTrue("服务应正在运行", server.isRunning());
        server.stop();
        Thread.sleep(500);
        assertFalse("服务应已停止", server.isRunning());
        System.out.println("服务停止成功");
    }

    // 6. 测试重复启动（应忽略）
    @Test
    public void testServerDuplicateStart() throws Exception {
        System.out.println("测试重复启动");

        server = new JQuickThriftServer.Builder()
                .port(TEST_PORT)
                .registerService(new UserServiceImpl())
                .build();

        server.start();
        Thread.sleep(1000);

        // 第二次启动应该被忽略
        server.start();

        assertTrue("服务仍应运行中", server.isRunning());
        System.out.println("重复启动测试通过");
    }

    // 7. 测试无效端口（应抛出异常）
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPort() {
        System.out.println("测试无效端口");

        new JQuickThriftServer.Builder()
                .port(-1)
                .registerService(new UserServiceImpl())
                .build();
    }

    // 8. 测试无服务注册（应抛出异常）
    @Test(expected = IllegalStateException.class)
    public void testNoServiceRegistered() {
        System.out.println("测试无服务注册");

        new JQuickThriftServer.Builder()
                .port(TEST_PORT)
                .build();
    }

    // 9. 测试多服务注册
    @Test
    public void testMultipleServiceRegistration() throws Exception {
        System.out.println("测试多服务注册");

        // 创建两个不同的服务实例
        UserServiceImpl service1 = new UserServiceImpl();
        UserServiceImpl service2 = new UserServiceImpl();

        server = new JQuickThriftServer.Builder()
                .port(TEST_PORT)
                .registerService("UserService1", service1)
                .registerService("UserService2", service2)
                .build();

        server.start();
        Thread.sleep(1000);

        assertTrue("服务应正在运行", server.isRunning());
        System.out.println("多服务注册成功");
    }
}