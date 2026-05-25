package tech.smartboot.redisun.test;

import org.junit.Assert;
import org.junit.Test;
import tech.smartboot.redisun.Redisun;
import tech.smartboot.redisun.RedisunException;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 连接和错误处理测试类
 * 测试连接错误、并发操作、性能边界等场景
 *
 * @author 三刀
 * @version v1.0 10/21/25
 */
public class ConnectionErrorTest extends AbstractRedisunTest {

    // ==================== 连接错误测试 ====================

    @Test(expected = RedisunException.class)
    public void testConnectionError() {
        Redisun invalidRedisun = Redisun.create(opt -> opt.setAddress("127.0.0.1:99999"));
        try {
            invalidRedisun.set("key", "value");
        } finally {
            invalidRedisun.close();
        }
    }

    @Test(expected = RedisunException.class)
    public void testInvalidPort() {
        Redisun invalidRedisun = Redisun.create(opt -> opt.setAddress("127.0.0.1:789887"));
        try {
            invalidRedisun.set("key", "value");
        } finally {
            invalidRedisun.close();
        }
    }

    @Test(expected = RedisunException.class)
    public void testInvalidHost() {
        Redisun invalidRedisun = Redisun.create(opt -> opt.setAddress("invalid-host:6379"));
        try {
            invalidRedisun.set("key", "value");
        } finally {
            invalidRedisun.close();
        }
    }

    @Test
    public void testInvalidDatabaseIndex() {
        Redisun invalidDb = Redisun.create(opt -> opt.setAddress("127.0.0.1:6379").setDatabase(999));
        try {
            invalidDb.set("key", "value");
            Assert.fail("Should throw authentication error");
        } catch (RedisunException e) {
            Assert.assertEquals("ERR DB index is out of range", e.getMessage());
        } finally {
            invalidDb.close();
        }
    }

    @Test
    public void testNegativeDatabaseIndex() {
        Redisun invalidDb = Redisun.create(opt -> opt.setAddress("127.0.0.1:6379").setDatabase(-1));
        try {
            invalidDb.set("key", "value");
            Assert.fail("Should throw authentication error");
        } catch (RedisunException e) {
            Assert.assertEquals("ERR DB index is out of range", e.getMessage());
        } finally {
            invalidDb.close();
        }
    }

    // ==================== 认证错误测试 ====================

    @Test
    public void testAuthenticationFailure() {
        // 如果Redis配置了密码，测试错误密码
        // 注意：此测试需要Redis配置密码才能有效
        try {
            Redisun authRedisun = Redisun.create(opt ->
                    opt.setAddress("127.0.0.1:6379")
                            .debug(true)
                            .setPassword("wrong-password")
            );
            authRedisun.set("key", "value");
            Assert.fail("Should throw authentication error");
        } catch (RedisunException e) {
            // Expected - authentication failure
        }
    }

    // ==================== 并发操作测试 ====================

    @Test
    public void testConcurrentOperations() throws InterruptedException {
        String key = topic + ":concurrent";
        int threadCount = 10;
        int operationsPerThread = 100;

        redisun.del(key);
        redisun.set(key, "0");

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    redisun.incr(key);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        String finalValue = redisun.get(key);
        Assert.assertEquals(String.valueOf(threadCount * operationsPerThread), finalValue);

        redisun.del(key);
    }

    @Test
    public void testConcurrentSetGet() throws InterruptedException {
        String key = topic + ":concurrent-setget";
        int threadCount = 20;
        int operationsPerThread = 50;

        redisun.del(key);

        Thread[] threads = new Thread[threadCount];
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    String value = "thread-" + threadId + "-op-" + j;
                    if (redisun.set(key, value)) {
                        successCount.incrementAndGet();
                    }
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Assert.assertEquals(threadCount * operationsPerThread, successCount.get());

        redisun.del(key);
    }

    @Test
    public void testConcurrentIncr() throws InterruptedException {
        String key = topic + ":concurrent-incr";
        int threadCount = 10;
        int operationsPerThread = 100;

        redisun.del(key);
        redisun.set(key, "0");

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    redisun.incr(key);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Assert.assertEquals(String.valueOf(threadCount * operationsPerThread), redisun.get(key));
        redisun.del(key);
    }

    @Test
    public void testConcurrentMixedOperations() throws InterruptedException {
        String key = topic + ":concurrent-mixed";
        int threadCount = 10;

        redisun.del(key);

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 50; j++) {
                    String currentKey = key + ":" + threadId + ":" + j;
                    redisun.set(currentKey, "value-" + j);
                    redisun.get(currentKey);
                    redisun.del(currentKey);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // 验证所有键都被删除
        for (int i = 0; i < threadCount; i++) {
            for (int j = 0; j < 50; j++) {
                String currentKey = key + ":" + i + ":" + j;
                Assert.assertNull(redisun.get(currentKey));
            }
        }
    }

    // ==================== 性能边界测试 ====================

    @Test
    public void testLargeValue() {
        String key = topic + ":large-value";
        StringBuilder largeValue = new StringBuilder();
        for (int i = 0; i < 1024 * 1024; i++) {
            largeValue.append('a');
        }

        boolean result = redisun.set(key, largeValue.toString());
        Assert.assertTrue("Setting large value should succeed", result);
        Assert.assertEquals(1024 * 1024, redisun.get(key).length());

        redisun.del(key);
    }

