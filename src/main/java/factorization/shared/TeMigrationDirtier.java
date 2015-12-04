package factorization.shared;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

public class TeMigrationDirtier {
    public static final TeMigrationDirtier instance = new TeMigrationDirtier();

    private TeMigrationDirtier() {
        Core.loadBus(this);
    }

    public void register(TileEntityCommon tec) {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) return;
        queue.add(new Entry(tec));
    }

    static class Entry {
        final WeakReference<TileEntityCommon> ref;
        int visits = 0;

        Entry(TileEntityCommon ref) {
            this.ref = new WeakReference<TileEntityCommon>(ref);
        }
    }

    ArrayList<Entry> queue = new ArrayList<Entry>();


    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        for (Iterator<Entry> iterator = queue.iterator(); iterator.hasNext(); ) {
            Entry entry = iterator.next();
            TileEntityCommon tec = entry.ref.get();
            if (tec == null) {
                iterator.remove();
                continue;
            }
            if (tec.hasWorldObj()) {
                tec.getCoord().getChunk().setChunkModified();
                iterator.remove();
            }
            if (entry.visits++ > 400) {
                iterator.remove();
            }
        }
    }
}
