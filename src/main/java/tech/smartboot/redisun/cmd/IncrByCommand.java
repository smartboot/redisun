package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis INCRBY 命令实现类
 * <p>
 * 将 key 所储存的值加上给定的增量值（increment）。
 * 如果 key 不存在，那么 key 的值会先被初始化为 0 ，然后再执行 INCRBY 操作。
 * 如果值包含错误的类型，或字符串类型的值不能表示为数字，那么返回一个错误。
 * 本操作的值限制在 64 位(bit)有符号数字表示之内。
 * </p>
 *
 * @see <a href="https://redis.io/commands/incrby/">Redis INCRBY Command</a>
 */
public class IncrByCommand extends Command {
    private static final BulkStrings CONSTANTS_INCRBY = BulkStrings.of("INCRBY");
    private final String key;
    private final long increment;

    public IncrByCommand(String key, long increment) {
        this.key = key;
        this.increment = increment;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>();
        param.add(CONSTANTS_INCRBY);
        param.add(RESP.ofString(key));
        param.add(RESP.ofString(String.valueOf(increment)));
        return param;
    }
}