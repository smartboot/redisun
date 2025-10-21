package tech.smartboot.redisun.response;

import tech.smartboot.redisun.RedisunException;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version v1.0 10/21/25
 */
public class IntegerResponse extends RedisResponse {
    private static final byte DECODE_STATE_INIT = 0;
    private static final byte DECODE_STATE_VALUE = 1;
    private static final byte DECODE_STATE_END = 2;
    private byte state = DECODE_STATE_INIT;
    private boolean isNegative;
    private int value;

    @Override
    public boolean decode(ByteBuffer readBuffer) {
        while (readBuffer.hasRemaining()) {
            switch (state) {
                case DECODE_STATE_INIT:
                    byte b = readBuffer.get();
                    if (b == '-') {
                        isNegative = true;
                    } else if (b == '+') {
                        isNegative = false;
                    } else if (b >= '0' && b <= '9') {
                        readBuffer.position(readBuffer.position() - 1);
                    } else {
                        throw new RedisunException("数据格式错误");
                    }
                    state = DECODE_STATE_VALUE;
                    break;
                case DECODE_STATE_VALUE:
                    value = readInt(readBuffer);
                    if (value >= 0) {
                        state = DECODE_STATE_END;
                        return true;
                    }
                    return false;
                default:
                    throw new RedisunException("数据格式错误");
            }
        }
        return false;
    }

    public int getValue() {
        return isNegative ? -value : value;
    }

    @Override
    public String toString() {
        return "IntegerResponse{" +
                "value=" + getValue() +
                '}';
    }
}
