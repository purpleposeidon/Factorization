package factorization.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import factorization.api.Coord;
import factorization.common.servo.Decorator;
import factorization.common.servo.Instruction;
import factorization.common.servo.TileEntityServoRail;

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
    
    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (!player.isSneaking()) {
            return false;
        }
        Coord c = new Coord(world, x, y, z);
        TileEntityServoRail rail = c.getTE(TileEntityServoRail.class);
        if (rail == null) {
            return false;
        }
        Decorator decor = rail.getDecoration();
        if (decor == null) {
            return false;
        }
        if (!decor.isFreeToPlace() && !player.capabilities.isCreativeMode && !world.isRemote) {
            c.spawnItem(decor.toItem());
        }
        rail.setDecoration(null);
        c.redraw();
        return true;
    }
}
