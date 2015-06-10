package factorization.constellation;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.algos.FastBag;
import factorization.shared.Core;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraftforge.client.event.RenderWorldLastEvent;

@SideOnly(Side.CLIENT)
public class ConstellationManager {
    static void addStar(IStar star) {
        FastBag<StarRegion> constellations = instance.constellations;
        if (!constellations.isEmpty()) {
            StarRegion last = constellations.get(constellations.size() - 1);
            if (last.couldEat(star)) {
                last.addStar(star);
                return;
            }
        }
        constellations.add(new StarRegion(star));
    }

    private ConstellationManager() {
        Core.loadBus(this);
    }
    private static final ConstellationManager instance = new ConstellationManager();
    private final FastBag<StarRegion> constellations = new FastBag<StarRegion>();

    private int lastIdx = 0;

    @SubscribeEvent
    public void tickStars(TickEvent.ClientTickEvent event) {
        if (constellations.isEmpty()) return;
        if (++lastIdx >= constellations.size()) {
            lastIdx = 0;
        }
        StarRegion sr = constellations.get(lastIdx);
        if (sr.isEmpty()) {
            constellations.remove(sr);
        } else {
            sr.tick(constellations);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH /* Step in front of other's GL state derpage */)
    public void draw(RenderWorldLastEvent event) {
        if (constellations.isEmpty()) return;
        Frustrum frustum = new Frustrum(); // Would be nice if we could yoink this from MC, no?
        BulkRender bulkRender = new BulkRender();
        for (StarRegion region : constellations) {
            region.draw(bulkRender, frustum);
        }
        bulkRender.finishDraw();
    }
}
