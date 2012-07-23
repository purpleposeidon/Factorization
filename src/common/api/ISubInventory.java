package factorization.api;

import net.minecraft.src.ItemStack;

public interface ISubInventory {
    int getSizeInventory(Object o);

    ItemStack getStackInSlot(Object o, int slot);

    void setInventorySlotContents(Object o, int slot, ItemStack is);
}
