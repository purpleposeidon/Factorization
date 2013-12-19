package factorization.sockets;

import java.io.IOException;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.FactoryType;

public class SocketEmpty extends TileEntitySocketBase {
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SOCKET_EMPTY;
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException { return this; }
    
    @Override
    public boolean canUpdate() {
        return false;
    }
}
