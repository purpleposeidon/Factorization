package factorization.util;

import net.minecraft.util.Vec3;

import java.util.Random;

public final class NumUtil {
    public static Random rand = new Random();
    private static ThreadLocal<Random> random_cache = new ThreadLocal<Random>();

    public static boolean significantChange(float a, float b, float threshold) {
        if (a == b) {
            return false;
        }
        if (a == 0 || b == 0) {
            a = Math.abs(a);
            b = Math.abs(b);
            if (a + b < 2) return true;
        }
        float thresh = Math.abs(a - b)/Math.max(a, b);
        return thresh > threshold;
    }

    public static boolean significantChange(float a, float b) {
        return significantChange(a, b, 0.05F);
    }

    /**
     *
     * @param oldValue
     * @param newValue
     * @param partial
     * @return the linear interpolation of the two values
     */
    public static float interp(float oldValue, float newValue, float partial) {
        return oldValue * (1 - partial) + newValue * partial;
    }

    public static double interp(double oldValue, double newValue, double partial) {
        return oldValue * (1 - partial) + newValue * partial;
    }

    public static void interp(Vec3 oldVal, Vec3 newVal, float partial, Vec3 dest) {
        dest.xCoord = interp(oldVal.xCoord, newVal.xCoord, partial);
        dest.yCoord = interp(oldVal.yCoord, newVal.yCoord, partial);
        dest.zCoord = interp(oldVal.zCoord, newVal.zCoord, partial);
    }

    public static float uninterp(float lowValue, float highValue, float currentValue) {
        if (currentValue < lowValue) return 0F;
        if (currentValue > highValue) return 1F;
        return (currentValue - lowValue) / (highValue - lowValue);
    }

    public static double uninterp(double lowValue, double highValue, double currentValue) {
        if (currentValue < lowValue) return 0F;
        if (currentValue > highValue) return 1F;
        return (currentValue - lowValue) / (highValue - lowValue);
    }

    public static double roundDown(double value, double units) {
        double scaled = value / units;
        return Math.floor(scaled) * units;
    }

    public static boolean intersect(double la, double ha, double lb, double hb) {
        //If we're not intersecting, then one is to the right of the other.
        //<--  (la ha) -- (lb hb) -->
        //<--- (lb hb) -- (la ha) -->
        return !(ha < lb || hb < la);
    }

    public static Random dirtyRandomCache() {
        Random ret = random_cache.get();
        if (ret == null) {
            ret = new Random();
            random_cache.set(ret);
        }
        return ret;
    }
}
