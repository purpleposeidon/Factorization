package factorization.fzds;

import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import factorization.fzds.api.IDeltaChunk;
import factorization.fzds.api.IFzdsShenanigans;
import factorization.notify.RenderMessages;
import factorization.shared.FzUtil;

public class MetaAxisAlignedBB extends AxisAlignedBB implements IFzdsShenanigans {
    // NORELEASE: Optimize & cache
    /*
     * So, there's 2 operations: calculate<Axis>Offset and intersectsWith.
     * For each operation, we need to gather a list of relevant AABBs in hammer space and apply the operation to each of them.
     * 1. Convert the argument AABB to an AABB in shadow space:
     * 		calculate the length of the diagonal
     * 		Calculate the center, as a vector. Transform the vector.
     * 		Build a new AABB using the transformed center; it will be a cube with the min/max just center ± diagonal/2
     * 		Faster for simpler cases, tho slower if the unnecessarily larger area
     * 
     * 		alternative method:
     * 			Convert input AABB to 8 points, convert them all to shadow space, make AABB out of the maximum
     * 			Too expensive!
     * 				
     * 2. We now have a list of AABBs in shadow space, and the passed in AABB in real world space.
     * 3. Iterate over the AABBs, converting each one to real space & applying the operation
     */
    private DimensionSliceEntity idc;
    private World shadowWorld;
    
