package factorization.api.datahelpers;

import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import factorization.util.DataUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.io.IOException;

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
        return ByteBufUtils.readUTF8String(dis);
    }

    @Override
    public NBTTagCompound putTag(NBTTagCompound value) throws IOException {
        return ByteBufUtils.readTag(dis);
    }

    @Override
    public ItemStack[] putItemArray(ItemStack[] value) throws IOException {
        for (int i = 0; i < value.length; i++) {
            ItemStack is = ItemStack.loadItemStackFromNBT(ByteBufUtils.readTag(dis));
            if (DataUtil.NULL_ITEM.isItemEqual(is)) is = null;
            value[i] = is;
        }
        return value;
    }

    @Override
    public int[] putIntArray(int[] value) throws IOException {
        int len = dis.readInt();
        if (value == null || len != value.length) {
            value = new int[len]; // Yeah, this could be a denial of service. :/
        }
        for (int i = 0; i < len; i++) {
            value[i] = dis.readInt();
        }
        return value;
    }
}
