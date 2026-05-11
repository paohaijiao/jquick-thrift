package com.github.paohaijiao.pool;


import com.github.paohaijiao.spi.anno.Priority;

/**
 * 序列化器SPI接口
 * 支持不同的序列化协议
 */
@Priority(300)
public interface JQuickSerializer {

    /**
     * 序列化对象
     */
    byte[] serialize(Object obj) throws Exception;

    /**
     * 反序列化对象
     */
    <T> T deserialize(byte[] data, Class<T> clazz) throws Exception;

    /**
     * 获取序列化协议名称
     */
    String getProtocolName();
}
