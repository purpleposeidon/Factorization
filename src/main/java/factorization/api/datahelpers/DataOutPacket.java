package factorization.api.datahelpers;

import java.io.DataOutput;
import java.io.IOException;

import factorization.util.DataUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.relauncher.Side;

public class DataOutPacket extends DataHelper {
    private final DataOutput dos;
    private final Side side;
    
    public DataOutPacket(DataOutput dos, Side side) {
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
    protected <E> E putImplementation(E value) throws IOException {
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
            dos.writeUTF((String)value);
        } else if (value instanceof NBTTagCompound) {
            CompressedStreamTools.write((NBTTagCompound) value, dos);
        }
        return value;
    }

    @Override
    public ItemStack[] putItemArray(ItemStack[] value) throws IOException {
        for (ItemStack is : value) {
            if (is == null) is = DataUtil.NULL_ITEM;
            NBTTagCompound out = is.writeToNBT(new NBTTagCompound());
            CompressedStreamTools.write(out, dos);
        }
        return value;
    }

    @Override
    public int[] putIntArray(int[] value) throws IOException {
        dos.writeInt(value.length);
        for (int v : value) {
            dos.writeInt(v);
        }
        return value;
    }
}
