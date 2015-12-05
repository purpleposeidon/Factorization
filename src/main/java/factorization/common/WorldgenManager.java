package factorization.common;

import factorization.colossi.WorldGenColossus;
import factorization.shared.Core;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.ArrayList;
import java.util.Random;

public class WorldgenManager {
    {
        Core.loadBus(this);
        setupWorldGenerators();
    }
    
    IWorldGenerator silverGen, darkIronGen; 
    
    void setupWorldGenerators() {
        if (FzConfig.gen_silver_ore) {
            silverGen = new IWorldGenerator() {
                WorldGenMinable gen = new WorldGenMinable(Core.registry.resource_block, FzConfig.silver_ore_node_new_size);
                @Override
                public void generate(Random rand, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
                    if (!FzConfig.gen_silver_ore) {
                        return;
                    }
                    if (!world.provider.isSurfaceWorld()) {
                        return;
                    }
                    int count = 1; // + (rand.nextBoolean() && rand.nextBoolean() && rand.nextBoolean() ? 1 : 0);
                    for (int i = 0; i < count; i++) {
                        int x = chunkX*16 + rand.nextInt(16);
                        int z = chunkZ*16 + rand.nextInt(16);
                        int y = 4 + rand.nextInt(42);
                        gen.generate(world, rand, pos);
                    }
                }
            };
            GameRegistry.registerWorldGenerator(silverGen, 0);
        }
        if (FzConfig.gen_dark_iron_ore) {
            darkIronGen = new DarkIronOreGenerator();
            GameRegistry.registerWorldGenerator(darkIronGen, 10); // Run after CoFH's flat bedrock, which has priority of 0
        }
        if (FzConfig.gen_colossi) {
            GameRegistry.registerWorldGenerator(new WorldGenColossus(), -50);
        }
    }
    
    private static ArrayList<Chunk> retrogenQueue = new ArrayList();
    
    @SubscribeEvent
    public void enqueueRetrogen(ChunkDataEvent.Load event) {
        if (!FzConfig.enable_retrogen) {
            return;
        }
        final NBTTagCompound data = event.getData();
        
        final String oldKey = data.getString("fzRetro");
        if (FzConfig.retrogen_key.equals(oldKey)) {
            return;
        }
        if (FzConfig.retrogen_silver || FzConfig.retrogen_dark_iron) {
            retrogenQueue.add(event.getChunk());
        }
    }
    
    @SubscribeEvent
    public void saveRetroKey(ChunkDataEvent.Save event) {
        final NBTTagCompound data = event.getData();
        data.setString("fzRetro", FzConfig.retrogen_key);
    }
    
    void doRetrogen(boolean test, Chunk chunk, String genType, IWorldGenerator gen) {
        if (!test) return;
        final int chunkX = chunk.xPosition, chunkZ = chunk.zPosition;
        final World world = chunk.worldObj;
        //log("Retrogenning %s in dimension %s at chunk coordinates (%s, %s)", genType, world.provider.getDimensionId(), chunkX, chunkZ);
        
        //Thanks, FML!
        long worldSeed = world.getSeed();
        Random fmlRandom = new Random(worldSeed);
        long xSeed = fmlRandom.nextLong() >> 2 + 1L;
        long zSeed = fmlRandom.nextLong() >> 2 + 1L;
        long chunkSeed = (xSeed * chunkX + zSeed * chunkZ) ^ worldSeed;
        fmlRandom.setSeed(chunkSeed);
        
        final IChunkProvider chunkProvider = world.getChunkProvider();
        gen.generate(fmlRandom, chunkX, chunkZ, world, chunkProvider, chunkProvider);
    }

    public void tickRetrogenQueue() {
        if (retrogenQueue.isEmpty()) return;
        log("Starting %s chunks", retrogenQueue.size());
        for (int i = 0; i < retrogenQueue.size(); i++) {
            Chunk chunk = retrogenQueue.get(i);
            doRetrogen(FzConfig.retrogen_silver, chunk, "Silver", silverGen);
            doRetrogen(FzConfig.retrogen_dark_iron, chunk, "Dark Iron", darkIronGen);
        }
        retrogenQueue.clear();
        log("Done");
    }
    
    public static void log(String format, Object... formatParameters) {
        Core.logWarning("Retrogen> " + format, formatParameters);
    }
}
