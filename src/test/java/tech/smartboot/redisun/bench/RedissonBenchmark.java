package tech.smartboot.redisun.bench;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.CountDownLatch;

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
    }

    @Test
    public void redisunSet() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < SET_COUNT; i++) {
            redissonClient.getBucket("test" + i).set("test" + i);
        }
        System.out.println("redisson cost " + (System.currentTimeMillis() - start) + "ms");
    }

    @Test
    public void get() {
        // 先设置数据
        for (int i = 0; i < SET_COUNT; i++) {
            redissonClient.getBucket("test" + i).set("test" + i);
        }

        // 测试GET性能
        long start = System.currentTimeMillis();
        for (int i = 0; i < SET_COUNT; i++) {
            redissonClient.getBucket("test" + i).get();
        }
        System.out.println("redisson get cost " + (System.currentTimeMillis() - start) + "ms");
    }

    @Test
    public void redisunConcurrentSet() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(SET_COUNT);
        CountDownLatch waiteThread = new CountDownLatch(CONCURRENT_CLIENT_COUNT);
        for (int i = 0; i < CONCURRENT_CLIENT_COUNT; i++) {
            int k = i;
            Thread thread = new Thread(() -> {
                int j = 0;
                waiteThread.countDown();
                while (latch.getCount() > 0) {
                    redissonClient.getBucket("test" + k + "_" + j).set("test" + j);
                    j++;
                    latch.countDown();
                }
            });
            thread.setDaemon(true);
            thread.start();
        }
        waiteThread.await();
        long start = System.currentTimeMillis();
        latch.await();
        System.out.println("redisson cost " + (System.currentTimeMillis() - start) + "ms");
    }

    @Test
    public void redissonSet() {

    }

    @After
    public void after() {
        redissonClient.shutdown();
    }
}
