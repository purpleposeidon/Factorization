package factorization.weird.poster;

import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.shared.Core;
import factorization.shared.ItemFactorization;
import factorization.util.SpaceUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;

public class ItemSpawnPoster extends ItemFactorization {
    public ItemSpawnPoster() {
        super("spawnPoster", Core.TabType.ART);
    }

    @Override
    public boolean onItemUse(ItemStack is, EntityPlayer player, World w, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (w.isRemote) return false;
        // Bit long. Sorry. Could create a class to do this cleaner but, to quote the wiseman John Carmack, "sod that."

        final Coord at = new Coord(w, x, y, z);
        AxisAlignedBB blockBox = null;

        {
            // Determine what the box should be. Ray tracing for multi-box blocks; fallbacks to the selection bounding box.

            final ArrayList<AxisAlignedBB> boxes = new ArrayList<AxisAlignedBB>();
            final AxisAlignedBB query = SpaceUtil.createAABB(at.add(-9, -9, -9), at.add(+9, +9, +9));
            at.getBlock().addCollisionBoxesToList(at.w, at.x, at.y, at.z, query, boxes, player);

            final Vec3 playerEye = SpaceUtil.fromPlayerEyePos(player);
            final Vec3 look = player.getLookVec();
            SpaceUtil.incrScale(look, 8);
            final Vec3 reachEnd = SpaceUtil.add(look, playerEye);

            double minDist = Double.POSITIVE_INFINITY;
            for (AxisAlignedBB box : boxes) {
                MovingObjectPosition mop = box.calculateIntercept(playerEye, reachEnd);
                if (mop == null) continue;
                if (mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) continue;
                if (mop.hitVec == null) continue;
                double vecLen = mop.hitVec.lengthVector();
                if (vecLen > minDist) continue;
                minDist = vecLen;
                side = mop.sideHit;
                blockBox = box;
            }

            if (blockBox == null) blockBox = at.getCollisionBoundingBoxFromPool();
            if (blockBox == null) blockBox = at.getSelectedBoundingBoxFromPool();
            if (blockBox == null) return false;
        }

        final ForgeDirection dir = ForgeDirection.getOrientation(side);
        double bestWidth;
        AxisAlignedBB bounds, plane;
        {
            // Setup box areas
            plane = SpaceUtil.flatten(blockBox, dir);

            final double pix = 1.0 / 16.0;
            bounds = plane.addCoord(dir.offsetX * pix, dir.offsetY * pix, dir.offsetZ * pix);

            for (Object ent : w.getEntitiesWithinAABB(EntityPoster.class, bounds)) {
                //new Notice(ent, "Already occupied").sendTo(player);
                return false;
            }
            final double xwidth = plane.maxX - plane.minX;
            final double ywidth = plane.maxY - plane.minY;
            final double zwidth = plane.maxZ - plane.minZ;
            bestWidth = SpaceUtil.getDiagonalLength(plane);

            if (xwidth != 0) bestWidth = xwidth;
            if (ywidth != 0 && ywidth < bestWidth) bestWidth = ywidth;
            if (zwidth != 0 && zwidth < bestWidth) bestWidth = zwidth;
            if (bestWidth <= 2.0 / 16.0) return false; // Don't be ridiculous
        }

        Quaternion rot;
        ForgeDirection top;
        {
            // Determine rotations & orientations
            double rotationAngle = 0;
            if (dir.offsetY == 0) {
                top = ForgeDirection.UP;
                if (dir == ForgeDirection.WEST) rotationAngle = 1;
                if (dir == ForgeDirection.SOUTH) rotationAngle = 2;
                if (dir == ForgeDirection.EAST) rotationAngle = 3;
            } else {
                top = ForgeDirection.WEST;
                rotationAngle = -dir.offsetY;
            }
            rot = Quaternion.getRotationQuaternionRadians(rotationAngle * Math.PI / 2, top);
        }

        // Create the thing
        EntityPoster poster = new EntityPoster(w);
        poster.setBase(bestWidth, rot, dir, top, bounds);
        final Vec3 spot = SpaceUtil.getMiddle(plane);
        if (SpaceUtil.sign(dir) == -1) {
            SpaceUtil.incrAdd(spot, SpaceUtil.scale(SpaceUtil.fromDirection(dir), 1.0 / 2560.0));
        }
        SpaceUtil.toEntPos(poster, spot);
        w.spawnEntityInWorld(poster);

        if (!player.capabilities.isCreativeMode) {
            is.stackSize--;
        }

        return true;
    }
}
