package factorization.fzds.interfaces;

import factorization.api.Coord;
import factorization.api.Mat;
import factorization.api.Quaternion;
import factorization.fzds.BasicTransformOrder;
import factorization.fzds.DeltaChunk;
import factorization.fzds.ITransformOrder;
import factorization.fzds.interfaces.transform.Pure;
import factorization.fzds.interfaces.transform.TransformData;
import factorization.util.NORELEASE;
import factorization.util.SpaceUtil;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import javax.annotation.CheckReturnValue;
import java.util.List;

import static factorization.fzds.interfaces.DeltaCapability.*;

public interface IDimensionSlice {
    default void todo() {
        NORELEASE.fixme("de java-8 this interface");
    }

    /**
     * Sets the parent. The parent can be null. Reparenting probably isn't synchronized, so set it before spawning in the IDC.
     * Attatches the slice to its parent. When the parent translates & rotates, the child follows, *and* does the same physics calculations as if it had native motion.
     * The child can rotate independently of the parent.
     * Local linear velocities in the child are not supported at this time, nor is dynamic displacement.
     *
     * The return value of {@link IDimensionSlice#getParentJoint} is determined from the positioning of the parent and this child.
     *
     * @param parent The IDC to be the parent
     * @throws java.lang.IllegalArgumentException if a loop is attempted.
     */
    void setParent(IDimensionSlice parent);
    
    IDimensionSlice getParent();

    /**
     * @return the center of location, in the parent's IDC space. Returns null if there is no parent.
     */
    Vec3 getParentJoint();
    
    List<IDimensionSlice> getChildren();

    TransformData<Pure> getTransform();

    TransformData<Pure> getVel();

    Mat getShadow2Real();
    Mat getReal2Shadow();

    TransformData<Pure> getTransform(float partial);

    boolean hasOrders();

    /**
     * @return the active order. If the order is inactive, then it'll return true on isNull.
     */
    ITransformOrder getOrder();

    /**
     * @see ITransformOrder
     * @see BasicTransformOrder
     * @param order The order to make.
     */
    void giveOrder(ITransformOrder order);

    void cancelOrder();


    /**
     * Helper function.
     * @param newCenter takes a real-world vector, and makes it the center of rotation, preserving the apparent position of the slice. (The IDC's actual position will change, however.)
     * 
     * TODO: If the rotation is not zero, things won't be preserved?
     * TODO: Flashy glitchiness.
     */
    default void changeRotationCenter(Vec3 newCenter) {
        Vec3 origCenter = getTransform().getOffset();
        Coord min = getMinCorner();
        Vec3 shadowCenter = real2shadow(newCenter).subtract(min.x, min.y, min.z);
        getTransform().setOffset(shadowCenter);
        Vec3 ds = origCenter.subtract(shadowCenter).add(SpaceUtil.fromEntPos(getEntity()));
        getTransform().setPos(ds);
    }

    /**
     * Helper function. Applies all parent rotations to rot.
     */
    default void multiplyParentRotations(Quaternion rot) {
        IDimensionSlice parent = getParent();
        while (parent != null) {
            parent.getTransform().getRot().incrToOtherMultiply(rot);
            parent = parent.getParent();
        }
    }
    
    
    /**
     * Checks if the {@link DeltaCapability} is enabled.
     * @param cap A {@link DeltaCapability}
     * @return true if enabled
     */
    boolean can(DeltaCapability cap);

    /**
     * Enables a {@link DeltaCapability}. This should be done before spawning the entity, as clients will not get updates to this.
     * @param caps An array of {@link DeltaCapability}
     * @return this
     */
    IDimensionSlice permit(DeltaCapability... caps);

    /**
     * Disables a {@link DeltaCapability}. This should be done before spawning the entity, as clients will not get updates to this.
     * @param caps An array of {@link DeltaCapability}
     * @return this
     */
    IDimensionSlice forbid(DeltaCapability... caps);

    default void loadUsualCapabilities() {
        forbid(DeltaCapability.values());
        permit(COLLIDE,
                MOVE,
                ROTATE,
                DRAG,
                REMOVE_EXTERIOR_ENTITIES,
                INTERACT,
                BLOCK_PLACE,
                BLOCK_MINE,
                REMOVE_ITEM_ENTITIES,
                ENTITY_PHYSICS);
    }


    default World getRealWorld() {
        return ((Entity) this).worldObj;
    }

    default World getShadowWorld() {
        if (getRealWorld().isRemote) {
            return DeltaChunk.getClientShadowWorld();
        } else {
            return DeltaChunk.getServerShadowWorld();
        }
    }

    
    /**
     * @param realVector A {@link Vec3} in real-world coordinates.
     * @return a new {@link Vec3} in shadow coordinates with translations & rotations applied.
     */
    @CheckReturnValue
    @Deprecated
    default Vec3 real2shadow(final Vec3 realVector) {
        return getReal2Shadow().mul(realVector);
    }

