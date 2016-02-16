package factorization.util;

import factorization.api.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;
import net.minecraft.world.chunk.Chunk;

import javax.vecmath.*;
import javax.vecmath.Vector3d;
import java.util.*;

/**
 * Operations on AxisAlignedBB (aka 'Box'), Vec3, EnumFacing, Entities, and conversions between them.
 *
 * Vec3 is unreliable (particularly historically!); it's best to use this.
 */
public final class SpaceUtil {

    public static final byte GET_POINT_MIN = 0x0;
    public static final byte GET_POINT_MAX = 0x7;

    private static ThreadLocal<ArrayList<EnumFacing>> direction_cache = new ThreadLocal<ArrayList<EnumFacing>>();

    public static EnumFacing determineOrientation(EntityPlayer player) {
        if (player.rotationPitch > 75) {
            return EnumFacing.DOWN;
        }
        if (player.rotationPitch <= -75) {
            return EnumFacing.UP;
        }
        return determineFlatOrientation(player);
    }

    public static EnumFacing determineFlatOrientation(EntityPlayer player) {
        //stolen from BlockPistonBase.determineOrientation. It was reversed, & we handle the y-axis differently
        int var7 = MathHelper.floor_double((double) ((180 + player.rotationYaw) * 4.0F / 360.0F) + 0.5D) & 3;
        int r = var7 == 0 ? 2 : (var7 == 1 ? 5 : (var7 == 2 ? 3 : (var7 == 3 ? 4 : 0)));
        return EnumFacing.VALUES[r];
    }

    public static DeltaCoord getFlatDiagonalFacing(EntityPlayer player) {
        double angle = Math.toRadians(90 + player.rotationYaw);
        int dx = Math.cos(angle) > 0 ? 1 : -1;
        int dz = Math.sin(angle) > 0 ? 1 : -1;
        return new DeltaCoord(dx, 0, dz);
    }

    @Deprecated // newVec!
    public static Vec3 newvec() {
        return new Vec3(0, 0, 0);
    }

    public static Vec3 copy(Vec3 a) {
        return new Vec3(a.xCoord, a.yCoord, a.zCoord);
    }

