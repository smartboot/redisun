package tech.smartboot.redisun.test;

import org.junit.Assert;
import org.junit.Test;
import tech.smartboot.redisun.RedisunException;

import java.util.concurrent.CompletableFuture;

/**
 * Set类型命令测试类
 * 测试SADD等命令
 *
 * @author 三刀
 * @version v1.0 10/21/25
 */
public class SetCommandTest extends AbstractRedisunTest {

    // ==================== SADD命令测试 ====================

    @Test
    public void testSAddCommand() {
        String key = topic + ":sadd";
        String member1 = "member1";
        String member2 = "member2";

        redisun.del(key);

        // 添加新成员到集合，应该返回添加的成员数
        Assert.assertEquals(1, redisun.sadd(key, member1));

        // 再次添加相同成员，应该返回0
        Assert.assertEquals(0, redisun.sadd(key, member1));

        // 添加多个新成员，应该返回添加的成员数
        Assert.assertEquals(2, redisun.sadd(key, member2, "member3"));

        // 添加部分重复成员，应该只计算新成员
        Assert.assertEquals(1, redisun.sadd(key, "member3", "member4"));

        redisun.del(key);
    }

    @Test(expected = RedisunException.class)
    public void testSAddWithNullKey() {
        redisun.sadd(null, "member");
    }

    @Test(expected = NullPointerException.class)
    public void testSAddWithNullMembers() {
        redisun.sadd(topic + ":sadd", (String[]) null);
    }

    // ==================== 异步方法测试 ====================

    @Test
    public void testAsyncSetCommands() throws Exception {
        String key = topic + ":async";

        CompletableFuture<Integer> saddFuture = redisun.asyncSadd(key, "m1", "m2");
        Assert.assertEquals(Integer.valueOf(2), saddFuture.get());

        // 再次添加相同成员
        saddFuture = redisun.asyncSadd(key, "m1", "m3");
        Assert.assertEquals(Integer.valueOf(1), saddFuture.get());

        redisun.del(key);
    }

    // ==================== 边界条件测试 ====================

    @Test
    public void testSetEdgeCases() {
        String key = topic + ":set-edge";

        redisun.del(key);

        // 测试空字符串成员
        Assert.assertEquals(1, redisun.sadd(key, ""));
        Assert.assertEquals(0, redisun.sadd(key, ""));

        // 测试包含空格的成员
        Assert.assertEquals(1, redisun.sadd(key, "  "));

        // 测试特殊字符
        Assert.assertEquals(1, redisun.sadd(key, "!@#$%^&*()"));

        // 测试Unicode
        Assert.assertEquals(1, redisun.sadd(key, "中文🎉"));

        // 测试大量成员
        for (int i = 0; i < 1000; i++) {
            redisun.sadd(key, "member" + i);
        }

        // 验证重复添加返回0
        int duplicateCount = 0;
        for (int i = 0; i < 1000; i++) {
            duplicateCount += redisun.sadd(key, "member" + i);
        }
        Assert.assertEquals(0, duplicateCount);

        redisun.del(key);
    }

    @Test
    public void testSetTypeOperations() {
        String key = topic + ":set-type";

        redisun.del(key);

        // 对集合执行字符串操作应该失败
        redisun.sadd(key, "member");

        try {
            redisun.incr(key);
            Assert.fail("INCR on set should throw exception");
        } catch (RedisunException e) {
            // Expected
        }

        try {
            redisun.append(key, "suffix");
            Assert.fail("APPEND on set should throw exception");
        } catch (RedisunException e) {
            // Expected
        }

        redisun.del(key);
    }
}
