package tech.smartboot.redisun.bench;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.redisun.Redisun;

import java.util.concurrent.CountDownLatch;

/**
 * @author 三刀
 * @version v1.0 10/23/25
 */
public class RedisunBenchmark extends Bench {
    private Redisun redisun;

    @Before
    public void before() {
        redisun = Redisun.create(options -> {
            options.setAddress(ADDRESS).debug(false);
        });
    }

    @Test
    public void redisunSet() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < SET_COUNT; i++) {
            redisun.set("test" + i, "test" + i);
//            System.out.println("redisun");
        }
        System.out.println("redisun set  cost " + (System.currentTimeMillis() - start) + "ms");
    }

    @Test
    public void get() {
        // 先设置数据
        for (int i = 0; i < SET_COUNT; i++) {
            redisun.set("test" + i, "test" + i);
        }
        
        // 测试GET性能
        long start = System.currentTimeMillis();
        for (int i = 0; i < SET_COUNT; i++) {
            redisun.get("test" + i);
        }
        System.out.println("redisun get  cost " + (System.currentTimeMillis() - start) + "ms");
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
                    redisun.set("test" + k + "_" + j, "test" + j);
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
        System.out.println("redisun cost " + (System.currentTimeMillis() - start) + "ms");
    }

    @Test
    public void redissonSet() {

    }

    @After
    public void after() {

    }
}
