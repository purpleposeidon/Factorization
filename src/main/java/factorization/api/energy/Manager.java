package factorization.api.energy;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * This class is internal.
 */
enum Manager {
    INSTANCE;

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        while (true) {
            IWorkerContext context = queue.poll();
            if (context == null) break;
            if (!context.isValid()) return;
            for (WorkUnit unit : unitPrototypes) {
                if (context.give(unit, true) == IWorker.Accepted.NEVER) continue;
                for (IEnergyNet net : unit.listener.nets) {
                    net.workerAdded(context, unit);
                }
            }
        }
    }

    private static WorkUnit[] unitPrototypes = new WorkUnit[0]; // Array for ease of iteration.
    private static final Queue<IWorkerContext> queue = Queues.newConcurrentLinkedQueue();
    private static final List<IEnergyNet> enets = Lists.newArrayList();
    static final Map<ResourceLocation, WorkUnit> prototypesByName = Maps.newHashMap();
    static {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    static void addWorker(IWorkerContext context) {
        if (!context.isManageable()) return;
        Manager.queue.add(context);
    }

    static void invalidateWorker(IWorkerContext context) {
        for (WorkUnit unit : unitPrototypes) {
            if (context.give(unit, true) == IWorker.Accepted.NEVER) continue;
            for (IEnergyNet net : unit.listener.nets) {
                net.workerDestroyed(context);
            }
        }
    }

    static void needsPower(IWorkerContext context) {
        for (WorkUnit unit : unitPrototypes) {
            if (context.give(unit, true) == IWorker.Accepted.NEVER) continue;
            for (IEnergyNet net : unit.listener.nets) {
                net.workerNeedsPower(context);
            }
        }
    }

    static boolean offer(IWorkerContext source, WorkUnit unit) {
        for (IEnergyNet net : unit.listener.nets) {
            if (net.injectPower(source, unit)) {
                return true;
            }
        }
        return false;
    }

    static void registerNet(IEnergyNet enet) {
        if (enets.contains(enet)) {
            throw new IllegalArgumentException("Already registered: " + enet);
        }
        enets.add(enet);
        for (WorkUnit unit : unitPrototypes) {
            if (enet.canHandlePower(unit)) {
                unit.listener.registerEnet(enet);
            }
        }
    }

    static void registerUnitPrototype(WorkUnit unit) {
        for (WorkUnit old : unitPrototypes) {
            if (old.name.equals(unit.name)) {
                throw new IllegalArgumentException("Unit " + unit.name + " was already registered");
            }
        }
        for (IEnergyNet net : enets) {
            if (net.canHandlePower(unit)) {
                unit.listener.registerEnet(net);
            }
        }
        unitPrototypes = ArrayUtils.add(unitPrototypes, unit);
        prototypesByName.put(unit.name, unit);
    }

    static class ListenerList {
        private static final IEnergyNet[] NONE = new IEnergyNet[0];
        IEnergyNet[] nets = NONE;

        void registerEnet(IEnergyNet net) {
            if (ArrayUtils.contains(nets, net)) return;
            nets = ArrayUtils.add(nets, net);
        }
    }
}
