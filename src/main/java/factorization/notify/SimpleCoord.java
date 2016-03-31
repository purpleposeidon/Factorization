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

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SimpleCoord)) return false;
        SimpleCoord o = (SimpleCoord) obj;
        return w == o.w && pos.equals(o.pos);
    }
}
