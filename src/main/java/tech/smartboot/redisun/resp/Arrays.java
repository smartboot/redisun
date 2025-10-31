package tech.smartboot.redisun.resp;

import org.smartboot.socket.transport.WriteBuffer;
import tech.smartboot.redisun.RedisunException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RESP Arrays 类型实现
 * <p>
 * Arrays是RESP协议中的聚合类型，用于表示元素的有序集合。
 * 在RESP2中，Arrays是唯一可以表示复杂数据结构的类型。
 * 在RESP3中，Arrays与Maps、Sets等类型共同构成了丰富的数据结构表示能力。
 * <p>
 * Arrays的编码格式:
 * 1. 以'*'字符开头标识这是一个数组类型
 * 2. 紧接着是数组元素个数（十进制数字）
 * 3. 然后是CRLF(\r\n)终止符
 * 4. 接下来是每个元素的RESP编码表示
 * <p>
 * 例如，一个包含3个元素的数组["foo", "bar", "Hello"]会被编码为:
 * "*3\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$5\r\nHello\r\n"
 * <p>
 * 空数组表示为"*0\r\n"
 *
 * @author 三刀
 * @version v1.0 10/21/25
 * @see <a href="https://redis.io/docs/latest/develop/reference/protocol-spec/#arrays">RESP Arrays Specification</a>
 */
public final class Arrays extends RESP<List<RESP>> {
    // 解析状态常量
    private static final byte DECODE_STATE_INIT = 0;   // 初始化状态，读取元素个数
    private static final byte DECODE_STATE_ITEM = 1;   // 读取数组元素状态
    private static final byte DECODE_STATE_END = 2;    // 解析完成状态

    // 当前解析状态
    private byte state = DECODE_STATE_INIT;

    // 数组元素个数
    private int count;

    // 当前正在解析的元素
    private RESP item;

    /**
     * 私有构造函数，防止外部直接实例化
     * 应该通过RESP.newInstance()方法创建实例
     */
    Arrays() {
    }

    /**
     * 解析字节缓冲区中的Arrays数据
     * <p>
     * 解析过程分为三个阶段:
     * 1. 读取数组元素个数
     * 2. 逐个解析数组元素
     * 3. 解析完成
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
                    // 读取数组元素个数
                    count = readInt(readBuffer);
                    if (count == 0) {
                        // 空数组
                        state = DECODE_STATE_END;
                        value = Collections.emptyList();
                        return true;
                    } else if (count > 0) {
                        // 初始化数组容器
                        value = new ArrayList<>(count);
                        state = DECODE_STATE_ITEM;
                    } else {
                        return false;
                    }
                    break;
                case DECODE_STATE_ITEM:
                    // 解析数组元素
                    if (item == null) {
                        // 创建新的元素对象
                        item = RESP.newInstance(readBuffer);
                    } else if (item.decode(readBuffer)) {
                        // 元素解析完成，添加到数组中
                        value.add(item);
                        item = null;
                        count--;
                        if (count == 0) {
                            // 所有元素解析完成
                            state = DECODE_STATE_END;
                            return true;
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
     * 将Arrays对象写入到输出缓冲区
     *
     * @param writeBuffer 输出缓冲区
     * @throws IOException IO异常
     */
    @Override
    public void writeTo(WriteBuffer writeBuffer) throws IOException {
        // 写入数组类型标识符
        writeBuffer.write(RESP_DATA_TYPE_ARRAY);
        // 写入数组元素个数
        writeInt(writeBuffer, value.size());
        // 逐个写入数组元素
        for (RESP item : value) {
            item.writeTo(writeBuffer);
        }
    }
}