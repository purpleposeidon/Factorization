package factorization.fzds;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.ChunkPosition;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.World;

public class HammerChunkProvider implements IChunkProvider {
    
    private World world;
    
    public HammerChunkProvider(World world) {
        this.world = world;
    }
    
    @Override
    public boolean chunkExists(int var1, int var2) {
        return true;
    }

    @Override
    public Chunk provideChunk(int chunkX, int chunkZ) {
        Chunk ret = new Chunk(world, chunkX, chunkZ);
        System.out.println("provideChunk: " + chunkX + " " + chunkZ + "   " + ret); //NORLEASE
        return ret;
    }

    @Override
    public Chunk loadChunk(int chunkX, int chunkZ) {
        return this.provideChunk(chunkX, chunkZ);
    }

    @Override
    public void populate(IChunkProvider var1, int var2, int var3) {}

    @Override
    public boolean saveChunks(boolean var1, IProgressUpdate var2) {
        //seems to be a callback for when the entire world is saved
        return true;
    }
    
    @Override
    public boolean unloadQueuedChunks() {
        // ??? Wtf is this?
        return false;
    }

    @Override
    public boolean canSave() {
        return true;
    }

    @Override
    public String makeString() {
        return "FzdsHammerChunkProvider";
    }

    @Override
    public List getPossibleCreatures(EnumCreatureType var1, int var2, int var3, int var4) {
        return new ArrayList();
    }

    @Override
    public ChunkPosition findClosestStructure(World var1, String var2, int var3, int var4, int var5) {
        return null;
    }

    @Override
    public int getLoadedChunkCount() {
        return 0;
    }
    
    @Override
    public void recreateStructures(int var1, int var2) { }

}
