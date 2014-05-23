package factorization.fzds;

import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

class Range {
    double lower, upper;
    boolean initialized = false;
    Range() { }
    
    Range(double lower, double upper) {
        this.lower = lower;
        this.upper = upper;
        this.initialized = true;
    }
    
    boolean intersects(Range other) {
        return !(upper < other.lower || other.upper < lower);
    }
    
    void include(Vec3 point, ForgeDirection axis) {
        switch (axis) {
        case EAST:
        case WEST:
            include(point.xCoord);
            break;
        case UP:
        case DOWN:
            include(point.yCoord);
            break;
        case NORTH:
        case SOUTH:
            include(point.zCoord);
            break;
        case UNKNOWN: break;
        }
    }
    
    void include(double point) {
        if (!initialized) {
            initialized = true;
            lower = upper = point;
            return;
        }
        lower = Math.min(point, lower);
        upper = Math.max(point, upper);
    }
    
    void reset() {
        initialized = false;
        lower = 0;
        upper = 0;
    }
}