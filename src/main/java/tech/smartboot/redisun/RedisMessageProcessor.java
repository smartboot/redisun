package tech.smartboot.redisun;

import org.smartboot.socket.Protocol;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioSession;
import tech.smartboot.redisun.response.RedisResponse;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version v1.0 10/21/25
 */
public class RedisMessageProcessor extends AbstractMessageProcessor<RedisResponse> implements Protocol<RedisResponse> {
    @Override
    public RedisResponse decode(ByteBuffer readBuffer, AioSession session) {
        if (readBuffer.remaining() < 2) {
            return null;
        }
        RedisSession redisSession = session.getAttachment();
        RedisResponse redisResponse = redisSession.getDecodingResponse();
        if (redisResponse == null) {
            redisResponse = RedisResponse.newInstance(readBuffer.get());
            redisSession.setDecodingResponse(redisResponse);
        }
        if (redisResponse.decode(readBuffer)) {
            redisSession.setDecodingResponse(null);
            return redisResponse;
        }
        return null;
    }

    @Override
    public void process0(AioSession session, RedisResponse msg) {
        System.out.println(msg);
        RedisSession redisSession = session.getAttachment();
        redisSession.getFuture().complete(msg);
    }

    @Override
    public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        switch (stateMachineEnum) {
            case NEW_SESSION:
                RedisSession redisSession = new RedisSession(session);
                session.setAttachment(redisSession);
                break;
        }
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
}
