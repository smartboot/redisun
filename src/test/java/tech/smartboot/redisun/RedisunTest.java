package tech.smartboot.redisun;

import org.junit.Test;

/**
 * @author 三刀
 * @version v1.0 10/21/25
 */
public class RedisunTest {

    @Test
    public void test() throws InterruptedException {
        Redisun redisun = Redisun.create(opt -> opt.setAddress("127.0.0.1:6379"));
        System.out.println(redisun.zadd().add("test", 1, "a"));
        Thread.sleep(30000l);
    }
}
