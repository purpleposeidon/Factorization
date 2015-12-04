package factorization.colossi;

import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.ICoordFunction;
import factorization.colossi.Brush.BrushMask;
import factorization.shared.Core;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.NoiseGeneratorOctaves;

import java.util.*;

public class ColossalBuilder {
    final int seed;
    final Random rand;
    final Coord start;
    int leg_size, leg_height, leg_spread, body_height, arm_size, arm_height;
    int body_arm_padding, body_back_padding, body_front_padding;
    int shoulder_start;
    int face_width, face_height, face_depth;
    
    static final BlockState LEG = new BlockState(Core.registry.colossal_block, ColossalBlock.MD_LEG);
    static final BlockState BODY = new BlockState(Core.registry.colossal_block, ColossalBlock.MD_BODY);
    static final BlockState ARM = new BlockState(Core.registry.colossal_block, ColossalBlock.MD_ARM);
    static final BlockState MASK = new BlockState(Core.registry.colossal_block, ColossalBlock.MD_MASK);
    static final BlockState EYE = new BlockState(Core.registry.colossal_block, ColossalBlock.MD_EYE);
    static final BlockState HEART = new BlockState(Core.registry.colossal_block, ColossalBlock.MD_CORE);
    static final BlockState BODY_CRACK = new BlockState(Core.registry.colossal_block, ColossalBlock.MD_BODY_CRACKED);
    static final BlockState MASK_CRACK = new BlockState(Core.registry.colossal_block, ColossalBlock.MD_MASK_CRACKED);
    static final BlockState AIR = new BlockState(Blocks.air, 0);
    
    public ColossalBuilder(int seed, Coord start) {
        this.seed = seed;
        this.rand = new Random(seed);
        leg_size = random_choice(1, 1, 1, 1, 2);
        leg_height = random_linear(leg_size*3/2, leg_size*5/2);
        leg_height = clipMax(2, leg_height);
        leg_spread = random_choice(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 5);
        body_height = leg_height + random_linear(-leg_height/2, leg_height*2);
        body_height = clipMax(3, leg_height/3, body_height);
        if (body_height > 10) {
            shoulder_start = random_linear(0, 1) + random_exponential(0, body_height/3);
        } else {
            shoulder_start = 0;
        }
        int body_leg_height = leg_height + body_height;
        arm_size = clipMin(leg_size, random_linear(2, leg_size));
        arm_height = clipMin(body_leg_height - shoulder_start - 1, random_linear(body_height + 1 + leg_height/2, (body_height + leg_height)*3/4));
        body_arm_padding = random_exponential(0, 2);
        body_back_padding = random_linear(1, 2) + random_exponential(0, 3);
        body_front_padding = clipMax(0, random_linear(-4, 2));
        face_width = clipMax(3, leg_spread + random_linear(leg_size - 1, leg_size * 2));
        face_width = face_width + (face_width % 2);
        face_height = clipMax(3, random_linear(body_height*1/3, body_height*3/5));
        face_depth = clipMax(2, (leg_size + body_back_padding)/2);
        start = start.add(new DeltaCoord(0, 0, -leg_size - leg_spread/2)).add(0, 0, -1);
        int radius = leg_size + (leg_spread + 1)/2;
        this.start = moveUp(start, radius);
    }

    static Coord moveUp(Coord start, final int radius) {
        class Counter implements ICoordFunction {
            float total, solid;

            boolean isClear(Coord at) {
                Coord min = at.add(-radius, 0, -radius);
                Coord max = at.add(+radius, 0, +radius);
                Coord.iterateCube(min, max, this);
                return isSufficientlyClear();
            }

            @Override
            public void handle(Coord here) {
                total++;
                if (here.isReplacable()) return;
                if (here.isAir()) return;
                if (here.isSolid()) solid++;
            }

            boolean isSufficientlyClear() {
                return solid / total < 0.2F;
            }
        }
        int max_iter = 32;
        Coord level = start.copy();
        while (max_iter-- > 0) {
            if (new Counter().isClear(level)) return level;
            level.y++;
        }
        return start; // Uh oh!
    }
    
    int clipMax(int... vals) {
        int ret = vals[0];
        for (int val : vals) {
            ret = Math.max(ret, val);
        }
        return ret;
    }
    
