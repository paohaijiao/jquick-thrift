package com.github.paohaijiao;

import com.example.thrift.demo.UserService;
import com.github.paohaijiao.client.JQuickThriftClient;
import com.github.paohaijiao.config.*;
import com.github.paohaijiao.console.JConsole;
import com.github.paohaijiao.discovery.impl.JQuickInMemoryServiceDiscovery;
import com.github.paohaijiao.domain.JQuickServiceInstance;
import com.github.paohaijiao.loadBalence.JQuickLoadBalancer;
import com.github.paohaijiao.loadBalence.impl.JQuickRandomLoadBalancer;
import com.github.paohaijiao.loadBalence.impl.JQuickRoundRobinLoadBalancer;
import com.github.paohaijiao.loadBalence.impl.JQuickWeightedLoadBalancer;
import com.github.paohaijiao.manager.JQuickDynamicFactory;
import com.github.paohaijiao.server.JQuickThriftServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * 客户端测试
 */
public class JQuickClientTest {

    private static final JConsole console = JConsole.getInstance();

    private static final int TEST_PORT = 9092;

    private JQuickThriftServer server;

    private JQuickThriftClient client;

    private JQuickDynamicFactory factory;

    private JQuickInMemoryServiceDiscovery discovery;

    @Before
    public void setUp() throws Exception {
        console.info("初始化测试环境");
        discovery = new JQuickInMemoryServiceDiscovery();
        discovery.registerInstance("TestService", "localhost", 9090);
        factory = new JQuickDynamicFactory();
        JQuickTransportConfig transportConfig = factory.getActiveTransportConfig();
        transportConfig.setTransportType("standard");  // 或 "framed"
        JQuickProtocolConfig protocolConfig = factory.getActiveProtocolConfig();
        if (protocolConfig.getType() == null) {
            protocolConfig.setType("binary");
        }
        console.info("测试环境初始化完成");
    }

    @After
    public void tearDown() {
        if (client != null) {
            client.close();
            console.info("客户端已关闭");
        }
        if (server != null && server.isRunning()) {
            server.stop();
            console.info("服务端已停止");
        }
        console.info("========== 客户端测试结束 ==========");
    }

    @Test
    public void testPooledClientCreation() {
        console.info("测试连接池客户端创建");
        JQuickClientConfig config = JQuickClientConfig.pooled();
        JQuickConnectionConfig connectionConfig = JQuickConnectionConfig.defaultConfig();
        client = factory.createClient(config, discovery, null, null, connectionConfig);
        assertNotNull("客户端不应为 null", client);
        assertEquals("客户端类型应为 pooled", "pooled", client.getClientType());
        console.info("连接池客户端创建成功");
    }

    @Test
    public void testSingleClientCreation() {
        console.info("测试单连接客户端创建");
        JQuickClientConfig config = JQuickClientConfig.single();
        JQuickConnectionConfig connectionConfig = JQuickConnectionConfig.defaultConfig();
        client = factory.createClient(config, discovery, null, null, connectionConfig);
        assertNotNull("客户端不应为 null", client);
        assertEquals("客户端类型应为 single", "single", client.getClientType());
        console.info("单连接客户端创建成功");
    }

    @Test
    public void testClientWithRoundRobinLoadBalancer() {
        console.info("测试轮询负载均衡客户端");
        JQuickClientConfig config = JQuickClientConfig.pooled();
        JQuickLoadBalancer loadBalancer = new JQuickRoundRobinLoadBalancer();
        JQuickConnectionConfig connectionConfig = JQuickConnectionConfig.defaultConfig();
        client = factory.createClient(config, discovery, loadBalancer, null, connectionConfig);
        assertNotNull("客户端不应为 null", client);
        assertEquals("负载均衡器名称应为 RoundRobin", "RoundRobin", loadBalancer.getName());
        console.info("轮询负载均衡客户端创建成功");
    }

    @Test
    public void testClientWithRandomLoadBalancer() {
        console.info("测试随机负载均衡客户端");
        JQuickClientConfig config = JQuickClientConfig.pooled();
        JQuickLoadBalancer loadBalancer = new JQuickRandomLoadBalancer();
        JQuickConnectionConfig connectionConfig = JQuickConnectionConfig.defaultConfig();
        client = factory.createClient(config, discovery, loadBalancer, null, connectionConfig);
        assertNotNull("客户端不应为 null", client);
        assertEquals("负载均衡器名称应为 Random", "Random", loadBalancer.getName());
        console.info("随机负载均衡客户端创建成功");
    }

    @Test
    public void testClientWithWeightedLoadBalancer() {
        console.info("测试加权负载均衡客户端");
        JQuickClientConfig config = JQuickClientConfig.pooled();
        JQuickLoadBalancer loadBalancer = new JQuickWeightedLoadBalancer();
        JQuickConnectionConfig connectionConfig = JQuickConnectionConfig.defaultConfig();
        client = factory.createClient(config, discovery, loadBalancer, null, connectionConfig);
        assertNotNull("客户端不应为 null", client);
        assertEquals("负载均衡器名称应为 Weighted", "Weighted", loadBalancer.getName());
        console.info("加权负载均衡客户端创建成功");
    }

    @Test
    public void testGetServiceProxy() {
        console.info("测试获取服务代理");
        JQuickClientConfig config = JQuickClientConfig.pooled();
        JQuickConnectionConfig connectionConfig = JQuickConnectionConfig.defaultConfig();
        client = factory.createClient(config, discovery, null, null, connectionConfig);
        UserService.Iface proxy = client.getService(UserService.Iface.class, "UserService");
        assertNotNull("服务代理不应为 null", proxy);
        console.info("服务代理获取成功: " + proxy.getClass().getName());
    }

