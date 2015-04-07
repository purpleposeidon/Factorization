package factorization.mechanics;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.FactoryType;
import factorization.shared.Core;
import factorization.sockets.TileEntitySocketBase;
import net.minecraft.item.ItemStack;

import java.io.IOException;

public class SocketHandCrank extends TileEntitySocketBase {
    @Override
    public FactoryType getFactoryType() {
        return null;
        //return FactoryType.SOCKET_HAND_CRANK;
    }

    @Override
    public ItemStack getCreatingItem() {
        return Core.registry.dark_iron_sprocket;
    }

    @Override
    public FactoryType getParentFactoryType() {
        return FactoryType.SOCKET_EMPTY;
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        return this;
    }
}
