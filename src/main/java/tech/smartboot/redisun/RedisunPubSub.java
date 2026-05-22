package tech.smartboot.redisun;

import io.github.smartboot.socket.transport.AioQuickClient;
import tech.smartboot.redisun.resp.Arrays;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis 发布订阅 (pub/sub) 是一种消息通信模式：发送者 (pub) 发送消息，订阅者 (sub) 接收消息。
 * Available since: Redis Open Source 2.0.0
 *
 * @author dufuzhong
 * @version v1.0 2025-12-07
 */
class RedisunPubSub {
    // 频道订阅者（subscribe）
    private final Map<String, Subscriber> subscribers = new ConcurrentHashMap<>();
    private final Map<String, Subscriber> pending = new ConcurrentHashMap<>();
    // 模式订阅者（psubscribe）【glob 风格的正则表达式订阅】
    private final Map<String, Subscriber> pSubscribers = new ConcurrentHashMap<>();
    private final Map<String, Subscriber> pPending = new ConcurrentHashMap<>();

    // 一个 Redisun 对象，对应一个 RedisunPubSub 管理对象，绑定一个 AioQuickClient 客户端对象
    private final Redisun redisun;
    private final AioQuickClient client;
    private volatile boolean close = false;
    private volatile boolean subscribed = false;

    public RedisunPubSub(Redisun redisun, AioQuickClient client) {
        this.redisun = redisun;
        this.client = client;
    }

    public AioQuickClient getClient() {
        return client;
    }

    void subscribe(Subscriber pubsub, String... channels) {
        for (String channel : channels) {
            pending.put(channel, pubsub);
        }
    }

    void pSubscribe(Subscriber pubsub, String... patterns) {
        for (String pattern : patterns) {
            pPending.put(pattern, pubsub);
        }
    }

    /**
     * 处理来自服务端的订阅/取消订阅消息
     */
    void handleMessage(RESP msg) {
        if (!(msg instanceof Arrays)) {
            System.err.println("Invalid message type");
            return;
        }
        Arrays arrays = (Arrays) msg;
        if (arrays.getValue().size() < 3) {
            System.err.println("Invalid message format");
            return;
        }
        RESP messageTypeResp = arrays.getValue().get(0);
        if (!(messageTypeResp instanceof BulkStrings)) {
            System.err.println("Invalid message type");
            return;
        }
        if (! subscribed) {
            subscribed = true;
        }
        String messageType = ((BulkStrings) messageTypeResp).getValue();

        // 接收来自服务器的发布消息
        if ("message".equals(messageType)) {
            handleMessagePush(arrays);
            return;
        }
        // 接收来自服务器的模式订阅消息
        if ("pmessage".equals(messageType)) {
            handlePMessagePush(arrays);
            return;
        }
        // 订阅: 订阅成功 或者 取消订阅成功
        if ("subscribe".equals(messageType) || "unsubscribe".equals(messageType)) {
            handleSubscriptionConfirmation(messageType, arrays);
            return;
        }
        // 模式订阅: 模式订阅成功 或者 取消模式订阅成功
        if ("psubscribe".equals(messageType) || "punsubscribe".equals(messageType)) {
            handlePatternSubscriptionConfirmation(messageType, arrays);
        }
    }

    void resubscribe(Throwable ex) {
        if (close) {
            return;
        }
        
        // 通知所有订阅者发生错误
        subscribers.values().forEach(v -> subscriberOnError(ex, v));
        pending.values().forEach(v -> subscriberOnError(ex, v));
        pSubscribers.values().forEach(v -> subscriberOnError(ex, v));
        pPending.values().forEach(v -> subscriberOnError(ex, v));

        // 组合旧回调对象的订阅频道
        Map<Subscriber, Set<String>> oldSubscribers = new ConcurrentHashMap<>();
        extractedHandle(oldSubscribers, subscribers);
        extractedHandle(oldSubscribers, pending);
        Map<Subscriber, Set<String>> pOldSubscribers = new ConcurrentHashMap<>();
        extractedHandle(pOldSubscribers, pSubscribers);
        extractedHandle(pOldSubscribers, pPending);

        // 释放旧订阅资源
        redisun.releasePubSub();
        // 重新订阅
        oldSubscribers.forEach((key, value)
                -> redisun.subscribe(key, value.toArray(new String[0])));
        pOldSubscribers.forEach((key, value)
                -> redisun.pSubscribe(key, value.toArray(new String[0])));
    }

    private void extractedHandle(Map<Subscriber, Set<String>> pack, Map<String, Subscriber> source) {
        source.forEach((key, value)
                -> pack.computeIfAbsent(value, k -> new HashSet<>()).add(key));
    }

    private void subscriberOnError(Throwable ex, Subscriber subscriber) {
        try {
            subscriber.onError(ex);
        } catch (Exception ignored) {
            // 忽略订阅者内部错误
        }
    }


