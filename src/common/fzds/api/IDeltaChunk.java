package factorization.fzds.api;

import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import factorization.api.Coord;
import factorization.api.Quaternion;

public abstract class IDeltaChunk extends Entity {
    public IDeltaChunk(World w) { super(w); }
    public abstract Coord getCorner();
    public abstract Coord getCenter();
    public abstract Coord getFarCorner();
    
    public abstract Vec3 real2shadow(Vec3 real);
    public abstract Vec3 shadow2real(Vec3 real);
    public abstract void shadow2real(Coord c);
    public abstract void real2shadow(Coord c);
    
    public abstract void blocksChanged(int x, int y, int z);
    
    public abstract boolean can(Caps cap);
    public abstract IDeltaChunk permit(Caps cap);
    public abstract IDeltaChunk forbid(Caps cap);
    
    public abstract Quaternion getRotation();
    public abstract Quaternion getRotationalVelocity();
    public abstract void setRotation(Quaternion r);
    public abstract void setRotationalVelocity(Quaternion w);
    
    
}
