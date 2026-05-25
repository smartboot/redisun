package tech.smartboot.redisun.test;

import org.junit.Assert;
import org.junit.Test;
import tech.smartboot.redisun.Redisun;
import tech.smartboot.redisun.RedisunException;

import java.util.Arrays;
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
    public void testDelExisting() {
        String key = topic + ":del-existing";
        redisun.set(key, "value");

        // 删除存在的键返回1
        Assert.assertEquals(1, redisun.del(key));
        Assert.assertNull(redisun.get(key));
    }

    @Test
    public void testDelNonExistent() {
        String key = topic + ":del-non";
        redisun.del(key);

        // 删除不存在的键返回0
        Assert.assertEquals(0, redisun.del(key));
    }

    @Test
    public void testDelMultiple() {
        String key1 = topic + ":del1";
        String key2 = topic + ":del2";
        String key3 = topic + ":del3";

        redisun.set(key1, "value1");
        redisun.set(key2, "value2");
        redisun.set(key3, "value3");

        // 删除多个键
        Assert.assertEquals(3, redisun.del(key1, key2, key3));
        Assert.assertNull(redisun.get(key1));
        Assert.assertNull(redisun.get(key2));
        Assert.assertNull(redisun.get(key3));
    }

    @Test
    public void testDelMixed() {
        String key1 = topic + ":del-mixed1";
        String key2 = topic + ":del-mixed2";

        redisun.set(key1, "value1");
        redisun.del(key2);

        // 混合存在和不存在的键
        Assert.assertEquals(1, redisun.del(key1, key2));

        redisun.del(key1);
    }

    @Test(expected = NullPointerException.class)
    public void testDelWithNullKeys() {
        redisun.del((String[]) null);
    }

    @Test
    public void testDelList() {
        String key = topic + ":del-list";
        redisun.del(key);

        redisun.lpush(key, "value1", "value2");
        Assert.assertEquals(1, redisun.del(key));
        Assert.assertNull(redisun.lpop(key));
    }

    @Test
    public void testDelHash() {
        String key = topic + ":del-hash";
        redisun.del(key);

        redisun.hset(key, "field", "value");
        Assert.assertEquals(1, redisun.del(key));
        Assert.assertNull(redisun.hget(key, "field"));
    }

    // ==================== EXISTS命令测试 ====================

    @Test
    public void testExistsExisting() {
        String key = topic + ":exists-existing";
        redisun.set(key, "value");

        Assert.assertEquals(1, redisun.exists(key));

        redisun.del(key);
    }

    @Test
    public void testExistsNonExistent() {
        String key = topic + ":exists-non";
        redisun.del(key);

        Assert.assertEquals(0, redisun.exists(key));
    }

    @Test
    public void testExistsMultiple() {
        String key1 = topic + ":exists1";
        String key2 = topic + ":exists2";
        String key3 = topic + ":exists3";

        redisun.set(key1, "value1");
        redisun.set(key2, "value2");
        redisun.del(key3);

        Assert.assertEquals(2, redisun.exists(key1, key2, key3));

        redisun.del(key1, key2);
    }

    @Test(expected = NullPointerException.class)
    public void testExistsWithNullKeys() {
        redisun.exists((String[]) null);
    }

    // ==================== EXPIRE命令测试 ====================

    @Test
    public void testExpireExisting() throws InterruptedException {
        String key = topic + ":expire-existing";
        redisun.set(key, "value");

        // 对存在的键设置过期返回1
        Assert.assertEquals(1, redisun.expire(key, 1));

        // 验证键还存在
        Assert.assertEquals("value", redisun.get(key));

        // 等待过期
        Thread.sleep(1100);
        Assert.assertNull(redisun.get(key));
    }

    @Test
    public void testExpireNonExistent() {
        String key = topic + ":expire-non";
        redisun.del(key);

        // 对不存在的键设置过期返回0
        Assert.assertEquals(0, redisun.expire(key, 1));
    }

    @Test
    public void testExpireWithNX() {
        String key = topic + ":expire-nx";
        redisun.set(key, "value");

        // 先设置过期时间
        redisun.expire(key, 10);

        // NX选项: 仅当没有过期时间时设置，应该失败
        Assert.assertEquals(0, redisun.expire(key, 1, cmd -> cmd.setIfNotExists()));

        redisun.del(key);
    }

    @Test
    public void testExpireWithXX() {
        String key = topic + ":expire-xx";
        redisun.set(key, "value");

        // XX选项: 仅当已有过期时间时设置，应该失败（因为没有过期时间）
        Assert.assertEquals(0, redisun.expire(key, 1, cmd -> cmd.setIfExists()));

        // 先设置过期时间
        redisun.expire(key, 10);

        // 现在应该成功
        Assert.assertEquals(1, redisun.expire(key, 1, cmd -> cmd.setIfExists()));

        redisun.del(key);
    }

    @Test(expected = RedisunException.class)
    public void testExpireWithNullKey() {
        redisun.expire(null, 10);
    }

    @Test
    public void testExpireNegative() {
        String key = topic + ":expire-neg";
        redisun.set(key, "value");

        // 负值过期时间应该删除键
        Assert.assertEquals(1, redisun.expire(key, -1));
        Assert.assertNull(redisun.get(key));
    }

    // ==================== TTL命令测试 ====================

    @Test
    public void testTtlWithExpire() throws InterruptedException {
        String key = topic + ":ttl-expire";
        redisun.set(key, "value");
        redisun.expire(key, 10);

        long ttl = redisun.ttl(key);
        Assert.assertTrue("TTL should be positive", ttl > 0 && ttl <= 10);

        redisun.del(key);
    }

    @Test
    public void testTtlWithoutExpire() {
        String key = topic + ":ttl-no-expire";
        redisun.set(key, "value");

        // 无过期时间返回-1
        Assert.assertEquals(-1, redisun.ttl(key));

        redisun.del(key);
    }

    @Test
    public void testTtlNonExistent() {
        String key = topic + ":ttl-non";
        redisun.del(key);

        // 不存在的键返回-2
        Assert.assertEquals(-2, redisun.ttl(key));
    }

    @Test(expected = RedisunException.class)
    public void testTtlWithNullKey() {
        redisun.ttl(null);
    }

    @Test
    public void testTtlDecreasing() throws InterruptedException {
        String key = topic + ":ttl-decrease";
        redisun.set(key, "value");
        redisun.expire(key, 5);

        long ttl1 = redisun.ttl(key);
        Thread.sleep(1000);
        long ttl2 = redisun.ttl(key);

        Assert.assertTrue("TTL should decrease", ttl2 < ttl1);

        redisun.del(key);
    }

    // ==================== TYPE命令测试 ====================

    @Test
    public void testTypeString() {
        String key = topic + ":type-string";
        redisun.set(key, "value");

        Assert.assertEquals("string", redisun.type(key));

        redisun.del(key);
    }

    @Test
    public void testTypeList() {
        String key = topic + ":type-list";
        redisun.lpush(key, "value");

        Assert.assertEquals("list", redisun.type(key));

        redisun.del(key);
    }

    @Test
    public void testTypeSet() {
        String key = topic + ":type-set";
        redisun.sadd(key, "member");

        Assert.assertEquals("set", redisun.type(key));

        redisun.del(key);
    }

    @Test
    public void testTypeHash() {
        String key = topic + ":type-hash";
        redisun.hset(key, "field", "value");

        Assert.assertEquals("hash", redisun.type(key));

        redisun.del(key);
    }

    @Test
    public void testTypeZSet() {
        String key = topic + ":type-zset";
        redisun.zadd(key, 1.0, "member");

        Assert.assertEquals("zset", redisun.type(key));

        redisun.del(key);
    }

    @Test
    public void testTypeNone() {
        String key = topic + ":type-none";
        redisun.del(key);

        Assert.assertEquals("none", redisun.type(key));
    }

    @Test(expected = RedisunException.class)
    public void testTypeWithNullKey() {
        redisun.type(null);
    }

    // ==================== DBSIZE命令测试 ====================

    @Test
    public void testDBSize() {
        String key = topic + ":dbsize";
        redisun.del(key);

        long initialSize = redisun.dbsize();

        redisun.set(key, "value");
        long newSize = redisun.dbsize();

        Assert.assertEquals(initialSize + 1, newSize);

        redisun.del(key);
    }

    @Test
    public void testDBSizeAfterOperations() {
        String key1 = topic + ":dbsize1";
        String key2 = topic + ":dbsize2";

        redisun.del(key1, key2);

        long size1 = redisun.dbsize();

        redisun.set(key1, "value1");
        long size2 = redisun.dbsize();
        Assert.assertEquals(size1 + 1, size2);

        redisun.set(key2, "value2");
        long size3 = redisun.dbsize();
        Assert.assertEquals(size2 + 1, size3);

        redisun.del(key1);
        long size4 = redisun.dbsize();
        Assert.assertEquals(size3 - 1, size4);

        redisun.del(key2);
    }

    // ==================== FLUSHDB命令测试 ====================

    @Test
    public void testFlushDb() {
        String key1 = topic + ":flushdb1";
        String key2 = topic + ":flushdb2";

        redisun.set(key1, "value1");
        redisun.set(key2, "value2");

        Assert.assertTrue(redisun.flushDb());

        Assert.assertNull(redisun.get(key1));
        Assert.assertNull(redisun.get(key2));
        Assert.assertEquals(0, redisun.dbsize());
    }

    @Test
    public void testFlushDbAfterOperations() {
        String key = topic + ":flushdb-after";

        redisun.set(key, "value");
        Assert.assertTrue(redisun.flushDb());

        // 测试FLUSHDB后仍然可以添加键
        Assert.assertTrue(redisun.set(key, "new-value"));
        Assert.assertEquals("new-value", redisun.get(key));

        redisun.del(key);
    }

    // ==================== FLUSHALL命令测试 ====================

    @Test
    public void testFlushAll() {
        String key1 = topic + ":flushall1";
        String key2 = topic + ":flushall2";

        redisun.set(key1, "value1");
        redisun.set(key2, "value2");

        Assert.assertTrue(redisun.flushAll());

        Assert.assertNull(redisun.get(key1));
        Assert.assertNull(redisun.get(key2));
        Assert.assertEquals(0, redisun.dbsize());
    }

    @Test
    public void testFlushAllAfterOperations() {
        String key = topic + ":flushall-after";

        redisun.set(key, "value");
        Assert.assertTrue(redisun.flushAll());

        // 测试FLUSHALL后仍然可以添加键
        Assert.assertTrue(redisun.set(key, "new-value"));
        Assert.assertEquals("new-value", redisun.get(key));

        redisun.del(key);
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

    // ==================== SELECT命令测试 ====================

    @Test
    public void testSelectDatabase() {
        Redisun redisunDb1 = Redisun.create(opt -> opt.debug(true).setAddress("127.0.0.1:6379").setDatabase(1));

        String key = topic + ":select-test";
        String value = "select-test-value";

        redisunDb1.set(key, value);
        Assert.assertEquals(value, redisunDb1.get(key));

        // 在默认数据库0中应该获取不到这个键值
        Assert.assertNull("Should not be able to get key from database 0", redisun.get(key));

        redisunDb1.del(key);
        redisunDb1.close();
    }

    @Test
    public void testSelectIsolation() {
        Redisun redisunDb1 = Redisun.create(opt -> opt.debug(true).setAddress("127.0.0.1:6379").setDatabase(1));
        Redisun redisunDb2 = Redisun.create(opt -> opt.debug(true).setAddress("127.0.0.1:6379").setDatabase(2));

        String key = topic + ":isolation";

        redisunDb1.set(key, "db1-value");
        redisunDb2.set(key, "db2-value");

        Assert.assertEquals("db1-value", redisunDb1.get(key));
        Assert.assertEquals("db2-value", redisunDb2.get(key));
        Assert.assertNull(redisun.get(key));

        redisunDb1.del(key);
        redisunDb2.del(key);
        redisunDb1.close();
        redisunDb2.close();
    }

    // ==================== 异步方法测试 ====================

    @Test
    public void testAsyncDel() throws Exception {
        String key = topic + ":async-del";
        redisun.set(key, "value");

        CompletableFuture<Integer> future = redisun.asyncDel(Arrays.asList(key));
        Assert.assertEquals(Integer.valueOf(1), future.get());

        Assert.assertNull(redisun.get(key));
    }

    @Test
    public void testAsyncExists() throws Exception {
        String key = topic + ":async-exists";
        redisun.set(key, "value");

        CompletableFuture<Integer> future = redisun.asyncExists(key);
        Assert.assertEquals(Integer.valueOf(1), future.get());

        redisun.del(key);

        future = redisun.asyncExists(key);
        Assert.assertEquals(Integer.valueOf(0), future.get());
    }

    @Test
    public void testAsyncExpire() throws Exception {
        String key = topic + ":async-expire";
        redisun.set(key, "value");

        // Note: asyncExpire is private, test via expire
        // This is a placeholder for the concept

        redisun.del(key);
    }

    @Test
    public void testAsyncTtl() throws Exception {
        String key = topic + ":async-ttl";
        redisun.set(key, "value");
        redisun.expire(key, 10);

        CompletableFuture<Long> future = redisun.asyncTtl(key);
        Long ttl = future.get();
        Assert.assertTrue("TTL should be positive", ttl > 0);

        redisun.del(key);
    }

    @Test
    public void testAsyncType() throws Exception {
        String key = topic + ":async-type";
        redisun.set(key, "value");

        CompletableFuture<String> future = redisun.asyncType(key);
        Assert.assertEquals("string", future.get());

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

    @Test
    public void testTypeAfterExpire() throws InterruptedException {
        String key = topic + ":type-expire";
        redisun.set(key, "value");
        redisun.expire(key, 1);

        Assert.assertEquals("string", redisun.type(key));

        Thread.sleep(1100);
        Assert.assertEquals("none", redisun.type(key));
    }

    @Test
    public void testExistsAfterExpire() throws InterruptedException {
        String key = topic + ":exists-expire";
        redisun.set(key, "value");
        redisun.expire(key, 1);

        Assert.assertEquals(1, redisun.exists(key));

        Thread.sleep(1100);
        Assert.assertEquals(0, redisun.exists(key));
    }
}
