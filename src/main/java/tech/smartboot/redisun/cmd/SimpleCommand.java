package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;

import java.util.List;

public class SimpleCommand extends Command {
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
    public static final BulkStrings CONSTANTS_DECRBY = BulkStrings.of("DECRBY");

    /**
     * Redis APPEND 命令实现类
     * <p>
     * 如果 key 已经存在并且是一个字符串，APPEND 命令将 value 追加到 key 原来的值的末尾。
     * 如果 key 不存在，APPEND 就简单地将给定 key 设为 value ，就像执行 SET key value 一样。
     * </p>
     *
     * @see <a href="https://redis.io/commands/append/">Redis APPEND Command</a>
     */
    public static final BulkStrings CONSTANTS_APPEND = BulkStrings.of("APPEND");
    private final List<BulkStrings> arrays;

    public SimpleCommand(BulkStrings... arrays) {
        this.arrays = java.util.Arrays.asList(arrays);
    }

    @Override
    protected List<BulkStrings> buildParams() {
        return arrays;
    }
}
