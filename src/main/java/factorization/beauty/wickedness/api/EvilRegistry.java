package factorization.beauty.wickedness.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class EvilRegistry {
    public static final String NORMAL = "normal";
    public static final String ON_BREAK = "on_break";

    public static int getId(IEvil evil) {
        if (evil == null) return 0;
        return evil.getId().hashCode();
    }

    public static void register(String category, IEvil evil) {
        getCategory(category).put(evil.getId().hashCode(), evil);
    }

    public static void register(IEvil evil) {
        register(NORMAL, evil);
    }

    public static IEvil get(String category, int id) {
        if (id == 0) return null;
        return getCategory(category).get(id);
    }

    public static IEvil find(String category, int color1, int color2) {
        ArrayList<IEvil> all = new ArrayList<>(getCategory(category).values());
        Collections.shuffle(all);
        float bestDist = Float.POSITIVE_INFINITY;
        IEvil best = null;
        for (IEvil evil : all) {
            float dist = colorDistance(evil.getMainColor(), color1) + colorDistance(evil.getSecondColor(), color2);
            if (dist < bestDist) {
                bestDist = dist;
                best = evil;
            }
        }
        return best;
    }

    private static float colorDistance(int c1, int c2) {
        //noinspection PointlessBitwiseExpression
        return    Math.abs((c1 & 0xFF0000) - (c2 & 0xFF0000)) >> 16
                + Math.abs((c1 & 0x00FF00) - (c2 & 0x00FF00)) >> 8
                + Math.abs((c1 & 0x0000FF) - (c2 & 0x0000FF)) >> 0;
    }

    private static HashMap<Integer, IEvil> getCategory(String category) {
        if (!registry.containsKey(category)) {
            HashMap<Integer, IEvil> ret = new HashMap<Integer, IEvil>();
            registry.put(category, ret);
            return ret;
        }
        return registry.get(category);
    }

    public static final HashMap<String, HashMap<Integer, IEvil>> registry = new HashMap<>();

}
