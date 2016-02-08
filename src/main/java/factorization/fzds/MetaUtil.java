package factorization.fzds;

import factorization.api.Mat;
import factorization.util.NORELEASE;
import factorization.util.SpaceUtil;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Iterator;

public final class MetaUtil {
    public static double getMinRadius(AxisAlignedBB box) {
        NORELEASE.fixme("SpaceUtil");
        double sizeX = box.maxX - box.minX;
        double sizeY = box.maxY - box.minY;
        double sizeZ = box.maxZ - box.minZ;
        return Math.min(sizeX, Math.min(sizeY, sizeZ)) / 2;
    }

    public static double getMaxRadius(AxisAlignedBB box) {
        double sizeX = box.maxX - box.minX;
        double sizeY = box.maxY - box.minY;
        double sizeZ = box.maxZ - box.minZ;
        return Math.max(sizeX, Math.max(sizeY, sizeZ)) / 2;
    }

    public static AxisAlignedBB boxAround(Vec3 mid, double r) {
        return new AxisAlignedBB(
                mid.xCoord - r, mid.yCoord - r, mid.zCoord - r,
                mid.xCoord + r, mid.yCoord + r, mid.zCoord + r);
    }

    public static Iterable<AxisAlignedBB> iterate(World world, AxisAlignedBB region) {
        NORELEASE.fixme("custom iterator + last-chunk cache");
        NORELEASE.fixme("Entities?");
        return world.getCollidingBoundingBoxes(null, region);
    }

    public static boolean intersects(World world, Mat mat, AxisAlignedBB box) {
        Vec3 mid = mat.mul(SpaceUtil.getMiddle(box));
        AxisAlignedBB big = boxAround(mid, getMaxRadius(box));
        AxisAlignedBB small = null;
        for (AxisAlignedBB hit : iterate(world, big)) {
            if (small == null) {
                small = boxAround(mid, getMinRadius(box));
            }
            if (small.intersectsWith(hit)) {
                return true;
            }
            NORELEASE.fixme("Unaligned bounding box intersection :DDDD");
            NORELEASE.fixme("Entities?");
            //http://www.metanetsoftware.com/technique/tutorialA.html
            //http://www.geometrictools.com/Documentation/DynamicCollisionDetection.pdf
            // (And also the toplevel, perhaps.)
            //Big grid of various intersections:
            //http://www.realtimerendering.com/intersections.html
            //   http://www.gamasutra.com/view/feature/131790/simple_intersection_tests_for_games.php?page=5

            /*
            for the x, y, and z axes:
                flatten the turned box into a convex polygon.
                check if the polygon intersects:
                    for each of the other two axes:
                        flatten the polygon into a range. If it doesn't intersect w/ the box, then there's no collision.
             */
            return NORELEASE.just(true);
        }
        /*
        We have an AABB & a OBB.
        The OBS has three axes; they're made by rotating the three real-world axes by the rotation quaternion.
        Taking the dot product between a normal vector (eg, an axis) and an extent gives the length of the extent on that normal.
         */
        return false;
    }
}
