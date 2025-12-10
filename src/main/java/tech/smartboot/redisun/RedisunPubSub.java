package tech.smartboot.redisun;

import org.smartboot.socket.transport.AioQuickClient;
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

    private final Map<String, Subscriber> subscribers = new ConcurrentHashMap<>();
    private final Map<String, Subscriber> pending = new ConcurrentHashMap<>();
    private final Redisun redisun;
    private final AioQuickClient client;
    private boolean close = false;
    private boolean subscribed = false;

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
        subscribed = true;
        String messageType = ((BulkStrings) messageTypeResp).getValue();

        // 接收来自服务器的发布消息
        if ("message".equals(messageType)) {
            handleMessagePush(arrays);
            return;
        }
        // 订阅: 订阅成功 或者 取消订阅成功
        if ("subscribe".equals(messageType) || "unsubscribe".equals(messageType)) {
            handleSubscriptionConfirmation(messageType, arrays);
        }
    }

    void resubscribe(Throwable ex) {
        if (close) {
            return;
        }
        
        // 通知所有订阅者发生错误
        for (Subscriber subscriber : subscribers.values()) {
            try {
                subscriber.onError(ex);
            } catch (Exception e) {
                // 忽略订阅者内部错误
            }
        }
        
        for (Subscriber subscriber : pending.values()) {
            try {
                subscriber.onError(ex);
            } catch (Exception e) {
                // 忽略订阅者内部错误
            }
        }
        
        Map<Subscriber, Set<String>> oldSubscribers = new ConcurrentHashMap<>();
        for (Map.Entry<String, Subscriber> entry : subscribers.entrySet()) {
            Set<String> keys = oldSubscribers.computeIfAbsent(entry.getValue(), k -> new HashSet<>());
            keys.add(entry.getKey());
        }
        for (Map.Entry<String, Subscriber> entry : pending.entrySet()) {
            Set<String> keys = oldSubscribers.computeIfAbsent(entry.getValue(), k -> new HashSet<>());
            keys.add(entry.getKey());
        }
        // 重新订阅
        redisun.releasePubSub();
        for (Map.Entry<Subscriber, Set<String>> entry : oldSubscribers.entrySet()) {
            redisun.subscribe(entry.getKey(), entry.getValue().toArray(new String[0]));
        }
    }


    /**
     * 处理消息推送
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
     * 处理订阅/取消订阅确认消息
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
            if (subscribers.isEmpty() && pending.isEmpty()) {
                redisun.releasePubSub();
            }
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
        redisun.releasePubSub();
    }

}
