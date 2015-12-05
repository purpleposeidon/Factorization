package factorization.sockets;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.FactoryType;
import factorization.shared.Core;
import net.minecraft.item.ItemStack;

import java.io.IOException;

public class SocketBareMotor extends TileEntitySocketBase {

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        return this;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SOCKET_BARE_MOTOR;
    }

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public ItemStack getCreatingItem() {
        return new ItemStack(Core.registry.motor);
    }

    @Override
    public FactoryType getParentFactoryType() {
        return FactoryType.SOCKET_EMPTY;
    }

}
