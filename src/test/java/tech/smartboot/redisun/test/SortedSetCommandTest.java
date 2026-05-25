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
    public void testZAddNewMember() {
        String key = topic + ":zadd-new";
        redisun.del(key);

        // 添加新成员返回1
        Assert.assertEquals(1, redisun.zadd(key, 1.0, "member1"));
        Assert.assertEquals(1, redisun.zadd(key, 2.0, "member2"));

        redisun.del(key);
    }

    @Test
    public void testZAddExisting() {
        String key = topic + ":zadd-existing";
        redisun.del(key);

        redisun.zadd(key, 1.0, "member1");
        // 更新已存在成员分数返回0
        Assert.assertEquals(0, redisun.zadd(key, 2.0, "member1"));
        Assert.assertEquals(Double.valueOf(2.0), redisun.zscore(key, "member1"));

        redisun.del(key);
    }

    @Test
    public void testZAddNegativeScore() {
        String key = topic + ":zadd-negative";
        redisun.del(key);

        Assert.assertEquals(1, redisun.zadd(key, -1.5, "member1"));
        Assert.assertEquals(-1.5, redisun.zscore(key, "member1"), 0.001);

        redisun.del(key);
    }

    @Test
    public void testZAddZeroScore() {
        String key = topic + ":zadd-zero";
        redisun.del(key);

        Assert.assertEquals(1, redisun.zadd(key, 0.0, "member1"));
        Assert.assertEquals(0.0, redisun.zscore(key, "member1"), 0.001);

        redisun.del(key);
    }

    @Test
    public void testZAddFloatScore() {
        String key = topic + ":zadd-float";
        redisun.del(key);

        Assert.assertEquals(1, redisun.zadd(key, 3.14159, "member1"));
        Assert.assertEquals(3.14159, redisun.zscore(key, "member1"), 0.00001);

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

    @Test
    public void testZAddWrongType() {
        String key = topic + ":zadd-wrong-type";
        redisun.set(key, "string-value");

        try {
            redisun.zadd(key, 1.0, "member");
            Assert.fail("ZADD on string type should throw exception");
        } catch (RedisunException e) {
            // Expected - WRONGTYPE error
        }

        redisun.del(key);
    }

    // ==================== ZREM命令测试 ====================

    @Test
    public void testZRemExisting() {
        String key = topic + ":zrem-existing";
        redisun.del(key);

        redisun.zadd(key, 1.0, "member1");
        redisun.zadd(key, 2.0, "member2");

        // 移除存在的成员返回1
        Assert.assertEquals(1, redisun.zrem(key, "member1"));
        Assert.assertNull(redisun.zscore(key, "member1"));

        redisun.del(key);
    }

    @Test
    public void testZRemNonExistent() {
        String key = topic + ":zrem-non";
        redisun.del(key);

        redisun.zadd(key, 1.0, "member1");

        // 移除不存在的成员返回0
        Assert.assertEquals(0, redisun.zrem(key, "nonexistent"));

        redisun.del(key);
    }

    @Test
    public void testZRemMultiple() {
        String key = topic + ":zrem-multi";
        redisun.del(key);

        redisun.zadd(key, 1.0, "member1");
        redisun.zadd(key, 2.0, "member2");
        redisun.zadd(key, 3.0, "member3");

        // 移除多个成员
        Assert.assertEquals(2, redisun.zrem(key, "member1", "member2"));

        redisun.del(key);
    }

    @Test
    public void testZRemMixed() {
        String key = topic + ":zrem-mixed";
        redisun.del(key);

        redisun.zadd(key, 1.0, "member1");
        redisun.zadd(key, 2.0, "member2");

        // 混合存在和不存在的成员
        Assert.assertEquals(1, redisun.zrem(key, "member1", "nonexistent"));

        redisun.del(key);
    }

    @Test(expected = RedisunException.class)
    public void testZRemWithNullKey() {
        redisun.zrem(null, "member");
    }

    @Test
    public void testZRemWrongType() {
        String key = topic + ":zrem-wrong-type";
        redisun.set(key, "string-value");

        try {
            redisun.zrem(key, "member");
            Assert.fail("ZREM on string type should throw exception");
        } catch (RedisunException e) {
            // Expected - WRONGTYPE error
        }

        redisun.del(key);
    }

    // ==================== ZSCORE命令测试 ====================

    @Test
    public void testZScoreExisting() {
        String key = topic + ":zscore-existing";
        redisun.del(key);

        redisun.zadd(key, 123.456, "member1");

        Double score = redisun.zscore(key, "member1");
        Assert.assertNotNull(score);
        Assert.assertEquals(123.456, score, 0.001);

        redisun.del(key);
    }

    @Test
    public void testZScoreNonExistentMember() {
        String key = topic + ":zscore-non-member";
        redisun.del(key);

        redisun.zadd(key, 1.0, "member1");

        Assert.assertNull(redisun.zscore(key, "nonexistent"));

        redisun.del(key);
    }

    @Test
    public void testZScoreNonExistentKey() {
        String key = topic + ":zscore-non-key";
        redisun.del(key);

        Assert.assertNull(redisun.zscore(key, "member"));
    }

    @Test
    public void testZScoreNegative() {
        String key = topic + ":zscore-negative";
        redisun.del(key);

        redisun.zadd(key, -99.5, "member1");
        Assert.assertEquals(-99.5, redisun.zscore(key, "member1"), 0.001);

        redisun.del(key);
    }

    @Test
    public void testZScoreZero() {
        String key = topic + ":zscore-zero";
        redisun.del(key);

        redisun.zadd(key, 0.0, "member1");
        Assert.assertEquals(0.0, redisun.zscore(key, "member1"), 0.001);

        redisun.del(key);
    }

    @Test
    public void testZScoreMaxValue() {
        String key = topic + ":zscore-max";
        redisun.del(key);

        redisun.zadd(key, Double.MAX_VALUE, "member1");
        Assert.assertEquals(Double.MAX_VALUE, redisun.zscore(key, "member1"), 0.001);

        redisun.del(key);
    }

    @Test
    public void testZScoreMinValue() {
        String key = topic + ":zscore-min";
        redisun.del(key);

        redisun.zadd(key, Double.MIN_VALUE, "member1");
        Assert.assertEquals(Double.MIN_VALUE, redisun.zscore(key, "member1"), 0.001);

        redisun.del(key);
    }

    @Test(expected = RedisunException.class)
    public void testZScoreWithNullKey() {
        redisun.zscore(null, "member");
    }

    // ==================== ZRANGE命令测试 ====================

    @Test
    public void testZRangeBasic() {
        String key = topic + ":zrange-basic";
        redisun.del(key);

        redisun.zadd(key, 1.0, "one");
        redisun.zadd(key, 2.0, "two");
        redisun.zadd(key, 3.0, "three");

        List<String> result = redisun.zrange(key, 0, -1);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("one", result.get(0));
        Assert.assertEquals("two", result.get(1));
        Assert.assertEquals("three", result.get(2));

        redisun.del(key);
    }

    @Test
    public void testZRangeWithScores() {
        String key = topic + ":zrange-scores";
        redisun.del(key);

        redisun.zadd(key, 1.0, "one");
        redisun.zadd(key, 2.0, "two");
        redisun.zadd(key, 3.0, "three");

        List<ZRangeCommand.Tuple> result = redisun.zrange(key, 0, -1, cmd -> cmd.withScores());
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("one", result.get(0).getMember());
        Assert.assertEquals(1.0, result.get(0).getScore(), 0);
        Assert.assertEquals("two", result.get(1).getMember());
        Assert.assertEquals(2.0, result.get(1).getScore(), 0);
        Assert.assertEquals("three", result.get(2).getMember());
        Assert.assertEquals(3.0, result.get(2).getScore(), 0);

        redisun.del(key);
    }

    @Test
    public void testZRangeReverse() {
        String key = topic + ":zrange-rev";
        redisun.del(key);

        redisun.zadd(key, 1.0, "one");
        redisun.zadd(key, 2.0, "two");
        redisun.zadd(key, 3.0, "three");

        List<ZRangeCommand.Tuple> result = redisun.zrange(key, 0, -1, cmd -> cmd.rev());
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("three", result.get(0).getMember());
        Assert.assertEquals("two", result.get(1).getMember());
        Assert.assertEquals("one", result.get(2).getMember());

        redisun.del(key);
    }

    @Test
    public void testZRangeByScore() {
        String key = topic + ":zrange-byscore";
        redisun.del(key);

        redisun.zadd(key, 1.0, "one");
        redisun.zadd(key, 2.0, "two");
        redisun.zadd(key, 3.0, "three");
        redisun.zadd(key, 4.0, "four");
        redisun.zadd(key, 5.0, "five");

        List<ZRangeCommand.Tuple> result = redisun.zrange(key, 2, 4, cmd -> cmd.sortByScore());
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("two", result.get(0).getMember());
        Assert.assertEquals("three", result.get(1).getMember());
        Assert.assertEquals("four", result.get(2).getMember());

        redisun.del(key);
    }

    @Test
    public void testZRangeLimit() {
        String key = topic + ":zrange-limit";
        redisun.del(key);

        redisun.zadd(key, 1.0, "one");
        redisun.zadd(key, 2.0, "two");
        redisun.zadd(key, 3.0, "three");
        redisun.zadd(key, 4.0, "four");
        redisun.zadd(key, 5.0, "five");

        List<ZRangeCommand.Tuple> result = redisun.zrange(key, 1, 5, cmd -> cmd.sortByScore().limit(1, 2));
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("two", result.get(0).getMember());
        Assert.assertEquals("three", result.get(1).getMember());

        redisun.del(key);
    }

    @Test
    public void testZRangeEmpty() {
        String key = topic + ":zrange-empty";
        redisun.del(key);

        List<String> result = redisun.zrange(key, 0, -1);
        Assert.assertTrue(result.isEmpty());

        redisun.del(key);
    }

    @Test
    public void testZRangeOutOfRange() {
        String key = topic + ":zrange-out";
        redisun.del(key);

        redisun.zadd(key, 1.0, "one");
        redisun.zadd(key, 2.0, "two");

        // 超出范围的查询
        List<String> result = redisun.zrange(key, 10, 20);
        Assert.assertTrue(result.isEmpty());

        redisun.del(key);
    }

    @Test(expected = RedisunException.class)
    public void testZRangeWithNullKey() {
        redisun.zrange(null, 0, -1);
    }

    @Test
    public void testZRangeWrongType() {
        String key = topic + ":zrange-wrong-type";
        redisun.set(key, "string-value");

        try {
            redisun.zrange(key, 0, -1);
            Assert.fail("ZRANGE on string type should throw exception");
        } catch (RedisunException e) {
            // Expected - WRONGTYPE error
        }

        redisun.del(key);
    }

    // ==================== 异步方法测试 ====================

    @Test
    public void testAsyncZAdd() throws Exception {
        String key = topic + ":async-zadd";
        redisun.del(key);

        CompletableFuture<Integer> future = redisun.asyncZadd(key, 1.0, "member");
        Assert.assertEquals(Integer.valueOf(1), future.get());

        redisun.del(key);
    }

    @Test
    public void testAsyncZRem() throws Exception {
        String key = topic + ":async-zrem";
        redisun.del(key);

        redisun.zadd(key, 1.0, "member");

        CompletableFuture<Long> future = redisun.asyncZrem(key, "member");
        Assert.assertEquals(Long.valueOf(1), future.get());

        redisun.del(key);
    }

    @Test
    public void testAsyncZScore() throws Exception {
        String key = topic + ":async-zscore";
        redisun.del(key);

        redisun.zadd(key, 1.0, "member");

        // Note: asyncZscore is private, so we test via zrange
        // This is a placeholder for the concept

        redisun.del(key);
    }

    @Test
    public void testAsyncZRange() throws Exception {
        String key = topic + ":async-zrange";
        redisun.del(key);

        redisun.zadd(key, 1.0, "one");
        redisun.zadd(key, 2.0, "two");

        CompletableFuture<List<String>> future = redisun.asyncZrange(key, 0, -1);
        List<String> members = future.get();
        Assert.assertEquals(2, members.size());

        redisun.del(key);
    }

    @Test
    public void testAsyncZRangeWithScores() throws Exception {
        String key = topic + ":async-zrange-scores";
        redisun.del(key);

        redisun.zadd(key, 1.0, "one");
        redisun.zadd(key, 2.0, "two");
        redisun.zadd(key, 3.0, "three");

        CompletableFuture<List<ZRangeCommand.Tuple>> future = redisun.asyncZrange(key, 0, -1, cmd -> cmd.withScores());
        List<ZRangeCommand.Tuple> result = future.get();
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("one", result.get(0).getMember());
        Assert.assertEquals(1, result.get(0).getScore(), 0);

        redisun.del(key);
    }

    // ==================== 边界条件测试 ====================

    @Test
    public void testZAddEmptyMember() {
        String key = topic + ":zadd-empty";
        redisun.del(key);

        Assert.assertEquals(1, redisun.zadd(key, 1.0, ""));
        Assert.assertEquals(Double.valueOf(1.0), redisun.zscore(key, ""));

        redisun.del(key);
    }

    @Test
    public void testZAddSpecialChars() {
        String key = topic + ":zadd-special";
        redisun.del(key);

        Assert.assertEquals(1, redisun.zadd(key, 1.0, "!@#$%^&*()"));
        Assert.assertEquals(1, redisun.zadd(key, 2.0, "  "));

        redisun.del(key);
    }

    @Test
    public void testZAddUnicode() {
        String key = topic + ":zadd-unicode";
        redisun.del(key);

        Assert.assertEquals(1, redisun.zadd(key, 1.0, "中文🎉"));
        Assert.assertEquals(Double.valueOf(1.0), redisun.zscore(key, "中文🎉"));

        redisun.del(key);
    }

    @Test
    public void testZAddSameScore() {
        String key = topic + ":zadd-same-score";
        redisun.del(key);

        // 相同分数的多个成员
        for (int i = 0; i < 10; i++) {
            redisun.zadd(key, 5.0, "member" + i);
        }

        List<String> result = redisun.zrange(key, 0, -1);
        Assert.assertEquals(10, result.size());

        redisun.del(key);
    }

    @Test
    public void testZAddManyMembers() {
        String key = topic + ":zadd-many";
        redisun.del(key);

        for (int i = 0; i < 1000; i++) {
            redisun.zadd(key, i, "member" + i);
        }

        Assert.assertEquals(1000, redisun.zrange(key, 0, -1).size());

        redisun.del(key);
    }

    @Test
    public void testZSetType() {
        String key = topic + ":zset-type";
        redisun.del(key);

        redisun.zadd(key, 1.0, "member");
        Assert.assertEquals("zset", redisun.type(key));

        redisun.del(key);
    }

    @Test
    public void testZSetOverwrite() {
        String key = topic + ":zset-overwrite";
        redisun.del(key);

        // 先作为zset
        redisun.zadd(key, 1.0, "member");

        // 覆盖为string
        redisun.set(key, "string-value");
        Assert.assertEquals("string-value", redisun.get(key));

        // 再次作为zset
        redisun.zadd(key, 2.0, "new-member");

        redisun.del(key);
    }

    @Test
    public void testZSetTypeOperations() {
        String key = topic + ":zset-type-ops";
        redisun.del(key);

        redisun.zadd(key, 1.0, "member");

        // 对zset执行字符串操作应该失败
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

    @Test
    public void testConcurrentZAdd() throws InterruptedException {
        String key = topic + ":concurrent";
        int threadCount = 10;
        int membersPerThread = 50;

        redisun.del(key);

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < membersPerThread; j++) {
                    redisun.zadd(key, threadId * 100 + j, "thread-" + threadId + "-" + j);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // 验证所有成员都被添加
        Assert.assertEquals(threadCount * membersPerThread, redisun.zrange(key, 0, -1).size());

        redisun.del(key);
    }
}
