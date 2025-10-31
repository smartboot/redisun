package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis STRLEN 命令实现类
 * <p>
 * 返回 key 所储存的字符串值的长度。
 * 当 key 储存的不是字符串值时，返回一个错误。
 * </p>
 *
 * @see <a href="https://redis.io/commands/strlen/">Redis STRLEN Command</a>
 */
public class StrlenCommand extends Command {
    private static final BulkStrings CONSTANTS_STRLEN = BulkStrings.of("STRLEN");
    private final String key;

    public StrlenCommand(String key) {
        this.key = key;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>();
        param.add(CONSTANTS_STRLEN);
        param.add(RESP.ofString(key));
        return param;
    }
}