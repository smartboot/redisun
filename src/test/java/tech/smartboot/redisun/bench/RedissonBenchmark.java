package tech.smartboot.redisun.bench;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RFuture;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 三刀
 * @version v1.0 10/23/25
 */
public class RedissonBenchmark extends Bench {
    private RedissonClient redissonClient;

    @Before
    public void before() {
        Config c = new Config();
        c.useSingleServer().setAddress(ADDRESS);
        redissonClient = Redisson.create(c);
        // 在每次测试前清空所有数据
        redissonClient.getKeys().flushall();

        // 预热逻辑
//        warmup();
    }

    @Test
    public void asyncSet() throws InterruptedException, ExecutionException {
        CountDownLatch latch = new CountDownLatch(SET_COUNT);
        long start = System.currentTimeMillis();
        for (int i = 0; i < SET_COUNT; i++) {
            redissonClient.getBucket("test" + i).setAsync("test" + i).exceptionally(throwable -> {
                latch.countDown();
                return null;
            }).thenRun(latch::countDown);
        }
        latch.await();
        long cost = System.currentTimeMillis() - start;
        System.out.println("[ASYNC SET] cost: " + cost + "ms");
        System.out.println("[ASYNC SET] ops/s: " + (SET_COUNT * 1000 / (cost + 1)));

        // 验证实际写入数量
        // 通过Redisson执行DBSIZE命令验证写入的键数量
        RFuture<Long> sizeFuture = redissonClient.getKeys().countAsync();
        Long size = sizeFuture.get();
        if (size >= SET_COUNT) {
            System.out.println("[ASYNC SET] Verification passed: at least " + SET_COUNT + " keys were written");
        } else {
            System.out.println("[ASYNC SET] Verification failed: only " + size + " keys were written, expected at least " + SET_COUNT);
        }
    }

    @Test
    public void asyncGet() throws InterruptedException {
        // 先设置数据
        CountDownLatch latch = new CountDownLatch(SET_COUNT);
        for (int i = 0; i < SET_COUNT; i++) {
            redissonClient.getBucket("test" + i).setAsync("test" + i).exceptionally(throwable -> {
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
                        redissonClient.getBucket("test" + (j % SET_COUNT)).getAsync().thenRun(() -> {
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
                        redissonClient.getBucket("test" + threadId + "_" + j).set("test" + j);
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
            redissonClient.getBucket("test" + i).setAsync("test" + i).exceptionally(throwable -> {
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
                        redissonClient.getBucket("test" + (j % SET_COUNT)).get();
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
        redissonClient.shutdown();
    }
}