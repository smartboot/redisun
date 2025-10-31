package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis EXISTS 命令实现类
 * <p>
 * 检查给定键是否存在。
 * </p>
 *
 * @see <a href="https://redis.io/commands/exists/">Redis EXISTS Command</a>
 */
public class ExistsCommand extends Command {
    private static final BulkStrings CONSTANTS_EXISTS = BulkStrings.of("EXISTS");
    private final List<String> keys;

    public ExistsCommand(List<String> keys) {
        this.keys = keys;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>(keys.size() + 1);
        param.add(CONSTANTS_EXISTS);
        for (String key : keys) {
            param.add(RESP.ofString(key));
        }
        return param;
    }
}