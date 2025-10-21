package tech.smartboot.redisun;

/**
 * @author 三刀
 * @version v1.0 10/21/25
 */
public interface ZAddCommand {

    boolean add(String key, double score, String member);
}
