package factorization.api.datahelpers;

import java.io.IOException;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import factorization.api.FzOrientation;

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
    
    public NBTTagCompound getTag() {
        return null;
    }
    
    public boolean isNBT() {
        return false;
    }
    
    
    /**
     * The put function will save or load a value. 
     * First call {@link DataHelper.as} to set the name that should be used and how it should be Shared.
     * For the {@link IDataSerializable}, the name will be used as a prefix.
     * If writing, the original will be returned. If reading, the loaded value will be returned.
     * Must be able to handle these types: Boolean, Byte, Short, Integer, Float, Double, String, ItemStack, IDataSerializable, Enum
     * @param An object. It must never be null.
     * @return o if isWriter(); else the read value
     * @throws IOException
     */
    public final <E> E put(E o) throws IOException {
        if (!valid) {
            return o;
        }
        if (o instanceof IDataSerializable) {
            return (E) ((IDataSerializable) o).serialize(name, this);
        }
        if (o instanceof Enum) {
            Enum value = (Enum) o;
            int i = value.ordinal();
            i = put(i);
            if (isWriter()) {
                return (E) value;
            }
            return (E) value.getClass().getEnumConstants()[i];
        }
        if (o instanceof ItemStack) {
            ItemStack value = (ItemStack) o;
            NBTTagCompound writtenTag = value.writeToNBT(new NBTTagCompound());
            if (isReader()) {
                return (E) ItemStack.loadItemStackFromNBT(put(writtenTag));
            } else {
                put(writtenTag);
                return o;
            }
        }
        return (E) putImplementation(o);
    }
    
    /** Reads or writes a value, and returns what was read or written.
     * Here is a template for all the types: <pre>
        if (o instanceof Boolean) {
        } else if (o instanceof Byte) {
        } else if (o instanceof Short) {
        } else if (o instanceof Integer) {
        } else if (o instanceof Long) {
        } else if (o instanceof Float) {
        } else if (o instanceof Double) {		
        } else if (o instanceof String) {
        } else if (o instanceof NBTTagCompound) {
        }
        </pre>
        The actual list is: Boolean Byte Short Integer Long Float Double String
     * @throws IOException
     */
    protected abstract <E> Object putImplementation(E o) throws IOException;
    
    /*
     * For compatability with old code:
     * 
for t in "Boolean Byte Short Int Long Float Double String FzOrientation ItemStack".split():
    print("""public final _ put%(_ value) throws IOException { return (_)put(value); }""".replace('_', t.lower()).replace('%', t))
     */
    public final boolean putBoolean(boolean value) throws IOException { return (boolean)put(value); }
    public final byte putByte(byte value) throws IOException { return (byte)put(value); }
    public final short putShort(short value) throws IOException { return (short)put(value); }
    public final int putInt(int value) throws IOException { return (int)put(value); }
    public final long putLong(long value) throws IOException { return (long)put(value); }
    public final float putFloat(float value) throws IOException { return (float)put(value); }
    public final double putDouble(double value) throws IOException { return (double)put(value); }
    
    public final String putString(String value) throws IOException { return (String)put(value); }
    public final FzOrientation putFzOrientation(FzOrientation value) throws IOException { return (FzOrientation)put(value); }
    public final ItemStack putItemStack(ItemStack value) throws IOException { return (ItemStack)put(value); }

    public final <E extends Enum> E putEnum(E value) throws IOException { return (E)put(value); }
    
    private static final Class[] validTypes = new Class[] {
        Boolean.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, NBTTagCompound.class
    };
    public final Object putUntypedOject(Object value) throws IOException {
        if (!valid) {
            return value;
        }
        final String orig_name = name;
        if (isReader()) {
            int typeIndex = asSameShare(orig_name + ".type").put(-1);
            asSameShare(orig_name);
            if (typeIndex < 0 || typeIndex > validTypes.length) {
                return value; //Fun times.
            }
            Class type = validTypes[typeIndex];
            if (value != null && value.getClass() == type) {
                return put(value);
            }
            //We don't have a good value. So, we'll have to create one.
            if (type == Boolean.class) {
                value = false;
            } else if (type == Short.class) {
                value = (short) 0;
            } else if (type == Integer.class) {
                value = (int) 0;
            } else if (type == Long.class) {
                value = (long) 0;
            } else if (type == Float.class) {
                value = (float) 0;
            } else if (type == Double.class) {
                value = (double) 0;
            } else if (type == NBTTagCompound.class) {
                value = new NBTTagCompound();
            } else {
                return null;
            }
            return put(value);
        } else {
            for (int i = 0; i < validTypes.length; i++) {
                Class type = validTypes[i];
                if (value.getClass() == type) { //TODO NORELEASE: Pull getClass out
                    asSameShare(orig_name + ".type").put(i);
                    asSameShare(orig_name);
                    put(value);
                    return value;
                }
            }
            return value; //Uh, yeah. We don't know what it is.
        }
    }
}
