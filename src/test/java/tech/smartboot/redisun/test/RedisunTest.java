package tech.smartboot.redisun.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.redisun.Redisun;
import tech.smartboot.redisun.RedisunException;
import tech.smartboot.redisun.cmd.ZRangeCommand;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author 三刀
 * @version v1.0 10/21/25
 */
public class RedisunTest {
    private Redisun redisun;
    //清理环境
    private String topic;

    @Before
    public void init() {
        redisun = Redisun.create(opt -> opt.debug(true).setAddress("127.0.0.1:6379"));
        topic = "test-" + UUID.randomUUID();
    }

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

    /**
     * 测试GET命令的各种场景
     */
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

    /**
     * 测试DEL命令的各种场景
     */
    @Test
    public void testDelCommand() {
        String key1 = topic + ":del1";
        String key2 = topic + ":del2";
        String key3 = topic + ":del3";
        String value = "del-test-value";

        // Test deleting non-existent key
        int deletedCount = redisun.del(key1);
        Assert.assertEquals("Deleting non-existent key should return 0", 0, deletedCount);

        // Set up test data
        redisun.set(key1, value);
        redisun.set(key2, value);
        redisun.set(key3, value);

        // Test deleting single key
        deletedCount = redisun.del(key1);
        Assert.assertEquals("Deleting single key should return 1", 1, deletedCount);
        Assert.assertNull("Deleted key should no longer exist", redisun.get(key1));

        // Test deleting multiple keys using array
        deletedCount = redisun.del(key2, key3);
        Assert.assertEquals("Deleting multiple keys should return correct count", 2, deletedCount);
        Assert.assertNull("Deleted key2 should no longer exist", redisun.get(key2));
        Assert.assertNull("Deleted key3 should no longer exist", redisun.get(key3));

        // Test deleting mix of existing and non-existing keys
        redisun.set(key1, value);
        String[] keys = {key1, topic + ":nonexistent1", topic + ":nonexistent2"};

        // Test EXPIRE command
        String expireKey = topic + ":expire";
        redisun.set(expireKey, value);
        int expireResult = redisun.expire(expireKey, 1);
        Assert.assertEquals("Setting expiration should return 1", 1, expireResult);

        // Wait for expiration
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Assert.assertNull("Key should have expired", redisun.get(expireKey));

        // Test EXPIRE on non-existent key
        expireResult = redisun.expire(topic + ":nonexistent", 1);
        Assert.assertEquals("Setting expiration on non-existent key should return 0", 0, expireResult);

        // Test EXPIRE with NX option
        redisun.set(expireKey, value);
        expireResult = redisun.expire(expireKey, 1, cmd -> cmd.setIfNotExists());
        Assert.assertEquals("Setting expiration with NX option should return 1", 1, expireResult);

        // Test EXPIRE with XX option
        expireResult = redisun.expire(expireKey, 1, cmd -> cmd.setIfExists());
        Assert.assertEquals("Setting expiration with XX option should return 1", 1, expireResult);

        // Test EXPIRE with NX option on key with existing expiration
        expireResult = redisun.expire(expireKey, 1, cmd -> cmd.setIfNotExists());
        Assert.assertEquals("Setting expiration with NX option on key with existing expiration should return 0", 0, expireResult);
        deletedCount = redisun.del(keys);
        Assert.assertEquals("Deleting mix of existing and non-existing keys should return correct count", 1, deletedCount);
    }

    /**
     * 测试DBSIZE命令的各种场景
     */
    @Test
    public void testDBSizeCommand() {
        String key1 = topic + ":dbsize1";
        String key2 = topic + ":dbsize2";
        String value = "dbsize-test-value";

        // Get initial database size
        long initialSize = redisun.dbsize();

        // Add some keys
        redisun.set(key1, value);
        redisun.set(key2, value);

        // Check that database size increased
        long newSize = redisun.dbsize();
        Assert.assertEquals("Database size should increase by 2", initialSize + 2, newSize);

        // Delete one key
        redisun.del(key1);

        // Check that database size decreased
        long finalSize = redisun.dbsize();
        Assert.assertEquals("Database size should decrease by 1", newSize - 1, finalSize);

        // Delete all remaining test keys
        redisun.del(key2);

        // Check that database size is back to initial
        long lastSize = redisun.dbsize();
        Assert.assertEquals("Database size should be back to initial", initialSize, lastSize);
    }

