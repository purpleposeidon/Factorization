package factorization.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
