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
    protected <E> E putImplementation(E o) throws IOException {
        if (!tag.hasKey(name)) {
            return o;
        }
        try {
            if (o instanceof Boolean) {
                return (E) (Boolean) tag.getBoolean(name);
            } else if (o instanceof Byte) {
                return (E) (Byte) tag.getByte(name);
            } else if (o instanceof Short) {
                return (E) (Short) tag.getShort(name);
            } else if (o instanceof Integer) {
                return (E) (Integer) tag.getInteger(name);
            } else if (o instanceof Long) {
                return (E) (Long) tag.getLong(name);
            } else if (o instanceof Float) {
                return (E) (Float) tag.getFloat(name);
            } else if (o instanceof Double) {
                return (E) (Double) tag.getDouble(name);			
            } else if (o instanceof String) {
                return (E) (String) tag.getString(name);
            } else if (o instanceof NBTTagCompound) {
                return (E) (NBTTagCompound) tag.getCompoundTag(name);
            }
        } catch (Throwable t) {
            Core.logWarning("Failed to load " + name + "; will use default value " + o);
            Core.logWarning("The tag: " + tag);
            t.printStackTrace();
        }
        return o;
    }
    
    @Override
    public boolean hasLegacy(String name) {
        return tag.hasKey(name);
    }

    @Override
    protected ArrayList<ItemStack> putItemList_efficient(ArrayList<ItemStack> value) throws IOException {
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
    }

    @Override
    public ItemStack[] putItemArray(ItemStack[] value) throws IOException {
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
    }

    @Override
    public int[] putIntArray(int[] value) throws IOException {
        return tag.getIntArray(name);
    }
}
