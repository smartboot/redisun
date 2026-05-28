package tech.smartboot.redisun;

import io.github.smartboot.socket.transport.WriteBuffer;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.io.IOException;
import java.util.List;

class SimpleCommand extends Command {
    /**
     * Redis DECRBY 命令常量
     * <p>
     * 将 key 所储存的值减去给定的减量值（decrement）。
     * 如果 key 不存在，那么 key 的值会先被初始化为 0 ，然后再执行 DECRBY 操作。
     * 如果值包含错误的类型，或字符串类型的值不能表示为数字，那么返回一个错误。
     * 本操作的值限制在 64 位(bit)有符号数字表示之内。
     * </p>
     *
     * @see <a href="https://redis.io/commands/decrby/">Redis DECRBY Command</a>
     */
    public static final BulkStrings CONSTANTS_DECRBY = BulkStrings.of("DECRBY");

    /**
     * Redis APPEND 命令常量
     * <p>
     * 如果 key 已经存在并且是一个字符串，APPEND 命令将 value 追加到 key 原来的值的末尾。
     * 如果 key 不存在，APPEND 就简单地将给定 key 设为 value ，就像执行 SET key value 一样。
     * </p>
     *
     * @see <a href="https://redis.io/commands/append/">Redis APPEND Command</a>
     */
    public static final BulkStrings CONSTANTS_APPEND = BulkStrings.of("APPEND");

    /**
     * Redis DECR 命令常量
     * <p>
     * 将 key 中储存的数字值减一。
     * 如果 key 不存在，那么 key 的值会先被初始化为 0 ，然后再执行 DECR 操作。
     * 如果值包含错误的类型，或字符串类型的值不能表示为数字，那么返回一个错误。
     * 本操作的值限制在 64 位(bit)有符号数字表示之内。
     * </p>
     *
     * @see <a href="https://redis.io/commands/decr/">Redis DECR Command</a>
     */
    public static final BulkStrings CONSTANTS_DECR = BulkStrings.of("DECR");

    /**
     * Redis INCR 命令常量
     * <p>
     * 将 key 中储存的数字值增一。
     * 如果 key 不存在，那么 key 的值会先被初始化为 0 ，然后再执行 INCR 操作。
     * 如果值包含错误的类型，或字符串类型的值不能表示为数字，那么返回一个错误。
     * 本操作的值限制在 64 位(bit)有符号数字表示之内。
     * </p>
     *
     * @see <a href="https://redis.io/commands/incr/">Redis INCR Command</a>
     */
    public static final BulkStrings CONSTANTS_INCR = BulkStrings.of("INCR");

    /**
     * Redis INCRBY 命令常量
     * <p>
     * 将 key 所储存的值加上给定的增量值（increment）。
     * 如果 key 不存在，那么 key 的值会先被初始化为 0 ，然后再执行 INCRBY 操作。
     * 如果值包含错误的类型，或字符串类型的值不能表示为数字，那么返回一个错误。
     * 本操作的值限制在 64 位(bit)有符号数字表示之内。
     * </p>
     *
     * @see <a href="https://redis.io/commands/incrby/">Redis INCRBY Command</a>
     */
    public static final BulkStrings CONSTANTS_INCRBY = BulkStrings.of("INCRBY");

    /**
     * Redis STRLEN 命令常量
     * <p>
     * 返回 key 所储存的字符串值的长度。
     * 当 key 储存的不是字符串值时，返回一个错误。
     * </p>
     *
     * @see <a href="https://redis.io/commands/strlen/">Redis STRLEN Command</a>
     */
    public static final BulkStrings CONSTANTS_STRLEN = BulkStrings.of("STRLEN");

    /**
     * Redis TTL 命令常量
     * <p>
     * 以秒为单位返回 key 的剩余过期时间。
     * </p>
     *
     * @see <a href="https://redis.io/commands/ttl/">Redis TTL Command</a>
     */
    public static final BulkStrings CONSTANTS_TTL = BulkStrings.of("TTL");

    /**
     * Redis TYPE 命令常量
     * <p>
     * 返回 key 所储存的值的类型。
     * </p>
     *
     * @see <a href="https://redis.io/commands/type/">Redis TYPE Command</a>
     */
    public static final BulkStrings CONSTANTS_TYPE = BulkStrings.of("TYPE");

    /**
     * Redis DBSIZE 命令常量
     * <p>
     * 返回当前数据库中键的数量。
     * </p>
     *
     * @see <a href="https://redis.io/commands/dbsize/">Redis DBSIZE Command</a>
     */
    public static final BulkStrings CONSTANTS_DBSIZE = BulkStrings.of("DBSIZE");

