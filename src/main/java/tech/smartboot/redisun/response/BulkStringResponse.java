package tech.smartboot.redisun.response;

import tech.smartboot.redisun.RedisunException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version v1.0 10/21/25
 */
public class BulkStringResponse extends RedisResponse {
    private static final byte DECODE_STATE_INIT = 0;
    private static final byte DECODE_STATE_VALUE = 1;
    private static final byte DECODE_STATE_END = 2;
    private byte state = DECODE_STATE_INIT;
    private int length;
    private ByteArrayOutputStream value;

    @Override
    public boolean decode(ByteBuffer readBuffer) {
        while (readBuffer.hasRemaining()) {
            switch (state) {
                case DECODE_STATE_INIT:
                    length = readInt(readBuffer);
                    if (length == 0) {
                        state = DECODE_STATE_END;
                        value = new ByteArrayOutputStream(0);
                    } else if (length > 0) {
                        value = new ByteArrayOutputStream(length);
                        state = DECODE_STATE_VALUE;
                    } else {
                        return false;
                    }
                    break;
                case DECODE_STATE_VALUE:
                    int size = Math.min(readBuffer.remaining(), length - value.size());
                    byte[] bytes = new byte[size];
                    readBuffer.get(bytes);
                    try {
                        value.write(bytes);
                    } catch (IOException e) {
                        throw new RedisunException(e);
                    }
                    if (value.size() == length) {
                        state = DECODE_STATE_END;
                    }
                    break;
                case DECODE_STATE_END:
                    if (readBuffer.remaining() >= 2) {
                        if (readBuffer.get() == CR && readBuffer.get() == LF) {
                            return true;
                        }
                        throw new RedisunException("数据格式错误");
                    }
                    break;
                default:
                    throw new RedisunException("数据格式错误");
            }
        }
        return false;
    }

    public ByteArrayOutputStream getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "BulkStringResponse{" +
                "value=" + value +
                '}';
    }
}
