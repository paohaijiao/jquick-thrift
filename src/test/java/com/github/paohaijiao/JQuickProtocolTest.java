package com.github.paohaijiao;

import com.github.paohaijiao.config.JQuickProtocolConfig;
import com.github.paohaijiao.console.JConsole;
import com.github.paohaijiao.protocol.JQuickBinaryProtocolFactory;
import com.github.paohaijiao.protocol.JQuickCompactProtocolFactory;
import com.github.paohaijiao.protocol.JQuickJsonProtocolFactory;
import com.github.paohaijiao.protocol.JQuickProtocolFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 协议工厂测试
 */
public class JQuickProtocolTest {

    private static final JConsole console = JConsole.getInstance();

    @Before
    public void setUp() {
        console.info("========== 协议测试开始 ==========");
    }

    @After
    public void tearDown() {
        console.info("========== 协议测试结束 ==========");
    }

    @Test
    public void testBinaryProtocolFactory() {
        console.info("测试 Binary 协议工厂");
        JQuickProtocolConfig config = JQuickProtocolConfig.binary();
        JQuickProtocolFactory factory = new JQuickBinaryProtocolFactory(config);
        assertEquals("协议类型应为 binary", "binary", factory.getProtocolType());
        assertNotNull("协议工厂不应为 null", factory);
        console.info("Binary 协议工厂创建成功");
    }

    @Test
    public void testCompactProtocolFactory() {
        console.info("测试 Compact 协议工厂");
        JQuickProtocolConfig config = JQuickProtocolConfig.compact();
        JQuickProtocolFactory factory = new JQuickCompactProtocolFactory(config);
        assertEquals("协议类型应为 compact", "compact", factory.getProtocolType());
        assertNotNull("协议工厂不应为 null", factory);
        console.info("Compact 协议工厂创建成功");
    }

    @Test
    public void testJsonProtocolFactory() {
        console.info("测试 JSON 协议工厂");
        JQuickProtocolConfig config = JQuickProtocolConfig.json();
        JQuickProtocolFactory factory = new JQuickJsonProtocolFactory(config);
        assertEquals("协议类型应为 json", "json", factory.getProtocolType());
        assertNotNull("协议工厂不应为 null", factory);
        console.info("JSON 协议工厂创建成功");
    }

    @Test
    public void testProtocolConfig() {
        console.info("测试协议配置");
        JQuickProtocolConfig config = new JQuickProtocolConfig();
        config.setType("binary");
        config.setStrictRead(true);
        config.setStrictWrite(true);
        config.setStringLengthLimit(1024);
        config.setContainerLengthLimit(10240);
        assertEquals("类型应为 binary", "binary", config.getType());
        assertTrue("strictRead 应为 true", config.isStrictRead());
        assertTrue("strictWrite 应为 true", config.isStrictWrite());
        assertEquals("stringLengthLimit 应为 1024", 1024, config.getStringLengthLimit());
        console.info("协议配置测试通过");
    }

    @Test
    public void testBinaryProtocolConfigDefaults() {
        console.info("测试 Binary 协议默认配置");
        JQuickProtocolConfig config = JQuickProtocolConfig.binary();
        assertEquals("默认类型应为 binary", "binary", config.getType());
        assertTrue("默认 strictRead 应为 true", config.isStrictRead());
        assertTrue("默认 strictWrite 应为 true", config.isStrictWrite());
        console.info("Binary 协议默认配置测试通过");
    }
}
