package factorization.api.energy;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is internal.
 */
enum WorkerBoss {
    INSTANCE;

    static final ArrayList<IContext> queue = Lists.newArrayList();
    static final Multimap<ResourceLocation, IEnergyNet> watchers = ArrayListMultimap.create();
    static final List<WorkUnit> knownUnits = Lists.newArrayList();
    static final List<ResourceLocation> knownNames = Lists.newArrayList();
    static {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (queue.isEmpty()) return;
        for (IContext context : queue) {
            if (!context.isValid()) return;
            for (WorkUnit unit : knownUnits) {
                if (context.give(unit, true) == IWorker.Accepted.NEVER) continue;
                for (IEnergyNet net : watchers.get(unit.name)) {
                    net.add(context, unit);
                }
            }
        }
        queue.clear();
    }

    static void addWorker(IContext context) {
        if (!context.isManageable()) return;
        WorkerBoss.queue.add(context);
    }

    static void invalidateWorker(IContext context) {
        for (WorkUnit unit : knownUnits) {
            if (context.give(unit, true) == IWorker.Accepted.NEVER) continue;
            for (IEnergyNet net : watchers.get(unit.name)) {
                net.destroyed(context);
            }
        }
    }

    static void registerNet(IEnergyNet watcher, WorkUnit[] units) {
        for (WorkUnit unit : units) {
            WorkerBoss.watchers.put(unit.name, watcher);
            if (!WorkerBoss.knownNames.contains(unit.name)) {
                WorkerBoss.knownNames.add(unit.name);
                WorkerBoss.knownUnits.add(unit);
            }
        }
    }

    static void needsPower(IContext context) {
        for (WorkUnit unit : knownUnits) {
            if (context.give(unit, true) == IWorker.Accepted.NEVER) continue;
            for (IEnergyNet net : watchers.get(unit.name)) {
                net.needsPower(context);
            }
        }
    }
}
