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
    
    Type[] forbiddenBiomeTypes = new Type[] {
            SPOOKY,
            NETHER,
            END,
            MAGICAL,
            WATER,
            RIVER,
            OCEAN,
            BEACH,
            DENSE,
            FOREST
    };
    
    {
        if (FzConfig.gen_colossi) {
            Core.loadBus(this);
            MinecraftForge.TERRAIN_GEN_BUS.register(this);
        }
    }
    
    static int GENERATION_SPACING = 32; // TODO NORELEASE: Config option. Defaultt o 50?
    static int GENERATION_START_X = 9, GENERATION_START_Z = 9;
    static final double SMOOTH_END = 8*3, SMOOTH_START = 8*5;
    static {
        if (GENERATION_START_X > GENERATION_SPACING || GENERATION_START_Z > GENERATION_SPACING) {
            throw new IllegalArgumentException();
        }
    }
    
    static double position(int generation_spacing, int pos_start, double pos) {
     // chunkX % dist = x_start
        // target_x = ((dist * n) + x_start)*16 + 8
        int radius = GENERATION_SPACING / 2;
        int mul = (int) (((pos + 8)/16 - pos_start + radius)/generation_spacing); // the 'n' of the nearest location
        double target_pos1 = ((generation_spacing * mul) + pos_start) * 16 + 8;
        double target_pos2 = ((generation_spacing * (mul + 1)) + pos_start) * 16 + 8;
        double dp1 = target_pos1 - pos;
        double dp2 = target_pos2 - pos;
        if (Math.abs(dp1) < Math.abs(dp2)) {
            return target_pos1;
        }
        return target_pos2;
    }
    
    static double dist(int generation_spacing, int pos_start, double pos) {
        // chunkX % dist = x_start
        // target_x = ((dist * n) + x_start)*16 + 8
        int radius = GENERATION_SPACING / 2;
        int mul = (int) (((pos + 8)/16 - pos_start + radius)/generation_spacing); // the 'n' of the nearest location
        double target_pos1 = ((generation_spacing * mul) + pos_start) * 16 + 8;
        double target_pos2 = ((generation_spacing * (mul + 1)) + pos_start) * 16 + 8;
        double dist1 = Math.abs(pos - target_pos1);
        double dist2 = Math.abs(pos - target_pos2);
        return Math.min(dist1, dist2);
    }
    
    static double distance(double blockX, double blockZ) {
        double distX = dist(GENERATION_SPACING, GENERATION_START_X, blockX);
        double distZ = dist(GENERATION_SPACING, GENERATION_START_Z, blockZ);
        
        //double distSq = distX * distX + distZ * distZ;
        //return Math.sqrt(distSq);
        return Math.abs(distX + distZ)/2;
    }
    
    static boolean isGenChunk(int chunkX, int chunkZ) {
        boolean x = (((chunkX % GENERATION_SPACING) + GENERATION_SPACING) % GENERATION_SPACING) == GENERATION_START_X;
        boolean z = (((chunkZ % GENERATION_SPACING) + GENERATION_SPACING) % GENERATION_SPACING) == GENERATION_START_Z;
        return x && z;
    }
    
    static Coord getNearest(Coord player) {
        double cx = position(GENERATION_SPACING, GENERATION_START_X, player.x);
        double cz = position(GENERATION_SPACING, GENERATION_START_Z, player.z);
        Coord ret = player.copy();
        ret.x = (int) cx;
        ret.z = (int) cz;
        return ret;
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
            double[] ret = parent.generateNoiseOctaves(noiseOut, chunkXTimes4, noiseStartY, chunkZTimes4, noiseSizeX, noiseSizeY, noiseSizeZ,
                    scaleX, scaleY, scaleZ);
            // int blockIndex = x * blockDataHeight * 16 | z * blockDataHeight | y;
            // The noise is 5x33x5
            int noiseSrcX = chunkXTimes4*16/4; // chunkXTimes4 is actually 'noiseStartX'
            int noiseSrcZ = chunkZTimes4*16/4; // chunkZTimes4 is actually 'noiseStartZ'
            
            double noise_src_dist = distance(noiseSrcX + 2 * 4, noiseSrcZ + 2 * 4);
            if (noise_src_dist > SMOOTH_START * 2) return ret;
            
            int y_count = parentIndex == 5 ? 1 : 33;
            
            for (int noiseX = 0; noiseX < 5; noiseX++) {
                for (int noiseZ = 0; noiseZ < 5; noiseZ++) {
                    int world_x = noiseSrcX + noiseX * 4;
                    int world_z = noiseSrcZ + noiseZ * 4;
                    int column = ((noiseX * 5) + noiseZ) * y_count;
                    double d = distance(world_x, world_z);
                    if (y_count > 1) {
                        d -= Math.abs(ret[column] - ret[column + 1]) / 1000;
                    } else {
                        d -= Math.abs(ret[column] / 1000);
                    }
                    for (int noiseY = 0; noiseY < y_count; noiseY++) {
                        ret[column + noiseY] = smooth(d, ret[column + noiseY], 500);
                    }
                }
            }
            
            return ret;
        }
        
        double smooth(double dist, double max, double min) {
            if (dist > SMOOTH_START) return max;
            if (dist < SMOOTH_END) return min;
            double val = FzUtil.uninterp(SMOOTH_END, SMOOTH_START, dist);
            val = Math.pow(2, val * val * val) - 1;
            return FzUtil.interp(min, max, val);
        }
        
    }
    
    @SubscribeEvent
    public void injectNoiseSmoothers(InitNoiseGensEvent event) {
        if (!genOnWorld(event.world)) return;
        int[] target_noises = new int[] { 0, 1, 2, 5 };
        for (int noise_index : target_noises) {
            NoiseGenerator previousGenerator = event.newNoiseGens[noise_index];
            event.newNoiseGens[noise_index] = new SmoothNoiseNearColossi(noise_index, (NoiseGeneratorOctaves) previousGenerator);
        }
    }
    
    boolean genOnWorld(World world) {
        return world.getWorldInfo().isMapFeaturesEnabled() && world.provider.isSurfaceWorld() && FzConfig.gen_colossi;
    }
    
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
        System.out.println("Colossus position candidate at " + blockX + ", " + blockZ + ":  " + bad_biome);
        start.moveToTopBlock();
        while (!start.isSolid()) {
            if (start.y <= 0) return;
            start.y--;
        }
        Block dirt = start.getBlock();
        int dirt_md = start.getMd();
        start.y++;
        if (bad_biome) {
            start.setId(Blocks.standing_sign); // NORELEASE
            TileEntitySign sign = start.getTE(TileEntitySign.class);
            sign.signText[0] = "Bad biome!"; 
            return;
        }
        
        ColossalBuilder builder = new ColossalBuilder(Math.abs(worldRandom.nextInt()), start);
        int width = builder.get_width();
        int depth = builder.get_depth();
        int height = builder.get_height();
        Coord at = start.copy();
        for (int dw = -width; dw <= width; dw++) {
            for (int dd = -depth; dd <= depth; dd++) {
                at.x = start.x + dw;
                at.z = start.z + dd;
                at.moveToTopBlock();
                while (at.y < start.y) {
                    at.setIdMd(dirt, dirt_md, false);
                    at.y++;
                }
                int end = at.y;
                at.y = start.y + height;
                while (at.y >= end) {
                    at.setAir();
                    at.y--;
                }
            }
        }
        
        builder.construct();
    }
    
    private boolean cancel(World w, int cx, int cz) {
        int d = 1;
        for (int dx = -d; dx <= d; dx++) {
            for (int dz = -d; dz <= d; dz++) {
                if (isGenChunk(cx + dx, cz + dz) && !isBadBiome(w, cx + dx, cz + dz)) {
                    return true;
                }
            }
        }
        return false;
    }

    // There's a GrowTreeEvent, but I think it's actually for normal in-game sapling growth?
    
    @SubscribeEvent
    public void cancelDecorations(Decorate event) {
        if (!genOnWorld(event.world)) return;
        switch (event.type) {
        case BIG_SHROOM:
        case LAKE:
        case LILYPAD:
        case PUMPKIN:
        case TREE:
            if (cancel(event.world, event.chunkX / 16, event.chunkZ / 16)) {
                event.setResult(Result.DENY);
            }
            return;
        default: break;
        }
    }
}
