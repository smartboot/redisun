package tech.smartboot.redisun.resp;

import org.smartboot.socket.transport.WriteBuffer;
import tech.smartboot.redisun.RedisunException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * RESP Maps 类型实现
 * <p>
 * Maps是RESP3协议中引入的新数据类型，用于表示键值对的集合。
 * 它是RESP2中通过特殊格式数组表示字典的替代方案，提供了更直观的表示方式。
 * <p>
 * Maps的编码格式:
 * 1. 以'%'字符开头标识这是一个Map类型
 * 2. 紧接着是键值对个数（十进制数字）
 * 3. 然后是CRLF(\r\n)终止符
 * 4. 接下来交替出现键和值的RESP编码表示
 * <p>
 * 例如，一个包含键值对{"key" => "value"}的Map会被编码为:
 * "%1\r\n$3\r\nkey\r\n$5\r\nvalue\r\n"
 * <p>
 * 空Map表示为"%0\r\n"
 * <p>
 * Maps类型在Redis命令返回复杂结构数据时非常有用，比如HELLO命令的返回值。
 *
 * @author 三刀
 * @version v1.0 10/21/25
 * @see <a href="https://redis.io/docs/latest/develop/reference/protocol-spec/#maps">RESP Maps Specification</a>
 */
public final class Maps extends RESP<Map<RESP, RESP>> {
    // 解析状态常量
    private static final byte DECODE_STATE_INIT = 0;   // 初始化状态，读取键值对个数
    private static final byte DECODE_STATE_KEY = 1;    // 读取键状态
    private static final byte DECODE_STATE_VALUE = 2;  // 读取值状态
    private static final byte DECODE_STATE_END = 3;    // 解析完成状态

    // 当前解析状态
    private byte state = DECODE_STATE_INIT;

    // 键值对个数
    private int count;

    // 当前正在解析的键
    private RESP key;

    // 当前正在解析的值
    private RESP val;

    /**
     * 私有构造函数，防止外部直接实例化
     * 应该通过RESP.newInstance()方法创建实例
     */
    Maps() {
    }

    /**
     * 解析字节缓冲区中的Maps数据
     * <p>
     * 解析过程分为四个阶段:
     * 1. 读取键值对个数
     * 2. 读取键
     * 3. 读取值
     * 4. 解析完成
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
                    // 读取键值对个数
                    count = readInt(readBuffer);
                    if (count == 0) {
                        // 空Map
                        state = DECODE_STATE_END;
                        value = Collections.emptyMap();
                        return true;
                    } else if (count > 0) {
                        // 初始化Map容器
                        value = new HashMap<>(count);
                        state = DECODE_STATE_KEY;
                        break;
                    } else {
                        return false;
                    }
                case DECODE_STATE_KEY:
                    // 解析键
                    if (key == null) {
                        // 创建新的键对象
                        key = RESP.newInstance(readBuffer);
                    } else if (key.decode(readBuffer)) {
                        // 键解析完成，进入值解析状态
                        state = DECODE_STATE_VALUE;
                    } else {
                        return false;
                    }
                    break;
                case DECODE_STATE_VALUE:
                    // 解析值
                    if (val == null) {
                        // 创建新的值对象
                        val = RESP.newInstance(readBuffer);
                    } else if (val.decode(readBuffer)) {
                        // 值解析完成，将键值对添加到Map中
                        value.put(key, val);
                        key = null;
                        val = null;
                        count--;
                        if (count == 0) {
                            // 所有键值对解析完成
                            state = DECODE_STATE_END;
                            return true;
                        } else {
                            // 继续解析下一个键
                            state = DECODE_STATE_KEY;
                        }
                    } else {
                        return false;
                    }
                    break;
                default:
                    throw new RedisunException("数据格式错误");
            }
        }
        return false;
    }

    /**
     * 将Maps对象写入到输出缓冲区
     *
     * @param writeBuffer 输出缓冲区
     * @throws IOException IO异常
     */
    @Override
    public void writeTo(WriteBuffer writeBuffer) throws IOException {
        // 写入Map类型标识符
        writeBuffer.write(RESP_DATA_TYPE_MAP);
        // 写入键值对个数
        writeInt(writeBuffer, value.size());
        // 逐个写入键值对
        for (Map.Entry<RESP, RESP> entry : value.entrySet()) {
            entry.getKey().writeTo(writeBuffer);
            entry.getValue().writeTo(writeBuffer);
        }
    }
}