namespace java com.example.thrift.demo

// 用户实体
struct User {
    1: i32 id,
    2: string name,
    3: string email,
    4: i32 age
}

// 响应结果
struct Response {
    1: i32 code,
    2: string message,
    3: string data
}

// 用户服务接口
service UserService {
    // 获取用户
    User getUser(1: i32 userId),

    // 保存用户
    Response saveUser(1: User user),

    // 删除用户
    Response deleteUser(1: i32 userId),

    // 获取所有用户
    list<User> listAllUsers(),

    // 打招呼
    string sayHello(1: string name)
}