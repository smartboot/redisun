package tech.smartboot.redisun.cmd;

import io.github.smartboot.socket.transport.WriteBuffer;
import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.RESP;

import java.io.IOException;

/**
 * @author 三刀
 * @version v1.0 10/23/25
 */
public class GetCommand extends Command {
    private final byte[] key;
    private static final byte[] HEADER = new byte[]{RESP.RESP_DATA_TYPE_ARRAY, '2', '\r', '\n', RESP.RESP_DATA_TYPE_BULK, '3', '\r', '\n', 'G', 'E', 'T', '\r', '\n', RESP.RESP_DATA_TYPE_BULK};

    public GetCommand(String key) {
        this.key = key.getBytes();
    }

    @Override
    public void writeTo(WriteBuffer writeBuffer) throws IOException {
        writeBuffer.write(HEADER);
        RESP.writeInt(writeBuffer, key.length);
        writeBuffer.write(key);
        writeBuffer.write(RESP.CRLF);
    }
}
