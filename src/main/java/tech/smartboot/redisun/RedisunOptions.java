package tech.smartboot.redisun;

import org.smartboot.socket.extension.multiplex.MultiplexOptions;
import org.smartboot.socket.extension.plugins.StreamMonitorPlugin;

/**
 * @author 三刀
 * @version v1.0 10/21/25
 */
public class RedisunOptions {
    private final MultiplexOptions multiplexOptions;

    private int database = 0;
    private String password;
    private String username;


    public RedisunOptions(MultiplexOptions multiplexOptions) {
        this.multiplexOptions = multiplexOptions;
    }

    public RedisunOptions setAddress(String address) {
        if (address == null) {
            this.username = null;
            this.password = null;
            return this;
        }
        String host;
        int port;
        boolean ssl = false;
        // 解析 ssl 前缀
        if (address.startsWith("rediss://")) {
            ssl = true;
            address = address.substring(9);
        } else if (address.startsWith("redis://")) {
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
            host = address.substring(0, colonIndex);
            try {
                port = Integer.parseInt(address.substring(colonIndex + 1));
            } catch (NumberFormatException e) {
                port = ssl ? 6380 : 6379; // 默认端口
            }
        } else {
            host = address;
            port = ssl ? 6380 : 6379; // 默认端口
        }
        multiplexOptions.setHost(host);
        multiplexOptions.setPort(port);
        multiplexOptions.setSsl(ssl);
        return this;
    }

    public int getDatabase() {
        return database;
    }

    public RedisunOptions setDatabase(int database) {
        this.database = database;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public RedisunOptions setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public RedisunOptions setUsername(String username) {
        this.username = username;
        return this;
    }

    /**
     * 设置建立连接的超时时间
     */
    protected RedisunOptions connectTimeout(int connectTimeout) {
        this.multiplexOptions.connectTimeout(connectTimeout);
        return this;
    }

    public RedisunOptions maxConnections(int maxConnections) {
        this.multiplexOptions.maxConnections(maxConnections);
        return this;
    }

    public RedisunOptions minConnections(int minConnections) {
        this.multiplexOptions.minConnections(minConnections);
        return this;
    }

    public RedisunOptions debug(boolean debug) {
        if (debug) {
            multiplexOptions.addPlugin(new StreamMonitorPlugin<>());
        }
        return this;
    }
}