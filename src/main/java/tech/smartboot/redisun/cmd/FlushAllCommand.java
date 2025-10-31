package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis FLUSHALL 命令实现类
 * <p>
 * FLUSHALL命令用于删除所有数据库中的所有键，而不仅仅是当前选择的数据库。
 * 该命令总是成功执行并返回OK。
 * </p>
 *
 * @author 三刀
 * @version v1.0 10/27/25
 * @see <a href="https://redis.io/commands/flushall/">Redis FLUSHALL Command</a>
 */
public class FlushAllCommand extends Command {
    private static final BulkStrings CONSTANTS_FLUSHALL = BulkStrings.of("FLUSHALL");

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>();
        param.add(CONSTANTS_FLUSHALL);
        return param;
    }
}