package tech.smartboot.redisun.test;

import org.junit.Assert;
import org.junit.Test;
import tech.smartboot.redisun.RedisunException;

import java.util.concurrent.CompletableFuture;

/**
 * List类型命令测试类
 * 测试LPUSH, RPUSH, LPOP, RPUSH等命令
 *
 * @author 三刀
 * @version v1.0 10/21/25
 */
public class ListCommandTest extends AbstractRedisunTest {

    // ==================== LPUSH命令测试 ====================

    @Test
    public void testLPushCommand() {
        String key = topic + ":lpush";

        redisun.del(key);

        Assert.assertEquals(1, redisun.lpush(key, "value1"));
        Assert.assertEquals(3, redisun.lpush(key, "value2", "value3"));

        // 验证列表内容: LPUSH插入到头部，所以顺序是value3, value2, value1
        Assert.assertEquals("value3", redisun.lpop(key));
        Assert.assertEquals("value2", redisun.lpop(key));
        Assert.assertEquals("value1", redisun.lpop(key));

        redisun.del(key);
    }

    @Test(expected = RedisunException.class)
    public void testLPushWithNullKey() {
        redisun.lpush(null, "value");
    }

    @Test(expected = NullPointerException.class)
    public void testLPushWithNullValues() {
        redisun.lpush(topic + ":lpush", (String[]) null);
    }

    // ==================== RPUSH命令测试 ====================

    @Test
    public void testRPushCommand() {
        String key = topic + ":rpush";

        redisun.del(key);

        Assert.assertEquals(1, redisun.rpush(key, "value1"));
        Assert.assertEquals(3, redisun.rpush(key, "value2", "value3"));

        // 验证列表内容: RPUSH插入到尾部，所以顺序是value1, value2, value3
        Assert.assertEquals("value1", redisun.lpop(key));
        Assert.assertEquals("value2", redisun.lpop(key));
        Assert.assertEquals("value3", redisun.lpop(key));

        redisun.del(key);
    }

    @Test(expected = RedisunException.class)
    public void testRPushWithNullKey() {
        redisun.rpush(null, "value");
    }

    @Test(expected = NullPointerException.class)
    public void testRPushWithNullValues() {
        redisun.rpush(topic + ":rpush", (String[]) null);
    }

    // ==================== LPOP命令测试 ====================

    @Test
    public void testLPopCommand() {
        String key = topic + ":lpop";

        redisun.del(key);

        // 从空列表弹出
        Assert.assertNull(redisun.lpop(key));

        // 添加元素
        redisun.rpush(key, "value1", "value2", "value3");

        // 从头部弹出
        Assert.assertEquals("value1", redisun.lpop(key));
        Assert.assertEquals("value2", redisun.lpop(key));
        Assert.assertEquals("value3", redisun.lpop(key));

        // 再次从空列表弹出
        Assert.assertNull(redisun.lpop(key));

        redisun.del(key);
    }

    @Test(expected = RedisunException.class)
    public void testLPopWithNullKey() {
        redisun.lpop(null);
    }

    // ==================== RPOP命令测试 ====================

    @Test
    public void testRPopCommand() {
        String key = topic + ":rpop";

        redisun.del(key);

        // 从空列表弹出
        Assert.assertNull(redisun.rpop(key));

        // 添加元素
        redisun.rpush(key, "value1", "value2", "value3");

        // 从尾部弹出
        Assert.assertEquals("value3", redisun.rpop(key));
        Assert.assertEquals("value2", redisun.rpop(key));
        Assert.assertEquals("value1", redisun.rpop(key));

        // 再次从空列表弹出
        Assert.assertNull(redisun.rpop(key));

        redisun.del(key);
    }

    @Test(expected = RedisunException.class)
    public void testRPopWithNullKey() {
        redisun.rpop(null);
    }

    // ==================== 综合列表操作测试 ====================

    @Test
    public void testListCommands() {
        String key = topic + ":list";

        redisun.del(key);

        // LPUSH和RPUSH组合
        redisun.lpush(key, "left1");
        redisun.lpush(key, "left2");
        redisun.rpush(key, "right1");
        redisun.rpush(key, "right2");

        // 列表应该是: left2, left1, right1, right2
        Assert.assertEquals("left2", redisun.lpop(key));
        Assert.assertEquals("left1", redisun.lpop(key));
        Assert.assertEquals("right2", redisun.rpop(key));
        Assert.assertEquals("right1", redisun.rpop(key));

        redisun.del(key);
    }

    // ==================== 异步方法测试 ====================

    @Test
    public void testAsyncListCommands() throws Exception {
        String key = topic + ":async";

        CompletableFuture<Long> lpushFuture = redisun.asyncLpush(key, "v1", "v2");
        Assert.assertEquals(Long.valueOf(2), lpushFuture.get());

        CompletableFuture<Long> rpushFuture = redisun.asyncRpush(key, "v3");
        Assert.assertEquals(Long.valueOf(3), rpushFuture.get());

        CompletableFuture<String> lpopFuture = redisun.asyncLpop(key);
        Assert.assertEquals("v2", lpopFuture.get());

        CompletableFuture<String> rpopFuture = redisun.asyncRpop(key);
        Assert.assertEquals("v3", rpopFuture.get());

        redisun.del(key);
    }

    // ==================== 边界条件测试 ====================

    @Test
    public void testListEdgeCases() {
        String key = topic + ":list-edge";

        redisun.del(key);

        // 测试空字符串元素
        redisun.lpush(key, "");
        Assert.assertEquals("", redisun.lpop(key));

        // 测试包含空格的元素
        redisun.rpush(key, "  ");
        Assert.assertEquals("  ", redisun.rpop(key));

        // 测试特殊字符
        redisun.lpush(key, "!@#$%^&*()");
        Assert.assertEquals("!@#$%^&*()", redisun.lpop(key));

        // 测试Unicode
        redisun.rpush(key, "中文🎉");
        Assert.assertEquals("中文🎉", redisun.rpop(key));

        // 测试大量元素
        for (int i = 0; i < 1000; i++) {
            redisun.rpush(key, "element" + i);
        }
        for (int i = 0; i < 1000; i++) {
            Assert.assertEquals("element" + i, redisun.lpop(key));
        }

        redisun.del(key);
    }

    @Test
    public void testListTypeOperations() {
        String key = topic + ":list-type";

        redisun.del(key);

        // 对列表执行字符串操作应该失败
        redisun.lpush(key, "value");

        try {
            redisun.incr(key);
            Assert.fail("INCR on list should throw exception");
        } catch (RedisunException e) {
            // Expected
        }

        try {
            redisun.append(key, "suffix");
            Assert.fail("APPEND on list should throw exception");
        } catch (RedisunException e) {
            // Expected
        }

        redisun.del(key);
    }
}