    public MetaAxisAlignedBB(DimensionSliceEntity idc, World shadowWorld) {
        super(0, 0, 0, 0, 0, 0);
        this.idc = idc;
        this.shadowWorld = shadowWorld;
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
    
    List<AxisAlignedBB> getShadowBoxesWithinShadowBox(AxisAlignedBB aabb) {
        aabbHolder.held = aabb; //.expand(padding, padding, padding);
        return shadowWorld.getCollidingBoundingBoxes(aabbHolder, aabb);
    }
    
    List<AxisAlignedBB> getShadowBoxesInRealBox(AxisAlignedBB realBox) {
        double expansion = 2.45;
        // The worst case would be a 1x2x1 collision box rotated 45+90°,45°,0° at the top.
        // (MC hard-codes the max BB size to 1x2x1, instead of 1x1x1, for fences.)
        // So if the expansion is too small, then the player might not collide with blocks
        // that 'e should collide -- and does, in fact, collide at 0° rotation -- 
        // because this expanded area does not contain the block that would need checking.
        
        // Optimization: make the expansion depend on the rotation; so the expansion would
        // range from 0, at no rotation, to <whatever the maximum should be> at the most extreme angles.
        // Could even enlarge the realBox differently on only certain sides maybe?
        double cx = realBox.maxX - realBox.minX;
        double cy = realBox.maxY - realBox.minY;
        double cz = realBox.maxZ - realBox.minZ;
        System.out.println("RealBox: " + (cx*cy*cz) + " = " + cx + ", " + cy + ", " + cz);
        AxisAlignedBB useBox = realBox;
        if (!idc.getRotation().isZero()) {
            useBox = realBox.expand(expansion, expansion, expansion);
        }
        double bx = useBox.maxX - useBox.minX;
        double by = useBox.maxY - useBox.minY;
        double bz = useBox.maxZ - useBox.minZ;
        System.out.println("useBox: " + (bx * by * bz) + " = " + bx + ", " + by + ", " + bz);
        
        AxisAlignedBB shadowBox = convertRealBoxToShadowBox(useBox);
        double dx = shadowBox.maxX - shadowBox.minX;
        double dy = shadowBox.maxY - shadowBox.minY;
        double dz = shadowBox.maxZ - shadowBox.minZ;
        System.out.println("ShadowBoxArea: " + (dx*dy*dz) + " = " + dx + ", " + dy + ", " + dz);
        return getShadowBoxesWithinShadowBox(shadowBox);
    }
    
    AxisAlignedBB convertRealBoxToShadowBox(AxisAlignedBB realBox) {
        // This function returns a box is likely larger than what it should really be.
        // A more accurate algo would be to translate each corner and make a box that contains them.
        Vec3 realMiddle = Vec3.createVectorHelper(0, 0, 0);
        FzUtil.setMiddle(realBox, realMiddle);
        double d = FzUtil.getDiagonalLength(realBox);
        Vec3 shadowMiddle = convertRealVecToShadowVec(realMiddle);
        return AxisAlignedBB.getBoundingBox(
                shadowMiddle.xCoord - d,
                shadowMiddle.yCoord - d,
                shadowMiddle.zCoord - d,
                shadowMiddle.xCoord + d,
                shadowMiddle.yCoord + d,
                shadowMiddle.zCoord + d);
    }
    
    private Vec3 minMinusMiddle = Vec3.createVectorHelper(0, 0, 0);
    private Vec3 maxMinusMiddle = Vec3.createVectorHelper(0, 0, 0);
    AxisAlignedBB convertShadowBoxToRealBox(AxisAlignedBB shadowBox) {
        // We're gonna try a different approach here.
        // Will work well so long as everything is a cube.
        Vec3 shadowMiddle = Vec3.createVectorHelper(0, 0, 0);
        FzUtil.setMiddle(shadowBox, shadowMiddle);
        FzUtil.getMin(shadowBox, minMinusMiddle);
        FzUtil.getMax(shadowBox, maxMinusMiddle);
        FzUtil.incrSubtract(minMinusMiddle, shadowMiddle);
        FzUtil.incrSubtract(maxMinusMiddle, shadowMiddle);
        Vec3 realMiddle = convertShadowVecToRealVec(shadowMiddle);
        FzUtil.incrAdd(minMinusMiddle, realMiddle);
        FzUtil.incrAdd(maxMinusMiddle, realMiddle);
        return FzUtil.createAABB(minMinusMiddle, maxMinusMiddle);
    }
    
    Vec3 convertRealVecToShadowVec(Vec3 real) {
        return idc.real2shadow(real);
    }
    
    Vec3 convertShadowVecToRealVec(Vec3 shadow) {
        return idc.shadow2real(shadow);
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
        collider = collider.copy();
        List<AxisAlignedBB> shadowBoxes = getShadowBoxesInRealBox(collider.expand(1, 0, 0));
        for (AxisAlignedBB shadowBox : shadowBoxes) {
            AxisAlignedBB realShadow = convertShadowBoxToRealBox(shadowBox);
            currentOffset = realShadow.calculateXOffset(collider, currentOffset);
        }
        return currentOffset;
    }
    
    @Override
    public double calculateYOffset(AxisAlignedBB collider, double currentOffset) {
        collider = collider.copy();
        List<AxisAlignedBB> shadowBoxes = getShadowBoxesInRealBox(collider.expand(0, 1, 0));
        for (AxisAlignedBB shadowBox : shadowBoxes) {
            AxisAlignedBB realShadow = convertShadowBoxToRealBox(shadowBox);
            currentOffset = realShadow.calculateYOffset(collider, currentOffset);
        }
        return currentOffset;
    }
    
    @Override
    public double calculateZOffset(AxisAlignedBB collider, double currentOffset) {
        collider = collider.copy();
        List<AxisAlignedBB> shadowBoxes = getShadowBoxesInRealBox(collider.expand(0, 0, 1));
        for (AxisAlignedBB shadowBox : shadowBoxes) {
            AxisAlignedBB realShadow = convertShadowBoxToRealBox(shadowBox);
            currentOffset = realShadow.calculateZOffset(collider, currentOffset);
        }
        return currentOffset;
    }
    
    
    
    
    
    @Override
    public boolean intersectsWith(AxisAlignedBB collider) {
        if (!idc.realArea.intersectsWith(collider)) return false;
        List<AxisAlignedBB> shadowBoxes = getShadowBoxesInRealBox(collider);
        for (AxisAlignedBB shadowBox : shadowBoxes) {
            AxisAlignedBB realShadow = convertShadowBoxToRealBox(shadowBox);
            if (realShadow.intersectsWith(collider)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String toString() {
        return "META" + super.toString();
    }

}
