package tech.smartboot.redisun.cmd;

import org.smartboot.socket.transport.WriteBuffer;
import tech.smartboot.redisun.Command;
import tech.smartboot.redisun.resp.BulkStrings;
import tech.smartboot.redisun.resp.RESP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version v1.0 10/23/25
 */
public class GetCommand extends Command {
    private static final BulkStrings CMD_GET = BulkStrings.of("GET");
    private final byte[] key;
    private static final byte[] HEADER = new byte[]{RESP.RESP_DATA_TYPE_ARRAY, '2', '\r', '\n', RESP.RESP_DATA_TYPE_BULK, '3', '\r', '\n', 'G', 'E', 'T', '\r', '\n', RESP.RESP_DATA_TYPE_BULK};
//    private static final byte[][] FAST_HEADER = new byte[][]{
//            new byte[]{RESP.RESP_DATA_TYPE_ARRAY, '2', '\r', '\n', RESP.RESP_DATA_TYPE_BULK, '3', '\r', '\n', 'G', 'E', 'T', '\r', '\n', RESP.RESP_DATA_TYPE_BULK, '0', '\r', '\n'},
//            new byte[]{RESP.RESP_DATA_TYPE_ARRAY, '2', '\r', '\n', RESP.RESP_DATA_TYPE_BULK, '3', '\r', '\n', 'G', 'E', 'T', '\r', '\n', RESP.RESP_DATA_TYPE_BULK, '1', '\r', '\n'},
//            new byte[]{RESP.RESP_DATA_TYPE_ARRAY, '2', '\r', '\n', RESP.RESP_DATA_TYPE_BULK, '3', '\r', '\n', 'G', 'E', 'T', '\r', '\n', RESP.RESP_DATA_TYPE_BULK, '2', '\r', '\n'},
//            new byte[]{RESP.RESP_DATA_TYPE_ARRAY, '2', '\r', '\n', RESP.RESP_DATA_TYPE_BULK, '3', '\r', '\n', 'G', 'E', 'T', '\r', '\n', RESP.RESP_DATA_TYPE_BULK, '3', '\r', '\n'},
//            new byte[]{RESP.RESP_DATA_TYPE_ARRAY, '2', '\r', '\n', RESP.RESP_DATA_TYPE_BULK, '3', '\r', '\n', 'G', 'E', 'T', '\r', '\n', RESP.RESP_DATA_TYPE_BULK, '4', '\r', '\n'},
//            new byte[]{RESP.RESP_DATA_TYPE_ARRAY, '2', '\r', '\n', RESP.RESP_DATA_TYPE_BULK, '3', '\r', '\n', 'G', 'E', 'T', '\r', '\n', RESP.RESP_DATA_TYPE_BULK, '5', '\r', '\n'},
//            new byte[]{RESP.RESP_DATA_TYPE_ARRAY, '2', '\r', '\n', RESP.RESP_DATA_TYPE_BULK, '3', '\r', '\n', 'G', 'E', 'T', '\r', '\n', RESP.RESP_DATA_TYPE_BULK, '6', '\r', '\n'},
//            new byte[]{RESP.RESP_DATA_TYPE_ARRAY, '2', '\r', '\n', RESP.RESP_DATA_TYPE_BULK, '3', '\r', '\n', 'G', 'E', 'T', '\r', '\n', RESP.RESP_DATA_TYPE_BULK, '7', '\r', '\n'},
//            new byte[]{RESP.RESP_DATA_TYPE_ARRAY, '2', '\r', '\n', RESP.RESP_DATA_TYPE_BULK, '3', '\r', '\n', 'G', 'E', 'T', '\r', '\n', RESP.RESP_DATA_TYPE_BULK, '8', '\r', '\n'},
//            new byte[]{RESP.RESP_DATA_TYPE_ARRAY, '2', '\r', '\n', RESP.RESP_DATA_TYPE_BULK, '3', '\r', '\n', 'G', 'E', 'T', '\r', '\n', RESP.RESP_DATA_TYPE_BULK, '9', '\r', '\n'},
//    };

    public GetCommand(String key) {
        this.key = key.getBytes();
    }

    @Override
    protected List<BulkStrings> buildParams() {
        List<BulkStrings> param = new ArrayList<>(2);
        param.add(CMD_GET);
        param.add(RESP.ofString(key));
        return param;
    }

    @Override
    public void writeTo(WriteBuffer writeBuffer) throws IOException {
//        if (key.length < FAST_HEADER.length) {
//            writeBuffer.write(FAST_HEADER[key.length]);
//        } else {
        writeBuffer.write(HEADER);
        RESP.writeInt(writeBuffer, key.length);
//        }
        writeBuffer.write(key);
        writeBuffer.write(RESP.CRLF);
//        super.writeTo(writeBuffer);
    }
}
