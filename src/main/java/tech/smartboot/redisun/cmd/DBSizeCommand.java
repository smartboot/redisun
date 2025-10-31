package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis DBSIZE 命令实现类
 * <p>
 * 返回当前数据库中键的数量。
 * </p>
 *
 * @see <a href="https://redis.io/commands/dbsize/">Redis DBSIZE Command</a>
 */
public class DBSizeCommand extends Command {
    private static final BulkStrings CONSTANTS_DBSIZE = BulkStrings.of("DBSIZE");

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>();
        param.add(CONSTANTS_DBSIZE);
        return param;
    }
}