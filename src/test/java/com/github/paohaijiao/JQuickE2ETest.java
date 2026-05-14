package com.github.paohaijiao;

import com.example.thrift.demo.Response;
import com.example.thrift.demo.User;
import com.example.thrift.demo.UserService;
import com.example.thrift.demo.UserServiceImpl;
import com.github.paohaijiao.client.JQuickThriftClient;
import com.github.paohaijiao.config.JQuickClientConfig;
import com.github.paohaijiao.config.JQuickConnectionConfig;
import com.github.paohaijiao.config.JQuickServerConfig;
import com.github.paohaijiao.console.JConsole;
import com.github.paohaijiao.discovery.impl.JQuickInMemoryServiceDiscovery;
import com.github.paohaijiao.manager.JQuickDynamicFactory;
import com.github.paohaijiao.server.JQuickThriftServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * 端到端集成测试
 */
public class JQuickE2ETest {

    private static final JConsole console = JConsole.getInstance();
    private static final int TEST_PORT = 9090;
    private static final String SERVICE_NAME = "UserService";

    private JQuickThriftServer server;
    private JQuickThriftClient client;
    private JQuickDynamicFactory factory;
    private JQuickInMemoryServiceDiscovery discovery;

    @Before
    public void setUp() throws Exception {
        console.info("========================================");
        console.info("端到端集成测试开始");
        console.info("========================================");
        factory = new JQuickDynamicFactory();
        discovery = new JQuickInMemoryServiceDiscovery();
        // 创建并启动服务端
        JQuickServerConfig serverConfig = JQuickServerConfig.threadPool(TEST_PORT);
        serverConfig.setMaxWorkerThreads(50);
        server = factory.createServer(serverConfig);
        server.registerService(SERVICE_NAME, new UserServiceImpl());
        server.start();
        // 注册服务实例
        discovery.registerInstance(SERVICE_NAME, "localhost", TEST_PORT);
        Thread.sleep(1000);
        console.info("服务端已启动，端口: " + TEST_PORT);
        console.info("服务实例已注册: localhost:" + TEST_PORT);
        // 创建客户端
        JQuickClientConfig clientConfig = JQuickClientConfig.pooled();
        JQuickConnectionConfig connectionConfig = JQuickConnectionConfig.defaultConfig();
        connectionConfig.setTimeout(5000);
        connectionConfig.setMaxRetries(3);
        client = factory.createClient(clientConfig, discovery, null, null, connectionConfig);
        console.info("客户端已创建");
    }

    @After
    public void tearDown() {
        console.info("========================================");
        console.info("清理测试资源");
        if (client != null) {
            client.close();
            console.info("客户端已关闭");
        }
        if (server != null && server.isRunning()) {
            server.stop();
            console.info("服务端已停止");
        }
        console.info("========================================");
    }

