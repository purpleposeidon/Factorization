package factorization.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import factorization.api.Coord;
import factorization.common.Core.TabType;

public class ItemWrathIgniter extends ItemFactorization {
    public ItemWrathIgniter(int par1) {
        super(par1, "tool/wrath_igniter", TabType.TOOLS);
        setMaxStackSize(1);
        setMaxDamage((6 * 2) - 1);
        setNoRepair();
    }

    @Override
    public boolean isDamageable() {
        return true;
    }
    
    @Override
    public boolean onItemUse(ItemStack par1ItemStack,
            EntityPlayer par2EntityPlayer, World par3World, int par4, int par5,
            int par6, int par7, float par8, float par9, float par10) {
        return tryPlaceIntoWorld(par1ItemStack, par2EntityPlayer, par3World, par4, par5,
                par6, par7, par8, par9, par10);
    }
    
    public boolean tryPlaceIntoWorld(ItemStack is, EntityPlayer player, World w, int x, int y,
            int z, int side, float vecx, float vecy, float vecz) {
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
    public boolean hasContainerItem() {
        return true;
    }
    
    @Override
    public ItemStack getContainerItemStack(ItemStack is) {
        is = is.copy();
        is.setItemDamage(is.getItemDamage() + 1);
        if (is.getItemDamage() > getMaxDamage()) {
            is.stackSize = 0;
        }
        return is;
    }
    
    @Override
    public boolean doesContainerItemLeaveCraftingGrid(ItemStack par1ItemStack) {
        return false;
    }
}
