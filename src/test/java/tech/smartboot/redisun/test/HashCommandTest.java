package tech.smartboot.redisun.test;

import org.junit.Assert;
import org.junit.Test;
import tech.smartboot.redisun.RedisunException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Hash类型命令测试类
 * 测试HSET, HGET, HMSET, HMGET等命令
 *
 * @author 三刀
 * @version v1.0 10/21/25
 */
public class HashCommandTest extends AbstractRedisunTest {

    // ==================== HSET命令测试 ====================

    @Test
    public void testHSetCommand() {
        String key = topic + ":hset";
        String field = "field1";
        String value = "value1";

        redisun.del(key);

        Assert.assertEquals(1, redisun.hset(key, field, value));
        Assert.assertEquals(0, redisun.hset(key, field, "newvalue"));
        Assert.assertEquals(1, redisun.hset(key, "field2", "value2"));

        redisun.del(key);
    }

    @Test(expected = RedisunException.class)
    public void testHSetWithNullKey() {
        redisun.hset(null, "field", "value");
    }

    @Test(expected = RedisunException.class)
    public void testHSetWithNullField() {
        redisun.hset(topic + ":hset", null, "value");
    }

    @Test(expected = RedisunException.class)
    public void testHSetWithNullValue() {
        redisun.hset(topic + ":hset", "field", null);
    }

    // ==================== HGET命令测试 ====================

    @Test
    public void testHGetCommand() {
        String key = topic + ":hget";
        String field = "field1";
        String value = "value1";

        redisun.del(key);

        Assert.assertNull(redisun.hget(key, field));

        redisun.hset(key, field, value);
        Assert.assertEquals(value, redisun.hget(key, field));
        Assert.assertNull(redisun.hget(key, "nonexistent"));

        redisun.del(key);
    }

    @Test(expected = RedisunException.class)
    public void testHGetWithNullKey() {
        redisun.hget(null, "field");
    }

    @Test(expected = RedisunException.class)
    public void testHGetWithNullField() {
        redisun.hget(topic + ":hget", null);
    }

    // ==================== HMSET命令测试 ====================

    @Test
    public void testHmSetCommand() {
        String key = topic + ":hmset";

        redisun.del(key);

        Map<String, String> hash = new HashMap<>();
        hash.put("field1", "value1");
        hash.put("field2", "value2");
        hash.put("field3", "value3");

        boolean result = redisun.hmset(key, hash);
        Assert.assertTrue("HMSET should succeed", result);

        Assert.assertEquals("value1", redisun.hget(key, "field1"));
        Assert.assertEquals("value2", redisun.hget(key, "field2"));
        Assert.assertEquals("value3", redisun.hget(key, "field3"));

        Map<String, String> updateHash = new HashMap<>();
        updateHash.put("field1", "updated_value1");
        updateHash.put("field4", "value4");
        result = redisun.hmset(key, updateHash);
        Assert.assertTrue("HMSET update should succeed", result);

        Assert.assertEquals("updated_value1", redisun.hget(key, "field1"));
        Assert.assertEquals("value4", redisun.hget(key, "field4"));
        Assert.assertEquals("value2", redisun.hget(key, "field2"));
        Assert.assertEquals("value3", redisun.hget(key, "field3"));

        redisun.del(key);
    }

    @Test(expected = RedisunException.class)
    public void testHmSetWithNullKey() {
        redisun.hmset(null, new HashMap<>());
    }

    @Test(expected = NullPointerException.class)
    public void testHmSetWithNullHash() {
        redisun.hmset(topic + ":hmset", null);
    }

    // ==================== HMGET命令测试 ====================

    @Test
    public void testHmGetCommand() {
        String key = topic + ":hmget";

        redisun.del(key);

        redisun.hset(key, "field1", "value1");
        redisun.hset(key, "field2", "value2");
        redisun.hset(key, "field3", "value3");

        List<String> fields = Arrays.asList("field1", "field2", "field3");
        List<String> values = redisun.hmget(key, fields);

        Assert.assertEquals(3, values.size());
        Assert.assertEquals("value1", values.get(0));
        Assert.assertEquals("value2", values.get(1));
        Assert.assertEquals("value3", values.get(2));

        fields = Arrays.asList("field1", "nonexistent", "field3");
        values = redisun.hmget(key, fields);

        Assert.assertEquals(3, values.size());
        Assert.assertEquals("value1", values.get(0));
        Assert.assertNull(values.get(1));
        Assert.assertEquals("value3", values.get(2));

        List<String> varargsValues = redisun.hmget(key, "field1", "field2");
        Assert.assertEquals(2, varargsValues.size());
        Assert.assertEquals("value1", varargsValues.get(0));
        Assert.assertEquals("value2", varargsValues.get(1));

        List<String> nonExistentValues = redisun.hmget("nonexistent_key", "field1", "field2");
        Assert.assertEquals(2, nonExistentValues.size());
        Assert.assertNull(nonExistentValues.get(0));
        Assert.assertNull(nonExistentValues.get(1));

        redisun.del(key);
    }

    @Test(expected = NullPointerException.class)
    public void testHmGetWithNullKey() {
        redisun.hmget(topic + ":hmget", (List<String>) null);
    }

    // ==================== 异步方法测试 ====================

    @Test
    public void testAsyncHashCommands() throws Exception {
        String key = topic + ":async";

        CompletableFuture<Integer> hsetFuture = redisun.asyncHset(key + ":hash", "field", "value");
        Assert.assertEquals(Integer.valueOf(1), hsetFuture.get());

        CompletableFuture<String> hgetFuture = redisun.asyncHget(key + ":hash", "field");
        Assert.assertEquals("value", hgetFuture.get());

        Map<String, String> hash = new HashMap<>();
        hash.put("f1", "v1");
        hash.put("f2", "v2");
        CompletableFuture<Boolean> hmsetFuture = redisun.asyncHmset(key + ":hash2", hash);
        Assert.assertTrue("Async HMSET should succeed", hmsetFuture.get());

        CompletableFuture<List<String>> hmgetFuture = redisun.asyncHmget(key + ":hash2", "f1", "f2");
        List<String> hvalues = hmgetFuture.get();
        Assert.assertEquals(2, hvalues.size());
        Assert.assertEquals("v1", hvalues.get(0));
        Assert.assertEquals("v2", hvalues.get(1));

        redisun.del(key + ":hash", key + ":hash2");
    }

    // ==================== 边界条件测试 ====================

    @Test
    public void testHashEdgeCases() {
        String key = topic + ":hash-edge";

        redisun.del(key);

        // 测试空字段名
        redisun.hset(key, "", "empty-field-value");
        Assert.assertEquals("empty-field-value", redisun.hget(key, ""));

        // 测试空值
        redisun.hset(key, "empty-value-field", "");
        Assert.assertEquals("", redisun.hget(key, "empty-value-field"));

        // 测试包含特殊字符的字段
        redisun.hset(key, "field!@#$%", "special");
        Assert.assertEquals("special", redisun.hget(key, "field!@#$%"));

        // 测试包含Unicode的字段和值
        redisun.hset(key, "中文字段", "中文值🎉");
        Assert.assertEquals("中文值🎉", redisun.hget(key, "中文字段"));

        // 测试大量字段
        for (int i = 0; i < 100; i++) {
            redisun.hset(key, "field" + i, "value" + i);
        }
        for (int i = 0; i < 100; i++) {
            Assert.assertEquals("value" + i, redisun.hget(key, "field" + i));
        }

        redisun.del(key);
    }
}
