package tech.smartboot.redisun.bench;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 三刀
 * @version v1.0 10/28/25
 */
public class JedisBenchmark extends Bench {
    private JedisPool jedisPool;

    @Before
    public void before() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(CONCURRENT_CLIENT_COUNT);
        jedisPool = new JedisPool(config, "127.0.0.1", 6379);

        try (Jedis jedis = jedisPool.getResource()) {
            // 在每次测试前清空所有数据
            jedis.flushAll();
        }

        // 预热逻辑
//        warmup();
    }

    @Test
    public void asyncSet() throws InterruptedException {
        System.out.println("[ASYNC SET] cost: -");
        System.out.println("[ASYNC SET] ops/s: -");
    }

    @Test
    public void asyncGet() throws InterruptedException {
        System.out.println("[ASYNC GET] cost: -");
        System.out.println("[ASYNC GET] ops/s: -");
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
                    try (Jedis jedis = jedisPool.getResource()) {
                        jedis.set("test" + threadId + "_" + j, "test" + j);
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
        try (Jedis jedis = jedisPool.getResource()) {
            for (int i = 0; i < SET_COUNT; i++) {
                jedis.set("test" + i, "test" + i);
            }
        }

        AtomicInteger successCount = new AtomicInteger(0);

        Thread[] threads = new Thread[CONCURRENT_CLIENT_COUNT];
        long start = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENT_CLIENT_COUNT; i++) {
            threads[i] = new Thread(() -> {
                int j = 0;
                while (successCount.get() < SET_COUNT) {
                    try (Jedis jedis = jedisPool.getResource()) {
                        jedis.get("test" + (j % SET_COUNT));
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
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
}