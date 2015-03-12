package factorization.api.datahelpers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import factorization.util.DataUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import factorization.api.FzOrientation;
import net.minecraftforge.fluids.FluidTank;

public abstract class DataHelper {
    /***
     * The field name; used for serializing to/from NBT.
     */
    protected String name;
    /***
     * If true, whatever is getting put needs to be saved/loaded. Set by {@link DataHelper#as}.
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
     * First call {@link DataHelper#as} to set the name that should be used and how it should be Shared.
     * For the {@link IDataSerializable}, the name will be used as a prefix.
     * If writing, the original will be returned. If reading, the loaded value will be returned.
     * Must be able to handle these types: Boolean, Byte, Short, Integer, Float, Double, String, ItemStack, IDataSerializable, Enum
     * @param o An object. It must never be null.
     * @return The original value if isWriter() or the sharing does not permit write access; else the read value
     * @throws IOException
     */
    public <E> E put(E o) throws IOException {
        if (!valid) {
            return o;
        }
        if (o instanceof IDataSerializable) {
            return (E) ((IDataSerializable) o).serialize(name, this);
        }
        if (o instanceof Enum) {
            Enum value = (Enum) o;
            int i = putInt(value.ordinal());
            if (isWriter()) {
                return (E) value;
            }
            return (E) value.getDeclaringClass().getEnumConstants()[i];
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
        if (o instanceof UUID) {
            UUID uuid = (UUID) o;
            String base_name = name;
            if (isReader()) {
                long msb = asSameShare(base_name + "MSB").putLong(0);
                long lsb = asSameShare(base_name + "LSB").putLong(0);
                return (E) new UUID(msb, lsb);
            } else {
                asSameShare(base_name + "MSB").putLong(uuid.getMostSignificantBits());
                asSameShare(base_name + "LSB").putLong(uuid.getLeastSignificantBits());
                return (E) uuid;
            }
            
        }
        return putImplementation(o);
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
    protected abstract <E> E putImplementation(E o) throws IOException;
    
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
    public final UUID putUUID(UUID value) throws IOException { return (UUID)put(value); }
    
    public final String putString(String value) throws IOException { return (String)put(value); }
    public final FzOrientation putFzOrientation(FzOrientation value) throws IOException { return (FzOrientation)put(value); }
    public final ItemStack putItemStack(ItemStack value) throws IOException {
        if (value == null) {
            value = DataUtil.NULL_ITEM;
        }
        ItemStack ret = put(value);
        if (ret != null && ret.getItem() == null) {
            return null;
        }
        return ret;
    }

    public final ArrayList<ItemStack> putItemArray(ArrayList<ItemStack> value) throws IOException {
        if (!valid) return value;
        if (isReader() && hasLegacy(name + "_len")) {
            return putItemArray_legacy(value);
        }
        return putItemArray_efficient(value);
    }

    protected ArrayList<ItemStack> putItemArray_efficient(ArrayList<ItemStack> value) throws IOException {
        return putItemArray_legacy(value);
    }


    @Deprecated
    public final ArrayList<ItemStack> putItemArray_legacy(ArrayList<ItemStack> value) throws IOException {
        String prefix = name;
        int len = asSameShare(prefix + "_len").putInt(value.size());
        if (isReader()) {
            value.clear();
            value.ensureCapacity(len);
            for (int i = 0; i < len; i++) {
                value.add(asSameShare(prefix + "_" + i).putItemStack(null));
            }
        } else {
            for (int i = 0; i < len; i++) {
                asSameShare(prefix + "_" + i).putItemStack(value.get(i));
            }
        }
        return value;
    }

    public final NBTTagCompound putTag(NBTTagCompound value) throws IOException {
        return (NBTTagCompound)put(value);
    }

    public final FluidTank putTank(FluidTank tank) throws IOException {
        if (isWriter()) {
            NBTTagCompound tag = new NBTTagCompound();
            tank.writeToNBT(tag);
            putTag(tag);
            return tank;
        } else {
            NBTTagCompound tag = putTag(new NBTTagCompound());
            tank.readFromNBT(tag);
            return tank;
        }
    }
    
    public Vec3 putVec3(Vec3 val) throws IOException {
        String prefix = name;
        val.xCoord = asSameShare(prefix + ".x").putDouble(val.xCoord);
        val.yCoord = asSameShare(prefix + ".y").putDouble(val.yCoord);
        val.zCoord = asSameShare(prefix + ".z").putDouble(val.zCoord);
        name = prefix;
        return val;
    }
                                    

    public final <E extends Enum> E putEnum(E value) throws IOException { return (E)put(value); }
    
    public void log(String message) {}
    
    public boolean hasLegacy(String name) {
        return false;
    }
    
    public boolean isValid() {
        return valid;
    }
}
