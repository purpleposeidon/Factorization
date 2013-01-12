package factorization.fzds;

import java.util.List;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MetaAxisAlignedBB extends AxisAlignedBB {
    World shadowWorld;
    AxisAlignedBB shadowAABB;
    Vec3 offset;
    
    public MetaAxisAlignedBB(World shadowWorld, AxisAlignedBB shadowAABB, Vec3 offset) {
        super(0, 0, 0, 0, 0, 0);
        this.shadowWorld = shadowWorld;
        this.shadowAABB = shadowAABB; 
        this.offset = offset;
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
    
    AxisAlignedBB AabbReal2Shadow(AxisAlignedBB real) {
        return real.getOffsetBoundingBox(-offset.xCoord, -offset.yCoord, -offset.zCoord);
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
