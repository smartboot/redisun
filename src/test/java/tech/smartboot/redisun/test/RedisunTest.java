package tech.smartboot.redisun.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.redisun.Redisun;

import java.util.Date;
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
        int c = redisun.del(topic);
        System.out.println("clear old data, topic:" + topic + " ,count:" + c);
    }

    /**
     * 测试SET命令的各种选项
     */
    @Test
    public void testSetCommandOptions() {
        String key = topic + ":options";
        String value = "test-value";

        // 基本SET命令测试
        boolean result = redisun.set(key, value);
        Assert.assertTrue("基本SET命令应该成功", result);
        Assert.assertEquals("值应该正确设置", value, redisun.get(key));

        // 测试NX选项 - 键不存在时应该设置成功
        String nxKey = key + ":nx";
        result = redisun.set(nxKey, value, cmd -> cmd.setIfNotExists());
        Assert.assertTrue("NX选项在键不存在时应该成功", result);
        Assert.assertEquals("值应该正确设置", value, redisun.get(nxKey));

        // 测试NX选项 - 键存在时应该设置失败
        result = redisun.set(nxKey, value + "-new", cmd -> cmd.setIfNotExists());
        Assert.assertFalse("NX选项在键存在时应该失败", result);

        // 测试XX选项 - 键存在时应该设置成功
        result = redisun.set(nxKey, value + "-updated", cmd -> cmd.setIfExists());
        Assert.assertTrue("XX选项在键存在时应该成功", result);
        Assert.assertEquals("值应该被更新", value + "-updated", redisun.get(nxKey));

        // 测试XX选项 - 键不存在时应该设置失败
        String xxKey = key + ":xx";
        result = redisun.set(xxKey, value, cmd -> cmd.setIfExists());
        Assert.assertFalse("XX选项在键不存在时应该失败", result);
    }

    /**
     * 测试SET命令的过期时间选项
     */
    @Test
    public void testSetCommandExpiration() throws InterruptedException {
        String key = topic + ":expire";
        String value = "expire-test-value";

        // 测试EX选项（秒级过期）
        boolean result = redisun.set(key, value, cmd -> cmd.expire(1));
        Assert.assertTrue("使用EX选项设置过期时间应该成功", result);
        Assert.assertEquals("值应该正确设置", value, redisun.get(key));

        // 等待过期
        Thread.sleep(1100);
        Assert.assertNull("键应该已过期", redisun.get(key));

        // 测试PX选项（毫秒级过期）
        result = redisun.set(key, value, cmd -> cmd.expireMs(500));
        Assert.assertTrue("使用PX选项设置过期时间应该成功", result);
        Assert.assertEquals("值应该正确设置", value, redisun.get(key));

        // 等待过期
        Thread.sleep(600);
        Assert.assertNull("键应该已过期", redisun.get(key));

        // 测试PXAT选项（指定时间点过期）
        Date expireDate = new Date(System.currentTimeMillis() + 500);
        result = redisun.set(key, value, cmd -> cmd.expireAt(expireDate));
        Assert.assertTrue("使用PXAT选项设置过期时间应该成功", result);
        Assert.assertEquals("值应该正确设置", value, redisun.get(key));

        // 等待过期
        Thread.sleep(600);
        Assert.assertNull("键应该已过期", redisun.get(key));

        // 测试KEEPTTL选项
        // 先设置一个有过期时间的键
        result = redisun.set(key, value, cmd -> cmd.expire(2));
        Assert.assertTrue("设置有过期时间的键应该成功", result);

        // 更新值并保留TTL
        String newValue = "new-value-with-kept-ttl";
        result = redisun.set(key, newValue, cmd -> cmd.keepTTL());
        Assert.assertTrue("使用KEEPTTL选项应该成功", result);
        Assert.assertEquals("值应该被更新", newValue, redisun.get(key));

        // 等待过期
        Thread.sleep(2100);
        Assert.assertNull("键应该已过期", redisun.get(key));
    }

    /**
     * 测试SET命令选项组合
     */
    @Test
    public void testSetCommandOptionCombinations() {
        String key = topic + ":combination";
        String value = "combination-test-value";

        // 测试NX和EX组合
        boolean result = redisun.set(key, value, cmd -> cmd.setIfNotExists().expire(1));
        Assert.assertTrue("NX和EX选项组合应该成功", result);
        Assert.assertEquals("值应该正确设置", value, redisun.get(key));

        // 再次尝试使用NX和EX组合应该失败（因为键已存在）
        result = redisun.set(key, value + "-new", cmd -> cmd.setIfNotExists().expire(1));
        Assert.assertFalse("NX和EX选项组合在键存在时应该失败", result);
        Assert.assertEquals("值不应该被更新", value, redisun.get(key));

        // 测试XX和PX组合
        result = redisun.set(key, value + "-updated", cmd -> cmd.setIfExists().expireMs(500));
        Assert.assertTrue("XX和PX选项组合应该成功", result);
        Assert.assertEquals("值应该被更新", value + "-updated", redisun.get(key));
    }

    @Test
    public void zadd() {
        redisun.del(topic);
        int i = 0;
        while (i < 1) {
            System.out.println(redisun.zadd(topic, System.currentTimeMillis() % 100, UUID.randomUUID().toString()));
            i++;
        }
    }

    @After
    public void after() {
        int c = redisun.del(topic);
        System.out.println("clear old data, topic:" + topic + " ,count:" + c);
        redisun.close();
    }
}