    public static AxisAlignedBB copy(AxisAlignedBB box) {
        return new AxisAlignedBB(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    public static Vec3 fromEntPos(Entity ent) {
        return new Vec3(ent.posX, ent.posY, ent.posZ);
    }

    public static Vec3 fromEntVel(Entity ent) {
        return new Vec3(ent.motionX, ent.motionY, ent.motionZ);
    }

    public static void toEntVel(Entity ent, Vec3 vec) {
        ent.motionX = vec.xCoord;
        ent.motionY = vec.yCoord;
        ent.motionZ = vec.zCoord;
    }

    public static Vec3 fromPlayerEyePos(EntityPlayer ent) {
        // This is all iChun's fault. :/
        // Uh...
        if (ent.worldObj.isRemote) {
            return new Vec3(ent.posX, ent.posY + (ent.getEyeHeight() - ent.getDefaultEyeHeight()), ent.posZ);
        } else {
            return new Vec3(ent.posX, ent.posY + ent.getEyeHeight(), ent.posZ);
        }
    }

    /** Sets the entity's position directly. Does *NOT* update the bounding box! */
    public static void toEntPos(Entity ent, Vec3 pos) {
        ent.posX = pos.xCoord;
        ent.posY = pos.yCoord;
        ent.posZ = pos.zCoord;
    }

    /** Sets the entity's position using its setter. Will (presumably) update the bounding box. */
    public static void setEntPos(Entity ent, Vec3 pos) {
        ent.setPosition(pos.xCoord, pos.yCoord, pos.zCoord);
    }

    public static AxisAlignedBB setMin(AxisAlignedBB aabb, Vec3 v) {
        return new AxisAlignedBB(
                v.xCoord, v.yCoord, v.zCoord,
                aabb.maxX, aabb.maxY, aabb.maxZ);
    }

    public static Vec3 getMax(AxisAlignedBB aabb) {
        return new Vec3(aabb.maxX, aabb.maxY, aabb.maxZ);
    }

    public static Vec3 getMin(AxisAlignedBB aabb) {
        return new Vec3(aabb.minX, aabb.minY, aabb.minZ);
    }

    public static AxisAlignedBB setMax(AxisAlignedBB aabb, Vec3 v) {
        return new AxisAlignedBB(
                aabb.minX, aabb.minY, aabb.minZ,
                v.xCoord, v.yCoord, v.zCoord);
    }

    public static Vec3 getMiddle(AxisAlignedBB ab) {
        return new Vec3(
                (ab.minX + ab.maxX) / 2,
                (ab.minY + ab.maxY) / 2,
                (ab.minZ + ab.maxZ) / 2);
    }

    public static AxisAlignedBB incrContract(AxisAlignedBB box, double dx, double dy, double dz) {
        return box.contract(dx, dy, dz);
    }

    public static Vec3 fromDirection(EnumFacing dir) {
        //return new Vec3(pos.getX(), pos.getY(), pos.getZ());
        NORELEASE.fixme("There may be more bad conversions like this; there is a direct coord query from EnumFacing.");
        return new Vec3(dir.getDirectionVec().getX(), dir.getDirectionVec().getY(), dir.getDirectionVec().getZ());
    }

    public static SortedPair<Vec3> sort(Vec3 left, Vec3 right) {
        double minX = Math.min(left.xCoord, right.xCoord);
        double maxX = Math.max(left.xCoord, right.xCoord);
        double minY = Math.min(left.yCoord, right.yCoord);
        double maxY = Math.max(left.yCoord, right.yCoord);
        double minZ = Math.min(left.zCoord, right.zCoord);
        double maxZ = Math.max(left.zCoord, right.zCoord);
        return new SortedPair<Vec3>(new Vec3(minX, minY, minZ), new Vec3(maxX, maxY, maxZ));
    }

    /**
     * Copies a point on box into target.
     * pointFlags is a bit-flag, like <Z, Y, X>.
     * So if the value is 0b000, then target is the minimum point,
     * and 0b111 the target is the maximum.
     */
    public static Vec3 getVertex(AxisAlignedBB box, byte pointFlags) {
        boolean xSide = (pointFlags & 1) == 1;
        boolean ySide = (pointFlags & 2) == 2;
        boolean zSide = (pointFlags & 4) == 4;
        return new Vec3(
                xSide ? box.minX : box.maxX,
                ySide ? box.minY : box.maxY,
                zSide ? box.minZ : box.maxZ
        );
    }

    /**
     * @param box The box to be flattened
     * @param face The side of the box that will remain untouched; the opposite face will be brought to it
     * @return A new box, with a volume of 0. Returns null if face is invalid.
     */
    public static AxisAlignedBB flatten(AxisAlignedBB box, EnumFacing face) {
        byte[] lows = new byte[] { 0x2, 0x0, 0x4, 0x0, 0x1, 0x0 };
        byte[] hghs = new byte[] { 0x7, 0x5, 0x7, 0x3, 0x7, 0x6 };
        byte low = lows[face.ordinal()];
        byte high = hghs[face.ordinal()];
        assert low != high;
        assert (~low & 0x7) != high;
        return newBox(getVertex(box, low), getVertex(box, high));
    }

    public static double getDiagonalLength(AxisAlignedBB ab) {
        double x = ab.maxX - ab.minX;
        double y = ab.maxY - ab.minY;
        double z = ab.maxZ - ab.minZ;
        return Math.sqrt(x * x + y * y + z * z);
    }

    public static Vec3 averageVec(Vec3 a, Vec3 b) {
        return new Vec3((a.xCoord + b.xCoord) / 2, (a.yCoord + b.yCoord) / 2, (a.zCoord + b.zCoord) / 2);
    }


    public static double getAngle(Vec3 a, Vec3 b) {
        double dot = a.dotProduct(b);
        double mags = a.lengthVector() * b.lengthVector();
        double div = dot / mags;
        if (div > 1) div = 1;
        if (div < -1) div = -1;
        return Math.acos(div);
    }

    public static AxisAlignedBB newBox(Vec3 min, Vec3 max) {
        return new AxisAlignedBB(
                min.xCoord, min.yCoord, min.zCoord,
                max.xCoord, max.yCoord, max.zCoord);
    }

    public static AxisAlignedBB newBox(Vec3[] parts) {
        return newBox(getLowest(parts), getHighest(parts));
    }

    public static Vec3 scale(Vec3 base, double s) {
        return new Vec3(base.xCoord * s, base.yCoord * s, base.zCoord * s);
    }

    public static Vec3 componentMultiply(Vec3 a, Vec3 b) {
        return new Vec3(a.xCoord + b.yCoord, a.yCoord + b.yCoord, a.zCoord + b.yCoord);
    }

    public static Vec3 componentMultiply(Vec3 a, double x, double y, double z) {
        return new Vec3(a.xCoord + x, a.yCoord + y, a.zCoord + z);
    }

    public static AxisAlignedBB newBoxSort(Vec3 min, Vec3 max) {
        double minX = Math.min(min.xCoord, max.xCoord);
        double minY = Math.min(min.yCoord, max.yCoord);
        double minZ = Math.min(min.zCoord, max.zCoord);
        double maxX = Math.max(min.xCoord, max.xCoord);
        double maxY = Math.max(min.yCoord, max.yCoord);
        double maxZ = Math.max(min.zCoord, max.zCoord);
        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static AxisAlignedBB newBoxUnsort(Vec3 min, Vec3 max) {
        return new AxisAlignedBB(
                min.xCoord, min.yCoord, min.zCoord,
                max.xCoord, max.yCoord, max.zCoord);
    }

    public static AxisAlignedBB createAABB(Coord min, Coord max) {
        return new AxisAlignedBB(min.x, min.y, min.z,
                max.x, max.y, max.z);
    }

    public static AxisAlignedBB newBox(Coord min, Coord max) {
        return createAABB(min, max);
    }

    public static AxisAlignedBB addCoord(AxisAlignedBB box, Vec3 vec) {
        return box.addCoord(vec.xCoord, vec.yCoord, vec.zCoord);
        // NORELEASE: Is the above right? Should be equivalent to this:
        /*if (vec.xCoord < box.minX) box.minX = vec.xCoord;
        if (box.maxX < vec.xCoord) box.maxX = vec.xCoord;
        if (vec.yCoord < box.minY) box.minY = vec.yCoord;
        if (box.maxY < vec.yCoord) box.maxY = vec.yCoord;
        if (vec.zCoord < box.minZ) box.minZ = vec.zCoord;
        if (box.maxZ < vec.zCoord) box.maxZ = vec.zCoord;*/
    }

    public static Vec3[] getCorners(AxisAlignedBB box) {
        return new Vec3[]{
                new Vec3(box.minX, box.minY, box.minZ),
                new Vec3(box.minX, box.maxY, box.minZ),
                new Vec3(box.maxX, box.maxY, box.minZ),
                new Vec3(box.maxX, box.minY, box.minZ),

                new Vec3(box.minX, box.minY, box.maxZ),
                new Vec3(box.minX, box.maxY, box.maxZ),
                new Vec3(box.maxX, box.maxY, box.maxZ),
                new Vec3(box.maxX, box.minY, box.maxZ)
        };
    }

    public static Vec3 getLowest(Vec3[] vs) {
        double x, y, z;
        x = y = z = 0;
        boolean first = true;
        for (int i = 0; i < vs.length; i++) {
            Vec3 v = vs[i];
            if (v == null) continue;
            if (first) {
                first = false;
                x = v.xCoord;
                y = v.yCoord;
                z = v.zCoord;
                continue;
            }
            if (v.xCoord < x) x = v.xCoord;
            if (v.yCoord < y) y = v.yCoord;
            if (v.zCoord < z) z = v.zCoord;
        }
        return new Vec3(x, y, z);
    }

    public static Vec3 getHighest(Vec3[] vs) {
        double x, y, z;
        x = y = z = 0;
        boolean first = true;
        for (int i = 0; i < vs.length; i++) {
            Vec3 v = vs[i];
            if (v == null) continue;
            if (first) {
                first = false;
                x = v.xCoord;
                y = v.yCoord;
                z = v.zCoord;
                continue;
            }
            if (v.xCoord > x) x = v.xCoord;
            if (v.yCoord > y) y = v.yCoord;
            if (v.zCoord > z) z = v.zCoord;
        }
        return new Vec3(x, y, z);
    }

    public static ArrayList<EnumFacing> getRandomDirections(Random rand) {
        ArrayList<EnumFacing> ret = direction_cache.get();
        if (ret == null) {
            ret = new ArrayList(6);
            for (int i = 0; i < 6; i++) {
                ret.add(SpaceUtil.getOrientation(i));
            }
            direction_cache.set(ret);
        }
        Collections.shuffle(ret, rand);
        return ret;
    }

    public static int getAxis(EnumFacing fd) {
        if (fd.getDirectionVec().getX() != 0) {
            return 1;
        }
        if (fd.getDirectionVec().getY() != 0) {
            return 2;
        }
        if (fd.getDirectionVec().getZ() != 0) {
            return 3;
        }
        return 0;
    }

    public static boolean isZero(Vec3 vec) {
        return vec.xCoord == 0 && vec.yCoord == 0 && vec.zCoord == 0;
    }


    /**
     * Return the distance between point and the line defined as passing through the origin and lineVec
     * @param lineVec The vector defining the line, relative to the origin.
     * @param point The point being measured, relative to the origin
     * @return the distance between line defined by lineVec and point
     */
    public static double lineDistance(Vec3 lineVec, Vec3 point) {
        // http://mathworld.wolfram.com/Point-LineDistance3-Dimensional.html equation 9
        double mag = lineVec.lengthVector();
        Vec3 nPoint = scale(point, -1);
        return lineVec.crossProduct(nPoint).lengthVector() / mag;
    }

    public static EnumFacing getOrientation(int ordinal) {
        if (ordinal < 0) return null;
        if (ordinal >= 6) return null;
        return EnumFacing.VALUES[ordinal];
    }

    public static FzOrientation getOrientation(EntityPlayer player, EnumFacing facing, Vec3 hit) {
        double u = 0.5, v = 0.5; //We pick the axiis based on which side gets clicked
        if (facing == null) facing = EnumFacing.DOWN;
        assert facing != null;
        switch (facing) {
            default:
            case DOWN:
                u = 1 - hit.xCoord;
                v = hit.zCoord;
                break;
            case UP:
                u = hit.xCoord;
                v = hit.zCoord;
                break;
            case NORTH:
                u = hit.xCoord;
                v = hit.yCoord;
                break;
            case SOUTH:
                u = 1 - hit.xCoord;
                v = hit.yCoord;
                break;
            case WEST:
                u = 1 - hit.zCoord;
                v = hit.yCoord;
                break;
            case EAST:
                u = hit.zCoord;
                v = hit.yCoord;
                break;
        }
        u -= 0.5;
        v -= 0.5;
        double angle = Math.toDegrees(Math.atan2(v, u)) + 180;
        angle = (angle + 45) % 360;
        int pointy = (int) (angle/90);
        pointy = (pointy + 1) % 4;

        FzOrientation fo = FzOrientation.fromDirection(facing);
        for (int X = 0; X < pointy; X++) {
            fo = fo.getNextRotationOnFace();
        }
        EnumFacing orient = SpaceUtil.determineOrientation(player);
        if (orient.getAxis() != EnumFacing.Axis.Y
                && facing.getAxis() == EnumFacing.Axis.Y) {
            facing = orient;
            fo = orient == null ? null : FzOrientation.fromDirection(orient.getOpposite());
            if (fo != null) {
                FzOrientation perfect = fo.pointTopTo(EnumFacing.UP);
                if (perfect != null) {
                    fo = perfect;
                }
            }
        }
        double dist = Math.max(Math.abs(u), Math.abs(v));
        if (dist < 0.33) {
            FzOrientation perfect = fo.pointTopTo(EnumFacing.UP);
            if (perfect != null) {
                fo = perfect;
            }
        }
        return fo;
    }

    public static int sign(EnumFacing dir) {
        if (dir == null) return 0;
        return dir.getAxisDirection().getOffset();
    }

    public static double sum(Vec3 vec) {
        return vec.xCoord + vec.yCoord + vec.zCoord;
    }

    public static EnumFacing round(Vec3 vec, EnumFacing not) {
        if (isZero(vec)) return null;
        Vec3i work = null;
        double bestAngle = Double.POSITIVE_INFINITY;
        EnumFacing closest = null;
        for (EnumFacing dir : EnumFacing.VALUES) {
            if (dir == not) continue;
            work = dir.getDirectionVec();
            double dot = getAngle(vec, new Vec3(work));
            if (dot < bestAngle) {
                bestAngle = dot;
                closest = dir;
            }
        }
        return closest;
    }

    public static Vec3 floor(Vec3 vec) {
        return new Vec3(
                Math.floor(vec.xCoord),
                Math.floor(vec.yCoord),
                Math.floor(vec.zCoord));
    }

    public static Vec3i vInt(Vec3 vec) {
        return new Vec3i(
                Math.floor(vec.xCoord),
                Math.floor(vec.yCoord),
                Math.floor(vec.zCoord));
    }

    public static Vec3 normalize(Vec3 v) {
        // Vanilla's threshold is too low for my purposes.
        double length = v.lengthVector();
        if (length == 0) return newvec();
        double inv = 1.0 / length;
        if (Double.isNaN(inv) || Double.isInfinite(inv)) return newvec();
        return scale(v, inv);
    }

    public static AxisAlignedBB include(AxisAlignedBB box, Coord at) {
        double minX = box.minX;
        double maxX = box.maxX;
        double minY = box.minY;
        double maxY = box.maxY;
        double minZ = box.minZ;
        double maxZ = box.maxZ;

        if (at.x < minX) minX = at.x;
        if (at.x + 1 > maxX) maxX = at.x + 1;
        if (at.y < minY) minY = at.y;
        if (at.y + 1 > maxY) maxY = at.y + 1;
        if (at.z < minZ) minZ = at.z;
        if (at.z + 1 > maxZ) maxZ = at.z + 1;

        return new AxisAlignedBB(
                minX, minY, minZ,
                maxX, maxY, maxZ);
    }

    public static AxisAlignedBB include(AxisAlignedBB box, Vec3 at) {
        double minX = box.minX;
        double maxX = box.maxX;
        double minY = box.minY;
        double maxY = box.maxY;
        double minZ = box.minZ;
        double maxZ = box.maxZ;

        if (at.xCoord < minX) minX = at.xCoord;
        if (at.xCoord > maxX) maxX = at.xCoord;
        if (at.yCoord < minY) minY = at.yCoord;
        if (at.yCoord > maxY) maxY = at.yCoord;
        if (at.zCoord < minZ) minZ = at.zCoord;
        if (at.zCoord > maxZ) maxZ = at.zCoord;

        return new AxisAlignedBB(
                minX, minY, minZ,
                maxX, maxY, maxZ);
    }

    public static boolean contains(AxisAlignedBB box, Coord at) {
        return NumUtil.intersect(box.minX, box.maxX, at.x, at.x + 1)
                && NumUtil.intersect(box.minY, box.maxY, at.y, at.y + 1)
                && NumUtil.intersect(box.minZ, box.maxZ, at.z, at.z + 1);

    }

    public static double getVolume(AxisAlignedBB box) {
        if (box == null) return 0;
        double x = box.maxX - box.minX;
        double y = box.maxY - box.minY;
        double z = box.maxZ - box.minZ;
        double volume = x * y * z;

        if (volume < 0) return 0;
        return volume;
    }

    public static AxisAlignedBB getBox(Coord at, int R) {
        return SpaceUtil.createAABB(at.add(-R, -R, -R), at.add(+R, +R, +R));
    }

    public static Vec3 dup(double d) {
        return new Vec3(d, d, d);
    }

    public static EnumFacing demojangSide(int side) {
        switch (side) {
            case 0: return EnumFacing.SOUTH;
            case 1: return EnumFacing.WEST;
            case 2: return EnumFacing.NORTH;
            case 3: return EnumFacing.EAST;
            default:
            case 4: return EnumFacing.UP; // Making this up
            case 5: return EnumFacing.DOWN; // And this one
        }
    }

    /**
     * Rotate the allowed direction that is nearest to the rotated dir.
     * @param dir The original direction
     * @param rot The rotation to apply
     * @param allow The directions that may be used.
     * @return A novel direction
     */
    public static EnumFacing rotateDirection(EnumFacing dir, Quaternion rot, Iterable<EnumFacing> allow) {
        Vec3 v = fromDirection(dir);
        rot.applyRotation(v);
        EnumFacing best = null;
        double bestDot = Double.POSITIVE_INFINITY;
        for (EnumFacing fd : allow) {
            Vec3 f = fromDirection(fd);
            rot.applyRotation(f);
            double dot = v.dotProduct(f);
            if (dot < bestDot) {
                bestDot = dot;
                best = fd;
            }
        }
        return best;
    }

    public static EnumFacing rotateDirectionAndExclude(EnumFacing dir, Quaternion rot, Collection<EnumFacing> allow) {
        EnumFacing ret = rotateDirection(dir, rot, allow);
        allow.remove(ret);
        allow.remove(ret.getOpposite());
        return ret;
    }

    public static Vec3 subtract(Vec3 you, Vec3 me) {
        return you.subtract(me);
    }

    public static Vec3 setX(Vec3 v, double x) {
        return new Vec3(x, v.yCoord, v.zCoord);
    }

    public static Vec3 setY(Vec3 v, double y) {
        return new Vec3(v.xCoord, y, v.zCoord);
    }

    public static Vec3 setZ(Vec3 v, double z) {
        return new Vec3(v.xCoord, v.yCoord, z);
    }

    public static EnumFacing fromAxis(EnumFacing.Axis a) {
        if (a == EnumFacing.Axis.Y) return EnumFacing.DOWN;
        if (a == EnumFacing.Axis.X) return EnumFacing.WEST;
        if (a == EnumFacing.Axis.Z) return EnumFacing.NORTH;
        return null;
    }

    public static AxisAlignedBB newBox() {
        return new AxisAlignedBB(0, 0, 0, 0, 0, 0);
    }

    public static Vec3 newVec() {
        return new Vec3(0, 0, 0);
    }

    public static BlockPos newPos() {
        return new BlockPos(0, 0, 0);
    }

    public static AxisAlignedBB newBoxAround(BlockPos pos) {
        return new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }


    private static final int[][] ROTATION_MATRIX = {
            {0, 1, 4, 5, 3, 2},
            {0, 1, 5, 4, 2, 3},
            {5, 4, 2, 3, 0, 1},
            {4, 5, 2, 3, 1, 0},
            {2, 3, 1, 0, 4, 5},
            {3, 2, 0, 1, 4, 5},
            {0, 1, 2, 3, 4, 5}
    };
    // Rescued from Forge. (This is a table of simple mathematical facts and involves
    // no creativity or arrangement, therefore copyright doesn't apply. So there.)

    public static EnumFacing rotate(EnumFacing dir, EnumFacing axis) {
        // EnumFacing admittedly does have rotate methods.
        // However, I don't feel like trusting them to work the same as ForgeDirection did.
        // If this is in fact unnecessarily it'll be easy enough to inline the appropriate code.
        return EnumFacing.VALUES[ROTATION_MATRIX[axis.ordinal()][dir.ordinal()]];
    }

    public static EnumFacing rotateBack(EnumFacing dir, EnumFacing axis) {
        return rotate(rotate(rotate(dir, axis), axis), axis);
    }

    public static Iterable<BlockPos.MutableBlockPos> iteratePos(BlockPos src, int r) {
        return BlockPos.getAllInBoxMutable(src.add(-r, -r, -r), src.add(+r, +r, +r));
    }

    public static Vector3d toJavax(Vec3 val) {
        return new Vector3d(val.xCoord, val.yCoord, val.zCoord);
    }

    public static AxisAlignedBB getBox(Chunk chunk) {
        int minX = chunk.xPosition << 4;
        int minZ = chunk.zPosition << 4;
        return AxisAlignedBB.fromBounds(minX, 0, minZ, minX + 16, 0xFF, minZ + 16);
    }

    public static boolean equals(AxisAlignedBB a, AxisAlignedBB b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.minX == b.minX && a.minY == b.minY && a.minZ == b.minZ
                && a.maxX == b.maxX && a.maxY == b.maxY && a.maxZ == b.maxZ;
    }

    public static boolean equals(Vec3 a, Vec3 b) {
        return a.xCoord == b.xCoord && a.yCoord == b.yCoord && a.zCoord == b.zCoord;
    }
}
