package factorization.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public interface IActOnCraft {
    void onCraft(ItemStack is, IInventory craftMatrix, int craftSlot, ItemStack result, EntityPlayer player);
}
