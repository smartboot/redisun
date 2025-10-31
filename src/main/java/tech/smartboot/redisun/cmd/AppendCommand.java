package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis APPEND 命令实现类
 * <p>
 * 如果 key 已经存在并且是一个字符串，APPEND 命令将 value 追加到 key 原来的值的末尾。
 * 如果 key 不存在，APPEND 就简单地将给定 key 设为 value ，就像执行 SET key value 一样。
 * </p>
 *
 * @see <a href="https://redis.io/commands/append/">Redis APPEND Command</a>
 */
public class AppendCommand extends Command {
    private static final BulkStrings CONSTANTS_APPEND = BulkStrings.of("APPEND");
    private final String key;
    private final String value;

    public AppendCommand(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>();
        param.add(CONSTANTS_APPEND);
        param.add(RESP.ofString(key));
        param.add(RESP.ofString(value));
        return param;
    }
}