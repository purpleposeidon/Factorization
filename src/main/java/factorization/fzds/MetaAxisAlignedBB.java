package factorization.fzds;

import java.util.List;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import factorization.api.Quaternion;
import factorization.fzds.api.IFzdsShenanigans;
import factorization.shared.Core;
import factorization.shared.FzUtil;

public class MetaAxisAlignedBB extends AxisAlignedBB implements IFzdsShenanigans {
    //NORELEASE, Optimization: If not rotated, check more vanilla-like, and mutate the parameter to test instead of shadowaabbs.
    World shadowWorld;
    AxisAlignedBB shadowAABB;
    Vec3 offset; //used to convert realspace to hammerspace
    Quaternion rotation;
    Vec3 shadow2rotationOrigin; //used to convert hammerspace to the local DSE's rotationspace
    
    public MetaAxisAlignedBB(World shadowWorld, AxisAlignedBB shadowAABB, Vec3 offset, Quaternion rotation, Vec3 shadow2rotationOrigin) {
        super(0, 0, 0, 0, 0, 0);
        this.shadowWorld = shadowWorld;
        this.shadowAABB = shadowAABB; 
        this.offset = offset;
        this.rotation = rotation;
        this.shadow2rotationOrigin = shadow2rotationOrigin;
    }
    
    public MetaAxisAlignedBB setUnderlying(AxisAlignedBB bb) {
        this.setBB(bb);
        return this;
    }
    
    static class AabbHolder extends Entity {
        public AabbHolder() {
            super(null);
        }
        AxisAlignedBB held = null;
        public AxisAlignedBB getBoundingBox() {
            return held;
        }
        
        @Override protected void entityInit() { }
        @Override protected void readEntityFromNBT(NBTTagCompound var1) { }
        @Override protected void writeEntityToNBT(NBTTagCompound var1) { }
    }
    
    AabbHolder aabbHolder = new AabbHolder();
    
    List<AxisAlignedBB> getUnderlying(AxisAlignedBB aabb) {
        double padding = 0;
        aabbHolder.held = aabb.expand(padding, padding, padding);
        return shadowWorld.getCollidingBoundingBoxes(aabbHolder, aabb);
    }
    
    private void shadow2rotation(Vec3 v) {
        //TODO: Move this to DSE
        v.xCoord -= shadow2rotationOrigin.xCoord;
        v.yCoord -= shadow2rotationOrigin.yCoord;
        v.zCoord -= shadow2rotationOrigin.zCoord;
    }
    
    private void rotation2shadow(Vec3 v) {
        //TODO: Move this to DSE
        v.xCoord += shadow2rotationOrigin.xCoord;
        v.yCoord += shadow2rotationOrigin.yCoord;
        v.zCoord += shadow2rotationOrigin.zCoord;
    }
    
    private AxisAlignedBB AabbReal2Shadow(AxisAlignedBB real) {
        //return real.getOffsetBoundingBox(-offset.xCoord, -offset.yCoord, -offset.zCoord);
        /*
         * Given: AABB in real space
         * Return: AABB in hammer space, with rotations applied
         * Real space -> hammer space -> rotation space; rotate -> un-rotation space == hammer space
         */
        AxisAlignedBB shadow = real.getOffsetBoundingBox(-offset.xCoord, -offset.yCoord, -offset.zCoord);
        Vec3 shadowMax = FzUtil.getMax(shadow);
        Vec3 shadowMin = FzUtil.getMin(shadow);
        Vec3 centerInShadowSpace = FzUtil.averageVec(shadowMax, shadowMin);
        Vec3 center = centerInShadowSpace.addVector(0, 0, 0);
        shadow2rotation(center);
        rotation.conjugate().applyRotation(center); //rotation.applyRotation(center);
        rotation2shadow(center);
        Vec3 diff = centerInShadowSpace.subtract(center); // NO LONGER client-side only; praise be to notch
        AxisAlignedBB moved = shadow.getOffsetBoundingBox(diff.xCoord, diff.yCoord, diff.zCoord);
//		moved.minX = round(moved.minX);
//		moved.minY = round(moved.minY);
//		moved.minZ = round(moved.minZ);
//		moved.maxX = round(moved.maxX);
//		moved.maxY = round(moved.maxY);
//		moved.maxZ = round(moved.maxZ);
        return moved;
    }
    
