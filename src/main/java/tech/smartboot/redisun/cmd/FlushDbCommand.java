package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis FLUSHDB 命令实现类
 * <p>
 * FLUSHDB命令用于删除当前选定数据库中的所有键。
 * 该命令总是成功执行并返回OK。
 * </p>
 *
 * @author 三刀
 * @version v1.0 10/27/25
 * @see <a href="https://redis.io/commands/flushdb/">Redis FLUSHDB Command</a>
 */
public class FlushDbCommand extends Command {
    private static final BulkStrings CONSTANTS_FLUSHDB = BulkStrings.of("FLUSHDB");

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>();
        param.add(CONSTANTS_FLUSHDB);
        return param;
    }
}