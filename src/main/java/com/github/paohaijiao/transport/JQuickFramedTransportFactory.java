package com.github.paohaijiao.transport;

import com.github.paohaijiao.config.JQuickTransportConfig;
import com.github.paohaijiao.console.JConsole;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.layered.TFramedTransport;

/**
 * 帧传输工厂
 */
public class JQuickFramedTransportFactory implements JQuickTransportFactory {

    private static final JConsole console = JConsole.getInstance();

    private final JQuickTransportConfig config;

    public JQuickFramedTransportFactory(JQuickTransportConfig config) {
        this.config = config;
        console.debug("JQuickFramedTransportFactory initialized, maxFrameSize=" + config.getMaxFrameSize());
    }

    @Override
    public TTransport createClientTransport(String host, int port, JQuickTransportConfig cfg) throws Exception {
        JQuickTransportConfig useConfig = cfg != null ? cfg : config;
        console.debug("Creating framed client transport to " + host + ":" + port);
        TSocket socket = new TSocket(host, port, useConfig.getTimeout());
//        socket.setKeepAlive(useConfig.isKeepAlive());
//        socket.setTcpNoDelay(useConfig.isTcpNoDelay());
        socket.open();
        if (useConfig.getMaxFrameSize() > 0) {
            return new TFramedTransport(socket, useConfig.getMaxFrameSize());
        }
        return new TFramedTransport(socket);
    }

    @Override
    public TServerTransport createServerTransport(int port, JQuickTransportConfig cfg) throws Exception {
        JQuickTransportConfig useConfig = cfg != null ? cfg : config;
        console.debug("Creating framed server transport on port " + port);
        return new TServerSocket(port);
    }

    @Override
    public String getTransportType() {
        return "framed";
    }

    @Override
    public JQuickTransportConfig getConfig() {
        return config;
    }
}
