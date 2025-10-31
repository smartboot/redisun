package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis LPUSH 命令实现类
 * <p>
 * 将一个或多个值插入到列表的头部(左边)。 如果 key 不存在，一个空列表会被创建并执行 LPUSH 操作。
 * 当 key 存在但不是列表类型时，返回一个错误。
 * </p>
 *
 * @see <a href="https://redis.io/commands/lpush/">Redis LPUSH Command</a>
 */
public class LPushCommand extends Command {
    private static final BulkStrings CONSTANTS_LPUSH = BulkStrings.of("LPUSH");
    private final String key;
    private final String[] values;

    public LPushCommand(String key, String... values) {
        this.key = key;
        this.values = values;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>();
        param.add(CONSTANTS_LPUSH);
        param.add(RESP.ofString(key));
        for (String value : values) {
            param.add(RESP.ofString(value));
        }
        return param;
    }
}