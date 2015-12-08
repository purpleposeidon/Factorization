package factorization.api.energy;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;

public class WorkerManager {
    public static void addTileEntity(IWorker<TileEntity> tileEntity) {
        te_queue.add(tileEntity);
    }

    public static void addEntity(IWorker<Entity> entity) {
        ent_queue.add(entity);
    }

    public static void registerWatcher(IWatcher watcher, WorkUnit... units) {
        for (WorkUnit unit : units) {
            watchers.put(unit.name, watcher);
            if (!knownNames.contains(unit.name)) {
                knownNames.add(unit.name);
                knownUnits.add(unit);
            }
        }
    }


    public interface IWatcher {
        void addTileEntity(TileEntity tileEntity, WorkUnit unit);
        void addEntity(Entity entity, WorkUnit unit);
    }


    private static final ArrayList<IWorker<TileEntity>> te_queue = new ArrayList<IWorker<TileEntity>>();
    private static final ArrayList<IWorker<Entity>> ent_queue = new ArrayList<IWorker<Entity>>();
    private static final Multimap<String, IWatcher> watchers = ArrayListMultimap.create();
    private static final ArrayList<WorkUnit> knownUnits = new ArrayList<WorkUnit>();
    private static final ArrayList<String> knownNames = new ArrayList<String>();
    static {
        MinecraftForge.EVENT_BUS.register(new WorkerManager());
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        iterateQueue(te_queue, true);
        iterateQueue(ent_queue, false);
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    private <T> void iterateQueue(ArrayList<IWorker<T>> queue, boolean isTE) {
        if (queue.isEmpty()) return;
        for (IWorker<T> workerItem : queue) {
            T worker = (T) workerItem;
            if (isTE) {
                TileEntity te = (TileEntity) worker;
                if (te.isInvalid() || te.getWorld() == null || te.getWorld().isRemote) continue;
            } else {
                Entity ent = (Entity) worker;
                if (ent.isDead || ent.worldObj == null || ent.worldObj.isRemote) continue;
            }
            for (WorkUnit unit : knownUnits) {
                if (workerItem.canHandle(unit, true, worker, null, null) == IWorker.Accepted.NEVER) continue;
                for (IWatcher iw : watchers.get(unit.name)) {
                    if (isTE) {
                        iw.addEntity((Entity) worker, unit);
                    } else {
                        iw.addTileEntity((TileEntity) worker, unit);
                    }
                }
            }
        }
        queue.clear();
    }

}
