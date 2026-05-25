package tech.smartboot.redisun.test;

import org.junit.Assert;
import org.junit.Test;
import tech.smartboot.redisun.Redisun;
import tech.smartboot.redisun.RedisunException;
import tech.smartboot.redisun.Subscriber;

import java.util.concurrent.CopyOnWriteArrayList;
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
    public void testPubSubCommands() throws InterruptedException {
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
    public void testUnsubscribe() throws InterruptedException {
        String channel = topic + ":unsubscribe-test";
        String message1 = "Message 1";
        String message2 = "Message 2";

        AtomicInteger messageCount = new AtomicInteger(0);
        StringBuilder receivedMessages = new StringBuilder();

        Subscriber pubsub = new Subscriber() {
            @Override
            public void onMessage(String ch, String msg) {
                messageCount.incrementAndGet();
                if (receivedMessages.length() > 0) {
                    receivedMessages.append(",");
                }
                receivedMessages.append(msg);
            }
        };

        Redisun subscriber = Redisun.create(opt -> opt.debug(true).setAddress("127.0.0.1:6379"));
        subscriber.subscribe(pubsub, channel);

        Thread.sleep(1000);

        int receivers1 = redisun.publish(channel, message1);
        Thread.sleep(500);
        Assert.assertEquals(1, receivers1);
        Assert.assertEquals(1, messageCount.get());

        subscriber.unsubscribe(channel);
        Thread.sleep(1000);

        int receivers2 = redisun.publish(channel, message2);
        Thread.sleep(500);
        Assert.assertEquals(0, receivers2);
        Assert.assertEquals(1, messageCount.get());
        Assert.assertEquals(message1, receivedMessages.toString());
    }

    @Test
    public void testMultipleSubscriptions() throws InterruptedException {
        String channel1 = topic + ":multi-sub-1";
        String channel2 = topic + ":multi-sub-2";
        String channel3 = topic + ":multi-sub-3";

        java.util.Map<String, java.util.List<String>> receivedMessages = new java.util.concurrent.ConcurrentHashMap<>();
        receivedMessages.put(channel1, new CopyOnWriteArrayList<>());
        receivedMessages.put(channel2, new CopyOnWriteArrayList<>());
        receivedMessages.put(channel3, new CopyOnWriteArrayList<>());

        Subscriber pubsub1 = (ch, msg) -> receivedMessages.get(ch).add(msg);
        Subscriber pubsub2 = (ch, msg) -> receivedMessages.get(ch).add(msg);
        Subscriber pubsub3 = (ch, msg) -> receivedMessages.get(ch).add(msg);

        Thread thread1 = new Thread(() -> {
            Redisun subscriber = Redisun.create(opt -> opt.debug(false).setAddress("127.0.0.1:6379"));
            try {
                subscriber.subscribe(pubsub1, channel1, channel2);
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                // Normal exit
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread thread2 = new Thread(() -> {
            Redisun subscriber = Redisun.create(opt -> opt.debug(false).setAddress("127.0.0.1:6379"));
            try {
                subscriber.subscribe(pubsub2, channel2, channel3);
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                // Normal exit
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Redisun subscriber = Redisun.create(opt -> opt.debug(false).setAddress("127.0.0.1:6379"));
        subscriber.subscribe(pubsub3, channel1);

        thread1.start();
        thread2.start();

        Thread.sleep(1500);

        String msg1 = "Message to channel1";
        int receivers1 = redisun.publish(channel1, msg1);
        Thread.sleep(500);
        Assert.assertEquals(2, receivers1);
        Assert.assertEquals(2, receivedMessages.get(channel1).size());

        String msg2 = "Message to channel2";
        int receivers2 = redisun.publish(channel2, msg2);
        Thread.sleep(500);
        Assert.assertEquals(2, receivers2);
        Assert.assertEquals(2, receivedMessages.get(channel2).size());

        String msg3 = "Message to channel3";
        int receivers3 = redisun.publish(channel3, msg3);
        Thread.sleep(500);
        Assert.assertEquals(1, receivers3);
        Assert.assertEquals(1, receivedMessages.get(channel3).size());

        subscriber.unsubscribe(channel1);
        Thread.sleep(1000);

        String msg4 = "Another message to channel1";
        int receivers4 = redisun.publish(channel1, msg4);
        Thread.sleep(500);
        Assert.assertEquals(1, receivers4);
        Assert.assertEquals(3, receivedMessages.get(channel1).size());

        thread1.interrupt();
        thread2.interrupt();
    }

    @Test
    public void testSingleConnectionMultipleChannels() throws InterruptedException {
        String channel1 = topic + ":single-conn-1";
        String channel2 = topic + ":single-conn-2";
        String channel3 = topic + ":single-conn-3";

        java.util.Map<String, java.util.List<String>> receivedMessages = new java.util.concurrent.ConcurrentHashMap<>();
        receivedMessages.put(channel1, new CopyOnWriteArrayList<>());
        receivedMessages.put(channel2, new CopyOnWriteArrayList<>());
        receivedMessages.put(channel3, new CopyOnWriteArrayList<>());

        Subscriber pubsub = (ch, msg) -> receivedMessages.get(ch).add(msg);

        Redisun subscriber = Redisun.create(opt -> opt.debug(true).setAddress("127.0.0.1:6379"));
        subscriber.subscribe(pubsub, channel1, channel2, channel3);

        Thread.sleep(1500);

        String msg1 = "Message 1 to channel1";
        int receivers1 = redisun.publish(channel1, msg1);
        Thread.sleep(500);
        Assert.assertEquals(1, receivers1);
        Assert.assertEquals(1, receivedMessages.get(channel1).size());

        String msg2 = "Message 1 to channel2";
        int receivers2 = redisun.publish(channel2, msg2);
        Thread.sleep(500);
        Assert.assertEquals(1, receivers2);
        Assert.assertEquals(1, receivedMessages.get(channel2).size());

        String msg3 = "Message 1 to channel3";
        int receivers3 = redisun.publish(channel3, msg3);
        Thread.sleep(500);
        Assert.assertEquals(1, receivers3);
        Assert.assertEquals(1, receivedMessages.get(channel3).size());

        // 快速连续发布
        redisun.publish(channel1, "Message 2 to channel1");
        redisun.publish(channel2, "Message 2 to channel2");
        redisun.publish(channel3, "Message 2 to channel3");
        Thread.sleep(1000);

        Assert.assertEquals(2, receivedMessages.get(channel1).size());
        Assert.assertEquals(2, receivedMessages.get(channel2).size());
        Assert.assertEquals(2, receivedMessages.get(channel3).size());

        // 部分取消订阅
        subscriber.unsubscribe(channel2);
        Thread.sleep(2000);

        int receivers7 = redisun.publish(channel1, "Message 3 to channel1");
        int receivers8 = redisun.publish(channel2, "Message 3 to channel2");
        int receivers9 = redisun.publish(channel3, "Message 3 to channel3");
        Thread.sleep(1000);

        Assert.assertEquals(1, receivers7);
        Assert.assertEquals(0, receivers8);
        Assert.assertEquals(1, receivers9);

        Assert.assertEquals(3, receivedMessages.get(channel1).size());
        Assert.assertEquals(2, receivedMessages.get(channel2).size());
        Assert.assertEquals(3, receivedMessages.get(channel3).size());

        // 取消所有订阅
        subscriber.unsubscribe();
        Thread.sleep(1000);

        int receivers10 = redisun.publish(channel1, "No one receives this");
        int receivers11 = redisun.publish(channel3, "No one receives this");
        Thread.sleep(500);

        Assert.assertEquals(0, receivers10);
        Assert.assertEquals(0, receivers11);
    }

    // ==================== 模式订阅测试 ====================

    @Test
    public void testPUnsubscribe() throws InterruptedException {
        StringBuilder receivedChannel = new StringBuilder();
        redisun.pSubscribe((channel, message) -> {
            receivedChannel.append(channel);
        }, "channel:*");

        Thread.sleep(2000);

        String channel = "channel:123";
        int publish = redisun.publish(channel, "你好");
        Assert.assertEquals(1, publish);
        Thread.sleep(1000);

        redisun.pUnsubscribe("channel:*");
        Thread.sleep(1000);

        publish = redisun.publish("channel:456", "你好");
        Assert.assertEquals(0, publish);
        Assert.assertEquals(channel, receivedChannel.toString());
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

    // ==================== 回调测试 ====================

    @Test
    public void testSubscriptionCallbacks() throws InterruptedException {
        String channel = topic + ":callback-test";
        AtomicBoolean subscribed = new AtomicBoolean(false);
        AtomicBoolean unsubscribed = new AtomicBoolean(false);

        Subscriber subscriber = new Subscriber() {
            @Override
            public void onSubscribe(String channel) {
                subscribed.set(true);
            }

            @Override
            public void onUnsubscribe(String channel) {
                unsubscribed.set(true);
            }

            @Override
            public void onMessage(String channel, String message) {
            }
        };

        Redisun redisunSubscriber = Redisun.create(opt -> opt.debug(true).setAddress("127.0.0.1:6379"));
        redisunSubscriber.subscribe(subscriber, channel);

        Thread.sleep(1000);
        Assert.assertTrue("onSubscribe should be called", subscribed.get());

        redisunSubscriber.unsubscribe(channel);
        Thread.sleep(1000);

        Assert.assertTrue("onUnsubscribe should be called", unsubscribed.get());
        redisunSubscriber.close();
    }

    @Test
    public void testErrorCallback() throws InterruptedException {
        String channel = topic + ":error-test";
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

        Redisun redisunSubscriber = Redisun.create(opt -> opt.debug(true).setAddress("127.0.0.1:6379"));
        redisunSubscriber.subscribe(subscriber, channel);

        Thread.sleep(1000);
        redisunSubscriber.close();
        Thread.sleep(1000);
    }

    // ==================== 高并发测试 ====================

    @Test
    public void testMultipleSubscriptionsToSameChannel() throws InterruptedException {
        String channel = topic + ":multiple-same-channel";
        AtomicInteger messageCount1 = new AtomicInteger(0);
        AtomicInteger messageCount2 = new AtomicInteger(0);

        Subscriber subscriber1 = (ch, msg) -> messageCount1.incrementAndGet();
        Subscriber subscriber2 = (ch, msg) -> messageCount2.incrementAndGet();

        Redisun redisunSubscriber1 = Redisun.create(opt -> opt.debug(false).setAddress("127.0.0.1:6379"));
        Redisun redisunSubscriber2 = Redisun.create(opt -> opt.debug(false).setAddress("127.0.0.1:6379"));

        redisunSubscriber1.subscribe(subscriber1, channel);
        redisunSubscriber2.subscribe(subscriber2, channel);

        Thread.sleep(1000);

        int receivers = redisun.publish(channel, "test-message");
        Thread.sleep(1000);

        Assert.assertEquals(2, receivers);
        Assert.assertEquals(1, messageCount1.get());
        Assert.assertEquals(1, messageCount2.get());

        redisunSubscriber1.close();
        redisunSubscriber2.close();
    }

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
}