    double round(double val) {
        double r = 16;
        val = val * r;
        val = val - Math.rint(r);
        val = val / r;
        return val;
    }
    
    private AxisAlignedBB mutateLocal(AxisAlignedBB bb) {
        return bb;
    }
    
    private AxisAlignedBB mutateForeign(AxisAlignedBB bb) {
        return AabbReal2Shadow(bb);
    }
    
    
    /*
     * The three functions are decomposed here:
     *  - Vec3 offset = rotateOffset(XcurrentOffset, YcurrentOffset, ZcurrentOffset)
     *  - bbs.get(i).calculate_AXIS_Offset
     * 
     * *NOTE* currentOffset is the length of a vector aligned to the relevant axis in **real world space**.
     * So, we just need to rotate the vector and use that to adjust the peek-area.
     */
    @Override
    public double calculateXOffset(AxisAlignedBB collider, double currentOffset) {
        AxisAlignedBB shadow_version = mutateForeign(collider);
        Vec3 offset = rotateOffset(currentOffset, 0, 0);
        List<AxisAlignedBB> bbs = getUnderlying(shadow_version.addCoord(offset.xCoord, offset.yCoord, offset.zCoord));
        for (int i = 0; i < bbs.size(); i++) {
            AxisAlignedBB here = mutateLocal(bbs.get(i));
            currentOffset = here.calculateXOffset(shadow_version, currentOffset);
        }
        return currentOffset;
    }
    
    @Override
    public double calculateYOffset(AxisAlignedBB collider, double currentOffset) {
        AxisAlignedBB shadow_version = mutateForeign(collider);
        Vec3 offset = rotateOffset(0, currentOffset, 0);
        List<AxisAlignedBB> bbs = getUnderlying(shadow_version.addCoord(offset.xCoord, offset.yCoord, offset.zCoord));
        for (int i = 0; i < bbs.size(); i++) {
            AxisAlignedBB here = mutateLocal(bbs.get(i));
            currentOffset = here.calculateYOffset(shadow_version, currentOffset);
        }
        return currentOffset;
    }
    
    @Override
    public double calculateZOffset(AxisAlignedBB collider, double currentOffset) {
        AxisAlignedBB shadow_version = mutateForeign(collider);
        Vec3 offset = rotateOffset(0, 0, currentOffset);
        List<AxisAlignedBB> bbs = getUnderlying(shadow_version.addCoord(offset.xCoord, offset.yCoord, offset.zCoord));
        for (int i = 0; i < bbs.size(); i++) {
            AxisAlignedBB here = mutateLocal(bbs.get(i));
            currentOffset = here.calculateZOffset(shadow_version, currentOffset);
        }
        return currentOffset;
    }
    
    private Vec3 _currentOffset_vector = Vec3.createVectorHelper(0, 0, 0);
    Vec3 rotateOffset(double x, double y, double z) {
        _currentOffset_vector.xCoord = x;
        _currentOffset_vector.yCoord = y;
        _currentOffset_vector.zCoord = z;
        rotation.applyRotation(_currentOffset_vector);
        return _currentOffset_vector;
    }
    
    @Override
    public boolean intersectsWith(AxisAlignedBB collider) {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) {
            //System.out.println("NORELEASE");
            return false; //NORELEASE
        }
        double d = 2;
        AxisAlignedBB shadow_version = mutateForeign(collider.expand(d, d, d));
        List<AxisAlignedBB> bbs = getUnderlying(shadow_version);
        for (int i = 0; i < bbs.size(); i++) {
            AxisAlignedBB here = mutateLocal(bbs.get(i));
            if (here.intersectsWith(shadow_version)) {
                return true;
            }
        }
        return false;
    }

}
