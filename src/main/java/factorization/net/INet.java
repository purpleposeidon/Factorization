package factorization.net;

import io.netty.buffer.ByteBuf;

import java.io.IOException;

/**
 * Can be implemented on Entities and TileEntities.
 * getMessages returns a list of IMsg that are specific to the implementation of this interface.
 * More general IMsgs may be passed in that aren't in getMessages(). If they aren't from getMessages(),
 * they'll likely be from StandardMessageType.
 * handleMessage* should return true if it can use the message, and should always defer first to
 * super.handleMessage* if such a thing exists.
 */
public interface INet {
    /**
     * @return the static final E[] VALUES = values() in your message type enum.
     */
    Enum[] getMessages();
    boolean handleMessageFromServer(Enum messageType, ByteBuf input) throws IOException;
    boolean handleMessageFromClient(Enum messageType, ByteBuf input) throws IOException;
}