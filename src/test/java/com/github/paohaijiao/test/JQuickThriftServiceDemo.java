package com.github.paohaijiao.test;

import com.github.paohaijiao.manager.JQuickDynamicThriftServiceManager;

public class JQuickThriftServiceDemo {

    public static void main(String[] args) {
        JQuickDynamicThriftServiceManager manager = JQuickDynamicThriftServiceManager.getInstance();
        manager.printAllServices();
        UserService.Client userClient = manager.getClient("UserService");
        try {
            User user = userClient.getUser(123L);
            System.out.println("用户信息: " + user);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 方式2：使用执行器（自动管理连接）
        User result = manager.execute("UserService", client -> {
            return ((UserService.Client) client).getUser(456L);
        });

        // 方式3：获取服务信息
        ThriftServiceInfo info = manager.getServiceInfo("UserService");
        System.out.println("服务信息: " + info);

        // 方式4：动态重新加载服务（热部署）
        // manager.reload();
    }
}
