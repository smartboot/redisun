package tech.smartboot.redisun;

import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.extension.multiplex.MultiplexClient;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;
import tech.smartboot.redisun.cmd.AppendCommand;
import tech.smartboot.redisun.cmd.DBSizeCommand;
import tech.smartboot.redisun.cmd.DecrByCommand;
import tech.smartboot.redisun.cmd.DecrCommand;
import tech.smartboot.redisun.cmd.DelCommand;
import tech.smartboot.redisun.cmd.ExistsCommand;
import tech.smartboot.redisun.cmd.ExpireCommand;
import tech.smartboot.redisun.cmd.FlushAllCommand;
import tech.smartboot.redisun.cmd.FlushDbCommand;
import tech.smartboot.redisun.cmd.GetCommand;
import tech.smartboot.redisun.cmd.HGetCommand;
import tech.smartboot.redisun.cmd.HSetCommand;
import tech.smartboot.redisun.cmd.HelloCommand;
import tech.smartboot.redisun.cmd.IncrByCommand;
import tech.smartboot.redisun.cmd.IncrCommand;
import tech.smartboot.redisun.cmd.LPopCommand;
import tech.smartboot.redisun.cmd.LPushCommand;
import tech.smartboot.redisun.cmd.MGetCommand;
import tech.smartboot.redisun.cmd.MSetCommand;
import tech.smartboot.redisun.cmd.RPopCommand;
import tech.smartboot.redisun.cmd.RPushCommand;
import tech.smartboot.redisun.cmd.SAddCommand;
import tech.smartboot.redisun.cmd.SelectCommand;
import tech.smartboot.redisun.cmd.SetCommand;
import tech.smartboot.redisun.cmd.StrlenCommand;
import tech.smartboot.redisun.cmd.TtlCommand;
import tech.smartboot.redisun.cmd.TypeCommand;
import tech.smartboot.redisun.cmd.ZAddCommand;
import tech.smartboot.redisun.cmd.ZRangeCommand;
import tech.smartboot.redisun.cmd.ZRemCommand;
import tech.smartboot.redisun.cmd.ZScoreCommand;
import tech.smartboot.redisun.resp.Arrays;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.Doubles;
import tech.smartboot.redisun.resp.Integers;
import tech.smartboot.redisun.resp.Nulls;
import tech.smartboot.redisun.resp.RESP;
import tech.smartboot.redisun.resp.SimpleErrors;
import tech.smartboot.redisun.resp.SimpleStrings;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.ArrayList;
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
                syncExecute(helloCommand);

                // 如果配置的数据库不为0，则自动切换数据库
                if (options.getDatabase() != 0) {
                    syncExecute(new SelectCommand(options.getDatabase()));
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
        try {
            return asyncZadd(key, score, member).get();
        } catch (Throwable e) {
            throw new RedisunException(e);
        }
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
        return execute(new ZAddCommand(key, score, member)).thenApply(resp -> {
            if (resp instanceof Integers) {
                return ((Integers) resp).getValue();
            }
            throw new RedisunException("invalid response:" + resp);
        });
    }

    /**
     * 移除有序集合中的一个或多个成员
     *
     * @param key     有序集合的键
     * @param members 要移除的一个或多个成员
     * @return 被成功移除的成员数量
     */
    public long zrem(String key, String... members) {
        try {
            return asyncZrem(key, members).get();
        } catch (Throwable e) {
            throw new RedisunException(e);
        }
    }

    /**
     * 移除有序集合中的一个或多个成员（异步版本）
     *
     * @param key     有序集合的键
     * @param members 要移除的一个或多个成员
     * @return 被成功移除的成员数量
     */
    public CompletableFuture<Long> asyncZrem(String key, String... members) {
        return execute(new ZRemCommand(key, members)).thenApply(resp -> {
            if (resp instanceof Integers) {
                return ((Integers) resp).getValue().longValue();
            }
            throw new RedisunException("invalid response:" + resp);
        });
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
        try {
            return asyncZrange(key, start, stop).get();
        } catch (Throwable e) {
            throw new RedisunException(e);
        }
    }

    public List<ZRangeCommand.Tuple> zrange(String key, long start, long stop, Consumer<ZRangeCommand> options) {
        try {
            return asyncZrange(key, start, stop, options).get();
        } catch (Throwable e) {
            throw new RedisunException(e);
        }
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
            if (resp instanceof tech.smartboot.redisun.resp.Arrays) {
                List<RESP> resps = ((tech.smartboot.redisun.resp.Arrays) resp).getValue();
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
        try {
            return asyncZscore(key, member).get();
        } catch (Throwable e) {
            throw new RedisunException(e);
        }
    }

    /**
     * 返回有序集合中指定成员的分数（异步版本）
     *
     * @param key    有序集合的键
     * @param member 成员
     * @return 成员的分数，如果成员不存在则返回null
     */
    private CompletableFuture<Double> asyncZscore(String key, String member) {
        return execute(new ZScoreCommand(key, member)).thenApply(resp -> {
            if (resp instanceof BulkStrings) {
                return Double.valueOf(((BulkStrings) resp).getValue());
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
        try {
            return asyncGet(key).get();
        } catch (Throwable e) {
            throw new RedisunException(e);
        }
    }

    /**
     * 获取指定键的值
     *
     * @param key 要获取值的键
     * @return 键对应的值，如果键不存在则返回null
     */
    public CompletableFuture<String> asyncGet(String key) {
        return execute(new GetCommand(key)).thenApply(r -> {
            if (r instanceof BulkStrings) {
                return ((BulkStrings) r).getValue();
            } else if (r instanceof Nulls) {
                return null;
            }
            throw new RedisunException("invalid response:" + r);
        });
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
        try {
            return asyncSet(key, value, options).get();
        } catch (Throwable e) {
            throw new RedisunException(e);
        }
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

    /**
     * 同时获取一个或多个 key 的值
     *
     * @param keys 要获取值的键列表
     * @return 包含所有键值的列表，不存在的键返回null
     */
    public List<String> mget(List<String> keys) {
        try {
            return asyncMget(keys).get();
        } catch (Throwable e) {
            throw new RedisunException(e);
        }
    }

    /**
     * 同时获取一个或多个 key 的值（异步版本）
     *
     * @param keys 要获取值的键列表
     * @return 包含所有键值的列表，不存在的键返回null
     */
    public CompletableFuture<List<String>> asyncMget(List<String> keys) {
        return execute(new MGetCommand(keys)).thenApply(resp -> {
            if (resp instanceof tech.smartboot.redisun.resp.Arrays) {
                List<RESP> resps = ((tech.smartboot.redisun.resp.Arrays) resp).getValue();
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
        RESP r = syncExecute(new DBSizeCommand());
        if (r instanceof Integers) {
            return ((Integers) r).getValue();
        }
        throw new RedisunException("invalid response:" + r);
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
        RESP r = syncExecute(new DelCommand(keys));
        if (r instanceof Integers) {
            return ((Integers) r).getValue();
        }
        throw new RedisunException("invalid response:" + r);
    }


    /**
     * 同步执行Redis命令
     *
     * @param command 要执行的Redis命令
     * @return 命令执行结果
     */
    private RESP syncExecute(Command command) {
        RESP resp;
        try {
            // 执行命令并等待结果
            resp = execute(command).get();
        } catch (Throwable e) {
            throw new RedisunException(e);
        }
        // 处理错误响应
        if (resp instanceof SimpleErrors) {
            throw new RedisunException(((SimpleErrors) resp).getValue());
        }
        return resp;
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
                    // session无效
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
        RESP r = syncExecute(new FlushAllCommand());
        if (r instanceof SimpleStrings) {
            return SimpleStrings.OK.equals(((SimpleStrings) r).getValue());
        }
        throw new RedisunException("invalid response:" + r);
    }

    /**
     * 清空当前数据库中的所有键
     *
     * @return 操作是否成功
     */
    public boolean flushDb() {
        RESP r = syncExecute(new FlushDbCommand());
        if (r instanceof SimpleStrings) {
            return SimpleStrings.OK.equals(((SimpleStrings) r).getValue());
        }
        throw new RedisunException("invalid response:" + r);
    }

    /**
     * 同时设置一个或多个 key-value 对
     *
     * @param items 要设置的键值列表
     * @return 操作是否成功
     */
    public boolean mset(Map<String, String> items) {
        try {
            return asyncMset(items).get();
        } catch (Throwable e) {
            throw new RedisunException(e);
        }
    }

    /**
     * 同时设置一个或多个 key-value 对（异步版本）
     *
     * @param items 要设置的键值列表
     * @return 操作是否成功
     */
    public CompletableFuture<Boolean> asyncMset(Map<String, String> items) {
        return execute(new MSetCommand(items)).thenApply(resp -> {
            if (resp instanceof SimpleStrings) {
                return SimpleStrings.OK.equals(((SimpleStrings) resp).getValue());
            }
            throw new RedisunException("invalid response:" + resp);
        });
    }


    /**
     * 关闭Redisun客户端，释放资源
     */
    public void close() {
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
        RESP r = syncExecute(new SAddCommand(key, members));
        if (r instanceof Integers) {
            return ((Integers) r).getValue();
        }
        throw new RedisunException("invalid response:" + r);
    }

    /**
     * 将一个或多个成员加入到集合中（异步版本）
     *
     * @param key     集合的键
     * @param members 要添加的一个或多个成员
     * @return 被成功添加到集合中的新元素数量，不包括已被添加的元素
     */
    public CompletableFuture<Integer> asyncSadd(String key, String... members) {
        return execute(new SAddCommand(key, members)).thenApply(resp -> {
            if (resp instanceof Integers) {
                return ((Integers) resp).getValue();
            }
            throw new RedisunException("invalid response:" + resp);
        });
    }

    /**
     * 将一个或多个值插入到列表的头部(左边)
     *
     * @param key    列表的键
     * @param values 要插入的一个或多个值
     * @return 执行后列表的长度
     */
    public long lpush(String key, String... values) {
        try {
            return asyncLpush(key, values).get();
        } catch (Throwable e) {
            throw new RedisunException(e);
        }
    }

    /**
     * 将一个或多个值插入到列表的头部(左边)（异步版本）
     *
     * @param key    列表的键
     * @param values 要插入的一个或多个值
     * @return 执行后列表的长度
     */
    public CompletableFuture<Long> asyncLpush(String key, String... values) {
        return execute(new LPushCommand(key, values)).thenApply(resp -> {
            if (resp instanceof Integers) {
                return ((Integers) resp).getValue().longValue();
            }
            throw new RedisunException("invalid response:" + resp);
        });
    }

    /**
     * 将一个或多个值插入到列表的尾部(右边)
     *
     * @param key    列表的键
     * @param values 要插入的一个或多个值
     * @return 执行后列表的长度
     */
    public long rpush(String key, String... values) {
        RESP r = syncExecute(new RPushCommand(key, values));
        if (r instanceof Integers) {
            return ((Integers) r).getValue();
        }
        throw new RedisunException("invalid response:" + r);
    }

    /**
     * 将一个或多个值插入到列表的尾部(右边)（异步版本）
     *
     * @param key    列表的键
     * @param values 要插入的一个或多个值
     * @return 执行后列表的长度
     */
    public CompletableFuture<Long> asyncRpush(String key, String... values) {
        return execute(new RPushCommand(key, values)).thenApply(resp -> {
            if (resp instanceof Integers) {
                return ((Integers) resp).getValue().longValue();
            }
            throw new RedisunException("invalid response:" + resp);
        });
    }

    /**
     * 返回哈希表中指定字段的值
     *
     * @param key   哈希表的键
     * @param field 要获取值的字段
     * @return 返回给定字段的值，如果字段不存在则返回null
     */
    public String hget(String key, String field) {
        RESP r = syncExecute(new HGetCommand(key, field));
        if (r instanceof Nulls) {
            return null;
        } else if (r instanceof BulkStrings) {
            return ((BulkStrings) r).getValue();
        }
        throw new RedisunException("invalid response:" + r);
    }

    /**
     * 返回哈希表中指定字段的值（异步版本）
     *
     * @param key   哈希表的键
     * @param field 要获取值的字段
     * @return 返回给定字段的值，如果字段不存在则返回null
     */
    public CompletableFuture<String> asyncHget(String key, String field) {
        return execute(new HGetCommand(key, field)).thenApply(resp -> {
            if (resp instanceof Nulls) {
                return null;
            } else if (resp instanceof BulkStrings) {
                return ((BulkStrings) resp).getValue();
            }
            throw new RedisunException("invalid response:" + resp);
        });
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
        RESP r = syncExecute(new HSetCommand(key, field, value));
        if (r instanceof Integers) {
            return ((Integers) r).getValue();
        }
        throw new RedisunException("invalid response:" + r);
    }

    /**
     * 将哈希表 key 中的字段 field 的值设为 value（异步版本）
     *
     * @param key   哈希表的键
     * @param field 哈希表中的字段
     * @param value 要设置的值
     * @return 如果字段是哈希表中的一个新建字段，并且值设置成功，返回1；
     * 如果哈希表中域字段已经存在且旧值已被新值覆盖，返回0
     */
    public CompletableFuture<Integer> asyncHset(String key, String field, String value) {
        return execute(new HSetCommand(key, field, value)).thenApply(resp -> {
            if (resp instanceof Integers) {
                return ((Integers) resp).getValue();
            }
            throw new RedisunException("invalid response:" + resp);
        });
    }

    /**
     * 返回 key 所储存的字符串值的长度
     *
     * @param key 要获取长度的键
     * @return 字符串值的长度
     */
    public int strlen(String key) {
        RESP r = syncExecute(new StrlenCommand(key));
        if (r instanceof Integers) {
            return ((Integers) r).getValue();
        }
        throw new RedisunException("invalid response:" + r);
    }

    /**
     * 返回 key 所储存的字符串值的长度（异步版本）
     *
     * @param key 要获取长度的键
     * @return 字符串值的长度
     */
    public CompletableFuture<Integer> asyncStrlen(String key) {
        return execute(new StrlenCommand(key)).thenApply(resp -> {
            if (resp instanceof Integers) {
                return ((Integers) resp).getValue();
            }
            throw new RedisunException("invalid response:" + resp);
        });
    }

    /**
     * 如果 key 已经存在并且是一个字符串，该命令将 value 追加到 key 原来的值的末尾
     *
     * @param key   要追加的键
     * @param value 要追加的值
     * @return 追加操作后 key 中字符串的长度
     */
    public int append(String key, String value) {
        RESP r = syncExecute(new AppendCommand(key, value));
        if (r instanceof Integers) {
            return ((Integers) r).getValue();
        }
        throw new RedisunException("invalid response:" + r);
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
        return execute(new AppendCommand(key, value)).thenApply(resp -> {
            if (resp instanceof Integers) {
                return ((Integers) resp).getValue();
            }
            throw new RedisunException("invalid response:" + resp);
        });
    }

    /**
     * 将 key 中储存的数字值减一
     *
     * @param key 要减少的键
     * @return 执行命令后 key 的值
     */
    public long decr(String key) {
        RESP r = syncExecute(new DecrCommand(key));
        if (r instanceof Integers) {
            return ((Integers) r).getValue();
        }
        throw new RedisunException("invalid response:" + r);
    }

    /**
     * 将 key 中储存的数字值减一（异步版本）
     *
     * @param key 要减少的键
     * @return 执行命令后 key 的值
     */
    public CompletableFuture<Long> asyncDecr(String key) {
        return execute(new DecrCommand(key)).thenApply(resp -> {
            if (resp instanceof Integers) {
                return ((Integers) resp).getValue().longValue();
            }
            throw new RedisunException("invalid response:" + resp);
        });
    }

    /**
     * 将 key 所储存的值减去给定的减量值（decrement）
     *
     * @param key       要减少的键
     * @param decrement 减量值
     * @return 执行命令后 key 的值
     */
    public long decrBy(String key, long decrement) {
        RESP r = syncExecute(new DecrByCommand(key, decrement));
        if (r instanceof Integers) {
            return ((Integers) r).getValue();
        }
        throw new RedisunException("invalid response:" + r);
    }

    /**
     * 将 key 所储存的值减去给定的减量值（decrement）（异步版本）
     *
     * @param key       要减少的键
     * @param decrement 减量值
     * @return 执行命令后 key 的值
     */
    public CompletableFuture<Long> asyncDecrBy(String key, long decrement) {
        return execute(new DecrByCommand(key, decrement)).thenApply(resp -> {
            if (resp instanceof Integers) {
                return ((Integers) resp).getValue().longValue();
            }
            throw new RedisunException("invalid response:" + resp);
        });
    }

    /**
     * 将 key 中储存的数字值增一
     *
     * @param key 要增加的键
     * @return 执行命令后 key 的值
     */
    public long incr(String key) {
        RESP r = syncExecute(new IncrCommand(key));
        if (r instanceof Integers) {
            return ((Integers) r).getValue();
        }
        throw new RedisunException("invalid response:" + r);
    }

    /**
     * 将 key 中储存的数字值增一（异步版本）
     *
     * @param key 要增加的键
     * @return 执行命令后 key 的值
     */
    public CompletableFuture<Long> asyncIncr(String key) {
        return execute(new IncrCommand(key)).thenApply(resp -> {
            if (resp instanceof Integers) {
                return ((Integers) resp).getValue().longValue();
            }
            throw new RedisunException("invalid response:" + resp);
        });
    }

    /**
     * 将 key 所储存的值加上给定的增量值（increment）
     *
     * @param key       要增加的键
     * @param increment 增量值
     * @return 执行命令后 key 的值
     */
    public long incrBy(String key, long increment) {
        RESP r = syncExecute(new IncrByCommand(key, increment));
        if (r instanceof Integers) {
            return ((Integers) r).getValue();
        }
        throw new RedisunException("invalid response:" + r);
    }

    /**
     * 将 key 所储存的值加上给定的增量值（increment）（异步版本）
     *
     * @param key       要增加的键
     * @param increment 增量值
     * @return 执行命令后 key 的值
     */
    public CompletableFuture<Long> asyncIncrBy(String key, long increment) {
        return execute(new IncrByCommand(key, increment)).thenApply(resp -> {
            if (resp instanceof Integers) {
                return ((Integers) resp).getValue().longValue();
            }
            throw new RedisunException("invalid response:" + resp);
        });
    }

    /**
     * 检查给定键是否存在
     *
     * @param keys 要检查的键
     * @return 存在的键数量
     */
    public int exists(String... keys) {
        try {
            return asyncExists(keys).get();
        } catch (Throwable e) {
            throw new RedisunException(e);
        }
    }

    /**
     * 异步检查给定键是否存在
     *
     * @param keys 要检查的键
     * @return 包含存在键数量的CompletableFuture
     */
    public CompletableFuture<Integer> asyncExists(String... keys) {
        return execute(new ExistsCommand(java.util.Arrays.asList(keys))).thenApply(resp -> {
            if (resp instanceof Integers) {
                return ((Integers) resp).getValue();
            }
            throw new RedisunException("invalid response:" + resp);
        });
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
        try {
            return asyncExpire(key, seconds, options).get();
        } catch (Throwable e) {
            throw new RedisunException(e);
        }
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
        return execute(cmd).thenApply(resp -> {
            if (resp instanceof Integers) {
                return ((Integers) resp).getValue();
            }
            throw new RedisunException("invalid response:" + resp);
        });
    }

    /**
     * 以秒为单位返回 key 的剩余过期时间
     *
     * @param key 要查询过期时间的键
     * @return 剩余过期时间（秒），-1表示没有设置过期时间，-2表示键不存在
     */
    public long ttl(String key) {
        try {
            return asyncTtl(key).get();
        } catch (Throwable e) {
            throw new RedisunException(e);
        }
    }

    /**
     * 以秒为单位返回 key 的剩余过期时间（异步版本）
     *
     * @param key 要查询过期时间的键
     * @return 剩余过期时间（秒），-1表示没有设置过期时间，-2表示键不存在
     */
    private CompletableFuture<Long> asyncTtl(String key) {
        return execute(new TtlCommand(key)).thenApply(resp -> {
            if (resp instanceof Integers) {
                return ((Integers) resp).getValue().longValue();
            }
            throw new RedisunException("invalid response:" + resp);
        });
    }

    /**
     * 返回 key 所储存的值的类型
     *
     * @param key 要查询类型的键
     * @return 键值的类型
     */
    public String type(String key) {
        try {
            return asyncType(key).get();
        } catch (Throwable e) {
            throw new RedisunException(e);
        }
    }

    /**
     * 返回 key 所储存的值的类型（异步版本）
     *
     * @param key 要查询类型的键
     * @return 键值的类型
     */
    private CompletableFuture<String> asyncType(String key) {
        return execute(new TypeCommand(key)).thenApply(resp -> {
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
        try {
            return asyncLpop(key).get();
        } catch (Throwable e) {
            throw new RedisunException(e);
        }
    }

    /**
     * 移除并返回列表的头部(左边)第一个元素（异步版本）
     *
     * @param key 列表的键
     * @return 列表的头部元素，如果列表为空则返回null
     */
    public CompletableFuture<String> asyncLpop(String key) {
        return execute(new LPopCommand(key)).thenApply(resp -> {
            if (resp instanceof Nulls) {
                return null;
            } else if (resp instanceof BulkStrings) {
                return ((BulkStrings) resp).getValue();
            }
            throw new RedisunException("invalid response:" + resp);
        });
    }

    /**
     * 移除并返回列表的尾部(右边)最后一个元素
     *
     * @param key 列表的键
     * @return 列表的尾部元素，如果列表为空则返回null
     */
    public String rpop(String key) {
        try {
            return asyncRpop(key).get();
        } catch (Throwable e) {
            throw new RedisunException(e);
        }
    }

    /**
     * 移除并返回列表的尾部(右边)最后一个元素（异步版本）
     *
     * @param key 列表的键
     * @return 列表的尾部元素，如果列表为空则返回null
     */
    public CompletableFuture<String> asyncRpop(String key) {
        return execute(new RPopCommand(key)).thenApply(resp -> {
            if (resp instanceof Nulls) {
                return null;
            } else if (resp instanceof BulkStrings) {
                return ((BulkStrings) resp).getValue();
            }
            throw new RedisunException("invalid response:" + resp);
        });
    }
}