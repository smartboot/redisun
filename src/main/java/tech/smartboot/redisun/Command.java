package tech.smartboot.redisun;

import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.WriteBuffer;
import tech.smartboot.redisun.response.RedisResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * @author 三刀
 * @version v1.0 10/21/25
 */
abstract class Command {
    private static final byte[] CMD = "HELLO 3 ".getBytes();
    protected static final byte[] CRLF = "\r\n".getBytes();

    protected abstract void writeTo(WriteBuffer writeBuffer) throws IOException;


}
