package tech.smartboot.redisun.resp;

import org.smartboot.socket.transport.WriteBuffer;
import tech.smartboot.redisun.RedisunException;

/**
 * RESP Simple Errors 类型实现
 * <p>
 * Simple Errors是RESP协议中的错误类型，用于表示命令执行过程中发生的错误。
 * 它与Simple Strings类似，但语义不同，表示错误信息而不是正常响应。
 * <p>
 * Simple Errors的编码格式:
 * 1. 以'-'字符开头标识这是一个错误类型
 * 2. 紧接着是错误信息内容（不能包含CR或LF）
 * 3. 最后是CRLF(\r\n)终止符
 * <p>
 * 例如，未知命令错误会被编码为: "-ERR unknown command 'asdf'\r\n"
 * 类型错误会被编码为: "-WRONGTYPE Operation against a key holding the wrong kind of value\r\n"
 * <p>
 * 错误信息通常以错误类型前缀开头，如"ERR"、"WRONGTYPE"等，便于客户端识别错误类型。
 * <p>
 * 注意：当前实现中，SimpleErrors继承自SimpleStrings，但禁止写入操作，
 * 因为错误类型通常由服务器返回给客户端，而不是由客户端发送给服务器。
 *
 * @author 三刀
 * @version v1.0 10/22/25
 * @see <a href="https://redis.io/docs/latest/develop/reference/protocol-spec/#simple-errors">RESP Simple Errors Specification</a>
 */
public final class SimpleErrors extends SimpleStrings {

    /**
     * 私有构造函数，防止外部直接实例化
     * 应该通过RESP.newInstance()方法创建实例
     */
    SimpleErrors() {
    }

    /**
     * 将SimpleErrors对象写入到输出缓冲区
     * <p>
     * 当前实现中禁止写入SimpleErrors对象，因为错误类型通常由服务器返回给客户端，
     * 而不是由客户端发送给服务器。
     *
     * @param writeBuffer 输出缓冲区
     * @throws RedisunException 不应该尝试写入SimpleErrors类型时抛出异常
     */
    @Override
    public void writeTo(WriteBuffer writeBuffer) {
        throw new RedisunException("not support");
    }
}