package factorization.api.datahelpers;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class DataInNBT extends DataHelperNBT {	
    public DataInNBT(NBTTagCompound theTag) {
        tag = theTag;
    }
    
    @Override
    public DataHelper makeChild_do() {
        if (tag.hasKey(current_child_name)) {
            return new DataInNBT(tag.getCompoundTag(current_child_name));
        }
        return new DataInNBT(new NBTTagCompound());
    }
    
    @Override
    protected void finishChild_do() {}

    @Override
    protected boolean shouldStore(Share share) {
        return !share.is_transient && tag.hasKey(name);
    }
    
    @Override
    public boolean isReader() {
        return true;
    }

    @Override
    public boolean putBoolean(boolean value) {
        if (valid) {
            return tag.getBoolean(name);
        }
        return value;
    }

    @Override
    public byte putByte(byte value) {
        if (valid) {
            return tag.getByte(name);
        }
        return value;
    }

    @Override
    public short putShort(short value) {
        if (valid) {
            return tag.getShort(name);
        }
        return value;
    }

    @Override
    public int putInt(int value) {
        if (valid) {
            return tag.getInteger(name);
        }
        return value;
    }

    @Override
    public long putLong(long value) {
        if (valid) {
            return tag.getLong(name);
        }
        return value;
    }
    
    @Override
    public float putFloat(float value) {
        if (valid) {
            return tag.getFloat(name);
        }
        return value;
    }
    
    @Override
    public double putDouble(double value) {
        if (valid) {
            return tag.getDouble(name);
        }
        return value;
    }

    @Override
    public String putString(String value) {
        if (valid) {
            if (tag.hasKey(name)) {
                return tag.getString(name);
            }
            return null;
        }
        return value;
    }

    @Override
    public ItemStack putItemStack(ItemStack value) {
        if (valid) {
            return ItemStack.loadItemStackFromNBT(tag.getCompoundTag(name));
        }
        return value;
    }

}
