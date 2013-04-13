package factorization.api.datahelpers;

import java.io.IOException;

import net.minecraft.item.ItemStack;

public abstract class DataHelper {
    /***
     * The field name; used for serializing to/from NBT.
     */
    protected String name;
    /***
     * If true, whatever is getting put needs to be saved/loaded. Set by {@link DataHelper.as}.
     */
    protected boolean valid;
    
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
}