    int clipMin(int... vals) {
        int ret = vals[0];
        for (int val : vals) {
            ret = Math.min(ret, val);
        }
        return ret;
    }
    
    int random_choice(int... options) {
        return options[rand.nextInt(options.length)];
    }
    
    int random_linear(int min, int max) {
        if (min == max) return min;
        if (max < min) {
            int low = max;
            int high = min;
            min = low;
            max = high;
        }
        int spread = (max - min) + 1;
        if (spread == 0) return min;
        return rand.nextInt(spread) + min;
    }
    
    int random_linear_odd(int min, int max) {
        if (min == max) return min;
        if (max < min) {
            int low = max;
            int high = min;
            min = low;
            max = high;
        }
        int spread = (max - min) + 1;
        if (spread == 0) return min;
        int ret = min;
        ret = rand.nextInt(spread) + min;
        if (ret % 2 == 0) {
            if (ret == max) {
                ret = max - 1;
            } else {
                ret++;
            }
        }
        return ret;
    }
    
    int random_exponential(int min, int max) {
        double r = rand.nextDouble();
        r *= r;
        double spread = max - min;
        return min + (int)(spread*r);
    }
    
    boolean maybe(double weight) {
        return rand.nextDouble() <= weight;
    }

    public void construct() {
        int face_center_adjust = ((leg_size * 2 + leg_spread + 1) - face_width) / 2;
        Coord mask_start = start.add(leg_size + body_front_padding + 1, leg_height + 1 + body_height, face_center_adjust);
        Coord mask_end = mask_start.add(-face_depth, face_height, face_width);
        fill(mask_start, mask_end, MASK);
        
        Coord leg_start = start.copy();
        Coord leg_end = leg_start.add(leg_size, leg_height, leg_size);
        fill(leg_start, leg_end, LEG);
        DeltaCoord legDelta = new DeltaCoord(0, 0, leg_size + leg_spread + 1);
        leg_start.adjust(legDelta);
        leg_end.adjust(legDelta);
        fill(leg_start, leg_end, LEG);
        
        
        Coord body_inner_start = start.add(0, leg_height + 1, 0);
        Coord body_start = body_inner_start.add(-body_back_padding, 0, -body_arm_padding);
        Coord body_end = body_inner_start.add(leg_size + body_front_padding, body_height, leg_size * 2 + leg_spread + body_arm_padding + 1);
        fill(body_start, body_end, BODY);
        
        Coord arm_start = start.add(0, leg_height + 1 + body_height - shoulder_start, 0).add(0, 0, -body_arm_padding - 1).add(arm_size, 0, 0);
        if (leg_size > arm_size) {
            arm_start = arm_start.add((leg_size - arm_size)/2, 0, 0);
        }
        Coord arm_end = arm_start.add(-arm_size, -arm_height, -arm_size);
        fill(arm_start, arm_end, ARM);
        DeltaCoord armDelta = new DeltaCoord(0, 0, (leg_size + 1) * 2 + leg_spread + body_arm_padding * 2 + arm_size + 1);
        arm_start.adjust(armDelta);
        arm_end.adjust(armDelta);
        fill(arm_start, arm_end, ARM);
        
        paintMask(EnumFacing.UP);
        paintMask(EnumFacing.DOWN);
        
        Coord standard_eyeball = start.add(leg_size + body_front_padding + 1, leg_height + 1 + body_height + (face_height / 2), 1 + leg_size + leg_spread / 2);
        fill(standard_eyeball, standard_eyeball, EYE);
        
        Drawer drawer = new Drawer();
        drawer.generateBlob();
        drawer.applyBiomeDecorations();
        
        Coord heart_crack = start.add(leg_size + body_front_padding, leg_height + 1 + ((body_height + 1) / 2), leg_size + ((1 + leg_spread) / 2));
        Coord hc_front = heart_crack.add(EnumFacing.EAST);
        if (MASK.matches(hc_front) || EYE.matches(hc_front)) {
            heart_crack.adjust(EnumFacing.EAST);
            fill(heart_crack, heart_crack, MASK_CRACK);
        } else {
            fill(heart_crack, heart_crack, BODY_CRACK);
        }
        Coord heart = heart_crack.add(EnumFacing.WEST);
        fill(heart, heart, HEART);
        TileEntityColossalHeart heartTe = new TileEntityColossalHeart();
        heartTe.loadInfoFromBuilder(this);
        heart.setTE(heartTe);
    }
    
