package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis ZREM 命令实现类
 * <p>
 * 用于移除有序集合中的一个或多个成员。
 * </p>
 * 
 * @see <a href="https://redis.io/commands/zrem/">Redis ZREM Command</a>
 */
public class ZRemCommand extends Command {
    private static final BulkStrings CONSTANTS_ZREM = BulkStrings.of("ZREM");
    private final String key;
    private final String[] members;

    public ZRemCommand(String key, String... members) {
        this.key = key;
        this.members = members;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>(2 + members.length);
        param.add(CONSTANTS_ZREM);
        param.add(RESP.ofString(key));
        for (String member : members) {
            param.add(RESP.ofString(member));
        }
        return param;
    }
}