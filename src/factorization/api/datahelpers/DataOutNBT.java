package factorization.api.datahelpers;

import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;

public class DataOutNBT extends DataHelperNBT {
    public DataOutNBT(NBTTagCompound theTag) {
        tag = theTag;
    }
    
    public DataOutNBT() {
        this(new NBTTagCompound());
    }
    
    @Override
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
    protected <E> Object putImplementation(E value) throws IOException {
        if (value instanceof Boolean) {
            tag.setBoolean(name, (Boolean) value);
        } else if (value instanceof Byte) {
            tag.setByte(name, (Byte) value);
        } else if (value instanceof Short) {
            tag.setShort(name, (Short) value);
        } else if (value instanceof Integer) {
            tag.setInteger(name, (Integer) value);
        } else if (value instanceof Long) {
            tag.setLong(name, (Long) value);
        } else if (value instanceof Float) {
            tag.setFloat(name, (Float) value);
        } else if (value instanceof Double) {
            tag.setDouble(name, (Double) value);
        } else if (value instanceof String) {
            tag.setString(name, (String) value);
        } else if (value instanceof NBTTagCompound) {
            tag.setCompoundTag(name, (NBTTagCompound)value);
        }
        return value;
    }

}
