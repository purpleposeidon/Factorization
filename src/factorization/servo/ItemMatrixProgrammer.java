package factorization.servo;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import factorization.api.Coord;
import factorization.shared.Core;
import factorization.shared.ItemFactorization;
import factorization.shared.Core.TabType;

public class ItemMatrixProgrammer extends ItemFactorization {
    public ItemMatrixProgrammer(int id) {
        super(id, "tool.matrix_programmer", TabType.TOOLS);
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
            if (Core.dev_environ && !world.isRemote) {
                Coord c = new Coord(world, x, y, z);
                player.addChatMessage("" + c);
                player.addChatMessage("id: " + c.getId() + " md: " + c.getMd());
            }
            return false;
        }
        Coord c = new Coord(world, x, y, z);
        TileEntityServoRail rail = c.getTE(TileEntityServoRail.class);
        if (rail == null) {
            return false;
        }
        rail.priority = 0;
        Decorator decor = rail.getDecoration();
        if (decor == null) {
            return false;
        }
        if (!decor.isFreeToPlace() && !player.capabilities.isCreativeMode && !world.isRemote) {
            c.spawnItem(decor.toItem());
        }
        rail.setDecoration(null);
        c.redraw();
        return false;
    }
    
    @Override
    public boolean isItemTool(ItemStack is) {
        return true;
    }
}
