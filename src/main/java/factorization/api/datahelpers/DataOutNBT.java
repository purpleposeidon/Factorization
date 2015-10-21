package factorization.api.datahelpers;

import java.io.IOException;
import java.util.ArrayList;

import factorization.util.DataUtil;
import factorization.util.ItemUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

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
    protected <E> E putImplementation(E value) throws IOException {
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
            tag.setTag(name, (NBTTagCompound) value);
        }
        return value;
    }


    @Override
    protected ArrayList<ItemStack> putItemList_efficient(ArrayList<ItemStack> value) throws IOException {
        if (value.isEmpty()) return value;
        NBTTagList buffer = new NBTTagList();
        for (ItemStack item : value) {
            if (ItemUtil.normalize(item) == null) continue;
            NBTTagCompound btag = new NBTTagCompound();
            item.writeToNBT(btag);
            buffer.appendTag(btag);
        }
        tag.setTag(name, buffer);
        return value;
    }

    @Override
    public ItemStack[] putItemArray(ItemStack[] value) throws IOException {
        NBTTagList buffer = new NBTTagList();
        for (ItemStack item : value) {
            if (item == null) item = DataUtil.NULL_ITEM;
            NBTTagCompound btag = new NBTTagCompound();
            item.writeToNBT(btag);
            buffer.appendTag(btag);
        }
        tag.setTag(name, buffer);
        return value;
    }

    @Override
    public int[] putIntArray(int[] value) throws IOException {
        tag.setIntArray(name, value);
        return value;
    }
}