    /**
     * 测试FLUSHALL命令的各种场景
     */
    @Test
    public void testFlushAllCommand() {
        String key1 = topic + ":flushall1";
        String key2 = topic + ":flushall2";
        String value = "flushall-test-value";

        // Add some keys
        redisun.set(key1, value);
        redisun.set(key2, value);

        // Verify keys exist
        Assert.assertEquals("Key1 should exist", value, redisun.get(key1));
        Assert.assertEquals("Key2 should exist", value, redisun.get(key2));

        // Flush all keys
        boolean flushed = redisun.flushAll();
        Assert.assertTrue("FLUSHALL command should succeed", flushed);

        // Verify keys no longer exist
        Assert.assertNull("Key1 should no longer exist", redisun.get(key1));
        Assert.assertNull("Key2 should no longer exist", redisun.get(key2));

        // Verify database is empty
        long dbSize = redisun.dbsize();
        Assert.assertEquals("Database should be empty", 0, dbSize);

        // Test that we can still add keys after flushAll
        String newKey = topic + ":after-flush";
        boolean setResult = redisun.set(newKey, value);
        Assert.assertTrue("Should be able to set key after FLUSHALL", setResult);
        Assert.assertEquals("New key should exist", value, redisun.get(newKey));

        // 清理测试数据
        redisun.del(newKey);
    }

    /**
     * 测试ZADD命令的各种场景
     */
    @Test
    public void testZAddCommand() {
        String key = topic + ":zadd";

        // Test adding a single member
        int result = redisun.zadd(key, 1.0, "member1");
        Assert.assertEquals("Adding a new member should return 1", 1, result);

        // Test adding another member
        result = redisun.zadd(key, 2.5, "member2");
        Assert.assertEquals("Adding another new member should return 1", 1, result);

        // Test adding existing member with new score (should return 0 as it's an update)
        result = redisun.zadd(key, 3.0, "member1");
        Assert.assertEquals("Updating existing member should return 0", 0, result);

        // Test adding member with negative score
        result = redisun.zadd(key, -1.5, "member3");
        Assert.assertEquals("Adding member with negative score should return 1", 1, result);

        // Test adding member with zero score
        result = redisun.zadd(key, 0.0, "member4");
        Assert.assertEquals("Adding member with zero score should return 1", 1, result);

        // 清理测试数据
        redisun.del(key);
    }

    @Test
    public void testZRangeCommand() {
        String key = topic + ":zrange";

        // 先删除可能存在的键
        redisun.del(key);

        // 添加测试数据
        redisun.zadd(key, 1.0, "one");
        redisun.zadd(key, 2.0, "two");
        redisun.zadd(key, 3.0, "three");

        // 测试基本的 ZRANGE 命令
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

        // 测试 REV 选项
        List<ZRangeCommand.Tuple> result1 = redisun.zrange(key, 0, -1, cmd -> cmd.rev());
        Assert.assertEquals(3, result1.size());
        Assert.assertEquals("three", result1.get(0).getMember());
        Assert.assertEquals("two", result1.get(1).getMember());
        Assert.assertEquals("one", result1.get(2).getMember());

        // 测试 WITHSCORES 选项
        // 注意：WITHSCORES 选项在同步方法中无法直接测试，因为它会改变返回值类型
        // 我们会在异步方法中测试这个功能

        // 测试 LIMIT 选项
        result1 = redisun.zrange(key, 0, -1, cmd -> cmd.limit(1, 2));
        Assert.assertEquals(2, result1.size());
        Assert.assertEquals("two", result1.get(0));
        Assert.assertEquals("three", result1.get(1));

        // 清理测试数据
        redisun.del(key);
    }

