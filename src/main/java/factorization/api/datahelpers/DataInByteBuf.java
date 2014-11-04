package factorization.api.datahelpers;

import io.netty.buffer.ByteBuf;

import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.relauncher.Side;

public class DataInByteBuf extends DataHelper {
    private final ByteBuf dis;
    private final Side side;
    
    public DataInByteBuf(ByteBuf dis, Side side) {
        this.dis = dis;
        this.side = side;
    }

    @Override
    protected boolean shouldStore(Share share) {
        return share.is_public;
    }
    
    @Override
    public boolean isReader() {
        return true;
    }
    
    @Override
    protected <E> Object putImplementation(E o) throws IOException {
        if (o instanceof Boolean) {
            return dis.readBoolean();
        } else if (o instanceof Byte) {
            return dis.readByte();
        } else if (o instanceof Short) {
            return dis.readShort();
        } else if (o instanceof Integer) {
            return dis.readInt();
        } else if (o instanceof Long) {
            return dis.readLong();
        } else if (o instanceof Float) {
            return dis.readFloat();
        } else if (o instanceof Double) {
            return dis.readDouble();
        } else if (o instanceof String) {
            return ByteBufUtils.readUTF8String(dis);
        } else if (o instanceof NBTTagCompound) {
            return ByteBufUtils.readTag(dis);
        }
        return o;
    }

}
