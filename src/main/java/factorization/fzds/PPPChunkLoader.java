package factorization.fzds;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeChunkManager;

import java.util.List;
import java.util.Set;

public class PPPChunkLoader implements ForgeChunkManager.LoadingCallback {
    static PPPChunkLoader instance;
    PPPChunkLoader() {
        instance = this;
    }

    @Override
    public void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world) {
        // Dispose of all the tickets. It's possible that an IDC is chunk-loaded. If this is the case, then it will spawn a PPP which will cause the chunk to reload.
        // But (I guess?) this wouldn't cause chunks to unload & reload, just take longer to load up.
        // (Also, even if it did cause chunks to unload it's likely they wouldn't actually be unloaded for a while.)
        for (ForgeChunkManager.Ticket ticket : tickets) {
            ForgeChunkManager.releaseTicket(ticket);
        }
    }

    public ForgeChunkManager.Ticket register(Set<Chunk> chunkSet) {
        ForgeChunkManager.Ticket ticket = ForgeChunkManager.requestTicket(Hammer.instance, DeltaChunk.getServerShadowWorld(), ForgeChunkManager.Type.NORMAL);
        if (ticket == null) {
            Hammer.logSevere("Failed to acquire chunk ticket. You may need to adjust config/forgeChunkLoading.cfg");
            return null;
        }
        final int maxSize = ticket.getMaxChunkListDepth();
        final int size = chunkSet.size();
        if (maxSize > size) {
            Hammer.logSevere("Registering %s chunks for loading, but ticket only has room for %s. You may need to adjust config/forgeChunkLoading.cfg", size, maxSize);
        }
        for (Chunk chunk : chunkSet) {
            ForgeChunkManager.forceChunk(ticket, chunk.getChunkCoordIntPair());
        }
        return ticket;
    }

    public void release(ForgeChunkManager.Ticket ticket) {
        if (ticket == null) return;
        ForgeChunkManager.releaseTicket(ticket);
    }
}