    @Test
    public void testZRangeByScore() {
        String key = topic + ":zrangebyscore";

        // 先删除可能存在的键
        redisun.del(key);

        // 添加测试数据
        redisun.zadd(key, 1.0, "one");
        redisun.zadd(key, 2.0, "two");
        redisun.zadd(key, 3.0, "three");
        redisun.zadd(key, 4.0, "four");
        redisun.zadd(key, 5.0, "five");

        // 测试 BYSCORE 选项
        List<ZRangeCommand.Tuple> result = redisun.zrange(key, 2, 4, cmd -> cmd.sortByScore());
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("two", result.get(0).getMember());
        Assert.assertEquals("three", result.get(1).getMember());
        Assert.assertEquals("four", result.get(2).getMember());

        // 测试 BYSCORE 和 REV 选项组合
        result = redisun.zrange(key, 4, 2, cmd -> cmd.sortByScore().rev());
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("four", result.get(0).getMember());
        Assert.assertEquals("three", result.get(1).getMember());
        Assert.assertEquals("two", result.get(2).getMember());

        // 测试 BYSCORE 和 LIMIT 选项组合
        result = redisun.zrange(key, 1, 5, cmd -> cmd.sortByScore().limit(1, 3));
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("two", result.get(0).getMember());
        Assert.assertEquals("three", result.get(1).getMember());
        Assert.assertEquals("four", result.get(2).getMember());

        // 清理测试数据
        redisun.del(key);
    }

    @Test
    public void testAsyncZRangeWithScores() throws Exception {
        String key = topic + ":zrange-withscores";

        // 先删除可能存在的键
        redisun.del(key);

        // 添加测试数据
        redisun.zadd(key, 1.0, "one");
        redisun.zadd(key, 2.0, "two");
        redisun.zadd(key, 3.0, "three");

        // 测试 WITHSCORES 选项（异步方法）
        List<ZRangeCommand.Tuple> result = redisun.asyncZrange(key, 0, -1, cmd -> cmd.withScores()).get();
        Assert.assertEquals(3, result.size()); // 3个成员，每个成员后面跟一个分数
        Assert.assertEquals("one", result.get(0).getMember());
        Assert.assertEquals(1, result.get(0).getScore(), 0);
        Assert.assertEquals("two", result.get(1).getMember());
        Assert.assertEquals(2, result.get(1).getScore(), 0);
        Assert.assertEquals("three", result.get(2).getMember());
        Assert.assertEquals(3, result.get(2).getScore(), 0);

        // 清理测试数据
        redisun.del(key);
    }

    /**
     * 测试自动数据库切换功能
     */
    @Test
    public void testSelectCommand() {
        // 创建一个新的Redisun实例，指定数据库1
        Redisun redisunDb1 = Redisun.create(opt -> opt.debug(true).setAddress("127.0.0.1:6379").setDatabase(1));

        String key = topic + ":auto-select";
        String value = "auto-select-test-value";

        // 在数据库1中设置一个键值
        boolean setResult = redisunDb1.set(key, value);
        Assert.assertTrue("Setting key-value in database 1 should succeed", setResult);
        Assert.assertEquals("Value should be set correctly in database 1", value, redisunDb1.get(key));

        // 在默认数据库0中应该获取不到这个键值
        Assert.assertNull("Should not be able to get key from database 0", redisun.get(key));

        // 关闭数据库1的连接
        redisunDb1.close();
    }

    /**
     * 测试FLUSHDB命令的各种场景
     */
    @Test
    public void testFlushDbCommand() {
        String key1 = topic + ":flushdb1";
        String key2 = topic + ":flushdb2";
        String value = "flushdb-test-value";

        // Add some keys
        redisun.set(key1, value);
        redisun.set(key2, value);

        // Verify keys exist
        Assert.assertEquals("Key1 should exist", value, redisun.get(key1));
        Assert.assertEquals("Key2 should exist", value, redisun.get(key2));

        // Flush current database
        boolean flushed = redisun.flushDb();
        Assert.assertTrue("FLUSHDB command should succeed", flushed);

        // Verify keys no longer exist
        Assert.assertNull("Key1 should no longer exist", redisun.get(key1));
        Assert.assertNull("Key2 should no longer exist", redisun.get(key2));

        // Verify database is empty
        long dbSize = redisun.dbsize();
        Assert.assertEquals("Database should be empty", 0, dbSize);

        // Test that we can still add keys after flushDb
        String newKey = topic + ":after-flush";
        boolean setResult = redisun.set(newKey, value);
        Assert.assertTrue("Should be able to set key after FLUSHDB", setResult);
        Assert.assertEquals("New key should exist", value, redisun.get(newKey));

        // 清理测试数据
        redisun.del(newKey);
    }

