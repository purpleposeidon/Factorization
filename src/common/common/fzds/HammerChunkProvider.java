package factorization.common.fzds;

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
import factorization.api.Coord;

public class HammerChunkProvider implements IChunkProvider {
    //TODO: Move to HammerManager
    //each cell is a few chunks wide, with chunks of bedrock between.
    static int cellWidth = 3;
    static int wallWidth = 16;
    static int wallHeight = 16*8; //for production: 16*10 (16*8 is the minimum or something)
    
    private World world;
    
    public HammerChunkProvider(World world) {
        this.world = world;
    }
    
    @Override
    public boolean chunkExists(int var1, int var2) {
        return true;
    }
    
    public static Coord getCellStart(World hammerWorld, int cellIndex) {
        int cellSize = (cellWidth + wallWidth)*16;
        return new Coord(hammerWorld, cellSize*cellIndex - 2, wallHeight, (1 + cellWidth)*16/2);
        //return new Coord(hammerWorld, cellSize*cellIndex, wallHeight, cellSize / 2);
    }

    @Override
    public Chunk provideChunk(int chunkX, int chunkZ) {
        if (chunkZ < -1  || chunkZ > cellWidth || chunkX < -1) {
            return new Chunk(world, chunkX, chunkZ);
        }
        int totalWidth = cellWidth + wallWidth;
        int x = chunkX % totalWidth;
        int z = chunkZ % totalWidth;
        if (x < 0) x += totalWidth;
        if (z < 0) z += totalWidth;
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
    public void recreateStructures(int var1, int var2) { }

}
