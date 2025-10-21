package tech.smartboot.redisun;

import org.smartboot.socket.transport.AioSession;
import tech.smartboot.redisun.response.RedisResponse;

import java.util.concurrent.CompletableFuture;

/**
 * @author 三刀
 * @version v1.0 10/21/25
 */
public class RedisSession {
    private final AioSession session;
    /**
     * 正在解码的响应
     */
    private RedisResponse decodingResponse;
    private CompletableFuture<RedisResponse> future = new CompletableFuture<>();

    public RedisSession(AioSession session) {
        this.session = session;
    }


    RedisResponse getDecodingResponse() {
        return decodingResponse;
    }

    void setDecodingResponse(RedisResponse decodingResponse) {
        this.decodingResponse = decodingResponse;
    }

    public CompletableFuture<RedisResponse> getFuture() {
        return future;
    }

    public void setFuture(CompletableFuture<RedisResponse> future) {
        this.future = future;
    }
}
