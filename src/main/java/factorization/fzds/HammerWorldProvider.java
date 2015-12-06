package factorization.fzds;

import net.minecraft.util.BlockPos;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

public class HammerWorldProvider extends WorldProvider {
    HammerChunkProvider chunkProvider = null;

    @Override
    protected void registerWorldChunkManager() {
        super.registerWorldChunkManager();
        setAllowedSpawnTypes(false, false);
    }

    @Override
    public void setAllowedSpawnTypes(boolean hostiles, boolean peaceful) {
        super.setAllowedSpawnTypes(false, false);
    }
    
    @Override
    public String getDimensionName() {
        return "FZHammer";
    }

    @Override
    public String getInternalNameSuffix() {
        return "_fzhammer";
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
    public BlockPos getRandomizedSpawnPoint() {
        return new BlockPos(0, 0, 0);
    }

    @Override
    public boolean canBlockFreeze(BlockPos pos, boolean byWater) {
        return false;
    }

    @Override
    public boolean canSnowAt(BlockPos pos, boolean checkLight) {
        return false;
    }

    @Override
    public boolean canDoRainSnowIce(Chunk chunk) {
        return false;
    }

    @Override
    public boolean canDoLightning(Chunk chunk) {
        return false;
    }
}
