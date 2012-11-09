package factorization.common.astro;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.src.Block;
import net.minecraft.src.Chunk;
import net.minecraft.src.ChunkPosition;
import net.minecraft.src.EnumCreatureType;
import net.minecraft.src.EnumSkyBlock;
import net.minecraft.src.IChunkProvider;
import net.minecraft.src.IProgressUpdate;
import net.minecraft.src.World;

class WEChunkProvider implements IChunkProvider {
    WEWorld wew;
    Chunk myChunk = null;
    public WEChunkProvider(WEWorld wew) {
        this.wew = wew;
    }

    @Override
    public boolean chunkExists(int chunkX, int chunkZ) {
        return chunkX == 0 && chunkZ == 0;
    }

    @Override
    public Chunk provideChunk(int chunkX, int chunkZ) {
        if (myChunk != null) {
            return myChunk;
        }
        Chunk ret = myChunk = new Chunk(wew, chunkX, chunkZ);
        int y = 0;
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 5; z++) {
                ret.setBlockID(x + 1, y, z + 1, x + 1);
            }
        }
        y = 5;
        for (int x = 1; x < 8; x++) {
            for (int z = 1; z < 5; z++) {
                if (Math.abs(x + z) % 2 == 0) {
                    ret.setBlockID(x + 1, y, z + 1, Block.glowStone.blockID);
                }
            }
        }
        
        ret.setBlockID(4, 6, 4, Block.waterMoving.blockID);
        ret.setBlockID(4, 7, 4, Block.ice.blockID);
        ret.setBlockID(4, 8, 4, Block.torchWood.blockID);
        ret.setBlockID(4, 9, 4, Block.wood.blockID);
        
        ret.generateSkylightMap();
        ret.updateSkylight();
        ret.resetRelightChecks();
        ret.enqueueRelightChecks();
        
        for (int x = 0; x < 10; x++) {
            for (y = 0; y < 16; y++) {
                for (int z = 0; z < 10; z++) {
                    ret.setLightValue(EnumSkyBlock.Block, x, y, z, 15);
                    ret.setLightValue(EnumSkyBlock.Sky, x, y, z, 15);
                }
            }
        }
        return ret;
    }

    @Override
    public Chunk loadChunk(int chunkX, int chunkZ) { return null; }

    @Override
    public void populate(IChunkProvider var1, int chunkX, int chunkZ) { }

    @Override
    public boolean saveChunks(boolean var1, IProgressUpdate var2) { return false; }

    @Override
    public boolean unload100OldestChunks() { return false; }

    @Override
    public boolean canSave() { return false; }

    @Override
    public String makeString() {
        return "WorldEntityProvider";
    }

    @Override
    public List getPossibleCreatures(EnumCreatureType var1, int var2, int var3, int var4) { return new ArrayList(0); }

    @Override
    public ChunkPosition findClosestStructure(World var1, String var2, int var3, int var4, int var5) { return null; }

    @Override
    public int getLoadedChunkCount() { return 0; }

    @Override
    public void func_82695_e(int var1, int var2) {} //This is the "add terrain features" function.
    
}