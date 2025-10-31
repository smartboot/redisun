package tech.smartboot.redisun.bench;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.redisun.Redisun;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 三刀
 * @version v1.0 10/23/25
 */
public class RedisunBenchmark extends Bench {
    private Redisun redisun;

    @Before
    public void before() {
        redisun = Redisun.create(options -> {
            options.setAddress(ADDRESS).debug(false).minConnections(Runtime.getRuntime().availableProcessors()).maxConnections(Runtime.getRuntime().availableProcessors());
        });
        // 在每次测试前清空所有数据
        redisun.flushAll();

        // 预热逻辑
//        warmup();
    }

    @Test
    public void asyncSet() throws InterruptedException {
        // 先获取初始键数量
        long initialSize = redisun.dbsize();

        CountDownLatch latch = new CountDownLatch(SET_COUNT);
        long start = System.currentTimeMillis();
        for (int i = 0; i < SET_COUNT; i++) {
            redisun.asyncSet("test" + i, "test" + i).thenRun(latch::countDown);
        }
        latch.await();
        long cost = System.currentTimeMillis() - start;
        System.out.println("[ASYNC SET] cost: " + cost + "ms");
        System.out.println("[ASYNC SET] ops/s: " + (SET_COUNT * 1000 / (cost + 1)));

        // 验证实际写入数量
        long finalSize = redisun.dbsize();
        long addedKeys = finalSize - initialSize;

        if (addedKeys >= SET_COUNT) {
            System.out.println("[ASYNC SET] Verification passed: at least " + SET_COUNT + " keys were written");
        } else {
            System.out.println("[ASYNC SET] Verification failed: only " + addedKeys + " keys were written, expected at least " + SET_COUNT);
        }
    }

    @Test
    public void asyncGet() throws InterruptedException {
        // 先设置数据
        CountDownLatch latch = new CountDownLatch(SET_COUNT);
        for (int i = 0; i < SET_COUNT; i++) {
            redisun.asyncSet("test" + i, "test" + i).thenRun(latch::countDown);
        }
        latch.await();

        // 使用从系统属性传入的客户端数量进行异步GET测试
        AtomicInteger successCount = new AtomicInteger(0);

        Thread[] threads = new Thread[CONCURRENT_CLIENT_COUNT];
        long start = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENT_CLIENT_COUNT; i++) {
            threads[i] = new Thread(() -> {
                int j = 0;
                while (successCount.get() < SET_COUNT) {
                    try {
                        redisun.asyncGet("test" + (j % SET_COUNT)).thenRun(() -> {
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
        AtomicInteger successCount = new AtomicInteger(0);

        Thread[] threads = new Thread[CONCURRENT_CLIENT_COUNT];
        long start = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENT_CLIENT_COUNT; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                int j = 0;
                while (successCount.get() < SET_COUNT) {
                    try {
                        redisun.set("test" + threadId + "_" + j, "test" + j);
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
        // 先设置数据
        CountDownLatch latch = new CountDownLatch(SET_COUNT);
        for (int i = 0; i < SET_COUNT; i++) {
            redisun.asyncSet("test" + i, "test" + i).thenRun(latch::countDown);
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
                        redisun.get("test" + (j % SET_COUNT));
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
        redisun.close();
    }
}