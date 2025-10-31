package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis EXPIRE 命令实现类
 * <p>
 * 为给定 key 设置过期时间，以秒计。
 * 如果 key 不存在或设置成功返回 1，否则返回 0。
 * </p>
 *
 * @see <a href="https://redis.io/commands/expire/">Redis EXPIRE Command</a>
 */
public class ExpireCommand extends Command {
    private static final BulkStrings CONSTANTS_EXPIRE = BulkStrings.of("EXPIRE");
    private static final BulkStrings CONSTANTS_NX = BulkStrings.of("NX");
    private static final BulkStrings CONSTANTS_XX = BulkStrings.of("XX");
    private static final BulkStrings CONSTANTS_GT = BulkStrings.of("GT");
    private static final BulkStrings CONSTANTS_LT = BulkStrings.of("LT");
    
    private final String key;
    private final int seconds;
    private BulkStrings option;

    public ExpireCommand(String key, int seconds) {
        this.key = key;
        this.seconds = seconds;
    }
    
    public ExpireCommand setIfNotExists() {
        this.option = CONSTANTS_NX;
        return this;
    }
    
    public ExpireCommand setIfExists() {
        this.option = CONSTANTS_XX;
        return this;
    }
    
    public ExpireCommand setIfGreater() {
        this.option = CONSTANTS_GT;
        return this;
    }
    
    public ExpireCommand setIfLess() {
        this.option = CONSTANTS_LT;
        return this;
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>();
        param.add(CONSTANTS_EXPIRE);
        param.add(RESP.ofString(key));
        param.add(RESP.ofString(String.valueOf(seconds)));
        
        if (option != null) {
            param.add(option);
        }
        
        return param;
    }
}