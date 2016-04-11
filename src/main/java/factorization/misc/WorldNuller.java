package factorization.misc;

import com.google.common.collect.Lists;
import factorization.shared.Core;
import factorization.util.FzUtil;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;

public class WorldNuller {
    public static String disablePropName = "fz.misc.disableUnloadedWorldNulling";
    public static boolean nullificationDisabled = Boolean.getBoolean(disablePropName);

    public static void init() {
        if (nullificationDisabled) return;

        Core.loadBus(new ServerNuller());
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            Core.loadBus(new ClientNuller());
        }
    }

    @SubscribeEvent
    public void queueWorldNull(WorldEvent.Unload event) {
        if (nullificationDisabled) return;
        unload_queue.add(new UnloadEntry(event.world));
    }

    static class UnloadEntry {
        private static Logger logger = LogManager.getLogger("worldNulling");
        WeakReference<World> worldRef;
        long timeOfUnload;
        String name;
        static final String delayPropName = "fz.misc.worldNullingDelay";
        static final int DELAY = 1000 * Integer.parseInt(System.getProperty(delayPropName, "30"));
        static {
            if (DELAY <= 0) {
                nullificationDisabled = true;
            }
        }

        UnloadEntry(World world) {
            this.worldRef = new WeakReference<World>(world);
            name = "<" + world.toString() + " " + FzUtil.getWorldDimension(world) + " " + world.getChunkProvider().makeString() + ">";
        }

        static void log(String msg) {
            logger.info(msg);
        }

        static boolean spam = true;

        boolean tick(long now) {
            long passedTime = now - timeOfUnload;
            if (passedTime < DELAY) return false;
            World world = worldRef.get();
            int secs = DELAY / 1000;
            if (world == null) {
                log("Unloaded world " + name + " was garbage collected.");
                return true;
            }
            log("Unloaded world " + name + " is still hanging around after " + secs + " seconds.");
            if (spam) {
                log("This may be due to a world leak, or there is little memory pressure.");
                log("It is about to have its fields nulled out. Use JVM option -D" + disablePropName + "=true to disable this.");
                log("Use -D" + delayPropName + "=<seconds> to adjust the wait time.");
                spam = false;
            }
            for (Field field : world.getClass().getFields()) {
                field.setAccessible(true);
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (field.getType().isPrimitive()) {
                    continue;
                }
                try {
                    field.set(world, null);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            log("Nullification completed.");
            return true;
        }
    }

    final List<UnloadEntry> unload_queue = Lists.newArrayList();
    void tick(TickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (unload_queue.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (Iterator<UnloadEntry> iter = unload_queue.iterator(); iter.hasNext(); ) {
            UnloadEntry e = iter.next();
            if (e.tick(now)) {
                iter.remove();
            }
        }
    }

    public static class ServerNuller extends WorldNuller {
        @SubscribeEvent
        public void serverTick(TickEvent.ServerTickEvent event) {
            tick(event);
        }
    }

    public static class ClientNuller extends WorldNuller {
        @SubscribeEvent
        public void clientTick(TickEvent.ClientTickEvent event) {
            tick(event);
        }
    }
}
