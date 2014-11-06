package factorization.fzds.api;

import static factorization.fzds.api.DeltaCapability.*;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.shared.EntityFz;

public abstract class IDeltaChunk extends EntityFz {
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
    
    /***
     * @return the rotational offset. This is relative to getCorner().
     */
    public abstract Vec3 getRotationalCenterOffset();
    
    
    /***
     * @param newOffset The new rotational offset. This is relative to getCorner().
     */
    public abstract void setRotationalCenterOffset(Vec3 newOffset);
    
    /***
     * Helper function.
     * @param newCenter takes a real-world vector, and makes it the center of rotation, preserving the apparent position of the slice. (The IDC's actual position will change, however.)
     * 
     * TODO: If the rotation is not zero, things won't be preserved?
     * TODO: Flashy glitchiness.
     */
    public void changeRotationCenter(Vec3 newCenter) {
        Vec3 origCenter = getRotationalCenterOffset();
        Vec3 shadowCenter = real2shadow(newCenter);
        Coord min = getCorner();
        shadowCenter.xCoord -= min.x;
        shadowCenter.yCoord -= min.y;
        shadowCenter.zCoord -= min.z;
        setRotationalCenterOffset(shadowCenter);
        Vec3 ds = origCenter.subtract(shadowCenter);
        ds.xCoord += posX;
        ds.yCoord += posY;
        ds.zCoord += posZ;
        setPosition(ds.xCoord, ds.yCoord, ds.zCoord);
    }
    
    
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
    
    public void loadUsualCapabilities() {
        for (DeltaCapability cap : new DeltaCapability[] {
                COLLIDE,
                MOVE,
                ROTATE,
                DRAG,
                REMOVE_EXTERIOR_ENTITIES,
                TRANSPARENT,
                INTERACT,
                BLOCK_PLACE,
                BLOCK_MINE,
                REMOVE_ITEM_ENTITIES,
                ENTITY_PHYSICS,
        }) {
            permit(cap);
        }
        for (DeltaCapability cap : new DeltaCapability[] {
                
        }) {
            forbid(cap);
        }
    }
    
    
    
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
     * @return the center, in shadow coordinates. (Justs averages getCorner() and getFarCorner())
     */
    public Coord getCenter() {
        return getCorner().center(getFarCorner());
    }
    /***
     * @return the upper corner, in shadow coordinates
     */
    public abstract Coord getFarCorner();
    
    public void setPartName(String name) {
        if (name == null) throw new NullPointerException();
        this.partName = name;
    }
    
    protected String partName = "";
    
    @Override
    public String toString() {
        if (partName != "") {
            return "[DSE " + partName + " " + getEntityId() + "]";
        }
        return super.toString() + " - from " + getCorner() + "  to  " + getFarCorner() +
                "   center at " + getRotationalCenterOffset();
    }
    
    private Object controller;
    
    /**
     * @return the object set by setController. May be null.
     */
    public Object getController() {
        return controller;
    }
    
    /**
     * @param controller The controller responsible for this IDC. It is the responsibility of the controller to set this value when deserialized.
     */
    public void setController(Object controller) {
        this.controller = controller;
    }
}
