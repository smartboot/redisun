package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis RPUSH 命令实现类
 * <p>
 * 将一个或多个值插入到列表的尾部(右边)。 如果 key 不存在，一个空列表会被创建并执行 RPUSH 操作。
 * 当 key 存在但不是列表类型时，返回一个错误。
 * </p>
 *
 * @see <a href="https://redis.io/commands/rpush/">Redis RPUSH Command</a>
 */
public class RPushCommand extends Command {
    private static final BulkStrings CONSTANTS_RPUSH = BulkStrings.of("RPUSH");
    private final String key;
    private final String[] values;

    public RPushCommand(String key, String... values) {
        this.key = key;
        this.values = values;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>();
        param.add(CONSTANTS_RPUSH);
        param.add(RESP.ofString(key));
        for (String value : values) {
            param.add(RESP.ofString(value));
        }
        return param;
    }
}