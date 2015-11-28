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
    public boolean putBoolean(boolean value) throws IOException {
        return dis.readBoolean();
    }

    @Override
    public byte putByte(byte value) throws IOException {
        return dis.readByte();
    }

    @Override
    public short putShort(short value) throws IOException {
        return dis.readShort();
    }

    @Override
    public int putInt(int value) throws IOException {
        return dis.readInt();
    }

    @Override
    public long putLong(long value) throws IOException {
        return dis.readLong();
    }

    @Override
    public float putFloat(float value) throws IOException {
        return dis.readFloat();
    }

    @Override
    public double putDouble(double value) throws IOException {
        return dis.readDouble();
    }

    @Override
    public String putString(String value) throws IOException {
        return dis.readUTF();
    }

    @Override
    public NBTTagCompound putTag(NBTTagCompound value) throws IOException {
        return DataUtil.readTag(dis);
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
