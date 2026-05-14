package com.github.paohaijiao.transport;

import com.github.paohaijiao.config.JQuickTransportConfig;
import com.github.paohaijiao.console.JConsole;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

/**
 * 标准传输工厂（非帧传输）
 */
public class JQuickStandardTransportFactory implements JQuickTransportFactory {

    private static final JConsole console = JConsole.getInstance();

    private final JQuickTransportConfig config;

    public JQuickStandardTransportFactory(JQuickTransportConfig config) {
        this.config = config;
        console.debug("JQuickStandardTransportFactory initialized, timeout=" + config.getTimeout() + ", keepAlive=" + config.isKeepAlive());
    }

    @Override
    public TTransport createClientTransport(String host, int port, JQuickTransportConfig cfg) throws Exception {
        JQuickTransportConfig useConfig = cfg != null ? cfg : config;
        console.debug("Creating client transport to " + host + ":" + port);
        TSocket socket = new TSocket(host, port, useConfig.getTimeout());
//        socket.setKeepAlive(useConfig.isKeepAlive());
//        socket.setTcpNoDelay(useConfig.isTcpNoDelay());
        socket.open();
        return socket;
    }

    @Override
    public TServerTransport createServerTransport(int port, JQuickTransportConfig cfg) throws Exception {
        JQuickTransportConfig useConfig = cfg != null ? cfg : config;
        console.debug("Creating server transport on port " + port);
        return new TServerSocket(port);
    }

    @Override
    public String getTransportType() {
        return "standard";
    }

    @Override
    public JQuickTransportConfig getConfig() {
        return config;
    }
}
