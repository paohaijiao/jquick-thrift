package com.example.thrift.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用户服务实现类
 * 实现 Thrift 生成的 UserService.Iface 接口
 */
public class UserServiceImpl implements UserService.Iface {

    private final Map<Integer, User> database = new ConcurrentHashMap<>();

    private final AtomicInteger idGenerator = new AtomicInteger(1);

    /**
     * 构造函数 - 初始化测试数据
     */
    public UserServiceImpl() {
        initTestData();
    }

    /**
     * 初始化测试数据
     */
    private void initTestData() {
        // 创建用户1
        User user1 = new User();
        user1.setId(idGenerator.getAndIncrement());
        user1.setName("张三");
        user1.setEmail("zhangsan@example.com");
        user1.setAge(25);
        database.put(user1.getId(), user1);

        // 创建用户2
        User user2 = new User();
        user2.setId(idGenerator.getAndIncrement());
        user2.setName("李四");
        user2.setEmail("lisi@example.com");
        user2.setAge(30);
        database.put(user2.getId(), user2);

        System.out.println("初始化数据完成，共 " + database.size() + " 条记录");
        System.out.println("用户列表: " + database.keySet());
    }

    /**
     * 根据用户ID获取用户信息
     * @param userId 用户ID
     * @return 用户对象
     * @throws RuntimeException 如果用户不存在
     */
    @Override
    public User getUser(int userId) {
        System.out.println("查询用户: " + userId);

        User user = database.get(userId);
        if (user == null) {
            System.err.println("用户不存在: " + userId);
            throw new RuntimeException("用户不存在: " + userId);
        }

        System.out.println("返回用户: " + user.getName());
        return user;
    }

    /**
     * 保存用户信息（新增或更新）
     * @param user 用户对象
     * @return 响应结果
     */
    @Override
    public Response saveUser(User user) {
        System.out.println("保存用户: " + user.getName() + ", ID: " + user.getId());

        Response response = new Response();

        try {
            if (user.getId() <= 0) {
                // 新增用户
                int newId = idGenerator.getAndIncrement();
                user.setId(newId);
                database.put(newId, user);

                response.setCode(200);
                response.setMessage("用户创建成功");
                response.setData("用户ID: " + newId);

                System.out.println("新增用户成功: " + user.getName() + " (ID: " + newId + ")");

            } else {
                // 更新用户
                if (database.containsKey(user.getId())) {
                    database.put(user.getId(), user);

                    response.setCode(200);
                    response.setMessage("用户更新成功");
                    response.setData("用户ID: " + user.getId());

                    System.out.println("更新用户成功: " + user.getName() + " (ID: " + user.getId() + ")");

                } else {
                    response.setCode(404);
                    response.setMessage("用户不存在，无法更新");
                    response.setData("");

                    System.err.println("更新失败，用户不存在: ID=" + user.getId());
                }
            }
        } catch (Exception e) {
            System.err.println("保存用户失败: " + e.getMessage());
            e.printStackTrace();

            response.setCode(500);
            response.setMessage("保存失败: " + e.getMessage());
            response.setData("");
        }

        return response;
    }

    /**
     * 删除用户
     * @param userId 用户ID
     * @return 响应结果
     */
    @Override
    public Response deleteUser(int userId) {
        System.out.println("删除用户: " + userId);

        Response response = new Response();

        User removed = database.remove(userId);
        if (removed != null) {
            response.setCode(200);
            response.setMessage("删除成功");
            response.setData("已删除用户: " + removed.getName());

            System.out.println("删除用户成功: " + removed.getName() + " (ID: " + userId + ")");

        } else {
            response.setCode(404);
            response.setMessage("用户不存在");
            response.setData("");

            System.err.println("删除失败，用户不存在: ID=" + userId);
        }

        return response;
    }

    /**
     * 获取所有用户列表
     * @return 用户列表
     */
    @Override
    public List<User> listAllUsers() {
        System.out.println("获取所有用户，当前总数: " + database.size());

        List<User> userList = new ArrayList<>(database.values());

        // 打印用户列表（用于调试）
        for (User user : userList) {
            System.out.println("  - " + user.getId() + ": " + user.getName() +
                    " (" + user.getAge() + "岁, " + user.getEmail() + ")");
        }

        return userList;
    }

    /**
     * 打招呼
     * @param name 名称
     * @return 问候语
     */
    @Override
    public String sayHello(String name) {
        System.out.println("收到打招呼请求: " + name);

        String message = "Hello, " + name + "! 欢迎使用Thrift!";
        System.out.println("返回消息: " + message);

        return message;
    }

    /**
     * 获取数据库统计信息（辅助方法，不在Thrift接口中）
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalUsers", database.size());
        stats.put("nextId", idGenerator.get());
        stats.put("databaseKeys", new ArrayList<>(database.keySet()));
        return stats;
    }

    /**
     * 清空所有数据（辅助方法，用于测试）
     */
    public void clearAllData() {
        database.clear();
        idGenerator.set(1);
        System.out.println("已清空所有数据");
    }

    /**
     * 重置测试数据（辅助方法，用于测试）
     */
    public void resetTestData() {
        clearAllData();
        initTestData();
        System.out.println("已重置测试数据");
    }
}
