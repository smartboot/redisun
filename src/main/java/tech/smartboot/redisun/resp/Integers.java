package tech.smartboot.redisun.resp;

import org.smartboot.socket.transport.WriteBuffer;
import tech.smartboot.redisun.RedisunException;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * RESP Integers 类型实现
 * <p>
 * Integers是RESP协议中的整数类型，用于表示有符号的64位整数。
 * 它通常用于表示命令的返回值，如列表长度、自增计数器等。
 * <p>
 * Integers的编码格式:
 * 1. 以':'字符开头标识这是一个整数类型
 * 2. 紧接着是可选的符号位（'+'或'-'）
 * 3. 然后是整数的十进制表示
 * 4. 最后是CRLF(\r\n)终止符
 * <p>
 * 例如，整数1000会被编码为: ":1000\r\n"
 * 负整数-1会被编码为: ":-1\r\n"
 * 零表示为: ":0\r\n"
 * <p>
 * 在RESP3中，整数类型还可以用于表示布尔值（0表示false，1表示true）。
 *
 * @author 三刀
 * @version v1.0 10/21/25
 * @see <a href="https://redis.io/docs/latest/develop/reference/protocol-spec/#integers">RESP Integers Specification</a>
 */
public final class Integers extends RESP<Integer> {
    // 解析状态常量
    private static final byte DECODE_STATE_INIT = 0;   // 初始化状态，读取符号位
    private static final byte DECODE_STATE_VALUE = 1;  // 读取数值状态
    private static final byte DECODE_STATE_END = 2;    // 解析完成状态

    // 当前解析状态
    private byte state = DECODE_STATE_INIT;

    // 是否为负数
    private boolean isNegative;

    /**
     * 私有构造函数，防止外部直接实例化
     * 应该通过RESP.newInstance()方法创建实例
     */
    Integers() {
    }

    /**
     * 解析字节缓冲区中的Integers数据
     * <p>
     * 解析过程分为两个阶段:
     * 1. 读取符号位（可选）
     * 2. 读取数值
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
                    // 读取第一个字节，判断是否有符号位
                    byte b = readBuffer.get();
                    if (b == '-') {
                        isNegative = true;
                    } else if (b == '+') {
                        isNegative = false;
                    } else if (b >= '0' && b <= '9') {
                        // 没有符号位，回退一个字节
                        readBuffer.position(readBuffer.position() - 1);
                    } else {
                        throw new RedisunException("数据格式错误");
                    }
                    state = DECODE_STATE_VALUE;
                    break;
                case DECODE_STATE_VALUE:
                    // 读取数值部分
                    int v = readInt(readBuffer);
                    if (v >= 0) {
                        state = DECODE_STATE_END;
                        value = isNegative ? -v : v;
                        return true;
                    }
                    return false;
                default:
                    throw new RedisunException("数据格式错误");
            }
        }
        return false;
    }

    /**
     * 返回对象的字符串表示
     *
     * @return 对象的字符串表示
     */
    @Override
    public String toString() {
        return "IntegerResponse{" +
                "value=" + getValue() +
                '}';
    }

    /**
     * 将Integers对象写入到输出缓冲区
     *
     * @param writeBuffer 输出缓冲区
     * @throws IOException IO异常
     */
    @Override
    public void writeTo(WriteBuffer writeBuffer) throws IOException {
        // 写入整数类型标识符
        writeBuffer.write(RESP_DATA_TYPE_INTEGER);
        writeInt(writeBuffer,value);
//        // 处理负数情况
//        if (value < 0) {
//            writeBuffer.write('-');
//            value = -value;
//        }
//        // 写入数值
//        writeBuffer.write(String.valueOf(value).getBytes());
        // 写入行终止符
        writeBuffer.write(CRLF);
    }
}