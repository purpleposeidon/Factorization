package factorization.fzds;

import factorization.api.Coord;
import net.minecraft.util.Vec3;

public interface IDeltaChunk {
    Coord getCorner();
    Coord getCenter();
    Coord getFarCorner();
    
    Vec3 real2shadow(Vec3 real);
    Vec3 shadow2real(Vec3 real);
    void shadow2real(Coord c);
    void real2shadow(Coord c);
    
    void blocksChanged(int x, int y, int z);
    
    void dropContents();
    
    boolean can(Caps cap);
    IDeltaChunk permit(Caps cap);
    IDeltaChunk forbid(Caps cap);
    
    
}
