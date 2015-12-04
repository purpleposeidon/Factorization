package factorization.fzds;

import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

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
