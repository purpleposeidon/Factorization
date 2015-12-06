package factorization.notify;

import factorization.api.ISaneCoord;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public class SimpleCoord implements ISaneCoord {
    World w;
    int x, y, z;
    
    public SimpleCoord(World w, int x, int y, int z) {
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    @Override public World w() { return w; }

    @Override
    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }

}
