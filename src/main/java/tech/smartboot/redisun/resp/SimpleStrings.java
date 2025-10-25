package tech.smartboot.redisun.resp;

import org.smartboot.socket.transport.WriteBuffer;
import tech.smartboot.redisun.RedisunException;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * RESP Simple Strings 类型实现
 * <p>
 * Simple Strings是RESP协议中最基本的字符串类型，用于表示非二进制安全的简单文本。
 * 它不能包含CR或LF字符，通常用于表示状态信息，如"OK"。
 * <p>
 * Simple Strings的编码格式:
 * 1. 以'+'字符开头标识这是一个简单字符串类型
 * 2. 紧接着是字符串内容（不能包含CR或LF）
 * 3. 最后是CRLF(\r\n)终止符
 * <p>
 * 例如，字符串"OK"会被编码为: "+OK\r\n"
 * 空的简单字符串表示为: "+\r\n"
 * <p>
 * Simple Strings主要用于表示状态信息，如命令执行成功时返回的"OK"。
 * 对于需要包含二进制数据或特殊字符的字符串，应该使用Bulk Strings类型。
 *
 * @author 三刀
 * @version v1.0 10/21/25
 * @see <a href="https://redis.io/docs/latest/develop/reference/protocol-spec/#simple-strings">RESP Simple Strings Specification</a>
 */
public class SimpleStrings extends RESP<String> {
    public static final String OK = "OK";

    /**
     * 受保护的构造函数，允许子类访问
     * 应该通过RESP.newInstance()方法创建实例
     */
    SimpleStrings() {
    }

    /**
     * 解析字节缓冲区中的SimpleStrings数据
     * <p>
     * 解析过程相对简单，只需要找到行终止符并提取中间的字符串内容。
     *
     * @param readBuffer 包含RESP数据的字节缓冲区
     * @return 如果解析完成返回true，否则返回false表示需要更多数据
     * @throws RedisunException 当数据格式错误时抛出异常
     */
    @Override
    public boolean decode(ByteBuffer readBuffer) {
        int start = readBuffer.position();
        readBuffer.mark();
        // 查找行结束符 \r\n
        while (readBuffer.hasRemaining()) {
            byte b = readBuffer.get();
            if (b != LF) {
                continue;
            }
            // 验证前一个字符是否为CR
            if (readBuffer.get(readBuffer.position() - 2) != CR) {
                throw new RedisunException("invalid response");
            }
            // 提取字符串内容（不包括行终止符）
            byte[] bytes = new byte[readBuffer.position() - start - 2];
            readBuffer.position(start);
            readBuffer.get(bytes);
            value = new String(bytes);
            // 跳过行终止符
            readBuffer.position(readBuffer.position() + 2);
            return true;
        }
        readBuffer.reset();
        return false;
    }

    /**
     * 返回对象的字符串表示
     *
     * @return 对象的字符串表示
     */
    @Override
    public String toString() {
        return "StringResponse{" + "value='" + value + '\'' + '}';
    }

    /**
     * 将SimpleStrings对象写入到输出缓冲区
     *
     * @param writeBuffer 输出缓冲区
     * @throws IOException IO异常
     */
    @Override
    public void writeTo(WriteBuffer writeBuffer) throws IOException {
        // 写入简单字符串类型标识符
        writeBuffer.write(RESP_DATA_TYPE_STRING);
        // 写入字符串内容
        writeBuffer.write(value.getBytes());
        // 写入行终止符
        writeBuffer.write(CRLF);
    }
}