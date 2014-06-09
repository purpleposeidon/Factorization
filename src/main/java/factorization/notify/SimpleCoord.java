package factorization.notify;

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
    @Override public int x() { return x; }
    @Override public int y() { return y; }
    @Override public int z() { return z; }

}
