package factorization.api;

import java.io.DataInput;
import java.io.IOException;

public interface IEntityMessage {
    boolean handleMessageFromServer(short messageType, DataInput input) throws IOException;
    boolean handleMessageFromClient(short messageType, DataInput input) throws IOException;
}
