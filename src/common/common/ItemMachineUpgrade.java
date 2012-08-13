package factorization.common;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.World;
import factorization.api.Coord;

public class ItemMachineUpgrade extends Item {
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
        Core.proxy.addName(this, this.name);
    }

    @Override
    public String getTextureFile() {
        return Core.texture_file_item;
    }

    @Override
    public boolean tryPlaceIntoWorld(ItemStack stack, EntityPlayer player, World world, int X, int Y,
            int Z, int side, float vecx, float vecy, float vecz) {
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
