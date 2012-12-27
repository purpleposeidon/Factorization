package factorization.fzds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.ChunkPosition;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.World;
import factorization.api.Coord;

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
        if (chunkZ < -1  || chunkZ > Hammer.cellWidth || chunkX < -1) {
            return new Chunk(world, chunkX, chunkZ);
        }
        int totalWidth = Hammer.cellWidth + Hammer.wallWidth;
        int x = chunkX % totalWidth;
        int z = chunkZ % totalWidth;
        if (x < 0) x += totalWidth;
        if (z < 0) z += totalWidth;
        Chunk ret;
        if (x >= Hammer.cellWidth || z >= Hammer.cellWidth) {
            //this is a wall chunk
//			byte bedrock[] = new byte[16*16*16*8]; //8 seems to be the minimum # of cubic chunks
//			Arrays.fill(bedrock, 0, 16*16*Hammer.wallHeight, (byte)Block.bedrock.blockID);
            byte bedrock[] = new byte[16*16*Hammer.wallHeight];
            Arrays.fill(bedrock, (byte)Block.bedrock.blockID);
            ret = new Chunk(world, bedrock, chunkX, chunkZ);
        } else if (chunkX < 0 || chunkZ < 0 || chunkZ > Hammer.cellWidth) {
            //This is an outside chunk
            ret = new Chunk(world, chunkX, chunkZ);
        } else {
            //this is a cell chunk
            ret = new Chunk(world, chunkX, chunkZ);
//			for (int i = 0; i < 16; i++) {
//				for (int j = 0; j < 16; j++) {
//					ret.setBlockID(i, 5, j, 1);
//				}
//			}
        }
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
