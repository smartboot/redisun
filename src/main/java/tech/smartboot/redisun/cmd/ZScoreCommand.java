package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis ZSCORE 命令实现类
 * <p>
 * 返回有序集合中指定成员的分数。
 * </p>
 * 
 * @see <a href="https://redis.io/commands/zscore/">Redis ZSCORE Command</a>
 */
public class ZScoreCommand extends Command {
    private static final BulkStrings CONSTANTS_ZSCORE = BulkStrings.of("ZSCORE");
    private final String key;
    private final String member;

    public ZScoreCommand(String key, String member) {
        this.key = key;
        this.member = member;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>(3);
        param.add(CONSTANTS_ZSCORE);
        param.add(RESP.ofString(key));
        param.add(RESP.ofString(member));
        return param;
    }
}