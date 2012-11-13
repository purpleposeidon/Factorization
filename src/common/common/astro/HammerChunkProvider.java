package factorization.common.astro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.src.Block;
import net.minecraft.src.Chunk;
import net.minecraft.src.ChunkPosition;
import net.minecraft.src.EnumCreatureType;
import net.minecraft.src.IChunkProvider;
import net.minecraft.src.IProgressUpdate;
import net.minecraft.src.World;

public class HammerChunkProvider implements IChunkProvider {

    //each cell is 3 chunks wide, with a chunk of bedrock between
    int cellWidth = 3;
    int wallWidth = 1;
    int wallHeight = 16*10;
    
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
        int totalWidth = cellWidth + wallWidth;
        int x = chunkX % totalWidth;
        int z = chunkZ % totalWidth;
        if (x < 0) x += totalWidth;
        if (z < 0) z += totalWidth;
        System.out.println(x + " & " + z);
        if (x >= cellWidth || z >= cellWidth) {
            //this is a wall chunk
            byte bedrock[] = new byte[16*16*wallHeight];
            Arrays.fill(bedrock, (byte)Block.bedrock.blockID);
            return new Chunk(world, bedrock, chunkX, chunkZ);
        } else {
            //this is a cell chunk
            return new Chunk(world, chunkX, chunkZ);
        }
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
    public boolean unload100OldestChunks() {
        // ??? Wtf is this?
        return false;
    }

    @Override
    public boolean canSave() {
        return true;
    }

    @Override
    public String makeString() {
        return "FZHammarChunkSource";
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
    public void func_82695_e(int var1, int var2) {
        // Generates special terrain features or something. Not needed here.
    }

}
