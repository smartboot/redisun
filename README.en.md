This project aims to provide a lightweight Redis client implementation that supports asynchronous non-blocking I/O operations for the Java platform. The following is a brief description of the project:

### Features
- **Asynchronous Communication**: Implements asynchronous non-blocking communication based on Java NIO to improve performance.
- **Command Support**: Supports common Redis commands such as `HELLO` and `ZADD`.
- **Response Parsing**: Supports parsing of multiple Redis response types, including strings, integers, arrays, hash tables, and more.
- **Connection Management**: Automatically manages connection pools and the connection lifecycle to ensure efficient reuse.
- **Plugin Extension**: Supports a plugin mechanism for easy functional expansion.

### Core Components
- **Command**: Abstract base class defining Redis commands.
- **RedisResponse**: Abstract class defining the basic structure and parsing methods of Redis responses.
- **RedisMessageProcessor**: Message processor responsible for parsing received data and handling responses.
- **RedisSession**: Encapsulates session state and manages the response parsing process.
- **Redisun**: Core class providing client connection and command execution functionality.

### Usage Example
```java
Redisun redisun = Redisun.create(options -> {
    options.setHost("127.0.0.1");
    options.setPort(6379);
});

try {
    // Execute Redis command
    CompletableFuture<RedisResponse> future = redisun.execute(new HelloCommand());
    RedisResponse response = future.get();
    System.out.println("Server: " + ((HelloCommand.Response) response).getServer());
} catch (Exception e) {
    e.printStackTrace();
} finally {
    redisun.close();
}
```

### Build and Test
Ensure Maven is installed, then run the following command:
```bash
mvn clean package
```

Run tests:
```bash
mvn test
```

### License
This project is licensed under the MIT License. For details, please refer to the [LICENSE](LICENSE) file.

### Contribution
Pull requests and suggestions are welcome. Please refer to the project [issue tracker](https://gitee.com/smartboot/redisun/issues) for a list of pending tasks.

### Contact
For questions or suggestions, please submit an issue or contact the project maintainer.