    /**
     * 处理频道订阅消息推送
     */
    private void handleMessagePush(Arrays arrays) {
        RESP channelResp = arrays.getValue().get(1);
        RESP messageResp = arrays.getValue().get(2);
        if (!(channelResp instanceof BulkStrings) || !(messageResp instanceof BulkStrings)) {
            return;
        }
        String channel = ((BulkStrings) channelResp).getValue();
        String message = ((BulkStrings) messageResp).getValue();
        // 调用订阅回调
        try {
            Subscriber subscriber = subscribers.get(channel);
            if (subscriber != null) {
                subscriber.onMessage(channel, message);
            }
        } catch (Exception e) {
            System.err.println("Error handling message push: " + e.getMessage());
        }
    }

    /**
     * 处理模式订阅消息推送
     */
    private void handlePMessagePush(Arrays arrays) {
        if (arrays.getValue().size() < 4) {
            System.err.println("Invalid pmessage format");
            return;
        }
        RESP patternResp = arrays.getValue().get(1);
        RESP channelResp = arrays.getValue().get(2);
        RESP messageResp = arrays.getValue().get(3);
        if (!(patternResp instanceof BulkStrings) || !(channelResp instanceof BulkStrings) || !(messageResp instanceof BulkStrings)) {
            return;
        }
        String pattern = ((BulkStrings) patternResp).getValue();
        String channel = ((BulkStrings) channelResp).getValue();
        String message = ((BulkStrings) messageResp).getValue();
        // 调用模式订阅回调
        try {
            Subscriber subscriber = pSubscribers.get(pattern);
            if (subscriber != null) {
                subscriber.onPMessage(pattern, channel, message);
            }
        } catch (Exception e) {
            System.err.println("Error handling pmessage push: " + e.getMessage());
        }
    }

    /**
     * 处理频道订阅/取消订阅确认消息
     */
    private void handleSubscriptionConfirmation(String messageType, Arrays arrays) {
        // 需要从订阅列表中移除相应的频道
        RESP channelResp = arrays.getValue().get(1);
        if (!(channelResp instanceof BulkStrings)) {
            return;
        }
        String channel = ((BulkStrings) channelResp).getValue();
        if ("subscribe".equals(messageType)) {
            Subscriber subscriber = pending.remove(channel);
            if (subscriber != null) {
                subscribers.put(channel, subscriber);
                try {
                    subscriber.onSubscribe(channel);
                } catch (Exception e) {
                    System.err.println("Error in onSubscribe callback: " + e.getMessage());
                }
            }
            return;
        }
        if ("unsubscribe".equals(messageType)) {
            Subscriber subscriber = subscribers.remove(channel);
            if (subscriber != null) {
                try {
                    subscriber.onUnsubscribe(channel);
                } catch (Exception e) {
                    System.err.println("Error in onUnsubscribe callback: " + e.getMessage());
                }
            }

            subscriber = pending.remove(channel);
            if (subscriber != null) {
                try {
                    subscriber.onUnsubscribe(channel);
                } catch (Exception e) {
                    System.err.println("Error in onUnsubscribe callback: " + e.getMessage());
                }
            }
            // 如果订阅列表为空，则释放订阅资源
            releasePubSub();
        }
    }

    /**
     * 处理模式订阅/取消模式订阅确认消息
     */
    private void handlePatternSubscriptionConfirmation(String messageType, Arrays arrays) {
        RESP patternResp = arrays.getValue().get(1);
        if (!(patternResp instanceof BulkStrings)) {
            return;
        }
        String pattern = ((BulkStrings) patternResp).getValue();
        if ("psubscribe".equals(messageType)){
            Subscriber subscriber = pPending.remove(pattern);
            if (subscriber != null) {
                pSubscribers.put(pattern, subscriber);
                try {
                    subscriber.onPSubscribe(pattern);
                } catch (Exception e) {
                    System.err.println("Error in onPSubscribe callback: " + e.getMessage());
                }
            }
            return;
        }
        if ("punsubscribe".equals(messageType)) {
            Subscriber subscriber = pSubscribers.remove(pattern);
            if (subscriber != null) {
                try {
                    subscriber.onUnsubscribe(pattern);
                } catch (Exception e) {
                    System.err.println("Error in onUnsubscribe callback: " + e.getMessage());
                }
            }

            subscriber = pPending.remove(pattern);
            if (subscriber != null) {
                try {
                    subscriber.onPUnsubscribe(pattern);
                } catch (Exception e) {
                    System.err.println("Error in onPUnsubscribe callback: " + e.getMessage());
                }
            }
            releasePubSub();
        }
    }

    private void releasePubSub() {
        // 如果订阅列表为空，则释放订阅资源
        if (subscribers.isEmpty() && pending.isEmpty()
                && pSubscribers.isEmpty() && pPending.isEmpty()) {
            redisun.releasePubSub();
        }
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    /**
     * 释放订阅资源
     */
    public void close() {
        close = true;
        subscribers.clear();
        pending.clear();
        pSubscribers.clear();
        pPending.clear();
        redisun.releasePubSub();
    }

}
