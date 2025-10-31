package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis MGET 命令实现类
 * <p>
 * 返回所有(一个或多个)给定 key 的值。
 * 如果给定的 key 里面，有某个 key 不存在，那么这个 key 返回特殊值 nil 。
 * 因此，该命令永不失败。
 * </p>
 *
 * @see <a href="https://redis.io/commands/mget/">Redis MGET Command</a>
 */
public class MGetCommand extends Command {
    private static final BulkStrings CONSTANTS_MGET = BulkStrings.of("MGET");
    private final List<String> keys;

    public MGetCommand(List<String> keys) {
        this.keys = keys;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>(keys.size() + 1);
        param.add(CONSTANTS_MGET);
        for (String key : keys) {
            param.add(RESP.ofString(key));
        }
        return param;
    }
}