package factorization.mechanisms;

import factorization.api.Coord;
import net.minecraft.block.material.Material;
import net.minecraft.util.AxisAlignedBB;

public class MassHelper {
    public static boolean CONSIDER_VOLUME;

    public static double getBlockMass(Coord at) {
        double density = getMaterialDensity(at);
        if (!CONSIDER_VOLUME) return density;
        AxisAlignedBB box = at.getCollisionBoundingBoxFromPool();
        return density * getVolume(box);
    }

    public static double getMaterialDensity(Coord at) {
        Material mat = at.getBlock().getMaterial();
        if (mat == Material.air) {
            return 0.0;
        }
        if (mat == Material.cactus || mat == Material.leaves || mat == Material.plants || mat == Material.vine) {
            return 0.25;
        }
        if (mat == Material.wood) {
            return 0.5;
        }
        if (mat == Material.iron || mat == Material.anvil) {
            return 7;
        }
        if (mat == Material.cloth || mat == Material.carpet || mat == Material.web) {
            return 0.1;
        }
        if (mat == Material.water || mat == Material.gourd) {
            return 1.0;
        }
        if (mat == Material.snow || mat == Material.ice) {
            return 0.8;
        }
        return 2.0;
    }

    private static double MAX_VOLUME = 3 * 3 * 3;

    private static double getVolume(AxisAlignedBB box) {
        if (box == null) return 0;
        double x = box.maxX - box.minX;
        double y = box.maxY - box.minY;
        double z = box.maxZ - box.minZ;
        double volume = x * y * z;

        if (volume < 0) return 0;
        if (volume > MAX_VOLUME) volume = MAX_VOLUME;
        return volume;
    }
}
