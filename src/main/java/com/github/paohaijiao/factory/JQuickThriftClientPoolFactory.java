package com.github.paohaijiao.factory;

import com.github.paohaijiao.client.JQuickThriftClient;
import com.github.paohaijiao.enums.JQuickProtocolType;
import com.github.paohaijiao.enums.JQuickTransportType;
import com.github.paohaijiao.util.JQuickThriftUtil;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.thrift.TException;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TNonblockingSocket;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.layered.TFramedTransport;

import java.io.IOException;

/**
 * Thrift 客户端连接池工厂
 */
public class JQuickThriftClientPoolFactory<T> extends BasePooledObjectFactory<JQuickThriftClient<T>> {

    private final Class<T> clientClass;

    private final String host;

    private final int port;

    private final JQuickProtocolType protocolType;

    private final JQuickTransportType transportType;

    public JQuickThriftClientPoolFactory(Class<T> clientClass, String host, int port, JQuickProtocolType protocolType, JQuickTransportType transportType) {
        this.clientClass = clientClass;
        this.host = host;
        this.port = port;
        this.protocolType = protocolType;
        this.transportType = transportType;
    }

    @Override
    public JQuickThriftClient<T> create() throws Exception {
        T client = JQuickThriftUtil.createClient(clientClass, host, port, protocolType, transportType);
        TTransport transport = createTransportForClient();
        return new JQuickThriftClient<>(client, transport);
    }

    private TTransport createTransportForClient() throws TException, IOException {
        switch (transportType) {
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

    @Override
    public PooledObject<JQuickThriftClient<T>> wrap(JQuickThriftClient<T> client) {
        return new DefaultPooledObject<>(client);
    }

    @Override
    public void destroyObject(PooledObject<JQuickThriftClient<T>> p) {
        p.getObject().close();
    }

    @Override
    public boolean validateObject(PooledObject<JQuickThriftClient<T>> p) {
        return p.getObject() != null;
    }
}
