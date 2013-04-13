package factorization.api.datahelpers;

import java.io.DataInputStream;
import java.io.IOException;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.relauncher.Side;

public class DataInPacket extends DataHelper {
    private final DataInputStream dis;
    private final Side side;
    
    public DataInPacket(DataInputStream dis, Side side) {
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
        if (valid) {
            return dis.readBoolean();
        }
        return value;
    }

    @Override
    public byte putByte(byte value) throws IOException {
        if (valid) {
            return dis.readByte();
        }
        return value;
    }

    @Override
    public short putShort(short value) throws IOException {
        if (valid) {
            return dis.readShort();
        }
        return value;
    }

    @Override
    public int putInt(int value) throws IOException {
        if (valid) {
            return dis.readInt();
        }
        return value;
    }

    @Override
    public long putLong(long value) throws IOException {
        if (valid) {
            return dis.readLong();
        }
        return value;
    }

    @Override
    public float putFloat(float value) throws IOException {
        if (valid) {
            return dis.readFloat();
        }
        return value;
    }

    @Override
    public double putDouble(double value) throws IOException {
        if (valid) {
            return dis.readDouble();
        }
        return value;
    }

    @Override
    public String putString(String value) throws IOException {
        if (valid) {
            return dis.readUTF();
        }
        return value;
    }

    @Override
    public ItemStack putItemStack(ItemStack value) throws IOException {
        if (valid) {
            return ItemStack.loadItemStackFromNBT((NBTTagCompound) NBTBase.readNamedTag(dis));
        }
        return value;
    }

}
