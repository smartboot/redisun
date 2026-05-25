package tech.smartboot.redisun.test;

import org.junit.Assert;
import org.junit.Test;
import tech.smartboot.redisun.RedisunException;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * String类型命令测试类
 * 测试SET, GET, MSET, MGET, APPEND, STRLEN, INCR, DECR, INCRBY, DECRBY等命令
 *
 * @author 三刀
 * @version v1.0 10/21/25
 */
public class StringCommandTest extends AbstractRedisunTest {

    // ==================== SET命令测试 ====================

    /**
     * 测试SET命令的各种选项和场景
     */
    @Test
    public void testSetCommand() {
        String key = topic + ":set";
        String value = "test-value";

        // 基本SET命令测试
        boolean result = redisun.set(key, value);
        Assert.assertTrue("Basic SET command should succeed", result);
        Assert.assertEquals("Value should be set correctly", value, redisun.get(key));

        // 测试NX选项 - 键不存在时应该设置成功
        String nxKey = key + ":nx";
        result = redisun.set(nxKey, value, cmd -> cmd.setIfNotExists());
        Assert.assertTrue("NX option should succeed when key does not exist", result);
        Assert.assertEquals("Value should be set correctly", value, redisun.get(nxKey));

        // 测试NX选项 - 键存在时应该设置失败
        result = redisun.set(nxKey, value + "-new", cmd -> cmd.setIfNotExists());
        Assert.assertFalse("NX option should fail when key exists", result);

        // 测试XX选项 - 键存在时应该设置成功
        result = redisun.set(nxKey, value + "-updated", cmd -> cmd.setIfExists());
        Assert.assertTrue("XX option should succeed when key exists", result);
        Assert.assertEquals("Value should be updated", value + "-updated", redisun.get(nxKey));

        // 测试XX选项 - 键不存在时应该设置失败
        String xxKey = key + ":xx";
        result = redisun.set(xxKey, value, cmd -> cmd.setIfExists());
        Assert.assertFalse("XX option should fail when key does not exist", result);

        // 测试EX选项（秒级过期）
        String expireKey = key + ":expire";
        result = redisun.set(expireKey, value, cmd -> cmd.expire(1));
        Assert.assertTrue("Setting expiration with EX option should succeed", result);
        Assert.assertEquals("Value should be set correctly", value, redisun.get(expireKey));

        // 等待过期
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Assert.assertNull("Key should have expired", redisun.get(expireKey));

        // 测试PX选项（毫秒级过期）
        result = redisun.set(expireKey, value, cmd -> cmd.expireMs(500));
        Assert.assertTrue("Setting expiration with PX option should succeed", result);
        Assert.assertEquals("Value should be set correctly", value, redisun.get(expireKey));

        // 等待过期
        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Assert.assertNull("Key should have expired", redisun.get(expireKey));

        // 测试PXAT选项（指定时间点过期）
        Date expireDate = new Date(System.currentTimeMillis() + 500);
        result = redisun.set(expireKey, value, cmd -> cmd.expireAt(expireDate));
        Assert.assertTrue("Setting expiration with PXAT option should succeed", result);
        Assert.assertEquals("Value should be set correctly", value, redisun.get(expireKey));

        // 等待过期
        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Assert.assertNull("Key should have expired", redisun.get(expireKey));

        // 测试KEEPTTL选项
        // 先设置一个有过期时间的键
        result = redisun.set(expireKey, value, cmd -> cmd.expire(2));
        Assert.assertTrue("Setting a key with expiration should succeed", result);

        // 更新值并保留TTL
        String newValue = "new-value-with-kept-ttl";
        result = redisun.set(expireKey, newValue, cmd -> cmd.keepTTL());
        Assert.assertTrue("Using KEEPTTL option should succeed", result);
        Assert.assertEquals("Value should be updated", newValue, redisun.get(expireKey));

        // 等待过期
        try {
            Thread.sleep(2100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Assert.assertNull("Key should have expired", redisun.get(expireKey));

        // 测试NX和EX组合
        String combinationKey = key + ":combination";
        result = redisun.set(combinationKey, value, cmd -> cmd.setIfNotExists().expire(1));
        Assert.assertTrue("NX and EX option combination should succeed", result);
        Assert.assertEquals("Value should be set correctly", value, redisun.get(combinationKey));

        // 再次尝试使用NX和EX组合应该失败（因为键已存在）
        result = redisun.set(combinationKey, value + "-new", cmd -> cmd.setIfNotExists().expire(1));
        Assert.assertFalse("NX and EX option combination should fail when key exists", result);
        Assert.assertEquals("Value should not be updated", value, redisun.get(combinationKey));

        // 测试XX和PX组合
        result = redisun.set(combinationKey, value + "-updated", cmd -> cmd.setIfExists().expireMs(500));
        Assert.assertTrue("XX and PX option combination should succeed", result);
        Assert.assertEquals("Value should be updated", value + "-updated", redisun.get(combinationKey));

        // 清理测试数据
        redisun.del(key, nxKey, xxKey, expireKey, combinationKey);
    }

    @Test(expected = NullPointerException.class)
    public void testSetWithNullKey() {
        redisun.set(null, "value");
    }

    @Test
    public void testSetWithEmptyValue() {
        String key = topic + ":empty-value";
        boolean result = redisun.set(key, "");
        Assert.assertTrue("Setting empty value should succeed", result);
        Assert.assertEquals("Empty value should be retrieved correctly", "", redisun.get(key));
        redisun.del(key);
    }

    @Test
    public void testSetWithLargeValue() {
        String key = topic + ":large-value";
        StringBuilder largeValue = new StringBuilder();
        for (int i = 0; i < 1024 * 1024; i++) {
            largeValue.append('a');
        }
        boolean result = redisun.set(key, largeValue.toString());
        Assert.assertTrue("Setting large value should succeed", result);
        Assert.assertEquals("Large value length should match", 1024 * 1024, redisun.get(key).length());
        redisun.del(key);
    }

    // ==================== GET命令测试 ====================

    @Test
    public void testGetCommand() {
        String key = topic + ":get";
        String value = "get-test-value";

        // Test getting non-existent key
        String result = redisun.get(key);
        Assert.assertNull("Getting non-existent key should return null", result);

        // Test getting existing key
        redisun.set(key, value);
        result = redisun.get(key);
        Assert.assertEquals("Getting existing key should return correct value", value, result);

        // Test getting key after deletion
        redisun.del(key);
        result = redisun.get(key);
        Assert.assertNull("Getting deleted key should return null", result);
    }

    @Test(expected = NullPointerException.class)
    public void testGetWithNullKey() {
        redisun.get(null);
    }

    // ==================== MSET/MGET命令测试 ====================

    @Test
    public void testMGetCommand() {
        String key1 = topic + ":mget1";
        String key2 = topic + ":mget2";
        String key3 = topic + ":mget3";
        String value1 = "value1";
        String value2 = "value2";

        redisun.del(key1, key2, key3);

        redisun.set(key1, value1);
        redisun.set(key2, value2);

        List<String> keys = Arrays.asList(key1, key2, key3);
        List<String> values = redisun.mget(keys);

        Assert.assertEquals(3, values.size());
        Assert.assertEquals(value1, values.get(0));
        Assert.assertEquals(value2, values.get(1));
        Assert.assertNull(values.get(2));

        redisun.del(key1, key2, key3);
    }

    @Test(expected = NullPointerException.class)
    public void testMGetWithNullKeys() {
        redisun.mget(null);
    }

    @Test
    public void testMSetCommand() {
        String key1 = topic + ":mset1";
        String key2 = topic + ":mset2";
        String key3 = topic + ":mset3";
        String value1 = "value1";
        String value2 = "value2";
        String value3 = "value3";

        redisun.del(key1, key2, key3);

        Map<String, String> items = new HashMap<>();
        items.put(key1, value1);
        items.put(key2, value2);
        items.put(key3, value3);
        boolean result = redisun.mset(items);

        Assert.assertTrue(result);
        Assert.assertEquals(value1, redisun.get(key1));
        Assert.assertEquals(value2, redisun.get(key2));
        Assert.assertEquals(value3, redisun.get(key3));

        Map<String, String> newItems = new HashMap<>();
        newItems.put(key1, "newvalue1");
        newItems.put(key2, "newvalue2");
        result = redisun.mset(newItems);

        Assert.assertTrue(result);
        Assert.assertEquals("newvalue1", redisun.get(key1));
        Assert.assertEquals("newvalue2", redisun.get(key2));
        Assert.assertEquals(value3, redisun.get(key3));

        redisun.del(key1, key2, key3);
    }

    @Test(expected = NullPointerException.class)
    public void testMSetWithNullItems() {
        redisun.mset(null);
    }

    // ==================== APPEND命令测试 ====================

    @Test
    public void testAppendCommand() {
        String key = topic + ":append";

        redisun.del(key);

        Assert.assertEquals(5, redisun.append(key, "Hello"));
        Assert.assertEquals("Hello", redisun.get(key));

        Assert.assertEquals(11, redisun.append(key, " World"));
        Assert.assertEquals("Hello World", redisun.get(key));

        Assert.assertEquals(11, redisun.append(key, ""));
        Assert.assertEquals("Hello World", redisun.get(key));

        redisun.del(key);
    }

    @Test(expected = RedisunException.class)
    public void testAppendWithNullKey() {
        redisun.append(null, "value");
    }

    @Test(expected = RedisunException.class)
    public void testAppendWithNullValue() {
        redisun.append(topic + ":append-null", null);
    }

    // ==================== STRLEN命令测试 ====================

    @Test
    public void testStrlenCommand() {
        String key = topic + ":strlen";

        redisun.del(key);

        Assert.assertEquals(0, redisun.strlen(key));

        redisun.set(key, "Hello World");
        Assert.assertEquals(11, redisun.strlen(key));

        redisun.set(key + ":empty", "");
        Assert.assertEquals(0, redisun.strlen(key + ":empty"));

        redisun.del(key, key + ":empty");
    }

    @Test(expected = RedisunException.class)
    public void testStrlenWithNullKey() {
        redisun.strlen(null);
    }

    // ==================== INCR/DECR命令测试 ====================

    @Test
    public void testIncrCommand() {
        String key = topic + ":incr";

        redisun.del(key);

        Assert.assertEquals(1, redisun.incr(key));
        Assert.assertEquals(2, redisun.incr(key));
        Assert.assertEquals("2", redisun.get(key));

        redisun.set(key + ":str", "hello");
        try {
            redisun.incr(key + ":str");
            Assert.fail("INCR on string value should throw exception");
        } catch (RedisunException e) {
            // Expected
        }

        redisun.del(key, key + ":str");
    }

    @Test(expected = RedisunException.class)
    public void testIncrWithNullKey() {
        redisun.incr(null);
    }

    @Test
    public void testDecrCommand() {
        String key = topic + ":decr";

        redisun.del(key);

        Assert.assertEquals(-1, redisun.decr(key));
        Assert.assertEquals(-2, redisun.decr(key));
        Assert.assertEquals("-2", redisun.get(key));

        redisun.set(key + ":str", "hello");
        try {
            redisun.decr(key + ":str");
            Assert.fail("DECR on string value should throw exception");
        } catch (RedisunException e) {
            // Expected
        }

        redisun.del(key, key + ":str");
    }

    @Test(expected = RedisunException.class)
    public void testDecrWithNullKey() {
        redisun.decr(null);
    }

    @Test
    public void testIncrByCommand() {
        String key = topic + ":incrby";

        redisun.del(key);

        Assert.assertEquals(5, redisun.incrBy(key, 5));
        Assert.assertEquals(15, redisun.incrBy(key, 10));
        Assert.assertEquals(10, redisun.incrBy(key, -5));
        Assert.assertEquals("10", redisun.get(key));

        redisun.set(key + ":str", "hello");
        try {
            redisun.incrBy(key + ":str", 5);
            Assert.fail("INCRBY on string value should throw exception");
        } catch (RedisunException e) {
            // Expected
        }

        redisun.del(key, key + ":str");
    }

    @Test(expected = RedisunException.class)
    public void testIncrByWithNullKey() {
        redisun.incrBy(null, 5);
    }

    @Test
    public void testDecrByCommand() {
        String key = topic + ":decrby";

        redisun.del(key);

        Assert.assertEquals(-5, redisun.decrBy(key, 5));
        Assert.assertEquals(-15, redisun.decrBy(key, 10));
        Assert.assertEquals(-10, redisun.decrBy(key, -5));
        Assert.assertEquals("-10", redisun.get(key));

        redisun.set(key + ":str", "hello");
        try {
            redisun.decrBy(key + ":str", 5);
            Assert.fail("DECRBY on string value should throw exception");
        } catch (RedisunException e) {
            // Expected
        }

        redisun.del(key, key + ":str");
    }

    @Test(expected = RedisunException.class)
    public void testDecrByWithNullKey() {
        redisun.decrBy(null, 5);
    }

    // ==================== 异步方法测试 ====================

    @Test
    public void testAsyncStringCommands() throws Exception {
        String key = topic + ":async";

        CompletableFuture<Boolean> setFuture = redisun.asyncSet(key, "async-value");
        Assert.assertTrue("Async SET should succeed", setFuture.get());

        CompletableFuture<String> getFuture = redisun.asyncGet(key);
        Assert.assertEquals("async-value", getFuture.get());

        CompletableFuture<Integer> appendFuture = redisun.asyncAppend(key, "-suffix");
        Assert.assertEquals(Integer.valueOf(18), appendFuture.get());

        CompletableFuture<Integer> strlenFuture = redisun.asyncStrlen(key);
        Assert.assertEquals(Integer.valueOf(18), strlenFuture.get());

        redisun.set(key, "0");
        CompletableFuture<Long> incrFuture = redisun.asyncIncr(key);
        Assert.assertEquals(Long.valueOf(1), incrFuture.get());

        CompletableFuture<Long> decrFuture = redisun.asyncDecr(key);
        Assert.assertEquals(Long.valueOf(0), decrFuture.get());

        CompletableFuture<Long> incrByFuture = redisun.asyncIncrBy(key, 10);
        Assert.assertEquals(Long.valueOf(10), incrByFuture.get());

        CompletableFuture<Long> decrByFuture = redisun.asyncDecrBy(key, 5);
        Assert.assertEquals(Long.valueOf(5), decrByFuture.get());

        Map<String, String> items = new HashMap<>();
        items.put(key + ":1", "value1");
        items.put(key + ":2", "value2");
        CompletableFuture<Boolean> msetFuture = redisun.asyncMset(items);
        Assert.assertTrue("Async MSET should succeed", msetFuture.get());

        List<String> keys = Arrays.asList(key + ":1", key + ":2");
        CompletableFuture<List<String>> mgetFuture = redisun.asyncMget(keys);
        List<String> values = mgetFuture.get();
        Assert.assertEquals(2, values.size());
        Assert.assertEquals("value1", values.get(0));
        Assert.assertEquals("value2", values.get(1));

        redisun.del(key, key + ":1", key + ":2");
    }

    // ==================== 边界条件测试 ====================

    @Test
    public void testValueEdgeCases() {
        String key = topic + ":edge";

        redisun.set(key, "");
        Assert.assertEquals("", redisun.get(key));

        redisun.set(key, "   ");
        Assert.assertEquals("   ", redisun.get(key));

        redisun.set(key, "!@#$%^&*()_+-=[]{}|;':\",./<>?");
        Assert.assertEquals("!@#$%^&*()_+-=[]{}|;':\",./<>?", redisun.get(key));

        redisun.set(key, "line1\nline2\nline3");
        Assert.assertEquals("line1\nline2\nline3", redisun.get(key));

        redisun.set(key, "中文测试🎉🎊");
        Assert.assertEquals("中文测试🎉🎊", redisun.get(key));

        redisun.del(key);
    }

    @Test
    public void testNumberEdgeCases() {
        String key = topic + ":number-edge";

        redisun.set(key, String.valueOf(Long.MAX_VALUE));
        Assert.assertEquals(String.valueOf(Long.MAX_VALUE), redisun.get(key));

        redisun.set(key, String.valueOf(Long.MIN_VALUE));
        Assert.assertEquals(String.valueOf(Long.MIN_VALUE), redisun.get(key));

        redisun.del(key);
        redisun.set(key, String.valueOf(Long.MAX_VALUE - 1));
        long result = redisun.incr(key);
        Assert.assertEquals(Long.MAX_VALUE, result);

        redisun.set(key, String.valueOf(Long.MIN_VALUE + 1));
        result = redisun.decr(key);
        Assert.assertEquals(Long.MIN_VALUE, result);

        redisun.del(key);
    }
}
