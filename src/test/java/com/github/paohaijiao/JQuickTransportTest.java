package com.github.paohaijiao;

import com.github.paohaijiao.config.JQuickTransportConfig;
import com.github.paohaijiao.console.JConsole;
import com.github.paohaijiao.transport.JQuickFramedTransportFactory;
import com.github.paohaijiao.transport.JQuickStandardTransportFactory;
import com.github.paohaijiao.transport.JQuickTransportFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 传输层测试
 */
public class JQuickTransportTest {

    private static final JConsole console = JConsole.getInstance();
    private static final int TEST_PORT = 9095;

    @Before
    public void setUp() {
        console.info("========== 传输层测试开始 ==========");
    }

    @After
    public void tearDown() {
        console.info("========== 传输层测试结束 ==========");
    }

    @Test
    public void testStandardTransportFactory() throws Exception {
        console.info("测试标准传输工厂");
        JQuickTransportConfig config = JQuickTransportConfig.standard();
        JQuickTransportFactory factory = new JQuickStandardTransportFactory(config);
        assertEquals("传输类型应为 standard", "standard", factory.getTransportType());
        assertNotNull("传输工厂不应为 null", factory);
        console.info("标准传输工厂创建成功");
    }

    @Test
    public void testFramedTransportFactory() throws Exception {
        console.info("测试帧传输工厂");
        JQuickTransportConfig config = JQuickTransportConfig.framed();
        JQuickTransportFactory factory = new JQuickFramedTransportFactory(config);
        assertEquals("传输类型应为 framed", "framed", factory.getTransportType());
        assertNotNull("传输工厂不应为 null", factory);
        console.info("帧传输工厂创建成功");
    }

    @Test
    public void testTransportConfig() {
        console.info("测试传输配置");
        JQuickTransportConfig config = new JQuickTransportConfig();
        config.setTimeout(10000);
        config.setFramed(true);
        config.setMaxFrameSize(2 * 1024 * 1024);
        config.setKeepAlive(true);
        config.setTcpNoDelay(true);
        assertEquals("超时应为 10000", 10000, config.getTimeout());
        assertTrue("framed 应为 true", config.isFramed());
        assertEquals("maxFrameSize 应为 2MB", 2 * 1024 * 1024, config.getMaxFrameSize());
        assertTrue("keepAlive 应为 true", config.isKeepAlive());
        console.info("传输配置测试通过");
    }

    @Test
    public void testDefaultTransportConfig() {
        console.info("测试默认传输配置");
        JQuickTransportConfig config = JQuickTransportConfig.standard();
        assertEquals("默认超时应为 30000", 30000, config.getTimeout());
        assertFalse("默认 framed 应为 false", config.isFramed());
        assertTrue("默认 keepAlive 应为 true", config.isKeepAlive());
        assertTrue("默认 tcpNoDelay 应为 true", config.isTcpNoDelay());
        console.info("默认传输配置测试通过");
    }

    @Test
    public void testFramedTransportConfig() {
        console.info("测试帧传输配置");
        JQuickTransportConfig config = JQuickTransportConfig.framed();
        assertTrue("framed 应为 true", config.isFramed());
        console.info("帧传输配置测试通过");
    }
}