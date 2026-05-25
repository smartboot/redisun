package tech.smartboot.redisun.test;

import org.junit.Assert;
import org.junit.Test;
import tech.smartboot.redisun.Redisun;
import tech.smartboot.redisun.RedisunException;

import java.util.concurrent.CompletableFuture;

/**
 * 通用命令测试类
 * 测试DEL, EXISTS, EXPIRE, TTL, TYPE, DBSIZE, FLUSHDB, FLUSHALL, SELECT等命令
 *
 * @author 三刀
 * @version v1.0 10/21/25
 */
public class GenericCommandTest extends AbstractRedisunTest {

    // ==================== DEL命令测试 ====================

    @Test
    public void testDelCommand() {
        String key1 = topic + ":del1";
        String key2 = topic + ":del2";
        String key3 = topic + ":del3";
        String value = "del-test-value";

        // 删除不存在的键
        Assert.assertEquals(0, redisun.del(key1));

        // 设置测试数据
        redisun.set(key1, value);
        redisun.set(key2, value);
        redisun.set(key3, value);

        // 删除单个键
        Assert.assertEquals(1, redisun.del(key1));
        Assert.assertNull(redisun.get(key1));

        // 删除多个键
        Assert.assertEquals(2, redisun.del(key2, key3));
        Assert.assertNull(redisun.get(key2));
        Assert.assertNull(redisun.get(key3));

        // 删除混合存在和不存在的键
        redisun.set(key1, value);
        String[] keys = {key1, topic + ":nonexistent1", topic + ":nonexistent2"};
        Assert.assertEquals(1, redisun.del(keys));
    }

    @Test(expected = NullPointerException.class)
    public void testDelWithNullKeys() {
        redisun.del((String[]) null);
    }

    // ==================== EXISTS命令测试 ====================

    @Test
    public void testExistsCommand() {
        String key1 = topic + ":exists1";
        String key2 = topic + ":exists2";
        String value = "exists-test-value";

        redisun.del(key1, key2);

        // 键不存在
        Assert.assertEquals(0, redisun.exists(key1));
        Assert.assertEquals(0, redisun.exists(key1, key2));

        // 设置一个键
        redisun.set(key1, value);
        Assert.assertEquals(1, redisun.exists(key1));
        Assert.assertEquals(1, redisun.exists(key1, key2));

        // 设置另一个键
        redisun.set(key2, value);
        Assert.assertEquals(2, redisun.exists(key1, key2));

        redisun.del(key1, key2);
    }

    @Test(expected = NullPointerException.class)
    public void testExistsWithNullKeys() {
        redisun.exists((String[]) null);
    }

    // ==================== EXPIRE命令测试 ====================

    @Test
    public void testExpireCommand() throws InterruptedException {
        String key = topic + ":expire";
        String value = "expire-test-value";

        redisun.set(key, value);

        // 设置过期时间
        Assert.assertEquals(1, redisun.expire(key, 1));

        // 等待过期
        Thread.sleep(1100);
        Assert.assertNull("Key should have expired", redisun.get(key));

        // 对不存在的键设置过期时间
        Assert.assertEquals(0, redisun.expire(topic + ":nonexistent", 1));

        // 测试NX选项
        redisun.set(key, value);
        Assert.assertEquals(1, redisun.expire(key, 1, cmd -> cmd.setIfNotExists()));

        // 测试XX选项
        Assert.assertEquals(1, redisun.expire(key, 1, cmd -> cmd.setIfExists()));

        // 测试NX选项在已有过期时间的键上
        Assert.assertEquals(0, redisun.expire(key, 1, cmd -> cmd.setIfNotExists()));

        redisun.del(key);
    }

    @Test(expected = RedisunException.class)
    public void testExpireWithNullKey() {
        redisun.expire(null, 10);
    }

    // ==================== TTL命令测试 ====================

    @Test
    public void testTtlCommand() throws InterruptedException {
        String key = topic + ":ttl";
        String value = "ttl-test-value";

        // 键不存在
        Assert.assertEquals(-2, redisun.ttl(key));

        // 设置键值
        redisun.set(key, value);

        // 无过期时间
        Assert.assertEquals(-1, redisun.ttl(key));

        // 设置过期时间
        redisun.expire(key, 10);
        long ttl = redisun.ttl(key);
        Assert.assertTrue(ttl > 0 && ttl <= 10);

        // 删除键
        redisun.del(key);
        Assert.assertEquals(-2, redisun.ttl(key));

        redisun.del(key);
    }

