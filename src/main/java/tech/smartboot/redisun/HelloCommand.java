package tech.smartboot.redisun;

import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;

/**
 * @author 三刀
 * @version v1.0 10/21/25
 */
final class HelloCommand extends Command {

    private static final byte[] CMD = "HELLO 3 ".getBytes();

    public HelloCommand(Redisun redisun) {
        super(redisun);
    }

    @Override
    void writeTo(WriteBuffer writeBuffer) throws IOException {
        writeBuffer.write(CMD);
        writeBuffer.write(CRLF);
        writeBuffer.flush();
    }
}
