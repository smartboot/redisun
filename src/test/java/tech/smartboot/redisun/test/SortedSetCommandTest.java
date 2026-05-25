package tech.smartboot.redisun.test;

import org.junit.Assert;
import org.junit.Test;
import tech.smartboot.redisun.RedisunException;
import tech.smartboot.redisun.cmd.ZRangeCommand;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Sorted Set类型命令测试类
 * 测试ZADD, ZREM, ZSCORE, ZRANGE等命令
 *
 * @author 三刀
 * @version v1.0 10/21/25
 */
public class SortedSetCommandTest extends AbstractRedisunTest {

    // ==================== ZADD命令测试 ====================

    @Test
    public void testZAddCommand() {
        String key = topic + ":zadd";

        redisun.del(key);

        // 添加单个成员
        Assert.assertEquals(1, redisun.zadd(key, 1.0, "member1"));
        Assert.assertEquals(1, redisun.zadd(key, 2.5, "member2"));

        // 更新已存在成员的分数
        Assert.assertEquals(0, redisun.zadd(key, 3.0, "member1"));

        // 添加负分数成员
        Assert.assertEquals(1, redisun.zadd(key, -1.5, "member3"));

        // 添加零分数成员
        Assert.assertEquals(1, redisun.zadd(key, 0.0, "member4"));

        redisun.del(key);
    }

    @Test(expected = RedisunException.class)
    public void testZAddWithNullKey() {
        redisun.zadd(null, 1.0, "member");
    }

    @Test(expected = RedisunException.class)
    public void testZAddWithNullMember() {
        redisun.zadd(topic + ":zadd", 1.0, null);
    }

    // ==================== ZREM命令测试 ====================

    @Test
    public void testZRemCommand() {
        String key = topic + ":zrem";

        redisun.del(key);

        // 从空集合中移除成员
        Assert.assertEquals(0, redisun.zrem(key, "member1"));

        // 添加测试数据
        redisun.zadd(key, 1.0, "member1");
        redisun.zadd(key, 2.0, "member2");
        redisun.zadd(key, 3.0, "member3");

        // 移除单个存在的成员
        Assert.assertEquals(1, redisun.zrem(key, "member1"));
        Assert.assertNull(redisun.zscore(key, "member1"));

        // 移除不存在的成员
        Assert.assertEquals(0, redisun.zrem(key, "nonexistent"));

        // 移除多个成员（混合存在和不存在的）
        Assert.assertEquals(2, redisun.zrem(key, "member2", "nonexistent", "member3"));

        // 验证集合为空
        Assert.assertEquals(0, redisun.zrange(key, 0, -1).size());

        // 移除后再次添加相同成员
        redisun.zadd(key, 5.0, "member1");
        Assert.assertEquals(Double.valueOf(5.0), redisun.zscore(key, "member1"));

        redisun.del(key);
    }

    @Test(expected = RedisunException.class)
    public void testZRemWithNullKey() {
        redisun.zrem(null, "member1");
    }

    // ==================== ZSCORE命令测试 ====================

    @Test
    public void testZScoreCommand() {
        String key = topic + ":zscore";
        String member = "test-member";
        double score = 123.456;

        redisun.del(key);

        // 获取不存在的键的分数
        Assert.assertNull(redisun.zscore(key, member));

        // 添加成员
        redisun.zadd(key, score, member);

        // 获取存在的成员的分数
        Double result = redisun.zscore(key, member);
        Assert.assertNotNull(result);
        Assert.assertEquals(score, result, 0.001);

        // 获取不存在的成员的分数
        Assert.assertNull(redisun.zscore(key, "nonexistent"));

        // 测试负分数
        redisun.zadd(key, -99.5, "negative-member");
        Assert.assertEquals(-99.5, redisun.zscore(key, "negative-member"), 0.001);

        // 测试零分数
        redisun.zadd(key, 0.0, "zero-member");
        Assert.assertEquals(0.0, redisun.zscore(key, "zero-member"), 0.001);

        // 测试大数值分数
        redisun.zadd(key, Double.MAX_VALUE, "max-member");
        Assert.assertEquals(Double.MAX_VALUE, redisun.zscore(key, "max-member"), 0.001);

        // 测试极小数值分数
        redisun.zadd(key, Double.MIN_VALUE, "min-member");
        Assert.assertEquals(Double.MIN_VALUE, redisun.zscore(key, "min-member"), 0.001);

        // 移除成员后测试
        redisun.zrem(key, member);
        Assert.assertNull(redisun.zscore(key, member));

        redisun.del(key);
    }

    @Test(expected = RedisunException.class)
    public void testZScoreWithNullKey() {
        redisun.zscore(null, "member");
    }

    // ==================== ZRANGE命令测试 ====================

    @Test
    public void testZRangeCommand() {
        String key = topic + ":zrange";

        redisun.del(key);

        // 添加测试数据
        redisun.zadd(key, 1.0, "one");
        redisun.zadd(key, 2.0, "two");
        redisun.zadd(key, 3.0, "three");

        // 测试基本的ZRANGE命令
        List<String> result = redisun.zrange(key, 0, -1);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("one", result.get(0));
        Assert.assertEquals("two", result.get(1));
        Assert.assertEquals("three", result.get(2));

        // 测试范围查询
        result = redisun.zrange(key, 1, 2);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("two", result.get(0));
        Assert.assertEquals("three", result.get(1));

        // 测试REV选项
        List<ZRangeCommand.Tuple> result1 = redisun.zrange(key, 0, -1, cmd -> cmd.rev());
        Assert.assertEquals(3, result1.size());
        Assert.assertEquals("three", result1.get(0).getMember());
        Assert.assertEquals("two", result1.get(1).getMember());
        Assert.assertEquals("one", result1.get(2).getMember());

        // 测试LIMIT选项
        result1 = redisun.zrange(key, 1, 3, cmd -> cmd.sortByScore().limit(1, 2));
        Assert.assertEquals(2, result1.size());
        Assert.assertEquals("two", result1.get(0).getMember());
        Assert.assertEquals("three", result1.get(1).getMember());

        redisun.del(key);
    }

