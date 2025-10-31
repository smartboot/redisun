package tech.smartboot.redisun.cmd;

import org.smartboot.socket.transport.WriteBuffer;
import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * Redis SET 命令实现类
 * <p>
 * SET 命令用于设置指定键的值。如果键已存在，不管其类型如何，都会被覆盖。
 * 成功执行SET操作后，与该键关联的任何生存时间都将被丢弃。
 * <p>
 * SET命令语法：
 * SET key value [NX | XX] [GET] [EX seconds | PX milliseconds |
 * EXAT unix-time-seconds | PXAT unix-time-milliseconds | KEEPTTL]
 * <p>
 * 支持的选项：
 * - EX seconds: 设置键的过期时间（秒）
 * - PX milliseconds: 设置键的过期时间（毫秒）
 * - EXAT timestamp-seconds: 设置键在指定的UNIX时间戳（秒）过期
 * - PXAT timestamp-milliseconds: 设置键在指定的UNIX时间戳（毫秒）过期
 * - NX: 仅在键不存在时设置键
 * - XX: 仅在键已存在时设置键
 * - KEEPTTL: 保留与键关联的生存时间
 * <p>
 * 注意：考虑到EXAT和PXAT在实际场景下通过Date作为入参使用体验更好，
 * 所以未提供直接设置EXAT的方法，默认使用PXAT配合Date入参方式实现时间戳过期功能。
 *
 * @author 三刀
 * @version v1.0 10/23/25
 * @see <a href="https://redis.io/docs/latest/commands/set/">Redis SET Command</a>
 */
public class SetCommand extends Command {
    private static final BulkStrings CONSTANTS_KEEPTTL = BulkStrings.of("KEEPTTL");
    private static final BulkStrings CONSTANTS_SET = BulkStrings.of("SET");
    private static final BulkStrings CONSTANTS_NX = BulkStrings.of("NX");
    private static final BulkStrings CONSTANTS_XX = BulkStrings.of("XX");
    private static final BulkStrings CONSTANTS_EX = BulkStrings.of("EX");
    private static final BulkStrings CONSTANTS_PX = BulkStrings.of("PX");
    private static final BulkStrings CONSTANTS_PXAT = BulkStrings.of("PXAT");
    private static final byte[] HEADER = new byte[]{RESP.RESP_DATA_TYPE_ARRAY, '3', '\r', '\n', RESP.RESP_DATA_TYPE_BULK, '3', '\r', '\n', 'S', 'E', 'T', '\r', '\n', RESP.RESP_DATA_TYPE_BULK};
    private static final byte[] PART = new byte[]{'\r', '\n', RESP.RESP_DATA_TYPE_BULK};
    // 要设置的键
    private final byte[] key;
    // 要设置的值
    private final byte[] value;
    // NX/XX选项，控制键是否存在的行为
    private BulkStrings exists;
    // 过期时间选项的处理器
    private Consumer<List<BulkStrings>> expire;
    // KEEPTTL选项的静态处理器
    private static final Consumer<List<BulkStrings>> expire_keep_ttl = (list) -> list.add(CONSTANTS_KEEPTTL);

    /**
     * 构造函数，创建一个SET命令实例
     *
     * @param key   键
     * @param value 值
     */
    public SetCommand(String key, String value) {
        this.key = key.getBytes();
        this.value = value.getBytes();
    }

    /**
     * 向参数列表中添加SET命令及其参数
     *
     */
    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>();
        // 添加命令名称
        param.add(CONSTANTS_SET);
        // 添加键
        param.add(RESP.ofString(key));
        // 添加值
        param.add(RESP.ofString(value));
        // 如果设置了NX/XX选项，则添加到参数中
        if (exists != null) {
            param.add(exists);
        }

        // 如果设置了过期时间选项，则通过处理器添加到参数中
        if (expire != null) {
            expire.accept(param);
        }
        return param;
    }

    @Override
    public void writeTo(WriteBuffer writeBuffer) throws IOException {
        if (exists != null || expire != null) {
            super.writeTo(writeBuffer);
            return;
        }
        writeBuffer.write(HEADER);
        RESP.writeInt(writeBuffer, key.length);
        writeBuffer.write(key);
        writeBuffer.write(PART);
        RESP.writeInt(writeBuffer, value.length);
        writeBuffer.write(value);
        writeBuffer.write(RESP.CRLF);
    }

    /**
     * 设置NX选项：仅在键不存在时设置键
     * 对应于SET命令的NX选项
     *
     * @return 当前SetCommand实例，支持链式调用
     */
    public SetCommand setIfNotExists() {
        exists = CONSTANTS_NX;
        return this;
    }

    /**
     * 设置XX选项：仅在键已存在时设置键
     * 对应于SET命令的XX选项
     *
     * @return 当前SetCommand实例，支持链式调用
     */
    public SetCommand setIfExists() {
        exists = CONSTANTS_XX;
        return this;
    }

    /**
     * 设置键的过期时间（秒）
     * 对应于SET命令的EX选项
     *
     * @param expireSeconds 过期时间（秒）
     * @return 当前SetCommand实例，支持链式调用
     */
    public SetCommand expire(int expireSeconds) {
        expire = (param) -> {
            param.add(CONSTANTS_EX);
            param.add(RESP.ofString(String.valueOf(expireSeconds)));
        };
        return this;
    }

    /**
     * 设置键的过期时间（毫秒）
     * 对应于SET命令的PX选项
     *
     * @param expireMilliseconds 过期时间（毫秒）
     * @return 当前SetCommand实例，支持链式调用
     */
    public SetCommand expireMs(long expireMilliseconds) {
        expire = (param) -> {
            param.add(CONSTANTS_PX);
            param.add(RESP.ofString(String.valueOf(expireMilliseconds)));
        };
        return this;
    }

    /**
     * 设置键在指定的UNIX时间戳过期（毫秒）
     * 对应于SET命令的PXAT选项
     * <p>
     * 注意：考虑到EXAT和PXAT在实际场景下通过Date作为入参使用体验更好，
     * 所以此处使用PXAT配合Date入参方式实现时间戳过期功能。
     *
     * @param date 过期时间点
     * @return 当前SetCommand实例，支持链式调用
     */
    public SetCommand expireAt(Date date) {
        expire = (param) -> {
            param.add(CONSTANTS_PXAT);
            param.add(RESP.ofString(String.valueOf(date.getTime())));
        };
        return this;
    }

    /**
     * 保留与键关联的生存时间
     * 对应于SET命令的KEEPTTL选项
     *
     * @return 当前SetCommand实例，支持链式调用
     */
    public SetCommand keepTTL() {
        expire = expire_keep_ttl;
        return this;
    }
}