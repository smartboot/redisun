package tech.smartboot.redisun;

import io.github.smartboot.socket.buffer.BufferPagePool;
import io.github.smartboot.socket.extension.multiplex.MultiplexClient;
import io.github.smartboot.socket.transport.AioQuickClient;
import io.github.smartboot.socket.transport.AioSession;
import tech.smartboot.redisun.cmd.ExpireCommand;
import tech.smartboot.redisun.cmd.GetCommand;
import tech.smartboot.redisun.cmd.HelloCommand;
import tech.smartboot.redisun.cmd.SetCommand;
import tech.smartboot.redisun.cmd.ZRangeCommand;
import tech.smartboot.redisun.resp.Arrays;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.Doubles;
import tech.smartboot.redisun.resp.Integers;
import tech.smartboot.redisun.resp.Nulls;
import tech.smartboot.redisun.resp.RESP;
import tech.smartboot.redisun.resp.SimpleStrings;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Redisun客户端主类
 * 提供了与Redis服务器交互的高级API接口
 *
 * @author 三刀
 * @version v1.0 2025/10/21
 */
public final class Redisun {
    /**
     * Redisun客户端配置选项
     */
    private final RedisunOptions options;

    /**
     * 异步通道组，用于管理网络IO操作的线程池
     */
    private final AsynchronousChannelGroup group;

    /**
     * 多路复用客户端，用于管理与Redis服务器的连接
     */
    private final MultiplexClient<RESP> multiplexClient;

    private final BufferPagePool bufferPagePool = new BufferPagePool(Runtime.getRuntime().availableProcessors(), true);
    private volatile AioQuickClient currentClient;

    /**
     * 处理 Redis 的订阅和发布功能
     */
    private volatile RedisunPubSub pubSub;

    /**
     * 创建Redisun客户端实例的工厂方法
     *
     * @param opts Redisun配置选项的消费者函数，用于设置客户端参数
     * @return 配置完成的Redisun客户端实例
     */
    public static Redisun create(Consumer<RedisunOptions> opts) {
        Redisun redisun = new Redisun();
        opts.accept(redisun.options);
        return redisun;
    }

    /**
     * 私有构造函数，初始化Redisun客户端
     * 设置消息处理器和异步通道组
     */
    private Redisun() {
        // 创建Redis消息处理器
        RedisMessageProcessor processor = new RedisMessageProcessor();
        // 初始化多路复用选项，设置编解码器
        multiplexClient = new MultiplexClient<RESP>(processor, processor) {

            /**
             * 当创建新客户端连接时的回调方法
             * 用于执行HELLO命令进行身份验证和协议协商
             *
             * @param client 新创建的AioQuickClient实例
             */
            @Override
            protected void onNew(AioQuickClient client) {
                HelloCommand helloCommand = new HelloCommand();
                helloCommand.setUsername(options.getUsername());
                helloCommand.setPassword(options.getPassword());
                try {
                    get(execute(helloCommand));
                } catch (RedisunException e) {
                    String message = e.getMessage();
                    if (message != null && message.contains("ERR unknown command 'HELLO'")) {
                        throw new RedisunException("Please check the Redis version, which should be greater than or equal to 6.0.0");
                    } else {
                        throw e;
                    }
                }

                // 如果配置的数据库不为0，则自动切换数据库
                if (options.getDatabase() != 0) {
                    get(execute(new SimpleCommand(SimpleCommand.CONSTANTS_SELECT, RESP.ofString(String.valueOf(options.getDatabase())))));
                }
            }
        };
        multiplexClient.getMultiplexOptions().setBufferPool(bufferPagePool, bufferPagePool);
        multiplexClient.getMultiplexOptions().setReadBuffer(4096);
        multiplexClient.getMultiplexOptions().setWriteBuffer(4096, 8);
        multiplexClient.getMultiplexOptions().minConnections(4);
        multiplexClient.getMultiplexOptions().maxConnections(Runtime.getRuntime().availableProcessors());
        options = new RedisunOptions(multiplexClient.getMultiplexOptions());
        try {
            // 创建固定大小的线程池用于异步IO操作
            group = AsynchronousChannelGroup.withFixedThreadPool(Runtime.getRuntime().availableProcessors(), r -> new Thread(r, "redisun-thread"));
            // 将线程池设置到多路复用选项中
            multiplexClient.getMultiplexOptions().group(group);
        } catch (IOException e) {
            throw new RedisunException(e);
        }
    }

    /**
     * 向有序集合中添加一个或多个成员，或者更新已存在成员的分数
     *
     * @param key    有序集合的键
     * @param score  成员的分数
     * @param member 要添加的成员
     * @return 被成功添加的新成员数量
     */
    public int zadd(String key, double score, String member) {
        return get(asyncZadd(key, score, member));
    }

    /**
     * 异步向有序集合中添加一个或多个成员，或者更新已存在成员的分数
     *
     * @param key    有序集合的键
     * @param score  成员的分数
     * @param member 要添加的成员
     * @return 包含被成功添加的新成员数量的CompletableFuture
     */
    public CompletableFuture<Integer> asyncZadd(String key, double score, String member) {
        return execute(new SimpleCommand(SimpleCommand.CONSTANTS_ZADD, RESP.ofString(key), RESP.ofString(String.valueOf(score)), RESP.ofString(member))).thenApply(INTEGER_FUTURE);
    }

