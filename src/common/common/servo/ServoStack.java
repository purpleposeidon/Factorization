package factorization.common.servo;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.FactorizationUtil;

public class ServoStack implements IDataSerializable, Iterable, IInventory {
    private LinkedList<Object> contents = new LinkedList<Object>();
    private final int maxSize = 16;

    public void setContentsList(LinkedList<Object> obj) {
        contents = obj;
    }

    public boolean push(Object o) {
        if (contents.size() >= maxSize) {
            return false;
        }
        contents.addFirst(o);
        return true;
    }

    public void forcePush(Object o) {
        contents.addFirst(o);
    }

    public boolean append(Object o) {
        if (contents.size() >= maxSize) {
            return false;
        }
        contents.add(o);
        return true;
    }

    public void forceAppend(Object o) {
        contents.add(o);
    }

    public Object pop() {
        if (contents.isEmpty()) {
            return null;
        }
        return contents.removeFirst();
    }

    public Object popEnd() {
        if (contents.isEmpty()) {
            return null;
        }
        return contents.removeLast();
    }

    public <E> E popType(Class<? extends E> eClass) {
        return popType(eClass, true);
    }

    public Object remove(int index) {
        if (index >= contents.size()) {
            return null;
        }
        return contents.remove(index); // At least n is small. By default. >_>
    }

    public <E> E findType(Class<? extends E> eClass) {
        return popType(eClass, false);
    }

    private <E> E popType(Class<? extends E> eClass, boolean remove) {
        Iterator<Object> it = contents.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (o.getClass() == eClass) {
                if (remove) {
                    it.remove();
                }
                return (E) o;
            }
        }
        return null;
    }

    @Override
    public Iterator<Object> iterator() {
        return contents.iterator();
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getFreeSpace() {
        return Math.max(0, maxSize - contents.size());
    }

    public int getSize() {
        return contents.size();
    }

    public void pushmergeItemStack(ItemStack toAdd) {
        for (Object o : this) {
            if (!(o instanceof ItemStack)) {
                continue;
            }
            ItemStack is = (ItemStack) o;
            if (FactorizationUtil.couldMerge(is, toAdd)) {
                int free_space = Math.max(is.getMaxStackSize() - is.stackSize, 0);
                int delta = Math.min(free_space, toAdd.stackSize);
                toAdd.stackSize -= delta;
                is.stackSize += delta;
                if (toAdd.stackSize <= 0) {
                    return;
                }
            }
        }
        push(toAdd);
    }

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        int length = data.asSameShare(prefix + "size").putInt(contents.size());
        if (length == 0) {
            return this;
        }
        prefix = prefix + "#";
        if (data.isWriter()) {
            int i = 0;
            for (Object o : contents) {
                if (o instanceof ItemStack) {
                    o = ((ItemStack) o).writeToNBT(new NBTTagCompound());
                }
                data.asSameShare(prefix + i++).putUntypedOject(o);
            }
        } else {
            contents.clear();
            for (int i = 0; i < length; i++) {
                Object n = data.asSameShare(prefix + i).putUntypedOject(null);
                if (n instanceof NBTTagCompound) {
                    n = ItemStack.loadItemStackFromNBT((NBTTagCompound) n);
                }
                if (n != null) {
                    contents.add(n);
                }
            }
        }
        return this;
    }
    
    private class DataServoStack extends DataHelper {
        final boolean reader;
        public DataServoStack(boolean reader) {
            this.reader = reader;
        }

        @Override
        protected boolean shouldStore(Share share) {
            return true;
        }

        @Override
        public boolean isReader() {
            return reader;
        }

        @Override
        protected <E> Object putImplementation(E value) throws IOException {
            if (reader) {
                E ret = (E) popType(value.getClass());
                if (ret == null) {
                    throw new IOException();
                }
                return ret;
            } else {
                push(value);
                return value;
            }
        }
        
    }
    
    public DataHelper getDataHelper(boolean reader) {
        return new DataServoStack(reader);
    }

    @Override
    public int getSizeInventory() {
        return maxSize;
    }

    private ItemStack getItem(int i) {
        Object o = contents.get(i);
        if (o instanceof ItemStack) {
            return (ItemStack) o;
        }
        return null;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        return getItem(i);
    }

    @Override
    public ItemStack decrStackSize(int i, int j) {
        ItemStack is = getItem(i);
        if (is == null) {
            return null;
        }
        return is.splitStack(j);
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int i) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack) {
        if (i > contents.size()) {
            contents.add(itemstack);
            return;
        }
        contents.set(i, itemstack);
    }

    @Override
    public String getInvName() {
        return "ServoStack";
    }

    @Override
    public boolean isInvNameLocalized() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void onInventoryChanged() {
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer entityplayer) {
        return false;
    }

    @Override
    public void openChest() {
    }

    @Override
    public void closeChest() {
    }

    @Override
    public boolean isStackValidForSlot(int i, ItemStack itemstack) {
        if (i == contents.size() && getFreeSpace() > 0) {
            return true;
        }
        return contents.get(i) instanceof ItemStack;
    }
}
