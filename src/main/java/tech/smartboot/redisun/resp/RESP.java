package tech.smartboot.redisun.resp;

import org.smartboot.socket.transport.WriteBuffer;
import tech.smartboot.redisun.RedisunException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Redis Serialization Protocol (RESP)
 * Redis序列化协议实现的基础抽象类
 * <p>
 * RESP是Redis客户端与服务器之间通信的协议。它支持多种数据类型:
 * 1. 简单类型: 简单字符串、整数、错误、空值、布尔值、双精度浮点数、大数字
 * 2. 聚合类型: 数组、映射、集合、属性、推送消息
 * 3. 批量类型: 批量字符串、批量错误、逐字字符串
 * <p>
 * 协议特点:
 * - 简单易实现
 * - 快速解析
 * - 人类可读
 * <p>
 * RESP协议通过第一个字节标识数据类型，后续字节为具体数据内容。
 * 所有类型都以\r\n(CRLF)作为终止符。
 *
 * @param <T> 响应值的类型
 * @author 三刀
 * @version v1.0 10/21/25
 * @see <a href="https://redis.io/docs/latest/develop/reference/protocol-spec/">Redis Protocol Specification</a>
 */
public abstract class RESP<T> implements Serialization {
    // 协议控制字符
    public static final byte CR = '\r';  // 回车符
    public static final byte LF = '\n';  // 换行符
    public static final byte SP = ' ';   // 空格符
    public static final byte[] CRLF = new byte[]{CR, LF}; // 行终止符

    // RESP数据类型标识符
    // RESP2数据类型
    public static final byte RESP_DATA_TYPE_STRING = '+';        // 简单字符串
    public static final byte RESP_DATA_TYPE_ERROR = '-';         // 简单错误
    public static final byte RESP_DATA_TYPE_INTEGER = ':';       // 整数
    public static final byte RESP_DATA_TYPE_BULK = '$';          // 批量字符串
    public static final byte RESP_DATA_TYPE_ARRAY = '*';         // 数组

    // RESP3新增数据类型
    public static final byte RESP_DATA_TYPE_NULL = '_';          // 空值 (RESP3)
    public static final byte RESP_DATA_TYPE_BOOLEAN = '#';       // 布尔值 (RESP3)
    public static final byte RESP_DATA_TYPE_DOUBLE = ',';        // 双精度浮点数 (RESP3)
    public static final byte RESP_DATA_TYPE_BIG_NUMBER = '(';    // 大数字 (RESP3)
    public static final byte RESP_DATA_TYPE_BULK_ERROR = '!';    // 批量错误 (RESP3)
    public static final byte RESP_DATA_TYPE_VERBATIM_STRING = '='; // 逐字字符串 (RESP3)
    public static final byte RESP_DATA_TYPE_MAP = '%';           // 映射 (RESP3)
    public static final byte RESP_DATA_TYPE_SET = '~';           // 集合 (RESP3)
    public static final byte RESP_DATA_TYPE_ATTRIBUTE = '|';     // 属性 (RESP3)
    public static final byte RESP_DATA_TYPE_PUSH = '>';          // 推送消息 (RESP3)

    // 响应值
    protected T value;

    /**
     * 解析字节缓冲区中的RESP数据
     *
     * @param readBuffer 包含RESP数据的字节缓冲区
     * @return 如果解析完成返回true，否则返回false表示需要更多数据
     */
    abstract public boolean decode(ByteBuffer readBuffer);

    /**
     * 获取响应值
     *
     * @return 响应值
     */
    public final T getValue() {
        return value;
    }

    /**
     * 设置响应值
     *
     * @param value 要设置的响应值
     */
    public void setValue(T value) {
        this.value = value;
    }


    /**
     * 从字节缓冲区中读取整数
     *
     * @param readBuffer 字节缓冲区
     * @return 解析出的整数值，如果数据不完整返回-1
     * @throws RedisunException 当数据格式错误时抛出异常
     */
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

    protected void writeInt(WriteBuffer out, int value) throws IOException {
        // 处理特殊情况 0
        if (value == 0) {
            out.write('0');
            return;
        } else if (value == Integer.MIN_VALUE) {
            out.write(new byte[]{'-', '2', '1', '4', '7', '4', '8', '3', '6', '4', '8'});
            return;
        } else if (value < 0) {
            out.write('-');
            value = -value;
        }

        if (value < 10) {
            out.write('0' + value);
        } else if (value < 100) {
            out.write('0' + value / 10);
            out.write('0' + value % 10);
        } else if (value < 1000) {
            out.write('0' + value / 100);
            out.write('0' + value / 10 % 10);
            out.write('0' + value % 10);
        } else {
            // 用于存储转换后的数字字符
            byte[] buffer = new byte[10]; // 最大的 int 有 10 位
            int pos = 10;
            while (value != 0) {
                buffer[--pos] = (byte) ('0' + (value % 10));
                value /= 10;
            }
            out.write(buffer, pos, buffer.length - pos);
        }
    }

    /**
     * 根据数据类型创建对应的RESP对象实例
     *
     * @param type 数据类型标识符
     * @return 对应类型的RESP对象实例
     * @throws RedisunException 当不支持的数据类型时抛出异常
     */
    public static RESP newInstance(byte type) {
        switch (type) {
            case RESP_DATA_TYPE_INTEGER:
                return new Integers();
            case RESP_DATA_TYPE_STRING:
                return new SimpleStrings();
            case RESP_DATA_TYPE_ARRAY:
                return new Arrays();
            case RESP_DATA_TYPE_MAP:
                return new Maps();
            case RESP_DATA_TYPE_BULK:
                return new BulkStrings();
            case RESP_DATA_TYPE_ERROR:
                return new SimpleErrors();
            case RESP_DATA_TYPE_NULL:
                return new Nulls();
            default:
                throw new RedisunException("数据格式错误:" + ((char) type));
        }
    }

    /**
     * 创建包含指定字符串值的BulkStrings对象
     *
     * @param value 字符串值
     * @return BulkStrings对象
     */
    public static BulkStrings ofString(String value) {
        BulkStrings bulkStringResponse = new BulkStrings();
        bulkStringResponse.setValue(value);
        return bulkStringResponse;
    }

    /**
     * 创建包含指定RESP列表的Arrays对象
     *
     * @param list RESP对象列表
     * @return Arrays对象
     */
    public static RESP ofArray(List<RESP> list) {
        Arrays arrays = new Arrays();
        arrays.setValue(list);
        return arrays;
    }
}