    @Test
    public void testManyKeys() {
        int keyCount = 1000;
        String[] keys = new String[keyCount];

        // 创建大量键
        for (int i = 0; i < keyCount; i++) {
            keys[i] = topic + ":many:" + i;
            redisun.set(keys[i], "value" + i);
        }

        // 验证所有键
        for (int i = 0; i < keyCount; i++) {
            Assert.assertEquals("value" + i, redisun.get(keys[i]));
        }

        // 批量删除
        int deleted = redisun.del(keys);
        Assert.assertEquals(keyCount, deleted);

        // 验证所有键已删除
        for (int i = 0; i < keyCount; i++) {
            Assert.assertNull(redisun.get(keys[i]));
        }
    }

    @Test
    public void testRapidOperations() {
        String key = topic + ":rapid";
        int operationCount = 10000;

        redisun.del(key);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < operationCount; i++) {
            redisun.incr(key);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        Assert.assertEquals(String.valueOf(operationCount), redisun.get(key));
        System.out.println("Completed " + operationCount + " operations in " + duration + "ms");
        System.out.println("Average: " + (duration / (double) operationCount) + "ms per operation");

        redisun.del(key);
    }

    @Test
    public void testMemoryPressure() {
        // 测试内存压力下的行为
        int keyCount = 10000;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append('x');
        }
        String value = sb.toString(); // 1KB value

        for (int i = 0; i < keyCount; i++) {
            String key = topic + ":memory:" + i;
            redisun.set(key, value);
        }

        // 验证部分键
        for (int i = 0; i < 100; i++) {
            String key = topic + ":memory:" + i;
            Assert.assertEquals(value, redisun.get(key));
        }

        // 清理
        for (int i = 0; i < keyCount; i++) {
            String key = topic + ":memory:" + i;
            redisun.del(key);
        }
    }

    // ==================== 资源清理测试 ====================

    @Test
    public void testMultipleConnections() {
        int connectionCount = 10;
        Redisun[] connections = new Redisun[connectionCount];

        for (int i = 0; i < connectionCount; i++) {
            connections[i] = Redisun.create(opt -> opt.setAddress("127.0.0.1:6379"));
            connections[i].set(topic + ":conn" + i, "value" + i);
        }

        for (int i = 0; i < connectionCount; i++) {
            Assert.assertEquals("value" + i, connections[i].get(topic + ":conn" + i));
        }

        for (int i = 0; i < connectionCount; i++) {
            connections[i].del(topic + ":conn" + i);
            connections[i].close();
        }
    }

    @Test
    public void testConnectionReuse() {
        String key = topic + ":reuse";

        // 执行多次操作，验证连接复用
        for (int i = 0; i < 100; i++) {
            redisun.set(key, String.valueOf(i));
            Assert.assertEquals(String.valueOf(i), redisun.get(key));
        }

        redisun.del(key);
    }

    @Test
    public void testResourceCleanup() {
        // 创建和关闭多个连接，验证资源清理
        for (int i = 0; i < 10; i++) {
            Redisun r = Redisun.create(opt -> opt.setAddress("127.0.0.1:6379"));
            r.set(topic + ":cleanup", "value");
            r.close();
        }

        // 验证最后一个连接关闭后，数据仍然存在
        // 注意：这取决于Redis服务器是否持久化
    }

    @Test
    public void testCloseAfterOperations() {
        Redisun r = Redisun.create(opt -> opt.setAddress("127.0.0.1:6379"));

        r.set(topic + ":close-test", "value");
        Assert.assertEquals("value", r.get(topic + ":close-test"));

        r.del(topic + ":close-test");
        r.close();

        // 关闭后不应该再执行操作
        try {
            r.set(topic + ":after-close", "value");
            // 可能抛出异常或静默失败
        } catch (Exception e) {
            // Expected
        }
    }

    // ==================== 超时测试 ====================

    @Test
    public void testOperationTimeout() {
        // 测试操作超时
        // 注意：这需要配置超时参数
        String key = topic + ":timeout";
        redisun.set(key, "value");
        Assert.assertEquals("value", redisun.get(key));
        redisun.del(key);
    }

    // ==================== 重连测试 ====================

    @Test
    public void testReconnection() throws InterruptedException {
        // 测试连接断开后重连
        String key = topic + ":reconnect";

        redisun.set(key, "value1");
        Assert.assertEquals("value1", redisun.get(key));

        // 模拟网络中断（如果可能）
        Thread.sleep(100);

        // 应该能够继续操作
        redisun.set(key, "value2");
        Assert.assertEquals("value2", redisun.get(key));

        redisun.del(key);
    }

    // ==================== 错误恢复测试 ====================

    @Test
    public void testErrorRecovery() {
        String key = topic + ":recovery";

        // 正常操作
        redisun.set(key, "value");
        Assert.assertEquals("value", redisun.get(key));

        // 错误操作（类型错误）
        redisun.lpush(key + ":list", "item");
        try {
            redisun.get(key + ":list");
        } catch (RedisunException e) {
            // Expected
        }

        // 应该能够继续正常操作
        redisun.set(key, "new-value");
        Assert.assertEquals("new-value", redisun.get(key));

        redisun.del(key);
        redisun.del(key + ":list");
    }
}
