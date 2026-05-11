package com.github.paohaijiao;

import com.example.thrift.demo.Response;
import com.example.thrift.demo.User;
import com.example.thrift.demo.UserServiceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

/**
 * 服务实现类的单元测试
 */
public class UserServiceImplTest {

    private UserServiceImpl userService;

    @Before
    public void setUp() {
        userService = new UserServiceImpl();
        System.out.println("\n========== 开始测试 ==========");
    }

    @After
    public void tearDown() {
        System.out.println("========== 测试结束 ==========\n");
    }

    @Test
    public void testSayHello() {
        System.out.println("测试 sayHello 方法");
        String result = userService.sayHello("JUnit");
        assertNotNull("返回结果不应为null", result);
        assertTrue("结果应包含'Hello'", result.contains("Hello"));
        assertTrue("结果应包含参数'JUnit'", result.contains("JUnit"));
        System.out.println("返回结果: " + result);
    }

    // 2. 测试获取存在的用户
    @Test
    public void testGetUser_Success() {
        System.out.println("测试获取存在的用户");

        User user = userService.getUser(1);

        assertNotNull("用户不应为null", user);
        assertEquals("用户ID应为1", 1, user.getId());
        assertEquals("用户名应为'张三'", "张三", user.getName());
        assertEquals("邮箱应为'zhangsan@example.com'", "zhangsan@example.com", user.getEmail());
        assertEquals("年龄应为25", 25, user.getAge());

        System.out.println("获取到用户: " + user.getName());
    }

    // 3. 测试获取不存在的用户（应抛出异常）
    @Test(expected = RuntimeException.class)
    public void testGetUser_NotFound() {
        System.out.println("测试获取不存在的用户（应抛出异常）");

        userService.getUser(999);

        // 不应执行到这里
        fail("应该抛出RuntimeException");
    }

    // 4. 测试保存新用户
    @Test
    public void testSaveUser_Create() {
        System.out.println("测试创建新用户");

        User newUser = new User();
        newUser.setName("王五");
        newUser.setEmail("wangwu@example.com");
        newUser.setAge(28);

        Response response = userService.saveUser(newUser);

        assertNotNull("响应不应为null", response);
        assertEquals("响应码应为200", 200, response.getCode());
        assertTrue("消息应包含'成功'", response.getMessage().contains("成功"));
        assertNotNull("数据不应为null", response.getData());

        // 验证用户是否真的被保存了
        User savedUser = userService.getUser(3);
        assertEquals("保存的用户名应为'王五'", "王五", savedUser.getName());

        System.out.println("创建结果: " + response.getMessage());
        System.out.println("用户ID: " + savedUser.getId());
    }

    // 5. 测试更新已存在的用户
    @Test
    public void testSaveUser_Update() {
        System.out.println("测试更新已存在的用户");

        // 先获取用户
        User existingUser = userService.getUser(1);
        existingUser.setName("张三（已更新）");
        existingUser.setAge(26);

        // 更新用户
        Response response = userService.saveUser(existingUser);

        assertEquals("响应码应为200", 200, response.getCode());
        assertTrue("消息应包含'成功'", response.getMessage().contains("成功"));

        // 验证更新是否生效
        User updatedUser = userService.getUser(1);
        assertEquals("用户名已更新", "张三（已更新）", updatedUser.getName());
        assertEquals("年龄已更新", 26, updatedUser.getAge());

        System.out.println("更新结果: " + response.getMessage());
    }

    // 6. 测试更新不存在的用户
    @Test
    public void testSaveUser_UpdateNotFound() {
        System.out.println("测试更新不存在的用户");

        User nonExistUser = new User();
        nonExistUser.setId(999);
        nonExistUser.setName("不存在的用户");
        nonExistUser.setEmail("none@example.com");
        nonExistUser.setAge(20);

        Response response = userService.saveUser(nonExistUser);

        assertEquals("响应码应为404", 404, response.getCode());
        assertTrue("消息应包含'不存在'", response.getMessage().contains("不存在"));

        System.out.println("更新结果: " + response.getMessage());
    }

