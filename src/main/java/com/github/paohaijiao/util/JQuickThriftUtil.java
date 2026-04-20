package com.github.paohaijiao.util;

import com.github.paohaijiao.enums.JQuickProtocolType;
import com.github.paohaijiao.enums.JQuickTransportType;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TNonblockingSocket;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.layered.TFramedTransport;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.concurrent.TimeUnit;

/**
 * Thrift 工具类 - 封装服务端和客户端常用操作
 */
public class JQuickThriftUtil {




    /**
     * 创建 Thrift 客户端连接
     */
    public static <T> T createClient(Class<T> clientClass, String host, int port, JQuickProtocolType protocolType, JQuickTransportType transportType) throws Exception {
        TTransport transport = createTransport(host, port, transportType);
        TProtocol protocol = createProtocol(transport, protocolType);
        // 通过反射创建客户端实例
        Constructor<T> constructor = clientClass.getConstructor(TProtocol.class);
        T client = constructor.newInstance(protocol);

        // 打开连接
        transport.open();

        return client;
    }

    /**
     * 创建传输层
     */
    private static TTransport createTransport(String host, int port, JQuickTransportType type) throws TException, IOException {
        switch (type) {
            case SOCKET:
                return new TSocket(host, port);
            case NONBLOCKING:
                return new TNonblockingSocket(host, port);
            case HTTP:
                return new THttpClient("http://" + host + ":" + port);
            case FRAMED:
                return new TFramedTransport(new TSocket(host, port));
            default:
                return new TSocket(host, port);
        }
    }

    /**
     * 创建协议层
     */
    private static TProtocol createProtocol(TTransport transport, JQuickProtocolType type) {
        switch (type) {
            case BINARY:
                return new TBinaryProtocol(transport);
            case COMPACT:
                return new TCompactProtocol(transport);
            case JSON:
                return new TJSONProtocol(transport);
            default:
                return new TBinaryProtocol(transport);
        }
    }

    /**
     * 安全关闭连接
     */
    public static void closeQuietly(TTransport transport) {
        if (transport != null && transport.isOpen()) {
            transport.close();
        }
    }

    /**
     * 客户端调用接口
     */
    @FunctionalInterface
    public interface ThriftCallable<T, R> {
        R call(T client) throws Exception;
    }

    /**
     * Thrift 客户端包装类
     */
    public static class ThriftClient<T> implements AutoCloseable {
        private final T client;
        private final TTransport transport;

        public ThriftClient(T client, TTransport transport) {
            this.client = client;
            this.transport = transport;
        }

        public T getClient() {
            return client;
        }

        @Override
        public void close() {
            closeQuietly(transport);
        }
    }
}