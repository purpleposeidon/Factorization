package factorization.fzds;

import java.util.List;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import factorization.api.Quaternion;

public class MetaAxisAlignedBB extends AxisAlignedBB {
    World shadowWorld;
    AxisAlignedBB shadowAABB;
    Vec3 offset;
    Quaternion rotation;
    
    public MetaAxisAlignedBB(World shadowWorld, AxisAlignedBB shadowAABB, Vec3 offset, Quaternion rotation) {
        super(0, 0, 0, 0, 0, 0);
        this.shadowWorld = shadowWorld;
        this.shadowAABB = shadowAABB; 
        this.offset = offset;
        this.rotation = rotation;
    }
    
    public MetaAxisAlignedBB setUnderlying(AxisAlignedBB bb) {
        this.setBB(bb);
        return this;
    }
    
    static boolean intersect(double la, double ha, double lb, double hb) {
        //If we don't intersect, then we're overlapping.
        //If we're not overlapping, then one is to the right of the other.
        //<--  (la ha) -- (lb hb) -->
        //<--- (lb hb) -- (la ha) -->
        return !(ha < lb || hb < la);
    }
    
    List<AxisAlignedBB> getUnderlying(AxisAlignedBB aabb) {
        return shadowWorld.getAllCollidingBoundingBoxes(aabb);
    }
    
    private AxisAlignedBB biggerBox = AxisAlignedBB.getBoundingBox(0, 0, 0, 1, 1, 1); //could be thread-locale
    
    private AxisAlignedBB AabbReal2Shadow(AxisAlignedBB real) {
        //return real.getOffsetBoundingBox(-offset.xCoord, -offset.yCoord, -offset.zCoord);
        AxisAlignedBB o = real.getOffsetBoundingBox(-offset.xCoord, -offset.yCoord, -offset.zCoord);
        return o;
        /*
        Vec3[] vecs = RotatedBB.getAabbVertices(o);
        biggerBox.minX = biggerBox.maxX = vecs[0].xCoord;
        biggerBox.minY = biggerBox.maxY = vecs[0].yCoord;
        biggerBox.minZ = biggerBox.maxZ = vecs[0].zCoord;
        float d = 1F/32F; //TODO: Dynamically adjust this based on angle or something
        d = 0; //NORELEASE
        for (int i = 0; i < vecs.length; i++) {
            Vec3 v = vecs[i];
            rotation.rotateIncr(v);
            //TODO NORELEASE rotate silly!
            biggerBox.minX = Math.min(biggerBox.minX, v.xCoord - d);
            biggerBox.maxX = Math.max(biggerBox.maxX, v.xCoord + d);
            biggerBox.minY = Math.min(biggerBox.minY, v.yCoord - d);
            biggerBox.maxY = Math.max(biggerBox.maxY, v.yCoord + d);
            biggerBox.minZ = Math.min(biggerBox.minZ, v.zCoord - d);
            biggerBox.maxZ = Math.max(biggerBox.maxZ, v.zCoord + d);
        }
        //System.out.println((biggerBox.maxX - biggerBox.minX) + " " + (biggerBox.maxY - biggerBox.minY) + " " + (biggerBox.maxZ - biggerBox.minZ));
        if (biggerBox.minX >= biggerBox.maxX || biggerBox.minY >= biggerBox.maxY || biggerBox.minZ >= biggerBox.maxZ) {
            System.out.println("Error!"); //NORELEASE
        }
//		if (!biggerBox.toString().equals(o.toString())) { //NORELEASE
//			System.out.println("achoo");
//		}
        return biggerBox;*/
    }
    
    
    //The three functions below differ in these ways:
    //shadow_version.addCoord(XcurrentOffset, YcurrentOffset, ZcurrentOffset)
    //bbs.get(i).calculate_AXIS_Offset
    @Override
    public double calculateXOffset(AxisAlignedBB collider, double currentOffset) {
        AxisAlignedBB shadow_version = AabbReal2Shadow(collider);
        List<AxisAlignedBB> bbs = getUnderlying(shadow_version.addCoord(currentOffset, 0, 0));
        for (int i = 0; i < bbs.size(); i++) {
            currentOffset = bbs.get(i).calculateXOffset(shadow_version, currentOffset);
        }
        return currentOffset;
    }
    
    @Override
    public double calculateYOffset(AxisAlignedBB collider, double currentOffset) {
        AxisAlignedBB shadow_version = AabbReal2Shadow(collider);
        List<AxisAlignedBB> bbs = getUnderlying(shadow_version.addCoord(0, currentOffset, 0));
        for (int i = 0; i < bbs.size(); i++) {
            AxisAlignedBB here = bbs.get(i);
            currentOffset = here.calculateYOffset(shadow_version, currentOffset);
        }
        return currentOffset;
    }
    
    @Override
    public double calculateZOffset(AxisAlignedBB collider, double currentOffset) {
        AxisAlignedBB shadow_version = AabbReal2Shadow(collider);
        List<AxisAlignedBB> bbs = getUnderlying(shadow_version.addCoord(0, 0, currentOffset));
        for (int i = 0; i < bbs.size(); i++) {
            currentOffset = bbs.get(i).calculateZOffset(shadow_version, currentOffset);
        }
        return currentOffset;
    }
    
    @Override
    public boolean intersectsWith(AxisAlignedBB collider) {
        AxisAlignedBB shadow_version = AabbReal2Shadow(collider);
        List<AxisAlignedBB> bbs = getUnderlying(shadow_version);
        for (int i = 0; i < bbs.size(); i++) {
            if (bbs.get(i).intersectsWith(shadow_version)) {
                return true;
            }
        }
        return false;
    }

}
