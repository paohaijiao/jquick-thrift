//package com.github.paohaijiao;
//
//import com.example.thrift.demo.UserService;
//import com.example.thrift.demo.UserServiceImpl;
//import com.github.paohaijiao.client.JQuickThriftClient;
//import com.github.paohaijiao.config.JQuickConnectionConfig;
//import com.github.paohaijiao.domain.JQuickServiceInstance;
//import com.github.paohaijiao.loadBalence.JQuickLoadBalancer;
//import com.github.paohaijiao.loadBalence.impl.JQuickRandomLoadBalancer;
//import com.github.paohaijiao.loadBalence.impl.JQuickRoundRobinLoadBalancer;
//import com.github.paohaijiao.loadBalence.impl.JQuickWeightedLoadBalancer;
//import com.github.paohaijiao.pool.JQuickConnectionStrategy;
//import com.github.paohaijiao.pool.impl.JQuickPooledConnectionStrategy;
//import com.github.paohaijiao.server.JQuickThriftServer;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//import static org.junit.Assert.*;
//
///**
// * Thrift客户端测试用例
// */
//public class JQuickThriftClientTest {
//
//    private static final int TEST_PORT = 9092;
//    private JQuickThriftServer server;
//    private JQuickThriftClient client;
//
//    @Before
//    public void setUp() throws Exception {
//        System.out.println("\n========== 客户端测试开始 ==========");
//
//        // 启动服务端
//        server = new JQuickThriftServer.Builder()
//                .port(TEST_PORT)
//                .registerService(new UserServiceImpl())
//                .build();
//        server.start();
//        Thread.sleep(1000);
//
//        System.out.println("服务端已启动，端口: " + TEST_PORT);
//    }
//
//    @After
//    public void tearDown() {
//        if (client != null) {
//            client.close();
//            System.out.println("客户端已关闭");
//        }
//        if (server != null && server.isRunning()) {
//            server.stop();
//            System.out.println("服务端已停止");
//        }
//        System.out.println("========== 客户端测试结束 ==========\n");
//    }
//
//    // 1. 测试基础客户端创建
//    @Test
//    public void testBasicClientCreation() {
//        System.out.println("测试基础客户端创建");
//
//        JQuickConnectionConfig config = JQuickConnectionConfig.defaultConfig();
//        JQuickConnectionStrategy<?> strategy = new JQuickPooledConnectionStrategy<>(config);
//
//        client = new JQuickThriftClient.Builder()
//                .connectionConfig(config)
//                .connectionStrategy(strategy)
//                .build();
//
//        assertNotNull("客户端不应为null", client);
//        System.out.println("客户端创建成功");
//    }
//
//    // 2. 测试获取服务代理
//    @Test
//    public void testGetServiceProxy() {
//        System.out.println("测试获取服务代理");
//
//        JQuickConnectionConfig config = JQuickConnectionConfig.defaultConfig();
//
//        client = new JQuickThriftClient.Builder()
//                .connectionConfig(config)
//                .build();
//
//        // 手动添加服务实例（没有服务发现时）
//        // 注意：实际使用时需要通过服务发现获取实例
//
//        UserService.Iface proxy = client.getService(UserService.Iface.class, "UserService");
//        assertNotNull("服务代理不应为null", proxy);
//
//        System.out.println("服务代理获取成功: " + proxy.getClass().getName());
//    }
//
//    // 3. 测试连接配置
//    @Test
//    public void testConnectionConfig() {
//        System.out.println("测试连接配置");
//
//        JQuickConnectionConfig config = new JQuickConnectionConfig();
//        config.setTimeout(5000);
//        config.setMaxConnections(20);
//        config.setMaxIdle(10);
//        config.setMinIdle(2);
//        config.setMaxRetries(5);
//        config.setFramed(true);
//        config.setProtocolType("compact");
//
//        assertEquals("超时时间应为5000", 5000, config.getTimeout());
//        assertEquals("最大连接数应为20", 20, config.getMaxConnections());
//        assertEquals("最大空闲应为10", 10, config.getMaxIdle());
//        assertEquals("最小空闲应为2", 2, config.getMinIdle());
//        assertEquals("最大重试次数应为5", 5, config.getMaxRetries());
//        assertTrue("应启用帧传输", config.isFramed());
//        assertEquals("协议类型应为compact", "compact", config.getProtocolType());
//
//        System.out.println("连接配置测试通过");
//    }
//
//    // 4. 测试默认连接配置
//    @Test
//    public void testDefaultConnectionConfig() {
//        System.out.println("测试默认连接配置");
//
//        JQuickConnectionConfig config = JQuickConnectionConfig.defaultConfig();
//
//        assertEquals("默认超时应为30000", 30000, config.getTimeout());
//        assertEquals("默认最大连接数应为10", 10, config.getMaxConnections());
//        assertEquals("默认最大空闲应为5", 5, config.getMaxIdle());
//        assertEquals("默认最小空闲应为1", 1, config.getMinIdle());
//        assertEquals("默认最大重试应为3", 3, config.getMaxRetries());
//        assertFalse("默认不应启用帧传输", config.isFramed());
//        assertEquals("默认协议应为binary", "binary", config.getProtocolType());
//
//        System.out.println("默认连接配置测试通过");
//    }
//
//    // 5. 测试客户端统计信息
//    @Test
//    public void testClientStats() {
//        System.out.println("测试客户端统计信息");
//
//        JQuickConnectionConfig config = JQuickConnectionConfig.defaultConfig();
//
//        client = new JQuickThriftClient.Builder()
//                .connectionConfig(config)
//                .build();
//
//        Map<String, Object> stats = client.getStats();
//
//        assertNotNull("统计信息不应为null", stats);
//        assertTrue("应包含failoverCount", stats.containsKey("failoverCount"));
//        assertTrue("应包含proxyCacheSize", stats.containsKey("proxyCacheSize"));
//        assertTrue("应包含instanceCacheSize", stats.containsKey("instanceCacheSize"));
//
//        System.out.println("统计信息: " + stats);
//    }
//
//    // 6. 测试关闭客户端
//    @Test
//    public void testClientClose() {
//        System.out.println("测试客户端关闭");
//
//        JQuickConnectionConfig config = JQuickConnectionConfig.defaultConfig();
//
//        client = new JQuickThriftClient.Builder()
//                .connectionConfig(config)
//                .build();
//
//        assertNotNull("客户端不应为null", client);
//
//        client.close();
//        System.out.println("客户端关闭成功");
//    }
//
//    // 7. 测试客户端重复获取服务代理（应使用缓存）
//    @Test
//    public void testServiceProxyCache() {
//        System.out.println("测试服务代理缓存");
//
//        JQuickConnectionConfig config = JQuickConnectionConfig.defaultConfig();
//
//        client = new JQuickThriftClient.Builder()
//                .connectionConfig(config)
//                .build();
//
//        UserService.Iface proxy1 = client.getService(UserService.Iface.class, "UserService");
//        UserService.Iface proxy2 = client.getService(UserService.Iface.class, "UserService");
//
//        assertSame("两次获取应返回同一个代理对象", proxy1, proxy2);
//
//        Map<String, Object> stats = client.getStats();
//        assertEquals("代理缓存大小应为1", 1, stats.get("proxyCacheSize"));
//
//        System.out.println("服务代理缓存测试通过");
//    }
//
//    // 8. 测试不同负载均衡器
//    @Test
//    public void testLoadBalancers() {
//        System.out.println("测试负载均衡器");
//
//        // 测试轮询负载均衡器
//        JQuickLoadBalancer roundRobin = new JQuickRoundRobinLoadBalancer();
//        assertEquals("名称应为RoundRobin", "RoundRobin", roundRobin.getName());
//
//        // 测试随机负载均衡器
//        JQuickLoadBalancer random = new JQuickRandomLoadBalancer();
//        assertEquals("名称应为Random", "Random", random.getName());
//
//        // 测试加权负载均衡器
//        JQuickLoadBalancer weighted = new JQuickWeightedLoadBalancer();
//        assertEquals("名称应为Weighted", "Weighted", weighted.getName());
//
//        System.out.println("负载均衡器测试通过");
//    }
//
//    // 9. 测试负载均衡器选择实例
//    @Test
//    public void testLoadBalancerSelect() {
//        System.out.println("测试负载均衡器选择实例");
//
//        List<JQuickServiceInstance> instances = Arrays.asList(
//                new JQuickServiceInstance("UserService", "localhost", 9090, 1),
//                new JQuickServiceInstance("UserService", "localhost", 9091, 2),
//                new JQuickServiceInstance("UserService", "localhost", 9092, 3)
//        );
//
//        // 测试轮询
//        JQuickLoadBalancer roundRobin = new JQuickRoundRobinLoadBalancer();
//        JQuickServiceInstance selected = roundRobin.select(instances);
//        assertNotNull("选中的实例不应为null", selected);
//        System.out.println("轮询选中: " + selected.getAddress());
//
//        // 测试随机
//        JQuickLoadBalancer random = new JQuickRandomLoadBalancer();
//        selected = random.select(instances);
//        assertNotNull("选中的实例不应为null", selected);
//        System.out.println("随机选中: " + selected.getAddress());
//
//        // 测试加权
//        JQuickLoadBalancer weighted = new JQuickWeightedLoadBalancer();
//        selected = weighted.select(instances);
//        assertNotNull("选中的实例不应为null", selected);
//        System.out.println("加权选中: " + selected.getAddress());
//    }
//
//    // 10. 测试空实例列表
//    @Test
//    public void testLoadBalancerWithEmptyList() {
//        System.out.println("测试空实例列表");
//
//        List<JQuickServiceInstance> emptyList = Collections.emptyList();
//
//        JQuickLoadBalancer roundRobin = new JQuickRoundRobinLoadBalancer();
//        JQuickServiceInstance selected = roundRobin.select(emptyList);
//        assertNull("空列表应返回null", selected);
//
//        JQuickLoadBalancer random = new JQuickRandomLoadBalancer();
//        selected = random.select(emptyList);
//        assertNull("空列表应返回null", selected);
//
//        System.out.println("空实例列表测试通过");
//    }
//
//    // 11. 测试服务实例创建
//    @Test
//    public void testServiceInstance() {
//        System.out.println("测试服务实例");
//
//        JQuickServiceInstance instance = new JQuickServiceInstance("TestService", "192.168.1.1", 8080, 5);
//        instance.setHealthy(true);
//
//        assertEquals("服务名应为TestService", "TestService", instance.getServiceName());
//        assertEquals("主机应为192.168.1.1", "192.168.1.1", instance.getHost());
//        assertEquals("端口应为8080", 8080, instance.getPort());
//        assertEquals("权重应为5", 5, instance.getWeight());
//        assertEquals("地址应为192.168.1.1:8080", "192.168.1.1:8080", instance.getAddress());
//        assertTrue("应健康", instance.isHealthy());
//
//        System.out.println("服务实例: " + instance);
//    }
//
//    // 12. 测试服务实例相等性
//    @Test
//    public void testServiceInstanceEquality() {
//        System.out.println("测试服务实例相等性");
//
//        JQuickServiceInstance instance1 = new JQuickServiceInstance("UserService", "localhost", 9090);
//        JQuickServiceInstance instance2 = new JQuickServiceInstance("UserService", "localhost", 9090);
//        JQuickServiceInstance instance3 = new JQuickServiceInstance("UserService", "localhost", 9091);
//
//        assertEquals("相同实例应相等", instance1, instance2);
//        assertNotEquals("不同端口实例不应相等", instance1, instance3);
//
//        System.out.println("服务实例相等性测试通过");
//    }
//
//    // 13. 测试使用自定义负载均衡器的客户端构建
//    @Test
//    public void testClientWithCustomLoadBalancer() {
//        System.out.println("测试自定义负载均衡器的客户端构建");
//
//        JQuickConnectionConfig config = JQuickConnectionConfig.defaultConfig();
//        JQuickLoadBalancer customLoadBalancer = new JQuickRandomLoadBalancer();
//
//        client = new JQuickThriftClient.Builder()
//                .connectionConfig(config)
//                .loadBalancer(customLoadBalancer)
//                .build();
//
//        assertNotNull("客户端不应为null", client);
//        System.out.println("自定义负载均衡器客户端创建成功");
//    }
//
//    // 14. 测试使用自定义连接配置的客户端构建
//    @Test
//    public void testClientWithCustomConnectionConfig() {
//        System.out.println("测试自定义连接配置的客户端构建");
//
//        JQuickConnectionConfig config = new JQuickConnectionConfig();
//        config.setTimeout(10000);
//        config.setMaxConnections(30);
//        config.setMaxRetries(3);
//        config.setFramed(true);
//
//        client = new JQuickThriftClient.Builder()
//                .connectionConfig(config)
//                .build();
//
//        assertNotNull("客户端不应为null", client);
//        System.out.println("自定义连接配置客户端创建成功");
//    }
//
//    // 15. 测试不同协议类型的连接配置
//    @Test
//    public void testDifferentProtocolTypes() {
//        System.out.println("测试不同协议类型");
//
//        String[] protocols = {"binary", "compact", "json"};
//
//        for (String protocol : protocols) {
//            JQuickConnectionConfig config = new JQuickConnectionConfig();
//            config.setProtocolType(protocol);
//
//            JQuickThriftClient.Builder builder = new JQuickThriftClient.Builder()
//                    .connectionConfig(config);
//
//            // 不使用服务发现，直接构建
//            client = builder.build();
//            assertNotNull("协议 " + protocol + " 的客户端创建失败", client);
//
//            System.out.println("协议 " + protocol + " 客户端创建成功");
//            client.close();
//        }
//    }
//
//    // 16. 测试健康实例过滤
//    @Test
//    public void testHealthyInstanceFiltering() {
//        System.out.println("测试健康实例过滤");
//
//        JQuickServiceInstance healthyInstance = new JQuickServiceInstance("TestService", "localhost", 9090);
//        healthyInstance.setHealthy(true);
//
//        JQuickServiceInstance unhealthyInstance = new JQuickServiceInstance("TestService", "localhost", 9091);
//        unhealthyInstance.setHealthy(false);
//
//        List<JQuickServiceInstance> instances = Arrays.asList(healthyInstance, unhealthyInstance);
//        List<JQuickServiceInstance> healthyInstances = instances.stream()
//                .filter(JQuickServiceInstance::isHealthy)
//                .collect(Collectors.toList());
//        assertEquals("应有1个健康实例", 1, healthyInstances.size());
//        assertEquals("应为健康实例", healthyInstance, healthyInstances.get(0));
//
//        System.out.println("健康实例过滤测试通过");
//    }
//
//    // 17. 测试客户端构建器链式调用
//    @Test
//    public void testBuilderChaining() {
//        System.out.println("测试构建器链式调用");
//
//        JQuickConnectionConfig config = JQuickConnectionConfig.defaultConfig();
//
//        client = new JQuickThriftClient.Builder()
//                .connectionConfig(config)
//                .loadBalancer(new JQuickRandomLoadBalancer())
//                .build();
//
//        assertNotNull("客户端不应为null", client);
//        System.out.println("构建器链式调用测试通过");
//    }
//}
