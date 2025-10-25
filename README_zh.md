![](./logo.svg)

<div align="center">

[![Maven Central](https://img.shields.io/maven-central/v/tech.smartboot/redisun)](https://central.sonatype.com/artifact/tech.smartboot/redisun)
[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://gitee.com/smartboot/redisun/blob/master/LICENSE)

</div>

Redisun 是一个基于 [smart-socket](https://gitee.com/smartboot/smart-socket) 开发的轻量级 Redis 客户端，专为 Java 平台设计，支持异步非阻塞 I/O 操作，提供高性能的 Redis 连接和命令执行能力。

## 特性

- **轻量级设计**：核心代码仅由少量类组成，jar包仅33KB，资源占用极小
- **高性能通信**：基于 smart-socket Java AIO 实现，单线程处理能力强，资源消耗低
- **连接复用技术**：创新的连接复用机制，一个连接可同时处理多个并发请求
- **RESP 协议支持**：完整支持 Redis 序列化协议（RESP），兼容 Redis 服务器
- **命令扩展机制**：提供简单易用的命令扩展接口，方便添加自定义 Redis 命令
- **多数据库支持**：支持 Redis 多数据库切换
- **认证支持**：支持 Redis 服务器的用户名/密码认证

## 核心组件

- **Redisun**：核心客户端类，提供连接管理和命令执行功能
- **Command**：Redis 命令的抽象基类，支持自定义命令扩展
- **RESP**：Redis 序列化协议解析器，支持多种数据类型
- **RedisMessageProcessor**：消息处理器，负责解析 Redis 服务器响应
- **RedisSession**：会话管理器，维护客户端与服务器的会话状态
- **RedisunOptions**：客户端配置选项类，用于设置连接参数

## 安装

### Maven

```xml
<dependency>
    <groupId>tech.smartboot</groupId>
    <artifactId>redisun</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'tech.smartboot:redisun:1.0.0'
```

## 快速开始

### 基本用法

```java
import tech.smartboot.redisun.Redisun;

Redisun redisun = Redisun.create(options -> {
    options.setAddress("redis://127.0.0.1:6379");
    // 或者使用带认证信息的地址
    // options.setAddress("redis://username:password@127.0.0.1:6379");
});

// 设置键值
boolean setResult = redisun.set("mykey", "myvalue");
System.out.println("SET command result: " + setResult);

// 获取键值
String getResult = redisun.get("mykey");
System.out.println("GET command result: " + getResult);

// 执行有序集合命令
int zaddResult = redisun.zadd("myzset", 1.0, "member1");
System.out.println("ZADD command result: " + zaddResult);

// 删除键
int delResult = redisun.del("mykey");
System.out.println("DEL command result: " + delResult);

redisun.close();
```

### SET 命令高级用法

```java
Redisun redisun = Redisun.create(options -> {
    options.setAddress("redis://127.0.0.1:6379");
});

// 基本设置
redisun.set("key1", "value1");

// 仅在键不存在时设置（NX选项）
boolean nxResult = redisun.set("key2", "value2", cmd -> cmd.setIfNotExists());

// 仅在键存在时设置（XX选项）
boolean xxResult = redisun.set("key1", "newvalue", cmd -> cmd.setIfExists());

// 设置过期时间（秒）
redisun.set("key3", "value3", cmd -> cmd.expire(60));

// 设置过期时间（毫秒）
redisun.set("key4", "value4", cmd -> cmd.expireMs(30000));

// 在指定时间过期
redisun.set("key5", "value5", cmd -> cmd.expireAt(new Date(System.currentTimeMillis() + 60000)));

// 保留键的生存时间
redisun.set("key1", "anotherValue", cmd -> cmd.keepTTL());

redisun.close();
```

### 高级配置

```java
Redisun redisun = Redisun.create(options -> {
    // 设置服务器地址
    options.setAddress("redis://127.0.0.1:6379")
           // 设置数据库
           .setDatabase(1)
           // 启用调试模式
           .debug(true);
});
```

## 支持的命令

目前支持的 Redis 命令:

- `HELLO` - 服务器握手和认证
- `SET` - 设置键值对，支持多种选项（NX, XX, EX, PX, EXAT, PXAT, KEEPTTL）
- `GET` - 获取键的值
- `DEL` - 删除一个或多个键
- `ZADD` - 向有序集合添加成员

更多命令支持正在开发中，您也可以通过继承 [Command](src/main/java/tech/smartboot/redisun/Command.java) 类轻松扩展自定义命令。

## 构建与测试

确保已安装 Maven，然后运行以下命令：

```bash
mvn clean package
```

运行测试：

```bash
mvn test
```

## 许可证

该项目使用 [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) 许可证。

## 贡献

欢迎提交 Pull Request 和建议。请参阅项目 [issue 跟踪](https://gitee.com/smartboot/redisun/issues) 获取待办事项。

## 联系方式

如有问题或建议，请提交 issue 或联系项目维护者 [三刀](https://gitee.com/smartdms)。