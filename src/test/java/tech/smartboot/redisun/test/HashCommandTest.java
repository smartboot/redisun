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
    public void testHSetNewField() {
        String key = topic + ":hset-new";
        redisun.del(key);

        // 设置新字段应该返回1
        Assert.assertEquals(1, redisun.hset(key, "field1", "value1"));
        Assert.assertEquals("value1", redisun.hget(key, "field1"));

        redisun.del(key);
    }

    @Test
    public void testHSetExistingField() {
        String key = topic + ":hset-existing";
        redisun.del(key);

        redisun.hset(key, "field1", "original");
        // 更新已存在字段应该返回0
        Assert.assertEquals(0, redisun.hset(key, "field1", "updated"));
        Assert.assertEquals("updated", redisun.hget(key, "field1"));

        redisun.del(key);
    }

    @Test
    public void testHSetMultipleFields() {
        String key = topic + ":hset-multi";
        redisun.del(key);

        redisun.hset(key, "field1", "value1");
        redisun.hset(key, "field2", "value2");
        redisun.hset(key, "field3", "value3");

        Assert.assertEquals("value1", redisun.hget(key, "field1"));
        Assert.assertEquals("value2", redisun.hget(key, "field2"));
        Assert.assertEquals("value3", redisun.hget(key, "field3"));

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

    @Test
    public void testHSetWrongType() {
        String key = topic + ":hset-wrong-type";
        redisun.set(key, "string-value");

        try {
            redisun.hset(key, "field", "value");
            Assert.fail("HSET on string type should throw exception");
        } catch (RedisunException e) {
            // Expected - WRONGTYPE error
        }

        redisun.del(key);
    }

    // ==================== HGET命令测试 ====================

    @Test
    public void testHGetExistingField() {
        String key = topic + ":hget-existing";
        redisun.del(key);

        redisun.hset(key, "field1", "value1");
        Assert.assertEquals("value1", redisun.hget(key, "field1"));

        redisun.del(key);
    }

    @Test
    public void testHGetNonExistentField() {
        String key = topic + ":hget-non-field";
        redisun.del(key);

        redisun.hset(key, "field1", "value1");
        Assert.assertNull(redisun.hget(key, "nonexistent"));

        redisun.del(key);
    }

    @Test
    public void testHGetNonExistentKey() {
        String key = topic + ":hget-non-key";
        redisun.del(key);

        Assert.assertNull(redisun.hget(key, "field"));
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
    public void testHmSetMultiple() {
        String key = topic + ":hmset-multi";
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

        redisun.del(key);
    }

    @Test
    public void testHmSetUpdate() {
        String key = topic + ":hmset-update";
        redisun.del(key);

        Map<String, String> hash = new HashMap<>();
        hash.put("field1", "value1");
        hash.put("field2", "value2");
        redisun.hmset(key, hash);

        Map<String, String> updateHash = new HashMap<>();
        updateHash.put("field1", "updated1");
        updateHash.put("field3", "value3");
        boolean result = redisun.hmset(key, updateHash);
        Assert.assertTrue("HMSET update should succeed", result);

        Assert.assertEquals("updated1", redisun.hget(key, "field1"));
        Assert.assertEquals("value2", redisun.hget(key, "field2"));
        Assert.assertEquals("value3", redisun.hget(key, "field3"));

        redisun.del(key);
    }

    @Test
    public void testHmSetEmpty() {
        String key = topic + ":hmset-empty";
        redisun.del(key);

        Map<String, String> hash = new HashMap<>();
        try {
            redisun.hmset(key, hash);
            Assert.fail("HMSET with empty map should throw exception");
        } catch (RedisunException e) {
            Assert.assertEquals("ERR wrong number of arguments for 'hmset' command", e.getMessage());    // Expected - empty map
        }
        redisun.del(key);
    }

    @Test(expected = RedisunException.class)
    public void testHmSetWithNullKey() {
        redisun.hmset(null, new HashMap<>());
    }

    @Test(expected = NullPointerException.class)
    public void testHmSetWithNullMap() {
        redisun.hmset(topic + ":hmset", null);
    }

    @Test
    public void testHmSetWrongType() {
        String key = topic + ":hmset-wrong-type";
        redisun.set(key, "string-value");

        Map<String, String> hash = new HashMap<>();
        hash.put("field", "value");

        try {
            redisun.hmset(key, hash);
            Assert.fail("HMSET on string type should throw exception");
        } catch (RedisunException e) {
            // Expected - WRONGTYPE error
        }

        redisun.del(key);
    }

    // ==================== HMGET命令测试 ====================

    @Test
    public void testHmGetMultiple() {
        String key = topic + ":hmget-multi";
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

        redisun.del(key);
    }

    @Test
    public void testHmGetMixed() {
        String key = topic + ":hmget-mixed";
        redisun.del(key);

        redisun.hset(key, "field1", "value1");
        redisun.hset(key, "field3", "value3");

        List<String> fields = Arrays.asList("field1", "field2", "field3");
        List<String> values = redisun.hmget(key, fields);

        Assert.assertEquals(3, values.size());
        Assert.assertEquals("value1", values.get(0));
        Assert.assertNull(values.get(1));
        Assert.assertEquals("value3", values.get(2));

        redisun.del(key);
    }

    @Test
    public void testHmGetNonExistentKey() {
        String key = topic + ":hmget-non-key";
        redisun.del(key);

        List<String> fields = Arrays.asList("field1", "field2");
        List<String> values = redisun.hmget(key, fields);

        Assert.assertEquals(2, values.size());
        Assert.assertNull(values.get(0));
        Assert.assertNull(values.get(1));
    }

    @Test
    public void testHmGetVarargs() {
        String key = topic + ":hmget-varargs";
        redisun.del(key);

        redisun.hset(key, "field1", "value1");
        redisun.hset(key, "field2", "value2");

        List<String> values = redisun.hmget(key, "field1", "field2");
        Assert.assertEquals(2, values.size());
        Assert.assertEquals("value1", values.get(0));
        Assert.assertEquals("value2", values.get(1));

        redisun.del(key);
    }

    @Test(expected = RedisunException.class)
    public void testHmGetWithNullKey() {
        redisun.hmget(null, Arrays.asList("field"));
    }

    @Test(expected = NullPointerException.class)
    public void testHmGetWithNullFields() {
        redisun.hmget(topic + ":hmget", (List<String>) null);
    }

    // ==================== 异步方法测试 ====================

    @Test
    public void testAsyncHSet() throws Exception {
        String key = topic + ":async-hset";
        redisun.del(key);

        CompletableFuture<Integer> hsetFuture = redisun.asyncHset(key, "field", "value");
        Assert.assertEquals(Integer.valueOf(1), hsetFuture.get());

        CompletableFuture<String> hgetFuture = redisun.asyncHget(key, "field");
        Assert.assertEquals("value", hgetFuture.get());

        redisun.del(key);
    }

    @Test
    public void testAsyncHGet() throws Exception {
        String key = topic + ":async-hget";
        redisun.del(key);

        redisun.hset(key, "field", "value");

        CompletableFuture<String> hgetFuture = redisun.asyncHget(key, "field");
        Assert.assertEquals("value", hgetFuture.get());

        CompletableFuture<String> nonExistentFuture = redisun.asyncHget(key, "nonexistent");
        Assert.assertNull(nonExistentFuture.get());

        redisun.del(key);
    }

    @Test
    public void testAsyncHmSet() throws Exception {
        String key = topic + ":async-hmset";
        redisun.del(key);

        Map<String, String> hash = new HashMap<>();
        hash.put("f1", "v1");
        hash.put("f2", "v2");

        CompletableFuture<Boolean> hmsetFuture = redisun.asyncHmset(key, hash);
        Assert.assertTrue("Async HMSET should succeed", hmsetFuture.get());

        redisun.del(key);
    }

    @Test
    public void testAsyncHmGet() throws Exception {
        String key = topic + ":async-hmget";
        redisun.del(key);

        redisun.hset(key, "f1", "v1");
        redisun.hset(key, "f2", "v2");

        CompletableFuture<List<String>> hmgetFuture = redisun.asyncHmget(key, "f1", "f2");
        List<String> values = hmgetFuture.get();
        Assert.assertEquals(2, values.size());
        Assert.assertEquals("v1", values.get(0));
        Assert.assertEquals("v2", values.get(1));

        redisun.del(key);
    }

    // ==================== 边界条件测试 ====================

    @Test
    public void testHSetEmptyField() {
        String key = topic + ":hset-empty-field";
        redisun.del(key);

        redisun.hset(key, "", "empty-field-value");
        Assert.assertEquals("empty-field-value", redisun.hget(key, ""));

        redisun.del(key);
    }

    @Test
    public void testHSetEmptyValue() {
        String key = topic + ":hset-empty-value";
        redisun.del(key);

        redisun.hset(key, "field", "");
        Assert.assertEquals("", redisun.hget(key, "field"));

        redisun.del(key);
    }

    @Test
    public void testHSetSpecialChars() {
        String key = topic + ":hset-special";
        redisun.del(key);

        redisun.hset(key, "field!@#$%", "special");
        Assert.assertEquals("special", redisun.hget(key, "field!@#$%"));

        redisun.hset(key, "normal", "value!@#$%^&*()");
        Assert.assertEquals("value!@#$%^&*()", redisun.hget(key, "normal"));

        redisun.del(key);
    }

    @Test
    public void testHSetUnicode() {
        String key = topic + ":hset-unicode";
        redisun.del(key);

        redisun.hset(key, "中文字段", "中文值🎉");
        Assert.assertEquals("中文值🎉", redisun.hget(key, "中文字段"));

        redisun.del(key);
    }

    @Test
    public void testHSetManyFields() {
        String key = topic + ":hset-many";
        redisun.del(key);

        // 测试大量字段
        for (int i = 0; i < 100; i++) {
            redisun.hset(key, "field" + i, "value" + i);
        }
        for (int i = 0; i < 100; i++) {
            Assert.assertEquals("value" + i, redisun.hget(key, "field" + i));
        }

        redisun.del(key);
    }

    @Test
    public void testHashOverwrite() {
        String key = topic + ":hash-overwrite";
        redisun.del(key);

        // 先作为hash设置
        redisun.hset(key, "field", "hash-value");
        Assert.assertEquals("hash-value", redisun.hget(key, "field"));

        // 覆盖为string
        redisun.set(key, "string-value");
        Assert.assertEquals("string-value", redisun.get(key));

        // 再次作为hash设置
        try{
            redisun.hset(key, "field", "new-hash-value");
            Assert.fail("Expected RedisunException to be thrown");
        }catch (RedisunException e){
            Assert.assertEquals("WRONGTYPE Operation against a key holding the wrong kind of value", e.getMessage());
        }
        redisun.del(key);
    }

    @Test
    public void testHashType() {
        String key = topic + ":hash-type";
        redisun.del(key);

        redisun.hset(key, "field", "value");
        Assert.assertEquals("hash", redisun.type(key));

        redisun.del(key);
    }
}
