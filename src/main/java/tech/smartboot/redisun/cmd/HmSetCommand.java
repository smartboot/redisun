package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Redis HMSET 命令实现类
 * <p>
 * 同时将多个 field-value (域 - 值) 对设置到哈希表中。
 * 如果哈希表不存在，HMSET 会先创建一个空的哈希表再进行操作。
 * 如果字段已存在，旧值将被新值覆盖。
 * </p>
 *
 * @see <a href="https://redis.io/commands/hmset/">Redis HMSET Command</a>
 */
public class HmSetCommand extends Command {
    private static final BulkStrings CONSTANTS_HMSET = BulkStrings.of("HMSET");
    private final String key;
    private final Map<String, String> hash;

    public HmSetCommand(String key, Map<String, String> hash) {
        this.key = key;
        this.hash = hash;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>(2 + hash.size() * 2);
        param.add(CONSTANTS_HMSET);
        param.add(RESP.ofString(key));
        for (Map.Entry<String, String> entry : hash.entrySet()) {
            param.add(RESP.ofString(entry.getKey()));
            param.add(RESP.ofString(entry.getValue()));
        }
        return param;
    }
}
