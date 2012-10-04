package factorization.common;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IInventory;
import net.minecraft.src.ItemStack;
import factorization.api.IActOnCraft;

public class ItemMatrixProgrammer extends ItemCraftingComponent implements IActOnCraft {
    public ItemMatrixProgrammer(int id, String itemName, int icon) {
        super(id, itemName, icon);
    }

    @Override
    public void onCraft(ItemStack is, IInventory craftMatrix, int craftSlot, ItemStack result, EntityPlayer player) {
        is.stackSize++;
    }
    
}
