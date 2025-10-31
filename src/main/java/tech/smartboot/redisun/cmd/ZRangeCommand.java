package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Redis ZRANGE 命令实现类
 * <p>
 * 返回有序集合中指定范围的成员。
 * 从 Redis 6.2.0 开始，ZRANGE 命令支持 BYSCORE、BYLEX 和 REV 选项，
 * 可以替代 ZREVRANGE、ZRANGEBYSCORE、ZREVRANGEBYSCORE、ZRANGEBYLEX 和 ZREVRANGEBYLEX 命令。
 * </p>
 *
 * @see <a href="https://redis.io/commands/zrange/">Redis ZRANGE Command</a>
 */
public class ZRangeCommand extends Command {
    private static final BulkStrings CONSTANTS_ZRANGE = BulkStrings.of("ZRANGE");
    private static final BulkStrings CONSTANTS_BYSCORE = BulkStrings.of("BYSCORE");
    private static final BulkStrings CONSTANTS_BYLEX = BulkStrings.of("BYLEX");
    private static final BulkStrings CONSTANTS_REV = BulkStrings.of("REV");
    private static final BulkStrings CONSTANTS_WITHSCORES = BulkStrings.of("WITHSCORES");
    private static final BulkStrings CONSTANTS_LIMIT = BulkStrings.of("LIMIT");

    private final String key;
    private final String start;
    private final String stop;

    // 四个选项参数
    private BulkStrings sort = null;
    private BulkStrings rev = null;
    private BulkStrings withScores = null;

    // LIMIT参数
    private Consumer<List<BulkStrings>> limit = null;

    public ZRangeCommand(String key, String start, String stop) {
        this.key = key;
        this.start = start;
        this.stop = stop;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>(10);
        param.add(CONSTANTS_ZRANGE);
        param.add(RESP.ofString(key));
        param.add(RESP.ofString(start));
        param.add(RESP.ofString(stop));

        // 添加选项参数
        if (sort != null) {
            param.add(sort);
        }

        if (rev != null) {
            param.add(rev);
        }

        // 添加LIMIT参数
        if (limit != null) {
            limit.accept(param);
        }

        if (withScores != null) {
            param.add(withScores);
        }

        return param;
    }

    /**
     * 设置 BYSCORE 选项：按分数查询
     *
     * @return 当前 ZRangeCommand 实例，支持链式调用
     */
    public ZRangeCommand sortByScore() {
        this.sort = CONSTANTS_BYSCORE;
        return this;
    }

    /**
     * 设置 BYLEX 选项：按字典序查询
     *
     * @return 当前 ZRangeCommand 实例，支持链式调用
     */
    public ZRangeCommand sortByLex() {
        this.sort = CONSTANTS_BYLEX;
        return this;
    }

    /**
     * 设置 REV 选项：倒序排列
     *
     * @return 当前 ZRangeCommand 实例，支持链式调用
     */
    public ZRangeCommand rev() {
        this.rev = CONSTANTS_REV;
        return this;
    }

    /**
     * 设置 WITHSCORES 选项：同时返回成员的分数
     *
     * @return 当前 ZRangeCommand 实例，支持链式调用
     */
    public ZRangeCommand withScores() {
        this.withScores = CONSTANTS_WITHSCORES;
        return this;
    }

    /**
     * 设置 LIMIT 选项：限制返回结果数量
     *
     * @param offset 跳过的元素数量
     * @param count  返回的元素数量
     * @return 当前 ZRangeCommand 实例，支持链式调用
     */
    public ZRangeCommand limit(long offset, long count) {
        this.limit = bulkStrings -> {
            bulkStrings.add(CONSTANTS_LIMIT);
            bulkStrings.add(BulkStrings.ofString(String.valueOf(offset)));
            bulkStrings.add(BulkStrings.ofString(String.valueOf(count)));
        };
        return this;
    }

    public static class Tuple {
        private String member;
        private Double score;

        public void setMember(String member) {
            this.member = member;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public String getMember() {
            return member;
        }

        public Double getScore() {
            return score;
        }
    }
}