package tech.smartboot.redisun.resp;

import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;

/**
 * @author 三刀
 * @version v1.0 10/23/25
 */
public interface Serialization {
     void writeTo(WriteBuffer writeBuffer) throws IOException;
}
