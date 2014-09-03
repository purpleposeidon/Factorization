package factorization.common;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ReportedException;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.event.world.ChunkDataEvent;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import factorization.shared.Core;

public class WorldgenManager {
    {
        Core.loadBus(this);
        setupWorldGenerators();
    }
    
    IWorldGenerator silverGen, darkIronGen; 
    
    static void disclaimErrors(ReportedException e) {
        CrashReport crash = e.getCrashReport();
        CrashReportCategory disclaimer = crash.makeCategory("disclaimer");
        disclaimer.addCrashSection("HEY",
                "It's probably not Factorization's fault! Okay? Okay.\n"
                + "Just because I show up here doesn't mean I'm the cause. In any case there's not a thing I could possibly do differently.\n"
                + "Maybe you've got corrupted worldgen structures;\n"
                + "If this is the case, removing files like 'Mineshaft.dat', 'Village.dat', (but NOT things like 'map_0.dat')\n"
                + "from the data/ folder in the save may help.\n"
                + "Be sure to make a backup first tho."
                );
        throw e;
    }
    
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
                    try {
                        for (int i = 0; i < count; i++) {
                            int x = chunkX*16 + rand.nextInt(16);
                            int z = chunkZ*16 + rand.nextInt(16);
                            int y = 4 + rand.nextInt(42);
                            gen.generate(world, rand, x, y, z);
                        }
                    } catch (ReportedException e) {
                        disclaimErrors(e);
                    }
                }
            };
            GameRegistry.registerWorldGenerator(silverGen, 0);
        }
        if (FzConfig.gen_dark_iron_ore) {
            darkIronGen = new IWorldGenerator() {
                int x, z;
                Block stoneId;
                int stoneMd;
                int setBlockFlags = 0;
                
                void set(World world, int dx, int y, int dz) {
                    world.setBlock(x + dx, y, z + dz, Core.registry.dark_iron_ore, 0, setBlockFlags);
                }
                
                void clear(World world, int dx, int y, int dz) {
                    if (world.getBlock(x + dx, y, z + dz) == Blocks.bedrock) {
                        world.setBlock(x + dx, y, z + dz, stoneId, stoneMd, setBlockFlags);
                    }
                }
                
                void fracture(World world, int dx, int y, int dz) {
                    if (world.getBlock(x + dx, y, z + dz) == Blocks.bedrock) {
                        world.setBlock(x + dx, y, z + dz, Core.registry.fractured_bedrock_block, 0, setBlockFlags);
                    }
                }
                
                @Override
                public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
                    doGeneration(random, chunkX, chunkZ, world, chunkGenerator, chunkProvider);
                    world = null;
                }
                
                private void dob(Random random, World world, int dx, int y, int dz) {
                    set(world, dx, y, dz);
                    for (int i = 0; i < 4; i++) {
                        switch (random.nextInt(3)) {
                        case 0:
                            set(world, dx + 1, y, dz);
                            break;
                        case 1:
                            set(world, dx - 1, y, dz);
                            break;
                        case 3:
                            set(world, dx, y, dz + 1);
                            break;
                        case 4:
                            set(world, dx, y, dz - 1);
                            break;
                        default: break;
                        }
                    }
                    if (random.nextBoolean()) {
                        set(world, dx, y + 1, dz);
                    }
                }
                
                private void doGeneration(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
                    //Simple pre-reqs
                    if (!FzConfig.gen_dark_iron_ore) {
                        return;
                    }
                    if (!world.provider.isSurfaceWorld()) {
                        return;
                    }
                    try {
                        generation_implementation(random, chunkX, chunkZ, world, chunkGenerator, chunkProvider);
                    } catch (ReportedException e) {
                        disclaimErrors(e);
                    }
                }
                
                private void generation_implementation(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
                    if (chunkX == 0 && chunkZ == 0) {
                        x = z = 0;
                    } else {
                        if (random.nextInt(10) < 2) {
                            return;
                        }
                        
                        //Find & test the location
                        x = chunkX*16 + random.nextInt(16);
                        z = chunkZ*16 + random.nextInt(16);
                    }
                    if (world.getBlock(x, 0, z) != Blocks.bedrock) {
                        return;
                    }
                    Block stoneBlock = world.getBlock(x, 1, z);
                    if (stoneBlock == null) {
                        return;
                    }
                    if (!stoneBlock.isReplaceableOreGen(world, x, 1, z, Blocks.stone)) {
                        return;
                    }
                    stoneId = stoneBlock;
                    stoneMd = world.getBlockMetadata(x, 1, z);
                    
                    //The spike
                    int height = 4 + random.nextInt(3);
                    for (int y = 1; y < height + 1; y++) {
                        set(world, 0, y, 0);
                    }
                    int trailHeight = height + 2 + random.nextInt(3);
                    for (int y = height + 1; y < trailHeight; y++) {
                        if (random.nextBoolean()) {
                            set(world, 0, y, 0);
                        }
                    }
                    
                    //The little frill around the base of the spike
                    int roundCount = 3 + (random.nextBoolean() ? 1 : 0);
                    for (int i = 0; i < roundCount; i++) {
                        switch (random.nextInt(4)) {
                        case 0:
                            dob(random, world, -1, 1, 0);
                            break;
                        case 1:
                            dob(random, world, +1, 1, 0);
                            break;
                        case 2:
                            dob(random, world, 0, 1, -1);
                            break;
                        case 3:
                            dob(random, world, 0, 1, +1);
                            break;
                        default: break;
                        }
                    }
                    
                    //The blast radius
                    int radius = 3 + random.nextInt(3);
                    for (int y = 0; y < 7; y++) {
                        int rSq = radius*radius;
                        for (int dx = -radius; dx <= radius; dx++) {
                            for (int dz = -radius; dz <= radius; dz++) {
                                int rdist = rSq - (dx*dx + dz*dz);
                                if (rdist < 0) {
                                    continue;
                                }
                                if (rdist < 2 && random.nextInt(3) != 0) {
                                    continue;
                                }
                                if (y == 0) {
                                    fracture(world, dx, y, dz);
                                } else {
                                    clear(world, dx, y, dz);
                                }
                            }
                        }
                        if (y == 0) {
                            continue;
                        }
                        radius += 1;
                        if (y > 3) {
                            radius += random.nextInt(2);
                        }
                    }
                }
            };
            GameRegistry.registerWorldGenerator(darkIronGen, 0);
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
        //log("Retrogenning %s in dimension %s at chunk coordinates (%s, %s)", genType, world.provider.dimensionId, chunkX, chunkZ);
        
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
