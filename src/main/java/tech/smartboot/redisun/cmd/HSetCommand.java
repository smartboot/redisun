package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis HSET 命令实现类
 * <p>
 * 将哈希表 key 中的字段 field 的值设为 value 。
 * 如果哈希表不存在，一个新的哈希表被创建并进行 HSET 操作。
 * 如果字段 field 已经存在于哈希表中，旧值将被覆盖。
 * </p>
 *
 * @see <a href="https://redis.io/commands/hset/">Redis HSET Command</a>
 */
public class HSetCommand extends Command {
    private static final BulkStrings CONSTANTS_HSET = BulkStrings.of("HSET");
    private final String key;
    private final String field;
    private final String value;

    public HSetCommand(String key, String field, String value) {
        this.key = key;
        this.field = field;
        this.value = value;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>();
        param.add(CONSTANTS_HSET);
        param.add(RESP.ofString(key));
        param.add(RESP.ofString(field));
        param.add(RESP.ofString(value));
        return param;
    }
}