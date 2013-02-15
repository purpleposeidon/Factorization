package factorization.fzds;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Quaternion;

public class RotatedBB extends AxisAlignedBB {
    protected RotatedBB() {
        super(0, 0, 0, 1, 1, 1);
    }

    Quaternion rotation;
    AxisAlignedBB basis;
    
    public void set(Quaternion rotation, AxisAlignedBB basis) {
        this.rotation = rotation;
        this.basis = basis;
    }
    
    @Override
    public double calculateXOffset(AxisAlignedBB collider, double deltaX) {
        return super.calculateXOffset(collider, deltaX);
    }
    
    @Override
    public double calculateYOffset(AxisAlignedBB collider, double deltaY) {
        return super.calculateYOffset(collider, deltaY);
    }
    
    @Override
    public double calculateZOffset(AxisAlignedBB collider, double deltaZ) {
        return super.calculateZOffset(collider, deltaZ);
    }
    
    ForgeDirection[] axes = new ForgeDirection[] {ForgeDirection.UP, ForgeDirection.SOUTH, ForgeDirection.EAST};
    
    @Override
    public boolean intersectsWith(AxisAlignedBB other) {
        /**
         * The theory is this: If there exists an axis of any angle where in the
         * two shapes are projected onto and do not intersect, then they do not collide.
         * 
         * For this case, we must look at 12 axes: the global X, Y, and Z (corresponding to @param other's axis),
         * and the X, Y, and Z axis with our rotation.
         */
        for (int i = 0; i < axes.length; i++) {
            ForgeDirection dir = axes[i];
            //project ourselves to global XYZ
            Range thisConverted = project(dir);
            Range otherNative = getUnalteredRange(other, dir);
            if (!thisConverted.intersects(otherNative)) {
                return false;
            }
        }
        for (int i = 0; i < axes.length; i++) {
            ForgeDirection dir = axes[i];
            //project the other's onto our local XYZ
            Range otherConverted = project(other, dir, rotation);
            Range thisNative = getUnalteredRange(this.basis, dir);
            if (!otherConverted.intersects(thisNative)) {
                return false;
            }
        }
        return true;
    }
    
    private static ThreadLocal<Vec3[]> vertexBuffer = new ThreadLocal<Vec3[]>() { protected Vec3[] initialValue() {
        Vec3[] ret = new Vec3[8];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = Vec3.createVectorHelper(0, 0, 0);
        }
        return ret;
    } };
    
    private static void set(Vec3 v, double x, double y, double z) {
        v.xCoord = x;
        v.yCoord = y;
        v.zCoord = z;
    }
    
    private static void yoink(Vec3[] ret, int i, int X, int Y, int Z) {
        set(ret[i], ret[X].xCoord, ret[Y].yCoord, ret[Z].zCoord);
    }
    
    /* Gets the vertices of a vanilla AABB. */
    static Vec3[] getAabbVertices(AxisAlignedBB ab) {
        Vec3 ret[] = vertexBuffer.get();
        set(ret[0], ab.minX, ab.minY, ab.minZ);
        set(ret[1], ab.maxX, ab.maxY, ab.maxZ);
        
        //ret[0] = 0, 0, 0
        //ret[1] = 1, 1, 1
        yoink(ret, 2, 1, 0, 0);
        yoink(ret, 3, 0, 1, 0);
        yoink(ret, 4, 0, 0, 1);
        yoink(ret, 5, 0, 1, 1);
        yoink(ret, 6, 1, 0, 1);
        yoink(ret, 7, 1, 1, 0);
        return ret;
    }
    
    /* Return the shadow cast by this RotatedBB onto the axis defined by dir */
    private Range project(ForgeDirection dir) {
        Range range = new Range();
        Vec3 verts[] = getAabbVertices(this);
        for (int i = 0; i < verts.length; i++) {
            Vec3 v = verts[i];
            rotation.applyRotation(v);
            range.include(v, dir);
        }
        return range;
    }
    
    private static Range project(AxisAlignedBB bb, ForgeDirection dir, Quaternion rotation) {
        Range range = new Range();
        Vec3 verts[] = getAabbVertices(bb);
        rotation.incrConjugate();
        for (int i = 0; i < verts.length; i++) {
            Vec3 v = verts[i];
            rotation.applyRotation(v);
            range.include(v, dir);
        }
        rotation.incrConjugate();
        return range;
    }
    
    private static Range getUnalteredRange(AxisAlignedBB aabb, ForgeDirection dir) {
        switch (dir) {
        case EAST:
        case WEST:
            return new Range(aabb.minX, aabb.maxX);
        case UP:
        case DOWN:
            return new Range(aabb.minY, aabb.maxY);
        case NORTH:
        case SOUTH:
            return new Range(aabb.minZ, aabb.maxZ);
        }
        return null;
    }
}
