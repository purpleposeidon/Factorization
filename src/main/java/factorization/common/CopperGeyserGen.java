package factorization.common;

import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.ICoordFunction;
import factorization.shared.Core;
import factorization.util.NORELEASE;
import factorization.util.NumUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.NoiseGeneratorImproved;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.ArrayList;
import java.util.Random;

public class CopperGeyserGen implements IWorldGenerator {
    IBlockState geyser, tube, lava, extruder, air;

    {
        air = Blocks.air.getDefaultState();
        tube = Blocks.hardened_clay.getDefaultState();
        lava = Blocks.lava.getDefaultState();
        geyser = Core.registry.geyser.getDefaultState();
        extruder = Core.registry.extruder.getDefaultState();
        NORELEASE.fixme("Config options");
    }

    private Random volcanoRng = new Random(0);
    private boolean sampleVolcanism(int x, int z, int depth, long seed) {
        x >>= depth;
        z >>= depth;
        volcanoRng.setSeed((depth << 16L) + z + seed);
        return volcanoRng.nextFloat() > volcanismScale;
    }

    public float getVolcanism(int chunkX, int chunkZ, long seed) {
        chunkX /= minSpacing;
        chunkZ /= minSpacing;
        for (int depth = 0; depth < volcanismNoiseDepth; depth++) {
            if (sampleVolcanism(chunkX, chunkZ, depth, seed)) return (float) chancePerVolcanism[depth];
        }
        return (float) chancePerVolcanism[chancePerVolcanism.length - 1];
    }