    /**
     * Redis FLUSHALL 命令常量
     * <p>
     * FLUSHALL命令用于删除所有数据库中的所有键，而不仅仅是当前选择的数据库。
     * 该命令总是成功执行并返回OK。
     * </p>
     *
     * @see <a href="https://redis.io/commands/flushall/">Redis FLUSHALL Command</a>
     */
    public static final BulkStrings CONSTANTS_FLUSHALL = BulkStrings.of("FLUSHALL");

    /**
     * Redis FLUSHDB 命令常量
     * <p>
     * FLUSHDB命令用于删除当前选定数据库中的所有键。
     * 该命令总是成功执行并返回OK。
     * </p>
     *
     * @see <a href="https://redis.io/commands/flushdb/">Redis FLUSHDB Command</a>
     */
    public static final BulkStrings CONSTANTS_FLUSHDB = BulkStrings.of("FLUSHDB");

    /**
     * Redis LPOP 命令常量
     * <p>
     * 移除并返回列表的头部(左边)第一个元素，当列表不存在时返回null。
     * </p>
     *
     * @see <a href="https://redis.io/commands/lpop/">Redis LPOP Command</a>
     */
    public static final BulkStrings CONSTANTS_LPOP = BulkStrings.of("LPOP");

    /**
     * Redis RPOP 命令常量
     * <p>
     * 移除并返回列表的尾部(右边)最后一个元素，当列表不存在时返回null。
     * </p>
     *
     * @see <a href="https://redis.io/commands/rpop/">Redis RPOP Command</a>
     */
    public static final BulkStrings CONSTANTS_RPOP = BulkStrings.of("RPOP");

    /**
     * Redis DEL 命令常量
     * <p>
     * 删除一个或多个键。
     * </p>
     *
     * @see <a href="https://redis.io/commands/del/">Redis DEL Command</a>
     */
    public static final BulkStrings CONSTANTS_DEL = BulkStrings.of("DEL");

    /**
     * Redis EXISTS 命令常量
     * <p>
     * 检查给定键是否存在。
     * </p>
     *
     * @see <a href="https://redis.io/commands/exists/">Redis EXISTS Command</a>
     */
    public static final BulkStrings CONSTANTS_EXISTS = BulkStrings.of("EXISTS");

    /**
     * Redis HGET 命令常量
     * <p>
     * 返回哈希表中指定字段的值。
     * </p>
     *
     * @see <a href="https://redis.io/commands/hget/">Redis HGET Command</a>
     */
    public static final BulkStrings CONSTANTS_HGET = BulkStrings.of("HGET");

    /**
     * Redis HSET 命令常量
     * <p>
     * 将哈希表 key 中的字段 field 的值设为 value。
     * </p>
     *
     * @see <a href="https://redis.io/commands/hset/">Redis HSET Command</a>
     */
    public static final BulkStrings CONSTANTS_HSET = BulkStrings.of("HSET");

    /**
     * Redis LPUSH 命令常量
     * <p>
     * 将一个或多个值插入到列表的头部(左边)。
     * </p>
     *
     * @see <a href="https://redis.io/commands/lpush/">Redis LPUSH Command</a>
     */
    public static final BulkStrings CONSTANTS_LPUSH = BulkStrings.of("LPUSH");

    /**
     * Redis RPUSH 命令常量
     * <p>
     * 将一个或多个值插入到列表的尾部(右边)。
     * </p>
     *
     * @see <a href="https://redis.io/commands/rpush/">Redis RPUSH Command</a>
     */
    public static final BulkStrings CONSTANTS_RPUSH = BulkStrings.of("RPUSH");

    /**
     * Redis SADD 命令常量
     * <p>
     * 将一个或多个成员加入到集合中。
     * </p>
     *
     * @see <a href="https://redis.io/commands/sadd/">Redis SADD Command</a>
     */
    public static final BulkStrings CONSTANTS_SADD = BulkStrings.of("SADD");

    /**
     * Redis ZADD 命令常量
     * <p>
     * 向有序集合中添加一个或多个成员，或者更新已存在成员的分数。
     * </p>
     *
     * @see <a href="https://redis.io/commands/zadd/">Redis ZADD Command</a>
     */
    public static final BulkStrings CONSTANTS_ZADD = BulkStrings.of("ZADD");

    /**
     * Redis ZREM 命令常量
     * <p>
     * 移除有序集合中的一个或多个成员。
     * </p>
     *
     * @see <a href="https://redis.io/commands/zrem/">Redis ZREM Command</a>
     */
    public static final BulkStrings CONSTANTS_ZREM = BulkStrings.of("ZREM");

    /**
     * Redis ZSCORE 命令常量
     * <p>
     * 返回有序集合中指定成员的分数。
     * </p>
     *
     * @see <a href="https://redis.io/commands/zscore/">Redis ZSCORE Command</a>
     */
    public static final BulkStrings CONSTANTS_ZSCORE = BulkStrings.of("ZSCORE");