    void fill(Coord min, Coord max, BlockState state) {
        min = min.copy();
        max = max.copy();
        Coord.sort(min, max);
        Coord at = min.copy();
        for (int x = min.x; x <= max.x; x++) {
            at.x = x;
            for (int y = min.y; y <= max.y; y++) {
                at.y = y;
                for (int z = min.z; z <= max.z; z++) {
                    at.z = z;
                    at.setIdMd(state.block, state.md, true);
                }
            }
        }
    }
    
    int get_width() {
        return (leg_size + 1) * 2 + leg_spread + body_arm_padding * 2 + (arm_size + 1) * 2 + 1;
    }
    
    int get_depth() {
        return leg_size + body_front_padding + body_back_padding;
    }
    
    int get_height() {
        return leg_height + 1 + body_height + face_height + 4;
    }
    
    void paintMask(EnumFacing dir) {
        MaskTemplate mask = MaskLoader.pickMask(rand, dir, face_width + 1, face_width + 1);
        if (mask == null) return;
        int mask_start = ((leg_spread + leg_size * 2) - face_width + 1) / 2;
        Coord mask_anchor = start.add(body_front_padding + leg_size + 1, leg_height + 1 + body_height - 1, mask_start);
        if (dir == EnumFacing.DOWN) {
            mask_anchor = mask_anchor.add(0, face_height, 0);
        }
        Brush maskBrush = new Brush(MASK, BrushMask.ALL, rand);
        Brush eyeBrush = new Brush(EYE, BrushMask.ALL, rand);
        mask.paint(mask_anchor, maskBrush, eyeBrush);
    }
    
    class Drawer implements ICoordFunction {
        final int BORDER = 2 + (leg_size * 7 / 3);
        final Coord body_inner_start = start.add(0, leg_height + 1, 0);
        final Coord bodyStart = body_inner_start.add(-body_back_padding, 0, -body_arm_padding);
        final Coord bodyEnd = body_inner_start.add(leg_size + body_front_padding, body_height, leg_size * 2 + leg_spread + body_arm_padding + 1);
        {
            bodyEnd.add(0, (int)(-leg_size * 0.5), 0);
            bodyStart.add(-leg_size, 0, 0);
            Coord.sort(bodyStart, bodyEnd);
            bodyStart.y++; // Don't scrape the ground
        }
        int arm_ext = 1 + arm_size / 2;
        int max_radius = leg_size * 4;
        final Coord blobStart = bodyStart.add(-max_radius, -max_radius, -max_radius);
        final Coord blobEnd = bodyEnd.add(max_radius, max_radius, max_radius);
        
        final double[] noise;
        final DeltaCoord size;
        final double len;
        
        final HashSet<Coord> painted = new HashSet();
        
        Drawer() {
            Coord.sort(blobStart, blobEnd);
            size = blobEnd.difference(blobStart);
            size.x++;
            size.y++;
            size.z++;
            NoiseGeneratorOctaves noiseGen = new NoiseGeneratorOctaves(rand, 2);
            noise = new double[size.x * size.y * size.z];
            double s = 1.0 / 256; // 1.0/512.0;
            noiseGen.generateNoiseOctaves(noise,
                    blobStart.x, blobStart.y, blobStart.z,
                    size.x, size.y, size.z,
                    blobEnd.x * s, blobEnd.y * s, blobEnd.z * s);
            for (int i = 0; i < noise.length; i++) {
                noise[i] = (noise[i] + 1) / 2;
            }
            this.len = size.magnitude();
        }
        
        double sum;
        int n;
        
        public void reset() {
            sum = 1;
            n = -1;
        }
        
        double sample(Coord here) {
            int x = here.x - blobStart.x;
            int y = here.y - blobStart.y;
            int z = here.z - blobStart.z;
            int idx = y + (z * size.y) + (x * size.y * size.z);
            return noise[idx];
        }
        
        @Override
        public void handle(Coord here) {
            double s = sample(here);
            if (s > sum && here.isReplacable()) {
                here.setId(Blocks.stone);
                sum += (sum + s) / len;
            }
        }
        
        Coord work = start.copy();
        
