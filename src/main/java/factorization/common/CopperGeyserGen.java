package factorization.common;

import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.ICoordFunction;
import factorization.shared.Core;
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
    }

    private static int getWidth() {
        return FzConfig.volcanism_region_size_in_chunks;
    }

    private static Random getRegionRandom(World world, long x, long z) {
        long seed = (x / getWidth() | ((z / getWidth()) * getWidth())) + world.getSeed() + 99;
        return new Random(seed);
    }

    public static boolean isChunkVolcanic(World world, int x, int z, Random chunkRandom, float biomeModifier) {
        Random regionRand = getRegionRandom(world, x, z);
        float biomeRand = regionRand.nextFloat();
        int xRand = regionRand.nextInt(getWidth());
        int zRand = regionRand.nextInt(getWidth());
        // biomeModifier may change throughout the region since chunks may have different biomes,
        // so the RNG calls are always made to increase consistency. Maybe it's not necessary. But it's easy.
        //

        if (biomeRand > FzConfig.region_volcanism_chance * biomeModifier) return false;
        int localX = x % getWidth();
        int localZ = z % getWidth();
        if (xRand == localX && zRand == localZ) return true;
        return chunkRandom.nextInt(getWidth() * getWidth()) < FzConfig.average_extra_geysers;
    }

    public static final float biome_suitability_modifier = 0.1F;
    public static float getModifierForBiome(BiomeGenBase biome) {
        int points = 0;
        for (BiomeDictionary.Type type : BiomeDictionary.getTypesForBiome(biome)) {
            // It's not safe to do a switch on BiomeDictionary.Type; I've gotten a crash caused by weirdos modifying the enum.
            if (type == BiomeDictionary.Type.OCEAN
                    || type == BiomeDictionary.Type.RIVER
                    || type == BiomeDictionary.Type.WATER
                    || type == BiomeDictionary.Type.NETHER
                    || type == BiomeDictionary.Type.END) {
                return 0;
            }
            if (type == BiomeDictionary.Type.HOT) points++;
            if (type == BiomeDictionary.Type.JUNGLE) points -= 2;
            if (type == BiomeDictionary.Type.MAGICAL) points--;
            if (type == BiomeDictionary.Type.WASTELAND) points++;
            if (type == BiomeDictionary.Type.MOUNTAIN) points++;
            if (type == BiomeDictionary.Type.HILLS) points++;
            if (type == BiomeDictionary.Type.DEAD) points++;
            if (type == BiomeDictionary.Type.MESA) points += 3;
            if (type == BiomeDictionary.Type.SANDY) points++;
            if (type == BiomeDictionary.Type.SNOWY) points++;
            if (type == BiomeDictionary.Type.MUSHROOM) points--;
            if (type == BiomeDictionary.Type.CONIFEROUS) points++;
        }
        return 1 + biome_suitability_modifier * points;
    }

    private static final boolean notify = true; // We do want notify, right?
    int extrudersPerChamber = 6;

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
        int x = 8 + chunkX * 16;
        int z = 8 + chunkZ * 16;
        Coord start = new Coord(world, x, world.getSeaLevel(), z);
        BiomeGenBase biome = start.getBiome();
        float rarityModifier = getModifierForBiome(biome);
        if (!isChunkVolcanic(world, chunkX, chunkZ, random, rarityModifier)) return;
        generateAt(random, start);
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
        if (avail_space > 20) avail_space = 20;

        final int height = avail_space + random.nextInt(avail_space);
        final int minR = 2, maxR = 5;
        final int rRange = maxR - minR;
        final int rBoost = 2;
        final int startR = random.nextInt(rRange - rBoost) + minR + rBoost;
        int varR = startR;
        NoiseSampler pipeNoise = new NoiseSampler(at, maxR, height, random, 1.0 / 8.0);
        boolean firstLayer = true;
        Coord pos = at.copy();
        int fuzz = 3;
        boolean top_is_water = random.nextInt(9) == 0;
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
        int depth = height;
        while (depth-- > 0) {
            final double maxVarR = varR;
            final double minVarR = varR - fuzz;
            final double shrinkComplete = NumUtil.uninterp(minR, startR, varR);
            final double diveComplete = depth / (double) height;
            for (int dx = -varR; dx <= +varR; dx++) {
                for (int dz = -varR; dz <= +varR; dz++) {
                    pos.setOff(at, dx, 0, dz);
                    double sample = Math.abs(pipeNoise.sample(pos));
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    double interp = NumUtil.uninterp(minVarR, maxVarR, dist);
                    double combined = interp + sample;
                    if (combined > 1 && interp > 0.15) continue;
                    if (firstLayer) {
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
            if (firstLayer) {
                geyser_spot = at.add(0, -1, 0);
                firstLayer = false;
            } else if (varR > minR && shrinkComplete > diveComplete && random.nextBoolean()) {
                varR--;
            }
            at.y--;
        }
        if (geyser_spot != null) {
            geyser_spot.set(geyser, notify);
        }

        int R = chamberSize;
        at.y += R / 2;
        final NoiseSampler chamberNoise = new NoiseSampler(at.add(-R, -R, -R), at.add(+R, +R, +R), random, 1.0 / 8);
        int chamberSure = chamberSize - 1;
        final double yesSq = chamberSure * chamberSure;
        final double noSq = (chamberSize * chamberSize) - 1 /* squish slightly to avoid ugly 1-block nubs at axis of the chamber */;
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
                // Could clamp?
                return 0;
            }
            DeltaCoord dc = at.difference(min);
            // What's the data order? Look at the loops in NoiseGeneratorImproved.populateNoiseArray
            // The loop order is: XZY.
            int idx = dc.x * (size.y * size.z) + dc.z * size.y + dc.y;
            if (idx < 0 || idx >= noise.length) {
                // Shouldn't happen.
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
            max = max.add(1, 1, 1);
            size = max.difference(min);
            maxIdx = size.x * size.y * size.z;
            noise = new double[maxIdx];
            new NoiseGeneratorImproved(rng).populateNoiseArray(noise, min.x, min.y, min.z, size.x, size.y, size.z, s, s, s, 1);
            sample(this.min);
            sample(this.max);
        }

    }

    private class ChamberBuilder implements ICoordFunction {
        private final Coord middle;
        private final double noSq;
        private final double yesSq;
        private final NoiseSampler chamberNoise;
        private final double no, yes;
        ArrayList<Coord> topLavas = new ArrayList<Coord>();

        public ChamberBuilder(Coord middle, double noSq, double yesSq, NoiseSampler chamberNoise) {
            this.middle = middle;
            this.noSq = noSq;
            this.yesSq = yesSq;
            this.no = Math.sqrt(noSq);
            this.yes = Math.sqrt(yesSq);
            this.chamberNoise = chamberNoise;
        }

        @Override
        public void handle(Coord here) {
            double distSq = middle.distanceSq(here);
            if (distSq > noSq) return;
            if (distSq > yesSq) {
                double dist = NumUtil.uninterp(yes, no, Math.sqrt(distSq));
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
