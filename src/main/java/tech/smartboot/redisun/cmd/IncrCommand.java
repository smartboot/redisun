package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis INCR 命令实现类
 * <p>
 * 将 key 中储存的数字值增一。
 * 如果 key 不存在，那么 key 的值会先被初始化为 0 ，然后再执行 INCR 操作。
 * 如果值包含错误的类型，或字符串类型的值不能表示为数字，那么返回一个错误。
 * 本操作的值限制在 64 位(bit)有符号数字表示之内。
 * </p>
 *
 * @see <a href="https://redis.io/commands/incr/">Redis INCR Command</a>
 */
public class IncrCommand extends Command {
    private static final BulkStrings CONSTANTS_INCR = BulkStrings.of("INCR");
    private final String key;

    public IncrCommand(String key) {
        this.key = key;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>();
        param.add(CONSTANTS_INCR);
        param.add(RESP.ofString(key));
        return param;
    }
}