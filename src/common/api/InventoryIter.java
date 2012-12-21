package factorization.api;

import java.util.ArrayList;
import java.util.Iterator;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class InventoryIter implements Iterator<ItemStack> {
    ArrayList<InvIter> iters = new ArrayList();
    private ItemStack currentItem = null;
    private InvIter currentItemIter = null;

    public InventoryIter(IInventory inv, int start, int end) {
        this.iters.add(new IInventoryIter(inv, start, end));
        currentItemIter = iters.get(0);
    }

    public InventoryIter(IInventory inv) {
        this(inv, 0, inv.getSizeInventory());
    }

    public ItemStack next() {
        if (!currentItemIter.hasNext()) {
            iters.remove(0);
        }
        currentItemIter = iters.get(0);
        currentItem = (ItemStack) currentItemIter.next();
        currentItem = currentItem.copy(); //reminds you to call updateItem()
        return currentItem;
    }

    public boolean hasNext() {
        if (iters.size() > 1) {
            return true;
        }
        return iters.get(0).hasNext();
    }

    public void updateItem() {
        if (currentItem.stackSize == 0) {
            currentItem = null;
        }
        currentItemIter.updateItem();
    }

    @Override
    @Deprecated
    public void remove() {
    }

    //internals

    private interface InvIter extends Iterator {
        void updateItem();
    }

    private class IInventoryIter implements InvIter {
        private IInventory inv;
        private int index, endIndex;

        public IInventoryIter(IInventory inv, int startIndex, int endIndex) {
            this.inv = inv;
            this.index = startIndex;
            this.endIndex = endIndex;
        }

        @Override
        public boolean hasNext() {
            if (index == endIndex) {
                return false;
            }
            return index < inv.getSizeInventory();
        }

        @Override
        public Object next() {
            index++;
            ItemStack is = inv.getStackInSlot(index);
            if (is != null && is.getItem() instanceof ISubInventory) {
                iters.add(new ISubInventoryIter(is));
            }
            return is;
        }

        @Override
        public void updateItem() {
            inv.setInventorySlotContents(index - 1, currentItem);
        }

        @Override
        @Deprecated
        public void remove() {
        }
    }

    private class ISubInventoryIter implements InvIter {
        private ItemStack is;
        private ISubInventory sub;
        private int index = 0;

        public ISubInventoryIter(ItemStack is) {
            this.is = is;
            this.sub = (ISubInventory) is.getItem();
        }

        @Override
        public boolean hasNext() {
            return index < sub.getSizeInventory(is);
        }

        @Override
        public Object next() {
            index++;
            return sub.getStackInSlot(is, index - 1);
        }

        @Override
        public void updateItem() {
            sub.setInventorySlotContents(is, index - 1, currentItem);
        }

        @Override
        @Deprecated
        public void remove() {
        }
    }
}
