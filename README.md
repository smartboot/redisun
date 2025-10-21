该项目旨在提供一个轻量级的 Redis 客户端实现，支持异步非阻塞 I/O 操作，适用于 Java 平台。以下是项目的简要说明：

### 特性
- **异步通信**：基于 Java NIO 实现异步非阻塞通信，提高性能。
- **命令支持**：支持常见的 Redis 命令，如 `HELLO` 和 `ZADD`。
- **响应解析**：支持多种 Redis 响应类型解析，包括字符串、整数、数组、哈希表等。
- **连接管理**：自动管理连接池和连接生命周期，确保高效复用。
- **插件扩展**：支持插件机制，便于扩展功能。

### 核心组件
- **Command**：定义 Redis 命令的抽象基类。
- **RedisResponse**：抽象类，定义 Redis 响应的基本结构和解析方法。
- **RedisMessageProcessor**：消息处理器，负责解析接收到的数据并处理响应。
- **RedisSession**：封装会话状态，管理响应解析过程。
- **Redisun**：核心类，提供客户端连接和命令执行功能。

### 使用示例
```java
Redisun redisun = Redisun.create(options -> {
    options.setHost("127.0.0.1");
    options.setPort(6379);
});

try {
    // 执行 Redis 命令
    CompletableFuture<RedisResponse> future = redisun.execute(new HelloCommand());
    RedisResponse response = future.get();
    System.out.println("Server: " + ((HelloCommand.Response) response).getServer());
} catch (Exception e) {
    e.printStackTrace();
} finally {
    redisun.close();
}
```

### 构建与测试
确保已安装 Maven，然后运行以下命令：
```bash
mvn clean package
```

运行测试：
```bash
mvn test
```

### 许可证
该项目使用 MIT 许可证。详情请查看 [LICENSE](LICENSE) 文件。

### 贡献
欢迎提交 Pull Request 和建议。请参阅项目 [issue 跟踪](https://gitee.com/smartboot/redisun/issues) 获取待办事项。

### 联系方式
如有问题或建议，请提交 issue 或联系项目维护者。