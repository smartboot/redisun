package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis TTL 命令实现类
 * <p>
 * 以秒为单位返回 key 的剩余过期时间。
 * </p>
 *
 * @see <a href="https://redis.io/commands/ttl/">Redis TTL Command</a>
 */
public class TtlCommand extends Command {
    private static final BulkStrings CONSTANTS_TTL = BulkStrings.of("TTL");
    private final String key;

    public TtlCommand(String key) {
        this.key = key;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>();
        param.add(CONSTANTS_TTL);
        param.add(RESP.ofString(key));
        return param;
    }
}