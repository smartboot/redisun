package tech.smartboot.redisun;

import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * @author 三刀
 * @version v1.0 10/21/25
 */
abstract class Command {
    public static final Consumer<RedisSession> HELLO_COMMAND=new Consumer<RedisSession>() {
        @Override
        public void accept(RedisSession redisSession) {

        }
    };
    protected static final byte[] CRLF = "\r\n".getBytes();
    protected final Redisun redisun;

    public Command(Redisun redisun) {
        this.redisun = redisun;
    }

    abstract void writeTo(WriteBuffer writeBuffer) throws IOException;
}