        Coord clipToBody(Coord at) {
            work.set(at);
            at = work;
            
            if (at.x < bodyStart.x) at.x = bodyStart.x;
            if (at.y < bodyStart.y) at.y = bodyStart.y;
            if (at.z < bodyStart.z) at.z = bodyStart.z;
            if (at.x > bodyEnd.x) at.x = bodyEnd.x;
            if (at.y > bodyEnd.y) at.y = bodyEnd.y;
            if (at.z > bodyEnd.z) at.z = bodyEnd.z;
            
            return at;
        }
        
        void generateBlob() {
            Coord.iterateEmptyBox(bodyStart, bodyEnd, new ICoordFunction() {
                @Override
                public void handle(Coord here) {
                    if (here.x >= bodyEnd.x - leg_size/2 || here.z == bodyStart.z || here.z == bodyEnd.z) return;
                    reset();
                    double val = sample(here);
                    paint(here, (int) (val + 1.9));
                }
            });
        }
        
        void paint(Coord center, int r) {
            if (r <= 0) return;
            Coord at = center.copy();
            double rShrink = 0.99;
            if (r > max_radius * rShrink) r *= rShrink;
            for (int dx = -r; dx <= r; dx++) {
                at.x = center.x + dx;
                int hypotX = dx * dx;
                for (int dy = -r; dy <= r; dy++) {
                    at.y = center.y + dy;
                    int hypotY = hypotX + dy * dy;
                    for (int dz = -r; dz <= r; dz++) {
                        at.z = center.z + dz;
                        int hypotSq = hypotY + dz * dz;
                        double R = r + sample(at) * 0.5;
                        R *= R;
                        if (hypotSq <= R) {
                            draw(at);
                        }
                    }
                }
            }
        }
        
        void draw(Coord at) {
            if (at.x > bodyEnd.x) return;
            if (at.isReplacable()) {
                at.setId(Blocks.stone);
                painted.add(at.copy());
            }
        }
        
        void applyBiomeDecorations() {
            BiomeGenBase biome = null; // 1 biome per colossus.
            for (Coord at : painted) {
                biome = at.getBiome();
                break;
            }
            if (biome == null) return;
            ArrayList<Coord> sorted = new ArrayList();
            sorted.addAll(painted);
            Collections.sort(sorted, new Comparator<Coord>() {
                @Override
                public int compare(Coord a, Coord b) {
                    if (a.x == b.x) {
                        return a.z - b.z;
                    }
                    return a.x - b.x;
                }
            });
            Block[] blocks = new Block[0x100];
            byte[] mds = new byte[0x100];
            
            double stoneNoise = rand.nextDouble();
            Coord lastCol = null;
            for (Coord at : sorted) {
                if (lastCol == null || (lastCol.x != at.x || lastCol.z != at.z)) {
                    biomeifyBlocks(lastCol, blocks, mds, biome, stoneNoise);
                    lastCol = at;
                    Arrays.fill(blocks, Blocks.air);
                    Arrays.fill(mds, (byte) 0);
                }
                blocks[at.y] = at.getBlock();
                mds[at.y]= (byte) at.getMd(); 
            }
            biomeifyBlocks(lastCol, blocks, mds, biome, stoneNoise);
            
            
            
        }
        
        void biomeifyBlocks(Coord col, Block[] blocks, byte[] mds, BiomeGenBase biome, double stoneNoise) {
            if (col == null) return;
            biome.genTerrainBlocks(col.w, rand, blocks, mds, 0, 0, stoneNoise);
            Coord at = col.copy();
            boolean onSolid = false;
            for (int y = 0; y < blocks.length; y++) {
                Block id = blocks[y];
                byte md = mds[y];
                if (id == Blocks.bedrock) continue;
                if (id == Blocks.air) {
                    onSolid = false;
                    continue;
                }
                if (!onSolid && id instanceof BlockFalling) {
                    if (id == Blocks.sand) {
                        id = Blocks.sandstone;
                        md = 0;
                        // NORELEASE: 1.8 red sandstone
                    } else {
                        id = Blocks.stone;
                        md = 0;
                    }
                }
                at.y = y;
                if (id == Blocks.grass) {
                    at.y++;
                    if (at.isSolid()) id = Blocks.dirt;
                    at.y--;
                }
                at.setIdMd(id, md, false);
                onSolid = true;
            }
        }
        
    }
    
    
}
