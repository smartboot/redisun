package tech.smartboot.redisun.bench;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 三刀
 * @version v1.0 10/28/25
 */
public class LettuceBenchmark extends Bench {
    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> connection;

    @Before
    public void before() {
        redisClient = RedisClient.create(ADDRESS);
        connection = redisClient.connect();

        // 在每次测试前清空所有数据
        RedisCommands<String, String> syncCommands = connection.sync();
        syncCommands.flushall();

        // 预热逻辑
//        warmup();
    }

    @Test
    public void asyncSet() throws InterruptedException, ExecutionException {
        RedisAsyncCommands<String, String> asyncCommands = connection.async();

        CountDownLatch latch = new CountDownLatch(SET_COUNT);
        long start = System.currentTimeMillis();
        for (int i = 0; i < SET_COUNT; i++) {
            RedisFuture<String> future = asyncCommands.set("test" + i, "test" + i);
            future.exceptionally(throwable -> {
                latch.countDown();
                return null;
            }).thenRun(latch::countDown);
        }
        latch.await();
        long cost = System.currentTimeMillis() - start;
        System.out.println("[ASYNC SET] cost: " + cost + "ms");
        System.out.println("[ASYNC SET] ops/s: " + (SET_COUNT * 1000 / (cost + 1)));

        // 验证实际写入数量
        RedisCommands<String, String> syncCommands = connection.sync();
        Long size = syncCommands.dbsize();
        if (size >= SET_COUNT) {
            System.out.println("[ASYNC SET] Verification passed: at least " + SET_COUNT + " keys were written");
        } else {
            System.out.println("[ASYNC SET] Verification failed: only " + size + " keys were written, expected at least " + SET_COUNT);
        }
    }

    @Test
    public void asyncGet() throws InterruptedException {
        RedisAsyncCommands<String, String> asyncCommands = connection.async();

        // 先设置数据
        CountDownLatch latch = new CountDownLatch(SET_COUNT);
        for (int i = 0; i < SET_COUNT; i++) {
            RedisFuture<String> future = asyncCommands.set("test" + i, "test" + i);
            future.exceptionally(throwable -> {
                latch.countDown();
                return null;
            }).thenRun(latch::countDown);
        }
        latch.await();

        AtomicInteger successCount = new AtomicInteger(0);

        Thread[] threads = new Thread[CONCURRENT_CLIENT_COUNT];
        long start = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENT_CLIENT_COUNT; i++) {
            threads[i] = new Thread(() -> {
                int j = 0;
                while (successCount.get() < SET_COUNT) {
                    try {
                        asyncCommands.get("test" + (j % SET_COUNT)).thenRun(() -> {
                            successCount.incrementAndGet();
                        });
                        j++;
                    } catch (Exception e) {
                        // 忽略异常，继续执行
                    }
                }
            });
            threads[i].setDaemon(true);
            threads[i].start();
        }

        // 等待达到SET_COUNT个操作完成
        while (successCount.get() < SET_COUNT) {
            Thread.sleep(1);
        }

        long cost = System.currentTimeMillis() - start;
        System.out.println("[ASYNC GET] cost: " + cost + "ms");
        System.out.println("[ASYNC GET] ops/s: " + (SET_COUNT * 1000 / (cost + 1)));
    }

    @Test
    public void concurrentSet() throws InterruptedException {
        RedisCommands<String, String> syncCommands = connection.sync();

        AtomicInteger successCount = new AtomicInteger(0);

        Thread[] threads = new Thread[CONCURRENT_CLIENT_COUNT];
        long start = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENT_CLIENT_COUNT; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                int j = 0;
                while (successCount.get() < SET_COUNT) {
                    try {
                        syncCommands.set("test" + threadId + "_" + j, "test" + j);
                        successCount.incrementAndGet();
                        j++;
                    } catch (Exception e) {
                        // 忽略异常，继续执行
                    }
                }
            });
            threads[i].setDaemon(true);
            threads[i].start();
        }

        // 等待达到SET_COUNT个操作完成
        while (successCount.get() < SET_COUNT) {
            Thread.sleep(1);
        }

        long cost = System.currentTimeMillis() - start;
        System.out.println("[CONCURRENT SET] cost: " + cost + "ms");
        System.out.println("[CONCURRENT SET] ops/s: " + (SET_COUNT * 1000 / (cost + 1)));
    }

    @Test
    public void concurrentGet() throws InterruptedException {
        RedisCommands<String, String> syncCommands = connection.sync();
        RedisAsyncCommands<String, String> asyncCommands = connection.async();

        // 先设置数据
        CountDownLatch latch = new CountDownLatch(SET_COUNT);
        for (int i = 0; i < SET_COUNT; i++) {
            RedisFuture<String> future = asyncCommands.set("test" + i, "test" + i);
            future.exceptionally(throwable -> {
                latch.countDown();
                return null;
            }).thenRun(latch::countDown);
        }
        latch.await();

        AtomicInteger successCount = new AtomicInteger(0);

        Thread[] threads = new Thread[CONCURRENT_CLIENT_COUNT];
        long start = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENT_CLIENT_COUNT; i++) {
            threads[i] = new Thread(() -> {
                int j = 0;
                while (successCount.get() < SET_COUNT) {
                    try {
                        syncCommands.get("test" + (j % SET_COUNT));
                        successCount.incrementAndGet();
                        j++;
                    } catch (Exception e) {
                        // 忽略异常，继续执行
                    }
                }
            });
            threads[i].setDaemon(true);
            threads[i].start();
        }

        // 等待达到SET_COUNT个操作完成
        while (successCount.get() < SET_COUNT) {
            Thread.sleep(1);
        }

        long cost = System.currentTimeMillis() - start;
        System.out.println("[CONCURRENT GET] cost: " + cost + "ms");
        System.out.println("[CONCURRENT GET] ops/s: " + (SET_COUNT * 1000 / (cost + 1)));
    }

    @After
    public void after() {
        if (connection != null) {
            connection.close();
        }
        if (redisClient != null) {
            redisClient.shutdown();
        }
    }
}