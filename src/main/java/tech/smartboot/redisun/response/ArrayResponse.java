package tech.smartboot.redisun.response;

import tech.smartboot.redisun.RedisunException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author 三刀
 * @version v1.0 10/21/25
 */
public class ArrayResponse extends RedisResponse {
    private static final byte DECODE_STATE_INIT = 0;
    private static final byte DECODE_STATE_ITEM = 1;
    private static final byte DECODE_STATE_END = 2;
    private byte state = DECODE_STATE_INIT;
    private List<RedisResponse> arrays;
    private int count;
    private RedisResponse item;

    @Override
    public boolean decode(ByteBuffer readBuffer) {
        while (readBuffer.hasRemaining()) {
            switch (state) {
                case DECODE_STATE_INIT:
                    count = readInt(readBuffer);
                    if (count == 0) {
                        state = DECODE_STATE_END;
                        arrays = Collections.emptyList();
                        return true;
                    } else if (count > 0) {
                        arrays = new ArrayList<>(count);
                        state = DECODE_STATE_ITEM;
                    } else {
                        return false;
                    }
                    break;
                case DECODE_STATE_ITEM:
                    if (item == null) {
                        item = RedisResponse.newInstance(readBuffer.get());
                    } else if (item.decode(readBuffer)) {
                        arrays.add(item);
                        item = null;
                        count--;
                        if (count == 0) {
                            state = DECODE_STATE_END;
                            return true;
                        }
                    }
                    break;
                default:
                    throw new RedisunException("数据格式错误");
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "ArrayResponse{" +
                "arrays=" + arrays +
                '}';
    }
}
