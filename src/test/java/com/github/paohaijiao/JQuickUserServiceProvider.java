package com.github.paohaijiao;

import com.github.paohaijiao.config.JQuickThriftServiceConfig;
import com.github.paohaijiao.constants.JQuickThriftPriorityConstants;
import com.github.paohaijiao.provider.JQuickThriftServiceProvider;
import com.github.paohaijiao.spi.anno.Priority;
import org.apache.thrift.TBase;
import org.apache.thrift.TServiceClient;

@Priority(JQuickThriftPriorityConstants.BUSINESS_SERVICE)
public class JQuickUserServiceProvider implements JQuickThriftServiceProvider {

    @Override
    public String getServiceName() {
        return "UserService";
    }

    @Override
    public String getVersion() {
        return "2.1.0";
    }

    @Override
    public Object getProcessor() {
        // return new UserService.Processor<>(new UserServiceImpl());
        return null; // 实际使用时返回实际的 Processor
    }

    @Override
    public Class<? extends TServiceClient> getClientClass() {
        // return UserService.Client.class;
        return null; // 实际使用时返回实际的 Client 类
    }

    @Override
    public Class<? extends TBase<?, ?>> getRequestType() {
        // return UserService.getUser_args.class;
        return null;
    }

    @Override
    public Class<? extends TBase<?, ?>> getResponseType() {
        // return UserService.getUser_result.class;
        return null;
    }

    @Override
    public JQuickThriftServiceConfig getConfig() {
        JQuickThriftServiceConfig config = new JQuickThriftServiceConfig();
        config.setHost("localhost");
        config.setPort(9091);
        config.setTimeout(3000);
        config.setMaxRetries(2);
        config.setMaxPoolSize(30);
        return config;
    }
}
