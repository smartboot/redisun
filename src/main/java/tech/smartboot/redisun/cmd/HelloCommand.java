package tech.smartboot.redisun.cmd;

import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.Arrays;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.Maps;
import tech.smartboot.redisun.resp.RESP;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀
 * @version v1.0 10/21/25
 */
public final class HelloCommand extends Command {
    private static final BulkStrings CONSTANTS_HELLO = BulkStrings.of("HELLO");
    private static final BulkStrings CONSTANTS_AUTH = BulkStrings.of("AUTH");
    private static final BulkStrings CONSTANTS_SETNAME = BulkStrings.of("SETNAME");
    private static final BulkStrings CONSTANTS_REDISUN = BulkStrings.of("redisun");
    /*
     *
     */
    private int protoVer = 3;
    private String username;
    private String password;

    @Override
    public List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>();
        param.add(CONSTANTS_HELLO);
        param.add(RESP.ofString(String.valueOf(protoVer)));
        if (password != null) {
            param.add(CONSTANTS_AUTH);
            if (username != null) {
                param.add(RESP.ofString(username));
            }
            param.add(RESP.ofString(password));
        }
        param.add(CONSTANTS_SETNAME);
        param.add(CONSTANTS_REDISUN);
        return param;
    }


    public void setUsername(String username) {
        this.username = username;
    }


    public void setPassword(String password) {
        this.password = password;
    }

    public static Response toResponse(RESP redisResponse) {
        Response response = new Response();
        Maps mapsResponse = (Maps) redisResponse;
        for (Map.Entry<RESP, RESP> entry : mapsResponse.getValue().entrySet()) {
            RESP key = entry.getKey();
            RESP value = entry.getValue();
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
                Arrays arrayResponse = (Arrays) value;
                for (RESP rsp : arrayResponse.getValue()) {
                    Module m = new Module();
                    for (Map.Entry<RESP, RESP> entry1 : ((Maps) rsp).getValue().entrySet()) {
                        RESP key1 = entry1.getKey();
                        RESP value1 = entry1.getValue();
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
