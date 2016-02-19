package factorization.common;

import factorization.colossi.WorldGenColossus;
import factorization.shared.Core;
import factorization.util.NORELEASE;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class WorldgenManager {
    {
        Core.loadBus(this);
        setupWorldGenerators();
    }
    
    IWorldGenerator copperGeyserGen, darkIronGen;
    
    void setupWorldGenerators() {
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
    
    private static final ArrayList<Chunk> retrogenQueue = new ArrayList<Chunk>();
    private static final ThreadLocal<Boolean> retrogen_active = new ThreadLocal<Boolean>();
    private static final HashSet<Chunk> recursively_loaded = new HashSet<Chunk>();

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
        if (event.getChunk().getWorld().isRemote) return;
        if (retrogen_active.get() == Boolean.TRUE) {
            recursively_loaded.add(event.getChunk());
            return;
        }
        if (FzConfig.retrogen_copper_geyser || FzConfig.retrogen_dark_iron) {
            retrogenQueue.add(event.getChunk());
        }
    }

    @SubscribeEvent
    public void saveRetroKey(ChunkDataEvent.Save event) {
        if (event.getChunk().getWorld().isRemote) return;
        if (recursively_loaded.contains(event.getChunk())) {
            // Don't remove the chunk! It may still be loaded & get saved again!
            return;
        }
        final NBTTagCompound data = event.getData();
        data.setString("fzRetro", FzConfig.retrogen_key);
    }

    @SubscribeEvent
    public void removeRecursivelyLoadedChunk(ChunkDataEvent.Unload event) {
        if (event.getChunk().getWorld().isRemote) return;
        recursively_loaded.remove(event.getChunk());
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

    boolean checkInhabitedTime = false;

    public void tickRetrogenQueue() {
        if (retrogenQueue.isEmpty()) return;
        retrogen_active.set(Boolean.TRUE);
        log("Starting %s chunks", retrogenQueue.size());
        int skipped = 0;
        final int max_inhabited_time = 20 * 60 * 6;
        // Vanilla's ocean monument retrogen uses 3 minutes.
        // I'll go with 6 minutes. My world has a wide range of times,
        // However *tons* of chunks have 0 time.
        // 3 minutes seems *way* too short; pretty sure getInhabitedTime is not how long a player entity has been in,
        // but rather how long they've been receiving packet updates...
        // I suspect very few chunks have an occupancy time of less than 3 minutes.
        // See usages of chunk.getInhabitedTime().
        int recursive_chunks_at_start = recursively_loaded.size();
        for (int i = 0; i < retrogenQueue.size(); i++) {
            Chunk chunk = retrogenQueue.get(i);
            chunk.setModified(true);
            if (checkInhabitedTime && chunk.getInhabitedTime() > max_inhabited_time) {
                skipped++;
                continue;
            }
            doRetrogen(FzConfig.gen_copper_geysers, chunk, "Copper/Geyser", copperGeyserGen);
            doRetrogen(FzConfig.retrogen_dark_iron, chunk, "Dark Iron", darkIronGen);
        }
        recursive_chunks_at_start -= recursively_loaded.size();
        if (skipped > 0) {
            log(skipped + " chunks were skipped due to sustained player presence");
        }
        if (recursive_chunks_at_start > 0) {
            log("WARNING: " + recursive_chunks_at_start + " chunks needing retrogen applied were loaded (likely by other mods) while FZ's retrogen was occuring");
            log("         They will not be retrogened. Retrogen will be applied to them the next time they load.");
        }
        retrogenQueue.clear();
        log("Done");
        retrogen_active.remove();
    }
    
    public static void log(String format, Object... formatParameters) {
        Core.logWarning("Retrogen> " + format, formatParameters);
    }
}