    @Test
    public void testExistsCommand() {
        String key1 = topic + ":exists1";
        String key2 = topic + ":exists2";
        String value = "exists-test-value";

        // 确保键不存在
        redisun.del(key1, key2);

        // 测试键不存在的情况
        Assert.assertEquals("No keys should exist", 0, redisun.exists(key1));
        Assert.assertEquals("No keys should exist", 0, redisun.exists(key1, key2));

        // 设置一个键
        redisun.set(key1, value);

        // 测试一个键存在的情况
        Assert.assertEquals("One key should exist", 1, redisun.exists(key1));
        Assert.assertEquals("One key should exist", 1, redisun.exists(key1, key2));

        // 设置另一个键
        redisun.set(key2, value);

        // 测试两个键都存在的情况
        Assert.assertEquals("Two keys should exist", 2, redisun.exists(key1, key2));

        // 清理测试数据
        redisun.del(key1, key2);
    }

    @Test
    public void testIncrCommand() {
        String key = topic + ":incr";

        // 先删除可能存在的键
        redisun.del(key);

        // 首次调用应该返回1
        Assert.assertEquals(1, redisun.incr(key));

        // 再次调用应该返回2
        Assert.assertEquals(2, redisun.incr(key));

        // 验证GET命令能正确获取值
        Assert.assertEquals("2", redisun.get(key));

        // 设置一个字符串值，INCR应该会失败
        redisun.set(key + ":str", "hello");
        try {
            redisun.incr(key + ":str");
            Assert.fail("INCR on string value should throw exception");
        } catch (RedisunException e) {
            // 预期的异常
        }

        // 清理测试数据
        redisun.del(key, key + ":str");
    }

    @Test
    public void testDecrCommand() {
        String key = topic + ":decr";

        // 先删除可能存在的键
        redisun.del(key);

        // 首次调用应该返回-1
        Assert.assertEquals(-1, redisun.decr(key));

        // 再次调用应该返回-2
        Assert.assertEquals(-2, redisun.decr(key));

        // 验证GET命令能正确获取值
        Assert.assertEquals("-2", redisun.get(key));

        // 设置一个字符串值，DECR应该会失败
        redisun.set(key + ":str", "hello");
        try {
            redisun.decr(key + ":str");
            Assert.fail("DECR on string value should throw exception");
        } catch (RedisunException e) {
            // 预期的异常
        }

        // 清理测试数据
        redisun.del(key, key + ":str");
    }

    @Test
    public void testIncrByCommand() {
        String key = topic + ":incrby";

        // 先删除可能存在的键
        redisun.del(key);

        // 首次调用应该返回5
        Assert.assertEquals(5, redisun.incrBy(key, 5));

        // 再次调用应该返回15
        Assert.assertEquals(15, redisun.incrBy(key, 10));

        // 使用负数进行减法操作
        Assert.assertEquals(10, redisun.incrBy(key, -5));

        // 验证GET命令能正确获取值
        Assert.assertEquals("10", redisun.get(key));

        // 设置一个字符串值，INCRBY应该会失败
        redisun.set(key + ":str", "hello");
        try {
            redisun.incrBy(key + ":str", 5);
            Assert.fail("INCRBY on string value should throw exception");
        } catch (RedisunException e) {
            // 预期的异常
        }

        // 清理测试数据
        redisun.del(key, key + ":str");
    }

    @Test
    public void testDecrByCommand() {
        String key = topic + ":decrby";

        // 先删除可能存在的键
        redisun.del(key);

        // 首次调用应该返回-5
        Assert.assertEquals(-5, redisun.decrBy(key, 5));

        // 再次调用应该返回-15
        Assert.assertEquals(-15, redisun.decrBy(key, 10));

        // 使用负数进行加法操作
        Assert.assertEquals(-10, redisun.decrBy(key, -5));

        // 验证GET命令能正确获取值
        Assert.assertEquals("-10", redisun.get(key));

        // 设置一个字符串值，DECRBY应该会失败
        redisun.set(key + ":str", "hello");
        try {
            redisun.decrBy(key + ":str", 5);
            Assert.fail("DECRBY on string value should throw exception");
        } catch (RedisunException e) {
            // 预期的异常
        }

        // 清理测试数据
        redisun.del(key, key + ":str");
    }

