package factorization.fzds.api;

import static factorization.fzds.api.DeltaCapability.*;

import java.util.ArrayList;

import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.shared.EntityFz;

public abstract class IDeltaChunk extends EntityFz {
    //This would be an actual interface, but it needs to extend Entity.
    public IDeltaChunk(World w) { super(w); }
    
    /***
     * Sets the parent. The parent can be null. Reparenting probably isn't synchronized, so set it before spawning in the IDC.
     * Attatches the slice to its parent. When the parent translates & rotates, the child follows, *and* the does the same physics calculations as if it had native motion.
     * The child can rotate independently of the parent.
     * 
     * @param parent
     * @param jointPositionAtParent Specifies a location to attatch to in the parent's local entity space, relative to parent.getCorner()
     * 
     * 
     * Suppose you have an IDC for a large door. The rotational center is located at (0, 0, 0) to allow it to be opened on a hinge, and also kicked down.
     * setParent can be used to add a doorknob that will stay connected as appropriate, but still be able to be spun independently.
     * 
     * The knob will have its rotation point be the center of its area so that it can turn appropriately on its axis. If the knob is just a 3-block long stick passing through the door,
     * then the center would well be (0.5, 0.5, 1).
     * If the door has height = H and width = W, then jointPositionAtParent would be (W - 1, H / 2.0, 0)
     * 
     */
    public abstract void setParent(IDeltaChunk parent, Vec3 jointPositionAtParent);
    
    public abstract IDeltaChunk getParent();
    
    public abstract Vec3 getParentJoint();
    
    public abstract ArrayList<IDeltaChunk> getChildren();
    
    /***
     * @return the {@link Quaternion} representing the rotation (theta).
     */
    public abstract Quaternion getRotation();
    /***
     * @return the rotational offset. This is relative to getCorner().
     */
    public abstract Vec3 getRotationalCenterOffset();
    /***
     * @return the {@link Quaternion} representing the rotational velocity (omega).
     * 
     * This does not take into account setTargetRotation
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
    
    /**
     * Orders the IDC to move to a target rotation. This will set the rotational velocity to 0.
     * If setTargetRotation is called before a previous order has completed, the old order will be interrupted.
     * 
     * @param target The target rotation
     * @param tickTime How many ticks to reach that rotation
     * @param interp How to interpolate between them.
     */
    public abstract void orderTargetRotation(Quaternion target, int tickTime, Interpolation interp);
    
    public abstract boolean hasOrderedRotation();
    
    public abstract void cancelOrderedRotation();
    
    
    
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
        String ret;
        if (partName != "") {
            ret = "[DSE " + partName + " " + getEntityId() + "]";
        } else {
            ret = super.toString() + " - from " + getCorner() + "  to  " + getFarCorner() +
            "   center at " + getRotationalCenterOffset();
        }
        if (getParent() != null) {
            ret += ":PARENT=" + getParent().getEntityId();
        }
        ret += " " + ((int) posX) + " " + ((int) posY) + " " + ((int) posZ);
        return ret;
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
