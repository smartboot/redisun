package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis LPOP 命令实现类
 * <p>
 * 移除并返回列表的头部(左边)第一个元素，当列表不存在时返回null。
 * </p>
 *
 * @see <a href="https://redis.io/commands/lpop/">Redis LPOP Command</a>
 */
public class LPopCommand extends Command {
    private static final BulkStrings CONSTANTS_LPOP = BulkStrings.of("LPOP");
    private final String key;

    public LPopCommand(String key) {
        this.key = key;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>();
        param.add(CONSTANTS_LPOP);
        param.add(RESP.ofString(key));
        return param;
    }
}