package factorization.api.datahelpers;

import factorization.util.DataUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;

import java.io.IOException;

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
    public boolean putBoolean(boolean value) throws IOException {
        dos.writeBoolean(value);
        return value;
    }

    @Override
    public byte putByte(byte value) throws IOException {
        dos.writeByte(value);
        return value;
    }

    @Override
    public short putShort(short value) throws IOException {
        dos.writeShort(value);
        return value;
    }

    @Override
    public int putInt(int value) throws IOException {
        dos.writeInt(value);
        return value;
    }

    @Override
    public long putLong(long value) throws IOException {
        dos.writeLong(value);
        return value;
    }

    @Override
    public float putFloat(float value) throws IOException {
        dos.writeFloat(value);
        return value;
    }

    @Override
    public double putDouble(double value) throws IOException {
        dos.writeDouble(value);
        return value;
    }

    @Override
    public String putString(String value) throws IOException {
        ByteBufUtils.writeUTF8String(dos, value);
        return value;
    }

    @Override
    public NBTTagCompound putTag(NBTTagCompound value) throws IOException {
        ByteBufUtils.writeTag(dos, value);
        return value;
    }

    @Override
    public ItemStack[] putItemArray(ItemStack[] value) throws IOException {
        for (ItemStack is : value) {
            if (is == null) is = DataUtil.NULL_ITEM;
            ByteBufUtils.writeTag(dos, is.writeToNBT(new NBTTagCompound()));
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
