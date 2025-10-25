package tech.smartboot.redisun;

import tech.smartboot.redisun.resp.RESP;

import java.util.concurrent.CompletableFuture;

/**
 * Redis会话管理类
 * <p>
 * 该类用于管理Redis客户端与服务器之间的会话状态，包括：
 * 1. 正在解码的响应对象跟踪
 * 2. 异步操作结果的Future管理
 * </p>
 * <p>
 * 每个与Redis服务器的连接都会关联一个RedisSession实例，
 * 用于维护该连接的会话状态和异步操作结果。
 * </p>
 *
 * @author 三刀
 * @version v1.0 10/21/25
 * @see RESP Redis序列化协议响应对象
 * @see CompletableFuture 异步计算结果容器
 */
final class RedisSession {
    /**
     * 正在解码的响应对象
     * <p>
     * 由于TCP传输的特性，Redis响应可能分多次接收，
     * 该字段用于保存当前正在进行解码的RESP响应对象。
     * 当解码完成后会将其置为null。
     * </p>
     */
    private RESP decodingResponse;

    /**
     * 异步操作结果的Future对象
     * <p>
     * 用于保存当前会话中正在进行的异步操作结果。
     * 当Redis服务器返回响应后，会通过此Future对象通知调用方。
     * </p>
     */
    private CompletableFuture<RESP> future;

    /**
     * 获取正在解码的响应对象
     *
     * @return 正在解码的RESP响应对象，如果当前没有正在解码的对象则返回null
     */
    RESP getDecodingResponse() {
        return decodingResponse;
    }

    /**
     * 设置正在解码的响应对象
     *
     * @param decodingResponse 正在解码的RESP响应对象
     */
    void setDecodingResponse(RESP decodingResponse) {
        this.decodingResponse = decodingResponse;
    }

    /**
     * 获取异步操作结果的Future对象
     *
     * @return 异步操作结果的Future对象
     */
    public CompletableFuture<RESP> getFuture() {
        return future;
    }

    /**
     * 设置异步操作结果的Future对象
     *
     * @param future 异步操作结果的Future对象
     */
    public void setFuture(CompletableFuture<RESP> future) {
        this.future = future;
    }
}