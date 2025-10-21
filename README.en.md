# Redisun

Redisun is a Redis client based on [smart-socket](https://github.com/smartboot/smart-socket), designed to provide a simple and efficient way to connect and operate Redis.

## Project Features

- Built on smart-socket, delivering excellent performance.
- Provides clean and easy-to-use API interfaces.
- Supports asynchronous non-blocking communication mode.
- Easily integrated into Java-based projects.

## Technology Stack

- Java 1.8+
- smart-socket
- Redis protocol support

## Installation and Usage

1. **Maven Dependency**

   ```xml
   <dependency>
       <groupId>tech.smartboot</groupId>
       <artifactId>redisun</artifactId>
       <version>1.0.0</version>
   </dependency>
   ```

2. **Quick Start**

   ```java
   RedisunClient client = new RedisunClient("127.0.0.1", 6379);
   client.connect();
   String response = client.sendCommand("SET", "key", "value");
   System.out.println(response); // Output: OK
   ```

## Developer Information

- **Author**: 三刀 (Seer)
- **Contact**: zhengjunweimail@163.com
- **Organization**: [SmartBoot](https://gitee.com/smartboot)

## License

Redisun is open-sourced under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.txt).

## Repository Links

- Gitee: [https://gitee.com/smartboot/redisun](https://gitee.com/smartboot/redisun)
- GitHub: [https://github.com/smartboot/redisun](https://github.com/smartboot/redisun)

Feel free to submit PRs or raise issues to help us improve the project!