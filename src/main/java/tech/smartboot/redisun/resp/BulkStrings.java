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
    private static final byte DECODE_STATE_SIMPLE_VALUE = 1;  // 读取较短字符串数据状态
    private static final byte DECODE_STATE_LONG_VALUE = 2;  // 读取较长字符串数据状态
    private static final byte DECODE_STATE_END = 3;    // 解析完成状态

    // 当前解析状态
    private byte state = DECODE_STATE_INIT;

    // 字符串长度，初始值为-1表示尚未读取长度
    private int length = -1;

    // 用于存储字符串数据的输出流，当字符串较长时使用
    private ByteArrayOutputStream out;

    // 空字符串对应的输出流实例，用于优化空字符串处理
    private final static ByteArrayOutputStream EMPTY_OUTPUT_STREAM = new ByteArrayOutputStream(0);

    /**
     * 私有构造函数，防止外部直接实例化
     * 应该通过RESP.newInstance()方法创建实例
     */
    BulkStrings() {
    }

    /**
     * 解析字节缓冲区中的BulkStrings数据
     * <p>
     * 解析过程分为四个阶段:
     * 1. DECODE_STATE_INIT: 读取字符串长度
     * 2. DECODE_STATE_SIMPLE_VALUE: 处理较短的字符串数据（可以直接从缓冲区读取）
     * 3. DECODE_STATE_LONG_VALUE: 处理较长的字符串数据（需要分批读取）
     * 4. DECODE_STATE_END: 验证结束符并完成解析
     *
     * @param readBuffer 包含RESP数据的字节缓冲区
     * @return 如果解析完成返回true，否则返回false表示需要更多数据
     * @throws RedisunException 当数据格式错误时抛出异常
     */
    @Override
    public boolean decode(ByteBuffer readBuffer) {
        switch (state) {
            case DECODE_STATE_INIT:
                // 读取字符串长度
                length = readInt(readBuffer);
                if (length > 0) {
                    // 正常字符串，判断是否可以一次性读取
                    if (length + 2 <= readBuffer.capacity()) {
                        // 字符串较短，可以直接读取
                        state = DECODE_STATE_SIMPLE_VALUE;
                    } else {
                        // 字符串较长，需要分批读取
                        out = new ByteArrayOutputStream(length);
                        state = DECODE_STATE_LONG_VALUE;
                        return decode(readBuffer);
                    }
                } else if (length == 0) {
                    // 空字符串情况
                    if (readBuffer.remaining() >= 2) {
                        // 缓冲区中有足够的数据验证结束符
                        if (readBuffer.getShort() == CRLF_VALUE) {
                            // 成功读取空字符串
                            return true;
                        }
                        throw new RedisunException("数据格式错误");
                    } else {
                        // 数据不足，等待更多数据
                        state = DECODE_STATE_END;
                        out = EMPTY_OUTPUT_STREAM;
                        return false;
                    }
                } else {
                    // length < 0 的情况，通常是NULL字符串(-1)，直接返回
                    return false;
                }
            case DECODE_STATE_SIMPLE_VALUE: {
                // 处理较短的字符串，可以直接从缓冲区完整读取
                int len = length + 2; // 字符串长度 + 结束符长度
                if (readBuffer.remaining() < len) {
                    // 缓冲区中数据不足，等待更多数据
                    return false;
                }
                byte[] bytes = new byte[len];
                readBuffer.get(bytes);
                // 验证结束符
                if (bytes[bytes.length - 2] == CR && bytes[bytes.length - 1] == LF) {
                    // 设置解析结果值
                    value = new String(bytes, 0, len - 2);
                    return true;
                }
                throw new RuntimeException("invalid simple string");
            }
            case DECODE_STATE_LONG_VALUE:
                // 处理较长的字符串，需要分批读取直到完成
                // 计算本次可以读取的字节数
                int size = Math.min(readBuffer.remaining(), length - out.size());
                byte[] bytes = new byte[size];
                readBuffer.get(bytes);
                try {
                    // 将读取的数据写入输出流
                    out.write(bytes);
                } catch (IOException e) {
                    throw new RedisunException(e);
                }
                if (out.size() == length) {
                    // 字符串数据读取完成，进入结束状态
                    state = DECODE_STATE_END;
                } else {
                    // 还未读取完所有数据，继续等待
                    break;
                }
            case DECODE_STATE_END:
                // 验证结束符
                if (readBuffer.remaining() >= 2) {
                    if (readBuffer.getShort() == CRLF_VALUE) {
                        // 解析完成，设置字符串值
                        if (out != EMPTY_OUTPUT_STREAM) {
                            value = out.toString();
                        }
                        return true;
                    }
                    throw new RedisunException("数据格式错误");
                } else {
                    // 结束符数据不足，等待更多数据
                    return false;
                }
            default:
                throw new RedisunException("数据格式错误");
        }
        return false;
    }

    /**
     * 将BulkStrings对象写入到输出缓冲区
     * <p>
     * 写入过程遵循RESP Bulk Strings格式:
     * 1. 写入Bulk String类型标识符 '$'
     * 2. 写入字符串长度
     * 3. 写入CRLF结束符
     * 4. 写入字符串数据
     * 5. 写入CRLF结束符
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
        // 写入字符串数据
        writeBuffer.write(bytes);
        // 写入CRLF结束符
        writeBuffer.write(CRLF);
    }

    /**
     * 创建一个包含指定数据的BulkStrings实例
     * <p>
     * 该方法提供了一种高效的方式来创建BulkStrings对象，
     * 通过预构建整个RESP格式的字节数组，避免了在写入时重复计算格式。
     *
     * @param data 要包装成BulkString的字符串数据
     * @return 包含指定数据的BulkStrings实例
     */
    public static BulkStrings of(String data) {
        // 获取字符串的字节表示
        byte[] bytes = data.getBytes();
        // 获取字符串长度的字节表示
        byte[] length = String.valueOf(bytes.length).getBytes();
        // 构建完整的RESP格式字节数组
        // 格式: $ + 长度 + CRLF + 数据 + CRLF
        byte[] result = new byte[length.length + bytes.length + 5]; // 5 = 1($符号) + 2(CRLF) + 2(CRLF)
        result[0] = RESP.RESP_DATA_TYPE_BULK;
        System.arraycopy(length, 0, result, 1, length.length);
        System.arraycopy(RESP.CRLF, 0, result, length.length + 1, 2);
        System.arraycopy(bytes, 0, result, length.length + 3, bytes.length);
        System.arraycopy(RESP.CRLF, 0, result, length.length + 3 + bytes.length, 2);

        // 返回一个匿名BulkStrings子类实例，重写了writeTo方法以提高性能
        return new BulkStrings() {
            @Override
            public void writeTo(WriteBuffer writeBuffer) throws IOException {
                // 直接写入预构建的完整RESP格式字节数组
                writeBuffer.write(result);
            }
        };
    }
}