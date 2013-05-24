package factorization.api.datahelpers;

import java.io.IOException;

import factorization.api.FzOrientation;
import factorization.common.Core;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public abstract class DataHelper {
    /***
     * The field name; used for serializing to/from NBT.
     */
    protected String name;
    /***
     * If true, whatever is getting put needs to be saved/loaded. Set by {@link DataHelper.as}.
     */
    protected boolean valid;
    
    protected DataHelper current_child;
    protected String current_child_name;
    public final DataHelper makeChild() {
        current_child_name = name;
        if (valid) {
            return current_child = makeChild_do();
        } else {
            return new DataIdentity(this);
        }
    }
    
    public final void finishChild() {
        if (valid) {
            finishChild_do();
        }
    }
    
    protected abstract DataHelper makeChild_do();
    
    protected abstract void finishChild_do();
    
    public DataHelper as(Share share, String set_name) {
        name = set_name;
        valid = shouldStore(share);
        return this;
    }
    
    public DataHelper asSameShare(String set_name) {
        name = set_name;
        return this;
    }
    
    protected abstract boolean shouldStore(Share share);
    public abstract boolean isReader();
    public boolean isWriter() {
        return !isReader();
    }
    
    public NBTTagCompound getTag() {
        return null;
    }
    
    public boolean isNBT() {
        return false;
    }
    
    /**
     * The put and put<Type> functions will save or load a value.
     * First call {@link DataHelper.as} to set the name that should be used and how it should be Shared.
     * For the {@link IDataSerializable}, the name will be used as a prefix.
     * If writing, the original will be returned. If reading, the loaded value will be returned.
     * 
     * @arg ids An {@link IDataSerializable}
     * @return If writing, returns the argument. If reading, returns the object that was read.
     */
    public <T extends IDataSerializable> T put(T ids) throws IOException {
        if (valid) {
            if (name == null) {
                name = "";
            }
            return (T) ids.serialize(name, this);
        }
        return ids;
    }
    
    public abstract boolean putBoolean(boolean value) throws IOException;
    public abstract byte putByte(byte value) throws IOException;
    public abstract short putShort(short value) throws IOException;
    public abstract int putInt(int value) throws IOException;
    public abstract long putLong(long value) throws IOException;
    public abstract float putFloat(float value) throws IOException;
    public abstract double putDouble(double value) throws IOException;
    public abstract String putString(String value) throws IOException;
    public abstract ItemStack putItemStack(ItemStack value) throws IOException;
    
    public FzOrientation putFzOrientation(FzOrientation value) throws IOException {
        //In case it becomes ForgeOrientation
        byte v = (byte) value.ordinal();
        return FzOrientation.getOrientation(putByte(v));
    }
    
    public <E extends Enum> E putEnum(E value) throws IOException {
        int i = value.ordinal();
        i = putInt(i);
        if (isWriter()) {
            return value;
        }
        return (E) value.getClass().getEnumConstants()[i];
    }
    
    public Object putObject(Object o) throws IOException {
        if (o instanceof Boolean) {
            return putBoolean((Boolean) o);
        } else if (o instanceof Byte) {
            return putByte((Byte) o);
        } else if (o instanceof Short) {
            return putShort((Short) o);
        } else if (o instanceof Integer) {
            return putInt((Integer) o);
        } else if (o instanceof Float) {
            return putFloat((Float) o);
        } else if (o instanceof Double) {
            return putDouble((Double) o);
        } else if (o instanceof String) {
            return putString((String) o);
        } else if (o instanceof ItemStack) {
            return putItemStack((ItemStack) o);
        } else if (o instanceof IDataSerializable) {
            return put((IDataSerializable)o);
        } else if (o instanceof Enum) {
            return putEnum((Enum) o);
        } else {
            Core.logWarning("Don't know how to serialize " + o);
            return null;
        }
    }
}
