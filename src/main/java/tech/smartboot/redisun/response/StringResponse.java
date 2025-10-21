package tech.smartboot.redisun.response;

import tech.smartboot.redisun.RedisunException;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version v1.0 10/21/25
 */
public class StringResponse extends RedisResponse {
    @Override
    public boolean decode(ByteBuffer readBuffer) {
        int start = readBuffer.position();
        readBuffer.mark();
        // 查找行结束符 \r\n
        while (readBuffer.hasRemaining()) {
            byte b = readBuffer.get();
            if (b != LF) {
                continue;
            }
            if (readBuffer.get(readBuffer.position() - 1) != CR) {
                throw new RedisunException("invalid response");
            }
            byte[] bytes = new byte[readBuffer.position() - start - 2];
            readBuffer.position(start);
            readBuffer.get(bytes);
            value = new String(bytes);
            readBuffer.position(readBuffer.position() + 2);
            return true;
        }
        readBuffer.reset();
        return false;
    }


    @Override
    public String toString() {
        return "StringResponse{" +
                "value='" + value + '\'' +
                '}';
    }
}
