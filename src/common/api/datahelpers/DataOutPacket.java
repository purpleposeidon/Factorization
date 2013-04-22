package factorization.api.datahelpers;

import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.relauncher.Side;

public class DataOutPacket extends DataHelper {
    private final DataOutputStream dos;
    private final Side side;
    
    public DataOutPacket(DataOutputStream dos, Side side) {
        this.dos = dos;
        this.side = side;
    }
    
    @Override
    public DataHelper makeChild_do() {
        return this;
    }
    
    @Override
    protected void finishChild_do() {}

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
        if (valid) {
            dos.writeBoolean(value);
        }
        return value;
    }

    @Override
    public byte putByte(byte value) throws IOException {
        if (valid) {
            dos.writeByte(value);
        }
        return value;
    }

    @Override
    public short putShort(short value) throws IOException {
        if (valid) {
            dos.write(value);
        }
        return value;
    }

    @Override
    public int putInt(int value) throws IOException {
        if (valid) {
            dos.writeInt(value);
        }
        return value;
    }

    @Override
    public long putLong(long value) throws IOException {
        if (valid) {
            dos.writeLong(value);
        }
        return value;
    }

    @Override
    public float putFloat(float value) throws IOException {
        if (valid) {
            dos.writeFloat(value);
        }
        return value;
    }

    @Override
    public double putDouble(double value) throws IOException {
        if (valid) {
            dos.writeDouble(value);
        }
        return value;
    }

    @Override
    public String putString(String value) throws IOException {
        if (valid) {
            dos.writeUTF(value);
        }
        return value;
    }

    @Override
    public ItemStack putItemStack(ItemStack value) throws IOException {
        if (valid) {
            NBTBase.writeNamedTag(value.writeToNBT(new NBTTagCompound()), dos);
        }
        return value;
    }

}