    /**
     * Redis MGET 命令常量
     * <p>
     * 返回所有(一个或多个)给定 key 的值。
     * </p>
     *
     * @see <a href="https://redis.io/commands/mget/">Redis MGET Command</a>
     */
    public static final BulkStrings CONSTANTS_MGET = BulkStrings.of("MGET");

    /**
     * Redis MSET 命令常量
     * <p>
     * 同时设置一个或多个 key-value 对。
     * </p>
     *
     * @see <a href="https://redis.io/commands/mset/">Redis MSET Command</a>
     */
    public static final BulkStrings CONSTANTS_MSET = BulkStrings.of("MSET");

    /**
     * Redis HMGET 命令常量
     * <p>
     * 返回哈希表中指定字段的值。
     * </p>
     *
     * @see <a href="https://redis.io/commands/hmget/">Redis HMGET Command</a>
     */
    public static final BulkStrings CONSTANTS_HMGET = BulkStrings.of("HMGET");

    /**
     * Redis HMSET 命令常量
     * <p>
     * 同时将多个 field-value (域 - 值) 对设置到哈希表中。
     * </p>
     *
     * @see <a href="https://redis.io/commands/hmset/">Redis HMSET Command</a>
     */
    public static final BulkStrings CONSTANTS_HMSET = BulkStrings.of("HMSET");

    /**
     * Redis SELECT 命令常量
     * <p>
     * 切换到指定的数据库。
     * </p>
     *
     * @see <a href="https://redis.io/commands/select/">Redis SELECT Command</a>
     */
    public static final BulkStrings CONSTANTS_SELECT = BulkStrings.of("SELECT");

    /**
     * Redis PUBLISH 命令常量
     * <p>
     * 将消息发布到指定的频道。
     * </p>
     *
     * @see <a href="https://redis.io/commands/publish/">Redis PUBLISH Command</a>
     */
    public static final BulkStrings CONSTANTS_PUBLISH = BulkStrings.of("PUBLISH");

    /**
     * Redis SUBSCRIBE 命令常量
     * <p>
     * 订阅给定的一个或多个频道的信息。
     * </p>
     *
     * @see <a href="https://redis.io/commands/subscribe/">Redis SUBSCRIBE Command</a>
     */
    public static final BulkStrings CONSTANTS_SUBSCRIBE = BulkStrings.of("SUBSCRIBE");

    /**
     * Redis UNSUBSCRIBE 命令常量
     * <p>
     * 取消订阅给定的一个或多个频道。
     * </p>
     *
     * @see <a href="https://redis.io/commands/unsubscribe/">Redis UNSUBSCRIBE Command</a>
     */
    public static final BulkStrings CONSTANTS_UNSUBSCRIBE = BulkStrings.of("UNSUBSCRIBE");

    /**
     * Redis PSUBSCRIBE 命令常量
     * <p>
     * 订阅一个或多个符合给定模式的频道。
     * </p>
     *
     * @see <a href="https://redis.io/commands/psubscribe/">Redis PSUBSCRIBE Command</a>
     */
    public static final BulkStrings CONSTANTS_PSUBSCRIBE = BulkStrings.of("PSUBSCRIBE");

    /**
     * Redis PUNSUBSCRIBE 命令常量
     * <p>
     * 取消订阅所有给定模式的频道。
     * </p>
     *
     * @see <a href="https://redis.io/commands/punsubscribe/">Redis PUNSUBSCRIBE Command</a>
     */
    public static final BulkStrings CONSTANTS_PUNSUBSCRIBE = BulkStrings.of("PUNSUBSCRIBE");

    private final BulkStrings[] arrays;

    public SimpleCommand(BulkStrings... arrays) {
        this.arrays = arrays;
    }

    public SimpleCommand(BulkStrings cmd, String key, String... values) {
        this.arrays = new BulkStrings[values.length + 2];
        arrays[0] = cmd;
        arrays[1] = RESP.ofString(key);
        for (int i = 0; i < values.length; i++) {
            arrays[i + 2] = RESP.ofString(values[i]);
        }
    }


    public SimpleCommand(BulkStrings cmd, String[] keys) {
        this.arrays = new BulkStrings[keys.length + 1];
        arrays[0] = cmd;
        for (int i = 0; i < keys.length; i++) {
            arrays[i + 1] = RESP.ofString(keys[i]);
        }
    }

    @Override
    protected List<BulkStrings> buildParams() {
        throw new UnsupportedOperationException();
    }

    public void writeTo(WriteBuffer writeBuffer) throws IOException {
        // 写入数组类型标识符
        writeBuffer.write(RESP.RESP_DATA_TYPE_ARRAY);
        // 写入数组元素个数
        RESP.writeInt(writeBuffer, arrays.length);
        // 逐个写入数组元素
        for (RESP item : arrays) {
            item.writeTo(writeBuffer);
        }
    }
}
