package factorization.colossi;

import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.colossi.Brush.BrushMask;
import factorization.shared.Core;

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
    
    public ColossalBuilder(int seed, Coord start) {
        this.seed = seed;
        this.rand = new Random(seed);
        for (int x = 0; x < 100; x++) rand.nextInt();
        //leg_size = random_choice(1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3); //, 4 , 4, 4, 4, 5, 5, 5, 6, 7
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
        body_back_padding = random_linear(0, 1) + random_exponential(0, 4);
        body_front_padding = clipMax(0, random_linear(-4, 2));
        face_width = clipMax(3, leg_spread + random_linear(leg_size - 1, leg_size * 2));
        face_width = face_width + (face_width % 2);
        face_height = clipMax(3, random_linear(body_height*1/3, body_height*3/5));
        face_depth = clipMax(2, (leg_size + body_back_padding)/2);
        this.start = start.add(new DeltaCoord(0, 0, -leg_size - leg_spread/2)).add(0, 0, -1);
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
        
        paintMask(ForgeDirection.UP);
        paintMask(ForgeDirection.DOWN);
        
        Coord standard_eyeball = start.add(leg_size + body_front_padding + 1, leg_height + 1 + body_height + (face_height / 2), 1 + leg_size + leg_spread / 2);
        fill(standard_eyeball, standard_eyeball, EYE);
        
        growTerrainBlob();
        
        Coord heart = start.add(leg_size + body_front_padding, leg_height + 1 + ((body_height + 1) / 2), leg_size + ((1 + leg_spread) / 2));
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
    
    void paintMask(ForgeDirection dir) {
        MaskTemplate mask = MaskLoader.pickMask(rand, dir, face_width + 1, face_width + 1);
        if (mask == null) return;
        int mask_start = ((leg_spread + leg_size * 2) - face_width + 1) / 2;
        Coord mask_anchor = start.add(body_front_padding + leg_size + 1, leg_height + 1 + body_height - 1, mask_start);
        if (dir == ForgeDirection.DOWN) {
            mask_anchor = mask_anchor.add(0, face_height, 0);
        }
        Brush maskBrush = new Brush(MASK, BrushMask.ALL, rand);
        Brush eyeBrush = new Brush(EYE, BrushMask.ALL, rand);
        mask.paint(mask_anchor, maskBrush, eyeBrush);
    }
    
    void growTerrainBlob() {
        int BORDER = 6 + (leg_size * 7 / 3);
        Coord blobStart = start.add(-body_back_padding - BORDER - leg_size, 0, -body_arm_padding - arm_size - BORDER);
        Coord blobEnd = start.add(body_front_padding + leg_size + 1 + clipMin(leg_size - 1, 2), leg_height + 1 + body_height + face_height + BORDER, leg_size * 2 + leg_spread + body_arm_padding + arm_size + 1 + BORDER);
        blobEnd = blobEnd.add(0, face_height*5/2, 0);
        BlobBuilder life = new BlobBuilder(rand, blobStart, blobEnd);
        life.populateCellsFromWorld();
        
        life.sprinkleSeeds(0.05);
        life.bubble();
        life.pruneTick();
        for (int i = 0; i < 3; i++) {
            life.life(1, 1);
        }
        life.reinforce(1);
        life.rainSeeds(0.3, 0.1);
        life.bubble();
        //life.smooth(4);
        life.reinforce(0.8);
        life.pruneTick();
        
        life.sprinkleSeeds(0.025);
        life.pruneTick();
        life.reinforce(0.5);
        life.rainSeeds(0.05, 0.75);
        
        
        
        
        life.removeReinforcements();
        //life.growthTick();
        life.upset(2, 1, 4);
        //
        
        /*life.sprinkleSeeds(0.25);
        life.life(3, 5);
        life.life(3, 5);
        life.life(3, 5);
        life.life(3, 5);
        life.life(3, 5);
        life.pruneTick();*/
        
        /*life.sprinkleSeeds(0.025);
        life.bubble();
        life.removeReinforcements();
        life.pruneTick();*/
        //life.rainSeeds(0.05, 0.75);
        
        /*life.sprinkleSeeds(0.025);
        life.bubble();
        life.life(2, 10);*/
        
        /*{
            // This is quite good.
            life.sprinkleSeeds(0.025);
            life.pruneTick();
            life.rainSeeds(0.05, 0.75);
        }*/
        
        //life.life(0, 3);
        //life.rainSeeds(0.05, 0.75);
        //life.life(0, 0);
        //life.sprinkleSeeds(0.025);
        //life.growthTick();
        
        //life.reinforce(1);
        
        //life.sprinkleSeeds(0.025);
        //life.pruneTick();
        //life.rainSeeds(0.05, 0.75);
        
        //life.bubble();
        //life.smooth(2);
        
        life.saveCellsToWorld(Blocks.stone);
    }
    
}
