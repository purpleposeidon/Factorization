package factorization.api.datahelpers;

import java.io.DataInput;
import java.io.IOException;

import factorization.util.DataUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.relauncher.Side;

public class DataInPacket extends DataHelper {
    private final DataInput dis;
    private final Side side;
    
    public DataInPacket(DataInput dis, Side side) {
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
    protected <E> E putImplementation(E o) throws IOException {
        if (o instanceof Boolean) {
            return (E) (Boolean) dis.readBoolean();
        } else if (o instanceof Byte) {
            return (E) (Byte) dis.readByte();
        } else if (o instanceof Short) {
            return (E) (Short) dis.readShort();
        } else if (o instanceof Integer) {
            return (E) (Integer) dis.readInt();
        } else if (o instanceof Long) {
            return (E) (Long) dis.readLong();
        } else if (o instanceof Float) {
            return (E) (Float) dis.readFloat();
        } else if (o instanceof Double) {
            return (E) (Double) dis.readDouble();
        } else if (o instanceof String) {
            return (E) (String) dis.readUTF();
        } else if (o instanceof NBTTagCompound) {
            return (E) (NBTTagCompound) DataUtil.readTag(dis);
        }
        return o;
    }

    @Override
    public ItemStack[] putItemArray(ItemStack[] value) throws IOException {
        for (int i = 0; i < value.length; i++) {
            ItemStack is = ItemStack.loadItemStackFromNBT(DataUtil.readTag(dis));
            if (DataUtil.NULL_ITEM.isItemEqual(is)) is = null;
            value[i] = is;
        }
        return value;
    }

    @Override
    public int[] putIntArray(int[] value) throws IOException {
        int len = dis.readInt();
        if (value == null || value.length != len) {
            value = new int[len]; // Yeah, this could be a denial of service :/
        }
        for (int i = 0; i < len; i++) {
            value[i] = dis.readInt();
        }
        return value;
    }
}