    @Test
    public void testServiceProxyCache() {
        console.info("测试服务代理缓存");
        JQuickClientConfig config = JQuickClientConfig.pooled();
        JQuickConnectionConfig connectionConfig = JQuickConnectionConfig.defaultConfig();
        client = factory.createClient(config, discovery, null, null, connectionConfig);
        UserService.Iface proxy1 = client.getService(UserService.Iface.class, "UserService");
        UserService.Iface proxy2 = client.getService(UserService.Iface.class, "UserService");
        assertSame("两次获取应返回同一个代理对象", proxy1, proxy2);
        Map<String, Object> stats = client.getStats();
        assertEquals("代理缓存大小应为 1", 1, stats.get("proxyCacheSize"));
        console.info("服务代理缓存测试通过");
    }

    @Test
    public void testClientStats() {
        console.info("测试客户端统计信息");
        JQuickClientConfig config = JQuickClientConfig.pooled();
        JQuickConnectionConfig connectionConfig = JQuickConnectionConfig.defaultConfig();
        client = factory.createClient(config, discovery, null, null, connectionConfig);
        Map<String, Object> stats = client.getStats();
        assertNotNull("统计信息不应为 null", stats);
        assertTrue("应包含 failoverCount", stats.containsKey("failoverCount"));
        assertTrue("应包含 proxyCacheSize", stats.containsKey("proxyCacheSize"));
        assertTrue("应包含 instanceCacheSize", stats.containsKey("instanceCacheSize"));
        console.info("统计信息: " + stats);
    }

    @Test
    public void testClientClose() {
        console.info("测试客户端关闭");
        JQuickClientConfig config = JQuickClientConfig.pooled();
        JQuickConnectionConfig connectionConfig = JQuickConnectionConfig.defaultConfig();
        client = factory.createClient(config, discovery, null, null, connectionConfig);
        assertNotNull("客户端不应为 null", client);
        assertFalse("客户端不应已关闭", client.isClosed());
        client.close();
        assertTrue("客户端应已关闭", client.isClosed());
        console.info("客户端关闭成功");
    }

    @Test
    public void testConnectionConfig() {
        console.info("测试连接配置");
        JQuickConnectionConfig config = new JQuickConnectionConfig();
        config.setTimeout(5000);
        config.setMaxConnections(20);
        config.setMaxIdle(10);
        config.setMinIdle(2);
        config.setMaxRetries(5);
        config.setFramed(true);
        config.setProtocolType("compact");
        assertEquals("超时时间应为 5000", 5000, config.getTimeout());
        assertEquals("最大连接数应为 20", 20, config.getMaxConnections());
        assertEquals("最大空闲应为 10", 10, config.getMaxIdle());
        assertEquals("最小空闲应为 2", 2, config.getMinIdle());
        assertEquals("最大重试次数应为 5", 5, config.getMaxRetries());
        assertTrue("应启用帧传输", config.isFramed());
        assertEquals("协议类型应为 compact", "compact", config.getProtocolType());
        console.info("连接配置测试通过");
    }

    @Test
    public void testDefaultConnectionConfig() {
        console.info("测试默认连接配置");
        JQuickConnectionConfig config = JQuickConnectionConfig.defaultConfig();
        assertEquals("默认超时应为 30000", 30000, config.getTimeout());
        assertEquals("默认最大连接数应为 10", 10, config.getMaxConnections());
        assertEquals("默认最大空闲应为 5", 5, config.getMaxIdle());
        assertEquals("默认最小空闲应为 1", 1, config.getMinIdle());
        assertEquals("默认最大重试应为 3", 3, config.getMaxRetries());
        assertFalse("默认不应启用帧传输", config.isFramed());
        assertEquals("默认协议应为 binary", "binary", config.getProtocolType());
        console.info("默认连接配置测试通过");
    }

    @Test
    public void testClientConfig() {
        console.info("测试客户端配置");
        JQuickClientConfig config = new JQuickClientConfig();
        config.setClientType("pooled");
        config.setMaxRetries(5);
        config.setMultiplexed(true);
        config.setLoadBalancer("weighted");
        assertEquals("客户端类型应为 pooled", "pooled", config.getClientType());
        assertEquals("最大重试应为 5", 5, config.getMaxRetries());
        assertTrue("multiplexed 应为 true", config.isMultiplexed());
        assertEquals("负载均衡器应为 weighted", "weighted", config.getLoadBalancer());
        console.info("客户端配置测试通过");
    }

    @Test
    public void testLoadBalancerSelect() {
        console.info("测试负载均衡器选择实例");
        List<JQuickServiceInstance> instances = Arrays.asList(
                new JQuickServiceInstance("UserService", "localhost", 9090, 1),
                new JQuickServiceInstance("UserService", "localhost", 9091, 2),
                new JQuickServiceInstance("UserService", "localhost", 9092, 3)
        );
        JQuickLoadBalancer roundRobin = new JQuickRoundRobinLoadBalancer();
        JQuickServiceInstance selected = roundRobin.select(instances);
        assertNotNull("选中的实例不应为 null", selected);
        console.info("轮询选中: " + selected.getAddress());
        JQuickLoadBalancer random = new JQuickRandomLoadBalancer();
        selected = random.select(instances);
        assertNotNull("选中的实例不应为 null", selected);
        console.info("随机选中: " + selected.getAddress());
        JQuickLoadBalancer weighted = new JQuickWeightedLoadBalancer();
        selected = weighted.select(instances);
        assertNotNull("选中的实例不应为 null", selected);
        console.info("加权选中: " + selected.getAddress());
    }
}