    /**
     * This is a list of probabilities between 0 and 1. Areas with low volcanism choose from the begining of the
     * list, and areas with high volcanism choose from the end.
     * The length of this list defines volcanismDepth. The chance of getting the highest volcanism is
     * volcanismScale**volcanismDepth.
     */
    double chancePerVolcanism[] = new double[] {
            0.00,
            0.00,
            0.00,
            0.03,
            0.20,
            0.90,
    };
    float volcanismScale = 0.5F;
    /**
     * This defines the minimum distance between geysers in chunks. This is the size of the grid that geysers can occur
     * on. Reasonable values are in 1 to 4. Due to the grid-nature of this, large values make it possible for a player
     * to walk in a straight line forever without finding a geyser.
     */
    int minSpacing = 3;
    final int volcanismNoiseDepth = chancePerVolcanism.length;
    private static final boolean notify = NORELEASE.just(true);
    int extrudersPerChamber = 6;

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
        if (chunkX % minSpacing != 0 || chunkZ % minSpacing != 0) {
            return;
        }
        float volcanism = getVolcanism(chunkX, chunkZ, world.getSeed());
        NORELEASE.println("Volcanism: " + volcanism + "\t at " + chunkX + ", " + chunkZ);
        if (random.nextDouble() > volcanism) return;
        int x = 8 + chunkX * 16;
        int z = 8 + chunkZ * 16;
        Coord start = new Coord(world, x, 0, z);
        boolean validBiome = validBiome(start.getBiome());
        NORELEASE.println("Might generate copper geyser " + validBiome + " at " + start);
        if (!validBiome) return;
        generateAt(random, start);
    }

    boolean validBiome(BiomeGenBase biome) {
        for (BiomeDictionary.Type type : BiomeDictionary.getTypesForBiome(biome)) {
            if (type == BiomeDictionary.Type.OCEAN
                    || type == BiomeDictionary.Type.NETHER
                    || type == BiomeDictionary.Type.END
                    || type == BiomeDictionary.Type.RIVER) {
                return false;
            }
        }
        return true;
    }

    public void generateAt(Random random, Coord at) {
        at.moveToTopBlock();
        while (at.y > 8) {
            at.y--;
            if (at.isSolid()) {
                Material mat = at.getBlock().getMaterial();
                if (mat == Material.wood || mat == Material.plants) continue;
                break;
            }
        }
        int minChamberSize = 5, maxChamberSize = 8;
        int chamberSize = minChamberSize + random.nextInt(maxChamberSize - minChamberSize);
        int avail_space = at.y - chamberSize;
        if (avail_space < 6) return;
        avail_space /= 3;

        final int height = avail_space + random.nextInt(avail_space);
        int minR = 4, maxR = 7;
        int varR = maxR;
        NoiseSampler pipeNoise = new NoiseSampler(at, maxR, height, random, 8);
        boolean first = true;
        Coord pos = at.copy();
        int depth = height;
        int fuzz = 3;
        boolean top_is_water = random.nextInt(3) == 0;
        if (top_is_water) {
            // see World.canBlockFreezeBody
            float temp = at.getBiome().getFloatTemperature(at.toBlockPos());
            if (temp <= 0.15) {
                top_is_water = false;
            }
        }
        IBlockState top_layer = (top_is_water ? Blocks.water : Blocks.air).getDefaultState();
        Coord groundClean = at.copy();
        Coord geyser_spot = null;
        while (depth-- > 0) {
            final double maxRSq = varR * varR;
            final double minRSq = (varR - fuzz) * (varR - fuzz);
            for (int dx = -varR; dx <= +varR; dx++) {
                for (int dz = -varR; dz <= +varR; dz++) {
                    pos.setOff(at, dx, 0, dz);
                    double dist = dx * dx + dz * dz;
                    double interp = NumUtil.uninterp(minRSq, maxRSq, dist);
                    if (interp > 1) continue;
                    if (interp > 0) {
                        double sample = Math.abs(pipeNoise.sample(pos));
                        if (sample - interp < 0) continue;
                    }
                    if (first) {
                        pos.set(top_layer, notify);
                        int clearUp = pos.getColumnHeight() - pos.y;
                        if (clearUp < 0) clearUp = 0;
                        clearUp += 4;
                        for (int i = 0; i < clearUp; i++) {
                            groundClean.setOff(pos, 0, i, 0);
                            if (at.isSolid()) {
                                Material mat = at.getBlock().getMaterial();
                                if (mat != Material.plants && mat != Material.wood) {
                                    at.set(air, notify);
                                }
                            } else if (at.isAir()) {
                                break;
                            }
                        }
                    } else {
                        pos.set(tube, notify);
                    }
                }
            }
            if (first) {
                geyser_spot = at.add(0, -1, 0);
                first = false;
            } else if (varR > minR && random.nextDouble() > depth / (double) height) {
                varR--;
            }
            at.y--;
        }
        if (geyser_spot != null) {
            geyser_spot.set(geyser, notify);
        }

        int R = chamberSize;
        at.y += R;
        final NoiseSampler chamberNoise = new NoiseSampler(at.add(-R, -R, -R), at.add(+R, +R, +R), random, 1.0 / 256);
        int chamberSure = chamberSize - 1;
        final double yesSq = chamberSure * chamberSure;
        final double noSq = chamberSize * chamberSize;
        ChamberBuilder builder = new ChamberBuilder(at, noSq, yesSq, chamberNoise);
        Coord.iterateCube(chamberNoise.min, chamberNoise.max, builder);
        placeExtruders(random, builder.topLavas);
    }

    private void placeExtruders(Random random, ArrayList<Coord> topLavas) {
        int available = extrudersPerChamber;
        while (!topLavas.isEmpty() && available > 0) {
            int i = random.nextInt(topLavas.size());
            Coord lavaSpot = topLavas.get(i);
            boolean good = true;
            for (EnumFacing dir : EnumFacing.VALUES) {
                lavaSpot.adjust(dir);
                if (lavaSpot.getMaterial() == Material.lava || lavaSpot.isSolid()) {
                    lavaSpot.adjust(dir.getOpposite());
                    continue;
                }
                good = false;
                break;
            }
            if (good) {
                lavaSpot.set(extruder, notify);
                available--;
            }
            topLavas.remove(i);
        }
    }

    class NoiseSampler {
        Coord min, max;
        DeltaCoord size;
        int maxIdx;
        double noise[];

        double sample(Coord at) {
            if (!at.inside(min, max)) {
                return 0;
            }
            DeltaCoord dc = at.difference(min);
            int idx = dc.x * (size.y * size.z) + dc.y * size.z + dc.z;
            if (idx < 0 || idx >= noise.length) {
                return 0;
            }
            return noise[idx];
        }

        NoiseSampler(Coord center, int r, int depth, Random rng, double scale) {
            this(center.add(-r, -depth, -r), center.add(+r, 1, +r), rng, scale);
        }

        NoiseSampler(Coord min, Coord max, Random rng, double s) {
            this.min = min;
            this.max = max;
            size = max.difference(min);
            maxIdx = size.x * size.y * size.z;
            noise = new double[maxIdx];
            new NoiseGeneratorImproved(rng).populateNoiseArray(noise, min.x, min.y, min.z, size.x, size.y, size.z, s, s, s, 1);
        }

    }

    private class ChamberBuilder implements ICoordFunction {
        private final Coord middle;
        private final double noSq;
        private final double yesSq;
        private final NoiseSampler chamberNoise;
        ArrayList<Coord> topLavas = new ArrayList<Coord>();

        public ChamberBuilder(Coord middle, double noSq, double yesSq, NoiseSampler chamberNoise) {
            this.middle = middle;
            this.noSq = noSq;
            this.yesSq = yesSq;
            this.chamberNoise = chamberNoise;
        }

        @Override
        public void handle(Coord here) {
            double distSq = middle.distanceSq(here);
            if (distSq > noSq) return;
            if (distSq > yesSq) {
                double dist = NumUtil.uninterp(yesSq, noSq, distSq);
                if (Math.abs(chamberNoise.sample(here)) > dist) return;
            }
            Block block = here.getBlock();
            if (block == tube.getBlock()) return;
            //if (block instanceof BlockOre) return;
            IBlockState use = lava;
            if (here.y == middle.y - 1) {
                topLavas.add(here.copy());
            } else if (here.y > middle.y) {
                use = air;
            }
            here.set(use, notify);
        }
    }
}