    /**
     * @param shadowVector A {@link Vec3} in shadow coordinates
     * @return a new {@link Vec3} in real-world coordinates with translations & rotations unapplied
     */
    @CheckReturnValue
    @Deprecated
    default Vec3 shadow2real(final Vec3 shadowVector) {
        return getShadow2Real().mul(shadowVector);
    }

    @CheckReturnValue
    @Deprecated
    default Coord real2shadow(Coord real) {
        return getReal2Shadow().mul(getShadowWorld(), real);
    }

    @CheckReturnValue
    @Deprecated
    default Coord shadow2real(Coord shadow) {
        return getShadow2Real().mul(getRealWorld(), shadow);
    }

    @CheckReturnValue
    @Deprecated
    default BlockPos real2shadow(BlockPos real) {
        return getReal2Shadow().mul(real);
    }

    @CheckReturnValue
    @Deprecated
    default BlockPos shadow2real(BlockPos shadow) {
        return getShadow2Real().mul(shadow);
    }

    /**
     * @param shadowBox Box to convert from shadow space to real space.
     * @return The real-space box. Warning: not guaranteed to match MetaAABB's converted collision box!
     */
    @CheckReturnValue
    @Deprecated
    default AxisAlignedBB shadow2real(AxisAlignedBB shadowBox) {
        Vec3 min = SpaceUtil.getMin(shadowBox);
        Vec3 max = SpaceUtil.getMax(shadowBox);
        return SpaceUtil.newBoxSort(shadow2real(min), shadow2real(max));
    }

    /**
     * @param realBox Box to convert from real space to shadow space.
     * @return The shadow-space box. Warning: Not guaranteed to match MetaAABB's converted collision box!
     */
    @CheckReturnValue
    @Deprecated
    default AxisAlignedBB real2shadow(AxisAlignedBB realBox) {
        Vec3 min = SpaceUtil.getMin(realBox);
        Vec3 max = SpaceUtil.getMax(realBox);
        return SpaceUtil.newBoxSort(real2shadow(min), real2shadow(max));
    }

    /**
     * Helper method.
     * @param dir EnumFacing in shadow space
     * @return dir with rotation applied. Slight possibility for it to be the UNKNOWN direction.
     */
    @CheckReturnValue
    @Deprecated
    default EnumFacing shadow2real(EnumFacing dir) {
        Vec3 v = SpaceUtil.fromDirection(dir);
        getTransform().getRot().applyRotation(v);
        return SpaceUtil.round(v, null);
    }

    /**
     * Helper method.
     * @param dir EnumFacing in real space
     * @return dir with reverse-rotation applied. Slight possibility for it to be the UNKNOWN direction.
     */
    @CheckReturnValue
    @Deprecated
    default EnumFacing real2shadow(EnumFacing dir) {
        Vec3 v = SpaceUtil.fromDirection(dir);
        getTransform().getRot().applyReverseRotation(v);
        return SpaceUtil.round(v, null);
    }
    
    /**
     * @return the lower corner, in shadow coordinates.
     */
    Coord getMinCorner();

    /**
     * @return the center, in shadow coordinates. (Justs averages getCorner() and getFarCorner())
     */
    default Coord getCenter() {
        return getMinCorner().center(getMaxCorner());
    }

    /**
     * @return the upper corner, in shadow coordinates
     */
    Coord getMaxCorner();

    void setPartName(String name);


    /**
     * @return the object set by setController.
     */
    IDCController getController();

    /**
     * @param controller The controller responsible for this IDC. The IDC will not track this value across serializations; it is the controller's responsibility to set it.
     */
    void setController(IDCController controller);

    /**
     * Checks if the IDC's area is clear. If it is not clear, then the controller is
     * notified using {@link IDCController#collidedWithWorld(World, AxisAlignedBB, World, AxisAlignedBB)}.
     */
    void findAnyCollidingBox();

    /**
     * @return Return the entity that implements this interface.
     * Note that ent.posX/ent.motionX are blindly driven by the transform. It is best to use non-entity methods for controlling
     * the IDC whenever possible.
     * @see IDimensionSlice#killIDS()
     * @see IDimensionSlice#getTransform()
     * @see IDimensionSlice#getVel()
     */
    default DimensionSliceEntityBase getEntity() {
        return (DimensionSliceEntityBase) this;
    }

    default boolean isDead() {
        return getEntity().isDead;
    }

    void killIDS();

    NBTTagCompound getTag();
}
