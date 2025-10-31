package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis DECR 命令实现类
 * <p>
 * 将 key 中储存的数字值减一。
 * 如果 key 不存在，那么 key 的值会先被初始化为 0 ，然后再执行 DECR 操作。
 * 如果值包含错误的类型，或字符串类型的值不能表示为数字，那么返回一个错误。
 * 本操作的值限制在 64 位(bit)有符号数字表示之内。
 * </p>
 *
 * @see <a href="https://redis.io/commands/decr/">Redis DECR Command</a>
 */
public class DecrCommand extends Command {
    private static final BulkStrings CONSTANTS_DECR = BulkStrings.of("DECR");
    private final String key;

    public DecrCommand(String key) {
        this.key = key;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>();
        param.add(CONSTANTS_DECR);
        param.add(RESP.ofString(key));
        return param;
    }
}