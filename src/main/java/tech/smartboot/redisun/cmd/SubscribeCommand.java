package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * 订阅给定的一个或多个频道的信息。 Available since: Redis Open Source 2.0.0
 * @author dufuzhong
 * @version v1.0 2025-12-07
 * @see <a href="https://redis.io/docs/latest/commands/subscribe/">Redis SUBSCRIBE Command</a>
 */
public class SubscribeCommand extends Command {
    private static final BulkStrings CMD_SUBSCRIBE = BulkStrings.of("SUBSCRIBE");
    private final String[] channel;

    public SubscribeCommand(String... channel) {
        this.channel = channel;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>(channel.length + 1);
        param.add(CMD_SUBSCRIBE);
        for (String s : channel) {
            param.add(RESP.ofString(s));
        }
        return param;
    }

}
