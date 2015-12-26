package factorization.api;

import factorization.net.StandardMessageType;
import io.netty.buffer.ByteBuf;

import java.io.IOException;


public interface IEntityMessage {
    boolean handleMessageFromServer(StandardMessageType messageType, ByteBuf input) throws IOException;
    boolean handleMessageFromClient(StandardMessageType messageType, ByteBuf input) throws IOException;
}
