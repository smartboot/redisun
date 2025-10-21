package tech.smartboot.redisun;

import org.smartboot.socket.extension.plugins.Plugin;
import org.smartboot.socket.extension.plugins.SslPlugin;
import org.smartboot.socket.extension.plugins.StreamMonitorPlugin;
import org.smartboot.socket.extension.ssl.factory.ClientSSLContextFactory;
import org.smartboot.socket.timer.HashedWheelTimer;
import org.smartboot.socket.timer.TimerTask;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author 三刀
 * @version v1.0 10/21/25
 */
public final class Redisun {
    private final Options options = new Options();
    private boolean closed;
    private boolean firstConnected = true;
    private AioQuickClient client;
    private AioSession session;
    /**
     * 可链路复用的连接
     */
    private final ConcurrentLinkedQueue<AioQuickClient> resuingClients = new ConcurrentLinkedQueue<>();
    /**
     * 所有连接
     */
    private final ConcurrentHashMap<AioQuickClient, AioQuickClient> clients = new ConcurrentHashMap<>();
    /**
     * 连接监控定时任务
     * <p>
     * 用于定期清理空闲连接的定时任务引用，通过双重检查锁定确保
     * 系统中只有一个监控任务在运行，避免资源浪费
     * </p>
     */
    private volatile TimerTask timerTask;
    private final RedisMessageProcessor processor = new RedisMessageProcessor();
    /**
     * 记录最后一次使用连接的时间戳，用于连接空闲超时检测
     * <p>
     * 当创建新的HTTP请求时会更新此时间戳，监控任务会定期检查此值
     * 以判断连接是否已空闲超过指定时间（默认30秒）
     * </p>
     */
    private long latestTime = System.currentTimeMillis();

    public static Redisun create(Consumer<Options> opts) {
        Redisun redisun = new Redisun();
        opts.accept(redisun.options);
        return redisun;
    }

    private Redisun() {

    }

    public ZAddCommand zadd() {
        return new ZAddCommand();
    }


    private AioQuickClient acquireConnection() throws Throwable {
        AioQuickClient client;
        while (true) {
            client = resuingClients.poll();
            if (client == null) {
                break;
            }
            AioSession session = client.getSession();
            if (session.isInvalid()) {
                releaseConnection(client);
                continue;
            }
            RedisSession attachment = session.getAttachment();
            //重置附件，为下一个响应作准备
            attachment.setDecodingResponse(null);
            return client;
        }

        if (firstConnected) {
            boolean noneSslPlugin = true;
            for (Plugin responsePlugin : options.getPlugins()) {
                processor.addPlugin(responsePlugin);
                if (responsePlugin instanceof SslPlugin) {
                    noneSslPlugin = false;
                }
            }
            if (noneSslPlugin && options.isSsl()) {
                processor.addPlugin(new SslPlugin<>(new ClientSSLContextFactory()));
            }
            processor.addPlugin(new StreamMonitorPlugin<>());
//            if (options.idleTimeout() > 0) {
//                processor.addPlugin(new IdleStatePlugin<>(options.idleTimeout()));
//            }

            firstConnected = false;
        }
        client = new AioQuickClient(options.getHost(), options.getPort(), processor, processor);
        if (options.getConnectTimeout() > 0) {
            client.connectTimeout(options.getConnectTimeout());
        }
        if (options.group() == null) {
            client.start();
        } else {
            client.start(options.group());
        }
        clients.put(client, client);
        startConnectionMonitor();

        return client;
    }

    /**
     * 启动连接监控任务，用于清理无效连接和空闲连接
     *
     * <p>该方法会启动一个定时任务，每隔1分钟执行一次检查：
     * <ul>
     *   <li>如果连接超过30秒没有被使用，则将其关闭</li>
     *   <li>如果所有连接都已关闭，则取消监控任务</li>
     *   <li>如果任务取消后又有新连接创建，则重新启动监控任务</li>
     * </ul>
     *
     * @see #releaseConnection(AioQuickClient)
     * @see #acquireConnection()
     */
    private void startConnectionMonitor() {
        // 使用双重检查锁定确保只有一个监控任务在运行
        if (timerTask != null) {
            return;
        }
        synchronized (this) {
            if (timerTask != null) {
                return;
            }
            timerTask = HashedWheelTimer.DEFAULT_TIMER.scheduleWithFixedDelay(() -> {
                long time = latestTime;
                // 如果超过30秒没有使用连接，则清理可复用连接队列中的连接
                if (System.currentTimeMillis() - time > 30 * 1000) {
                    AioQuickClient c;
                    // 当latestTime没有更新且队列中还有连接时，持续清理
                    while (time == latestTime && (c = resuingClients.poll()) != null) {
                        System.out.println("release...");
                        releaseConnection(c);
                    }
                }
                // 如果没有活动连接，则取消监控任务
                if (clients.isEmpty()) {
                    TimerTask oldTask = timerTask;
                    timerTask = null;
                    oldTask.cancel();
                    // 取消任务后再次检查是否有新连接加入，如果有则重新启动监控任务
                    if (!clients.isEmpty()) {
                        startConnectionMonitor();
                    }
                }
            }, 1, TimeUnit.MINUTES);
        }
    }

    private void releaseConnection(AioQuickClient client) {
        client.shutdownNow();
        clients.remove(client);
    }

    public void close() {
        closed = true;
        clients.forEach((client, aioQuickClient) -> releaseConnection(client));
    }
}