    @Test(expected = RedisunException.class)
    public void testTtlWithNullKey() {
        redisun.ttl(null);
    }

    // ==================== TYPE命令测试 ====================

    @Test
    public void testTypeCommand() {
        // 键不存在
        Assert.assertEquals("none", redisun.type(topic + ":nonexistent"));

        // String类型
        String stringKey = topic + ":type:string";
        redisun.set(stringKey, "value");
        Assert.assertEquals("string", redisun.type(stringKey));

        // List类型
        String listKey = topic + ":type:list";
        redisun.lpush(listKey, "value");
        Assert.assertEquals("list", redisun.type(listKey));

        // Set类型
        String setKey = topic + ":type:set";
        redisun.sadd(setKey, "member");
        Assert.assertEquals("set", redisun.type(setKey));

        // Hash类型
        String hashKey = topic + ":type:hash";
        redisun.hset(hashKey, "field", "value");
        Assert.assertEquals("hash", redisun.type(hashKey));

        // ZSet类型
        String zsetKey = topic + ":type:zset";
        redisun.zadd(zsetKey, 1.0, "member");
        Assert.assertEquals("zset", redisun.type(zsetKey));

        redisun.del(stringKey, listKey, setKey, hashKey, zsetKey);
    }

    @Test(expected = RedisunException.class)
    public void testTypeWithNullKey() {
        redisun.type(null);
    }

    // ==================== DBSIZE命令测试 ====================

    @Test
    public void testDBSizeCommand() {
        String key1 = topic + ":dbsize1";
        String key2 = topic + ":dbsize2";
        String value = "dbsize-test-value";

        long initialSize = redisun.dbsize();

        redisun.set(key1, value);
        redisun.set(key2, value);

        long newSize = redisun.dbsize();
        Assert.assertEquals(initialSize + 2, newSize);

        redisun.del(key1);

        long finalSize = redisun.dbsize();
        Assert.assertEquals(newSize - 1, finalSize);

        redisun.del(key2);
    }

    // ==================== FLUSHDB命令测试 ====================

    @Test
    public void testFlushDbCommand() {
        String key1 = topic + ":flushdb1";
        String key2 = topic + ":flushdb2";
        String value = "flushdb-test-value";

        redisun.set(key1, value);
        redisun.set(key2, value);

        Assert.assertEquals(value, redisun.get(key1));
        Assert.assertEquals(value, redisun.get(key2));

        Assert.assertTrue("FLUSHDB command should succeed", redisun.flushDb());

        Assert.assertNull(redisun.get(key1));
        Assert.assertNull(redisun.get(key2));
        Assert.assertEquals(0, redisun.dbsize());

        // 测试FLUSHDB后仍然可以添加键
        String newKey = topic + ":after-flush";
        Assert.assertTrue(redisun.set(newKey, value));
        Assert.assertEquals(value, redisun.get(newKey));

        redisun.del(newKey);
    }

    // ==================== FLUSHALL命令测试 ====================

    @Test
    public void testFlushAllCommand() {
        String key1 = topic + ":flushall1";
        String key2 = topic + ":flushall2";
        String value = "flushall-test-value";

        redisun.set(key1, value);
        redisun.set(key2, value);

        Assert.assertEquals(value, redisun.get(key1));
        Assert.assertEquals(value, redisun.get(key2));

        Assert.assertTrue("FLUSHALL command should succeed", redisun.flushAll());

        Assert.assertNull(redisun.get(key1));
        Assert.assertNull(redisun.get(key2));
        Assert.assertEquals(0, redisun.dbsize());

        // 测试FLUSHALL后仍然可以添加键
        String newKey = topic + ":after-flush";
        Assert.assertTrue(redisun.set(newKey, value));
        Assert.assertEquals(value, redisun.get(newKey));

        redisun.del(newKey);
    }

    // ==================== SELECT命令测试 ====================

    @Test
    public void testSelectCommand() {
        Redisun redisunDb1 = Redisun.create(opt -> opt.debug(true).setAddress("127.0.0.1:6379").setDatabase(1));

        String key = topic + ":auto-select";
        String value = "auto-select-test-value";

        boolean setResult = redisunDb1.set(key, value);
        Assert.assertTrue("Setting key-value in database 1 should succeed", setResult);
        Assert.assertEquals("Value should be set correctly in database 1", value, redisunDb1.get(key));

        // 在默认数据库0中应该获取不到这个键值
        Assert.assertNull("Should not be able to get key from database 0", redisun.get(key));

        redisunDb1.close();
    }