    @Test
    public void testGetServiceProxy() throws Exception {
        console.info("【测试1】获取服务代理");
        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);
        assertNotNull("服务代理不应为 null", proxy);
        console.info("成功获取服务代理: " + proxy.getClass().getName());
    }

    @Test
    public void testSayHello() throws Exception {
        console.info("【测试2】sayHello 方法测试");
        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);
        String result = proxy.sayHello("ThriftClient");
        assertNotNull("返回结果不应为 null", result);
        assertTrue("结果应包含 Hello", result.contains("Hello"));
        assertTrue("结果应包含参数", result.contains("ThriftClient"));
        console.info("调用结果: " + result);
        console.info("sayHello 测试通过");
    }

    @Test
    public void testGetUserSuccess() throws Exception {
        console.info("【测试3】获取存在的用户");
        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);
        User user = proxy.getUser(1);
        assertNotNull("用户不应为 null", user);
        assertEquals("用户ID应为 1", 1, user.getId());
        assertEquals("用户名应为 张三", "张三", user.getName());
        console.info("获取到的用户: ID=" + user.getId() + ", 姓名=" + user.getName() + ", 邮箱=" + user.getEmail() + ", 年龄=" + user.getAge());
        console.info("getUser 测试通过");
    }

    @Test(expected = Exception.class)
    public void testGetUserNotFound() throws Exception {
        console.info("【测试4】获取不存在的用户（应抛出异常）");
        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);
        proxy.getUser(999);
        fail("应该抛出异常");
    }

    @Test
    public void testCreateUser() throws Exception {
        console.info("【测试5】创建新用户");
        User newUser = new User();
        newUser.setName("王五");
        newUser.setEmail("wangwu@example.com");
        newUser.setAge(28);
        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);
        Response response = proxy.saveUser(newUser);
        assertNotNull(response);
        assertEquals("响应码应为 200", 200, response.getCode());
        assertTrue("消息应包含成功", response.getMessage().contains("成功"));
        console.info("创建结果: " + response.getMessage());
        console.info("创建用户测试通过");
    }

    @Test
    public void testUpdateUser() throws Exception {
        console.info("【测试6】更新用户");
        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);
        User user = proxy.getUser(1);
        user.setName("张三（已更新）");
        user.setAge(26);
        Response response = proxy.saveUser(user);
        assertEquals("响应码应为 200", 200, response.getCode());
        User updatedUser = proxy.getUser(1);
        assertEquals("用户名已更新", "张三（已更新）", updatedUser.getName());
        assertEquals("年龄已更新", 26, updatedUser.getAge());
        console.info("更新结果: " + response.getMessage());
        console.info("更新用户测试通过");
    }

    @Test
    public void testDeleteUser() throws Exception {
        console.info("【测试7】删除用户");
        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);
        List<User> beforeList = proxy.listAllUsers();
        int beforeCount = beforeList.size();
        console.info("删除前用户数量: " + beforeCount);
        Response response = proxy.deleteUser(2);
        assertEquals("响应码应为 200", 200, response.getCode());
        List<User> afterList = proxy.listAllUsers();
        assertEquals("用户数量应减少 1", beforeCount - 1, afterList.size());
        console.info("删除结果: " + response.getMessage());
        console.info("删除用户测试通过");
    }

    @Test
    public void testListAllUsers() throws Exception {
        console.info("【测试8】获取所有用户列表");
        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);
        List<User> users = proxy.listAllUsers();
        assertNotNull("用户列表不应为 null", users);
        assertTrue("用户列表不应为空", users.size() > 0);
        console.info("用户列表大小: " + users.size());
        for (User user : users) {
            console.info("  - ID: " + user.getId() + ", 姓名: " + user.getName() + ", 年龄: " + user.getAge());
        }
        console.info("listAllUsers 测试通过");
    }

    @Test
    public void testMultipleCalls() throws Exception {
        console.info("【测试9】多次调用测试（验证连接复用）");
        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);
        long startTime = System.currentTimeMillis();
        int callCount = 10;
        for (int i = 0; i < callCount; i++) {
            String result = proxy.sayHello("User" + i);
            assertNotNull(result);
            console.info("  调用 " + (i + 1) + ": " + result);
        }
        long endTime = System.currentTimeMillis();
        console.info("成功执行 " + callCount + " 次调用，总耗时: " + (endTime - startTime) + "ms");
    }

    @Test
    public void testCompositeOperations() throws Exception {
        console.info("【测试10】复合操作测试");
        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);
        List<User> initialUsers = proxy.listAllUsers();
        console.info("1. 初始用户数: " + initialUsers.size());
        User newUser = new User();
        newUser.setName("测试用户");
        newUser.setEmail("test@example.com");
        newUser.setAge(25);
        Response createResp = proxy.saveUser(newUser);
        console.info("2. 创建用户: " + createResp.getMessage());
        String data = createResp.getData();
        String userIdStr = data.replaceAll("[^0-9]", "");
        int newUserId = Integer.parseInt(userIdStr);
        User createdUser = proxy.getUser(newUserId);
        console.info("3. 获取新用户: " + createdUser.getName());
        createdUser.setName("测试用户（已更新）");
        Response updateResp = proxy.saveUser(createdUser);
        console.info("4. 更新用户: " + updateResp.getMessage());
        User updatedUser = proxy.getUser(newUserId);
        console.info("5. 验证更新后姓名: " + updatedUser.getName());
        Response deleteResp = proxy.deleteUser(newUserId);
        console.info("6. 删除用户: " + deleteResp.getMessage());
        List<User> finalUsers = proxy.listAllUsers();
        console.info("7. 最终用户数: " + finalUsers.size());
        assertEquals("最终用户数应等于初始用户数", initialUsers.size(), finalUsers.size());
        console.info("复合操作测试通过");
    }

    @Test
    public void testClientStats() throws Exception {
        console.info("【测试11】客户端统计信息");
        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);
        proxy.sayHello("Stats");
        proxy.getUser(1);
        proxy.listAllUsers();
        Map<String, Object> stats = client.getStats();
        console.info("客户端统计信息:");
        for (Map.Entry<String, Object> entry : stats.entrySet()) {
            console.info("  " + entry.getKey() + ": " + entry.getValue());
        }
        assertNotNull("统计信息不应为 null", stats);
        assertTrue("应包含 failoverCount", stats.containsKey("failoverCount"));
        assertTrue("应包含 proxyCacheSize", stats.containsKey("proxyCacheSize"));
        console.info("统计信息测试通过");
    }

    @Test
    public void testExceptionHandling() throws Exception {
        console.info("【测试12】异常处理测试");
        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);
        try {
            proxy.getUser(88888);
            fail("应该抛出异常");
        } catch (Exception e) {
            console.info("捕获到异常: " + e.getMessage());
            assertTrue("异常信息应包含用户不存在", e.getMessage().contains("用户不存在") || e.getMessage().contains("88888"));
        }
        console.info("异常处理测试通过");
    }

    @Test
    public void testConcurrentCalls() throws Exception {
        console.info("【测试13】并发调用测试");
        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);
        int threadCount = 5;
        int callsPerThread = 20;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        Thread[] threads = new Thread[threadCount];
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < callsPerThread; j++) {
                    try {
                        String result = proxy.sayHello("Thread" + threadId + "_Call" + j);
                        if (result != null && result.contains("Hello")) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        console.error("线程 " + threadId + " 调用失败: " + e.getMessage());
                    }
                }
            });
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        console.info("并发测试结果:");
        console.info("  总调用次数: " + (threadCount * callsPerThread));
        console.info("  成功次数: " + successCount.get());
        console.info("  失败次数: " + failCount.get());
        console.info("  总耗时: " + duration + "ms");
        console.info("  TPS: " + (successCount.get() * 1000.0 / duration));
        assertEquals("所有调用都应成功", threadCount * callsPerThread, successCount.get());
        console.info("并发调用测试通过");
    }

    @Test
    public void testConnectionPoolEffect() throws Exception {
        console.info("【测试14】连接池效果测试");
        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);
        int callCount = 100;
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < callCount; i++) {
            proxy.sayHello("Test" + i);
        }
        long endTime = System.currentTimeMillis();
        Map<String, Object> stats = client.getStats();
        console.info("连接池统计:");
        console.info("  调用次数: " + callCount);
        console.info("  总耗时: " + (endTime - startTime) + "ms");
        console.info("  平均耗时: " + (endTime - startTime) / callCount + "ms");
        if (stats.containsKey("activeConnections")) {
            console.info("  活跃连接数: " + stats.get("activeConnections"));
        }
        if (stats.containsKey("idleConnections")) {
            console.info("  空闲连接数: " + stats.get("idleConnections"));
        }
        console.info("连接池效果测试通过");
    }
}
