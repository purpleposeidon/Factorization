package factorization.weird.poster;

import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.shared.Core;
import factorization.shared.ItemFactorization;
import factorization.util.ItemUtil;
import factorization.util.SpaceUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.ArrayList;

public class ItemSpawnPoster extends ItemFactorization {
    public ItemSpawnPoster() {
        super("spawnPoster", Core.TabType.ART);
    }

    @Override
    public boolean onItemUse(ItemStack is, EntityPlayer player, World w, BlockPos pos, int side, float hitX, float hitY, float hitZ) {
        if (w.isRemote) return false;
        final PosterPlacer placer = new PosterPlacer(is, player, w, pos, side);
        if (placer.invoke()) return false;
        placer.spawn();
        return true;
    }

    public static class PosterPlacer {
        private boolean myResult;
        private ItemStack is;
        private EntityPlayer player;
        private World w;
        private int x;
        private int y;
        private int z;
        private int side;

        final EnumFacing dir;

        final Coord at;
        AxisAlignedBB blockBox = null;

        public EntityPoster result;

        double bestWidth;
        AxisAlignedBB bounds, plane;
        Quaternion rot;
        EnumFacing top;

        public PosterPlacer(ItemStack is, EntityPlayer player, World w, BlockPos pos, int side) {
            this.is = is;
            this.player = player;
            this.w = w;
            this.x = x;
            this.y = y;
            this.z = z;
            this.side = side;
            this.at = new Coord(w, pos);
            this.dir = SpaceUtil.getOrientation(side);
        }

        boolean is() {
            return myResult;
        }

        public boolean invoke() {
            if (determineBoundingBox()) return true;
            if (determineSize()) return true;
            determineOrientation();

            // Create the thing
            EntityPoster poster = new EntityPoster(w);
            poster.setBase(bestWidth, rot, dir, top, bounds);
            final Vec3 spot = SpaceUtil.getMiddle(plane);
            if (SpaceUtil.sign(dir) == -1) {
                SpaceUtil.incrAdd(spot, SpaceUtil.scale(SpaceUtil.fromDirection(dir), 1.0 / 2560.0));
            }
            SpaceUtil.toEntPos(poster, spot);
            result = poster;
            return false;
        }

        private void determineOrientation() {
            // Determine rotations & orientations
            double rotationAngle = 0;
            if (dir.getDirectionVec().getY() == 0) {
                top = EnumFacing.UP;
                if (dir == EnumFacing.WEST) rotationAngle = 1;
                if (dir == EnumFacing.SOUTH) rotationAngle = 2;
                if (dir == EnumFacing.EAST) rotationAngle = 3;
            } else {
                top = EnumFacing.WEST;
                rotationAngle = -dir.getDirectionVec().getY();
            }
            rot = Quaternion.getRotationQuaternionRadians(rotationAngle * Math.PI / 2, top);
        }

        private boolean determineSize() {
            // Setup box areas
            plane = SpaceUtil.flatten(blockBox, dir);

            final double pix = 1.0 / 16.0;
            bounds = plane.addCoord(dir.getDirectionVec().getX() * pix, dir.getDirectionVec().getY() * pix, dir.getDirectionVec().getZ() * pix);

            for (Object ent : w.getEntitiesWithinAABB(EntityPoster.class, bounds)) {
                if (ent instanceof EntityPoster) {
                    EntityPoster poster = (EntityPoster) ent;
                    if (ItemUtil.is(poster.inv, Core.registry.spawnPoster)) return true;
                    // Allow multiple posters if there are no empty ones
                } else {
                    return true;
                }
            }
            final double xwidth = plane.maxX - plane.minX;
            final double ywidth = plane.maxY - plane.minY;
            final double zwidth = plane.maxZ - plane.minZ;
            bestWidth = SpaceUtil.getDiagonalLength(plane);

            if (xwidth != 0) bestWidth = xwidth;
            if (ywidth != 0 && ywidth < bestWidth) bestWidth = ywidth;
            if (zwidth != 0 && zwidth < bestWidth) bestWidth = zwidth;
            if (bestWidth <= 2.0 / 16.0) return true;
            return false;
        }

        private boolean determineBoundingBox() {
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

            if (blockBox == null) blockBox = at.getCollisionBoundingBox();
            if (blockBox == null) {
                MovingObjectPosition mop = at.getBlock().collisionRayTrace(at.w, at.x, at.y, at.z, playerEye, reachEnd);
                if (mop != null) {
                    // Oh, look, the mop doesn't actually help us! Let's just act like this block's like BlockTorch and sets its bounds idiotically like it does
                    blockBox = at.getBlockBounds();
                }
            }
            //Client-side only: if (blockBox == null) blockBox = at.getSelectedBoundingBoxFromPool();
            if (blockBox == null) return true;
            return false;
        }

        public void spawn() {
            w.spawnEntityInWorld(result);

            if (!player.capabilities.isCreativeMode) {
                is.stackSize--;
            }
        }
    }
}
