package tech.smartboot.redisun;

import org.smartboot.socket.transport.WriteBuffer;
import tech.smartboot.redisun.response.ArrayResponse;
import tech.smartboot.redisun.response.MapsResponse;
import tech.smartboot.redisun.response.RedisResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀
 * @version v1.0 10/21/25
 */
final class HelloCommand extends Command {

    private static final byte[] CMD = "HELLO 3 ".getBytes();


    @Override
    protected void writeTo(WriteBuffer writeBuffer) throws IOException {
        writeBuffer.write(CMD);
        writeBuffer.write(CRLF);
        writeBuffer.flush();
    }

    public static Response toResponse(RedisResponse redisResponse) {
        Response response = new Response();
        MapsResponse mapsResponse = (MapsResponse) redisResponse;
        for (Map.Entry<RedisResponse, RedisResponse> entry : mapsResponse.getValue().entrySet()) {
            RedisResponse key = entry.getKey();
            RedisResponse value = entry.getValue();
            if ("server".equals(key.getValue())) {
                response.setServer(value.getValue().toString());
            } else if ("version".equals(key.getValue())) {
                response.setVersion(value.getValue().toString());
            } else if ("mode".equals(key.getValue())) {
                response.setMode(value.getValue().toString());
            } else if ("proto".equals(key.getValue())) {
                response.setProto(Integer.parseInt(value.getValue().toString()));
            } else if ("id".equals(key.getValue())) {
                response.setId(Integer.parseInt(value.getValue().toString()));
            } else if ("role".equals(key.getValue())) {
                response.setRole(value.getValue().toString());
            } else if ("modules".equals(key.getValue())) {
                ArrayResponse arrayResponse = (ArrayResponse) value;
                for (RedisResponse rsp : arrayResponse.getValue()) {
                    Module m = new Module();
                    for (Map.Entry<RedisResponse, RedisResponse> entry1 : ((MapsResponse) rsp).getValue().entrySet()) {
                        RedisResponse key1 = entry1.getKey();
                        RedisResponse value1 = entry1.getValue();
                        if ("name".equals(key1.getValue())) {
                            m.setName(value1.getValue().toString());
                        } else if ("ver".equals(key1.getValue())) {
                            m.setVer(Integer.parseInt(value1.getValue().toString()));
                        } else if ("path".equals(key1.getValue())) {
                            m.setPath(value1.getValue().toString());
                        }
                    }
                    response.getModules().add(m);
                }
            }

        }
        return response;
    }

    public static class Response {
        private String server;
        private String version;
        private String mode;
        private int proto;
        private int id;
        private String role;
        private List<Module> modules = new ArrayList<>();

        public String getServer() {
            return server;
        }

        public void setServer(String server) {
            this.server = server;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public int getProto() {
            return proto;
        }

        public void setProto(int proto) {
            this.proto = proto;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public List<Module> getModules() {
            return modules;
        }
    }

    public static class Module {
        private String name;
        private int ver;
        private String path;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getVer() {
            return ver;
        }

        public void setVer(int ver) {
            this.ver = ver;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
}
