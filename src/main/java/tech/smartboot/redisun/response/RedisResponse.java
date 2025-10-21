package tech.smartboot.redisun.response;

import tech.smartboot.redisun.RedisunException;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version v1.0 10/21/25
 */
public abstract class RedisResponse {
    public static final byte CR = '\r';
    public static final byte LF = '\n';
    public static final byte SP = ' ';
    public static final byte[] CRLF = new byte[]{CR, LF};
    public static final byte RESP_DATA_TYPE_STRING = '+';
    public static final byte RESP_DATA_TYPE_ERROR = '-';
    public static final byte RESP_DATA_TYPE_INTEGER = ':';
    public static final byte RESP_DATA_TYPE_BULK = '$';
    public static final byte RESP_DATA_TYPE_ARRAY = '*';
    public static final byte RESP_DATA_TYPE_NULL = '_';
    public static final byte RESP_DATA_TYPE_BOOLEAN = '#';
    public static final byte RESP_DATA_TYPE_DOUBLE = ',';
    public static final byte RESP_DATA_TYPE_BIG_NUMBER = '(';
    public static final byte RESP_DATA_TYPE_BULK_ERROR = '!';
    public static final byte RESP_DATA_TYPE_VERBATIM_STRING = '=';
    public static final byte RESP_DATA_TYPE_MAP = '%';
    public static final byte RESP_DATA_TYPE_SET = '~';
    public static final byte RESP_DATA_TYPE_ATTRIBUTE = '|';
    public static final byte RESP_DATA_TYPE_PUSH = '>';

    abstract public boolean decode(ByteBuffer readBuffer);

    protected int readInt(ByteBuffer readBuffer) {
        int v = 0;
        readBuffer.mark();
        while (readBuffer.hasRemaining()) {
            byte b = readBuffer.get();
            if (b >= '0' && b <= '9') {
                v = v * 10 + b - '0';
                continue;
            } else if (readBuffer.remaining() < 1) {//非完整包，正常退出
                readBuffer.reset();
                return -1;
            }
            if (b == '\r' && readBuffer.get() == '\n') {
                readBuffer.mark();
                return v;
            } else {
                throw new RedisunException("数据格式错误");
            }
        }
        return -1;
    }

    public static RedisResponse newInstance(byte type) {
        switch (type) {
            case RESP_DATA_TYPE_INTEGER:
                return new IntegerResponse();
            case RESP_DATA_TYPE_STRING:
                return new StringResponse();
            case RESP_DATA_TYPE_ARRAY:
                return new ArrayResponse();
            case RESP_DATA_TYPE_MAP:
                return new MapsResponse();
            case RESP_DATA_TYPE_BULK:
                return new BulkStringResponse();
            default:
                throw new RedisunException("数据格式错误:" + ((char) type));
        }
    }
}
