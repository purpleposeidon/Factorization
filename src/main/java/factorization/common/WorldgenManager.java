package factorization.common;

import factorization.colossi.WorldGenColossus;
import factorization.shared.Core;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
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
    
    IWorldGenerator copperGeyserGen, darkIronGen;
    
    void setupWorldGenerators() {
        // NORELEASE: Apparently we should only generate in particular side of chunks? Like [-8, +8] instead of [0, 16]
        if (FzConfig.gen_copper_geysers) {
            copperGeyserGen = new CopperGeyserGen();
            GameRegistry.registerWorldGenerator(copperGeyserGen, -4);
        }
        if (FzConfig.gen_dark_iron_ore) {
            darkIronGen = new DarkIronOreGenerator();
            GameRegistry.registerWorldGenerator(darkIronGen, 10); // Run after CoFH's flat bedrock, which has priority of 0
        }
        if (FzConfig.gen_colossi) {
            GameRegistry.registerWorldGenerator(new WorldGenColossus(), -50);
        }
    }
    
    private static ArrayList<Chunk> retrogenQueue = new ArrayList<Chunk>();
    
    @SubscribeEvent
    public void enqueueRetrogen(ChunkDataEvent.Load event) {
        // See also: http://minecraft.curseforge.com/projects/simpleretrogen
        if (!FzConfig.enable_retrogen) {
            return;
        }
        final NBTTagCompound data = event.getData();
        
        final String oldKey = data.getString("fzRetro");
        if (FzConfig.retrogen_key.equals(oldKey)) {
            return;
        }
        if (FzConfig.retrogen_copper_geyser || FzConfig.retrogen_dark_iron) {
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
        final World world = chunk.getWorld();
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
        int skipped = 0;
        for (int i = 0; i < retrogenQueue.size(); i++) {
            Chunk chunk = retrogenQueue.get(i);
            chunk.setModified(true);
            if (chunk.getInhabitedTime() > 3600 /* 3 minutes, the amount of time used by ocean monuments. See usages of that call. */) {
                // (It might not be unreasonable to also check the inhabited time of adjacent chunks?)
                skipped++;
                continue;
            }
            doRetrogen(FzConfig.gen_copper_geysers, chunk, "Copper/Geyser", copperGeyserGen);
            doRetrogen(FzConfig.retrogen_dark_iron, chunk, "Dark Iron", darkIronGen);
        }
        if (skipped > 0) {
            log(skipped + " chunks were skipped due to sustained player presence");
        }
        retrogenQueue.clear();
        log("Done");
    }
    
    public static void log(String format, Object... formatParameters) {
        Core.logWarning("Retrogen> " + format, formatParameters);
    }
}
