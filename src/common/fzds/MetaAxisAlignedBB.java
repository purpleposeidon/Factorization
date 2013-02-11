package factorization.fzds;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import factorization.api.Quaternion;

public class MetaAxisAlignedBB extends AxisAlignedBB {
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
    
    private AxisAlignedBB biggerBox = AxisAlignedBB.getBoundingBox(0, 0, 0, 1, 1, 1); //could be thread-locale NORELEASE still used?
    
    private Vec3 getMin(AxisAlignedBB aabb) {
        return Vec3.createVectorHelper(aabb.minX, aabb.minY, aabb.minZ);
    }
    
    private void setMin(AxisAlignedBB aabb, Vec3 v) {
        aabb.minX = v.xCoord;
        aabb.minY = v.yCoord;
        aabb.minZ = v.zCoord;
    }
    
    private Vec3 getMax(AxisAlignedBB aabb) {
        return Vec3.createVectorHelper(aabb.maxX, aabb.maxY, aabb.maxZ);
    }
    
    private void setMax(AxisAlignedBB aabb, Vec3 v) {
        aabb.maxX = v.xCoord;
        aabb.maxY = v.yCoord;
        aabb.maxZ = v.zCoord;
    }
    
    private Vec3 averageVec(Vec3 a, Vec3 b) {
        return Vec3.createVectorHelper((a.xCoord + b.xCoord)/2, (a.yCoord + b.yCoord)/2, (a.zCoord + b.zCoord)/2);
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
    
    private void markVector(Vec3 v, double r, double g, double b) { //NORELEASE
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) {
            //System.out.println("--> " + v);
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            player.worldObj.spawnParticle("reddust", v.xCoord, v.yCoord, v.zCoord, r - 1, g, b);
        }
    }
    
    
    
    private AxisAlignedBB AabbReal2Shadow(AxisAlignedBB real) {
        //return real.getOffsetBoundingBox(-offset.xCoord, -offset.yCoord, -offset.zCoord);
        /*
         * Given: AABB in real space
         * Return: AABB in hammer space, with rotations applied
         * Real space -> hammer space -> rotation space; rotate -> un-rotation space == hammer space
         */
        AxisAlignedBB shadow = real.getOffsetBoundingBox(-offset.xCoord, -offset.yCoord, -offset.zCoord);
        Vec3 shadowMax = getMax(shadow);
        Vec3 shadowMin = getMin(shadow);
        Vec3 centerInShadowSpace = averageVec(shadowMax, shadowMin);
        Vec3 center = centerInShadowSpace.addVector(0, 0, 0);
        shadow2rotation(center);
        rotation.rotateIncr(center);
        rotation2shadow(center);
        Vec3 diff = centerInShadowSpace.subtract(center);
        AxisAlignedBB moved = shadow.getOffsetBoundingBox(diff.xCoord, diff.yCoord, diff.zCoord);
        double d = 1.0/16.0;
        moved.minX -= d;
        moved.minY -= d;
        moved.minZ -= d;
        moved.maxX += d;
        moved.maxY += d;
        moved.maxZ += d;
        return moved;
    }
    
    private AxisAlignedBB mutateLocal(AxisAlignedBB bb) {
        return bb;
    }
    
    
    //The three functions below differ in these ways:
    //shadow_version.addCoord(XcurrentOffset, YcurrentOffset, ZcurrentOffset)
    //bbs.get(i).calculate_AXIS_Offset
    @Override
    public double calculateXOffset(AxisAlignedBB collider, double currentOffset) {
        AxisAlignedBB shadow_version = AabbReal2Shadow(collider);
        List<AxisAlignedBB> bbs = getUnderlying(shadow_version.addCoord(currentOffset, 0, 0));
        for (int i = 0; i < bbs.size(); i++) {
            AxisAlignedBB here = mutateLocal(bbs.get(i));
            currentOffset = here.calculateXOffset(shadow_version, currentOffset);
        }
        return currentOffset;
    }
    
    @Override
    public double calculateYOffset(AxisAlignedBB collider, double currentOffset) {
        AxisAlignedBB shadow_version = AabbReal2Shadow(collider);
        List<AxisAlignedBB> bbs = getUnderlying(shadow_version.addCoord(0, currentOffset, 0));
        for (int i = 0; i < bbs.size(); i++) {
            AxisAlignedBB here = mutateLocal(bbs.get(i));
            currentOffset = here.calculateYOffset(shadow_version, currentOffset);
        }
        return currentOffset;
    }
    
    @Override
    public double calculateZOffset(AxisAlignedBB collider, double currentOffset) {
        AxisAlignedBB shadow_version = AabbReal2Shadow(collider);
        List<AxisAlignedBB> bbs = getUnderlying(shadow_version.addCoord(0, 0, currentOffset));
        for (int i = 0; i < bbs.size(); i++) {
            AxisAlignedBB here = mutateLocal(bbs.get(i));
            currentOffset = here.calculateZOffset(shadow_version, currentOffset);
        }
        return currentOffset;
    }
    
    @Override
    public boolean intersectsWith(AxisAlignedBB collider) {
        AxisAlignedBB shadow_version = AabbReal2Shadow(collider);
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
