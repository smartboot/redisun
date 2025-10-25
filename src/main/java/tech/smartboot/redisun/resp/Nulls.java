package tech.smartboot.redisun.resp;

import org.smartboot.socket.transport.WriteBuffer;
import tech.smartboot.redisun.RedisunException;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * RESP Nulls 类型实现
 * <p>
 * Nulls是RESP3协议中引入的新数据类型，用于表示空值。
 * 在RESP2中，空值通常用特殊字符串或空数组来表示，而RESP3引入了专门的Null类型。
 * <p>
 * Nulls的编码格式:
 * 1. 以'_'字符开头标识这是一个Null类型
 * 2. 紧接着是CRLF(\r\n)终止符
 * 3. 总共3个字节: "_\r\n"
 * <p>
 * 例如，当服务器需要返回一个空值时，会发送: "_\r\n"
 * <p>
 * 客户端接收到Nulls类型时，应该将其解析为编程语言中的null或对应的空值表示。
 *
 * @author 三刀
 * @version v1.0 10/21/25
 * @see <a href="https://redis.io/docs/latest/develop/reference/protocol-spec/#nulls">RESP Nulls Specification</a>
 */
public final class Nulls extends RESP<Void> {

    /**
     * 私有构造函数，防止外部直接实例化
     * 应该通过RESP.newInstance()方法创建实例
     */
    Nulls() {
    }

    /**
     * 解析字节缓冲区中的Nulls数据
     * <p>
     * Nulls类型的格式非常简单，只需要验证接下来的两个字节是CRLF即可。
     *
     * @param readBuffer 包含RESP数据的字节缓冲区
     * @return 如果解析完成返回true，否则返回false表示需要更多数据
     * @throws RedisunException 当数据格式错误时抛出异常
     */
    @Override
    public boolean decode(ByteBuffer readBuffer) {
        // 检查缓冲区中是否至少还有2个字节(CRLF)
        if (readBuffer.remaining() < 2) {
            return false;
        }

        // 验证接下来的两个字节是否为CRLF
        if (readBuffer.get() == CR && readBuffer.get() == LF) {
            // 解析成功，value保持为null
            return true;
        }

        // 格式错误，抛出异常
        throw new RedisunException("数据格式错误");
    }

    /**
     * 返回对象的字符串表示
     *
     * @return 对象的字符串表示
     */
    @Override
    public String toString() {
        return "Nulls{" +
                "value=" + getValue() +
                '}';
    }

    /**
     * 将Nulls对象写入到输出缓冲区
     * <p>
     * 注意：根据RESP协议，Nulls类型不应该被写入，因为它是服务器返回给客户端的特殊类型
     * 客户端不应该发送Nulls类型给服务器，所以这里抛出异常。
     *
     * @param writeBuffer 输出缓冲区
     * @throws IOException      IO异常
     * @throws RedisunException 不应该尝试写入Nulls类型时抛出异常
     */
    @Override
    public void writeTo(WriteBuffer writeBuffer) throws IOException {
        throw new RedisunException("数据格式错误");
    }
}