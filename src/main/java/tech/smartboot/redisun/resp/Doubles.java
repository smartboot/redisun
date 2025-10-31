package tech.smartboot.redisun.resp;

import org.smartboot.socket.transport.WriteBuffer;
import tech.smartboot.redisun.RedisunException;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Doubles extends RESP<Double> {
    // 解析状态常量
    private static final byte DECODE_STATE_INIT = 0;   // 初始化状态，检查开头字符
    private static final byte DECODE_STATE_VALUE = 1;  // 读取数值状态
    private static final byte DECODE_STATE_END = 2;    // 解析完成状态

    public static Doubles of(ByteBuffer readBuffer) {
        return new Doubles();
    }

    // 当前解析状态
    private byte state = DECODE_STATE_VALUE;

    // 存储解析过程中的字符串值
    private StringBuilder valueBuilder = new StringBuilder();

    /**
     * 私有构造函数，防止外部直接实例化
     * 应该通过RESP.newInstance()方法创建实例
     */
    private Doubles() {
    }

    /**
     * 解析字节缓冲区中的Doubles数据
     * <p>
     * 解析过程分为几个阶段:
     * 1. 检查开头的','字符
     * 2. 读取数值部分
     * 3. 遇到CRLF结束符表示解析完成
     *
     * @param readBuffer 包含RESP数据的字节缓冲区
     * @return 如果解析完成返回true，否则返回false表示需要更多数据
     * @throws RedisunException 当数据格式错误时抛出异常
     */
    @Override
    public boolean decode(ByteBuffer readBuffer) {
        while (readBuffer.hasRemaining()) {
            byte b = readBuffer.get();
            
            switch (state) {
                case DECODE_STATE_INIT:
                    if (b != ',') {
                        throw new RedisunException("Invalid double format: missing ',' prefix");
                    }
                    state = DECODE_STATE_VALUE;
                    break;
                    
                case DECODE_STATE_VALUE:
                    if (b == CR) {
                        // 准备读取LF
                        state = DECODE_STATE_END;
                    } else {
                        // 构建数值字符串
                        valueBuilder.append((char) b);
                    }
                    break;
                    
                case DECODE_STATE_END:
                    if (b != LF) {
                        throw new RedisunException("Invalid double format: missing LF after CR");
                    }
                    // 解析double值
                    try {
                        value = Double.parseDouble(valueBuilder.toString());
                    } catch (NumberFormatException e) {
                        throw new RedisunException("Invalid double format: " + valueBuilder.toString());
                    }
                    return true;
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
        return "Doubles{value=" + value + '}';
    }

    /**
     * 将Doubles对象写入到输出缓冲区
     *
     * @param writeBuffer 输出缓冲区
     * @throws IOException IO异常
     */
    @Override
    public void writeTo(WriteBuffer writeBuffer) throws IOException {
        if (value == null) {
            throw new IOException("Double value is null");
        }
        
        // 写入Double类型标识符
        writeBuffer.write(RESP_DATA_TYPE_DOUBLE);
        // 写入数值
        writeBuffer.write(value.toString().getBytes());
        // 写入CRLF结束符
        writeBuffer.write(CRLF);
    }
}