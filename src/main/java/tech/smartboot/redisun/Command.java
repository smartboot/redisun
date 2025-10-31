package tech.smartboot.redisun;

import org.smartboot.socket.transport.WriteBuffer;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.io.IOException;
import java.util.List;

/**
 * Redis命令的抽象基类
 * <p>
 * 该类定义了所有Redis命令的通用结构和行为规范。
 * 每个具体的Redis命令实现都应该继承此类，并实现 buildParams 方法来构建命令参数。
 * </p>
 *
 * @author 三刀
 * @version v1.0 10/23/25
 * @see BulkStrings Redis协议中的批量字符串类型
 * @see <a href="https://redis.io/docs/latest/commands">Redis Commands</a>
 */
public abstract class Command {
    /**
     * 构建Redis命令参数列表的抽象方法
     * <p>
     * 每个具体的命令实现类都需要实现此方法，用于构建符合Redis协议规范的命令参数列表。
     * 返回的参数列表将被用于构造完整的Redis命令请求。
     * </p>
     *
     * @return 包含命令参数的BulkStrings列表，遵循Redis协议规范
     * @see BulkStrings
     */
    protected abstract List<BulkStrings> buildParams();

    public void writeTo(WriteBuffer writeBuffer) throws IOException {
        List params = buildParams();
        RESP.ofArray(params).writeTo(writeBuffer);
    }
}