    @Test
    public void testAppendCommand() {
        String key = topic + ":append";

        // 先删除可能存在的键
        redisun.del(key);

        // 对不存在的键执行APPEND，相当于SET操作
        Assert.assertEquals(5, redisun.append(key, "Hello"));
        Assert.assertEquals("Hello", redisun.get(key));

        // 对已存在的键执行APPEND，追加字符串
        Assert.assertEquals(11, redisun.append(key, " World"));
        Assert.assertEquals("Hello World", redisun.get(key));

        // 追加空字符串
        Assert.assertEquals(11, redisun.append(key, ""));
        Assert.assertEquals("Hello World", redisun.get(key));

        // 清理测试数据
        redisun.del(key);
    }

    @Test
    public void testStrlenCommand() {
        String key = topic + ":strlen";

        // 先删除可能存在的键
        redisun.del(key);

        // 获取不存在键的长度，应该返回0
        Assert.assertEquals(0, redisun.strlen(key));

        // 设置一个字符串值
        redisun.set(key, "Hello World");

        // 获取字符串长度
        Assert.assertEquals(11, redisun.strlen(key));

        // 设置一个空字符串
        redisun.set(key + ":empty", "");

        // 获取空字符串长度
        Assert.assertEquals(0, redisun.strlen(key + ":empty"));

        // 清理测试数据
        redisun.del(key, key + ":empty");
    }

    @Test
    public void testHSetCommand() {
        String key = topic + ":hset";
        String field = "field1";
        String value = "value1";

        // 先删除可能存在的键
        redisun.del(key);

        // 设置哈希字段，应该返回1（新建字段）
        Assert.assertEquals(1, redisun.hset(key, field, value));

        // 再次设置相同字段，应该返回0（更新字段）
        Assert.assertEquals(0, redisun.hset(key, field, "newvalue"));

        // 设置另一个字段
        Assert.assertEquals(1, redisun.hset(key, "field2", "value2"));

        // 清理测试数据
        redisun.del(key);
    }

    @Test
    public void testHGetCommand() {
        String key = topic + ":hget";
        String field = "field1";
        String value = "value1";

        // 先删除可能存在的键
        redisun.del(key);

        // 获取不存在的哈希字段，应该返回null
        Assert.assertNull(redisun.hget(key, field));

        // 设置哈希字段
        redisun.hset(key, field, value);

        // 获取存在的哈希字段，应该返回对应的值
        Assert.assertEquals(value, redisun.hget(key, field));

        // 获取不存在的字段，应该返回null
        Assert.assertNull(redisun.hget(key, "nonexistent"));

        // 清理测试数据
        redisun.del(key);
    }

    @Test
    public void testSAddCommand() {
        String key = topic + ":sadd";
        String member1 = "member1";
        String member2 = "member2";

        // 先删除可能存在的键
        redisun.del(key);

        // 添加新成员到集合，应该返回添加的成员数
        Assert.assertEquals(1, redisun.sadd(key, member1));

        // 再次添加相同成员，应该返回0
        Assert.assertEquals(0, redisun.sadd(key, member1));

        // 添加多个新成员，应该返回添加的成员数
        Assert.assertEquals(2, redisun.sadd(key, member2, "member3"));

        // 添加部分重复成员，应该只计算新成员
        Assert.assertEquals(1, redisun.sadd(key, "member3", "member4"));

        // 清理测试数据
        redisun.del(key);
    }

    @Test
    public void testMGetCommand() {
        String key1 = topic + ":mget1";
        String key2 = topic + ":mget2";
        String key3 = topic + ":mget3";
        String value1 = "value1";
        String value2 = "value2";

        // 先删除可能存在的键
        redisun.del(key1, key2, key3);

        // 设置一些键值对
        redisun.set(key1, value1);
        redisun.set(key2, value2);

        // 测试获取多个键的值
        List<String> keys = Arrays.asList(key1, key2, key3);
        List<String> values = redisun.mget(keys);

        Assert.assertEquals(3, values.size());
        Assert.assertEquals(value1, values.get(0));
        Assert.assertEquals(value2, values.get(1));
        Assert.assertNull(values.get(2)); // 不存在的键返回null

        // 清理测试数据
        redisun.del(key1, key2, key3);
    }

