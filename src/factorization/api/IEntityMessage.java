package factorization.api;

import java.io.DataInputStream;
import java.io.IOException;

public interface IEntityMessage {
    boolean handleMessageFromServer(short messageType, DataInputStream input) throws IOException;
    boolean handleMessageFromClient(short messageType, DataInputStream input) throws IOException;
}
