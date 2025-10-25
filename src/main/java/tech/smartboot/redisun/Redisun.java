package tech.smartboot.redisun;

import org.smartboot.socket.extension.multiplex.MultiplexClient;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;
import tech.smartboot.redisun.cmd.DelCommand;
import tech.smartboot.redisun.cmd.GetCommand;
import tech.smartboot.redisun.cmd.HelloCommand;
import tech.smartboot.redisun.cmd.SetCommand;
import tech.smartboot.redisun.cmd.ZAddCommand;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.Integers;
import tech.smartboot.redisun.resp.Nulls;
import tech.smartboot.redisun.resp.RESP;
import tech.smartboot.redisun.resp.SimpleErrors;
import tech.smartboot.redisun.resp.SimpleStrings;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Redisun客户端主类
 * 提供了与Redis服务器交互的高级API接口
 *
 * @author 三刀
 * @version v1.0 10/21/25
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

    private final MultiplexClient<RESP> multiplexClient;

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
        multiplexClient = new MultiplexClient(processor, processor) {

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
            }
        };
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
        RESP r = syncExecute(new ZAddCommand(key, score, member));
        if (r instanceof Integers) {
            return ((Integers) r).getValue();
        }
        throw new RedisunException("invalid response:" + r);
    }

    /**
     * 获取指定键的值
     *
     * @param key 要获取值的键
     * @return 键对应的值，如果键不存在则返回null
     */
    public String get(String key) {
        RESP r = syncExecute(new GetCommand(key));
        if (r instanceof Nulls) {
            return null;
        } else if (r instanceof BulkStrings) {
            return ((BulkStrings) r).getValue();
        }
        throw new RedisunException("invalid response:" + r);
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
        SetCommand cmd = new SetCommand(key, value);
        if (options != null) {
            options.accept(cmd);
        }
        RESP r = syncExecute(cmd);
        if (r instanceof Nulls) {
            return false;
        }
        if (r instanceof SimpleStrings) {
            return SimpleStrings.OK.equals(((SimpleStrings) r).getValue());
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
        return del(Arrays.asList(keys));
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
        // 设置异常处理
        future.exceptionally(t -> {
            t.printStackTrace();
            return null;
        });
        try {
            // 获取可用的客户端连接
            final AioQuickClient client = multiplexClient.acquire();
            AioSession session = client.getSession();
            RedisSession redisSession = session.getAttachment();

            // 设置结果处理回调：清空future引用并将客户端放回连接池
            future.thenAccept(redisResponse -> {
                redisSession.setFuture(null);
                multiplexClient.reuse(client);
            });

            // 设置异常处理回调：释放客户端连接
            future.exceptionally(throwable -> {
                multiplexClient.release(client);
                return null;
            });

            // 检查当前会话是否正在执行其他命令
            if (redisSession.getFuture() != null || redisSession.getDecodingResponse() != null) {
                throw new RedisunException("当前session正在执行其他命令" + redisSession.getFuture() + "  " + redisSession.getDecodingResponse());
            }

            // 设置当前命令的future
            redisSession.setFuture(future);

            // 同步发送命令数据
            synchronized (client) {
                // 构建命令参数
                List<BulkStrings> params = command.buildParams();
                List p = params;
                // 将命令编码为RESP格式并写入缓冲区
                RESP.ofArray(p).writeTo(session.writeBuffer());
                // 刷新缓冲区，发送数据
                session.writeBuffer().flush();
            }
        } catch (Throwable e) {
            // 发生异常时完成future
            future.completeExceptionally(e);
        }
        return future;
    }


    /**
     * 关闭Redisun客户端，释放资源
     */
    public void close() {
        multiplexClient.close();
        if (group != null) {
            group.shutdown();
        }
    }
}