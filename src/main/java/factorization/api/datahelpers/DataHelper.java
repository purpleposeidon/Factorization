package factorization.api.datahelpers;

import factorization.api.FzOrientation;
import factorization.util.DataUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.fluids.FluidTank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * The put* family of methods will save or load a value.
 * First call {@link DataHelper#as} to set the name that should be used and how it should be Shared.
 * For the {@link IDataSerializable}, the name will be used as a prefix.
 * If writing, the original will be returned. If reading, the loaded value will be returned.
 * Must be able to handle these types: Boolean, Byte, Short, Integer, Float, Double, String, ItemStack, IDataSerializable, Enum
 * Each put* method takes some type of parameter. In most cases it can not be null.
 * Each put* method returns the original value if isWriter(), otherwise it reads the new value. In the case of putIDS,
 * the value may or may not be modified, depending on its semantics.
 * Each put* method may throw an IOException.
 */
public abstract class DataHelper {
    /**
     * Set the name
     * @param share Set the context for sharing (eg, does the data get sent to the client?)
     * @param set_name Set the name, (nearly always) used by NBT.
     * @return The object to be used with put*()
     */
    public DataHelper as(Share share, String set_name) {
        name = set_name;
        if (shouldStore(share)) return this;
        return DataIdentity.instance;
    }

    /**
     * @param share The context that the DataHelper is being used in
     * @return true if the data should be considered, else false.
     */
    protected abstract boolean shouldStore(Share share);

    /**
     * Like {@link DataHelper#as(Share, String)}, but leaves the Share mode unchanged.
     * @param set_name Set the name
     * @return The object to be used with put*()
     */
    public DataHelper asSameShare(String set_name) {
        name = set_name;
        return this;
    }

    /***
     * The field name; used for serializing to/from NBT.
     */
    protected String name;

    /**
     * @return true if the DataHelper is an input context, and thus some things may need to be constructed.
     */
    public abstract boolean isReader();

    /**
     * @return true if the DataHelper is in an output context.
     */
    public final boolean isWriter() {
        return !isReader();
    }

    /**
     * @return true if the DataHelper is based off of an NBT tag.
     */
    public boolean isNBT() {
        return false;
    }

    /**
     * @return the NBT tag the DataHelper is using, or null if there is no tag.
     */
    public NBTTagCompound getTag() {
        return null;
    }

    /**
     * @param name NBT key
     * @return true if this is an NBT context and the tag has the given name.
     */
    public boolean hasLegacy(String name) {
        return false;
    }

    /**
     * @return true if shouldStore() evaluated to true.
     */
    public boolean isValid() {
        return true;
    }

    /**
     * @param message A message to associate with whatever's going on; used in more esoteric contexts.
     */
    public void log(String message) {}

    public <E extends IDataSerializable> E putIDS(E val) throws IOException {
        if (val == null) throw new NullPointerException();
        return (E) val.serialize(name, this);
    }

    /*
     * For compatability with old code:
     *
all_types = "IDataSerializable Boolean Byte Short Int Long Float Double String FzOrientation UUID ItemStack ItemList IntArray NBTTagCompound FluidTank AxisAlignedBB Vec3 Enum".split()
for t in all_types:
    print("""public final _ put%(_ value) throws IOException { return (_)put(value); }""".replace('_', t.lower()).replace('%', t))
     */
    public abstract boolean putBoolean(boolean value) throws IOException;
    public abstract byte putByte(byte value) throws IOException;
    public abstract short putShort(short value) throws IOException;
    public abstract int putInt(int value) throws IOException;
    public abstract long putLong(long value) throws IOException;
    public abstract float putFloat(float value) throws IOException;
    public abstract double putDouble(double value) throws IOException;
    public abstract String putString(String value) throws IOException;
    public abstract int[] putIntArray(int[] value) throws IOException;
    public abstract NBTTagCompound putTag(NBTTagCompound value) throws IOException;
    public abstract ItemStack[] putItemArray(ItemStack[] value) throws IOException;

    public ArrayList<ItemStack> putItemList(ArrayList<ItemStack> value) throws IOException {
        if (isReader() && hasLegacy(name + "_len")) {
            //noinspection deprecation
            return putItemArray_legacy(value);
        }
        return putItemList_efficient(value);
    }

    protected ArrayList<ItemStack> putItemList_efficient(ArrayList<ItemStack> value) throws IOException {
        //noinspection deprecation
        return putItemArray_legacy(value);
    }

    @Deprecated
    private ArrayList<ItemStack> putItemArray_legacy(ArrayList<ItemStack> value) throws IOException {
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

    public FzOrientation putFzOrientation(FzOrientation value) throws IOException {
        return putEnum(value);
    }

    public UUID putUUID(UUID uuid) throws IOException {
        String base_name = name;
        if (isReader()) {
            long msb = asSameShare(base_name + "MSB").putLong(0);
            long lsb = asSameShare(base_name + "LSB").putLong(0);
            return new UUID(msb, lsb);
        } else {
            asSameShare(base_name + "MSB").putLong(uuid.getMostSignificantBits());
            asSameShare(base_name + "LSB").putLong(uuid.getLeastSignificantBits());
            return uuid;
        }
    }

    public ItemStack putItemStack(ItemStack value) throws IOException {
        if (isReader()) {
            NBTTagCompound tag = putTag(new NBTTagCompound());
            ItemStack ret = ItemStack.loadItemStackFromNBT(tag);
            if (ret != null && ret.getItem() == null /* Indicating the NULL_ITEM */) {
                return null;
            }
            return ret;
        } else {
            ItemStack it = value == null ? DataUtil.NULL_ITEM : value;
            NBTTagCompound writtenTag = it.writeToNBT(new NBTTagCompound());
            putTag(writtenTag);
            return value;
        }
    }

    public FluidTank putTank(FluidTank tank) throws IOException {
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

    public AxisAlignedBB putBox(AxisAlignedBB box) throws IOException {
        double boxminX = asSameShare(name + ".minX").putDouble(box.minX);
        double boxmaxX = asSameShare(name + ".maxX").putDouble(box.maxX);
        double boxminY = asSameShare(name + ".minY").putDouble(box.minY);
        double boxmaxY = asSameShare(name + ".maxY").putDouble(box.maxY);
        double boxminZ = asSameShare(name + ".minZ").putDouble(box.minZ);
        double boxmaxZ = asSameShare(name + ".maxZ").putDouble(box.maxZ);
        return new AxisAlignedBB(boxminX, boxminY, boxminZ, boxmaxX, boxmaxY, boxmaxZ);
    }

    public Vec3 putVec3(Vec3 val) throws IOException {
        String prefix = name;
        double valxCoord = asSameShare(prefix + ".x").putDouble(val.xCoord);
        double valyCoord = asSameShare(prefix + ".y").putDouble(val.yCoord);
        double valzCoord = asSameShare(prefix + ".z").putDouble(val.zCoord);
        name = prefix;
        return new Vec3(valxCoord, valyCoord, valzCoord);
    }

    public <E extends Enum> E putEnum(E value) throws IOException {
        int i = putInt(value.ordinal());
        if (isWriter()) {
            return value;
        }
        return (E) value.getDeclaringClass().getEnumConstants()[i];
    }

    public Object putUnion(UnionEnumeration classes, Object val) throws IOException {
        Class<?> k;
        String origName = name;
        String typeName = origName + ".type";
        if (isWriter()) {
            byte index = classes.getIndex(val);
            asSameShare(typeName).putByte(index);
            k = classes.classByIndex(index);
        } else {
            byte index = asSameShare(typeName).putByte((byte) 0xFF);
            val = classes.byIndex(index);
            k = classes.classByIndex(index);
            if (val == null) throw new IOException("Tried to load invalid type with index: " + index);
        }
        asSameShare(origName);
        /*
for t in all_types:
    print("""if (k == %.class) return put%((%) val);""".replace('_', t.lower()).replace('%', t))
         */
        if (val instanceof IDataSerializable) return putIDS((IDataSerializable) val);
        if (k == Boolean.class) return putBoolean((Boolean) val);
        if (k == Byte.class) return putByte((Byte) val);
        if (k == Short.class) return putShort((Short) val);
        if (k == Integer.class) return putInt((Integer) val);
        if (k == Long.class) return putLong((Long) val);
        if (k == Float.class) return putFloat((Float) val);
        if (k == Double.class) return putDouble((Double) val);
        if (k == String.class) return putString((String) val);
        if (k == FzOrientation.class) return putFzOrientation((FzOrientation) val);
        if (k == UUID.class) return putUUID((UUID) val);
        if (k == ItemStack.class) return putItemStack((ItemStack) val);
        // if (k == ItemList.class) return putItemList((ItemList) val); // Type erasure means I can't tell...
        if (k == int[].class) return putIntArray((int[]) val);
        if (k == NBTTagCompound.class) return putTag((NBTTagCompound) val);
        if (k == FluidTank.class) return putTank((FluidTank) val);
        if (k == AxisAlignedBB.class) return putBox((AxisAlignedBB) val);
        if (k == Vec3.class) return putVec3((Vec3) val);
        if (val instanceof Enum) return putEnum((Enum) val);
        // End generated code. Gotta poke it a tad tho.
        throw new IOException("Unhandled class: " + k);
    }
}
