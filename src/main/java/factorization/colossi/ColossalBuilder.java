package factorization.colossi;

import java.util.Random;

import net.minecraft.init.Blocks;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.shared.Core;

public class ColossalBuilder {
    final Random rand;
    final Coord start;
    int leg_size, leg_height, leg_spread, body_height, arm_size, arm_height;
    int body_arm_padding, body_back_padding, body_front_padding;
    int shoulder_start;
    
    static final BlockState LEG = new BlockState(Core.registry.colossal_block, ColossalBlock.MD_LEG);
    static final BlockState BODY = new BlockState(Core.registry.colossal_block, ColossalBlock.MD_BODY);
    static final BlockState ARM = new BlockState(Core.registry.colossal_block, ColossalBlock.MD_ARM);
    static final BlockState MASK = new BlockState(Core.registry.colossal_block, ColossalBlock.MD_MASK);
    static final BlockState EYE = new BlockState(Core.registry.colossal_block, ColossalBlock.MD_EYE);
    static final BlockState HEART = new BlockState(Core.registry.colossal_block, ColossalBlock.MD_CORE);
    
    public ColossalBuilder(Random rand, Coord start) {
        this.rand = rand;
        for (int x = 0; x < 100; x++) rand.nextInt();
        leg_size = random_choice(1, 2, 2, 2, 3, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 6, 7);
        leg_height = random_linear(leg_size*3/2, leg_size*5/2);
        leg_height = clip(2, leg_height);
        leg_spread = random_linear_odd(1, 7);
        body_height = leg_height + random_linear(-leg_height/2, leg_height*2);
        body_height = clip(3, leg_height/3, body_height);
        arm_size = random_linear(2, leg_size);
        int total = body_height + leg_height;
        arm_height = random_linear(body_height + 1 + leg_height/2, (body_height + leg_height)*3/4);
        body_arm_padding = random_exponential(0, 3);
        body_back_padding = random_linear(0, 1) + random_exponential(0, 4);
        body_front_padding = clip(0, random_linear(-4, 2));
        if (body_height > 10) {
            shoulder_start = random_linear(0, 1) + random_exponential(0, body_height/3);
        } else {
            shoulder_start = 0;
        }
        this.start = start.add(new DeltaCoord(0, 0, -leg_size - leg_spread/2)).add(0, 0, -1);
    }
    
    int clip(int... vals) {
        int ret = vals[0];
        for (int val : vals) {
            ret = Math.max(ret, val);
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

    public void construct() {
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
        
        Coord arm_start = start.add(0, leg_height + 1 + body_height - shoulder_start, 0).add(0, 0, -body_arm_padding - 1).add(arm_size, 0, 0).add((leg_size - arm_size)/2, 0, 0);
        Coord arm_end = arm_start.add(-arm_size, -arm_height, -arm_size);
        fill(arm_start, arm_end, ARM);
        DeltaCoord armDelta = new DeltaCoord(0, 0, (leg_size + 1) * 2 + leg_spread + body_arm_padding * 2 + arm_size + 1);
        arm_start.adjust(armDelta);
        arm_end.adjust(armDelta);
        fill(arm_start, arm_end, ARM);
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
    
}
