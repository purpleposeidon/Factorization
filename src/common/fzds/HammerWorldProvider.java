package factorization.fzds;

import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.WorldProvider;

public class HammerWorldProvider extends WorldProvider {
    HammerChunkProvider chunkProvider = null;
    
    @Override
    public String getDimensionName() {
        return "FZHammer";
    }
    
    @Override
    public IChunkProvider createChunkGenerator() {
        if (chunkProvider == null) {
            chunkProvider = new HammerChunkProvider(worldObj);
        }
        return chunkProvider;
    }
    
    @Override
    public String getWelcomeMessage() {
        return "Entering FZ Hammerspace";
    }
    
    @Override
    public String getDepartMessage() {
        return "Leaving FZ Hammerspace";
    }
    
    @Override
    public boolean canRespawnHere() {
        return false;
    }
    
    @Override
    public boolean isSurfaceWorld() {
        return false;
    }
    
    @Override
    public double getVoidFogYFactor() {
        return 1D;
    }
    
    @Override
    public ChunkCoordinates getEntrancePortalLocation() {
        //err, this probably never gets called...
        return new ChunkCoordinates(0, 128, 00);
    }
    
    @Override
    public boolean getWorldHasVoidParticles() {
        return false;
    }
}