    /**
     * 移除有序集合中的一个或多个成员
     *
     * @param key     有序集合的键
     * @param members 要移除的一个或多个成员
     * @return 被成功移除的成员数量
     */
    public long zrem(String key, String... members) {
        return get(asyncZrem(key, members));
    }

    /**
     * 移除有序集合中的一个或多个成员（异步版本）
     *
     * @param key     有序集合的键
     * @param members 要移除的一个或多个成员
     * @return 被成功移除的成员数量
     */
    public CompletableFuture<Long> asyncZrem(String key, String... members) {
        BulkStrings[] params = new BulkStrings[members.length + 2];
        params[0] = SimpleCommand.CONSTANTS_ZREM;
        params[1] = RESP.ofString(key);
        for (int i = 0; i < members.length; i++) {
            params[i + 2] = RESP.ofString(members[i]);
        }
        return execute(new SimpleCommand(params)).thenApply(LONG_FUTURE);
    }

    /**
     * 返回有序集合中指定范围的成员
     *
     * @param key   有序集合的键
     * @param start 起始位置（包含）
     * @param stop  结束位置（包含）
     * @return 成员列表
     */
    public List<String> zrange(String key, long start, long stop) {
        return get(asyncZrange(key, start, stop));
    }

    public List<ZRangeCommand.Tuple> zrange(String key, long start, long stop, Consumer<ZRangeCommand> options) {
        return get(asyncZrange(key, start, stop, options));
    }

    /**
     * 返回有序集合中指定范围的成员（异步版本）
     *
     * @param key   有序集合的键
     * @param start 起始位置（包含）
     * @param stop  结束位置（包含）
     * @return 成员列表
     */
    public CompletableFuture<List<String>> asyncZrange(String key, long start, long stop) {
        return asyncZrange(key, start, stop, null).thenApply(bulkStrings -> bulkStrings.stream().map(ZRangeCommand.Tuple::getMember).collect(Collectors.toList()));
    }


    /**
     * 返回有序集合中指定范围的成员（异步版本）
     *
     * @param key     有序集合的键
     * @param start   起始位置（包含）
     * @param stop    结束位置（包含）
     * @param options ZRANGE命令的额外选项配置函数
     * @return 成员列表
     */
    public CompletableFuture<List<ZRangeCommand.Tuple>> asyncZrange(String key, long start, long stop, Consumer<ZRangeCommand> options) {
        ZRangeCommand cmd = new ZRangeCommand(key, String.valueOf(start), String.valueOf(stop));
        if (options != null) {
            options.accept(cmd);
        }
        return execute(cmd).thenApply(resp -> {
            if (resp instanceof Arrays) {
                List<RESP> resps = ((Arrays) resp).getValue();
                List<ZRangeCommand.Tuple> result = new ArrayList<>(resps.size());
                for (RESP r : resps) {
                    ZRangeCommand.Tuple tuple = new ZRangeCommand.Tuple();
                    if (r instanceof Arrays) {
                        Arrays arrays = (Arrays) r;
                        tuple.setMember(((BulkStrings) arrays.getValue().get(0)).getValue());
                        tuple.setScore(((Doubles) arrays.getValue().get(1)).getValue());
                    } else if (r instanceof BulkStrings) {
                        tuple.setMember(((BulkStrings) r).getValue());
                    }
                    result.add(tuple);
                }
                return result;
            }
            throw new RedisunException("invalid response:" + resp);
        });
    }

    /**
     * 返回有序集合中指定成员的分数
     *
     * @param key    有序集合的键
     * @param member 成员
     * @return 成员的分数，如果成员不存在则返回null
     */
    public Double zscore(String key, String member) {
        return get(asyncZscore(key, member));
    }

    /**
     * 返回有序集合中指定成员的分数（异步版本）
     *
     * @param key    有序集合的键
     * @param member 成员
     * @return 成员的分数，如果成员不存在则返回null
     */
    private CompletableFuture<Double> asyncZscore(String key, String member) {
        return execute(new SimpleCommand(SimpleCommand.CONSTANTS_ZSCORE, RESP.ofString(key), RESP.ofString(member))).thenApply(resp -> {
            if (resp instanceof BulkStrings) { //RESP 2
                return Double.valueOf(((BulkStrings) resp).getValue());
            } else if (resp instanceof Doubles) { //RESP 3
                return ((Doubles) resp).getValue();
            } else if (resp instanceof Nulls) {
                return null;
            }
            throw new RedisunException("invalid response:" + resp);
        });
    }

    /**
     * 获取指定键的值
     *
     * @param key 要获取值的键
     * @return 键对应的值，如果键不存在则返回null
     */
    public String get(String key) {
        return get(asyncGet(key));
    }

    /**
     * 获取指定键的值
     *
     * @param key 要获取值的键
     * @return 键对应的值，如果键不存在则返回null
     */
    public CompletableFuture<String> asyncGet(String key) {
        return execute(new GetCommand(key)).thenApply(BULK_STRING_FUTURE);
    }

    /**
     * 设置指定键的值
     *
     * @param key   要设置的键
     * @param value 要设置的值
     * @return 操作是否成功
     */
    public boolean set(String key, String value) {
        return set(key, value, null);
    }

