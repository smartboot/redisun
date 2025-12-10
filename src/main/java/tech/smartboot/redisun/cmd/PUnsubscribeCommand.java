package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * 取消订阅所有给定模式的频道
 * Available since: Redis Open Source 2.0.0
 *
 * @author dufuzhong
 * @version v1.0 2025-12-09
 * @see <a href="https://redis.io/docs/latest/commands/punsubscribe/">Redis PUNSUBSCRIBE Command</a>
 */
public class PUnsubscribeCommand extends Command {
    private static final BulkStrings CMD_PUNSUBSCRIBE = BulkStrings.of("PUNSUBSCRIBE");
    private final String[] patterns;

    /**
     * 创建 PUNSUBSCRIBE 命令
     * 如果不指定模式，则取消订阅所有模式
     * 
     * @param patterns 要取消订阅的模式列表，如果为空则取消所有模式订阅
     */
    public PUnsubscribeCommand(String... patterns) {
        this.patterns = patterns != null ? patterns : new String[0];
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>(patterns.length + 1);
        param.add(CMD_PUNSUBSCRIBE);
        for (String pattern : patterns) {
            param.add(RESP.ofString(pattern));
        }
        return param;
    }
}
