package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Redis MSET 命令实现类
 * <p>
 * 同时设置一个或多个 key-value 对。
 * 如果某个给定 key 已经存在，那么 MSET 会用新值覆盖原来的旧值，不存在的 key 会被创建。
 * MSET 是一个原子性(atomic)操作，整个 MSET 操作要么全部执行要么全部不执行。
 * </p>
 *
 * @see <a href="https://redis.io/commands/mset/">Redis MSET Command</a>
 */
public class MSetCommand extends Command {
    private static final BulkStrings CONSTANTS_MSET = BulkStrings.of("MSET");
    private final Map<String, String> keyValuePairs;

    public MSetCommand(Map<String, String> keyValuePairs) {
        this.keyValuePairs = keyValuePairs;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>(keyValuePairs.size() * 2 + 1);
        param.add(CONSTANTS_MSET);
        for (Map.Entry<String, String> entry : keyValuePairs.entrySet()) {
            param.add(RESP.ofString(entry.getKey()));
            param.add(RESP.ofString(entry.getValue()));
        }
        return param;
    }
}