package factorization.common;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import factorization.api.Coord;
import factorization.common.Core.TabType;

public class ItemMachineUpgrade extends Item {
    FactoryType machineType;
    public int upgradeId;
    String name, type;

    protected ItemMachineUpgrade(int id, String name, String type, FactoryType machineType, int upgradeId) {
        super(id);
        this.machineType = machineType;
        this.upgradeId = upgradeId;
        this.name = name;
        this.type = type;
        setUnlocalizedName("factorization:" + this.name);
        setMaxStackSize(16);
        Core.tab(this, TabType.MISC);
    }
    
    @Override
    public boolean onItemUse(ItemStack par1ItemStack,
            EntityPlayer par2EntityPlayer, World par3World, int par4, int par5,
            int par6, int par7, float par8, float par9, float par10) {
        return tryPlaceIntoWorld(par1ItemStack, par2EntityPlayer, par3World, par4, par5,
                par6, par7, par8, par9, par10);
    }

    public boolean tryPlaceIntoWorld(ItemStack stack, EntityPlayer player, World world,
            int X, int Y, int Z, int side, float vecx, float vecy, float vecz) {
        Coord here = new Coord(world, X, Y, Z);
        TileEntityCommon te = here.getTE(TileEntityCommon.class);
        if (te != null && te.takeUpgrade(stack)) {
            if (!player.capabilities.isCreativeMode) {
                stack.stackSize--;
            }
            here.redraw();
            return true;
        }
        return false;
    }

    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        list.add(type);
        Core.brand(list);
    }
}
