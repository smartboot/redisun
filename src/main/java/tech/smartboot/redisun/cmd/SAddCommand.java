package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis SADD 命令实现类
 * <p>
 * 将一个或多个成员加入到集合中，已经存在于集合的成员将被忽略。
 * 如果集合不存在，则创建一个只包含添加的成员作成员的集合。
 * </p>
 *
 * @see <a href="https://redis.io/commands/sadd/">Redis SADD Command</a>
 */
public class SAddCommand extends Command {
    private static final BulkStrings CONSTANTS_SADD = BulkStrings.of("SADD");
    private final String key;
    private final String[] members;

    public SAddCommand(String key, String... members) {
        this.key = key;
        this.members = members;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>();
        param.add(CONSTANTS_SADD);
        param.add(RESP.ofString(key));
        for (String member : members) {
            param.add(RESP.ofString(member));
        }
        return param;
    }
}