    /**
     * 设置指定键的值，并可选择设置额外选项
     *
     * @param key     要设置的键
     * @param value   要设置的值
     * @param options Set命令的额外选项配置函数
     * @return 操作是否成功
     */
    public boolean set(String key, String value, Consumer<SetCommand> options) {
        return get(asyncSet(key, value, options));
    }

    /**
     * 异步设置指定键的值
     *
     * @param key   要设置的键
     * @param value 要设置的值
     * @return 包含操作是否成功的CompletableFuture
     */
    public CompletableFuture<Boolean> asyncSet(String key, String value) {
        return asyncSet(key, value, null);
    }

    /**
     * 异步设置指定键的值，并可选择设置额外选项
     *
     * @param key     要设置的键
     * @param value   要设置的值
     * @param options Set命令的额外选项配置函数
     * @return 包含操作是否成功的CompletableFuture
     */
    public CompletableFuture<Boolean> asyncSet(String key, String value, Consumer<SetCommand> options) {
        SetCommand cmd = new SetCommand(key, value);
        if (options != null) {
            options.accept(cmd);
        }
        return execute(cmd).thenApply(SET_CMD_FUTURE);
    }

    private static final Function<RESP, Boolean> SET_CMD_FUTURE = resp -> {
        if (resp == SimpleStrings.OK_RESP) {
            return true;
        } else if (resp instanceof SimpleStrings) {
            return SimpleStrings.OK.equals(resp.getValue());
        } else if (resp instanceof Nulls) {
            return false;
        } else {
            throw new RedisunException("invalid response:" + resp);
        }
    };

    private static final Function<RESP, Boolean> OK_FUTURE = resp -> {
        if (resp instanceof SimpleStrings) {
            return SimpleStrings.OK.equals(((SimpleStrings) resp).getValue());
        }
        throw new RedisunException("invalid response:" + resp);
    };

    private static final Function<RESP, Integer> INTEGER_FUTURE = resp -> {
        if (resp instanceof Integers) {
            return ((Integers) resp).getValue().intValue();
        }
        throw new RedisunException("invalid response:" + resp);
    };

    private static final Function<RESP, Long> LONG_FUTURE = resp -> {
        if (resp instanceof Integers) {
            return ((Integers) resp).getValue().longValue();
        }
        throw new RedisunException("invalid response:" + resp);
    };


    private static final Function<RESP, String> BULK_STRING_FUTURE = resp -> {
        if (resp instanceof Nulls) {
            return null;
        } else if (resp instanceof BulkStrings) {
            return ((BulkStrings) resp).getValue();
        }
        throw new RedisunException("invalid response:" + resp);
    };

    /**
     * 同时获取一个或多个 key 的值
     *
     * @param keys 要获取值的键列表
     * @return 包含所有键值的列表，不存在的键返回null
     */
    public List<String> mget(List<String> keys) {
        return get(asyncMget(keys));
    }

