package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis HMGET 命令实现类
 * <p>
 * 返回哈希表中指定字段的值。
 * 如果字段不存在，返回 nil。
 * 如果 key 不存在，返回 nil。
 * </p>
 *
 * @see <a href="https://redis.io/commands/hmget/">Redis HMGET Command</a>
 */
public class HmGetCommand extends Command {
    private static final BulkStrings CONSTANTS_HMGET = BulkStrings.of("HMGET");
    private final String key;
    private final List<String> fields;

    public HmGetCommand(String key, List<String> fields) {
        this.key = key;
        this.fields = fields;
    }

    public HmGetCommand(String key, String... fields) {
        this(key, java.util.Arrays.asList(fields));
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>(2 + fields.size());
        param.add(CONSTANTS_HMGET);
        param.add(RESP.ofString(key));
        for (String field : fields) {
            param.add(RESP.ofString(field));
        }
        return param;
    }
}
