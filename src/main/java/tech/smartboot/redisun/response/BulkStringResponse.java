package tech.smartboot.redisun.response;

import tech.smartboot.redisun.RedisunException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version v1.0 10/21/25
 */
public class BulkStringResponse extends RedisResponse<String> {
    private static final byte DECODE_STATE_INIT = 0;
    private static final byte DECODE_STATE_VALUE = 1;
    private static final byte DECODE_STATE_END = 2;
    private byte state = DECODE_STATE_INIT;
    private int length;
    private ByteArrayOutputStream out;

    @Override
    public boolean decode(ByteBuffer readBuffer) {
        while (readBuffer.hasRemaining()) {
            switch (state) {
                case DECODE_STATE_INIT:
                    length = readInt(readBuffer);
                    if (length == 0) {
                        state = DECODE_STATE_END;
                        out = new ByteArrayOutputStream(0);
                    } else if (length > 0) {
                        out = new ByteArrayOutputStream(length);
                        state = DECODE_STATE_VALUE;
                    } else {
                        return false;
                    }
                    break;
                case DECODE_STATE_VALUE:
                    int size = Math.min(readBuffer.remaining(), length - out.size());
                    byte[] bytes = new byte[size];
                    readBuffer.get(bytes);
                    try {
                        out.write(bytes);
                    } catch (IOException e) {
                        throw new RedisunException(e);
                    }
                    if (out.size() == length) {
                        state = DECODE_STATE_END;
                    }
                    break;
                case DECODE_STATE_END:
                    if (readBuffer.remaining() >= 2) {
                        if (readBuffer.get() == CR && readBuffer.get() == LF) {
                            value = out.toString();
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

}
