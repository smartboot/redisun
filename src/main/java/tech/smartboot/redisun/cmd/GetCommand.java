package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version v1.0 10/23/25
 */
public class GetCommand extends Command {
    private static final BulkStrings CMD_GET = BulkStrings.of("GET");
    private final String key;

    public GetCommand(String key) {
        this.key = key;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>(2);
        param.add(CMD_GET);
        param.add(RESP.ofString(key));
        return param;
    }
}
