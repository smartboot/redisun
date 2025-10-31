package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis HGET 命令实现类
 * <p>
 * 返回哈希表中指定字段的值。
 * 如果字段不存在，返回 nil。
 * 如果 key 不存在，返回 nil。
 * </p>
 *
 * @see <a href="https://redis.io/commands/hget/">Redis HGET Command</a>
 */
public class HGetCommand extends Command {
    private static final BulkStrings CONSTANTS_HGET = BulkStrings.of("HGET");
    private final String key;
    private final String field;

    public HGetCommand(String key, String field) {
        this.key = key;
        this.field = field;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>();
        param.add(CONSTANTS_HGET);
        param.add(RESP.ofString(key));
        param.add(RESP.ofString(field));
        return param;
    }
}