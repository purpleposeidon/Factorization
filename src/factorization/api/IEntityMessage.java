package factorization.api;

import java.io.DataInputStream;
import java.io.IOException;

public interface IEntityMessage {
    boolean handleMessage(short messageType, DataInputStream input) throws IOException;
}
