package factorization.servo;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import factorization.api.Coord;
import factorization.shared.Core;
import factorization.shared.Core.TabType;
import factorization.shared.FzUtil;
import factorization.shared.ItemFactorization;

public class ItemMatrixProgrammer extends ItemFactorization {
    public ItemMatrixProgrammer() {
        super("tool.matrix_programmer", TabType.TOOLS);
        setMaxStackSize(1);
        setContainerItem(this);
        Core.loadBus(this);
    }
    
    @Override
    public boolean doesContainerItemLeaveCraftingGrid(ItemStack par1ItemStack) {
        return false;
    }
    
    @EventHandler
    
    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (!player.isSneaking()) {
            if (Core.dev_environ && !world.isRemote) {
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
    
    @SubscribeEvent
    public void clickPainting(EntityInteractEvent event) {
        ItemStack is = event.entityPlayer.getHeldItem();
        if (is == null) return;
        if (is.getItem() != this) return;
        if (!(event.target instanceof EntityPainting)) {
            return;
        }
        int d = event.entityPlayer.isSneaking() ? -1 : 1;
        EntityPainting painting = (EntityPainting) event.target;
        if (painting.worldObj.isRemote) {
            return;
        }
        EntityPainting.EnumArt origArt = painting.art;
        EntityPainting.EnumArt art = origArt;
        int hangingDirection = painting.hangingDirection;
        while (true) {
            art = FzUtil.shiftEnum(art, EntityPainting.EnumArt.values(), d);
            painting.setDirection(hangingDirection);
            painting.art = art;
            if (art == origArt) {
                return;
            }
            if (!painting.onValidSurface()) {
                continue;
            }
            NBTTagCompound save = new NBTTagCompound();
            painting.writeToNBTOptional(save);
            painting.setDead();
            Entity newPainting = EntityList.createEntityFromNBT(save, event.target.worldObj);
            newPainting.worldObj.spawnEntityInWorld(newPainting);
            break;
        }
    }
}
