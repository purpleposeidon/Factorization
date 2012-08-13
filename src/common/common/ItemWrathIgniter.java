package factorization.common;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IInventory;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.World;
import factorization.api.Coord;
import factorization.api.IActOnCraft;

public class ItemWrathIgniter extends Item implements IActOnCraft {
    public ItemWrathIgniter(int par1) {
        super(par1);
        setMaxStackSize(1);
        setMaxDamage((6 * 2) - 1);
        setNoRepair();
    }

    @Override
    public boolean isDamageable() {
        return true;
    }

    @Override
    public String getTextureFile() {
        return Core.texture_file_item;
    }

    @Override
    public String getItemName() {
        return "item.wrathigniter";
    }

    @Override
    public String getItemNameIS(ItemStack par1ItemStack) {
        return getItemName();
    }

    //@Override hello, server.
    public int getIconFromDamage(int par1) {
        return (16 * 3) + 1;
    }

    @Override
    public boolean tryPlaceIntoWorld(ItemStack is, EntityPlayer player, World w, int x, int y, int z, int side, float vecx, float vecy, float vecz) {
        Coord baseBlock = new Coord(w, x, y, z);
        Coord fireBlock = baseBlock.copy().towardSide(side);
        if (fireBlock.getId() != 0) {
            if (!fireBlock.isAir()) {
                return true;
            }
        }
        is.damageItem(2, player);
        TileEntityWrathFire.ignite(baseBlock, fireBlock, player);
        return true;
    }

    @Override
    public void onCraft(ItemStack is, IInventory craftMatrix, int craftSlot, ItemStack result,
            EntityPlayer player) {
        if (result.isItemEqual(Core.registry.lamp_item)) {
            if (player != null) {
                is.damageItem(1, player);
            }
            else {
                is.setItemDamage(is.getItemDamage() - 1);
            }
            if (is.getItemDamage() <= is.getMaxDamage()) {
                is.stackSize += 1;
            }
        }
    }
}
