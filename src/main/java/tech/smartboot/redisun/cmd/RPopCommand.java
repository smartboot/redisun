package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis RPOP 命令实现类
 * <p>
 * 移除并返回列表的尾部(右边)最后一个元素，当列表不存在时返回null。
 * </p>
 *
 * @see <a href="https://redis.io/commands/rpop/">Redis RPOP Command</a>
 */
public class RPopCommand extends Command {
    private static final BulkStrings CONSTANTS_RPOP = BulkStrings.of("RPOP");
    private final String key;

    public RPopCommand(String key) {
        this.key = key;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>();
        param.add(CONSTANTS_RPOP);
        param.add(RESP.ofString(key));
        return param;
    }
}