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
    public boolean putBoolean(boolean value) throws IOException {
        tag.setBoolean(name, value);
        return value;
    }

    @Override
    public byte putByte(byte value) throws IOException {
        tag.setByte(name, value);
        return value;
    }

    @Override
    public short putShort(short value) throws IOException {
        tag.setShort(name, value);
        return value;
    }

    @Override
    public int putInt(int value) throws IOException {
        tag.setInteger(name, value);
        return value;
    }

    @Override
    public long putLong(long value) throws IOException {
        tag.setLong(name, value);
        return value;
    }

    @Override
    public float putFloat(float value) throws IOException {
        tag.setFloat(name, value);
        return value;
    }

    @Override
    public double putDouble(double value) throws IOException {
        tag.setDouble(name, value);
        return value;
    }

    @Override
    public String putString(String value) throws IOException {
        tag.setString(name, value);
        return value;
    }

    @Override
    public NBTTagCompound putTag(NBTTagCompound value) throws IOException {
        tag.setTag(name, value);
        return value;
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
