package factorization.common;

import factorization.api.Coord;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.World;
import net.minecraft.src.forge.ITextureProvider;

public class ItemMachineUpgrade extends Item implements ITextureProvider {
    FactoryType machineType;
    public int upgradeId;
    String name;

    protected ItemMachineUpgrade(int id, String name, FactoryType machineType, int upgradeId) {
        super(id);
        this.machineType = machineType;
        this.upgradeId = upgradeId;
        this.name = name;
        setItemName(this.name);
        setIconIndex(9 * 16 + upgradeId);
        setMaxStackSize(16);
        Core.instance.addName(this, this.name);
    }

    @Override
    public String getTextureFile() {
        return Core.texture_file_item;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int X, int Y,
            int Z, int side) {
        Coord here = new Coord(world, X, Y, Z);
        TileEntityFactorization te = here.getTE(TileEntityFactorization.class);
        if (te.takeUpgrade(stack)) {
            if (!player.capabilities.isCreativeMode) {
                stack.stackSize--;
            }
            return true;
        }
        return false;
    }

}
