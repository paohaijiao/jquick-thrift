package com.github.paohaijiao;
import com.example.thrift.demo.*;
import com.github.paohaijiao.client.JQuickThriftClient;
import com.github.paohaijiao.config.JQuickConnectionConfig;
import com.github.paohaijiao.domain.JQuickServiceInstance;
import com.github.paohaijiao.pool.JQuickServiceDiscovery;
import com.github.paohaijiao.server.JQuickThriftServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * 客户端-服务端端到端集成测试
 * 测试完整的 RPC 调用流程
 */
public class JQuickThriftE2ETest {

    private JQuickThriftServer server;
    private JQuickThriftClient client;
    private static final int TEST_PORT = 9090;
    private static final String SERVICE_NAME = "UserService";

    /**
     * 内存服务发现实现（用于测试）
     */
    private static class InMemoryServiceDiscovery implements JQuickServiceDiscovery {
        private final Map<String, List<JQuickServiceInstance>> serviceMap = new ConcurrentHashMap<>();
        private final List<ServiceChangeListener> listeners = new CopyOnWriteArrayList<>();

        public void registerInstance(String serviceName, String host, int port) {
            JQuickServiceInstance instance = new JQuickServiceInstance(serviceName, host, port);
            serviceMap.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>()).add(instance);
            notifyListeners(serviceName);
        }

