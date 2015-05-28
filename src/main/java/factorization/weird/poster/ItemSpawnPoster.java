package factorization.weird.poster;

import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.notify.Notice;
import factorization.shared.Core;
import factorization.shared.ItemFactorization;
import factorization.util.SpaceUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class ItemSpawnPoster extends ItemFactorization {
    public ItemSpawnPoster() {
        super("spawnPoster", Core.TabType.ART);
    }

    @Override
    public boolean onItemUse(ItemStack is, EntityPlayer player, World w, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (w.isRemote) return false;
        Coord at = new Coord(w, x, y, z);
        ForgeDirection dir = ForgeDirection.getOrientation(side);


        AxisAlignedBB box = at.getCollisionBoundingBoxFromPool();
        if (box == null) {
            box = at.getSelectedBoundingBoxFromPool();
            if (box == null) {
                return true;
            }
        }
        AxisAlignedBB plane = SpaceUtil.flatten(box, dir);

        double d = 1.0 / 16.0;
        AxisAlignedBB bounds = plane.addCoord(dir.offsetX * d, dir.offsetY * d, dir.offsetZ * d);

        for (Object ent : w.getEntitiesWithinAABB(EntityPoster.class, bounds)) {
            //new Notice(ent, "Already occupied").sendTo(player);
            return false;
        }


        ForgeDirection top;
        double r = 0;
        if (dir.offsetY == 0) {
            top = ForgeDirection.UP;
            if (dir == ForgeDirection.WEST) r = 1;
            if (dir == ForgeDirection.SOUTH) r = 2;
            if (dir == ForgeDirection.EAST) r = 3;
        } else {
            top = ForgeDirection.WEST;
            r = -dir.offsetY;
        }
        Quaternion rot = Quaternion.getRotationQuaternionRadians(r * Math.PI / 2, top);

        double bestWidth = SpaceUtil.getDiagonalLength(plane);
        double xwidth = plane.maxX - plane.minX;
        double ywidth = plane.maxY - plane.minY;
        double zwidth = plane.maxZ - plane.minZ;

        if (xwidth != 0) bestWidth = xwidth;
        if (ywidth != 0 && ywidth < bestWidth) bestWidth = ywidth;
        if (zwidth != 0 && zwidth < bestWidth) bestWidth = zwidth;

        EntityPoster poster = new EntityPoster(w);
        poster.setBase(bestWidth, rot, dir, top, bounds);
        final Vec3 spot = SpaceUtil.getMiddle(plane);
        if (SpaceUtil.sign(dir) == -1) {
            SpaceUtil.incrAdd(spot, SpaceUtil.scale(SpaceUtil.fromDirection(dir), 1.0 / 2560.0));
        }
        SpaceUtil.toEntPos(poster, spot);
        w.spawnEntityInWorld(poster);

        return true;
    }
}
