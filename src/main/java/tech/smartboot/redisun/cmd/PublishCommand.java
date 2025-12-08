package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * 将信息发送到指定的频道。 Available since: Redis Open Source 2.0.0
 * @author dufuzhong
 * @version v1.0 2025-12-07
 * @see <a href="https://redis.io/docs/latest/commands/publish/">Redis PUBLISH Command</a>
 */
public class PublishCommand extends Command {
    private static final BulkStrings CMD_PUBLISH = BulkStrings.of("PUBLISH");
    private final String channel;
    private final String message;

    public PublishCommand(String channel, String message) {
        this.channel = channel;
        this.message = message;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>(3);
        param.add(CMD_PUBLISH);
        param.add(RESP.ofString(channel));
        param.add(RESP.ofString(message));
        return param;
    }
}