    @Test
    public void testMSetCommand() {
        String key1 = topic + ":mset1";
        String key2 = topic + ":mset2";
        String key3 = topic + ":mset3";
        String value1 = "value1";
        String value2 = "value2";
        String value3 = "value3";

        // 先删除可能存在的键
        redisun.del(key1, key2, key3);

        // 测试设置多个键值对
        Map<String, String> items = new HashMap<>();
        items.put(key1, value1);
        items.put(key2, value2);
        items.put(key3, value3);
        boolean result = redisun.mset(items);

        Assert.assertTrue(result);
        Assert.assertEquals(value1, redisun.get(key1));
        Assert.assertEquals(value2, redisun.get(key2));
        Assert.assertEquals(value3, redisun.get(key3));

        // 测试覆盖已存在的键
        Map<String, String> newItems = new HashMap<>();
        newItems.put(key1, "newvalue1");
        newItems.put(key2, "newvalue2");
        result = redisun.mset(newItems);

        Assert.assertTrue(result);
        Assert.assertEquals("newvalue1", redisun.get(key1));
        Assert.assertEquals("newvalue2", redisun.get(key2));
        Assert.assertEquals(value3, redisun.get(key3)); // 未更新的键保持原值

        // 清理测试数据
        redisun.del(key1, key2, key3);
    }

    @Test
    public void testListCommands() {
        String key = topic + ":list";

        // 先删除可能存在的键
        redisun.del(key);

        // 测试LPUSH命令
        Assert.assertEquals(1, redisun.lpush(key, "value1"));
        Assert.assertEquals(3, redisun.lpush(key, "value2", "value3"));

        // 测试RPUSH命令
        Assert.assertEquals(4, redisun.rpush(key, "value4"));
        Assert.assertEquals(6, redisun.rpush(key, "value5", "value6"));

        // 验证列表内容
        // LPUSH插入到头部，所以顺序是value3, value2, value1
        // RPUSH插入到尾部，所以顺序是value3, value2, value1, value4, value5, value6

        // 测试LPOP命令
        Assert.assertEquals("value3", redisun.lpop(key));
        Assert.assertEquals("value2", redisun.lpop(key));

        // 测试RPOP命令
        Assert.assertEquals("value6", redisun.rpop(key));
        Assert.assertEquals("value5", redisun.rpop(key));

        // 继续测试LPOP和RPOP
        Assert.assertEquals("value1", redisun.lpop(key));
        Assert.assertEquals("value4", redisun.rpop(key));

        // 列表应该为空
        Assert.assertNull(redisun.lpop(key));
        Assert.assertNull(redisun.rpop(key));

        // 清理测试数据
        redisun.del(key);
    }

    @After
    public void after() {
        redisun.close();
    }

    @Test
    public void testTtlAndTypeCommands() {
        String key = topic + ":ttltype";
        String value = "test-value";

        // 测试TYPE命令 - 键不存在
        String type = redisun.type(key);
        Assert.assertEquals("none", type);

        // 设置键值
        redisun.set(key, value);

        // 测试TYPE命令 - 字符串类型
        type = redisun.type(key);
        Assert.assertEquals("string", type);

        // 测试TTL命令 - 无过期时间
        long ttl = redisun.ttl(key);
        Assert.assertEquals(-1, ttl);

        // 设置过期时间
        redisun.expire(key, 10);

        // 测试TTL命令 - 有过期时间
        ttl = redisun.ttl(key);
        Assert.assertTrue(ttl > 0 && ttl <= 10);

        // 删除键
        redisun.del(key);

        // 测试TTL命令 - 键不存在
        ttl = redisun.ttl(key);
        Assert.assertEquals(-2, ttl);

        // 测试TYPE命令 - 键不存在
        type = redisun.type(key);
        Assert.assertEquals("none", type);
    }
}