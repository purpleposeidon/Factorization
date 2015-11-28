package factorization.api.datahelpers;

import java.io.IOException;
import java.util.ArrayList;

import factorization.shared.Core;

import factorization.util.DataUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

public class DataInNBT extends DataHelperNBT {	
    public DataInNBT(NBTTagCompound theTag) {
        tag = theTag;
    }
    
    @Override
    protected boolean shouldStore(Share share) {
        return !share.is_transient /*&& tag.hasKey(name)*/;
    }
    
    @Override
    public boolean isReader() {
        return true;
    }

    @Override
    public boolean hasLegacy(String name) {
        return tag.hasKey(name);
    }

    @Override
    public DataHelper as(Share share, String set_name) {
        return super.as(share, set_name);
    }

    @Override
    public DataHelper asSameShare(String set_name) {
        return super.asSameShare(set_name);
    }

    private void err() {
        Core.logWarning("Failed to load " + name + "; will use default value.");
        Core.logWarning("The tag: " + tag);
    }

    @Override
    public boolean putBoolean(boolean value) throws IOException {
        try {
            return tag.getBoolean(name);
        } catch (Throwable t) {
            err();
            return value;
        }
    }

    @Override
    public byte putByte(byte value) throws IOException {
        try {
            return tag.getByte(name);
        } catch (Throwable t) {
            err();
            return value;
        }
    }

    @Override
    public short putShort(short value) throws IOException {
        try {
            return tag.getShort(name);
        } catch (Throwable t) {
            err();
            return value;
        }
    }

    @Override
    public int putInt(int value) throws IOException {
        try {
            return tag.getInteger(name);
        } catch (Throwable t) {
            err();
            return value;
        }
    }

    @Override
    public long putLong(long value) throws IOException {
        try {
            return tag.getLong(name);
        } catch (Throwable t) {
            err();
            return value;
        }
    }

    @Override
    public float putFloat(float value) throws IOException {
        try {
            return tag.getFloat(name);
        } catch (Throwable t) {
            err();
            return value;
        }
    }

    @Override
    public double putDouble(double value) throws IOException {
        try {
            return tag.getDouble(name);
        } catch (Throwable t) {
            err();
            return value;
        }
    }

    @Override
    public String putString(String value) throws IOException {
        try {
            return tag.getString(name);
        } catch (Throwable t) {
            err();
            return value;
        }
    }

    @Override
    public NBTTagCompound putTag(NBTTagCompound value) throws IOException {
        try {
            return tag.getCompoundTag(name);
        } catch (Throwable t) {
            err();
            return value;
        }
    }

    @Override
    protected ArrayList<ItemStack> putItemList_efficient(ArrayList<ItemStack> value) throws IOException {
        try {
            NBTTagList buffer = tag.getTagList(name, Constants.NBT.TAG_COMPOUND);
            final int size = buffer.tagCount();
            value.clear();
            value.ensureCapacity(size);
            for (int i = 0; i < size; i++) {
                NBTTagCompound it = buffer.getCompoundTagAt(i);
                ItemStack is = ItemStack.loadItemStackFromNBT(it);
                value.add(is);
            }
            return value;

        } catch (Throwable t) {
            err();
            return value;
        }
    }

    @Override
    public ItemStack[] putItemArray(ItemStack[] value) throws IOException {
        try {
            NBTTagList buffer = tag.getTagList(name, Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < value.length; i++) {
                NBTTagCompound tag = buffer.getCompoundTagAt(i);
                if (tag == null) {
                    value[i] = null;
                } else {
                    value[i] = ItemStack.loadItemStackFromNBT(tag);
                }
            }
            return value;
        } catch (Throwable t) {
            err();
            return value;
        }
    }

    @Override
    public int[] putIntArray(int[] value) throws IOException {
        try {
            return tag.getIntArray(name);
        } catch (Throwable t) {
            err();
            return value;
        }
    }
}
