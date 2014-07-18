package factorization.fzds.api;

import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import factorization.api.Coord;
import factorization.api.Quaternion;

public abstract class IDeltaChunk extends Entity {
    //This would be an actual interface, but it needs to extend Entity.
    public IDeltaChunk(World w) { super(w); } //For java
    
    /***
     * @return the {@link Quaternion} representing the rotation (theta).
     */
    public abstract Quaternion getRotation();
    /***
     * @return the {@link Quaternion} representing the rotational velocity (omega).
     */
    public abstract Quaternion getRotationalVelocity();
    /***
     * Sets the rotation (theta).
     * @param r A {@link Quaternion}
     */
    public abstract void setRotation(Quaternion r);
    /***
     * Sets the rotational velocity (omega).
     * @param w A {@link Quaternion}
     */
    public abstract void setRotationalVelocity(Quaternion w);
    
    public abstract Vec3 getRotationalCenterOffset();
    
    public abstract void setRotationalCenterOffset(Vec3 newOffset);
    
    
    /***
     * Checks if the {@link DeltaCapability} is enabled.
     * @param cap A {@link DeltaCapability}
     * @return true if enabled
     */
    public abstract boolean can(DeltaCapability cap);
    /***
     * Enables a {@link DeltaCapability}. This should be done before spawning the entity, as clients will not get updates to this.
     * @param cap A {@link DeltaCapability}
     * @return this
     */
    public abstract IDeltaChunk permit(DeltaCapability cap);
    /***
     * Disables a {@link DeltaCapability}. This should be done before spawning the entity, as clients will not get updates to this.
     * @param cap A {@link DeltaCapability}
     * @return this
     */
    public abstract IDeltaChunk forbid(DeltaCapability cap);
    
    
    
    /***
     * @param realVector A {@link Vec3} in real-world coordinates.
     * @return a new {@link Vec3} in shadow coordinates with translations & rotations applied.
     */
    public abstract Vec3 real2shadow(final Vec3 realVector);
    /***
     * @param shadowVector A {@link Vec3} in shadow coordinates
     * @return a new {@link Vec3} in real-world coordinates with translations & rotations unapplied
     */
    public abstract Vec3 shadow2real(final Vec3 shadowVector);
    /***
     * @param realCoord A {@link Coord} in real world coordinates that will be mutated into shadow coordinates.
     */
    public abstract void real2shadow(Coord realCoord);
    /***
     * @param shadowCoord A {@link Coord} in shadow coordinates that will be mutated into real coordinates
     */
    public abstract void shadow2real(Coord shadowCoord);
    
    
    
    /***
     * @return the lower corner, in shadow coordinates.
     */
    public abstract Coord getCorner();
    /***
     * @return the center, in shadow coordinates
     * This method shouldn't be here. Just average getCorner() and getFarCorner().
     */
    @Deprecated
    public abstract Coord getCenter();
    /***
     * @return the upper corner, in shadow coordinates
     */
    public abstract Coord getFarCorner();
    
    
}
