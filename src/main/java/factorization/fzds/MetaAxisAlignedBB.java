package factorization.fzds;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import factorization.fzds.interfaces.IFzdsShenanigans;
import factorization.shared.Core;
import factorization.shared.FzUtil;
import factorization.shared.NORELEASE;

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
    private static final List<AxisAlignedBB> EMPTY = new ArrayList<AxisAlignedBB>();
    
    List<AxisAlignedBB> getShadowBoxesWithinShadowBox(AxisAlignedBB aabb) {
        aabbHolder.held = aabb; //.expand(padding, padding, padding);
        if (aabb.getAverageEdgeLength() > 1024) {
            Core.logSevere("Giant MetaAABB!? {}", this);
            return EMPTY;
        }
        return shadowWorld.getCollidingBoundingBoxes(aabbHolder, aabb);
    }
    
    List<AxisAlignedBB> getShadowBoxesInRealBox(AxisAlignedBB realBox) {
        double expansion = 0.3660254037844387;
        // It is important that expansion be the right value.
        // If it is too small, then collisions will be incorrect when rotated.
        // If it is too big, then there will be significant lag with e.g. splash potions.
        // Currently using width = 1.
        // (Which is cheating. It should be 2 due to fences, but I think this is a good tradeoff)
        
        // Here's how this number is derived 
        // Take our max-sized cube.
        // Calculate the distance between the center of the cube and a corner.
        // If we move that cube to the origin and rotate it so that the corner pokes out as far as it can,
        // this is equivalent to laying the corner_radius down flat, and the amount that it pokes over the cube's
        // area is what our expansion should be.
        // corner_radius = sqrt(3 * (width/2)**2)
        // expansion = corner_radius + 1 - 0.5
        
        // Optimization: make the expansion depend on the rotation; so the expansion would
        // range from 0, at no rotation, to <whatever the maximum should be> at the most extreme angles.
        // Could probably be done as a simpleish function depending on rotationQuaternion.w
        AxisAlignedBB shadowBox = convertRealBoxToShadowBox(realBox);
        if (!idc.getRotation().isZero()) {
            shadowBox = shadowBox.expand(expansion, expansion, expansion); NORELEASE.fixme(/* cache */);
        }
        return getShadowBoxesWithinShadowBox(shadowBox);
    }
    
    AxisAlignedBB convertRealBoxToShadowBox(AxisAlignedBB realBox) {
        // This function returns a box is likely larger than what it should really be.
        // A more accurate algo would be to translate each corner and make a box that contains them.
        Vec3 realMiddle = Vec3.createVectorHelper(0, 0, 0); NORELEASE.fixme(/* Can be cached */);
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
    private AxisAlignedBB shadowWorker = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
    private Vec3 shadowMiddle = Vec3.createVectorHelper(0, 0, 0);
    AxisAlignedBB convertShadowBoxToRealBox(AxisAlignedBB shadowBox) {
        // We're gonna try a different approach here.
        // Will work well so long as everything is a cube.
        FzUtil.setMiddle(shadowBox, shadowMiddle);
        FzUtil.getMin(shadowBox, minMinusMiddle);
        FzUtil.getMax(shadowBox, maxMinusMiddle);
        FzUtil.incrSubtract(minMinusMiddle, shadowMiddle);
        FzUtil.incrSubtract(maxMinusMiddle, shadowMiddle);
        Vec3 realMiddle = convertShadowVecToRealVec(shadowMiddle);
        FzUtil.incrAdd(minMinusMiddle, realMiddle);
        FzUtil.incrAdd(maxMinusMiddle, realMiddle);
        FzUtil.updateAABB(shadowWorker, minMinusMiddle, maxMinusMiddle);
        return shadowWorker;
    }
    
    Vec3 convertRealVecToShadowVec(Vec3 real) {
        return idc.real2shadow(real);
    }
    
    Vec3 convertShadowVecToRealVec(Vec3 shadow) {
        return idc.shadow2real(shadow);
    }
    
    private final AxisAlignedBB worker = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
    private AxisAlignedBB expand(AxisAlignedBB collider, double dx, double dy, double dz) {
        if (dx >= 0) {
            worker.minX = collider.minX;
            worker.maxX = collider.maxX + dx;
        } else {
            worker.minX = collider.minX + dx /* subtract! */;
            worker.maxX = collider.maxX;
        }
        if (dy >= 0) {
            worker.minY = collider.minY;
            worker.maxY = collider.maxY + dy;
        } else {
            worker.minY = collider.minY + dy /* subtract! */;
            worker.maxY = collider.maxY;
        }
        if (dz >= 0) {
            worker.minZ = collider.minZ;
            worker.maxZ = collider.maxZ + dz;
        } else {
            worker.minZ = collider.minZ + dz /* subtract! */;
            worker.maxZ = collider.maxZ;
        }
        return worker;
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
        List<AxisAlignedBB> shadowBoxes = getShadowBoxesInRealBox(expand(collider, currentOffset, 0, 0));
        for (AxisAlignedBB shadowBox : shadowBoxes) {
            AxisAlignedBB realShadow = convertShadowBoxToRealBox(shadowBox);
            currentOffset = realShadow.calculateXOffset(collider, currentOffset);
        }
        return currentOffset;
    }
    
    @Override
    public double calculateYOffset(AxisAlignedBB collider, double currentOffset) {
        collider = collider.copy();
        List<AxisAlignedBB> shadowBoxes = getShadowBoxesInRealBox(expand(collider, 0, currentOffset, 0));
        for (AxisAlignedBB shadowBox : shadowBoxes) {
            AxisAlignedBB realShadow = convertShadowBoxToRealBox(shadowBox);
            currentOffset = realShadow.calculateYOffset(collider, currentOffset);
        }
        return currentOffset;
    }
    
    @Override
    public double calculateZOffset(AxisAlignedBB collider, double currentOffset) {
        collider = collider.copy();
        List<AxisAlignedBB> shadowBoxes = getShadowBoxesInRealBox(expand(collider, 0, 0, currentOffset));
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
