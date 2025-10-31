package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis DECRBY 命令实现类
 * <p>
 * 将 key 所储存的值减去给定的减量值（decrement）。
 * 如果 key 不存在，那么 key 的值会先被初始化为 0 ，然后再执行 DECRBY 操作。
 * 如果值包含错误的类型，或字符串类型的值不能表示为数字，那么返回一个错误。
 * 本操作的值限制在 64 位(bit)有符号数字表示之内。
 * </p>
 *
 * @see <a href="https://redis.io/commands/decrby/">Redis DECRBY Command</a>
 */
public class DecrByCommand extends Command {
    private static final BulkStrings CONSTANTS_DECRBY = BulkStrings.of("DECRBY");
    private final String key;
    private final long decrement;

    public DecrByCommand(String key, long decrement) {
        this.key = key;
        this.decrement = decrement;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>();
        param.add(CONSTANTS_DECRBY);
        param.add(RESP.ofString(key));
        param.add(RESP.ofString(String.valueOf(decrement)));
        return param;
    }
}