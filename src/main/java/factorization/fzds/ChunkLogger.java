package factorization.fzds;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.world.ChunkEvent;

public class ChunkLogger {
    @SubscribeEvent
    public void chunkLoad(ChunkEvent.Load load) {
        if (load.world == DeltaChunk.getClientShadowWorld()) {
            Hammer.logInfo("Load %s", load.getChunk().getChunkCoordIntPair());
        }
    }

    @SubscribeEvent
    public void chunkUnload(ChunkEvent.Unload unload) {
        if (unload.world == DeltaChunk.getClientShadowWorld()) {
            Hammer.logInfo("Unload %s", unload.getChunk().getChunkCoordIntPair());
            Thread.dumpStack();
        }
    }
}
