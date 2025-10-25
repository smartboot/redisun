package tech.smartboot.redisun;

/**
 * @author 三刀
 * @version v1.0 10/21/25
 */
public class RedisunException extends RuntimeException {
    public RedisunException(Throwable throwable) {
        super(throwable);
    }

    public RedisunException(String s) {
        super(s);
    }
}
