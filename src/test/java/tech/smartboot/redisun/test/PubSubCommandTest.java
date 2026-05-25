package tech.smartboot.redisun.test;

import org.junit.Assert;
import org.junit.Test;
import tech.smartboot.redisun.Redisun;
import tech.smartboot.redisun.RedisunException;
import tech.smartboot.redisun.Subscriber;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 发布订阅命令测试类
 * 测试SUBSCRIBE, UNSUBSCRIBE, PSUBSCRIBE, PUNSUBSCRIBE, PUBLISH等命令
 *
 * @author 三刀
 * @version v1.0 10/21/25
 */
public class PubSubCommandTest extends AbstractRedisunTest {

    // ==================== SUBSCRIBE/PUBLISH测试 ====================

    @Test
    public void testPubSubBasic() throws InterruptedException {
        String channel = topic + ":pubsub";
        String message = "Hello, Redisun!";

        AtomicReference<String> receivedMessage = new AtomicReference<>();
        AtomicReference<String> receivedChannel = new AtomicReference<>();

        Subscriber pubsub = (ch, msg) -> {
            receivedChannel.set(ch);
            receivedMessage.set(msg);
        };

        Thread subscribeThread = new Thread(() -> {
            Redisun subscriber = Redisun.create(opt -> opt.debug(true).setAddress("127.0.0.1:6379"));
            try {
                subscriber.subscribe(pubsub, channel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        subscribeThread.start();
        Thread.sleep(1000);

        int receivers = redisun.publish(channel, message);
        Thread.sleep(1000);

        Assert.assertEquals(1, receivers);
        Assert.assertEquals(channel, receivedChannel.toString());
        Assert.assertEquals(message, receivedMessage.toString());

        subscribeThread.interrupt();
    }

    @Test
    public void testPubSubMultipleChannels() throws InterruptedException {
        String channel1 = topic + ":multi-1";
        String channel2 = topic + ":multi-2";
        String channel3 = topic + ":multi-3";

        Map<String, List<String>> receivedMessages = new ConcurrentHashMap<>();
        receivedMessages.put(channel1, new CopyOnWriteArrayList<>());
        receivedMessages.put(channel2, new CopyOnWriteArrayList<>());
        receivedMessages.put(channel3, new CopyOnWriteArrayList<>());

        Subscriber pubsub = (ch, msg) -> receivedMessages.get(ch).add(msg);

        Redisun subscriber = Redisun.create(opt -> opt.debug(false).setAddress("127.0.0.1:6379"));
        subscriber.subscribe(pubsub, channel1, channel2, channel3);

        Thread.sleep(1500);

        // 向每个频道发送消息
        Assert.assertEquals(1, redisun.publish(channel1, "msg1"));
        Assert.assertEquals(1, redisun.publish(channel2, "msg2"));
        Assert.assertEquals(1, redisun.publish(channel3, "msg3"));

        Thread.sleep(1000);

        Assert.assertEquals(1, receivedMessages.get(channel1).size());
        Assert.assertEquals(1, receivedMessages.get(channel2).size());
        Assert.assertEquals(1, receivedMessages.get(channel3).size());

        subscriber.close();
    }

    @Test
    public void testPubSubMultipleSubscribers() throws InterruptedException {
        String channel = topic + ":multi-sub";

        AtomicInteger messageCount1 = new AtomicInteger(0);
        AtomicInteger messageCount2 = new AtomicInteger(0);

        Subscriber subscriber1 = (ch, msg) -> messageCount1.incrementAndGet();
        Subscriber subscriber2 = (ch, msg) -> messageCount2.incrementAndGet();

        Redisun redisun1 = Redisun.create(opt -> opt.debug(false).setAddress("127.0.0.1:6379"));
        Redisun redisun2 = Redisun.create(opt -> opt.debug(false).setAddress("127.0.0.1:6379"));

        redisun1.subscribe(subscriber1, channel);
        redisun2.subscribe(subscriber2, channel);

        Thread.sleep(1000);

        int receivers = redisun.publish(channel, "test-message");
        Thread.sleep(1000);

        Assert.assertEquals(2, receivers);
        Assert.assertEquals(1, messageCount1.get());
        Assert.assertEquals(1, messageCount2.get());

        redisun1.close();
        redisun2.close();
    }

    // ==================== UNSUBSCRIBE测试 ====================

    @Test
    public void testUnsubscribeSingle() throws InterruptedException {
        String channel = topic + ":unsub-single";

        AtomicInteger messageCount = new AtomicInteger(0);
        Subscriber subscriber = (ch, msg) -> messageCount.incrementAndGet();

        Redisun redisunSubscriber = Redisun.create(opt -> opt.debug(false).setAddress("127.0.0.1:6379"));
        redisunSubscriber.subscribe(subscriber, channel);

        Thread.sleep(1000);

        // 发送第一条消息
        Assert.assertEquals(1, redisun.publish(channel, "msg1"));
        Thread.sleep(500);
        Assert.assertEquals(1, messageCount.get());

        // 取消订阅
        redisunSubscriber.unsubscribe(channel);
        Thread.sleep(1000);

        // 发送第二条消息
        Assert.assertEquals(0, redisun.publish(channel, "msg2"));
        Thread.sleep(500);
        Assert.assertEquals(1, messageCount.get());

        redisunSubscriber.close();
    }

    @Test
    public void testUnsubscribeMultiple() throws InterruptedException {
        String channel1 = topic + ":unsub-multi1";
        String channel2 = topic + ":unsub-multi2";

        AtomicInteger messageCount = new AtomicInteger(0);
        Subscriber subscriber = (ch, msg) -> messageCount.incrementAndGet();

        Redisun redisunSubscriber = Redisun.create(opt -> opt.debug(false).setAddress("127.0.0.1:6379"));
        redisunSubscriber.subscribe(subscriber, channel1, channel2);

        Thread.sleep(1000);

        // 发送消息到两个频道
        redisun.publish(channel1, "msg1");
        redisun.publish(channel2, "msg2");
        Thread.sleep(500);
        Assert.assertEquals(2, messageCount.get());

        // 取消订阅一个频道
        redisunSubscriber.unsubscribe(channel1);
        Thread.sleep(1000);

        // 再次发送消息
        redisun.publish(channel1, "msg3");
        redisun.publish(channel2, "msg3");
        Thread.sleep(500);
        Assert.assertEquals(3, messageCount.get());

        redisunSubscriber.close();
    }

    @Test
    public void testUnsubscribeAll() throws InterruptedException {
        String channel1 = topic + ":unsub-all1";
        String channel2 = topic + ":unsub-all2";

        AtomicInteger messageCount = new AtomicInteger(0);
        Subscriber subscriber = (ch, msg) -> messageCount.incrementAndGet();

        Redisun redisunSubscriber = Redisun.create(opt -> opt.debug(false).setAddress("127.0.0.1:6379"));
        redisunSubscriber.subscribe(subscriber, channel1, channel2);

        Thread.sleep(1000);

        // 发送消息
        redisun.publish(channel1, "msg1");
        Thread.sleep(500);
        Assert.assertEquals(1, messageCount.get());

        // 取消所有订阅
        redisunSubscriber.unsubscribe();
        Thread.sleep(1000);

        // 再次发送消息
        Assert.assertEquals(0, redisun.publish(channel1, "msg2"));
        Assert.assertEquals(0, redisun.publish(channel2, "msg2"));
        Thread.sleep(500);
        Assert.assertEquals(1, messageCount.get());

        redisunSubscriber.close();
    }

    // ==================== 模式订阅测试 ====================

    @Test
    public void testPSubscribe() throws InterruptedException {
        String pattern = topic + ":pattern:*";
        String channel1 = topic + ":pattern:1";
        String channel2 = topic + ":pattern:2";
        String channel3 = topic + ":other:1";

        AtomicInteger messageCount = new AtomicInteger(0);
        Subscriber subscriber = (ch, msg) -> messageCount.incrementAndGet();

        Redisun redisunSubscriber = Redisun.create(opt -> opt.debug(false).setAddress("127.0.0.1:6379"));
        redisunSubscriber.pSubscribe(subscriber, pattern);

        Thread.sleep(1000);

        // 发送消息到匹配模式的频道
        redisun.publish(channel1, "msg1");
        redisun.publish(channel2, "msg2");
        Thread.sleep(500);
        Assert.assertEquals(2, messageCount.get());

        // 发送消息到不匹配模式的频道
        redisun.publish(channel3, "msg3");
        Thread.sleep(500);
        Assert.assertEquals(2, messageCount.get());

        redisunSubscriber.close();
    }

    @Test
    public void testPUnsubscribe() throws InterruptedException {
        String pattern = topic + ":punsub:*";
        String channel = topic + ":punsub:test";

        AtomicInteger messageCount = new AtomicInteger(0);
        Subscriber subscriber = (ch, msg) -> messageCount.incrementAndGet();

        redisun.pSubscribe(subscriber, pattern);
        Thread.sleep(1000);

        // 发送消息
        redisun.publish(channel, "msg1");
        Thread.sleep(500);
        Assert.assertEquals(1, messageCount.get());

        // 取消模式订阅
        redisun.pUnsubscribe(pattern);
        Thread.sleep(1000);

        // 再次发送消息
        redisun.publish(channel, "msg2");
        Thread.sleep(500);
        Assert.assertEquals(1, messageCount.get());
    }

    @Test
    public void testPSubscribeMultiplePatterns() throws InterruptedException {
        String pattern1 = topic + ":multi:a*";
        String pattern2 = topic + ":multi:b*";
        String channel1 = topic + ":multi:abc";
        String channel2 = topic + ":multi:bcd";
        String channel3 = topic + ":multi:cde";

        Map<String, List<String>> receivedMessages = new ConcurrentHashMap<>();

        Subscriber subscriber = (ch, msg) -> {
            receivedMessages.computeIfAbsent(ch, k -> new CopyOnWriteArrayList<>()).add(msg);
        };

        Redisun redisunSubscriber = Redisun.create(opt -> opt.debug(false).setAddress("127.0.0.1:6379"));
        redisunSubscriber.pSubscribe(subscriber, pattern1, pattern2);

        Thread.sleep(1000);

        redisun.publish(channel1, "msg1");
        redisun.publish(channel2, "msg2");
        redisun.publish(channel3, "msg3");

        Thread.sleep(1000);

        Assert.assertTrue(receivedMessages.containsKey(channel1));
        Assert.assertTrue(receivedMessages.containsKey(channel2));
        Assert.assertFalse(receivedMessages.containsKey(channel3));

        redisunSubscriber.close();
    }

    // ==================== 异常场景测试 ====================

    @Test(expected = RedisunException.class)
    public void testSubscribeWithNullChannels() {
        redisun.subscribe(new Subscriber() {
            @Override
            public void onMessage(String channel, String message) {
            }
        }, (String[]) null);
    }

    @Test(expected = RedisunException.class)
    public void testSubscribeWithEmptyChannels() {
        redisun.subscribe(new Subscriber() {
            @Override
            public void onMessage(String channel, String message) {
            }
        });
    }

    @Test(expected = RedisunException.class)
    public void testSubscribeWithNullSubscriber() {
        redisun.subscribe(null, "test-channel");
    }

    @Test(expected = RedisunException.class)
    public void testPublishWithNullChannel() {
        redisun.publish(null, "message");
    }

    @Test(expected = RedisunException.class)
    public void testPublishWithNullMessage() {
        redisun.publish(topic + ":channel", null);
    }

    @Test(expected = RedisunException.class)
    public void testPSubscribeWithNullPatterns() {
        redisun.pSubscribe(new Subscriber() {
            @Override
            public void onMessage(String channel, String message) {
            }
        }, (String[]) null);
    }

    @Test(expected = RedisunException.class)
    public void testPSubscribeWithEmptyPatterns() {
        redisun.pSubscribe(new Subscriber() {
            @Override
            public void onMessage(String channel, String message) {
            }
        });
    }

    @Test(expected = RedisunException.class)
    public void testPSubscribeWithNullSubscriber() {
        redisun.pSubscribe(null, "pattern:*");
    }

    // ==================== 回调测试 ====================

    @Test
    public void testOnSubscribeCallback() throws InterruptedException {
        String channel = topic + ":callback-sub";
        AtomicBoolean subscribed = new AtomicBoolean(false);

        Subscriber subscriber = new Subscriber() {
            @Override
            public void onSubscribe(String channel) {
                subscribed.set(true);
            }

            @Override
            public void onMessage(String channel, String message) {
            }
        };

        Redisun redisunSubscriber = Redisun.create(opt -> opt.debug(false).setAddress("127.0.0.1:6379"));
        redisunSubscriber.subscribe(subscriber, channel);

        Thread.sleep(1000);
        Assert.assertTrue("onSubscribe should be called", subscribed.get());

        redisunSubscriber.close();
    }

    @Test
    public void testOnUnsubscribeCallback() throws InterruptedException {
        String channel = topic + ":callback-unsub";
        AtomicBoolean unsubscribed = new AtomicBoolean(false);

        Subscriber subscriber = new Subscriber() {
            @Override
            public void onUnsubscribe(String channel) {
                unsubscribed.set(true);
            }

            @Override
            public void onMessage(String channel, String message) {
            }
        };

        Redisun redisunSubscriber = Redisun.create(opt -> opt.debug(false).setAddress("127.0.0.1:6379"));
        redisunSubscriber.subscribe(subscriber, channel);

        Thread.sleep(1000);
        redisunSubscriber.unsubscribe(channel);
        Thread.sleep(1000);

        Assert.assertTrue("onUnsubscribe should be called", unsubscribed.get());
        redisunSubscriber.close();
    }

    @Test
    public void testOnErrorCallback() throws InterruptedException {
        String channel = topic + ":callback-error";
        AtomicReference<Throwable> errorRef = new AtomicReference<>(null);

        Subscriber subscriber = new Subscriber() {
            @Override
            public void onMessage(String channel, String message) {
            }

            @Override
            public void onError(Throwable throwable) {
                errorRef.set(throwable);
            }
        };

        Redisun redisunSubscriber = Redisun.create(opt -> opt.debug(false).setAddress("127.0.0.1:6379"));
        redisunSubscriber.subscribe(subscriber, channel);

        Thread.sleep(1000);
        redisunSubscriber.close();
        Thread.sleep(1000);

        // 错误回调可能在连接关闭时被调用
    }

    // ==================== 高并发测试 ====================

    @Test
    public void testHighVolumePubSub() throws InterruptedException {
        String channel = topic + ":high-volume";
        AtomicInteger messageCount = new AtomicInteger(0);
        final int MESSAGE_COUNT = 100;

        Subscriber subscriber = (ch, msg) -> messageCount.incrementAndGet();

        Redisun redisunSubscriber = Redisun.create(opt -> opt.debug(false).setAddress("127.0.0.1:6379"));
        redisunSubscriber.subscribe(subscriber, channel);

        Thread.sleep(1000);

        for (int i = 0; i < MESSAGE_COUNT; i++) {
            redisun.publish(channel, "message-" + i);
        }

        Thread.sleep(2000);

        Assert.assertEquals(MESSAGE_COUNT, messageCount.get());

        redisunSubscriber.close();
    }

    @Test
    public void testConcurrentPubSub() throws InterruptedException {
        String channel = topic + ":concurrent";
        AtomicInteger messageCount = new AtomicInteger(0);
        final int THREAD_COUNT = 5;
        final int MESSAGES_PER_THREAD = 20;

        Subscriber subscriber = (ch, msg) -> messageCount.incrementAndGet();

        Redisun redisunSubscriber = Redisun.create(opt -> opt.debug(false).setAddress("127.0.0.1:6379"));
        redisunSubscriber.subscribe(subscriber, channel);

        Thread.sleep(1000);

        Thread[] threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < MESSAGES_PER_THREAD; j++) {
                    redisun.publish(channel, "thread-" + threadId + "-msg-" + j);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Thread.sleep(2000);

        Assert.assertEquals(THREAD_COUNT * MESSAGES_PER_THREAD, messageCount.get());

        redisunSubscriber.close();
    }

    // ==================== 边界条件测试 ====================

    @Test
    public void testPublishToNoSubscribers() {
        String channel = topic + ":no-sub";

        // 发布到没有订阅者的频道
        int receivers = redisun.publish(channel, "message");
        Assert.assertEquals(0, receivers);
    }

    @Test
    public void testLargeMessage() throws InterruptedException {
        String channel = topic + ":large-msg";
        StringBuilder largeMessage = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeMessage.append("a");
        }

        AtomicReference<String> receivedMessage = new AtomicReference<>();

        Subscriber subscriber = (ch, msg) -> receivedMessage.set(msg);

        Redisun redisunSubscriber = Redisun.create(opt -> opt.debug(false).setAddress("127.0.0.1:6379"));
        redisunSubscriber.subscribe(subscriber, channel);

        Thread.sleep(1000);

        redisun.publish(channel, largeMessage.toString());
        Thread.sleep(1000);

        Assert.assertEquals(largeMessage.toString(), receivedMessage.get());

        redisunSubscriber.close();
    }

    @Test
    public void testSpecialCharactersInMessage() throws InterruptedException {
        String channel = topic + ":special-msg";
        String[] messages = {
            "!@#$%^&*()_+-=[]{}|;':\",./<>?",
            "中文测试🎉",
            "\n\r\t",
            "  spaces  "
        };

        List<String> receivedMessages = new CopyOnWriteArrayList<>();

        Subscriber subscriber = (ch, msg) -> receivedMessages.add(msg);

        Redisun redisunSubscriber = Redisun.create(opt -> opt.debug(false).setAddress("127.0.0.1:6379"));
        redisunSubscriber.subscribe(subscriber, channel);

        Thread.sleep(1000);

        for (String message : messages) {
            redisun.publish(channel, message);
        }

        Thread.sleep(1000);

        Assert.assertEquals(messages.length, receivedMessages.size());
        for (int i = 0; i < messages.length; i++) {
            Assert.assertEquals(messages[i], receivedMessages.get(i));
        }

        redisunSubscriber.close();
    }

    @Test
    public void testRapidSubscribeUnsubscribe() throws InterruptedException {
        String channel = topic + ":rapid";

        for (int i = 0; i < 10; i++) {
            AtomicInteger messageCount = new AtomicInteger(0);
            Subscriber subscriber = (ch, msg) -> messageCount.incrementAndGet();

            Redisun redisunSubscriber = Redisun.create(opt -> opt.debug(false).setAddress("127.0.0.1:6379"));
            redisunSubscriber.subscribe(subscriber, channel);
            Thread.sleep(100);

            redisun.publish(channel, "msg-" + i);
            Thread.sleep(100);

            Assert.assertEquals(1, messageCount.get());

            redisunSubscriber.unsubscribe(channel);
            Thread.sleep(100);
            redisunSubscriber.close();
        }
    }
}
