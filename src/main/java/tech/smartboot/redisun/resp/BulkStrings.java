package tech.smartboot.redisun.resp;

import org.smartboot.socket.transport.WriteBuffer;
import tech.smartboot.redisun.RedisunException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * RESP Bulk Strings 类型实现
 * <p>
 * Bulk Strings是RESP协议中的二进制安全字符串类型，可以包含任意二进制数据。
 * 它是Redis中最常用的字符串表示方式，用于表示命令参数和大多数返回值。
 * <p>
 * Bulk Strings的编码格式:
 * 1. 以'$'字符开头标识这是一个Bulk String类型
 * 2. 紧接着是字符串长度（十进制数字）
 * 3. 然后是CRLF(\r\n)终止符
 * 4. 接下来是实际的字符串数据
 * 5. 最后是CRLF(\r\n)终止符
 * <p>
 * 例如，字符串"hello"会被编码为: "$5\r\nhello\r\n"
 * 空字符串表示为: "$0\r\n\r\n"
 * <p>
 * 在RESP3中，Bulk Strings还可以用于表示二进制数据、图片等。
 *
 * @author 三刀
 * @version v1.0 10/21/25
 * @see <a href="https://redis.io/docs/latest/develop/reference/protocol-spec/#bulk-strings">RESP Bulk Strings Specification</a>
 */
public class BulkStrings extends RESP<String> {
    // 解析状态常量
    private static final byte DECODE_STATE_INIT = 0;   // 初始化状态，读取字符串长度
    private static final byte DECODE_STATE_VALUE = 1;  // 读取字符串数据状态
    private static final byte DECODE_STATE_END = 2;    // 解析完成状态

    // 当前解析状态
    private byte state = DECODE_STATE_INIT;

    // 字符串长度
    private int length;

    // 用于存储字符串数据的输出流
    private ByteArrayOutputStream out;

    /**
     * 私有构造函数，防止外部直接实例化
     * 应该通过RESP.newInstance()方法创建实例
     */
    BulkStrings() {
    }

    /**
     * 解析字节缓冲区中的BulkStrings数据
     * <p>
     * 解析过程分为三个阶段:
     * 1. 读取字符串长度
     * 2. 读取字符串数据
     * 3. 验证结束符
     *
     * @param readBuffer 包含RESP数据的字节缓冲区
     * @return 如果解析完成返回true，否则返回false表示需要更多数据
     * @throws RedisunException 当数据格式错误时抛出异常
     */
    @Override
    public boolean decode(ByteBuffer readBuffer) {
        while (readBuffer.hasRemaining()) {
            switch (state) {
                case DECODE_STATE_INIT:
                    // 读取字符串长度
                    length = readInt(readBuffer);
                    if (length == 0) {
                        // 空字符串
                        state = DECODE_STATE_END;
                        out = new ByteArrayOutputStream(0);
                    } else if (length > 0) {
                        // 初始化输出流以存储字符串数据
                        out = new ByteArrayOutputStream(length);
                        state = DECODE_STATE_VALUE;
                    } else {
                        // 负数长度是非法的（除了-1，但那是Null的表示方法，应该使用Nulls类）
                        return false;
                    }
                    break;
                case DECODE_STATE_VALUE:
                    // 读取字符串数据
                    int size = Math.min(readBuffer.remaining(), length - out.size());
                    byte[] bytes = new byte[size];
                    readBuffer.get(bytes);
                    try {
                        out.write(bytes);
                    } catch (IOException e) {
                        throw new RedisunException(e);
                    }
                    if (out.size() == length) {
                        // 字符串数据读取完成
                        state = DECODE_STATE_END;
                    }
                    break;
                case DECODE_STATE_END:
                    // 验证结束符
                    if (readBuffer.remaining() >= 2) {
                        if (readBuffer.get() == CR && readBuffer.get() == LF) {
                            // 解析完成，设置字符串值
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

    /**
     * 将BulkStrings对象写入到输出缓冲区
     *
     * @param writeBuffer 输出缓冲区
     * @throws IOException IO异常
     */
    @Override
    public void writeTo(WriteBuffer writeBuffer) throws IOException {
        // 获取字符串的字节表示
        byte[] bytes = value.getBytes();
        // 写入Bulk String类型标识符
        writeBuffer.write(RESP_DATA_TYPE_BULK);
        // 写入字符串长度
        writeInt(writeBuffer, bytes.length);
        // 写入行终止符
        writeBuffer.write(CRLF);
        // 写入字符串数据
        writeBuffer.write(bytes);
        // 写入行终止符
        writeBuffer.write(CRLF);
    }

    public static BulkStrings of(String data) {
        // 获取字符串的字节表示
        byte[] bytes = data.getBytes();
        byte[] length = String.valueOf(bytes.length).getBytes();
        byte[] result = new byte[length.length + bytes.length + 5];
        result[0] = RESP.RESP_DATA_TYPE_BULK;
        System.arraycopy(length, 0, result, 1, length.length);
        System.arraycopy(RESP.CRLF, 0, result, length.length + 1, 2);
        System.arraycopy(bytes, 0, result, length.length + 3, bytes.length);
        System.arraycopy(RESP.CRLF, 0, result, length.length + 3 + bytes.length, 2);
        return new BulkStrings() {
            @Override
            public void writeTo(WriteBuffer writeBuffer) throws IOException {
                writeBuffer.write(result);
            }
        };
    }
}