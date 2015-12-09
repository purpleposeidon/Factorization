package factorization.notify;

import factorization.api.ISaneCoord;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public class SimpleCoord implements ISaneCoord {
    final World w;
    final BlockPos pos;

    public SimpleCoord(World w, BlockPos pos) {
        this.w = w;
        this.pos = pos;
    }
    
    @Override public World w() { return w; }

    @Override
    public BlockPos toBlockPos() {
        return pos;
    }

}
