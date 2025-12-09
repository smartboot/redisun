package tech.smartboot.redisun;

import tech.smartboot.redisun.resp.Arrays;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Redis 发布订阅 (pub/sub) 是一种消息通信模式：发送者 (pub) 发送消息，订阅者 (sub) 接收消息。
 *  Available since: Redis Open Source 2.0.0
 *
 * @author dufuzhong
 * @version v1.0 2025-12-07
 */
@SuppressWarnings({"unused", "rawtypes"})
public abstract class RedisunPubSub {
    // 订阅频道
    private final Set<String> channels = ConcurrentHashMap.newKeySet();
    // 取消订阅
    private Consumer<String[]> unsubscribe;
    // 关闭Redis连接
    private Runnable releaseClient;
    // 网络异常重新订阅
    private BiConsumer<RedisunPubSub, String[]> subscribe;

    /**
     * 接收到订阅消息的回调方法
     * @param channel 频道名称
     * @param message 消息内容
     */
    public abstract void onMessage(String channel, String message);

    /**
     * 接收到订阅确认消息的回调方法
     * @param channel 频道名称
     */
    public void onSubscribe(String channel){}

    /**
     * 接收到取消订阅确认消息的回调方法
     * @param channel 频道名称
     */
    public void onUnsubscribe(String channel){}

    /**
     * 异常关闭处理方法
     * @param channels 订阅的频道列表
     * @param ex 异常信息
     * @return 是否需要重新订阅 (true:重新订阅)
     */
    public boolean onSessionClosed(String[] channels, Throwable ex) {
        return true;
    }

    /**
     * 设置取消订阅的回调方法
     * (Redisun 类设置的)
     */
    void setUnsubscribe(Consumer<String[]> unsubscribe) {
        this.unsubscribe = unsubscribe;
    }

    /**
     * 取消订阅当前订阅的所有频道
     * 注意：此方法会关闭订阅连接
     */
    public void unsubscribeAll() {
        unsubscribe.accept(new String[0]);
    }
    
    /**
     * 取消订阅指定的频道
     * @param channels 要取消订阅的频道列表
     */
    public void unsubscribe(String... channels) {
        unsubscribe.accept(channels);
    }

    void setReleaseClient(Runnable releaseClient) {
        this.releaseClient = releaseClient;
    }

    void setSubscribe(BiConsumer<RedisunPubSub, String[]> subscribe) {
        this.subscribe = subscribe;
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

    void onSessionClosed(Throwable ex){
        String[] channelArray = getStrings();
        channels.clear();
        releaseClient.run();
        // 主动关闭连接的不需要重新订阅和通知
        if (ex == null || ex.getCause() == null || channelArray.length == 0) {
            return;
        }
        try {
            if (! this.onSessionClosed(channelArray, ex)){
                return;
            }
        }catch (Exception e) {
            System.err.println("Error handling closed exception: " + e.getMessage());
        }
        try {
            subscribe.accept(this, channelArray);
        }catch (Exception e) {
            System.err.println("Error handling subscription confirmation: " + e.getMessage());
        }
    }

    public String[] getStrings() {
        return channels.toArray(new String[0]);
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
            this.onMessage(channel, message);
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
        if ("subscribe".equals(messageType)){
            channels.add(channel);
            try{
                this.onSubscribe(channel);
            } catch (Exception e) {
                System.err.println("Error handling subscription confirmation: " + e.getMessage());
            }
            return;
        }
        if ("unsubscribe".equals(messageType)) {
            channels.remove(channel);
            // 如果所有订阅都已取消，释放订阅连接
            if (channels.isEmpty()) {
                releaseClient.run();
            }
            // 调用取消订阅回调
            try {
                this.onUnsubscribe(channel);
            }catch (Exception e) {
                System.err.println("Error handling subscription confirmation: " + e.getMessage());
            }
        }
    }

}
