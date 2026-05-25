package tech.smartboot.redisun.test;

import org.junit.After;
import org.junit.Before;
import tech.smartboot.redisun.Redisun;

import java.util.UUID;

/**
 * Redisun测试基类
 * 提供通用的测试基础设施
 *
 * @author 三刀
 * @version v1.0 10/21/25
 */
public abstract class AbstractRedisunTest {
    protected Redisun redisun;
    protected String topic;

    @Before
    public void init() {
        redisun = Redisun.create(opt -> opt.debug(true).setAddress("127.0.0.1:6379"));
        topic = "test-" + UUID.randomUUID();
    }

    @After
    public void after() {
        if (redisun != null) {
            redisun.close();
        }
    }
}