    // 7. 测试删除存在的用户
    @Test
    public void testDeleteUser_Success() {
        System.out.println("测试删除存在的用户");

        // 先查看删除前的数量
        List<User> beforeUsers = userService.listAllUsers();
        int beforeCount = beforeUsers.size();

        // 删除用户2
        Response response = userService.deleteUser(2);

        assertEquals("响应码应为200", 200, response.getCode());
        assertTrue("消息应包含'成功'", response.getMessage().contains("成功"));
        assertTrue("数据应包含'李四'", response.getData().contains("李四"));

        // 验证删除后的数量
        List<User> afterUsers = userService.listAllUsers();
        assertEquals("用户数量应减少1", beforeCount - 1, afterUsers.size());

        // 验证用户2已不存在
        try {
            userService.getUser(2);
            fail("应该抛出用户不存在的异常");
        } catch (RuntimeException e) {
            assertEquals("异常信息应为'用户不存在: 2'", "用户不存在: 2", e.getMessage());
        }

        System.out.println("删除结果: " + response.getMessage());
    }

    // 8. 测试删除不存在的用户
    @Test
    public void testDeleteUser_NotFound() {
        System.out.println("测试删除不存在的用户");

        Response response = userService.deleteUser(999);

        assertEquals("响应码应为404", 404, response.getCode());
        assertTrue("消息应包含'不存在'", response.getMessage().contains("不存在"));

        System.out.println("删除结果: " + response.getMessage());
    }

    // 9. 测试获取所有用户
    @Test
    public void testListAllUsers() {
        System.out.println("测试获取所有用户");

        List<User> users = userService.listAllUsers();

        assertNotNull("用户列表不应为null", users);
        assertEquals("应有2个初始用户", 2, users.size());

        // 验证用户内容
        User user1 = users.get(0);
        assertEquals("第一个用户应为ID=1", 1, user1.getId());

        User user2 = users.get(1);
        assertEquals("第二个用户应为ID=2", 2, user2.getId());

        System.out.println("用户列表大小: " + users.size());
    }

    // 10. 测试连续操作
    @Test
    public void testMultipleOperations() {
        System.out.println("测试连续操作");

        // 1. 初始状态
        List<User> initialUsers = userService.listAllUsers();
        assertEquals("初始应有2个用户", 2, initialUsers.size());

        // 2. 添加用户
        User user3 = new User();
        user3.setName("赵六");
        user3.setEmail("zhaoliu@example.com");
        user3.setAge(32);
        Response saveResp = userService.saveUser(user3);
        assertEquals("添加成功", 200, saveResp.getCode());

        // 3. 验证添加后数量
        List<User> afterAdd = userService.listAllUsers();
        assertEquals("添加后应有3个用户", 3, afterAdd.size());

        // 4. 更新用户
        user3.setName("赵六（更新）");
        Response updateResp = userService.saveUser(user3);
        assertEquals("更新成功", 200, updateResp.getCode());

        // 5. 验证更新
        User updated = userService.getUser(3);
        assertEquals("用户名已更新", "赵六（更新）", updated.getName());

        // 6. 删除用户
        Response deleteResp = userService.deleteUser(2);
        assertEquals("删除成功", 200, deleteResp.getCode());

        // 7. 验证最终状态
        List<User> finalUsers = userService.listAllUsers();
        assertEquals("最终应有2个用户", 2, finalUsers.size());

        System.out.println("连续操作测试通过");
    }

    // 11. 测试保存带空字段的用户
    @Test
    public void testSaveUserWithEmptyFields() {
        System.out.println("测试保存带空字段的用户");

        User user = new User();
        user.setName("");
        user.setEmail("");
        user.setAge(0);

        Response response = userService.saveUser(user);

        // 应该能保存成功，因为服务端没有做验证
        assertEquals("应该保存成功", 200, response.getCode());

        System.out.println("保存结果: " + response.getMessage());
    }

    // 12. 测试统计信息
    @Test
    public void testGetStatistics() {
        System.out.println("测试获取统计信息");
        Map<String, Object> stats = userService.getStatistics();
        assertNotNull("统计信息不应为null", stats);
        assertEquals("用户总数应为2", 2, stats.get("totalUsers"));
        assertEquals("下一个ID应为3", 3, stats.get("nextId"));
        System.out.println("统计信息: " + stats);
    }

    // 13. 测试重置数据
    @Test
    public void testResetData() {
        System.out.println("测试重置数据");
        // 先添加一个用户
        User newUser = new User();
        newUser.setName("临时用户");
        newUser.setEmail("temp@example.com");
        newUser.setAge(18);
        userService.saveUser(newUser);
        // 验证添加后数量
        List<User> beforeReset = userService.listAllUsers();
        assertEquals("添加后应有3个用户", 3, beforeReset.size());
        // 重置数据
        userService.resetTestData();
        // 验证重置后只有初始的2个用户
        List<User> afterReset = userService.listAllUsers();
        assertEquals("重置后应有2个用户", 2, afterReset.size());
        System.out.println("数据重置测试通过");
    }
}
