# Redisun

Redisun 是一个基于 [smart-socket](https://github.com/smartboot/smart-socket) 的 Redis 客户端，旨在为 Redis 提供简单高效的连接和操作方式。

## 项目特性

- 基于 smart-socket 构建，性能优异。
- 提供简洁易用的 API 接口。
- 支持异步非阻塞通信模式。
- 易于集成到基于 Java 的项目中。

## 技术栈

- Java 1.8+
- smart-socket
- Redis 协议支持

## 安装与使用

1. **Maven 依赖**

   ```xml
   <dependency>
       <groupId>tech.smartboot</groupId>
       <artifactId>redisun</artifactId>
       <version>1.0.0</version>
   </dependency>
   ```

2. **快速开始**

   ```java
   RedisunClient client = new RedisunClient("127.0.0.1", 6379);
   client.connect();
   String response = client.sendCommand("SET", "key", "value");
   System.out.println(response); // 输出: OK
   ```

## 开发者信息

- **作者**: 三刀 (Seer)
- **联系方式**: zhengjunweimail@163.com
- **组织**: [SmartBoot](https://gitee.com/smartboot)

## 协议

Redisun 使用 [Apache 2.0 协议](http://www.apache.org/licenses/LICENSE-2.0.txt) 开源。

## 仓库地址

- Gitee: [https://gitee.com/smartboot/redisun](https://gitee.com/smartboot/redisun)
- GitHub: [https://github.com/smartboot/redisun](https://github.com/smartboot/redisun)

欢迎提交 PR 或提出 Issue 来帮助我们改进项目！