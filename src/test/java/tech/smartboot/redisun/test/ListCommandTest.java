package tech.smartboot.redisun.test;

import org.junit.Assert;
import org.junit.Test;
import tech.smartboot.redisun.RedisunException;

import java.util.concurrent.CompletableFuture;

/**
 * List类型命令测试类
 * 测试LPUSH, RPUSH, LPOP, RPOP等命令
 *
 * @author 三刀
 * @version v1.0 10/21/25
 */
public class ListCommandTest extends AbstractRedisunTest {

    // ==================== LPUSH命令测试 ====================

    @Test
    public void testLPushNewList() {
        String key = topic + ":lpush-new";
        redisun.del(key);

        // 推入新列表返回列表长度
        Assert.assertEquals(1, redisun.lpush(key, "value1"));
        Assert.assertEquals("value1", redisun.lpop(key));

        redisun.del(key);
    }

    @Test
    public void testLPushExisting() {
        String key = topic + ":lpush-existing";
        redisun.del(key);

        redisun.lpush(key, "first");
        Assert.assertEquals(2, redisun.lpush(key, "second"));

        // LPUSH插入到头部，所以顺序是second, first
        Assert.assertEquals("second", redisun.lpop(key));
        Assert.assertEquals("first", redisun.lpop(key));

        redisun.del(key);
    }

