package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis SELECT 命令实现类
 * <p>
 * SELECT 命令用于切换到指定的数据库。Redis 默认提供 16 个数据库，
 * 数据库索引号 index 用数字值指定，以 0 作为起始索引值。
 * 默认使用 0 号数据库。
 * </p>
 *
 * SELECT命令语法：
 * SELECT index
 *
 * @author 三刀
 * @version v1.0 10/26/25
 * @see <a href="https://redis.io/commands/select/">Redis SELECT Command</a>
 */
public class SelectCommand extends Command {
    private static final BulkStrings CONSTANTS_SELECT = BulkStrings.of("SELECT");
    // 要切换的数据库索引
    private final int index;

    /**
     * 构造函数，创建一个SELECT命令实例
     *
     * @param index 要切换到的数据库索引（从0开始）
     */
    public SelectCommand(int index) {
        this.index = index;
    }

    /**
     * 向参数列表中添加SELECT命令及其参数
     *
     * @return 包含命令参数的BulkStrings列表
     */
    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>();
        // 添加命令名称
        param.add(CONSTANTS_SELECT);
        // 添加数据库索引
        param.add(RESP.ofString(String.valueOf(index)));
        return param;
    }
}