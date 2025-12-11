package tech.smartboot.redisun;

/**
 * @author 三刀
 * @version v1.0 12/9/25
 */
public interface Subscriber {
    /**
     * 接收到订阅确认消息的回调方法
     *
     * @param channel 频道名称
     */
    default void onSubscribe(String channel) {
    }

    /**
     * 接收到取消订阅确认消息的回调方法
     *
     * @param channel 频道名称
     */
    default void onUnsubscribe(String channel) {
    }

    /**
     * 接收到订阅消息的回调方法
     *
     * @param channel 频道名称
     * @param message 消息内容
     */
    void onMessage(String channel, String message);

    /**
     * 当订阅连接出现错误时的回调方法
     *
     * @param throwable 错误信息
     */
    default void onError(Throwable throwable) {
    }

    /**
     * 接收到模式订阅消息的回调方法
     *
     * @param pattern 模式
     * @param channel 频道名称
     * @param message 订阅消息
     */
    default void onPMessage(String pattern, String channel, String message){
        onMessage(channel, message);
    }

    /**
     * 接收到模式订阅确认消息的回调方法
     *
     * @param pattern 模式
     */
    default void onPSubscribe(String pattern) {
    }

    /**
     * 接收到取消模式订阅确认消息的回调方法
     *
     * @param pattern 模式
     */
    default void onPUnsubscribe(String pattern) {
    }
}
