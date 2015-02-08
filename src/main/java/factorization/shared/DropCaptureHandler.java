package factorization.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import factorization.util.ItemUtil;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.world.BlockEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public enum DropCaptureHandler {
    CATCHER;
    
    private DropCaptureHandler() {
        Core.loadBus(this);
    }
    
    public static void startCapture(ICaptureDrops catcher) {
        CATCHER.catchers.set(catcher);
    }
    
    public static void endCapture() {
        CATCHER.catchers.set(null);
    }
    
    private ThreadLocal<ICaptureDrops> catchers = new ThreadLocal<ICaptureDrops>();
    
    void removeInvalids(Collection<ItemStack> drops) {
        for (Iterator<ItemStack> it = drops.iterator(); it.hasNext();) {
            if (ItemUtil.normalize(it.next()) == null) {
                it.remove();
            }
        }
    }
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void captureBlockDrops(BlockEvent.HarvestDropsEvent event) {
        ICaptureDrops catcher = catchers.get();
        if (catcher == null) return;
        removeInvalids(event.drops);
        if (catcher.captureDrops(event.x, event.y, event.z, event.drops)) {
            removeInvalids(event.drops);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void captureMobDrops(LivingDropsEvent event) {
        ICaptureDrops catcher = catchers.get();
        if (catcher == null) return;
        ArrayList<ItemStack> drops = new ArrayList();
        for (EntityItem ent : event.drops) {
            drops.add(ent.getEntityItem());
        }
        removeInvalids(drops);
        if (catcher.captureDrops((int)event.entity.posX, (int)event.entity.posY, (int)event.entity.posZ, drops)) {
            for (Iterator<EntityItem> it = event.drops.iterator(); it.hasNext();) {
                EntityItem ent = it.next();
                if (ent == null || ItemUtil.normalize(ent.getEntityItem()) == null) {
                    it.remove();
                }
            }
        }
    }
}
