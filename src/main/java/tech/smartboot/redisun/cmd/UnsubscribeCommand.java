package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * 取消订阅给定的频道
 * Available since: Redis Open Source 2.0.0
 *
 * @author dufuzhong
 * @version v1.0 2025-12-07
 * @see <a href="https://redis.io/docs/latest/commands/unsubscribe/">Redis UNSUBSCRIBE Command</a>
 */
public class UnsubscribeCommand extends Command {
    private static final BulkStrings CMD_UNSUBSCRIBE = BulkStrings.of("UNSUBSCRIBE");
    private final String[] channels;

    /**
     * 创建 UNSUBSCRIBE 命令
     * 如果不指定频道，则取消订阅所有频道
     * 
     * @param channels 要取消订阅的频道列表，如果为空则取消所有订阅
     */
    public UnsubscribeCommand(String... channels) {
        this.channels = channels != null ? channels : new String[0];
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>(channels.length + 1);
        param.add(CMD_UNSUBSCRIBE);
        for (String channel : channels) {
            param.add(RESP.ofString(channel));
        }
        return param;
    }
}