        public void registerInstance(String serviceName, String host, int port, int weight) {
            JQuickServiceInstance instance = new JQuickServiceInstance(serviceName, host, port, weight);
            serviceMap.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>()).add(instance);
            notifyListeners(serviceName);
        }

        private void notifyListeners(String serviceName) {
            List<JQuickServiceInstance> instances = getInstances(serviceName);
            for (ServiceChangeListener listener : listeners) {
                listener.onChange(serviceName, instances);
            }
        }

        @Override
        public List<JQuickServiceInstance> getInstances(String serviceName) {
            return serviceMap.getOrDefault(serviceName, new CopyOnWriteArrayList<>());
        }

        @Override
        public void subscribe(String serviceName, ServiceChangeListener listener) {
            listeners.add(listener);
            // 立即通知当前状态
            listener.onChange(serviceName, getInstances(serviceName));
        }

        @Override
        public void unsubscribe(String serviceName, ServiceChangeListener listener) {
            listeners.remove(listener);
        }

        @Override
        public void close() {
            serviceMap.clear();
            listeners.clear();
        }
    }

    private InMemoryServiceDiscovery serviceDiscovery;

    @Before
    public void setUp() throws Exception {
        System.out.println("\n========================================");
        System.out.println("端到端集成测试开始");
        System.out.println("========================================\n");

        // 1. 创建服务发现
        serviceDiscovery = new InMemoryServiceDiscovery();

        // 2. 启动 Thrift 服务端
        server = new JQuickThriftServer.Builder()
                .port(TEST_PORT)
                .registerService(SERVICE_NAME, new UserServiceImpl())
                .build();
        server.start();

        // 等待服务启动
        Thread.sleep(1000);
        System.out.println("✓ 服务端已启动，端口: " + TEST_PORT);

        // 3. 注册服务实例到服务发现
        serviceDiscovery.registerInstance(SERVICE_NAME, "localhost", TEST_PORT);
        System.out.println("✓ 服务实例已注册: localhost:" + TEST_PORT);

        // 4. 创建客户端
        JQuickConnectionConfig config = JQuickConnectionConfig.defaultConfig();
        config.setTimeout(5000);
        config.setMaxRetries(3);

        client = new JQuickThriftClient.Builder()
                .serviceDiscovery(serviceDiscovery)
                .connectionConfig(config)
                .build();

        System.out.println("✓ 客户端已创建\n");
    }

    @After
    public void tearDown() {
        System.out.println("\n========================================");
        System.out.println("清理测试资源");

        if (client != null) {
            client.close();
            System.out.println("✓ 客户端已关闭");
        }
        if (server != null && server.isRunning()) {
            server.stop();
            System.out.println("✓ 服务端已停止");
        }

        System.out.println("========================================\n");
    }

    /**
     * 测试1: 获取服务代理
     */
    @Test
    public void testGetServiceProxy() {
        System.out.println("【测试1】获取服务代理");

        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);

        assertNotNull("服务代理不应为null", proxy);
        System.out.println("✓ 成功获取服务代理: " + proxy.getClass().getName());
    }

    /**
     * : sayHello 方法
     */
    @Test
    public void testSayHello() throws Exception {
        System.out.println("【测试2】sayHello 方法测试");

        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);
        String result = proxy.sayHello("ThriftClient");

        assertNotNull("返回结果不应为null", result);
        assertTrue("结果应包含Hello", result.contains("Hello"));
        assertTrue("结果应包含参数", result.contains("ThriftClient"));

        System.out.println("调用结果: " + result);
        System.out.println("✓ sayHello 测试通过");
    }

    /**
     * 测试3: 获取用户（存在）
     */
    @Test
    public void testGetUser_Success() throws Exception {
        System.out.println("【测试3】获取存在的用户");

        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);
        User user = proxy.getUser(1);

        assertNotNull("用户不应为null", user);
        assertEquals("用户ID应为1", 1, user.getId());
        assertEquals("用户名应为'张三'", "张三", user.getName());

        System.out.println("获取到的用户: ID=" + user.getId() +
                ", 姓名=" + user.getName() +
                ", 邮箱=" + user.getEmail() +
                ", 年龄=" + user.getAge());
        System.out.println("✓ getUser 测试通过");
    }

    /**
     * 测试4: 获取用户（不存在）
     */
    @Test(expected = Exception.class)
    public void testGetUser_NotFound() throws Exception {
        System.out.println("【测试4】获取不存在的用户（应抛出异常）");

        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);
        proxy.getUser(999);

        fail("应该抛出异常");
    }

    /**
     * 测试5: 创建新用户
     */
    @Test
    public void testCreateUser() throws Exception {
        System.out.println("【测试5】创建新用户");

        User newUser = new User();
        newUser.setName("王五");
        newUser.setEmail("wangwu@example.com");
        newUser.setAge(28);

        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);
        Response response = proxy.saveUser(newUser);

        assertNotNull(response);
        assertEquals("响应码应为200", 200, response.getCode());
        assertTrue("消息应包含成功", response.getMessage().contains("成功"));

        System.out.println("创建结果: " + response.getMessage());
        System.out.println("响应数据: " + response.getData());
        System.out.println("✓ 创建用户测试通过");
    }

    /**
     * 测试6: 更新用户
     */
    @Test
    public void testUpdateUser() throws Exception {
        System.out.println("【测试6】更新用户");

        // 先获取用户
        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);
        User user = proxy.getUser(1);
        user.setName("张三（已更新）");
        user.setAge(26);

        // 更新用户
        Response response = proxy.saveUser(user);

        assertEquals("响应码应为200", 200, response.getCode());

        // 验证更新
        User updatedUser = proxy.getUser(1);
        assertEquals("用户名已更新", "张三（已更新）", updatedUser.getName());
        assertEquals("年龄已更新", 26, updatedUser.getAge());

        System.out.println("更新结果: " + response.getMessage());
        System.out.println("✓ 更新用户测试通过");
    }

    /**
     * 测试7: 删除用户
     */
    @Test
    public void testDeleteUser_Success() throws Exception {
        System.out.println("【测试7】删除用户");

        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);

        // 先获取用户列表
        List<User> beforeList = proxy.listAllUsers();
        int beforeCount = beforeList.size();
        System.out.println("删除前用户数量: " + beforeCount);

        // 删除用户
        Response response = proxy.deleteUser(2);

        assertEquals("响应码应为200", 200, response.getCode());
        assertTrue("消息应包含成功", response.getMessage().contains("成功"));

        // 验证删除后数量
        List<User> afterList = proxy.listAllUsers();
        assertEquals("用户数量应减少1", beforeCount - 1, afterList.size());

        System.out.println("删除结果: " + response.getMessage());
        System.out.println("删除后用户数量: " + afterList.size());
        System.out.println("✓ 删除用户测试通过");
    }

    /**
     * 测试8: 获取所有用户列表
     */
    @Test
    public void testListAllUsers() throws Exception {
        System.out.println("【测试8】获取所有用户列表");

        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);
        List<User> users = proxy.listAllUsers();

        assertNotNull("用户列表不应为null", users);
        assertTrue("用户列表不应为空", users.size() > 0);

        System.out.println("用户列表大小: " + users.size());
        for (User user : users) {
            System.out.println("  - ID: " + user.getId() +
                    ", 姓名: " + user.getName() +
                    ", 年龄: " + user.getAge());
        }
        System.out.println("✓ listAllUsers 测试通过");
    }

    // ==================== 高级功能测试 ====================

    /**
     * 测试9: 多次调用（验证连接复用）
     */
    @Test
    public void testMultipleCalls() throws Exception {
        System.out.println("【测试9】多次调用测试（验证连接复用）");

        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);

        long startTime = System.currentTimeMillis();
        int callCount = 10;

        for (int i = 0; i < callCount; i++) {
            String result = proxy.sayHello("User" + i);
            assertNotNull(result);
            System.out.println("  调用 " + (i + 1) + ": " + result);
        }

        long endTime = System.currentTimeMillis();
        System.out.println("✓ 成功执行 " + callCount + " 次调用，总耗时: " + (endTime - startTime) + "ms");
    }

    /**
     * 测试10: 连续操作（复合业务场景）
     */
    @Test
    public void testCompositeOperations() throws Exception {
        System.out.println("【测试10】复合操作测试");

        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);

        // 1. 查看初始用户
        List<User> initialUsers = proxy.listAllUsers();
        System.out.println("1. 初始用户数: " + initialUsers.size());

        // 2. 创建新用户
        User newUser = new User();
        newUser.setName("测试用户");
        newUser.setEmail("test@example.com");
        newUser.setAge(25);
        Response createResp = proxy.saveUser(newUser);
        System.out.println("2. 创建用户: " + createResp.getMessage());

        // 3. 获取新创建的用户ID
        String data = createResp.getData();
        String userIdStr = data.replaceAll("[^0-9]", "");
        int newUserId = Integer.parseInt(userIdStr);

        // 4. 获取新用户信息
        User createdUser = proxy.getUser(newUserId);
        System.out.println("3. 获取新用户: " + createdUser.getName());

        // 5. 更新用户
        createdUser.setName("测试用户（已更新）");
        Response updateResp = proxy.saveUser(createdUser);
        System.out.println("4. 更新用户: " + updateResp.getMessage());

        // 6. 验证更新
        User updatedUser = proxy.getUser(newUserId);
        System.out.println("5. 验证更新后姓名: " + updatedUser.getName());

        // 7. 删除用户
        Response deleteResp = proxy.deleteUser(newUserId);
        System.out.println("6. 删除用户: " + deleteResp.getMessage());

        // 8. 验证删除
        List<User> finalUsers = proxy.listAllUsers();
        System.out.println("7. 最终用户数: " + finalUsers.size());

        assertEquals("最终用户数应等于初始用户数", initialUsers.size(), finalUsers.size());
        System.out.println("✓ 复合操作测试通过");
    }

    /**
     * 测试11: 客户端统计信息
     */
    @Test
    public void testClientStats() throws Exception {
        System.out.println("【测试11】客户端统计信息");

        // 执行一些调用
        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);
        proxy.sayHello("Stats");
        proxy.getUser(1);
        proxy.listAllUsers();

        Map<String, Object> stats = client.getStats();

        System.out.println("客户端统计信息:");
        for (Map.Entry<String, Object> entry : stats.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }

        assertNotNull("统计信息不应为null", stats);
        assertTrue("应包含failoverCount", stats.containsKey("failoverCount"));
        assertTrue("应包含proxyCacheSize", stats.containsKey("proxyCacheSize"));

        System.out.println("✓ 统计信息测试通过");
    }

    /**
     * 测试12: 异常处理
     */
    @Test
    public void testExceptionHandling() throws Exception {
        System.out.println("【测试12】异常处理测试");

        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);

        try {
            proxy.getUser(88888);
            fail("应该抛出异常");
        } catch (Exception e) {
            System.out.println("捕获到异常: " + e.getMessage());
            assertTrue("异常信息应包含用户不存在",
                    e.getMessage().contains("用户不存在") || e.getMessage().contains("88888"));
        }

        System.out.println("✓ 异常处理测试通过");
    }

    // ==================== 性能测试 ====================

    /**
     * 测试13: 并发调用测试
     */
    @Test
    public void testConcurrentCalls() throws Exception {
        System.out.println("【测试13】并发调用测试");

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
                        System.err.println("线程 " + threadId + " 调用失败: " + e.getMessage());
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

        System.out.println("并发测试结果:");
        System.out.println("  总调用次数: " + (threadCount * callsPerThread));
        System.out.println("  成功次数: " + successCount.get());
        System.out.println("  失败次数: " + failCount.get());
        System.out.println("  总耗时: " + duration + "ms");
        System.out.println("  TPS: " + (successCount.get() * 1000.0 / duration));

        assertEquals("所有调用都应成功", threadCount * callsPerThread, successCount.get());
        System.out.println("✓ 并发调用测试通过");
    }

    /**
     * 测试14: 连接池效果测试
     */
    @Test
    public void testConnectionPoolEffect() throws Exception {
        System.out.println("【测试14】连接池效果测试");

        UserService.Iface proxy = client.getService(UserService.Iface.class, SERVICE_NAME);

        // 执行100次调用
        int callCount = 100;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < callCount; i++) {
            proxy.sayHello("Test" + i);
        }

        long endTime = System.currentTimeMillis();

        Map<String, Object> stats = client.getStats();

        System.out.println("连接池统计:");
        System.out.println("  调用次数: " + callCount);
        System.out.println("  总耗时: " + (endTime - startTime) + "ms");
        System.out.println("  平均耗时: " + (endTime - startTime) / callCount + "ms");

        if (stats.containsKey("activeConnections")) {
            System.out.println("  活跃连接数: " + stats.get("activeConnections"));
        }
        if (stats.containsKey("idleConnections")) {
            System.out.println("  空闲连接数: " + stats.get("idleConnections"));
        }

        System.out.println("✓ 连接池效果测试通过");
    }
}
