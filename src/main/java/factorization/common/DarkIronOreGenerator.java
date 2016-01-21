package factorization.common;

import java.util.Random;

import com.google.common.base.Predicate;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.NoiseGeneratorOctaves;

import net.minecraftforge.fml.common.IWorldGenerator;

import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.shared.Core;
import factorization.util.SpaceUtil;

public class DarkIronOreGenerator implements IWorldGenerator {
    static final int minMeteorR = 1, maxMeteorR = 2;
    static final int maxWidth = maxMeteorR * 2;
    static final int minBlastR = 20, maxBlastR = 28;
    static final int genRange = (int) Math.ceil(maxBlastR * 2 / 16.0);
    static final NoiseGeneratorOctaves noise = new NoiseGeneratorOctaves(new Random(0), 2);

    boolean base(int x, int z) {
        boolean a = (x/4 + z/4) % 3 == 0;
        boolean b = (x % 4 == 0) && (z % 4 == 0);
        return a && b;
    }

    boolean rule(int x, int z) {
        if (x < 0) x = -x;
        if (z < 0) z = -z;
        byte n = -1;
        int N = ((x-1)/8 + (z+1)/8) % 4;
        if (base(x, z)) n = 0;
        else if (base(x + 1, z)) n = 1;
        else if (base(x, z + 1)) n = 2;
        else if (base(x + 1, z + 1)) n = 3;
        else {
            return false;
        }
        return N == n;
    }


    @Override
    public void generate(Random UNUSABLE_rng, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
        //Simple pre-reqs
        if (!FzConfig.gen_dark_iron_ore) {
            return;
        }
        if (!world.provider.isSurfaceWorld()) {
            return;
        }
        int bedrockX = chunkX * 16 + 8;
        int bedrockZ = chunkZ * 16 + 8;
        if (world.getBlockState(new BlockPos(bedrockX, 0, bedrockZ)).getBlock() != Blocks.bedrock) {
            return;
        }
        for (int dcx = -genRange; dcx <= genRange; dcx++) {
            for (int dcz = -genRange; dcz <= genRange; dcz++) {
                final int srcChunkX = chunkX + dcx;
                final int srcChunkZ = chunkZ + dcz;
                if (!rule(srcChunkX, srcChunkZ)) continue;

                final Coord min = new Coord(world, chunkX * 16, 0, chunkZ * 16);
                final Coord max = min.add(16, 256, 16);

                final long seed = world.getSeed() + srcChunkX * 1000 + srcChunkZ;
                final Random random = new Random(seed);

                new BlitGen(min, max, srcChunkX, srcChunkZ, random).generate();
            }
        }
    }


    static class BlitGen {
        final Coord min, max;
        final int chunkX, chunkZ;
        final Random random;
        final AxisAlignedBB chunkBox;
        double[] samples = new double[maxWidth * maxWidth * maxWidth];

        Block stoneId = Blocks.stone;
        int stoneMd = 0;

        BlitGen(Coord min, Coord max, int chunkX, int chunkZ, Random random) {
            this.min = min;
            this.max = max;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.random = random;
            this.chunkBox = SpaceUtil.createAABB(min, max);

            Coord here = min.add(8, 0, 8);
            for (int y = 1; y < 8; y++) {
                here.y++;
                stoneId = here.getBlock();
                if (stoneId == Blocks.bedrock) continue;
                if (stoneId.isReplaceableOreGen(here.w, here.toBlockPos(), new Predicate<IBlockState>() {
                    @Override
                    public boolean apply(IBlockState input) {
                        return input.getBlock() == Blocks.stone;
                    }
                })) {
                    stoneMd = here.getMd();
                    break;
                }
            }
        }

        public void generate() {
            int x = chunkX * 16 + random.nextInt(16);
            int z = chunkZ * 16 + random.nextInt(16);

            int meteorRadius = random.nextInt(maxMeteorR - minMeteorR) + minMeteorR;
            Coord origin = new Coord(min.w, x, 1, z);
            meteorBlob(origin, meteorRadius);
            int blastRadius = random.nextInt(maxBlastR - minBlastR) + minBlastR;
            meteorBlast(origin, blastRadius);
        }

        double rSq, rSqEnd;
        Coord origin, corner;
        int blobSize;
        void meteorBlob(Coord origin, int r) {
            Coord blobMin = origin.add(-r, -r, -r);
            Coord blobMax = origin.add(+r, +r, +r);
            AxisAlignedBB blobBox = SpaceUtil.createAABB(min, max);
            if (!blobBox.intersectsWith(chunkBox)) return;

            this.origin = origin;
            rSq = r * r;
            rSqEnd = (r + 1) * (r + 1);
            blobSize = r * 2;
            int d = r;
            samples = noise.generateNoiseOctaves(samples, origin.x, origin.y, origin.z, blobSize, blobSize, blobSize, origin.x + d, origin.y + d, origin.z + d);
            corner = blobMin;
            Coord.iterateCube(blobMin, blobMax, paintMeteor);
        }

        ICoordFunction paintMeteor = new ICoordFunction() {
            @Override
            public void handle(Coord here) {
                if (!here.inside(min, max)) return;
                if (here.y < 0) return;
                double distSq = here.distanceSq(origin);
                if (distSq > rSqEnd) return;
                if (distSq > rSq) {
                    // At the edge. If the noise < 0, then we bail. Samples are within [-1, +1]
                    int sx = here.x - corner.x;
                    int sy = here.y - corner.y;
                    int sz = here.z - corner.z;
                    double sample = samples[sx * blobSize * blobSize + sy * blobSize + sz];
                    if (sample < 0) return;
                }
                if (here.y == 0) {
                    here.setId(Core.registry.fractured_bedrock_block, false);
                } else {
                    here.setId(Core.registry.dark_iron_ore, false);
                }
            }

        };

        Coord blastOrigin;
        void meteorBlast(Coord origin, int r) {
            int blastHeight = 7;
            blastOrigin = origin.add(0, r - 2, 0);
            rSq = r * r;
            rSqEnd = blastHeight * blastHeight;
            Coord min = origin.add(-r, 0, -r);
            Coord max = origin.add(+r, blastHeight, +r);
            Coord.iterateCube(min, max, paintBlast);
            min.y = 0;
            min.adjust(-r / 3, 0, -r / 3);
            max.adjust(+r / 3, 0, +r / 3);
            Coord.iterateCube(min, max, burnBlast);
        }

        ICoordFunction paintBlast = new ICoordFunction() {
            @Override
            public void handle(Coord here) {
                if (!here.inside(min, max)) return;
                double distSq = here.distanceSq(blastOrigin);
                if (distSq > rSq) return;
                if (here.getBlock() == Blocks.bedrock) {
                    here.setIdMd(stoneId, stoneMd, false);
                }
            }
        };

        ICoordFunction burnBlast = new ICoordFunction() {
            @Override
            public void handle(Coord here) {
                if (!here.inside(min, max)) return;
                if (here.getBlock() != Blocks.bedrock) return;
                double distSq = here.distanceSq(origin);
                double threshold = Math.sqrt(distSq) / Math.sqrt(rSqEnd);
                if (here.w.rand.nextFloat() * 5 > threshold) {
                    here.setIdMd(Core.registry.blasted_bedrock_block, 1, false);
                }
            }
        };
    }

}
