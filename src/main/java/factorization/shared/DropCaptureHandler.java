package factorization.shared;

import factorization.api.Coord;
import factorization.util.ItemUtil;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public enum DropCaptureHandler {
    CATCHER;
    
    private DropCaptureHandler() {
        Core.loadBus(this);
    }

    private static class Capturer {
        final ICaptureDrops net;
        final Coord src;
        final double distSq;

        private Capturer(ICaptureDrops net, Coord src, double distSq) {
            this.net = net;
            this.src = src;
            this.distSq = distSq;
        }

        boolean passes(World w, double x, double y, double z) {
            if (w != src.w) return false;
            double dx = x - src.x;
            double dy = y - src.y;
            double dz = z - src.z;

            return dx + dy + dz <= distSq;
        }
    }
    
    public static void startCapture(ICaptureDrops catcher, Coord src, double maxDist) {
        CATCHER.catchers.set(new Capturer(catcher, src, maxDist * maxDist));
    }
    
    public static void endCapture() {
        CATCHER.catchers.set(null);
    }
    
    private ThreadLocal<Capturer> catchers = new ThreadLocal<Capturer>();
    
    void removeInvalids(Collection<ItemStack> drops) {
        for (Iterator<ItemStack> it = drops.iterator(); it.hasNext();) {
            if (ItemUtil.normalize(it.next()) == null) {
                it.remove();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void captureBlockDrops(BlockEvent.HarvestDropsEvent event) {
        Capturer capturer = catchers.get();
        if (capturer == null) return;
        if (!capturer.passes(event.world, event.x, event.y, event.z)) return;
        removeInvalids(event.drops);
        if (capturer.net.captureDrops(event.drops)) {
            removeInvalids(event.drops);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void captureMobDrops(LivingDropsEvent event) {
        Capturer capturer = catchers.get();
        if (capturer == null) return;
        if (!capturer.passes(event.entity.worldObj, event.entity.posX, event.entity.posY, event.entity.posZ)) return;
        ArrayList<ItemStack> drops = new ArrayList();
        for (EntityItem ent : event.drops) {
            drops.add(ent.getEntityItem());
        }
        removeInvalids(drops);
        if (capturer.net.captureDrops(drops)) {
            for (Iterator<EntityItem> it = event.drops.iterator(); it.hasNext();) {
                EntityItem ent = it.next();
                if (ent == null || ItemUtil.normalize(ent.getEntityItem()) == null) {
                    it.remove();
                }
            }
        }
    }
}
