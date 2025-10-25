package tech.smartboot.redisun;

import org.smartboot.socket.Protocol;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioSession;
import tech.smartboot.redisun.resp.RESP;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * Redis消息处理器
 * <p>
 * 该类负责处理Redis服务器返回的响应消息，smart-socket框架的消息处理接口。
 * 主要功能包括：
 * 1. 解码从Redis服务器接收的字节流数据
 * 2. 处理完整的RESP响应消息
 * 3. 管理会话状态事件
 * </p>
 * <p>
 * 该处理器同时实现了Protocol接口用于解码和AbstractMessageProcessor用于消息处理，
 * 是Redis客户端与底层网络通信框架之间的桥梁。
 * </p>
 *
 * @author 三刀
 * @version v1.0 10/21/25
 * @see Protocol
 * @see AbstractMessageProcessor
 * @see RESP Redis序列化协议
 */
class RedisMessageProcessor extends AbstractMessageProcessor<RESP> implements Protocol<RESP> {
    /**
     * 解码从Redis服务器接收到的字节流数据
     * <p>
     * 该方法会尝试从ByteBuffer中解析出一个完整的RESP响应对象。
     * 由于TCP传输的特性，可能需要多次调用才能解析出完整的消息。
     * </p>
     *
     * @param readBuffer 待解码的字节缓冲区
     * @param session    当前会话对象
     * @return 解析成功的RESP对象，如果数据不完整则返回null
     * @see RESP#decode(ByteBuffer)
     */
    @Override
    public RESP decode(ByteBuffer readBuffer, AioSession session) {
        // 至少需要2个字节才能开始解析(类型标识符+至少1个数据字节)
        if (readBuffer.remaining() < 2) {
            return null;
        }

        // 获取当前会话关联的Redis会话对象
        RedisSession redisSession = session.getAttachment();

        // 获取正在进行解码的响应对象，如果为空则创建一个新的
        RESP redisResponse = redisSession.getDecodingResponse();
        if (redisResponse == null) {
            // 根据第一个字节确定RESP数据类型并创建对应实例
            redisResponse = RESP.newInstance(readBuffer.get());
            redisSession.setDecodingResponse(redisResponse);
        }

        // 尝试解码完整的RESP响应
        if (redisResponse.decode(readBuffer)) {
            // 解码成功，清除正在解码的响应引用
            redisSession.setDecodingResponse(null);
            return redisResponse;
        }

        // 解码未完成，等待更多数据
        return null;
    }

    /**
     * 处理完整的RESP响应消息
     * <p>
     * 当decode方法成功解析出一个完整的RESP对象后，会调用此方法进行处理。
     * 主要任务是将解析出的响应对象传递给等待结果的Future对象。
     * </p>
     *
     * @param session 当前会话对象
     * @param msg     解析完成的RESP响应消息
     */
    @Override
    public void process0(AioSession session, RESP msg) {
        // 获取当前会话关联的Redis会话对象
        RedisSession redisSession = session.getAttachment();
        CompletableFuture<RESP> future = redisSession.getFuture();
        future.complete(msg);
    }

    /**
     * 处理会话状态事件
     * <p>
     * 监听并处理会话生命周期中的各种事件，例如新建会话、连接断开等。
     * </p>
     *
     * @param session          当前会话对象
     * @param stateMachineEnum 状态机事件类型
     * @param throwable        异常信息（如果有）
     */
    @Override
    public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        switch (stateMachineEnum) {
            // 处理新建会话事件
            case NEW_SESSION:
                // 为新会话创建并绑定Redis会话对象
                RedisSession redisSession = new RedisSession();
                session.setAttachment(redisSession);
                break;
        }

        // 如果有异常发生，打印异常堆栈信息
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
}