    @Test
    public void testZRangeByScore() {
        String key = topic + ":zrangebyscore";

        redisun.del(key);

        // 添加测试数据
        redisun.zadd(key, 1.0, "one");
        redisun.zadd(key, 2.0, "two");
        redisun.zadd(key, 3.0, "three");
        redisun.zadd(key, 4.0, "four");
        redisun.zadd(key, 5.0, "five");

        // 测试BYSCORE选项
        List<ZRangeCommand.Tuple> result = redisun.zrange(key, 2, 4, cmd -> cmd.sortByScore());
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("two", result.get(0).getMember());
        Assert.assertEquals("three", result.get(1).getMember());
        Assert.assertEquals("four", result.get(2).getMember());

        // 测试BYSCORE和REV选项组合
        result = redisun.zrange(key, 4, 2, cmd -> cmd.sortByScore().rev());
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("four", result.get(0).getMember());
        Assert.assertEquals("three", result.get(1).getMember());
        Assert.assertEquals("two", result.get(2).getMember());

        // 测试BYSCORE和LIMIT选项组合
        result = redisun.zrange(key, 1, 5, cmd -> cmd.sortByScore().limit(1, 3));
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("two", result.get(0).getMember());
        Assert.assertEquals("three", result.get(1).getMember());
        Assert.assertEquals("four", result.get(2).getMember());

        redisun.del(key);
    }

    @Test
    public void testAsyncZRangeWithScores() throws Exception {
        String key = topic + ":zrange-withscores";

        redisun.del(key);

        redisun.zadd(key, 1.0, "one");
        redisun.zadd(key, 2.0, "two");
        redisun.zadd(key, 3.0, "three");

        CompletableFuture<List<ZRangeCommand.Tuple>> future = redisun.asyncZrange(key, 0, -1, cmd -> cmd.withScores());
        List<ZRangeCommand.Tuple> result = future.get();
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("one", result.get(0).getMember());
        Assert.assertEquals(1, result.get(0).getScore(), 0);
        Assert.assertEquals("two", result.get(1).getMember());
        Assert.assertEquals(2, result.get(1).getScore(), 0);
        Assert.assertEquals("three", result.get(2).getMember());
        Assert.assertEquals(3, result.get(2).getScore(), 0);

        redisun.del(key);
    }

    @Test(expected = RedisunException.class)
    public void testZRangeWithNullKey() {
        redisun.zrange(null, 0, -1);
    }

    // ==================== 异步方法测试 ====================

    @Test
    public void testAsyncSortedSetCommands() throws Exception {
        String key = topic + ":async";

        CompletableFuture<Integer> zaddFuture = redisun.asyncZadd(key, 1.0, "member");
        Assert.assertEquals(Integer.valueOf(1), zaddFuture.get());

        CompletableFuture<Long> zremFuture = redisun.asyncZrem(key, "member");
        Assert.assertEquals(Long.valueOf(1), zremFuture.get());

        // 重新添加用于后续测试
        redisun.zadd(key, 1.0, "m1");
        redisun.zadd(key, 2.0, "m2");

        CompletableFuture<List<String>> zrangeFuture = redisun.asyncZrange(key, 0, -1);
        List<String> members = zrangeFuture.get();
        Assert.assertEquals(2, members.size());

        redisun.del(key);
    }

    // ==================== 边界条件测试 ====================

    @Test
    public void testSortedSetEdgeCases() {
        String key = topic + ":zset-edge";

        redisun.del(key);

        // 测试空字符串成员
        redisun.zadd(key, 1.0, "");
        Assert.assertEquals(Double.valueOf(1.0), redisun.zscore(key, ""));

        // 测试包含空格的成员
        redisun.zadd(key, 2.0, "  ");

        // 测试特殊字符
        redisun.zadd(key, 3.0, "!@#$%^&*()");

        // 测试Unicode
        redisun.zadd(key, 4.0, "中文🎉");

        // 测试相同分数的多个成员
        for (int i = 0; i < 10; i++) {
            redisun.zadd(key, 5.0, "same-score-" + i);
        }
        List<String> result = redisun.zrange(key, 0, -1);
        // 此时应该有: 空字符串 + 空格 + 特殊字符 + Unicode + 10个相同分数成员 = 14个
        Assert.assertEquals(14, result.size());

        // 测试大量成员 - 先清空集合再测试
        redisun.del(key);
        for (int i = 0; i < 1000; i++) {
            redisun.zadd(key, i, "member" + i);
        }
        Assert.assertEquals(1000, redisun.zrange(key, 0, -1).size());

        redisun.del(key);
    }

    @Test
    public void testSortedSetTypeOperations() {
        String key = topic + ":zset-type";

        redisun.del(key);

        // 对有序集合执行字符串操作应该失败
        redisun.zadd(key, 1.0, "member");

        try {
            redisun.incr(key);
            Assert.fail("INCR on zset should throw exception");
        } catch (RedisunException e) {
            // Expected
        }

        try {
            redisun.append(key, "suffix");
            Assert.fail("APPEND on zset should throw exception");
        } catch (RedisunException e) {
            // Expected
        }

        redisun.del(key);
    }
}
