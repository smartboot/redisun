![](./logo.svg)

<div align="center">

[![Maven Central](https://img.shields.io/maven-central/v/tech.smartboot/redisun)](https://central.sonatype.com/artifact/tech.smartboot/redisun)
[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://github.com/smartboot/redisun/blob/master/LICENSE)

</div>

Redisun is a lightweight Redis client based on [smart-socket](https://github.com/smartboot/smart-socket) designed for the Java platform. It supports asynchronous non-blocking I/O operations and provides high-performance Redis connection and command execution capabilities.

## Features

- **Lightweight Design**: Core code consists of only a few classes, with a jar size of only 33KB and minimal resource usage
- **High-performance Communication**: Based on smart-socket Java AIO implementation with strong single-thread processing capabilities and low resource consumption
- **Connection Multiplexing Technology**: Innovative connection multiplexing mechanism that allows one connection to handle multiple concurrent requests
- **RESP Protocol Support**: Full support for Redis Serialization Protocol (RESP), compatible with Redis servers
- **Command Extension Mechanism**: Provides a simple and easy-to-use command extension interface for adding custom Redis commands
- **Multi-database Support**: Supports Redis multi-database switching
- **Authentication Support**: Supports username/password authentication for Redis servers

## Core Components

- **Redisun**: Core client class that provides connection management and command execution functionality
- **Command**: Abstract base class for Redis commands, supporting custom command extensions
- **RESP**: Redis serialization protocol parser, supporting multiple data types
- **RedisMessageProcessor**: Message processor responsible for parsing Redis server responses
- **RedisSession**: Session manager that maintains client-server session state
- **RedisunOptions**: Client configuration options class for setting connection parameters

## Installation

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

## Quick Start

### Basic Usage

```java
import tech.smartboot.redisun.Redisun;

Redisun redisun = Redisun.create(options -> {
    options.setAddress("redis://127.0.0.1:6379");
    // Or use an address with authentication information
    // options.setAddress("redis://username:password@127.0.0.1:6379");
});

// Set key-value
boolean setResult = redisun.set("mykey", "myvalue");
System.out.println("SET command result: " + setResult);

// Get value by key
String getResult = redisun.get("mykey");
System.out.println("GET command result: " + getResult);

// Execute sorted set command
int zaddResult = redisun.zadd("myzset", 1.0, "member1");
System.out.println("ZADD command result: " + zaddResult);

// Delete key
int delResult = redisun.del("mykey");
System.out.println("DEL command result: " + delResult);

redisun.close();
```

### Advanced SET Command Usage

```java
Redisun redisun = Redisun.create(options -> {
    options.setAddress("redis://127.0.0.1:6379");
});

// Basic set
redisun.set("key1", "value1");

// Set only if key does not exist (NX option)
boolean nxResult = redisun.set("key2", "value2", cmd -> cmd.setIfNotExists());

// Set only if key exists (XX option)
boolean xxResult = redisun.set("key1", "newvalue", cmd -> cmd.setIfExists());

// Set expiration time (seconds)
redisun.set("key3", "value3", cmd -> cmd.expire(60));

// Set expiration time (milliseconds)
redisun.set("key4", "value4", cmd -> cmd.expireMs(30000));

// Expire at specified time
redisun.set("key5", "value5", cmd -> cmd.expireAt(new Date(System.currentTimeMillis() + 60000)));

// Keep key's time to live
redisun.set("key1", "anotherValue", cmd -> cmd.keepTTL());

redisun.close();
```

### Advanced Configuration

```java
Redisun redisun = Redisun.create(options -> {
    // Set server address
    options.setAddress("redis://127.0.0.1:6379")
            // Set database
            .setDatabase(1)
            // Enable debug mode
            .debug(true);
});
```

## Supported Commands

Currently supported Redis commands:

- `HELLO` - Server handshake and authentication
- `SET` - Set key-value pairs, supporting multiple options (NX, XX, EX, PX, EXAT, PXAT, KEEPTTL)
- `GET` - Get the value of a key
- `DEL` - Delete one or more keys
- `ZADD` - Add members to a sorted set

More command support is under development. You can also easily extend custom commands by extending the [Command](src/main/java/tech/smartboot/redisun/Command.java) class.

## Build and Test

Make sure Maven is installed, then run the following command:

```bash
mvn clean package
```

Run tests:

```bash
mvn test
```

## License

This project is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt).

## Contributing

Pull requests and suggestions are welcome. Please refer to the project [issue tracker](https://github.com/smartboot/redisun/issues) for pending tasks.

## Contact

For questions or suggestions, please submit an issue or contact the project maintainer [三刀](https://github.com/smartdms).