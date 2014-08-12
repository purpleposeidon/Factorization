package factorization.colossi;

import static net.minecraftforge.common.BiomeDictionary.Type.*;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.NoiseGenerator;
import net.minecraft.world.gen.NoiseGeneratorOctaves;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.InitNoiseGensEvent;
import cpw.mods.fml.common.IWorldGenerator;
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
            DENSE
    };
    
    {
        if (FzConfig.gen_colossi) {
            Core.loadBus(this);;
            MinecraftForge.TERRAIN_GEN_BUS.register(this);
        }
    }
    
    static int GENERATION_SPACING = 80;
    static int GENERATION_START_X = 40, GENERATION_START_Z = 40;
    static final double SMOOTH_END = 8*3, SMOOTH_START = 8*5;
    
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
        //return 4487 - pos;
    }
    
    static double distance(double blockX, double blockZ) {
        double distX = dist(GENERATION_SPACING, GENERATION_START_X, blockX);
        double distZ = dist(GENERATION_SPACING, GENERATION_START_Z, blockZ);
        
        //double distSq = distX * distX + distZ * distZ;
        //return Math.sqrt(distSq);
        return Math.abs(distX + distZ)/2;
    }
    
    boolean isGenChunk(int chunkX, int chunkZ) {
        return (chunkX % GENERATION_SPACING) == GENERATION_START_X && (chunkZ % GENERATION_SPACING) == GENERATION_START_Z;
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

    @Override
    public void generate(Random worldRandom, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
        if (!genOnWorld(world)) return;
        if (!isGenChunk(chunkX, chunkZ)) return;
        int blockX = 8 + (chunkX * 16);
        int blockZ = 8 + (chunkZ * 16);
        
        BiomeGenBase biome = world.getBiomeGenForCoords(blockX, blockZ);
        for (Type bad : forbiddenBiomeTypes) {
            if (BiomeDictionary.isBiomeOfType(biome, bad)) {
                return;
            }
        }
        
        Coord start = new Coord(world, blockX, 0xFF, blockZ);
        start.moveToTopBlock();
        while (!start.isSolid()) {
            if (start.y <= 0) return;
            start.y--;
        }
        start.y++;
        Block dirt = start.getBlock();
        int dirt_md = start.getMd();
        
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
                while (at.y < start.y + height) {
                    at.setAir();
                    at.y++;
                }
            }
        }
        
        builder.construct();
    }
    
    
}
