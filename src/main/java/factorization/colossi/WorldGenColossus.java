package factorization.colossi;

import static net.minecraftforge.common.BiomeDictionary.Type.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.NoiseGenerator;
import net.minecraft.world.gen.NoiseGeneratorOctaves;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.InitNoiseGensEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.terraingen.SaplingGrowTreeEvent;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent.Decorate;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import factorization.api.Coord;
import factorization.common.FzConfig;
import factorization.shared.Core;
import factorization.shared.FzUtil;

public class WorldGenColossus implements IWorldGenerator {
    {
        if (FzConfig.gen_colossi) {
            Core.loadBus(this);
            MinecraftForge.TERRAIN_GEN_BUS.register(this);
        }
    }
    
    static int GENERATION_SPACING = 32; // TODO NORELEASE: Config option. Default to 50?
    static int GENERATION_START_X = 9, GENERATION_START_Z = 9;
    static final double SMOOTH_START = 16 * 5 / 2;
    static final double SMOOTH_END = 16 * 3 / 2;
    static final double SMOOTH_GEN_BORDER = SMOOTH_END + 24;
    static final double SMOOTH_END_FUZZ = SMOOTH_END + 32;
    static {
        if (GENERATION_START_X > GENERATION_SPACING || GENERATION_START_Z > GENERATION_SPACING) {
            throw new IllegalArgumentException();
        }
    }
    
    private static double dist(int generation_spacing, int pos) {
        pos = Math.abs(pos);
        int n = pos / generation_spacing;
        int dist = pos - n * generation_spacing;
        if (dist > generation_spacing / 2) {
            dist = generation_spacing - dist;
        }
        return dist;
    }
    
    public static double distance(int blockX, int blockZ) {
        double distX = dist(GENERATION_SPACING * 16, blockX - GENERATION_START_X * 16);
        double distZ = dist(GENERATION_SPACING * 16, blockZ - GENERATION_START_Z * 16);
        
        double distSq = distX * distX + distZ * distZ;
        return Math.sqrt(distSq);
    }
    
    public static boolean isGenChunk(int chunkX, int chunkZ) {
        boolean x = (((chunkX % GENERATION_SPACING) + GENERATION_SPACING) % GENERATION_SPACING) == GENERATION_START_X;
        boolean z = (((chunkZ % GENERATION_SPACING) + GENERATION_SPACING) % GENERATION_SPACING) == GENERATION_START_Z;
        return x && z;
    }
    
    public static ArrayList<Coord> getCandidatesNear(final Coord player, int chunkSearchDistance, boolean forceLoad) {
        ArrayList<Coord> ret = new ArrayList<Coord>();
        ChunkCoordIntPair chunkAt = player.getChunk().getChunkCoordIntPair();
        for (int dx = -chunkSearchDistance; dx <= chunkSearchDistance; dx++) {
            for (int dz = -chunkSearchDistance; dz <= chunkSearchDistance; dz++) {
                int cx = chunkAt.chunkXPos + dx;
                int cz = chunkAt.chunkZPos + dz;
                if (isGenChunk(cx, cz)) {
                    Chunk chunk = player.w.getChunkFromChunkCoords(cx, cz);
                    boolean unload = false;
                    if (forceLoad && !chunk.isTerrainPopulated) {
                        forceLoadChunk(player.w, cx, cz);
                        chunk = player.w.getChunkFromChunkCoords(cx, cz);
                        unload = true;
                    }
                    Coord at = new Coord(chunk);
                    at.getBlock();
                    for (TileEntity te : (Iterable<TileEntity>)chunk.chunkTileEntityMap.values()) {
                        if (te instanceof TileEntityColossalHeart) {
                            ret.add(new Coord(te));
                            break;
                        }
                    }
                    if (unload) {
                        releaseChunk(chunk);
                    }
                }
            }
        }
        ret.sort(new Comparator<Coord>() {
            @Override
            public int compare(Coord a, Coord b) {
                return player.distanceSq(a) - player.distanceSq(b);
            }
        });
        return ret;
    }
    
    private static void forceLoadChunk(World world, int cx, int cz) {
        ChunkProviderServer cps = (ChunkProviderServer) world.getChunkProvider();
        cps.populate(cps, cx, cz);
    }
    
    private static void releaseChunk(Chunk chunk) {
        // Not necessary I hope?
    }
    
    @SubscribeEvent
    public void injectNoiseSmoothers(InitNoiseGensEvent event) {
        // Create a flat arena around colossi.
        if (!genOnWorld(event.world)) return;
        int[] target_noises = new int[] { 0, 1, 2, 5 };
        for (int noise_index : target_noises) {
            NoiseGenerator parentGenerator = event.newNoiseGens[noise_index];
            event.newNoiseGens[noise_index] = new SmoothNoiseNearColossi(noise_index, (NoiseGeneratorOctaves) parentGenerator);
        }
    }
    
    static class SmoothNoiseNearColossi extends NoiseGeneratorOctaves {
        final NoiseGeneratorOctaves parent;
        final int parentIndex;

        public SmoothNoiseNearColossi(int parentIndex, NoiseGeneratorOctaves parent) {
            super(null, 0);
            this.parent = parent;
            this.parentIndex = parentIndex;
        }
        
