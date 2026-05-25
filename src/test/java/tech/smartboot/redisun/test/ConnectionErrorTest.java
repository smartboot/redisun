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
    public void testInvalidDatabaseIndex() {
        Redisun invalidDb = Redisun.create(opt -> opt.setAddress("127.0.0.1:6379").setDatabase(999));
        try {
            invalidDb.set("key", "value");
        } finally {
            invalidDb.close();
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
}
