package tech.smartboot.redisun.response;

import tech.smartboot.redisun.RedisunException;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀
 * @version v1.0 10/21/25
 */
public class MapsResponse extends RedisResponse<Map<RedisResponse, RedisResponse>> {
    private static final byte DECODE_STATE_INIT = 0;
    private static final byte DECODE_STATE_KEY = 1;
    private static final byte DECODE_STATE_VALUE = 2;
    private static final byte DECODE_STATE_END = 3;
    private byte state = DECODE_STATE_INIT;
    private int count;
    private RedisResponse key;
    private RedisResponse val;

    @Override
    public boolean decode(ByteBuffer readBuffer) {
        while (readBuffer.hasRemaining()) {
            switch (state) {
                case DECODE_STATE_INIT:
                    count = readInt(readBuffer);
                    if (count == 0) {
                        state = DECODE_STATE_END;
                        value = Collections.emptyMap();
                        return true;
                    } else if (count > 0) {
                        value = new HashMap<>(count);
                        state = DECODE_STATE_KEY;
                    } else {
                        return false;
                    }
                    break;
                case DECODE_STATE_KEY:
                    if (key == null) {
                        key = RedisResponse.newInstance(readBuffer.get());
                    } else if (key.decode(readBuffer)) {
                        state = DECODE_STATE_VALUE;
                    }
                    break;
                case DECODE_STATE_VALUE:
                    if (val == null) {
                        val = RedisResponse.newInstance(readBuffer.get());
                    } else if (val.decode(readBuffer)) {
                        value.put(key, val);
                        key = null;
                        val = null;
                        count--;
                        if (count == 0) {
                            state = DECODE_STATE_END;
                            return true;
                        } else {
                            state = DECODE_STATE_KEY;
                        }
                    }
                    break;
                default:
                    throw new RedisunException("数据格式错误");
            }
        }
        return false;
    }
}
