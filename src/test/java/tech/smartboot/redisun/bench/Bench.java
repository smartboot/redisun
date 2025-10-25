package tech.smartboot.redisun.bench;

/**
 * @author 三刀
 * @version v1.0 10/23/25
 */
public class Bench {
    protected static final int SET_COUNT = 500000;

    protected static final int CONCURRENT_CLIENT_COUNT = 16;

    protected static final String ADDRESS = "redis://127.0.0.1:6379";
}
