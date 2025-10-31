package tech.smartboot.redisun.bench;

/**
 * @author 三刀
 * @version v1.0 10/23/25
 */
public abstract class Bench {
    // 为了在GitHub Actions环境中更快地运行测试，减少测试数据量
    // 太多的次数会导致 Redisson 带不动
    protected static final int SET_COUNT = 50000;

    protected static final int CONCURRENT_CLIENT_COUNT = Integer.parseInt(System.getProperty("client.count", "8"));

    protected static final String ADDRESS = "redis://127.0.0.1:6379";

    // 预热次数
    protected static final int WARMUP_COUNT = 1000;

    public abstract void asyncSet() throws Throwable;

    public abstract void asyncGet() throws Throwable;

    public abstract void concurrentSet() throws Throwable;

    public abstract void concurrentGet() throws Throwable;
}