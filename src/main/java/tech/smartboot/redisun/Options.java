package tech.smartboot.redisun;

import org.smartboot.socket.extension.plugins.Plugin;
import tech.smartboot.redisun.response.RedisResponse;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version v1.0 10/21/25
 */
public class Options {
    /**
     * 绑定线程池资源组
     */
    private AsynchronousChannelGroup group;
    /**
     * 连接超时时间
     */
    private int connectTimeout;

    private int database = 0;
    private String password;
    private String username;
    private String host;
    private int port;
    private boolean ssl;
    /**
     * smart-socket 插件
     */
    private final List<Plugin<RedisResponse>> plugins = new ArrayList<>();

    public Options setAddress(String address) {
        if (address == null) {
            this.host = null;
            this.port = 0;
            this.ssl = false;
            this.username = null;
            this.password = null;
            return this;
        }

        // 解析 ssl 前缀
        if (address.startsWith("rediss://")) {
            this.ssl = true;
            address = address.substring(9);
        } else if (address.startsWith("redis://")) {
            this.ssl = false;
            address = address.substring(8);
        }

        // 解析认证信息
        int atIndex = address.indexOf('@');
        if (atIndex != -1) {
            String authPart = address.substring(0, atIndex);
            address = address.substring(atIndex + 1);

            // 解析 username:password 或仅 password
            int colonIndexInAuth = authPart.indexOf(':');
            if (colonIndexInAuth != -1) {
                this.username = authPart.substring(0, colonIndexInAuth);
                this.password = authPart.substring(colonIndexInAuth + 1);
            } else {
                this.password = authPart;
            }
        }

        // 解析 host 和 port
        int colonIndex = address.lastIndexOf(':');
        if (colonIndex != -1) {
            this.host = address.substring(0, colonIndex);
            try {
                this.port = Integer.parseInt(address.substring(colonIndex + 1));
            } catch (NumberFormatException e) {
                this.port = this.ssl ? 6380 : 6379; // 默认端口
            }
        } else {
            this.host = address;
            this.port = this.ssl ? 6380 : 6379; // 默认端口
        }

        return this;
    }

    String getHost() {
        return host;
    }

    int getPort() {
        return port;
    }

    boolean isSsl() {
        return ssl;
    }

    public int getDatabase() {
        return database;
    }

    public Options setDatabase(int database) {
        this.database = database;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public Options setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public Options setUsername(String username) {
        this.username = username;
        return this;
    }

    public List<Plugin<RedisResponse>> getPlugins() {
        return plugins;
    }
    public Options group(AsynchronousChannelGroup group) {
        this.group = group;
        return this;
    }

    public AsynchronousChannelGroup group() {
        return group;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * 设置建立连接的超时时间
     */
    protected Options connectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }
}