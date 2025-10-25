package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version v1.0 10/22/25
 */
public final class ZAddCommand extends Command {
    private static final BulkStrings CONSTANTS_ZADD = BulkStrings.of("ZADD");
    private final String key;
    private final String member;
    private final double score;

    public ZAddCommand(String key, double score, String member) {
        this.key = key;
        this.member = member;
        this.score = score;
    }

    @Override
    public List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>(4);
        param.add(CONSTANTS_ZADD);
        param.add(RESP.ofString(key));
        param.add(RESP.ofString(BigDecimal.valueOf(score).toPlainString()));
        param.add(RESP.ofString(member));
        return param;
    }
}
