![](./logo.svg)

<div align="center">

[![Maven Central](https://img.shields.io/maven-central/v/tech.smartboot/redisun)](https://central.sonatype.com/artifact/tech.smartboot/redisun)
[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://gitee.com/smartboot/redisun/blob/master/LICENSE)

</div>

Redisun 是一个基于 [smart-socket](https://gitee.com/smartboot/smart-socket) 开发的轻量级 Redis 客户端，专为 Java 平台设计，支持异步非阻塞 I/O 操作，提供高性能的 Redis 连接和命令执行能力。

🚀Redisun 正在不断扩展对 Redis 命令的支持。完整的命令列表和支持情况请参阅我们的  [查看已支持的命令列表](https://smartboot.tech/redisun/guides/about/#redis-命令支持情况) 页面。

## 特性

- **轻量级**：核心代码仅由少量类组成，jar包仅50KB
- **高性能**：基于 smart-socket Java AIO 实现，单线程处理能力强
- **连接复用**：一个连接可同时处理多个并发请求
- **可扩展**：提供简单易用的命令扩展接口，方便添加自定义 Redis 命令

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
    <version>1.1.0</version>
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

## 关注我们

如果您对我们的项目感兴趣，欢迎关注我们的微信公众号，获取最新动态和技术分享。

<img src="https://smartboot.tech/wx_dyh.png" width="20%">
