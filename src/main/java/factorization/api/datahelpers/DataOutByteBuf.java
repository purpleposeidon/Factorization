package factorization.api.datahelpers;

import io.netty.buffer.ByteBuf;

import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.relauncher.Side;

public class DataOutByteBuf extends DataHelper {
    private final ByteBuf dos;
    private final Side side;
    
    public DataOutByteBuf(ByteBuf dos, Side side) {
        this.dos = dos;
        this.side = side;
    }
    

    @Override
    protected boolean shouldStore(Share share) {
        return share.is_public;
    }
    
    @Override
    public boolean isReader() {
        return false;
    }
    
    @Override
    protected <E> Object putImplementation(E value) throws IOException {
        if (value instanceof Boolean) {
            dos.writeBoolean((Boolean)value);
        } else if (value instanceof Byte) {
            dos.writeByte((Byte)value);
        } else if (value instanceof Short) {
            dos.writeShort((Short)value);
        } else if (value instanceof Integer) {
            dos.writeInt((Integer)value);
        } else if (value instanceof Long) {
            dos.writeLong((Long) value);
        } else if (value instanceof Float) {
            dos.writeFloat((Float) value);
        } else if (value instanceof Double) {
            dos.writeDouble((Double) value);
        } else if (value instanceof String) {
            ByteBufUtils.writeUTF8String(dos, (String) value);
        } else if (value instanceof NBTTagCompound) {
            ByteBufUtils.writeTag(dos, (NBTTagCompound) value);
        }
        return value;
    }
}
