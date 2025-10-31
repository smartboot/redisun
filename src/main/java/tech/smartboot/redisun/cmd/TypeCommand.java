package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis TYPE 命令实现类
 * <p>
 * 返回 key 所储存的值的类型。
 * </p>
 *
 * @see <a href="https://redis.io/commands/type/">Redis TYPE Command</a>
 */
public class TypeCommand extends Command {
    private static final BulkStrings CONSTANTS_TYPE = BulkStrings.of("TYPE");
    private final String key;

    public TypeCommand(String key) {
        this.key = key;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>();
        param.add(CONSTANTS_TYPE);
        param.add(RESP.ofString(key));
        return param;
    }
}