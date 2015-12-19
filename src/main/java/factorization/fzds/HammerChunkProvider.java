package factorization.fzds;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.BlockPos;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

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
    public Chunk provideChunk(BlockPos pos) {
        return provideChunk(pos.getX(), pos.getZ());
    }

    @Override
    public Chunk provideChunk(int x, int z) {
        Chunk chunk = new Chunk(world, x >> 4, z >> 4);
        chunk.generateSkylightMap();
        byte[] biomes = chunk.getBiomeArray();
        Arrays.fill(biomes, (byte) BiomeGenBase.plains.biomeID);
        chunk.setTerrainPopulated(true);
        return chunk;
    }

    @Override
    public void populate(IChunkProvider var1, int var2, int var3) {}

    @Override
    public boolean func_177460_a(IChunkProvider p_177460_1_, Chunk p_177460_2_, int p_177460_3_, int p_177460_4_) {
        return false; // Not sure what this is.
    }

    @Override
    public boolean saveChunks(boolean var1, IProgressUpdate var2) {
        //seems to be a callback for when the entire world is saved
        return true;
    }
    
    @Override
    public boolean unloadQueuedChunks() {
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
    public List<BiomeGenBase.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public int getLoadedChunkCount() {
        return 0;
    }

    @Override
    public BlockPos getStrongholdGen(World world, String structureName, BlockPos position) {
        return null;
    }

    @Override
    public void recreateStructures(Chunk chunk, int x, int z) { }

    @Override
    public void saveExtraData() { }
}
