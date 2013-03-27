package factorization.common;

import net.minecraft.item.ItemStack;

public class ItemMatrixProgrammer extends ItemCraftingComponent {
    public ItemMatrixProgrammer(int id, String itemName) {
        super(id, itemName);
        setMaxStackSize(1);
        setContainerItem(this);
    }
    
    @Override
    public boolean doesContainerItemLeaveCraftingGrid(ItemStack par1ItemStack) {
        return false;
    }
}
