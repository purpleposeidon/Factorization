package factorization.api;

import java.io.DataInput;
import java.io.IOException;

import factorization.shared.NetworkFactorization.MessageType;


public interface IEntityMessage {
    boolean handleMessageFromServer(MessageType messageType, DataInput input) throws IOException;
    boolean handleMessageFromClient(MessageType messageType, DataInput input) throws IOException;
}
