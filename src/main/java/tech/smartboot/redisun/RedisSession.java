package tech.smartboot.redisun;

import tech.smartboot.redisun.resp.RESP;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    private final ConcurrentLinkedQueue<CompletableFuture<RESP>> pipeline = new ConcurrentLinkedQueue<>();
    /**
     * 正在解码的响应对象
     * <p>
     * 由于TCP传输的特性，Redis响应可能分多次接收，
     * 该字段用于保存当前正在进行解码的RESP响应对象。
     * 当解码完成后会将其置为null。
     * </p>
     */
    private RESP decodingResponse;

    private int offerCount = 0;
    private int pollCount = 0;

    public int incrOfferCount() {
        return ++offerCount;
    }

    public int getOfferCount() {
        return offerCount;
    }

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

    public CompletableFuture<RESP> poll() {
        pollCount++;
        return pipeline.poll();
    }

    public void offer(CompletableFuture<RESP> future) {
        pipeline.offer(future);
    }

    public int getPollCount() {
        return pollCount;
    }

    int load() {
        int size = offerCount - pollCount;
//        System.out.println("load: " + size);
        return size >= 0 ? size : -size;
    }

}