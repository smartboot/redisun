package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * 订阅一个或多个符合给定模式的频道。
 * Available since: Redis Open Source 2.0.0
 * 
 * @author dufuzhong
 * @version v1.0 2025-12-09
 * @see <a href="https://redis.io/docs/latest/commands/psubscribe/">Redis PSUBSCRIBE Command</a>
 */
public class PSubscribeCommand extends Command {
    private static final BulkStrings CMD_PSUBSCRIBE = BulkStrings.of("PSUBSCRIBE");
    private final String[] patterns;

    /**
     * 创建 PSUBSCRIBE 命令
     * 
     * @param patterns 要订阅的频道模式列表（支持通配符 * 和 ?）
     */
    public PSubscribeCommand(String... patterns) {
        this.patterns = patterns;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>(patterns.length + 1);
        param.add(CMD_PSUBSCRIBE);
        for (String pattern : patterns) {
            param.add(RESP.ofString(pattern));
        }
        return param;
    }
}
