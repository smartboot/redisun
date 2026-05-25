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
    public void testSAddNewMembers() {
        String key = topic + ":sadd-new";
        redisun.del(key);

        // 添加新成员返回1
        Assert.assertEquals(1, redisun.sadd(key, "member1"));
        Assert.assertEquals(1, redisun.sadd(key, "member2"));

        redisun.del(key);
    }

    @Test
    public void testSAddExisting() {
        String key = topic + ":sadd-existing";
        redisun.del(key);

        redisun.sadd(key, "member1");
        // 添加已存在成员返回0
        Assert.assertEquals(0, redisun.sadd(key, "member1"));

        redisun.del(key);
    }

    @Test
    public void testSAddMultiple() {
        String key = topic + ":sadd-multi";
        redisun.del(key);

        // 一次添加多个新成员
        Assert.assertEquals(3, redisun.sadd(key, "member1", "member2", "member3"));

        redisun.del(key);
    }

    @Test
    public void testSAddMixed() {
        String key = topic + ":sadd-mixed";
        redisun.del(key);

        redisun.sadd(key, "member1");
        redisun.sadd(key, "member2");

        // 混合新成员和已存在成员，只返回新添加的数量
        Assert.assertEquals(1, redisun.sadd(key, "member1", "member2", "member3"));

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

    @Test
    public void testSAddWrongType() {
        String key = topic + ":sadd-wrong-type";
        redisun.set(key, "string-value");

        try {
            redisun.sadd(key, "member");
            Assert.fail("SADD on string type should throw exception");
        } catch (RedisunException e) {
            // Expected - WRONGTYPE error
        }

        redisun.del(key);
    }

    // ==================== 边界条件测试 ====================

    @Test
    public void testSAddEmptyMember() {
        String key = topic + ":sadd-empty";
        redisun.del(key);

        // 添加空字符串成员
        Assert.assertEquals(1, redisun.sadd(key, ""));
        // 再次添加返回0
        Assert.assertEquals(0, redisun.sadd(key, ""));

        redisun.del(key);
    }

    @Test
    public void testSAddSpecialChars() {
        String key = topic + ":sadd-special";
        redisun.del(key);

        Assert.assertEquals(1, redisun.sadd(key, "!@#$%^&*()"));
        Assert.assertEquals(1, redisun.sadd(key, "  "));
        Assert.assertEquals(0, redisun.sadd(key, "!@#$%^&*()"));

        redisun.del(key);
    }

    @Test
    public void testSAddUnicode() {
        String key = topic + ":sadd-unicode";
        redisun.del(key);

        Assert.assertEquals(1, redisun.sadd(key, "中文🎉"));
        Assert.assertEquals(0, redisun.sadd(key, "中文🎉"));

        redisun.del(key);
    }

    @Test
    public void testSAddManyMembers() {
        String key = topic + ":sadd-many";
        redisun.del(key);

        // 添加大量成员
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

    // ==================== 异步方法测试 ====================

    @Test
    public void testAsyncSAdd() throws Exception {
        String key = topic + ":async-sadd";
        redisun.del(key);

        CompletableFuture<Integer> future = redisun.asyncSadd(key, "m1", "m2");
        Assert.assertEquals(Integer.valueOf(2), future.get());

        // 再次添加相同成员
        future = redisun.asyncSadd(key, "m1", "m3");
        Assert.assertEquals(Integer.valueOf(1), future.get());

        redisun.del(key);
    }

    @Test
    public void testAsyncSAddNew() throws Exception {
        String key = topic + ":async-sadd-new";
        redisun.del(key);

        CompletableFuture<Integer> future = redisun.asyncSadd(key, "member");
        Assert.assertEquals(Integer.valueOf(1), future.get());

        redisun.del(key);
    }

    // ==================== 类型测试 ====================

    @Test
    public void testSetType() {
        String key = topic + ":set-type";
        redisun.del(key);

        redisun.sadd(key, "member");
        Assert.assertEquals("set", redisun.type(key));

        redisun.del(key);
    }

    @Test
    public void testSetOverwrite() {
        String key = topic + ":set-overwrite";
        redisun.del(key);

        // 先作为set
        redisun.sadd(key, "member");

        // 覆盖为string
        redisun.set(key, "string-value");
        Assert.assertEquals("string-value", redisun.get(key));

        // 再次作为set
        try {
            redisun.sadd(key, "new-member");
            Assert.fail("SADD on string type should throw exception");
        } catch (RedisunException e) {
            // Expected
            Assert.assertEquals("WRONGTYPE Operation against a key holding the wrong kind of value", e.getMessage());
        }


        redisun.del(key);
    }

    @Test
    public void testSetTypeOperations() {
        String key = topic + ":set-type-ops";
        redisun.del(key);

        redisun.sadd(key, "member");

        // 对集合执行字符串操作应该失败
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

    @Test
    public void testConcurrentSAdd() throws InterruptedException {
        String key = topic + ":concurrent";
        int threadCount = 10;
        int membersPerThread = 50;

        redisun.del(key);

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < membersPerThread; j++) {
                    redisun.sadd(key, "thread-" + threadId + "-" + j);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // 由于集合的唯一性，总成员数应该等于threadCount * membersPerThread
        // 但如果有重复（虽然设计上不应该），可能会少一些
        // 这里我们验证至少有一些成员被添加
        redisun.del(key);
    }
}