        @Override
        public double[] generateNoiseOctaves(double[] noiseOut, int chunkXTimes4, int noiseStartY, int chunkZTimes4, int noiseSizeX, int noiseSizeY, int noiseSizeZ,
                double scaleX, double scaleY, double scaleZ) {
            // The terrain generator uses 4 noise sources, and does a bunch of crazy math on them to do world gen.
            // The noise arrays are *not* the same size as a chunk, and one of the noises is only 1 high.
            double[] ret = parent.generateNoiseOctaves(noiseOut, chunkXTimes4, noiseStartY, chunkZTimes4, noiseSizeX, noiseSizeY, noiseSizeZ,
                    scaleX, scaleY, scaleZ);
            // int blockIndex = x * blockDataHeight * 16 | z * blockDataHeight | y;
            // The noise is 5x33x5
            int noiseSrcX = chunkXTimes4 * 16 / 4; // chunkXTimes4 is actually 'noiseStartX'
            int noiseSrcZ = chunkZTimes4 * 16 / 4; // chunkZTimes4 is actually 'noiseStartZ'
            
            double noise_src_dist = distance(noiseSrcX + 8, noiseSrcZ + 8);
            if (noise_src_dist > SMOOTH_END_FUZZ) return ret;
            
            final int y_count = parentIndex == 5 ? 1 : 33;
            int distance_fuzz = 500; // Used to be 1k. Messes with the distance to make the border look natural.
            
            // Ia! Ia! Cachuchu flaht'n!
            for (int noiseX = 0; noiseX < 5; noiseX++) {
                for (int noiseZ = 0; noiseZ < 5; noiseZ++) {
                    int world_x = noiseSrcX + noiseX * 4;
                    int world_z = noiseSrcZ + noiseZ * 4;
                    int column = ((noiseX * 5) + noiseZ) * y_count;
                    double d = distance(world_x, world_z);
                    double dFuzz = 0;
                    if (y_count > 1) {
                        dFuzz = (ret[column] - ret[column + 1]) / distance_fuzz;
                    } else {
                        dFuzz = ret[column] / distance_fuzz;
                    }
                    d -= Math.cos(dFuzz) * 3;
                    for (int noiseY = 0; noiseY < y_count; noiseY++) {
                        ret[column + noiseY] = smooth(d, ret[column + noiseY], 500);
                    }
                }
            }
            return ret;
        }
        
        double smooth(double dist, double max, double min) {
            // "They're smooth posers" -- LSP
            if (dist > SMOOTH_START) return max;
            if (dist < SMOOTH_END) return min;
            double val = FzUtil.uninterp(SMOOTH_END, SMOOTH_START, dist); // val between 0, 1
            //val += 0.1;
            //val = Math.pow(2, val * val * val) - 1; // Gives a smooth _|-shaped curve.
            return FzUtil.interp(min, max, val);
        }
        
    }
    
    boolean genOnWorld(World world) {
        return world.getWorldInfo().isMapFeaturesEnabled() && world.provider.isSurfaceWorld() && FzConfig.gen_colossi;
    }
    
    static Type[] forbiddenBiomeTypes = new Type[] {
        SPOOKY,
        NETHER,
        END,
        MAGICAL,
        WATER,
        RIVER,
        OCEAN,
        BEACH,
        DENSE
    };
    
    boolean isBadBiome(World w, int chunkX, int chunkZ) {
        BiomeGenBase biome = w.getBiomeGenForCoords(8 + chunkX * 16, 8 + chunkZ * 16);
        for (Type bad : forbiddenBiomeTypes) {
            if (BiomeDictionary.isBiomeOfType(biome, bad)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void generate(Random worldRandom, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
        if (!genOnWorld(world)) return;
        if (!isGenChunk(chunkX, chunkZ)) return;
        int blockX = 8 + (chunkX * 16);
        int blockZ = 8 + (chunkZ * 16);
        
        Coord start = new Coord(world, blockX, 0xFF /* NORELEASE */, blockZ);
        boolean bad_biome = isBadBiome(world, chunkX, chunkZ);
        if (bad_biome) {
            return;
        }
        start.moveToTopBlock();
        while (!start.isSolid()) {
            if (start.y <= 0) return;
            start.y--;
        }
        start.y++;
        ColossalBuilder builder = new ColossalBuilder(Math.abs(worldRandom.nextInt()), start);
        builder.construct();
    }
    
    // Forbid decoration/population to make life easy for the colossi
    
    private boolean cancel(World w, int cx, int cz) {
        int blockX = cx * 16 + 8;
        int blockZ = cz * 16 + 8;
        return distance(blockX, blockZ) < SMOOTH_GEN_BORDER;
    }

    // GrowTreeEvent is for normal in-game sapling growth, so it's not included here.
    
    @SubscribeEvent
    public void cancelDecorations(Decorate event) {
        if (!genOnWorld(event.world)) return;
        switch (event.type) {
        case BIG_SHROOM:
        case LAKE:
        case PUMPKIN:
        case TREE:
            if (cancel(event.world, event.chunkX / 16, event.chunkZ / 16)) {
                event.setResult(Result.DENY);
            }
            return;
        default: break;
        }
    }
    
    @SubscribeEvent
    public void cancelPopulations(PopulateChunkEvent.Populate event) {
        if (!genOnWorld(event.world)) return;
        switch (event.type) {
        case LAKE:
        case LAVA:
        case ANIMALS:
        case CUSTOM:
            if (cancel(event.world, event.chunkX, event.chunkZ)) {
                event.setResult(Result.DENY);
            }
            return;
        default: break;
        }
    }
}
