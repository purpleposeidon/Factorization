package factorization.api.datahelpers;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class DataOutNBT extends DataHelper {
    private final NBTTagCompound tag;
    
    public DataOutNBT(NBTTagCompound theTag) {
        tag = theTag;
    }
    
    public DataOutNBT() {
        this(new NBTTagCompound());
    }
    
    public NBTTagCompound getTag() {
        return tag;
    }
    
    @Override
    protected boolean shouldStore(Share share) {
        return !share.is_transient;
    }
    
    @Override
    public boolean isReader() {
        return false;
    }

    @Override
    public boolean putBoolean(boolean value) {
        if (valid) {
            tag.setBoolean(name, value);
        }
        return value;
    }

    @Override
    public byte putByte(byte value) {
        if (valid) {
            tag.setByte(name, value);
        }
        return value;
    }

    @Override
    public short putShort(short value) {
        if (valid) {
            tag.setShort(name, value);
        }
        return value;
    }

    @Override
    public int putInt(int value) {
        if (valid) {
            tag.setInteger(name, value);
        }
        return value;
    }

    @Override
    public long putLong(long value) {
        if (valid) {
            tag.setLong(name, value);
        }
        return value;
    }
    
    @Override
    public float putFloat(float value) {
        if (valid) {
            tag.setFloat(name, value);
        }
        return value;
    }
    
    @Override
    public double putDouble(double value) {
        if (valid) {
            tag.setDouble(name, value);
        }
        return value;
    }

    @Override
    public String putString(String value) {
        if (valid) {
            tag.setString(name, value);
        }
        return value;
    }

    @Override
    public ItemStack putItemStack(ItemStack value) {
        if (valid) {
            tag.setCompoundTag(name, value.writeToNBT(new NBTTagCompound()));
        }
        return value;
    }

}