    // ==================== FLUSHDB vs FLUSHALL测试 ====================

    @Test
    public void testFlushDbVsFlushAll() {
        Redisun redisunDb1 = Redisun.create(opt -> opt.debug(true).setAddress("127.0.0.1:6379").setDatabase(1));

        String keyDb0 = topic + ":db0";
        String keyDb1 = topic + ":db1";

        redisun.set(keyDb0, "value0");
        redisunDb1.set(keyDb1, "value1");

        Assert.assertEquals("value0", redisun.get(keyDb0));
        Assert.assertEquals("value1", redisunDb1.get(keyDb1));

        // 执行FLUSHDB（只清空当前数据库）
        redisun.flushDb();

        Assert.assertNull("Key in DB 0 should be deleted", redisun.get(keyDb0));
        Assert.assertEquals("Key in DB 1 should still exist", "value1", redisunDb1.get(keyDb1));

        redisunDb1.del(keyDb1);
        redisunDb1.close();
    }

    // ==================== 异步方法测试 ====================

    @Test
    public void testAsyncGenericCommands() throws Exception {
        String key = topic + ":async";

        redisun.set(key, "value");

        CompletableFuture<Integer> delFuture = redisun.asyncDel(java.util.Arrays.asList(key));
        Assert.assertEquals(Integer.valueOf(1), delFuture.get());

        CompletableFuture<Integer> existsFuture = redisun.asyncExists(key);
        Assert.assertEquals(Integer.valueOf(0), existsFuture.get());

        redisun.set(key, "value");
        redisun.expire(key, 10);

        CompletableFuture<Long> ttlFuture = redisun.asyncTtl(key);
        Assert.assertTrue("TTL should be positive", ttlFuture.get() > 0);

        CompletableFuture<String> typeFuture = redisun.asyncType(key);
        Assert.assertEquals("string", typeFuture.get());

        redisun.del(key);
    }

    // ==================== 边界条件测试 ====================

    @Test
    public void testKeyNameEdgeCases() {
        // 测试特殊字符键名
        String specialKey = topic + ":special!@#$%^&*()";
        redisun.set(specialKey, "value");
        Assert.assertEquals("value", redisun.get(specialKey));
        redisun.del(specialKey);

        // 测试长键名
        StringBuilder longKey = new StringBuilder(topic + ":");
        for (int i = 0; i < 1000; i++) {
            longKey.append("a");
        }
        redisun.set(longKey.toString(), "value");
        Assert.assertEquals("value", redisun.get(longKey.toString()));
        redisun.del(longKey.toString());

        // 测试包含空格的键名
        String spaceKey = topic + ":key with spaces";
        redisun.set(spaceKey, "value");
        Assert.assertEquals("value", redisun.get(spaceKey));
        redisun.del(spaceKey);
    }

    @Test
    public void testBulkOperations() {
        int keyCount = 100;
        String[] keys = new String[keyCount];

        for (int i = 0; i < keyCount; i++) {
            keys[i] = topic + ":bulk:" + i;
            redisun.set(keys[i], "value" + i);
        }

        for (int i = 0; i < keyCount; i++) {
            Assert.assertEquals("value" + i, redisun.get(keys[i]));
        }

        int deleted = redisun.del(keys);
        Assert.assertEquals(keyCount, deleted);

        for (int i = 0; i < keyCount; i++) {
            Assert.assertNull(redisun.get(keys[i]));
        }
    }

    @Test
    public void testExpirePrecision() throws InterruptedException {
        String key = topic + ":expire-precision";

        // 测试1秒过期
        redisun.set(key, "value");
        redisun.expire(key, 1);

        long ttl1 = redisun.ttl(key);
        Assert.assertTrue("TTL should be around 1 second", ttl1 >= 0 && ttl1 <= 1);

        Thread.sleep(1100);
        Assert.assertNull("Key should have expired", redisun.get(key));

        // 测试2秒过期
        redisun.set(key, "value");
        redisun.expire(key, 2);

        Thread.sleep(500);
        long ttl2 = redisun.ttl(key);
        Assert.assertTrue("TTL should be around 1 second after 500ms", ttl2 >= 1 && ttl2 <= 2);

        Thread.sleep(1600);
        Assert.assertNull("Key should have expired after 2 seconds", redisun.get(key));
    }
}
