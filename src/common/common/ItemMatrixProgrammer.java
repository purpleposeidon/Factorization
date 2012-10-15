package factorization.common;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IInventory;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import factorization.api.IActOnCraft;

public class ItemMatrixProgrammer extends ItemCraftingComponent {
    public ItemMatrixProgrammer(int id, String itemName, int icon) {
        super(id, itemName, icon);
        setMaxStackSize(1);
        setContainerItem(this);
    }
    
    @Override
    public boolean doesContainerItemLeaveCraftingGrid(ItemStack par1ItemStack) {
        return false;
    }
}
