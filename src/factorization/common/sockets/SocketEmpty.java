package factorization.common.sockets;

import factorization.common.FactoryType;
import factorization.common.TileEntitySocketBase;

public class SocketEmpty extends TileEntitySocketBase {
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SOCKET_EMPTY;
    }
}