    /**
     * 同时获取一个或多个 key 的值（异步版本）
     *
     * @param keys 要获取值的键列表
     * @return 包含所有键值的列表，不存在的键返回null
     */
    public CompletableFuture<List<String>> asyncMget(List<String> keys) {
        if (keys.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        BulkStrings[] params = new BulkStrings[keys.size() + 1];
        params[0] = SimpleCommand.CONSTANTS_MGET;
        for (int i = 0; i < keys.size(); i++) {
            params[i + 1] = RESP.ofString(keys.get(i));
        }
        return execute(new SimpleCommand(params)).thenApply(resp -> {
            if (resp instanceof Arrays) {
                List<RESP> resps = ((Arrays) resp).getValue();
                List<String> result = new ArrayList<>(resps.size());
                for (RESP r : resps) {
                    if (r instanceof Nulls) {
                        result.add(null);
                    } else if (r instanceof BulkStrings) {
                        result.add(((BulkStrings) r).getValue());
                    } else {
                        throw new RedisunException("invalid response:" + r);
                    }
                }
                return result;
            }
            throw new RedisunException("invalid response:" + resp);
        });
    }

    /**
     * 返回当前数据库中键的数量
     *
     * @return 当前数据库中键的数量
     */
    public long dbsize() {
        return get(execute(new SimpleCommand(SimpleCommand.CONSTANTS_DBSIZE)).thenApply(INTEGER_FUTURE));
    }

    /**
     * 删除一个或多个键
     *
     * @param keys 要删除的键数组
     * @return 被成功删除的键数量
     */
    public int del(String... keys) {
        return del(java.util.Arrays.asList(keys));
    }

    /**
     * 删除一个或多个键
     *
     * @param keys 要删除的键列表
     * @return 被成功删除的键数量
     */
    public int del(List<String> keys) {
        return get(asyncDel(keys));
    }

    /**
     * 删除一个或多个键（异步版本）
     *
     * @param keys 要删除的键列表
     * @return 被成功删除的键数量的CompletableFuture
     */
    public CompletableFuture<Integer> asyncDel(List<String> keys) {
        BulkStrings[] params = new BulkStrings[keys.size() + 1];
        params[0] = SimpleCommand.CONSTANTS_DEL;
        for (int i = 0; i < keys.size(); i++) {
            params[i + 1] = RESP.ofString(keys.get(i));
        }
        return execute(new SimpleCommand(params)).thenApply(INTEGER_FUTURE);
    }

    private <T> T get(CompletableFuture<T> future) {
        try {
            return future.get();
        } catch (Throwable e) {
            while (e.getCause() != null) {
                e = e.getCause();
            }
            throw new RedisunException(e.getMessage());
        }
    }

    /**
     * 异步执行Redis命令
     *
     * @param command 要执行的Redis命令
     * @return 包含执行结果的CompletableFuture
     */
    private CompletableFuture<RESP> execute(Command command) {
        // 创建用于接收结果的CompletableFuture
        CompletableFuture<RESP> future = new CompletableFuture<>();
        AioQuickClient client = currentClient;
        AioSession session = null;
        RedisSession redisSession = null;
        try {
            // 获取可用的客户端连接
            if (client != null) {
                session = client.getSession();
                if (session != null && !session.isInvalid()) {
                    redisSession = session.getAttachment();
                    if (redisSession.load() > 1024) {
                        client = null;
                    }
                } else {
                    // session 无效
                    currentClient = null;
                    client = null;
                }
            }
            if (client == null) {
                client = multiplexClient.acquire();
                session = client.getSession();
                redisSession = session.getAttachment();
                if (redisSession.load() <= 1024) {
                    currentClient = client;
                    multiplexClient.reuse(client);
                } else {
                    AioQuickClient finalClient = client;
                    future.thenRun(() -> multiplexClient.reuse(finalClient));
                }
            }

            int offerCount = redisSession.incrOfferCount();
            int pollCount = redisSession.getPollCount();

            synchronized (client) {
                // 设置当前命令的future
                redisSession.offer(future);
                command.writeTo(session.writeBuffer());
            }

            // 刷新缓冲区，发送数据
            if (offerCount == redisSession.getOfferCount() && pollCount == redisSession.getPollCount()) {
                session.writeBuffer().flush();
            }
        } catch (Throwable e) {
            // 发生异常时完成future
            if (client != null) {
                multiplexClient.release(client);
            }
            future.completeExceptionally(e);
        }
        return future;
    }


    /**
     * 清空所有数据库中的所有键
     *
     * @return 操作是否成功
     */
    public boolean flushAll() {
        return get(execute(new SimpleCommand(SimpleCommand.CONSTANTS_FLUSHALL)).thenApply(OK_FUTURE));
    }

    /**
     * 清空当前数据库中的所有键
     *
     * @return 操作是否成功
     */
    public boolean flushDb() {
        return get(execute(new SimpleCommand(SimpleCommand.CONSTANTS_FLUSHDB)).thenApply(OK_FUTURE));
    }

    /**
     * 同时设置一个或多个 key-value 对
     *
     * @param items 要设置的键值列表
     * @return 操作是否成功
     */
    public boolean mset(Map<String, String> items) {
        return get(asyncMset(items));
    }

    /**
     * 同时设置一个或多个 key-value 对（异步版本）
     *
     * @param items 要设置的键值列表
     * @return 操作是否成功
     */
    public CompletableFuture<Boolean> asyncMset(Map<String, String> items) {
        if (items.isEmpty()) {
            return CompletableFuture.completedFuture(true);
        }
        BulkStrings[] params = new BulkStrings[items.size() * 2 + 1];
        params[0] = SimpleCommand.CONSTANTS_MSET;
        int i = 1;
        for (Map.Entry<String, String> entry : items.entrySet()) {
            params[i++] = RESP.ofString(entry.getKey());
            params[i++] = RESP.ofString(entry.getValue());
        }
        return execute(new SimpleCommand(params)).thenApply(OK_FUTURE);
    }

    /**
     * 关闭Redisun客户端，释放资源
     */
    public void close() {
        if (pubSub != null) {
            pubSub.close();
        }
        multiplexClient.close();
        if (group != null) {
            group.shutdown();
        }
        bufferPagePool.release();
    }

    /**
     * 将一个或多个成员加入到集合中
     *
     * @param key     集合的键
     * @param members 要添加的一个或多个成员
     * @return 被成功添加到集合中的新元素数量，不包括已被添加的元素
     */
    public int sadd(String key, String... members) {
        return get(asyncSadd(key, members));
    }

    /**
     * 将一个或多个成员加入到集合中（异步版本）
     *
     * @param key     集合的键
     * @param members 要添加的一个或多个成员
     * @return 被成功添加到集合中的新元素数量，不包括已被添加的元素
     */
    public CompletableFuture<Integer> asyncSadd(String key, String... members) {
        BulkStrings[] params = new BulkStrings[members.length + 2];
        params[0] = SimpleCommand.CONSTANTS_SADD;
        params[1] = RESP.ofString(key);
        for (int i = 0; i < members.length; i++) {
            params[i + 2] = RESP.ofString(members[i]);
        }
        return execute(new SimpleCommand(params)).thenApply(INTEGER_FUTURE);
    }

    /**
     * 将一个或多个值插入到列表的头部(左边)
     *
     * @param key    列表的键
     * @param values 要插入的一个或多个值
     * @return 执行后列表的长度
     */
    public long lpush(String key, String... values) {
        return get(asyncLpush(key, values));
    }

    /**
     * 将一个或多个值插入到列表的头部(左边)（异步版本）
     *
     * @param key    列表的键
     * @param values 要插入的一个或多个值
     * @return 执行后列表的长度
     */
    public CompletableFuture<Long> asyncLpush(String key, String... values) {
        BulkStrings[] params = new BulkStrings[values.length + 2];
        params[0] = SimpleCommand.CONSTANTS_LPUSH;
        params[1] = RESP.ofString(key);
        for (int i = 0; i < values.length; i++) {
            params[i + 2] = RESP.ofString(values[i]);
        }
        return execute(new SimpleCommand(params)).thenApply(LONG_FUTURE);
    }

    /**
     * 将一个或多个值插入到列表的尾部(右边)
     *
     * @param key    列表的键
     * @param values 要插入的一个或多个值
     * @return 执行后列表的长度
     */
    public long rpush(String key, String... values) {
        return get(asyncRpush(key, values));
    }

    /**
     * 将一个或多个值插入到列表的尾部(右边)（异步版本）
     *
     * @param key    列表的键
     * @param values 要插入的一个或多个值
     * @return 执行后列表的长度
     */
    public CompletableFuture<Long> asyncRpush(String key, String... values) {
        BulkStrings[] params = new BulkStrings[values.length + 2];
        params[0] = SimpleCommand.CONSTANTS_RPUSH;
        params[1] = RESP.ofString(key);
        for (int i = 0; i < values.length; i++) {
            params[i + 2] = RESP.ofString(values[i]);
        }
        return execute(new SimpleCommand(params)).thenApply(LONG_FUTURE);
    }

    /**
     * 返回哈希表中指定字段的值
     *
     * @param key   哈希表的键
     * @param field 要获取值的字段
     * @return 返回给定字段的值，如果字段不存在则返回null
     */
    public String hget(String key, String field) {
        return get(asyncHget(key, field));
    }

    /**
     * 返回哈希表中指定字段的值（异步版本）
     *
     * @param key   哈希表的键
     * @param field 要获取值的字段
     * @return 返回给定字段的值，如果字段不存在则返回null
     */
    public CompletableFuture<String> asyncHget(String key, String field) {
        return execute(new SimpleCommand(SimpleCommand.CONSTANTS_HGET, RESP.ofString(key), RESP.ofString(field))).thenApply(BULK_STRING_FUTURE);
    }

    /**
     * 将哈希表 key 中的字段 field 的值设为 value
     *
     * @param key   哈希表的键
     * @param field 哈希表中的字段
     * @param value 要设置的值
     * @return 如果字段是哈希表中的一个新建字段，并且值设置成功，返回1；
     * 如果哈希表中域字段已经存在且旧值已被新值覆盖，返回0
     */
    public int hset(String key, String field, String value) {
        return get(asyncHset(key, field, value));
    }

    /**
     * 将哈希表 key 中的字段 field 的值设为 value（异步版本）
     *
     * @param key   哈希表的键
     * @param field 哈希表中的字段
     * @param value 要设置的值
     * @return 如果字段是哈希表中的一个新建字段，并且值设置成功，返回 1；
     * 如果哈希表中域字段已经存在且旧值已被新值覆盖，返回 0
     */
    public CompletableFuture<Integer> asyncHset(String key, String field, String value) {
        return execute(new SimpleCommand(SimpleCommand.CONSTANTS_HSET, RESP.ofString(key), RESP.ofString(field), RESP.ofString(value))).thenApply(INTEGER_FUTURE);
    }

    /**
     * 同时将多个 field-value (域 - 值) 对设置到哈希表中
     *
     * @param key  哈希表的键
     * @param hash 包含字段 - 值对的 Map
     * @return 操作是否成功
     */
    public boolean hmset(String key, Map<String, String> hash) {
        return get(asyncHmset(key, hash));
    }

    /**
     * 同时将多个 field-value (域 - 值) 对设置到哈希表中（异步版本）
     *
     * @param key  哈希表的键
     * @param hash 包含字段 - 值对的 Map
     * @return 操作是否成功
     */
    public CompletableFuture<Boolean> asyncHmset(String key, Map<String, String> hash) {
        BulkStrings[] params = new BulkStrings[hash.size() * 2 + 2];
        params[0] = SimpleCommand.CONSTANTS_HMSET;
        params[1] = RESP.ofString(key);
        int i = 2;
        for (Map.Entry<String, String> entry : hash.entrySet()) {
            params[i++] = RESP.ofString(entry.getKey());
            params[i++] = RESP.ofString(entry.getValue());
        }
        return execute(new SimpleCommand(params)).thenApply(OK_FUTURE);
    }

    /**
     * 返回哈希表中指定字段的值
     *
     * @param key    哈希表的键
     * @param fields 要获取值的字段列表
     * @return 包含所有字段值的列表，不存在的字段返回 null
     */
    public List<String> hmget(String key, List<String> fields) {
        return get(asyncHmget(key, fields));
    }

    /**
     * 返回哈希表中指定字段的值
     *
     * @param key    哈希表的键
     * @param fields 要获取值的字段数组
     * @return 包含所有字段值的列表，不存在的字段返回 null
     */
    public List<String> hmget(String key, String... fields) {
        return hmget(key, java.util.Arrays.asList(fields));
    }

    /**
     * 返回哈希表中指定字段的值（异步版本）
     *
     * @param key    哈希表的键
     * @param fields 要获取值的字段列表
     * @return 包含所有字段值的列表，不存在的字段返回 null
     */
    public CompletableFuture<List<String>> asyncHmget(String key, List<String> fields) {
        BulkStrings[] params = new BulkStrings[fields.size() + 2];
        params[0] = SimpleCommand.CONSTANTS_HMGET;
        params[1] = RESP.ofString(key);
        for (int i = 0; i < fields.size(); i++) {
            params[i + 2] = RESP.ofString(fields.get(i));
        }
        return execute(new SimpleCommand(params)).thenApply(resp -> {
            if (resp instanceof Arrays) {
                List<RESP> resps = ((Arrays) resp).getValue();
                List<String> result = new ArrayList<>(resps.size());
                for (RESP r : resps) {
                    if (r instanceof Nulls) {
                        result.add(null);
                    } else if (r instanceof BulkStrings) {
                        result.add(((BulkStrings) r).getValue());
                    } else {
                        throw new RedisunException("invalid response:" + r);
                    }
                }
                return result;
            }
            throw new RedisunException("invalid response:" + resp);
        });
    }

    /**
     * 返回哈希表中指定字段的值（异步版本）
     *
     * @param key    哈希表的键
     * @param fields 要获取值的字段数组
     * @return 包含所有字段值的列表，不存在的字段返回 null
     */
    public CompletableFuture<List<String>> asyncHmget(String key, String... fields) {
        return asyncHmget(key, java.util.Arrays.asList(fields));
    }

    /**
     * 返回 key 所储存的字符串值的长度
     *
     * @param key 要获取长度的键
     * @return 字符串值的长度
     */
    public int strlen(String key) {
        return get(asyncStrlen(key));
    }

    /**
     * 返回 key 所储存的字符串值的长度（异步版本）
     *
     * @param key 要获取长度的键
     * @return 字符串值的长度
     */
    public CompletableFuture<Integer> asyncStrlen(String key) {
        return execute(new SimpleCommand(SimpleCommand.CONSTANTS_STRLEN, RESP.ofString(key))).thenApply(INTEGER_FUTURE);
    }

    /**
     * 如果 key 已经存在并且是一个字符串，该命令将 value 追加到 key 原来的值的末尾
     *
     * @param key   要追加的键
     * @param value 要追加的值
     * @return 追加操作后 key 中字符串的长度
     */
    public int append(String key, String value) {
        return get(asyncAppend(key, value));
    }

    /**
     * 如果 key 已经存在并且是一个字符串，该命令将 value 追加到 key 原来的值的末尾（异步版本）
     * 如果 key 不存在，APPEND 就简单地将给定 key 设为 value
     *
     * @param key   要追加的键
     * @param value 要追加的值
     * @return 追加操作后 key 中字符串的长度
     */
    public CompletableFuture<Integer> asyncAppend(String key, String value) {
        return execute(new SimpleCommand(SimpleCommand.CONSTANTS_APPEND, BulkStrings.ofString(key), BulkStrings.ofString(value))).thenApply(INTEGER_FUTURE);
    }

    /**
     * 将 key 中储存的数字值减一
     *
     * @param key 要减少的键
     * @return 执行命令后 key 的值
     */
    public long decr(String key) {
        return get(asyncDecr(key));
    }

    /**
     * 将 key 中储存的数字值减一（异步版本）
     *
     * @param key 要减少的键
     * @return 执行命令后 key 的值
     */
    public CompletableFuture<Long> asyncDecr(String key) {
        return execute(new SimpleCommand(SimpleCommand.CONSTANTS_DECR, RESP.ofString(key))).thenApply(LONG_FUTURE);
    }

    /**
     * 将 key 所储存的值减去给定的减量值（decrement）
     *
     * @param key       要减少的键
     * @param decrement 减量值
     * @return 执行命令后 key 的值
     */
    public long decrBy(String key, long decrement) {
        return get(asyncDecrBy(key, decrement));
    }

    /**
     * 将 key 所储存的值减去给定的减量值（decrement）（异步版本）
     *
     * @param key       要减少的键
     * @param decrement 减量值
     * @return 执行命令后 key 的值
     */
    public CompletableFuture<Long> asyncDecrBy(String key, long decrement) {
        return execute(new SimpleCommand(SimpleCommand.CONSTANTS_DECRBY, RESP.ofString(key), RESP.ofString(String.valueOf(decrement)))).thenApply(LONG_FUTURE);
    }

    /**
     * 将 key 中储存的数字值增一
     *
     * @param key 要增加的键
     * @return 执行命令后 key 的值
     */
    public long incr(String key) {
        return get(asyncIncr(key));
    }

    /**
     * 将 key 中储存的数字值增一（异步版本）
     *
     * @param key 要增加的键
     * @return 执行命令后 key 的值
     */
    public CompletableFuture<Long> asyncIncr(String key) {
        return execute(new SimpleCommand(SimpleCommand.CONSTANTS_INCR, RESP.ofString(key))).thenApply(LONG_FUTURE);
    }

    /**
     * 将 key 所储存的值加上给定的增量值（increment）
     *
     * @param key       要增加的键
     * @param increment 增量值
     * @return 执行命令后 key 的值
     */
    public long incrBy(String key, long increment) {
        return get(asyncIncrBy(key, increment));
    }

    /**
     * 将 key 所储存的值加上给定的增量值（increment）（异步版本）
     *
     * @param key       要增加的键
     * @param increment 增量值
     * @return 执行命令后 key 的值
     */
    public CompletableFuture<Long> asyncIncrBy(String key, long increment) {
        return execute(new SimpleCommand(SimpleCommand.CONSTANTS_INCRBY, RESP.ofString(key), RESP.ofString(String.valueOf(increment)))).thenApply(LONG_FUTURE);
    }

    /**
     * 检查给定键是否存在
     *
     * @param keys 要检查的键
     * @return 存在的键数量
     */
    public int exists(String... keys) {
        return get(asyncExists(keys));
    }

    /**
     * 异步检查给定键是否存在
     *
     * @param keys 要检查的键
     * @return 包含存在键数量的CompletableFuture
     */
    public CompletableFuture<Integer> asyncExists(String... keys) {
        BulkStrings[] params = new BulkStrings[keys.length + 1];
        params[0] = SimpleCommand.CONSTANTS_EXISTS;
        for (int i = 0; i < keys.length; i++) {
            params[i + 1] = RESP.ofString(keys[i]);
        }
        return execute(new SimpleCommand(params)).thenApply(INTEGER_FUTURE);
    }

    /**
     * 为给定 key 设置过期时间，以秒计
     *
     * @param key     要设置过期时间的键
     * @param seconds 过期时间（秒）
     * @return 设置成功返回 1，否则返回 0
     */
    public int expire(String key, int seconds) {
        return expire(key, seconds, null);
    }

    /**
     * 为给定 key 设置过期时间，以秒计，并支持选项
     *
     * @param key     要设置过期时间的键
     * @param seconds 过期时间（秒）
     * @param options EXPIRE命令的额外选项配置函数
     * @return 设置成功返回 1，否则返回 0
     */
    public int expire(String key, int seconds, Consumer<ExpireCommand> options) {
        return get(asyncExpire(key, seconds, options));
    }

    /**
     * 为给定 key 设置过期时间，以秒计（异步版本），并支持选项
     *
     * @param key     要设置过期时间的键
     * @param seconds 过期时间（秒）
     * @param options EXPIRE命令的额外选项配置函数
     * @return 设置成功返回 1，否则返回 0
     */
    private CompletableFuture<Integer> asyncExpire(String key, int seconds, Consumer<ExpireCommand> options) {
        ExpireCommand cmd = new ExpireCommand(key, seconds);
        if (options != null) {
            options.accept(cmd);
        }
        return execute(cmd).thenApply(INTEGER_FUTURE);
    }

    /**
     * 以秒为单位返回 key 的剩余过期时间
     *
     * @param key 要查询过期时间的键
     * @return 剩余过期时间（秒），-1表示没有设置过期时间，-2表示键不存在
     */
    public long ttl(String key) {
        return get(asyncTtl(key));
    }

    /**
     * 以秒为单位返回 key 的剩余过期时间（异步版本）
     *
     * @param key 要查询过期时间的键
     * @return 剩余过期时间（秒），-1表示没有设置过期时间，-2表示键不存在
     */
    public CompletableFuture<Long> asyncTtl(String key) {
        return execute(new SimpleCommand(SimpleCommand.CONSTANTS_TTL, RESP.ofString(key))).thenApply(LONG_FUTURE);
    }

    /**
     * 返回 key 所储存的值的类型
     *
     * @param key 要查询类型的键
     * @return 键值的类型
     */
    public String type(String key) {
        return get(asyncType(key));
    }

    /**
     * 返回 key 所储存的值的类型（异步版本）
     *
     * @param key 要查询类型的键
     * @return 键值的类型
     */
    public CompletableFuture<String> asyncType(String key) {
        return execute(new SimpleCommand(SimpleCommand.CONSTANTS_TYPE, RESP.ofString(key))).thenApply(resp -> {
            if (resp instanceof SimpleStrings) {
                return ((SimpleStrings) resp).getValue();
            }
            throw new RedisunException("invalid response:" + resp);
        });
    }

    /**
     * 移除并返回列表的头部(左边)第一个元素
     *
     * @param key 列表的键
     * @return 列表的头部元素，如果列表为空则返回null
     */
    public String lpop(String key) {
        return get(asyncLpop(key));
    }

    /**
     * 移除并返回列表的头部(左边)第一个元素（异步版本）
     *
     * @param key 列表的键
     * @return 列表的头部元素，如果列表为空则返回null
     */
    public CompletableFuture<String> asyncLpop(String key) {
        return execute(new SimpleCommand(SimpleCommand.CONSTANTS_LPOP, RESP.ofString(key))).thenApply(BULK_STRING_FUTURE);
    }

    /**
     * 移除并返回列表的尾部(右边)最后一个元素
     *
     * @param key 列表的键
     * @return 列表的尾部元素，如果列表为空则返回null
     */
    public String rpop(String key) {
        return get(asyncRpop(key));
    }

    /**
     * 移除并返回列表的尾部(右边)最后一个元素（异步版本）
     *
     * @param key 列表的键
     * @return 列表的尾部元素，如果列表为空则返回null
     */
    public CompletableFuture<String> asyncRpop(String key) {
        return execute(new SimpleCommand(SimpleCommand.CONSTANTS_RPOP, RESP.ofString(key))).thenApply(BULK_STRING_FUTURE);
    }

    /**
     * 发布消息到指定频道
     *
     * @param channel 频道名称
     * @param message 要发布的消息
     * @return 接收到此消息的客户端数量
     */
    public int publish(String channel, String message) {
        return get(asyncPublish(channel, message));
    }

    /**
     * 异步发布消息到指定频道
     *
     * @param channel 频道名称
     * @param message 要发布的消息
     * @return 接收到此消息的客户端数量
     */
    public CompletableFuture<Integer> asyncPublish(String channel, String message) {
        return execute(new SimpleCommand(SimpleCommand.CONSTANTS_PUBLISH, RESP.ofString(channel), RESP.ofString(message))).thenApply(INTEGER_FUTURE);
    }

    private synchronized RedisunPubSub redisunPubSub() throws Throwable {
        if (pubSub == null) {
            AioQuickClient client = multiplexClient.acquire();
            //避免处于订阅状态的会话被占用
            if (client == currentClient) {
                currentClient = null;
            }
            pubSub = new RedisunPubSub(this, client);
            AioSession session = client.getSession();
            RedisSession redisSession = session.getAttachment();
            redisSession.setPubSub(pubSub);
        }
        return pubSub;
    }

    synchronized void releasePubSub() {
        if (pubSub == null) {
            return;
        }
        AioSession session = pubSub.getClient().getSession();
        RedisSession redisSession = session.getAttachment();
        redisSession.setPubSub(null);
        multiplexClient.reuse(pubSub.getClient());
        pubSub = null;
    }

    /**
     * 取消订阅给定的一个或多个频道
     *
     * @param channels 要取消订阅的频道列表，如果为空则取消所有 频道订阅
     */
    public void unsubscribe(String... channels) {
        try {
            if (pubSub == null) {
                return;
            }
            AioSession session = pubSub.getClient().getSession();
            // 执行订阅命令
            synchronized (pubSub.getClient()) {
                BulkStrings[] params = new BulkStrings[channels.length + 1];
                params[0] = SimpleCommand.CONSTANTS_UNSUBSCRIBE;
                for (int i = 0; i < channels.length; i++) {
                    params[i + 1] = RESP.ofString(channels[i]);
                }
                new SimpleCommand(params).writeTo(session.writeBuffer());
            }
            session.writeBuffer().flush();
        } catch (Throwable e) {
            throw new RedisunException(e);
        }
    }

    /**
     * 订阅给定的一个或多个频道
     * 注意：一个 Redisun 对象只分配一个TCP连接进行订阅
     *
     * @param pubsub   消息回调处理类
     * @param channels 要订阅的频道列表
     */
    public void subscribe(Subscriber pubsub, String... channels) {
        if (channels == null || channels.length == 0) {
            throw new RedisunException("Channels must not be null or empty");
        }
        if (pubsub == null) {
            throw new RedisunException("Subscriber must not be null");
        }
        try {
            RedisunPubSub redisunPubSub = redisunPubSub();
            redisunPubSub.subscribe(pubsub, channels);
            AioSession session = redisunPubSub.getClient().getSession();
            // 执行频道订阅命令
            synchronized (redisunPubSub.getClient()) {
                BulkStrings[] params = new BulkStrings[channels.length + 1];
                params[0] = SimpleCommand.CONSTANTS_SUBSCRIBE;
                for (int i = 0; i < channels.length; i++) {
                    params[i + 1] = RESP.ofString(channels[i]);
                }
                new SimpleCommand(params).writeTo(session.writeBuffer());
            }
            session.writeBuffer().flush();
        } catch (Throwable e) {
            throw new RedisunException(e);
        }
    }

    /**
     * 订阅给定的一个或多个频道的模式
     * 注意：一个 Redisun 对象只分配一个TCP连接进行订阅
     *
     * @param pubsub   模式匹配消息回调处理类
     * @param patterns 要订阅的频道模式列表（支持通配符 * 和 ?）
     */
    public void pSubscribe(Subscriber pubsub, String... patterns) {
        if (patterns == null || patterns.length == 0) {
            throw new RedisunException("Patterns must not be null or empty");
        }
        if (pubsub == null) {
            throw new RedisunException("PSubscribe must not be null");
        }
        try {
            RedisunPubSub redisunPubSub = redisunPubSub();
            redisunPubSub.pSubscribe(pubsub, patterns);
            AioSession session = redisunPubSub.getClient().getSession();
            // 执行模式订阅命令
            synchronized (redisunPubSub.getClient()) {
                BulkStrings[] params = new BulkStrings[patterns.length + 1];
                params[0] = SimpleCommand.CONSTANTS_PSUBSCRIBE;
                for (int i = 0; i < patterns.length; i++) {
                    params[i + 1] = RESP.ofString(patterns[i]);
                }
                new SimpleCommand(params).writeTo(session.writeBuffer());
            }
            session.writeBuffer().flush();
        } catch (Throwable e) {
            throw new RedisunException(e);
        }
    }

    /**
     * 取消订阅给定的一个或多个频道的模式
     *
     * @param patterns 要取消订阅的模式列表，如果为空则取消所有 模式订阅
     */
    public void pUnsubscribe(String... patterns) {
        try {
            if (pubSub == null) {
                return;
            }
            AioSession session = pubSub.getClient().getSession();
            // 执行订阅命令
            synchronized (pubSub.getClient()) {
                BulkStrings[] params = new BulkStrings[patterns.length + 1];
                params[0] = SimpleCommand.CONSTANTS_PUNSUBSCRIBE;
                for (int i = 0; i < patterns.length; i++) {
                    params[i + 1] = RESP.ofString(patterns[i]);
                }
                new SimpleCommand(params).writeTo(session.writeBuffer());
            }
            session.writeBuffer().flush();
        } catch (Throwable e) {
            throw new RedisunException(e);
        }
    }
}
