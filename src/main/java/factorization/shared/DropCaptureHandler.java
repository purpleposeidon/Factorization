package factorization.shared;

import java.util.ArrayList;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class DropCaptureHandler {

    public static ICaptureDrops catcher;
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void captureBlockDrops(BlockEvent.HarvestDropsEvent event) {
        if(catcher==null)
            return;
        if (catcher.captureDrops(event.x, event.y, event.z, event.drops))
            event.drops.clear();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void captureMobDrops(LivingDropsEvent event) {
        if(catcher==null)
            return;
        ArrayList<ItemStack> drops=new ArrayList();
        for(EntityItem ent : event.drops) {
            drops.add(ent.getEntityItem());
        }
        if (catcher.captureDrops((int)event.entity.posX, (int)event.entity.posY, (int)event.entity.posZ, drops))
            event.drops.clear();
    }
}
