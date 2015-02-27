package factorization.api;

import factorization.shared.NetworkFactorization.MessageType;
import io.netty.buffer.ByteBuf;

import java.io.IOException;


public interface IEntityMessage {
    boolean handleMessageFromServer(MessageType messageType, ByteBuf input) throws IOException;
    boolean handleMessageFromClient(MessageType messageType, ByteBuf input) throws IOException;
}
