package factorization.common;

import cpw.mods.fml.common.IWorldGenerator;
import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.shared.Core;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.NoiseGeneratorOctaves;

import java.util.Random;

public class DarkIronOreGenerator implements IWorldGenerator {
    final int minMeteorR = 1, maxMeteorR = 2;
    final int maxWidth = maxMeteorR * 2;
    final int minBlastR = 20, maxBlastR = 28;
    final NoiseGeneratorOctaves noise = new NoiseGeneratorOctaves(new Random(0), 2);
    double[] samples = new double[maxWidth * maxWidth * maxWidth];


    Block stoneId;
    int stoneMd;

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
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
        //Simple pre-reqs
        if (!FzConfig.gen_dark_iron_ore) {
            return;
        }
        if (!world.provider.isSurfaceWorld()) {
            return;
        }
        if (!rule(chunkX, chunkZ)) return;

        int x, z;
        if (chunkX == 0 && chunkZ == 0) {
            x = z = 0;
        } else {
            //Find & test the location
            x = chunkX*16 + random.nextInt(16);
            z = chunkZ*16 + random.nextInt(16);
        }
        if (world.getBlock(x, 0, z) != Blocks.bedrock) {
            return;
        }
        for (int y = 1; y < 8; y++) {
            stoneId = world.getBlock(x, y, z);
            if (stoneId == Blocks.bedrock) continue;
            if (stoneId.isReplaceableOreGen(world, x, 1, z, Blocks.stone)) {
                stoneMd = world.getBlockMetadata(x, 1, z);
                break;
            }
            stoneId = null;
        }
        if (stoneId == null) {
            return;
        }

        int meteorRadius = random.nextInt(maxMeteorR - minMeteorR) + minMeteorR;
        Coord origin = new Coord(world, x, 1, z);
        meteorBlob(origin, meteorRadius);
        int blastRadius = random.nextInt(maxBlastR - minBlastR) + minBlastR;
        meteorBlast(origin, blastRadius);

        this.origin = null;
    }

    double rSq, rSqEnd;
    Coord origin, corner;
    int blobSize;
    void meteorBlob(Coord origin, int r) {
        this.origin = origin;
        rSq = r * r;
        rSqEnd = (r + 1) * (r + 1);
        blobSize = r * 2;
        int d = r;
        samples = noise.generateNoiseOctaves(samples, origin.x, origin.y, origin.z, blobSize, blobSize, blobSize, origin.x + d, origin.y + d, origin.z + d);
        Coord min = origin.add(-r, -r, -r);
        Coord max = origin.add(+r, +r, +r);
        corner = min;
        Coord.iterateCube(min, max, paintMeteor);
    }

    ICoordFunction paintMeteor = new ICoordFunction() {
        @Override
        public void handle(Coord here) {
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
            if (here.getBlock() != Blocks.bedrock) return;
            double distSq = here.distanceSq(origin);
            double threshold = Math.sqrt(distSq) / Math.sqrt(rSqEnd);
            if (here.w.rand.nextFloat() * 5 > threshold) {
                here.setIdMd(Core.registry.blasted_bedrock_block, 1, false);
            }
        }
    };

}
