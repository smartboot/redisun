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
public class DelCommand extends Command {
    private static final BulkStrings CMD_DEL = BulkStrings.of("DEL");
    private final List<String> key;

    public DelCommand(List<String> key) {
        this.key = key;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>(key.size() + 1);
        param.add(CMD_DEL);
        for (String s : key) {
            param.add(RESP.ofString(s));
        }
        return param;
    }
}