    @Test
    public void testLPushMultiple() {
        String key = topic + ":lpush-multi";
        redisun.del(key);

        // 一次推入多个元素
        Assert.assertEquals(3, redisun.lpush(key, "value1", "value2", "value3"));

        // 验证顺序: LPUSH插入到头部，所以顺序是value3, value2, value1
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

    @Test
    public void testLPushWrongType() {
        String key = topic + ":lpush-wrong-type";
        redisun.set(key, "string-value");

        try {
            redisun.lpush(key, "value");
            Assert.fail("LPUSH on string type should throw exception");
        } catch (RedisunException e) {
            // Expected - WRONGTYPE error
        }

        redisun.del(key);
    }

    // ==================== RPUSH命令测试 ====================

    @Test
    public void testRPushNewList() {
        String key = topic + ":rpush-new";
        redisun.del(key);

        // 推入新列表返回列表长度
        Assert.assertEquals(1, redisun.rpush(key, "value1"));
        Assert.assertEquals("value1", redisun.rpop(key));

        redisun.del(key);
    }

    @Test
    public void testRPushExisting() {
        String key = topic + ":rpush-existing";
        redisun.del(key);

        redisun.rpush(key, "first");
        Assert.assertEquals(2, redisun.rpush(key, "second"));

        // RPUSH插入到尾部，所以顺序是first, second
        Assert.assertEquals("second", redisun.rpop(key));
        Assert.assertEquals("first", redisun.rpop(key));

        redisun.del(key);
    }

    @Test
    public void testRPushMultiple() {
        String key = topic + ":rpush-multi";
        redisun.del(key);

        // 一次推入多个元素
        Assert.assertEquals(3, redisun.rpush(key, "value1", "value2", "value3"));

        // 验证顺序: RPUSH插入到尾部，所以顺序是value1, value2, value3
        Assert.assertEquals("value3", redisun.rpop(key));
        Assert.assertEquals("value2", redisun.rpop(key));
        Assert.assertEquals("value1", redisun.rpop(key));

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

    @Test
    public void testRPushWrongType() {
        String key = topic + ":rpush-wrong-type";
        redisun.set(key, "string-value");

        try {
            redisun.rpush(key, "value");
            Assert.fail("RPUSH on string type should throw exception");
        } catch (RedisunException e) {
            // Expected - WRONGTYPE error
        }

        redisun.del(key);
    }

    // ==================== LPOP命令测试 ====================

    @Test
    public void testLPopExisting() {
        String key = topic + ":lpop-existing";
        redisun.del(key);

        redisun.rpush(key, "value1", "value2", "value3");

        Assert.assertEquals("value1", redisun.lpop(key));
        Assert.assertEquals("value2", redisun.lpop(key));
        Assert.assertEquals("value3", redisun.lpop(key));

        redisun.del(key);
    }

    @Test
    public void testLPopEmpty() {
        String key = topic + ":lpop-empty";
        redisun.del(key);

        Assert.assertNull(redisun.lpop(key));

        redisun.del(key);
    }

    @Test
    public void testLPopNonExistent() {
        String key = topic + ":lpop-non";
        redisun.del(key);

        Assert.assertNull(redisun.lpop(key));
    }

    @Test(expected = RedisunException.class)
    public void testLPopWithNullKey() {
        redisun.lpop(null);
    }

    @Test
    public void testLPopWrongType() {
        String key = topic + ":lpop-wrong-type";
        redisun.set(key, "string-value");

        try {
            redisun.lpop(key);
            Assert.fail("LPOP on string type should throw exception");
        } catch (RedisunException e) {
            // Expected - WRONGTYPE error
        }

        redisun.del(key);
    }

    // ==================== RPOP命令测试 ====================

    @Test
    public void testRPopExisting() {
        String key = topic + ":rpop-existing";
        redisun.del(key);

        redisun.rpush(key, "value1", "value2", "value3");

        Assert.assertEquals("value3", redisun.rpop(key));
        Assert.assertEquals("value2", redisun.rpop(key));
        Assert.assertEquals("value1", redisun.rpop(key));

        redisun.del(key);
    }

    @Test
    public void testRPopEmpty() {
        String key = topic + ":rpop-empty";
        redisun.del(key);

        Assert.assertNull(redisun.rpop(key));

        redisun.del(key);
    }

    @Test
    public void testRPopNonExistent() {
        String key = topic + ":rpop-non";
        redisun.del(key);

        Assert.assertNull(redisun.rpop(key));
    }

    @Test(expected = RedisunException.class)
    public void testRPopWithNullKey() {
        redisun.rpop(null);
    }

    @Test
    public void testRPopWrongType() {
        String key = topic + ":rpop-wrong-type";
        redisun.set(key, "string-value");

        try {
            redisun.rpop(key);
            Assert.fail("RPOP on string type should throw exception");
        } catch (RedisunException e) {
            // Expected - WRONGTYPE error
        }

        redisun.del(key);
    }

    // ==================== 组合操作测试 ====================

    @Test
    public void testListFIFO() {
        String key = topic + ":fifo";
        redisun.del(key);

        // FIFO队列: RPUSH + LPOP
        redisun.rpush(key, "first");
        redisun.rpush(key, "second");
        redisun.rpush(key, "third");

        Assert.assertEquals("first", redisun.lpop(key));
        Assert.assertEquals("second", redisun.lpop(key));
        Assert.assertEquals("third", redisun.lpop(key));

        redisun.del(key);
    }

    @Test
    public void testListLIFO() {
        String key = topic + ":lifo";
        redisun.del(key);

        // LIFO栈: LPUSH + LPOP
        redisun.lpush(key, "first");
        redisun.lpush(key, "second");
        redisun.lpush(key, "third");

        Assert.assertEquals("third", redisun.lpop(key));
        Assert.assertEquals("second", redisun.lpop(key));
        Assert.assertEquals("first", redisun.lpop(key));

        redisun.del(key);
    }

    @Test
    public void testListMixed() {
        String key = topic + ":mixed";
        redisun.del(key);

        // 混合操作
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
    public void testAsyncLPush() throws Exception {
        String key = topic + ":async-lpush";
        redisun.del(key);

        CompletableFuture<Long> future = redisun.asyncLpush(key, "v1", "v2");
        Assert.assertEquals(Long.valueOf(2), future.get());

        redisun.del(key);
    }

    @Test
    public void testAsyncRPush() throws Exception {
        String key = topic + ":async-rpush";
        redisun.del(key);

        CompletableFuture<Long> future = redisun.asyncRpush(key, "v1", "v2");
        Assert.assertEquals(Long.valueOf(2), future.get());

        redisun.del(key);
    }

    @Test
    public void testAsyncLPop() throws Exception {
        String key = topic + ":async-lpop";
        redisun.del(key);

        redisun.lpush(key, "value");

        CompletableFuture<String> future = redisun.asyncLpop(key);
        Assert.assertEquals("value", future.get());

        // 空列表
        CompletableFuture<String> emptyFuture = redisun.asyncLpop(key);
        Assert.assertNull(emptyFuture.get());

        redisun.del(key);
    }

    @Test
    public void testAsyncRPop() throws Exception {
        String key = topic + ":async-rpop";
        redisun.del(key);

        redisun.rpush(key, "value");

        CompletableFuture<String> future = redisun.asyncRpop(key);
        Assert.assertEquals("value", future.get());

        // 空列表
        CompletableFuture<String> emptyFuture = redisun.asyncRpop(key);
        Assert.assertNull(emptyFuture.get());

        redisun.del(key);
    }

    @Test
    public void testAsyncListCommands() throws Exception {
        String key = topic + ":async";
        redisun.del(key);

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
    public void testLPushEmptyValue() {
        String key = topic + ":lpush-empty";
        redisun.del(key);

        redisun.lpush(key, "");
        Assert.assertEquals("", redisun.lpop(key));

        redisun.del(key);
    }

    @Test
    public void testLPushSpecialChars() {
        String key = topic + ":lpush-special";
        redisun.del(key);

        redisun.lpush(key, "!@#$%^&*()");
        Assert.assertEquals("!@#$%^&*()", redisun.lpop(key));

        redisun.del(key);
    }

    @Test
    public void testLPushUnicode() {
        String key = topic + ":lpush-unicode";
        redisun.del(key);

        redisun.lpush(key, "中文🎉");
        Assert.assertEquals("中文🎉", redisun.lpop(key));

        redisun.del(key);
    }

    @Test
    public void testLPushManyElements() {
        String key = topic + ":lpush-many";
        redisun.del(key);

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
    public void testListType() {
        String key = topic + ":list-type";
        redisun.del(key);

        redisun.lpush(key, "value");
        Assert.assertEquals("list", redisun.type(key));

        redisun.del(key);
    }

    @Test
    public void testListOverwrite() {
        String key = topic + ":list-overwrite";
        redisun.del(key);

        // 先作为list
        redisun.lpush(key, "list-value");
        Assert.assertEquals("list-value", redisun.lpop(key));

        // 覆盖为string
        redisun.set(key, "string-value");
        Assert.assertEquals("string-value", redisun.get(key));

        // 再次作为list
        redisun.lpush(key, "new-list-value");
        Assert.assertEquals("new-list-value", redisun.lpop(key));

        redisun.del(key);
    }

    @Test
    public void testConcurrentListOperations() throws InterruptedException {
        String key = topic + ":concurrent";
        int threadCount = 10;
        int operationsPerThread = 50;

        redisun.del(key);

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    redisun.lpush(key, "thread-" + threadId + "-" + j);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // 验证所有元素都被推入
        int count = 0;
        while (redisun.lpop(key) != null) {
            count++;
        }
        Assert.assertEquals(threadCount * operationsPerThread, count);

        redisun.del(key);
    }
}
