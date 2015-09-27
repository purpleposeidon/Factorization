package factorization.artifact;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SlotSane extends Slot {
    int mojangSlotIndex;
    public SlotSane(IInventory inv, int slotIndex, int x, int y) {
        super(inv, slotIndex, x, y);
        mojangSlotIndex = slotIndex;
    }

    @Override
    protected void onCrafting(ItemStack stack, int slot) {
        super.onCrafting(stack, slot);
        inventory.markDirty();
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return inventory.isItemValidForSlot(mojangSlotIndex, stack);
    }
}
